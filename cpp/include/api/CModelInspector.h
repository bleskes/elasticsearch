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
#ifndef INCLUDED_ml_api_CModelInspector_h
#define INCLUDED_ml_api_CModelInspector_h

#include <core/CoreTypes.h>

#include <model/CSearchKey.h>

#include <api/CAnomalyDetector.h>

#include <boost/function.hpp>

#include <string>
#include <vector>


namespace ml
{
namespace api
{

class CJsonOutputWriter;
class CModelVisualisationJsonWriter;

//! \brief
//! An anomaly detector with extra functions to interrogate the
//! model state
//!
//! DESCRIPTION:\n
//!
//! IMPLEMENTATION DECISIONS:\n
//! Inherits from CAnomalyDetector as CAnomalyDetector knows
//! how to restore its state
//!
class API_EXPORT CModelInspector : public CAnomalyDetector
{
    public:
        static const std::string NULL_PARTITION;
        static const std::string EMPTY_PARTITION_VALUE;
        static const std::string FIELD_NAME;
        static const std::string NO_BY_FIELD;

    public:
        typedef std::vector<std::size_t>                            TSizeVec;
        typedef std::vector<std::string>                            TStringVec;
        typedef std::vector<double>                                 TDoubleVec;
        typedef std::vector<std::size_t>::const_iterator            TSizeVecCItr;
        typedef boost::function1<void, const std::string &>         TPrinter;
        typedef boost::optional<const TKeyAnomalyDetectorPtrUMap &> TOptionalKeyAnomalyDetectorPtrMap;
        typedef boost::optional<bool>                               TOptionalBool;

        typedef std::pair<model::CSearchKey::TStrKeyPr, TAnomalyDetectorPtr> TStrKeyPrAnomalyDetectorPtrPr;

        struct API_EXPORT SPartitionInfo
        {
            SPartitionInfo(const std::string & name, std::size_t numSeries,
                                std::size_t numAttritbutes, std::size_t memoryUsage);

            std::string                 s_PartitionName;
            std::size_t                 s_NumberTimeSeries;
            std::size_t                 s_NumberAttributes;
            std::size_t                 s_MemoryUsage;
            boost::optional<bool>       s_IsInformative;
        };
        typedef std::vector<SPartitionInfo>                     TPartitionInfoVec;
        typedef std::vector<SPartitionInfo>::const_iterator     TPartitionInfoVecCItr;

        //! Detector overview
        struct API_EXPORT SDetectorInfo
        {
            SDetectorInfo(const std::string & name,
                          std::size_t numSeries,
                          std::size_t numAttritbutes,
                          std::size_t memoryUsage);

            std::string   s_DetectorName;
            std::size_t   s_NumberTimeSeries;
            std::size_t   s_NumberAttributes;
            std::size_t   s_MemoryUsage;
            TOptionalBool s_IsInformative;
        };
        typedef std::vector<SDetectorInfo>                     TDetectorInfoVec;
        typedef std::vector<SDetectorInfo>::const_iterator     TDetectorInfoVecCItr;

        typedef std::pair<std::string, TDoubleVec>             TStrDoubleVecPr;
        typedef std::vector<TStrDoubleVecPr>                   TStrDoubleVecPrVec;
        typedef std::pair<std::string, TStrDoubleVecPrVec>     TStrStrDoubleVecPrVecPr;
        typedef std::vector<TStrStrDoubleVecPrVecPr>           TStrStrDoubleVecPrVecPrVec;

        struct API_EXPORT SPopulationDistribution
        {
            std::string s_EntityName;
            TDoubleVec  s_Distribution;
            double      s_LowerBound;
            double      s_UpperBound;
        };
        typedef std::vector<SPopulationDistribution>                TPopulationDistributionVec;
        typedef std::pair<std::string, TPopulationDistributionVec>  TStrPopulationDistributionVecPr;
        typedef std::vector<TStrPopulationDistributionVecPr>        TStrPopulationDistributionVecPrVec;


        typedef std::pair<std::string, double>  TStrDoublePr;
        typedef std::vector<TStrDoublePr>       TStrDoublePrVec;


        //! The detector's people. If a population model a
        //! distribution is associated with it. Each distubution
        //! is for a given feature and the model attribute
        struct API_EXPORT SDetectorPeople
        {
            std::string                                             s_DetectorName;
            std::string                                             s_PersonFieldName;
            boost::optional<TDoubleVec>                             s_PersonProbabilities;
            bool                                                    s_IsPopulation;
            TStringVec                                              s_PeopleNames;
            boost::optional<TStrPopulationDistributionVecPrVec>     s_Distributions;
            boost::optional<std::string>                            s_CategoriesDescription;
            boost::optional<TStrDoublePrVec>                        s_CategoryFrequencies;
        };

        typedef std::vector<core_t::TTime>                       TTimeVec;
        typedef std::pair<double, double>                        TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr>                     TDoubleDoublePrVec;
        typedef std::pair<double, TDoubleDoublePr>               TDoubleDoubleDoublePrPr;
        typedef std::vector<TDoubleDoubleDoublePrPr>             TDoubleDoubleDoublePrPrVec;

