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

#include "CStringStoreTest.h"
#include "CMockDataAdder.h"
#include "CMockSearcher.h"

#include <core/CLogger.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>
#include <model/CStringStore.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CHierarchicalResultsWriter.h>
#include <api/CJsonOutputWriter.h>

#include <sstream>

using namespace prelert;

namespace
{
//! \brief
//! Mock object for unit tests
//!
//! DESCRIPTION:\n
//! Mock object for gathering anomaly results.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Only the minimal set of required functions are implemented.
//!
class CMockOutputWriter : public prelert::api::CJsonOutputWriter
{
    public:
        typedef boost::tuple<prelert::core_t::TTime, double /* probability */,
            std::string /* byFieldName*/, std::string /* overFieldName */,
            std::string /* partitionFieldName */> TResultsTp;
        typedef std::vector<TResultsTp> TResultsVec;
        typedef TResultsVec::const_iterator TResultsVecCItr;

    public:
        //! Constructor
        CMockOutputWriter(void) :
            prelert::api::CJsonOutputWriter("job", m_Writer)
        { }

        //! Destructor
        virtual ~CMockOutputWriter(void)
        {
            // Finalise the Json writer so it doesn't try and write to
            // m_Writer which will have been destroyed first
            prelert::api::CJsonOutputWriter::finalise();
        }

        //! Override for handling the anomaly results
        virtual bool acceptResult(const prelert::api::CHierarchicalResultsWriter::SResults &results)
        {
            if (results.s_ByFieldName != "count")
            {
                LOG_DEBUG("AAAA got result! " << results.s_BucketStartTime << " " << results.s_ByFieldValue << " / " << results.s_PartitionFieldValue);
                m_Results.push_back(TResultsTp(results.s_BucketStartTime,
                                               results.s_Probability,
                                               results.s_ByFieldValue,
                                               results.s_OverFieldValue,
                                               results.s_PartitionFieldValue));
            }
            return true;
        }

        //! Accessor for the collection of results
        const TResultsVec &results(void) const
        {
            return m_Results;
        }

        std::string printInternal(void)
        {
            return m_Writer.str();
        }

    private:
        //! Dummy stream buffer to keep the CJsonOutputWriter happy
        std::ostringstream m_Writer;

        //! Collection of results received
        TResultsVec m_Results;
};

core_t::TTime playData(core_t::TTime start, core_t::TTime span, int numBuckets,
                  int numPeople, int numPartitions, int anomaly,
                  api::CAnomalyDetector &detector)
{
    std::string people[] = { "Elgar", "Holst", "Delius", "Vaughan Williams", "Bliss", "Warlock", "Walton" };
    if (numPeople > 7)
    {
        LOG_ERROR("Too many people: " << numPeople);
        return start;
    }
    std::string partitions[] = { "tuba", "flute", "violin", "triangle", "jew's harp" };
    if (numPartitions > 5)
    {
        LOG_ERROR("Too many partitions: " << numPartitions);
        return start;
    }
    std::stringstream ss;
    ss << "_time,notes,composer,instrument\n";
    core_t::TTime t;
    int bucketNum = 0;
    for (t = start; t < start + span * numBuckets; t += span, bucketNum++)
    {
        for (int i = 0; i < numPeople; i++)
        {
            for (int j = 0; j < numPartitions; j++)
            {
                ss << t << "," << (people[i].size() * partitions[j].size()) << ",";
                ss << people[i] << "," << partitions[j] << "\n";
            }
        }
        if (bucketNum == anomaly)
        {
            ss << t << "," << 5564 << "," << people[numPeople - 1] << "," << partitions[numPartitions - 1] << "\n";
        }
    }

    api::CCsvInputParser parser(ss);

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::bind(&api::CAnomalyDetector::handleSettings,
                                                 &detector,
                                                 _1),
                                     boost::bind(&api::CAnomalyDetector::handleRecord,
                                                 &detector,
                                                 _1,
                                                 _2,
                                                 _3)));

    return t;
}

//! Helper class to look up a string in TStrPtr set
struct SLookup
{
    std::size_t operator()(const std::string &key) const
    {
        boost::hash<std::string> hasher;
        return hasher(key);
    }

    bool operator()(const std::string &lhs,
                    const model::CStringStore::TStrPtr &rhs) const
    {
        return lhs == *rhs;
    }
};


} // namespace

