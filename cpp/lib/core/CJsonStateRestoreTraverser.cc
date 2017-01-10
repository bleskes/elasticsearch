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
#include <core/CJsonStateRestoreTraverser.h>

#include <core/CLogger.h>
#include <core/CStringUtils.h>

#include <rapidjson/rapidjson.h>


namespace ml
{
namespace core
{

namespace
{
const std::string EMPTY_STRING;
}


CJsonStateRestoreTraverser::CJsonStateRestoreTraverser(std::istream &inputStream)
    : m_ReadStream(inputStream),
      m_Started(false),
      m_ExpectName(true),
      m_DesiredLevel(0),
      m_NextIndex(0),
      m_IsArrayOfObjects(false)
{
    m_Level[0] = 0;
    m_Level[1] = 0;
    m_IsEndOfLevel[0] = false;
    m_IsEndOfLevel[1] = false;
}

bool CJsonStateRestoreTraverser::isEof(void) const
{
    // Rapid JSON pull reader returns \0 when it reaches EOF
    return m_ReadStream.Peek() == '\0';
}

bool CJsonStateRestoreTraverser::next(void)
{
    if (!m_Started)
    {
        if (this->start() == false)
        {
            return false;
        }
    }

    if (this->nextIsEndOfLevel())
    {
        return false;
    }

    if (this->nextLevel() == m_DesiredLevel ||
        (this->currentLevel() == m_DesiredLevel && this->nextLevel() == m_DesiredLevel + 1))
    {
        return this->advance();
    }

    // If we get here then we're skipping over a nested object that's not of
    // interest
    while (this->nextLevel() > m_DesiredLevel)
    {
        if (this->advance() == false)
        {
            return false;
        }
    }

    if (this->nextLevel() == m_DesiredLevel)
    {
        return this->advance() && !this->nextIsEndOfLevel();
    }

    return false;
}

bool CJsonStateRestoreTraverser::nextObject(void)
{
    if (!m_IsArrayOfObjects)
    {
        return false;
    }

    // Advance to the next start object token
    bool ok = this->advance(true) && this->advance();
    ok = ok && this->next();

    return ok;
}

bool CJsonStateRestoreTraverser::hasSubLevel(void) const
{
    if (!m_Started)
    {
        if (const_cast<CJsonStateRestoreTraverser *>(this)->start() == false)
        {
            return false;
        }
    }

    return this->currentLevel() == 1 + m_DesiredLevel;
}

const std::string &CJsonStateRestoreTraverser::name(void) const
{
    if (!m_Started)
    {
        if (const_cast<CJsonStateRestoreTraverser *>(this)->start() == false)
        {
            return EMPTY_STRING;
        }
    }

    return this->currentName();
}

const std::string &CJsonStateRestoreTraverser::value(void) const
{
    if (!m_Started)
    {
        if (const_cast<CJsonStateRestoreTraverser *>(this)->start() == false)
        {
            return EMPTY_STRING;
        }
    }

    return this->currentValue();
}

bool CJsonStateRestoreTraverser::descend(void)
{
    if (!m_Started)
    {
        if (this->start() == false)
        {
            return false;
        }
    }

    if (this->currentLevel() != 1 + m_DesiredLevel)
    {
        return false;
    }

    ++m_DesiredLevel;

    // Don't advance if the next level has no elements.  Instead set the current
    // element to be completely empty so that the sub-level traverser will find
    // nothing and then ascend.
    if (this->nextIsEndOfLevel())
    {
        m_Name[1 - m_NextIndex].clear();
        m_Value[1 - m_NextIndex].clear();
        return true;
    }

    return this->advance();
}

bool CJsonStateRestoreTraverser::ascend(void)
{
    // If we're trying to ascend above the root level then something has gone
    // wrong
    if (m_DesiredLevel == 0)
    {
        LOG_ERROR("Inconsistency - trying to ascend above JSON root");
        return false;
    }

    --m_DesiredLevel;

    while (this->nextLevel() > m_DesiredLevel)
    {
        if (this->advance() == false)
        {
            return false;
        }
    }

    // This will advance onto the end-of-level marker.  Slightly unintuitively
    // it's then still necessary to call next() to move to the higher level.
    // This is to match the functionality of the pre-existing XML state
    // traverser.
    return this->advance();
}

void CJsonStateRestoreTraverser::debug(void) const
{
    LOG_DEBUG("Current: name = " << this->currentName() <<
              " value = " << this->currentValue() <<
              " level = " << this->currentLevel() <<
              ", Next: name = " << this->nextName() <<
              " value = " << this->nextValue() <<
              " level = " << this->nextLevel() <<
              " is array of objects = " << m_IsArrayOfObjects);
}

size_t CJsonStateRestoreTraverser::currentLevel(void) const
{
    return m_Level[1 - m_NextIndex];
}

bool CJsonStateRestoreTraverser::currentIsEndOfLevel(void) const
{
    return m_IsEndOfLevel[1 - m_NextIndex];
}

const std::string &CJsonStateRestoreTraverser::currentName(void) const
{
    return m_Name[1 - m_NextIndex];
}

const std::string &CJsonStateRestoreTraverser::currentValue(void) const
{
    return m_Value[1 - m_NextIndex];
}

size_t CJsonStateRestoreTraverser::nextLevel(void) const
{
    return m_Level[m_NextIndex];
}

bool CJsonStateRestoreTraverser::nextIsEndOfLevel(void) const
{
    return m_IsEndOfLevel[m_NextIndex];
}

const std::string &CJsonStateRestoreTraverser::nextName(void) const
{
    return m_Name[m_NextIndex];
}

const std::string &CJsonStateRestoreTraverser::nextValue(void) const
{
    return m_Value[m_NextIndex];
}

bool CJsonStateRestoreTraverser::start(void)
{
    m_Started = true;

    rapidjson::Token token;
    if (m_Reader.ParseFirst<rapidjson::kParseDefaultFlags>(m_ReadStream,
                                                           token) == false)
    {
        this->logError();
        return false;
    }

    // If the first token is start of array then this could be
    // an array of docs. Next should be start object
    if (token.type == rapidjson::kTokenArrayStart)
    {
        if (m_Reader.ParseNext<rapidjson::kParseDefaultFlags>(m_ReadStream,
                                                              token) == false)
        {
            this->logError();
            return false;
        }

        m_IsArrayOfObjects = true;
    }

    // For Ml state the first token should be the start of a JSON
    // object, but we don't store it
    if (token.type != rapidjson::kTokenObjectStart)
    {
        if (m_IsArrayOfObjects &&
            token.type == rapidjson::kTokenArrayEnd &&
            this->isEof())
        {
            LOG_DEBUG("JSON document is an empty array");
            return false;
        }

        LOG_ERROR("JSON state must be object at root" << token.type);
        return false;
    }

    // Advance twice to prime the current and next elements
    return this->advance() && this->advance();
}

bool CJsonStateRestoreTraverser::advance(bool stopAtStartOfObject)
{
    // We skip everything inside arrays, so will loop if we encounter one
    size_t           arrayDepth(0);
    bool             keepGoing(true);
    rapidjson::Token token;
    while (keepGoing)
    {
        if (m_Reader.ParseNext<rapidjson::kParseDefaultFlags>(m_ReadStream,
                                                              token) == false)
        {
            if (!this->isEof())
            {
                this->logError();
            }
            return false;
        }

        if (token.type == rapidjson::kTokenString)
        {
            // Ignore strings inside arrays
            if (arrayDepth == 0)
            {
                if (m_ExpectName)
                {
                    m_NextIndex = 1 - m_NextIndex;
                    m_Level[m_NextIndex] = m_Level[1 - m_NextIndex];
                    m_IsEndOfLevel[m_NextIndex] = false;
                    m_Name[m_NextIndex].assign(token.str, token.length);
                }
                else
                {
                    m_Value[m_NextIndex].assign(token.str, token.length);
                    keepGoing = stopAtStartOfObject;
                }

                m_ExpectName = !m_ExpectName;
            }
        }
        else if (token.type == rapidjson::kTokenDouble)
        {
            // Ignore numbers inside arrays

            if (arrayDepth == 0)
            {
                if (m_ExpectName)
                {
                    LOG_DEBUG("Error expected a string name found a double");
                }
                else
                {
                    m_Value[m_NextIndex].assign(CStringUtils::typeToString(token.d));
                    keepGoing = stopAtStartOfObject;
                }

                m_ExpectName = true;
            }
        }
        else if (token.type == rapidjson::kTokenUInt)
        {
            // Ignore numbers inside arrays
            if (arrayDepth == 0)
            {
                if (m_ExpectName)
                {
                    LOG_DEBUG("Error expected a string name found a unsigned int");
                }
                else
                {
                    m_Value[m_NextIndex].assign(CStringUtils::typeToString(token.u));
                    keepGoing = stopAtStartOfObject;
                }

                m_ExpectName = true;
            }
        }
        else if (token.type == rapidjson::kTokenBool)
        {
            // Ignore values inside arrays
            if (arrayDepth == 0)
            {
                if (m_ExpectName)
                {
                    LOG_DEBUG("Error expected a string name found a boolean");
                }
                else
                {
                    m_Value[m_NextIndex].assign(CStringUtils::typeToString(token.b));
                    keepGoing = stopAtStartOfObject;
                }

                m_ExpectName = true;
            }
        }
        else if (token.type == rapidjson::kTokenObjectStart)
        {
            // Ignore objects inside arrays
            if (arrayDepth == 0)
            {
                ++m_Level[m_NextIndex];
                m_Value[m_NextIndex].clear();
                m_ExpectName = true;
                keepGoing = false;
            }
        }
        else if (token.type == rapidjson::kTokenObjectEnd)
        {
            // Ignore objects inside arrays
            if (arrayDepth == 0)
            {
                m_NextIndex = 1 - m_NextIndex;
                m_Level[m_NextIndex] = m_Level[1 - m_NextIndex] - 1;
                m_IsEndOfLevel[m_NextIndex] = true;
                m_Name[m_NextIndex].clear();
                m_Value[m_NextIndex].clear();
                m_ExpectName = true;
                keepGoing = stopAtStartOfObject;
            }
        }
        else if (token.type == rapidjson::kTokenArrayStart)
        {
            LOG_ERROR("JSON state should not contain arrays");
            ++arrayDepth;
        }
        else if (token.type == rapidjson::kTokenArrayEnd)
        {
            if (arrayDepth == 0)
            {
                if (!m_IsArrayOfObjects)
                {
                    LOG_ERROR("Inconsistency - array end encountered at array depth 0");
                }
                else
                {
                    LOG_TRACE("End array encountered in array of objects");
                }
            }
            else
            {
                --arrayDepth;
            }
        }
        else
        {
            LOG_DEBUG("Ignoring unsupported token type " << token.type <<
                      " in JSON state");
        }
    }

    return true;
}

void CJsonStateRestoreTraverser::logError(void)
{
    const char *error(m_Reader.GetParseError());
    LOG_ERROR("Error parsing JSON at offset " << m_Reader.GetErrorOffset() <<
              ": " << ((error != 0) ? error : "No message"));
    this->setBadState();
}


}
}

