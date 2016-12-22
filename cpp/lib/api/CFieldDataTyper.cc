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
#include <api/CFieldDataTyper.h>

#include <core/CDataAdder.h>
#include <core/CDataSearcher.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CJsonStatePersistInserter.h>
#include <core/CJsonStateRestoreTraverser.h>
#include <core/CLogger.h>
#include <core/CStateCompressor.h>
#include <core/CStateDecompressor.h>
#include <core/CStringUtils.h>

#include <api/CFieldConfig.h>
#include <api/CJsonOutputWriter.h>
#include <api/COutputHandler.h>
#include <api/CTokenListReverseSearchCreator.h>

#include <boost/bind.hpp>

#include <sstream>


namespace prelert
{
namespace api
{

namespace
{

const std::string VERSION_TAG("a");
const std::string TYPER_TAG("b");
const std::string EXAMPLES_COLLECTOR_TAG("c");

} // unnamed

// Initialise statics
const std::string   CFieldDataTyper::ML_STATE_INDEX(".ml-state");
const std::string   CFieldDataTyper::PRELERTCATEGORY_NAME("prelertcategory");
const double        CFieldDataTyper::SIMILARITY_THRESHOLD(0.7);
const std::string   CFieldDataTyper::STATE_TYPE("categorizer_state");
const std::string   CFieldDataTyper::STATE_VERSION("1");


CFieldDataTyper::CFieldDataTyper(const std::string &jobId,
                                 const CFieldConfig &config,
                                 const model::CLimits &limits,
                                 COutputHandler &outputHandler,
                                 CJsonOutputWriter &jsonOutputWriter)
    : m_JobId(jobId),
      m_OutputHandler(outputHandler),
      m_ExtraFieldNames(1, PRELERTCATEGORY_NAME),
      m_WriteFieldNames(true),
      m_NumRecordsHandled(0),
      m_OutputFieldCategory(m_Overrides[PRELERTCATEGORY_NAME]),
      m_MaxMatchingLength(0),
      m_JsonOutputWriter(jsonOutputWriter),
      m_ExamplesCollector(limits.maxExamples()),
      m_CategorizationFieldName(config.categorizationFieldName()),
      m_CategorizationFilter()
{
    this->createTyper(m_CategorizationFieldName);

    LOG_INFO("Configuring categorization filtering");
    m_CategorizationFilter.configure(config.categorizationFilters());
}

CFieldDataTyper::~CFieldDataTyper(void)
{
    m_DataTyper->dumpStats();
}

void CFieldDataTyper::newOutputStream(void)
{
    m_WriteFieldNames = true;
    m_OutputHandler.newOutputStream();
}

bool CFieldDataTyper::handleSettings(const TStrStrUMap &settings)
{
    // Pass on the settings in case we're chained
    return m_OutputHandler.settings(settings);
}

bool CFieldDataTyper::handleRecord(bool isDryRun,
                                   const TStrVec &fieldNames,
                                   const TStrStrUMap &dataRowFields)
{
    // First time through we output the field names
    if (m_WriteFieldNames)
    {
        if (m_OutputHandler.fieldNames(fieldNames, m_ExtraFieldNames) == false)
        {
            LOG_ERROR("Unable to set field names for output:" << core_t::LINE_ENDING <<
                      this->debugPrintRecord(fieldNames, dataRowFields));
            return false;
        }
        m_WriteFieldNames = false;
    }

    m_OutputFieldCategory = core::CStringUtils::typeToString(this->computeType(fieldNames, dataRowFields));

    if (m_OutputHandler.writeRow(isDryRun, dataRowFields, m_Overrides) == false)
    {
        LOG_ERROR("Unable to write output with type " <<
                  m_OutputFieldCategory << " for input:" << core_t::LINE_ENDING <<
                  this->debugPrintRecord(fieldNames, dataRowFields));
        return false;
    }
    ++m_NumRecordsHandled;
    return true;
}

void CFieldDataTyper::finalise(void)
{
    // Pass on the request in case we're chained
    m_OutputHandler.finalise();
}

uint64_t CFieldDataTyper::numRecordsHandled(void) const
{
    return m_NumRecordsHandled;
}

COutputHandler &CFieldDataTyper::outputHandler(void)
{
    return m_OutputHandler;
}

int CFieldDataTyper::computeType(const TStrVec &fieldNames, const TStrStrUMap &dataRowFields)
{
    const std::string &categorizationFieldName = m_DataTyper->fieldName();
    TStrStrUMapCItr fieldIter = dataRowFields.find(categorizationFieldName);
    if (fieldIter == dataRowFields.end())
    {
        LOG_WARN("Assigning type -1 to record with no " <<
                 categorizationFieldName << " field:" << core_t::LINE_ENDING <<
                 this->debugPrintRecord(fieldNames, dataRowFields));
        return -1;
    }

    const std::string &fieldValue = fieldIter->second;
    if (fieldValue.empty())
    {
        LOG_WARN("Assigning type -1 to record with blank " <<
                 categorizationFieldName << " field:" << core_t::LINE_ENDING <<
                 this->debugPrintRecord(fieldNames, dataRowFields));
        return -1;
    }

    int type = -1;
    if (m_CategorizationFilter.empty())
    {
        type = m_DataTyper->computeType(false, fieldValue, fieldValue.length());
    }
    else
    {
        std::string filtered = m_CategorizationFilter.apply(fieldValue);
        type = m_DataTyper->computeType(false, filtered, fieldValue.length());
    }
    if (type < 1)
    {
        return -1;
    }

    bool exampleAdded = m_ExamplesCollector.add(static_cast<std::size_t>(type), fieldValue);
    bool searchTermsChanged = this->createReverseSearch(type);
    if (exampleAdded || searchTermsChanged)
    {
        const TStrSet &examples = m_ExamplesCollector.examples(static_cast<std::size_t>(type));
        m_JsonOutputWriter.writeCategoryDefinition(type,
                                                   m_SearchTerms,
                                                   m_SearchTermsRegex,
                                                   m_MaxMatchingLength,
                                                   examples);
    }

    return type;
}

void CFieldDataTyper::createTyper(const std::string &fieldName)
{
    // TODO - if we ever have more than one data typer class, this should be
    // replaced with a factory
    TTokenListDataTyperKeepsFields::TTokenListReverseSearchCreatorIntfCPtr
            reverseSearchCreator(new CTokenListReverseSearchCreator(fieldName));
    m_DataTyper.reset(new TTokenListDataTyperKeepsFields(reverseSearchCreator,
                                                         SIMILARITY_THRESHOLD,
                                                         fieldName));

    LOG_TRACE("Created new data typer for field '" << fieldName << "'");
}

bool CFieldDataTyper::createReverseSearch(int type)
{
    bool wasCached(false);
    if (m_DataTyper->createReverseSearch(core::CDataSearcher::EMPTY_STRING,
                                         type,
                                         m_SearchTerms,
                                         m_SearchTermsRegex,
                                         m_MaxMatchingLength,
                                         wasCached) == false)
    {
        m_SearchTerms.clear();
        m_SearchTermsRegex.clear();
    }
    return !wasCached;
}

bool CFieldDataTyper::restoreState(core::CDataSearcher &restoreSearcher,
                                   core_t::TTime &completeToTime)
{
    // Pass on the request in case we're chained
    if (this->outputHandler().restoreState(restoreSearcher,
                                           completeToTime) == false)
    {
        return false;
    }

    LOG_DEBUG("Restore typer state");

    try
    {
        // Restore from Elasticsearch compressed data
        core::CStateDecompressor decompressor(restoreSearcher);
        decompressor.setStateRestoreSearch(ML_STATE_INDEX, STATE_TYPE);

        core::CDataSearcher::TIStreamP strm(decompressor.search(1, 1));
        if (strm == 0)
        {
            LOG_ERROR("Unable to connect to data store");
            return false;
        }

        if (strm->bad())
        {
            LOG_ERROR("State restoration search returned bad stream");
            return false;
        }

        if (strm->fail())
        {
            // This is not fatal - we just didn't find the given document number
            return true;
        }

        // We're dealing with streaming JSON state
        core::CJsonStateRestoreTraverser traverser(*strm);

        if (this->acceptRestoreTraverser(traverser) == false)
        {
            LOG_ERROR("JSON restore failed");
            return false;
        }
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Failed to restore state! " << e.what());
        // This is fatal in terms of the categorizer we attempted to restore,
        // but returning false here can throw the system into a repeated cycle
        // of failure.  It's better to reset the categorizer and re-categorize from
        // scratch.
        this->resetAfterCorruptRestore();
        return true;
    }

    return true;
}

bool CFieldDataTyper::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    if (traverser.name() == VERSION_TAG)
    {
        std::string version;
        if (core::CStringUtils::stringToType(traverser.value(), version) == false)
        {
            LOG_ERROR("Cannot restore categorizer, invalid version: " << traverser.value());
            return false;
        }
        if (version != STATE_VERSION)
        {
            LOG_DEBUG("Categorizer has not been restored as the version has changed");
            return true;
        }
    }
    else
    {
        LOG_ERROR("Cannot restore categorizer - " << VERSION_TAG
           << " element expected but found "
           << traverser.name() << '=' << traverser.value());
        return false;
    }