bool CStringStoreTest::nameExists(const std::string &string)
{
    model::CStringStore::TStrPtrUSet names = model::CStringStore::names().m_Strings;
    return names.find(string,
                      ::SLookup(),
                      ::SLookup()) != names.end();
}

bool CStringStoreTest::influencerExists(const std::string &string)
{
    model::CStringStore::TStrPtrUSet names = model::CStringStore::influencers().m_Strings;
    return names.find(string,
                      ::SLookup(),
                      ::SLookup()) != names.end();
}

void CStringStoreTest::testPersonStringPruning(void)
{
    core_t::TTime BUCKET_SPAN(10000);
    core_t::TTime time = 100000000;

    api::CFieldConfig fieldConfig;
    api::CFieldConfig::TStrVec clause;
    clause.push_back("max(notes)");
    clause.push_back("by");
    clause.push_back("composer");
    clause.push_back("partitionfield=instrument");

    CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SPAN);
    modelConfig.bucketResultsDelay(2);

    model::CLimits limits;

    CMockDataAdder adder;
    CMockSearcher searcher(adder);

    LOG_DEBUG("Setting up detector");
    // Test that the stringstore entries are pruned correctly on persist/restore
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        LOG_TRACE("Setting up detector");
        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter);

        // There will be one anomaly in this batch, which will be stuck in the
        // results queue.

        time = playData(time, BUCKET_SPAN, 100, 3, 2, 99, detector);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), outputWriter.results().size());

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // "", "count", "max", "notes", "composer", "instrument", "Elgar", "Holst", "Delius", "flute", "tuba"
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("max"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        time += BUCKET_SPAN * 100;
        time = playData(time, BUCKET_SPAN, 100, 3, 2, 99, detector);

        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring detector");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // "", "count", "max", "notes", "composer", "instrument", "Elgar", "Holst", "Delius", "flute", "tuba"
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("max"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        // play some data in a lot later, to bring about pruning
        time += BUCKET_SPAN * 5000;
        time = playData(time, BUCKET_SPAN, 100, 3, 1, 101, detector);

        detector.finalise();
        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring detector again");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // While the 3 composers from the second partition should have been culled in the prune,
        // their names still exist in the first partition, so will still be in the string store
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("max"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        // Play some more data to cull out the third person
        time += BUCKET_SPAN * 5000;
        time = playData(time, BUCKET_SPAN, 100, 2, 2, 101, detector);

        detector.finalise();
        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring yet again");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // One composer should have been culled!
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("max"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));
        CPPUNIT_ASSERT(!this->nameExists("Delius"));
    }
}


void CStringStoreTest::testAttributeStringPruning(void)
{
    core_t::TTime BUCKET_SPAN(10000);
    core_t::TTime time = 100000000;

    api::CFieldConfig fieldConfig;
    api::CFieldConfig::TStrVec clause;
    clause.push_back("dc(notes)");
    clause.push_back("over");
    clause.push_back("composer");
    clause.push_back("partitionfield=instrument");

    CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SPAN);
    modelConfig.bucketResultsDelay(2);

    model::CLimits limits;

    CMockDataAdder adder;
    CMockSearcher searcher(adder);

    LOG_DEBUG("Setting up detector");
    // Test that the stringstore entries are pruned correctly on persist/restore
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        LOG_TRACE("Setting up detector");
        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter);

        // There will be one anomaly in this batch, which will be stuck in the
        // results queue.

        time = playData(time, BUCKET_SPAN, 100, 3, 2, 99, detector);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), outputWriter.results().size());

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // "", "count", "distinct_count", "notes", "composer", "instrument", "Elgar", "Holst", "Delius", "flute", "tuba"
        LOG_DEBUG(core::CContainerPrinter::print(model::CStringStore::names().m_Strings));
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("distinct_count"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        time += BUCKET_SPAN * 100;
        time = playData(time, BUCKET_SPAN, 100, 3, 2, 99, detector);

        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring detector");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // "", "count", "distinct_count", "notes", "composer", "instrument", "Elgar", "Holst", "Delius", "flute", "tuba"
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("distinct_count"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        // play some data in a lot later, to bring about pruning
        time += BUCKET_SPAN * 5000;
        time = playData(time, BUCKET_SPAN, 100, 3, 1, 101, detector);

        detector.finalise();
        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring detector again");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // While the 3 composers from the second partition should have been culled in the prune,
        // their names still exist in the first partition, so will still be in the string store
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("distinct_count"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("Delius"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));

        // Play some more data to cull out the third person
        time += BUCKET_SPAN * 5000;
        time = playData(time, BUCKET_SPAN, 100, 2, 2, 101, detector);

        detector.finalise();
        CPPUNIT_ASSERT(detector.persistState(adder));
    }
    LOG_DEBUG("Restoring yet again");
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter,
                                       api::CAnomalyDetector::TPersistCompleteFunc());

        core_t::TTime completeToTime(0);
        CPPUNIT_ASSERT(detector.restoreState(searcher, completeToTime));
        adder.clear();

        // No influencers in this configuration
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());

        // One composer should have been culled!
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("distinct_count"));
        CPPUNIT_ASSERT(this->nameExists("notes"));
        CPPUNIT_ASSERT(this->nameExists("composer"));
        CPPUNIT_ASSERT(this->nameExists("instrument"));
        CPPUNIT_ASSERT(this->nameExists("Elgar"));
        CPPUNIT_ASSERT(this->nameExists("Holst"));
        CPPUNIT_ASSERT(this->nameExists("flute"));
        CPPUNIT_ASSERT(this->nameExists("tuba"));
        CPPUNIT_ASSERT(!this->nameExists("Delius"));

    }
}