        struct SVisualisationData
        {
            model_t::EFeature          s_Feature;
            bool                       s_IsInformative;
            std::string                s_PriorDescription;
            TDoubleVec                 s_Baseline;
            TDoubleVec                 s_LowerBound;
            TDoubleVec                 s_UpperBound;
            TTimeVec                   s_Time;
            TDoubleVec                 s_Distribution;
            TDoubleDoubleDoublePrPrVec s_ConfidenceIntervals;
        };

        typedef std::vector<SVisualisationData> TVisualisationDataVec;
        typedef TVisualisationDataVec::const_iterator TVisualisationDataVecCItr;


    public:
        CModelInspector(model::CLimits &limits,
                        CFieldConfig &fieldConfig,
                        model::CModelConfig &modelConfig,
                        CJsonOutputWriter &jsonOutputWriter,
                        const std::string &persistedUrlBase,
                        const std::string &restoreSnapshotId);

        virtual ~CModelInspector(void);

        //! debug function
        void printOther(const TPrinter &printer) const;

        //! Print list of partitions
        void printPartitions(const TPrinter &printer) const;

        void printPartitionValues(const std::string &partitionName, const TPrinter &printer) const;

        //! The detectors configured fields of interest
        void fieldsOfInterest(const TPrinter &printer) const;

        // Overview of all detectors
        void printDetectors(const std::string &partitionName,
                        const std::string &partitionFieldValue, const TPrinter &printer) const;

        void printDetectorPeople(const std::string &partitionName,
                    const std::string &partitionFieldValue, std::size_t index,
                    const TPrinter &printer) const;

        void printPerson(const std::string &partitionName,
                                const std::string &partitionFieldValue, std::size_t index,
                                const std::string &personName, const TPrinter &printer) const;

        // Print the debug memory usage for the model
        void printDebugMemoryUsage(const TPrinter &printer) const;

        // Dump out everything we know in one big JSON document
        void printDump(const TPrinter &printer) const;

        // Dump everything partitioned on partitionFieldName in a JSON document
        void printDump(const TPrinter &printer, const std::string &partitionFieldName) const;

        // Dump all the detectors in partitionFieldValue in a JSON document
        void printDump(const TPrinter &printer, const std::string &partitionFieldName,
                        const std::string &partitionFieldValue) const;

    private:
        TAnomalyDetectorPtrVec partitionDetectors(const std::string &partitionName,
                                        const std::string &partitionFieldValue) const;

        void writeModelDistribution(const model::CModel *model, std::size_t pid,
                                    CModelVisualisationJsonWriter &writer) const;

        void writeDetectorKeyPair(const TStrKeyPrAnomalyDetectorPtrPr &detector,
                                CModelVisualisationJsonWriter &writer) const;

        void printFeaturesAndPeople(model::CModel *model,
                         const TPrinter &printer) const;


        //! Return true if all the models are informative, false if non-are
        //! or an empty boost::optional if some models are.
        //! param [in] detector
        boost::optional<bool> modelsAreInformative(const TAnomalyDetectorPtr detector) const;

        SDetectorPeople getDetectorPeople(const TAnomalyDetectorPtr detectorPtr,
                                        bool includeSampleDistributions) const;

        SVisualisationData getPersonVisualisation(std::size_t pid,
                                                    const model::CModel *model,
                                                    model_t::EFeature feature) const;

        SVisualisationData getPopulationPersonVisualisation(std::size_t cid,
                                            const model::CModel *model,
                                           model_t::EFeature feature) const;

        //! Get an array of probability distribution for the people and
        //! feature for either population models or non population models
        //! param [in]  isPopulation
        //! param [in]  maxPlots Get at most this number of distributions
        //! param [in]  feature
        //! param [in]  model
        TPopulationDistributionVec getSampleDistributions(bool isPopulation, std::size_t maxPlots,
                                                                model_t::EFeature feature,
                                                                const model::CModel *model) const;

        //! Get an arrays of probability distribution for the people and
        //! feature from population models
        //! param [in]  maxPlots Get at most this number of distributions
        //! param [in]  feature
        //! param [in]  model
        TPopulationDistributionVec getPopulationDistributions(std::size_t maxPlots,
                                        model_t::EFeature feature,
                                        const model::CModel *model) const;

        //! Get an arrays of probability distribution for the people and
        //! feature
        //! param [in]  maxPlots Get at most this number of distributions
        //! param [in]  feature
        //! param [in]  model
        TPopulationDistributionVec getPersonDistributions(std::size_t maxPlots,
                                        model_t::EFeature feature,
                                        const model::CModel *model) const;

        //! Get the frequency at which individual people occur
        //! in buckets. Returns a list of person name, freqency pairs
        //! param [in]  maxPeople Get at most this number of people
        //! param [in]  model
        TStrDoublePrVec getPersonFrequencies(std::size_t maxPeople,
                                        const model::CModel *model) const;

        SPopulationDistribution distributionFromPrior(model_t::EFeature feature,
                                                    const maths::CPrior *prior,
                                                    const std::string &attributeName) const;

        std::string getPersonFrequenciesDescription(const model::CModel *model) const;


        //! Populate the visualisation struct with the probability distribution
        //! charting values
        //!
        //! param[in] prior to extract the distribution from
        //! param[in,out] populate with the visualisation data
        void probabilityDistributionAndConfidenceIntervals(const maths::CPrior *prior,
                                                    SVisualisationData &visualisationData) const;

    private:
        const std::string        m_PersistedUrlBase;
};


}
}

#endif // INCLUDED_ml_api_CModelInspector_h