    if (traverser.next() == false)
    {
        LOG_ERROR("Cannot restore categorizer - end of object reached when "
           << TYPER_TAG << " was expected");
        return false;
    }

    if (traverser.name() == TYPER_TAG)
    {
        if (traverser.traverseSubLevel(boost::bind(
                &CDataTyper::acceptRestoreTraverser, m_DataTyper, _1)) == false)
        {
            LOG_ERROR("Cannot restore categorizer, unexpected element: " << traverser.value());
            return false;
        }
    }
    else
    {
        LOG_ERROR("Cannot restore categorizer - " << TYPER_TAG
           << " element expected but found "
           << traverser.name() << '=' << traverser.value());
        return false;
    }

    if (traverser.next() == false)
    {
        LOG_ERROR("Cannot restore categorizer - end of object reached when "
           << EXAMPLES_COLLECTOR_TAG << " was expected");
        return false;
    }

    if (traverser.name() == EXAMPLES_COLLECTOR_TAG)
    {
        if (traverser.traverseSubLevel(boost::bind(&CCategoryExamplesCollector::acceptRestoreTraverser,
                                                   boost::ref(m_ExamplesCollector),
                                                   _1)) == false ||
            traverser.haveBadState())
        {
            LOG_ERROR("Cannot restore categorizer, unexpected element: " << traverser.value());
            return false;
        }
    }
    else
    {
        LOG_ERROR("Cannot restore categorizer - " << EXAMPLES_COLLECTOR_TAG
           << " element expected but found "
           << traverser.name() << '=' << traverser.value());
        return false;
    }

