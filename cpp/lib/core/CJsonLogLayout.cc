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
#include <core/CJsonLogLayout.h>

#include <core/CProcess.h>

#include <log4cxx/helpers/optionconverter.h>
#include <log4cxx/helpers/stringhelper.h>
#include <log4cxx/helpers/transcoder.h>
#include <log4cxx/level.h>
#include <log4cxx/logstring.h>
#include <log4cxx/ndc.h>
#include <log4cxx/spi/location/locationinfo.h>
#include <log4cxx/spi/loggingevent.h>

#include <rapidjson/stringbuffer.h>
#include <rapidjson/writer.h>

#include <string>

#include <stdint.h>


namespace
{
const std::string LOGGER_NAME("logger");
const std::string TIMESTAMP_NAME("timestamp");
const std::string LEVEL_NAME("level");
const std::string PID_NAME("pid");
// Cast this to int64_t as the type varies between int32_t and uint32_t on
// different platforms and int64_t covers both
const int64_t     PID(static_cast<int64_t>(prelert::core::CProcess::instance().id()));
const std::string THREAD_NAME("thread");
const std::string MESSAGE_NAME("message");
const std::string NDC_NAME("ndc");
const std::string CLASS_NAME("class");
const std::string METHOD_NAME("method");
const std::string FILE_NAME("file");
const std::string LINE_NAME("line");
const std::string PROPERTIES_NAME("properties");
}

// NB: log4cxx extensions have to go in the log4cxx namespace, hence cannot
// stick to the convention of our code being in the prelert namespace.  Worse,
// certain log4cxx macros require using directives rather than nesting the
// method definitions in the namespaces!
using namespace log4cxx;
using namespace log4cxx::helpers;

IMPLEMENT_LOG4CXX_OBJECT(CJsonLogLayout)

CJsonLogLayout::CJsonLogLayout(void)
    : m_LocationInfo(true),
      m_Properties(false)
{
}

void CJsonLogLayout::locationInfo(bool locationInfo)
{
    m_LocationInfo = locationInfo;
}

bool CJsonLogLayout::locationInfo(void) const
{
    return m_LocationInfo;
}

void CJsonLogLayout::properties(bool properties)
{
    m_Properties = properties;
}

bool CJsonLogLayout::properties(void) const
{
    return m_Properties;
}

void CJsonLogLayout::activateOptions(Pool &/*p*/)
{
    // NO-OP
}

void CJsonLogLayout::setOption(const LogString &option,
                               const LogString &value)
{
    if (StringHelper::equalsIgnoreCase(option,
                                       LOG4CXX_STR("LOCATIONINFO"),
                                       LOG4CXX_STR("locationinfo")))
    {
        this->locationInfo(OptionConverter::toBoolean(value, false));
    }
    if (StringHelper::equalsIgnoreCase(option,
                                       LOG4CXX_STR("PROPERTIES"),
                                       LOG4CXX_STR("properties")))
    {
        this->properties(OptionConverter::toBoolean(value, false));
    }
}