void CStringStoreTest::testInfluencerStringPruning(void)
{
    core_t::TTime BUCKET_SPAN(10000);
    core_t::TTime time = 100000000;

    api::CFieldConfig fieldConfig;
    api::CFieldConfig::TStrVec clause;
    clause.push_back("max(notes)");
    clause.push_back("influencerfield=instrument");
    clause.push_back("influencerfield=composer");

    CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SPAN);
    modelConfig.bucketResultsDelay(2);

    model::CLimits limits;

    CMockDataAdder adder;
    CMockSearcher searcher(adder);

    LOG_DEBUG("Setting up detector");
    // Test that the stringstore entries are pruned correctly on persist/restore
    {
        model::CStringStore::influencers().clearEverythingTestOnly();
        model::CStringStore::names().clearEverythingTestOnly();

        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::influencers().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model::CStringStore::names().m_Strings.size());

        LOG_TRACE("Setting up detector");
        ::CMockOutputWriter outputWriter;
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       outputWriter);

        // Play in a few buckets with influencers, and see that they stick around for
        // 3 buckets
        LOG_DEBUG("Running 20 buckets");
        time = playData(time, BUCKET_SPAN, 20, 7, 5, 99, detector);

        LOG_TRACE(core::CContainerPrinter::print(model::CStringStore::names().m_Strings));
        LOG_TRACE(core::CContainerPrinter::print(model::CStringStore::influencers().m_Strings));

        CPPUNIT_ASSERT(this->influencerExists("Delius"));
        CPPUNIT_ASSERT(this->influencerExists("Walton"));
        CPPUNIT_ASSERT(this->influencerExists("Holst"));
        CPPUNIT_ASSERT(this->influencerExists("Vaughan Williams"));
        CPPUNIT_ASSERT(this->influencerExists("Warlock"));
        CPPUNIT_ASSERT(this->influencerExists("Bliss"));
        CPPUNIT_ASSERT(this->influencerExists("Elgar"));
        CPPUNIT_ASSERT(this->influencerExists("flute"));
        CPPUNIT_ASSERT(this->influencerExists("tuba"));
        CPPUNIT_ASSERT(this->influencerExists("violin"));
        CPPUNIT_ASSERT(this->influencerExists("triangle"));
        CPPUNIT_ASSERT(this->influencerExists("jew's harp"));

        CPPUNIT_ASSERT(!this->nameExists("Delius"));
        CPPUNIT_ASSERT(!this->nameExists("Walton"));
        CPPUNIT_ASSERT(!this->nameExists("Holst"));
        CPPUNIT_ASSERT(!this->nameExists("Vaughan Williams"));
        CPPUNIT_ASSERT(!this->nameExists("Warlock"));
        CPPUNIT_ASSERT(!this->nameExists("Bliss"));
        CPPUNIT_ASSERT(!this->nameExists("Elgar"));
        CPPUNIT_ASSERT(!this->nameExists("flute"));
        CPPUNIT_ASSERT(!this->nameExists("tuba"));
        CPPUNIT_ASSERT(!this->nameExists("violin"));
        CPPUNIT_ASSERT(!this->nameExists("triangle"));
        CPPUNIT_ASSERT(!this->nameExists("jew's harp"));
        CPPUNIT_ASSERT(this->nameExists("count"));
        CPPUNIT_ASSERT(this->nameExists("max"));
        CPPUNIT_ASSERT(this->nameExists("notes"));

        LOG_DEBUG("Running 3 buckets");
        time = playData(time, BUCKET_SPAN, 3, 3, 2, 99, detector);

        CPPUNIT_ASSERT(this->influencerExists("Delius"));
        CPPUNIT_ASSERT(this->influencerExists("Walton"));
        CPPUNIT_ASSERT(this->influencerExists("Holst"));
        CPPUNIT_ASSERT(this->influencerExists("Vaughan Williams"));
        CPPUNIT_ASSERT(this->influencerExists("Warlock"));
        CPPUNIT_ASSERT(this->influencerExists("Bliss"));
        CPPUNIT_ASSERT(this->influencerExists("Elgar"));
        CPPUNIT_ASSERT(this->influencerExists("flute"));
        CPPUNIT_ASSERT(this->influencerExists("tuba"));
        CPPUNIT_ASSERT(this->influencerExists("violin"));
        CPPUNIT_ASSERT(this->influencerExists("triangle"));
        CPPUNIT_ASSERT(this->influencerExists("jew's harp"));

        // They should be purged after 3 buckets
        LOG_DEBUG("Running 2 buckets");
        time = playData(time, BUCKET_SPAN, 2, 3, 2, 99, detector);
        CPPUNIT_ASSERT(this->influencerExists("Delius"));
        CPPUNIT_ASSERT(!this->influencerExists("Walton"));
        CPPUNIT_ASSERT(this->influencerExists("Holst"));
        CPPUNIT_ASSERT(!this->influencerExists("Vaughan Williams"));
        CPPUNIT_ASSERT(!this->influencerExists("Warlock"));
        CPPUNIT_ASSERT(!this->influencerExists("Bliss"));
        CPPUNIT_ASSERT(this->influencerExists("Elgar"));
        CPPUNIT_ASSERT(this->influencerExists("flute"));
        CPPUNIT_ASSERT(this->influencerExists("tuba"));
        CPPUNIT_ASSERT(!this->influencerExists("violin"));
        CPPUNIT_ASSERT(!this->influencerExists("triangle"));
        CPPUNIT_ASSERT(!this->influencerExists("jew's harp"));

        // Most should reappear
        LOG_DEBUG("Running 1 bucket");
        time = playData(time, BUCKET_SPAN, 1, 6, 3, 99, detector);
        CPPUNIT_ASSERT(this->influencerExists("Delius"));
        CPPUNIT_ASSERT(!this->influencerExists("Walton"));
        CPPUNIT_ASSERT(this->influencerExists("Holst"));
        CPPUNIT_ASSERT(this->influencerExists("Vaughan Williams"));
        CPPUNIT_ASSERT(this->influencerExists("Warlock"));
        CPPUNIT_ASSERT(this->influencerExists("Bliss"));
        CPPUNIT_ASSERT(this->influencerExists("Elgar"));
        CPPUNIT_ASSERT(this->influencerExists("flute"));
        CPPUNIT_ASSERT(this->influencerExists("tuba"));
        CPPUNIT_ASSERT(this->influencerExists("violin"));
        CPPUNIT_ASSERT(!this->influencerExists("triangle"));
        CPPUNIT_ASSERT(!this->influencerExists("jew's harp"));
    }
}


CppUnit::Test* CStringStoreTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CStringStoreTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CStringStoreTest>(
                                   "CStringStoreTest::testPersonStringPruning",
                                   &CStringStoreTest::testPersonStringPruning) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringStoreTest>(
                                   "CStringStoreTest::testAttributeStringPruning",
                                   &CStringStoreTest::testAttributeStringPruning) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringStoreTest>(
                                   "CStringStoreTest::testInfluencerStringPruning",
                                   &CStringStoreTest::testInfluencerStringPruning) );
    return suiteOfTests;
}
