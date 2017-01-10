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
#include <api/CCsvInputParser.h>

#include <core/CLogger.h>
#include <core/CTimeUtils.h>

#include <algorithm>
#include <istream>

#include <string.h>


namespace ml
{
namespace api
{


// Initialise statics
const char   CCsvInputParser::COMMA(',');
const char   CCsvInputParser::QUOTE('"');
const char   CCsvInputParser::RECORD_END('\n');
const char   CCsvInputParser::STRIP_BEFORE_END('\r');
const size_t CCsvInputParser::WORK_BUFFER_SIZE(131072); // 128kB


CCsvInputParser::CCsvInputParser(const std::string &input,
                                 char separator)
    : CInputParser(),
      m_Separator(separator),
      m_StringInputBuf(input),
      m_StrmIn(m_StringInputBuf),
      m_SeparatorAfterLastField(false),
      m_WorkBuffer(0),
      m_WorkBufferPtr(0),
      m_WorkBufferEnd(0),
      m_NoMoreRecords(false),
      m_WorkField(0),
      m_WorkFieldEnd(0),
      m_WorkFieldCapacity(0)
{
}

CCsvInputParser::CCsvInputParser(std::istream &strmIn,
                                 char separator)
    : CInputParser(),
      m_Separator(separator),
      m_StrmIn(strmIn),
      m_SeparatorAfterLastField(false),
      m_WorkBuffer(0),
      m_WorkBufferPtr(0),
      m_WorkBufferEnd(0),
      m_NoMoreRecords(false),
      m_WorkField(0),
      m_WorkFieldEnd(0),
      m_WorkFieldCapacity(0)
{
}

const std::string &CCsvInputParser::fieldNameStr(void) const
{
    return m_FieldNameStr;
}

bool CCsvInputParser::readStream(bool doDryRun,
                                 const TSettingsFunc &/*settingsFunc*/,
                                 const TReaderFunc &readerFunc)
{
    // Reset the record buffer pointers in case we're reading a new stream
    m_WorkBufferEnd = m_WorkBufferPtr;
    m_NoMoreRecords = false;
    TStrVec &fieldNames = this->fieldNames();

    if (!this->gotFieldNames())
    {
        if (this->parseCsvRecordFromStream() == false)
        {
            LOG_ERROR("Failed to parse CSV record from stream");
            return false;
        }

        if (this->parseFieldNames() == false)
        {
            if (fieldNames.empty())
            {
                // If we parsed no field names at all, return true, as Splunk
                // sometimes seems to send settings with no data
                LOG_TRACE("Field names are empty")
                return true;
            }

            LOG_ERROR("Failed to parse field names from stream");
            return false;
        }
    }
    // If we're doing a dry run, we'll need to store the field values so that
    // we can run through them a second time
    TStrStrUMapList dryRunFieldStore;

    // We reuse the same field map for every record
    TStrStrUMap recordFields;

    // Cache references to the strings in the map corresponding to each field
    // name - this avoids the need to repeatedly compute the same hashes
    TStrRefVec fieldValRefs;
    fieldValRefs.reserve(fieldNames.size());
    for (TStrVecCItr iter = fieldNames.begin();
         iter != fieldNames.end();
         ++iter)
    {
        fieldValRefs.push_back(boost::ref(recordFields[*iter]));
    }

    while (!m_NoMoreRecords)
    {
        if (this->parseCsvRecordFromStream() == false)
        {
            LOG_ERROR("Failed to parse CSV record from stream");
            return false;
        }

        if (m_NoMoreRecords)
        {
            break;
        }

        if (this->parseDataRecord(fieldValRefs) == false)
        {
            LOG_ERROR("Failed to parse data record from stream");
            return false;
        }

        if (readerFunc(doDryRun,
                       fieldNames,
                       recordFields) == false)
        {
            LOG_ERROR("Record handler function forced exit");
            return false;
        }

        if (doDryRun)
        {
            dryRunFieldStore.push_back(recordFields);
        }
    }

    if (doDryRun)
    {
        LOG_INFO("Completed dry run for " << dryRunFieldStore.size() <<
                 " records");

        // We did a dry run first time through - now replay the same data for
        // real
        size_t index(0);
        for (TStrStrUMapListCItr iter = dryRunFieldStore.begin();
             iter != dryRunFieldStore.end();
             ++iter, ++index)
        {
            if (readerFunc(false,
                           fieldNames,
                           *iter) == false)
            {
                LOG_ERROR("Record handler function forced exit");
                return false;
            }
        }
    }

    return true;
}

bool CCsvInputParser::parseCsvRecordFromStream(void)
{
    m_SeparatorAfterLastField = false;

    // For maximum performance, read the stream in large chunks that can be
    // moved around by memcpy().  Using memcpy() is an order of magnitude faster
    // than the naive approach of checking and copying one character at a time.
    // In modern versions of the GNU STL std::getline uses memchr() to search
    // for the delimiter and then memcpy() to transfer data to the target
    // std::string, but sadly this is not the case for the Microsoft and Apache
    // STLs.
    if (m_WorkBuffer.get() == 0)
    {
        m_WorkBuffer.reset(new char[WORK_BUFFER_SIZE]);
        m_WorkBufferPtr = m_WorkBuffer.get();
        m_WorkBufferEnd = m_WorkBufferPtr;
    }

    bool startOfRecord(true);
    size_t quoteCount(0);
    for (;;)
    {
        size_t avail(m_WorkBufferEnd - m_WorkBufferPtr);
        if (avail == 0)
        {
            if (m_StrmIn.eof())
            {
                // We have no buffered data and there's no more to read, so stop
                m_NoMoreRecords = true;
                break;
            }

            m_WorkBufferPtr = m_WorkBuffer.get();
            m_StrmIn.read(m_WorkBuffer.get(),
                          static_cast<std::streamsize>(WORK_BUFFER_SIZE));
            if (m_StrmIn.bad())
            {
                LOG_ERROR("Input stream is bad");
                m_CurrentRowStr.clear();
                m_WorkBufferEnd = m_WorkBufferPtr;
                return false;
            }

            avail = static_cast<size_t>(m_StrmIn.gcount());
            m_WorkBufferEnd = m_WorkBufferPtr + avail;
        }

        const char *delimPtr(reinterpret_cast<const char *>(::memchr(m_WorkBufferPtr,
                                                                     RECORD_END,
                                                                     avail)));
        const char *endPtr(m_WorkBufferEnd);
        if (delimPtr != 0)
        {
            endPtr = delimPtr;
            if (endPtr > m_WorkBufferPtr && *(endPtr - 1) == STRIP_BEFORE_END)
            {
                --endPtr;
            }
        }

        if (startOfRecord)
        {
            m_CurrentRowStr.assign(m_WorkBufferPtr, endPtr - m_WorkBufferPtr);
            startOfRecord = false;
        }
        else
        {
            if (endPtr == m_WorkBufferPtr)
            {
                size_t strLen(m_CurrentRowStr.length());
                if (strLen > 0 && m_CurrentRowStr[strLen - 1] == STRIP_BEFORE_END)
                {
                    m_CurrentRowStr.erase(strLen - 1);
                }
            }
            else
            {
                m_CurrentRowStr.append(m_WorkBufferPtr, endPtr - m_WorkBufferPtr);
            }
        }

        quoteCount += std::count(m_WorkBufferPtr, endPtr, QUOTE);
        if (delimPtr != 0)
        {
            m_WorkBufferPtr = delimPtr + 1;

            // In Excel style CSV, quote characters are escaped by doubling them
            // up.  Therefore, if what we've read of a record up to now contains
            // an odd number of quote characters then we need to read more.
            if ((quoteCount % 2) == 0)
            {
                break;
            }
            m_CurrentRowStr += RECORD_END;
        }
        else
        {
            m_WorkBufferPtr = m_WorkBufferEnd;
        }
    }

    // Ensure that m_WorkField is big enough to hold the entire record, even if
    // it turns out to be a single field - this avoids the need to check if it's
    // bit enough when it's populated (unlike std::vector or std::string)
    size_t minCapacity(m_CurrentRowStr.length() + 1);
    if (m_WorkFieldCapacity < minCapacity)
    {
        m_WorkFieldCapacity = minCapacity;
        m_WorkField.reset(new char[minCapacity]);
        m_WorkFieldEnd = m_WorkField.get();
    }

    return true;
}

bool CCsvInputParser::parseFieldNames(void)
{
    LOG_TRACE("Parse field names");

    m_FieldNameStr.clear();
    TStrVec &fieldNames = this->fieldNames();
    fieldNames.clear();

    const char *current(m_CurrentRowStr.data());
    const char *end(current + m_CurrentRowStr.length());
    while (current != end)
    {
        if (this->parseNextCsvToken(end, current) == false)
        {
            LOG_ERROR("Failed to get next CSV token");
            return false;
        }

        fieldNames.resize(fieldNames.size() + 1);
        size_t len(m_WorkFieldEnd - m_WorkField.get());
        fieldNames.back().assign(m_WorkField.get(), len);
    }

    if (fieldNames.empty())
    {
        // Don't scare the user with error messages if we've just received an
        // empty input
        if (m_NoMoreRecords)
        {
            LOG_DEBUG("Received input with settings only");
        }
        else
        {
            LOG_ERROR("No field names found in:" << core_t::LINE_ENDING << m_CurrentRowStr);
        }
        return false;
    }

    m_FieldNameStr = m_CurrentRowStr;
    this->gotFieldNames(true);

    LOG_TRACE("Field names " << m_FieldNameStr);

    return true;
}

bool CCsvInputParser::parseDataRecord(const TStrRefVec &fieldValRefs)
{
    const char *current(m_CurrentRowStr.data());
    const char *end(current + m_CurrentRowStr.length());
    for (TStrRefVecCItr iter = fieldValRefs.begin();
         iter != fieldValRefs.end();
         ++iter)
    {
        if (this->parseNextCsvToken(end, current) == false)
        {
            LOG_ERROR("Failed to get next CSV token");
            return false;
        }

        // Splunk can leave spurious carriage returns at the end of fields - see
        // http://splunk-base.splunk.com/answers/31034/why-is-there-a-carriage-return-appended-to-the-wmi-account_name-field
        // - so check for this and remove if found
        size_t len(m_WorkFieldEnd - m_WorkField.get());
        if (len > 0 && *(m_WorkFieldEnd - 1) == STRIP_BEFORE_END)
        {
            --len;
        }

        iter->get().assign(m_WorkField.get(), len);
    }

    if (current != end)
    {
        size_t extra(0);
        while (this->parseNextCsvToken(end, current) == true)
        {
            ++extra;
        }
        LOG_ERROR("Data record contains " << extra << " more fields than header:" << core_t::LINE_ENDING
           << m_CurrentRowStr << core_t::LINE_ENDING << "and:" << core_t::LINE_ENDING << m_FieldNameStr);
        return false;
    }

    this->gotData(true);

    return true;
}

bool CCsvInputParser::parseNextCsvToken(const char *end,
                                        const char *&current)
{
    m_WorkFieldEnd = m_WorkField.get();

    if (current == end)
    {
        // Allow one empty token at the end of a line
        if (!m_SeparatorAfterLastField)
        {
            LOG_ERROR("Trying to read too many fields from record:" << core_t::LINE_ENDING <<
                      m_CurrentRowStr);
            return false;
        }
        m_SeparatorAfterLastField = false;
        return true;
    }

    bool insideQuotes(false);
    do
    {
        if (insideQuotes)
        {
            if (*current == QUOTE)
            {
                // We need to look at the character after the quote
                ++current;
                if (current == end)
                {
                    m_SeparatorAfterLastField = false;
                    return true;
                }

                // The quoting state needs to be reversed UNLESS there are two
                // adjacent quotes
                if (*current != QUOTE)
                {
                    insideQuotes = false;

                    // Cater for the case where the character after the quote is
                    // the separator
                    if (*current == m_Separator)
                    {
                        ++current;
                        m_SeparatorAfterLastField = true;
                        return true;
                    }
                }
            }

            *(m_WorkFieldEnd++) = *current;
        }
        else
        {
            if (*current == m_Separator)
            {
                ++current;
                m_SeparatorAfterLastField = true;
                return true;
            }

            if (*current == QUOTE)
            {
                // We're not currently inside quotes so a quote puts us inside
                // quotes regardless of the next character, and we never want to
                // include this quote in the field value
                insideQuotes = true;
            }
            else
            {
                *(m_WorkFieldEnd++) = *current;
            }
        }
    }
    while (++current != end);

    m_SeparatorAfterLastField = false;

    // Inconsistency if the last character of the string is an unmatched quote
    if (insideQuotes)
    {
        LOG_ERROR("Unmatched final quote in record:" << core_t::LINE_ENDING <<
                  m_CurrentRowStr);
        return false;
    }

    return true;
}

std::istream &CCsvInputParser::getline(std::string &str)
{
    if (std::getline(m_StrmIn, str, RECORD_END))
    {
        size_t strLen(str.length());
        if (strLen > 0 && str[strLen - 1] == STRIP_BEFORE_END)
        {
            str.erase(strLen - 1);
        }
    }

    return m_StrmIn;
}


}
}