void CJsonLogLayout::format(LogString &output,
                            const spi::LoggingEventPtr &event,
                            Pool &/*p*/) const
{
    typedef rapidjson::Writer<rapidjson::StringBuffer> TStringBufferWriter;
    rapidjson::StringBuffer buffer;
    TStringBufferWriter writer(buffer);

    writer.StartObject();

    writer.String(LOGGER_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(LOGGER_NAME.length()));
    LOG4CXX_ENCODE_CHAR(logger, event->getLoggerName());
    writer.String(logger.c_str(),
                  static_cast<rapidjson::SizeType>(logger.length()));

    writer.String(TIMESTAMP_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(TIMESTAMP_NAME.length()));
    writer.Int64(event->getTimeStamp() / 1000);

    writer.String(LEVEL_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(LEVEL_NAME.length()));
    LOG4CXX_ENCODE_CHAR(level, event->getLevel()->toString());
    writer.String(level.c_str(),
                  static_cast<rapidjson::SizeType>(level.length()));

    writer.String(PID_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(PID_NAME.length()));
    writer.Int64(PID);

    writer.String(THREAD_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(THREAD_NAME.length()));
    LOG4CXX_ENCODE_CHAR(thread, event->getThreadName());
    writer.String(thread.c_str(),
                  static_cast<rapidjson::SizeType>(thread.length()));

    writer.String(MESSAGE_NAME.c_str(),
                  static_cast<rapidjson::SizeType>(MESSAGE_NAME.length()));
    LOG4CXX_ENCODE_CHAR(message, event->getRenderedMessage());
    writer.String(message.c_str(),
                  static_cast<rapidjson::SizeType>(message.length()));

    LogString logNdc;
    if (event->getNDC(logNdc))
    {
        writer.String(NDC_NAME.c_str(),
                      static_cast<rapidjson::SizeType>(NDC_NAME.length()));
        LOG4CXX_ENCODE_CHAR(ndc, logNdc);
        writer.String(ndc.c_str(),
                      static_cast<rapidjson::SizeType>(ndc.length()));
    }

    if (m_LocationInfo)
    {
        const spi::LocationInfo &locInfo = event->getLocationInformation();

        const std::string &className = locInfo.getClassName();
        if (!className.empty())
        {
            writer.String(CLASS_NAME.c_str(),
                          static_cast<rapidjson::SizeType>(CLASS_NAME.length()));
            writer.String(className.c_str(),
                          static_cast<rapidjson::SizeType>(className.length()));
        }

        const std::string &methodName = locInfo.getMethodName();
        if (!methodName.empty())
        {
            writer.String(METHOD_NAME.c_str(),
                          static_cast<rapidjson::SizeType>(METHOD_NAME.length()));
            writer.String(methodName.c_str(),
                          static_cast<rapidjson::SizeType>(methodName.length()));
        }

        writer.String(FILE_NAME.c_str(),
                      static_cast<rapidjson::SizeType>(FILE_NAME.length()));
        writer.String(locInfo.getFileName());

        writer.String(LINE_NAME.c_str(),
                      static_cast<rapidjson::SizeType>(LINE_NAME.length()));
        writer.Int(locInfo.getLineNumber());
    }

    if (m_Properties)
    {
        const spi::LoggingEvent::KeySet &propertySet = event->getPropertyKeySet();
        const spi::LoggingEvent::KeySet &keySet = event->getMDCKeySet();
        if (!(keySet.empty() && propertySet.empty()))
        {
            writer.String(PROPERTIES_NAME.c_str(),
                          static_cast<rapidjson::SizeType>(PROPERTIES_NAME.length()));
            writer.StartObject();

            for (spi::LoggingEvent::KeySet::const_iterator i = keySet.begin();
                 i != keySet.end();
                 ++i)
            {
                const LogString &key = *i;
                LogString value;
                if (event->getMDC(key, value))
                {
                    LOG4CXX_ENCODE_CHAR(name, key);
                    writer.String(name.c_str(),
                                  static_cast<rapidjson::SizeType>(name.length()));
                    LOG4CXX_ENCODE_CHAR(val, value);
                    writer.String(val.c_str(),
                                  static_cast<rapidjson::SizeType>(val.length()));
                }
            }
            for (spi::LoggingEvent::KeySet::const_iterator i = propertySet.begin();
                 i != propertySet.end();
                 ++i)
            {
                const LogString &key = *i;
                LogString value;
                if (event->getProperty(key, value))
                {
                    LOG4CXX_ENCODE_CHAR(name, key);
                    writer.String(name.c_str(),
                                  static_cast<rapidjson::SizeType>(name.length()));
                    LOG4CXX_ENCODE_CHAR(val, value);
                    writer.String(val.c_str(),
                                  static_cast<rapidjson::SizeType>(val.length()));
                }
            }

            writer.EndObject();
        }
    }

    writer.EndObject();

    output.append(LOG4CXX_STR(buffer.GetString()));
    output.append(LOG4CXX_EOL);
}

bool CJsonLogLayout::ignoresThrowable(void) const
{
    return false;
}

