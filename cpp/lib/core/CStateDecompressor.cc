/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
#include <core/CStateDecompressor.h>

#include <core/CBase64Filter.h>

#include <rapidjson/document.h>

#include <boost/iostreams/filter/gzip.hpp>

#include <string.h>

namespace prelert
{
namespace core
{

const std::string CStateDecompressor::EMPTY_DATA("H4sIAAAAAAAA/4uOBQApu0wNAgAAAA==");
const std::string CStateDecompressor::COMPRESSED("compressed");
const std::string CStateDecompressor::END_OF_STREAM("eos");

CStateDecompressor::CStateDecompressor(CDataSearcher &compressedSearcher) :
                                       m_Searcher(compressedSearcher),
                                       m_FilterSource(compressedSearcher)
{
    m_InFilter.reset(new TFilteredInput);
    m_InFilter->push(boost::iostreams::gzip_decompressor());
    m_InFilter->push(CBase64Decoder());
    m_InFilter->push(boost::ref(m_FilterSource));
}

CDataSearcher::TIStreamP CStateDecompressor::search(size_t /*currentDocNum*/,
                                                    size_t /*limit*/)
{
    return m_InFilter;
}

void CStateDecompressor::setStateRestoreSearch(const std::string &index,
                                               const std::string &type)
{
    m_Searcher.setStateRestoreSearch(index, type);
}

void CStateDecompressor::setStateRestoreSearch(const std::string &index,
                                               const std::string &type,
                                               const std::string &id)
{
    m_Searcher.setStateRestoreSearch(index, type, id);
}

CStateDecompressor::CDechunkFilter::CDechunkFilter(CDataSearcher &searcher) :
            m_Initialised(false),
            m_SentData(false),
            m_Searcher(searcher),
            m_CurrentDocNum(1),
            m_EndOfStream(false)
{
}

std::streamsize CStateDecompressor::CDechunkFilter::read(char *s, std::streamsize n)
{
    if (m_EndOfStream)
    {
        LOG_TRACE("EOS -1");
        return -1;
    }
    // return number of bytes read, -1 for EOF
    std::streamsize bytesDone = 0;
    while (bytesDone < n)
    {
        if (!m_IStream)
        {
            // Get a new input stream
            LOG_TRACE("Getting new stream, for document number " << m_CurrentDocNum);

            m_IStream = m_Searcher.search(m_CurrentDocNum, 1);
            if (!m_IStream)
            {
                LOG_ERROR("Unable to connect to data store");
                return this->endOfStream(s, n, bytesDone);
            }
            if (m_IStream->bad())
            {
                LOG_ERROR("Error connecting to data store");
                return this->endOfStream(s, n, bytesDone);
            }

            if (m_IStream->fail())
            {
                m_EndOfStream = true;
                // This is not fatal - we just didn't find the given document number
                // Presume that we have finished
                LOG_TRACE("No more documents to find");
                return this->endOfStream(s, n, bytesDone);
            }

            m_InputStreamWrapper.reset(new rapidjson::GenericReadStream(*m_IStream));
            m_Reader.reset(new rapidjson::PullReader);

            if (!this->readHeader())
            {
                return this->endOfStream(s, n, bytesDone);
            }
        }

        this->handleRead(s, n, bytesDone);
        if (m_EndOfStream)
        {
            return this->endOfStream(s, n, bytesDone);
        }

        if ((m_IStream) && (m_IStream->eof()))
        {
            LOG_TRACE("Stream EOF");
            m_IStream.reset();
            ++m_CurrentDocNum;
        }
    }
    LOG_TRACE("Returning " << bytesDone << ": " << std::string(s, bytesDone));
    return bytesDone;
}

bool CStateDecompressor::CDechunkFilter::readHeader(void)
{
    rapidjson::Token token;
    if (m_Reader->ParseFirst<rapidjson::kParseDefaultFlags>(*m_InputStreamWrapper,
                                                            token) == false)
    {
        LOG_ERROR("Failed to find valid JSON");
        LOG_DEBUG("Parse error: " << m_Reader->HasParseError() << ", " << m_Reader->GetParseError() << ": " << m_Reader->GetErrorOffset());
        m_Initialised = false;
        m_IStream.reset();
        ++m_CurrentDocNum;
        return false;
    }

    while (m_Reader->ParseNext<rapidjson::kParseDefaultFlags>(*m_InputStreamWrapper,
                                                              token))
    {
        LOG_TRACE("Read token, type: " << token.type);

        if (token.type == rapidjson::kTokenArrayStart &&
            std::string(token.str, token.length) == COMPRESSED)
        {
            m_Initialised = true;
            m_BufferOffset = 0;
            return true;
        }
    }
    // If we are here, we have got an empty document from downstream,
    // so the stream is finished
    LOG_TRACE("Failed to find 'compressed' data array!");
    m_Initialised = false;
    m_IStream.reset();
    ++m_CurrentDocNum;
    return false;
}

void CStateDecompressor::CDechunkFilter::handleRead(char *s, std::streamsize n, std::streamsize &bytesDone)
{
    // Extract data from the JSON array "compressed"
    if (!m_Initialised)
    {
        return;
    }

    // Copy any outstanding data
    if (m_BufferOffset > 0)
    {
        std::streamsize toCopy = std::min((n - bytesDone), (m_Token.length - m_BufferOffset));
        ::memcpy(s + bytesDone, m_Token.str + m_BufferOffset, toCopy);
        bytesDone += toCopy;
        m_BufferOffset += toCopy;
    }

    // Expect to have data in an array
    while (bytesDone < n &&
           m_Reader->ParseNext<rapidjson::kParseDefaultFlags>(*m_InputStreamWrapper,
                                                              m_Token))
    {
        m_BufferOffset = 0;
        if (m_Token.type == rapidjson::kTokenArrayEnd)
        {
            LOG_TRACE("Come to end of array");
            if (m_Reader->ParseNext<rapidjson::kParseDefaultFlags>(*m_InputStreamWrapper, m_Token) &&
                m_Token.type == rapidjson::kTokenString &&
                std::string(m_Token.str, m_Token.length) == END_OF_STREAM)
            {
                LOG_DEBUG("Explicit end-of-stream marker found in document with index " <<
                          m_CurrentDocNum);
                // Don't search for any more documents after seeing this - any
                // that exist will be stale - see bug 1248 in Bugzilla
                m_EndOfStream = true;
            }
            m_IStream.reset();
            ++m_CurrentDocNum;
            break;
        }
        m_SentData = true;
        if (m_Token.length <= (n - bytesDone))
        {
            ::memcpy(s + bytesDone, m_Token.str, m_Token.length);
            bytesDone += m_Token.length;
        }
        else
        {
            std::streamsize toCopy = n - bytesDone;
            ::memcpy(s + bytesDone, m_Token.str, toCopy);
            bytesDone += toCopy;
            m_BufferOffset = toCopy;
            break;
        }
    }
}

std::streamsize CStateDecompressor::CDechunkFilter::endOfStream(char *s, std::streamsize n, std::streamsize bytesDone)
{
    // return [ ] if not m_Initialised
    m_EndOfStream = true;
    if (!m_SentData && bytesDone == 0)
    {
        std::streamsize toCopy = std::min(std::streamsize(EMPTY_DATA.size()), n);
        ::memcpy(s, EMPTY_DATA.c_str(), toCopy);
        return toCopy;
    }

    LOG_TRACE("Returning " << bytesDone);

    return (bytesDone == 0) ? -1 : bytesDone;
}

void CStateDecompressor::CDechunkFilter::close(void)
{
}

} // core
} // prelert