    return true;
}

bool CFieldDataTyper::persistState(core::CDataAdder &persister)
{
    LOG_DEBUG("Persist typer state");

    // Pass on the request in case we're chained
    if (this->outputHandler().persistState(persister) == false)
    {
        return false;
    }

    return this->doPersistState(persister);
}

bool CFieldDataTyper::doPersistState(core::CDataAdder &persister)
{
    try
    {
        core::CStateCompressor compressor(persister);

        core::CDataAdder::TOStreamP
            strm = compressor.addStreamed(ML_STATE_INDEX, STATE_TYPE, m_JobId);

        if (strm == 0)
        {
            LOG_ERROR("Failed to create persistence stream");
            return false;
        }

        if (!strm->good())
        {
            LOG_ERROR("Persistence stream is bad before stream of "
                      "state for the categorizer");
            return false;
        }

        {
            // Keep the JSON inserter scoped as it only finishes the stream
            // when it is desctructed
            core::CJsonStatePersistInserter inserter(*strm);
            this->acceptPersistInserter(inserter);
        }

        if (strm->bad())
        {
            LOG_ERROR("Persistence stream went bad during stream of "
                      "state for the categorizer");
            return false;
        }

        if (compressor.streamComplete(strm, true) == false || strm->bad())
        {
            LOG_ERROR("Failed to complete last persistence stream");
            return false;
        }
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Failed to persist state! " << e.what());
        return false;
    }
    return true;
}

void CFieldDataTyper::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(VERSION_TAG, STATE_VERSION);
    inserter.insertLevel(TYPER_TAG,
            boost::bind(&CDataTyper::acceptPersistInserter, m_DataTyper, _1));
    inserter.insertLevel(EXAMPLES_COLLECTOR_TAG, boost::bind(
            &CCategoryExamplesCollector::acceptPersistInserter,
            boost::cref(m_ExamplesCollector),
            _1));
}

bool CFieldDataTyper::periodicPersistState(core::CDataAdder &persister)
{
    LOG_DEBUG("Periodic persist typer state");

    // Pass on the request in case we're chained
    if (this->outputHandler().periodicPersistState(persister) == false)
    {
        return false;
    }

    return this->doPersistState(persister);
}

void CFieldDataTyper::resetAfterCorruptRestore(void)
{
    LOG_WARN("Discarding corrupt categorizer state - will re-categorize from scratch");

    m_SearchTerms.clear();
    m_SearchTermsRegex.clear();
    this->createTyper(m_CategorizationFieldName);
    m_ExamplesCollector.clear();
}


}
}

