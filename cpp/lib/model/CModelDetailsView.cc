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

#include <model/CModelDetailsView.h>

#include <core/CSmallVector.h>

#include <maths/CBasicStatistics.h>
#include <maths/CTimeSeriesDecomposition.h>

#include <model/CDataGatherer.h>
#include <model/CEventRateModel.h>
#include <model/CEventRatePeersModel.h>
#include <model/CEventRatePopulationModel.h>
#include <model/CMetricModel.h>
#include <model/CMetricPopulationModel.h>

namespace prelert
{
namespace model
{
typedef core::CSmallVector<double, 1> TDouble1Vec;

static const std::string EMPTY_STRING("");

////////// CModelDetailsView Implementation //////////

CModelDetailsView::~CModelDetailsView(void)
{
}

bool CModelDetailsView::personId(const std::string &name,
                                 std::size_t &result) const
{
    return this->base().dataGatherer().personId(name, result);
}

bool CModelDetailsView::categoryId(const std::string &attribute,
                                   std::size_t &result) const
{
    return this->base().dataGatherer().attributeId(attribute, result);
}

const CModelDetailsView::TFeatureVec &CModelDetailsView::features(void) const
{
    return this->base().dataGatherer().features();
}

void CModelDetailsView::boxPlot(core_t::TTime time,
                                double boundsPercentile,
                                const TStrSet &terms,
                                CBoxPlotData &boxPlotData) const
{
    const TFeatureVec &features = this->features();
    for (std::size_t i = 0; i < features.size(); ++i)
    {
        const model_t::EFeature &feature = features[i];

        if (model_t::isConstant(feature) || model_t::isCategorical(feature))
        {
            continue;
        }

        if (terms.empty())
        {
            for (std::size_t byFieldId = 0; byFieldId < this->maxByFieldId(); ++byFieldId)
            {
                this->boxPlotForByFieldId(time, feature, byFieldId, boundsPercentile, boxPlotData);
            }
        }
        else
        {
            for (TStrSetCItr j = terms.begin(); j != terms.end(); ++j)
            {
                std::size_t byFieldId(0);
                if (this->byFieldId(*j, byFieldId))
                {
                    this->boxPlotForByFieldId(time, feature, byFieldId, boundsPercentile, boxPlotData);
                }
            }
        }

        this->addCurrentBucketValues(time, feature, terms, boxPlotData);
    }
}

void CModelDetailsView::boxPlotForByFieldId(core_t::TTime time,
                                            model_t::EFeature feature,
                                            std::size_t byFieldId,
                                            double boundsPercentile,
                                            CBoxPlotData &boxPlotData) const
{
    typedef std::pair<TDouble1Vec, TDouble1Vec> TDouble1VecDouble1VecPr;
    typedef core::CSmallVector<double, 4> TDouble4Vec;

    if (this->isByFieldIdActive(byFieldId))
    {
        const maths::CPrior *prior = this->prior(feature, byFieldId);
        if (!prior || prior->isNonInformative())
        {
            return;
        }

        static const maths_t::ESampleWeightStyle WEIGHT_STYLES_[] =
            {
                maths_t::E_SampleSeasonalVarianceScaleWeight,
                maths_t::E_SampleCountVarianceScaleWeight
            };
        static maths_t::TWeightStyleVec WEIGHT_STYLES(boost::begin(WEIGHT_STYLES_),
                                                      boost::end(WEIGHT_STYLES_));

        TDouble4Vec weights(WEIGHT_STYLES.size(), 1.0);
        double offset = 0.0;
        const maths::CTimeSeriesDecompositionInterface *trend = this->trend(feature, byFieldId);
        if (trend && trend->initialized())
        {
            weights[0] = this->seasonalVarianceScale(feature, byFieldId, time);
            weights[1] = this->countVarianceScale(feature, byFieldId, time);
            offset     = maths::CBasicStatistics::mean(trend->baseline(time, 0.0)) - trend->level();
        }

        TDouble1VecDouble1VecPr support = model_t::support(feature);
        TDoubleDoublePr bounds = prior->marginalLikelihoodConfidenceInterval(boundsPercentile, WEIGHT_STYLES, weights);
        double median = maths::CTools::truncate(
                            offset + maths::CBasicStatistics::mean(
                                         prior->marginalLikelihoodConfidenceInterval(0.0, WEIGHT_STYLES, weights)),
                            support.first[0], support.second[0]);
        double lowerValue = maths::CTools::truncate(offset + bounds.first, support.first[0], support.second[0]);
        double upperValue = maths::CTools::truncate(offset + bounds.second, lowerValue, support.second[0]);
        boxPlotData.get(feature, this->byFieldName(byFieldId)) = SByFieldData(lowerValue, upperValue, median);
    }
}

void CModelDetailsView::addCurrentBucketValues(core_t::TTime time,
                                               model_t::EFeature feature,
                                               const TStrSet &terms,
                                               CBoxPlotData &boxPlotData) const
{
    typedef CDataGatherer::TSizeSizePrUInt64UMap TSizeSizePrUInt64UMap;
    typedef CDataGatherer::TSizeSizePrUInt64UMapCItr TSizeSizePrUInt64UMapCItr;

    const CDataGatherer &gatherer = this->base().dataGatherer();
    if (!gatherer.dataAvailable(time))
    {
        return;
    }
    const TSizeSizePrUInt64UMap &personAttributeCounts = gatherer.bucketCounts(time);
    for (TSizeSizePrUInt64UMapCItr itr = personAttributeCounts.begin();
         itr != personAttributeCounts.end();
         ++itr)
    {
        std::size_t pid = gatherer.extractPersonId(*itr);
        std::size_t cid = gatherer.extractAttributeId(*itr);
        const std::string &byFieldName = this->byFieldName(pid, cid);
        if (this->contains(terms, byFieldName))
        {
            TDouble1Vec value = this->base().currentBucketValue(feature, pid, cid, time);
            if (!value.empty())
            {
                const std::string &overFieldName = this->base().isPopulation() ?
                                                   this->base().personName(pid) :
                                                   EMPTY_STRING;
                boxPlotData.get(feature, byFieldName).addValue(overFieldName, value[0]);
            }
        }
    }
}

bool CModelDetailsView::contains(const TStrSet &terms, const std::string &key)
{
    return terms.empty() || key.empty() || terms.find(key) != terms.end();
}

////////// CEventRateModelDetailsView Implementation //////////

CEventRateModelDetailsView::CEventRateModelDetailsView(const CEventRateModel &model) :
        m_Model(&model)
{
}

const maths::CPrior *
    CEventRateModelDetailsView::prior(model_t::EFeature feature,
                                      std::size_t byFieldId) const
{
    return m_Model->prior(feature, byFieldId);
}

const maths::CMultivariatePrior *
    CEventRateModelDetailsView::multivariatePrior(model_t::EFeature feature,
                                                  std::size_t byFieldId) const
{
    return m_Model->multivariatePrior(feature, byFieldId);
}

double CEventRateModelDetailsView::personProbability(std::size_t pid) const
{
    double result;
    m_Model->m_ProbabilityPrior.probability(static_cast<double>(pid), result);
    return result;
}

double CEventRateModelDetailsView::attributeProbability(std::size_t /*cid*/) const
{
    return 0.0;
}

const maths::CTimeSeriesDecompositionInterface *
    CEventRateModelDetailsView::trend(model_t::EFeature feature,
                                      std::size_t pid) const
{
    return m_Model->trend(feature, pid).empty() ? 0 : m_Model->trend(feature, pid)[0];
}

double CEventRateModelDetailsView::minimumSeasonalVarianceScale(void) const
{
    return CEventRateModel::MINIMUM_SEASONAL_VARIANCE_SCALE;
}

const CModel &CEventRateModelDetailsView::base(void) const
{
    return *m_Model;
}

std::size_t CEventRateModelDetailsView::maxByFieldId(void) const
{
    return this->base().dataGatherer().numberPeople();
}

bool CEventRateModelDetailsView::byFieldId(const std::string &byFieldName,
                                           std::size_t &result) const
{
    return this->base().dataGatherer().personId(byFieldName, result);
}

const std::string &CEventRateModelDetailsView::byFieldName(std::size_t byFieldId) const
{
    return this->base().personName(byFieldId);
}

const std::string &CEventRateModelDetailsView::byFieldName(std::size_t pid,
                                                           std::size_t /*cid*/) const
{
    return this->base().personName(pid);
}

bool CEventRateModelDetailsView::isByFieldIdActive(std::size_t byFieldId) const
{
    return this->base().dataGatherer().isPersonActive(byFieldId);
}

double CEventRateModelDetailsView::seasonalVarianceScale(model_t::EFeature feature,
                                                         std::size_t byFieldId,
                                                         core_t::TTime time) const
{
    return m_Model->seasonalVarianceScale(feature, byFieldId, time,
                                          CModel::SEASONAL_CONFIDENCE_INTERVAL).second[0];
}

double CEventRateModelDetailsView::countVarianceScale(model_t::EFeature /*feature*/,
                                                      std::size_t /*byFieldId*/,
                                                      core_t::TTime /*time*/) const
{

    return 1.0;
}

////////// CEventRatePopulationModelDetailsView Implementation //////////

CEventRatePopulationModelDetailsView::CEventRatePopulationModelDetailsView(const CEventRatePopulationModel &model) :
        m_Model(&model)
{
}

const maths::CPrior *
    CEventRatePopulationModelDetailsView::prior(model_t::EFeature feature,
                                                std::size_t byFieldId) const
{
    return m_Model->prior(feature, byFieldId);
}

const maths::CMultivariatePrior *
    CEventRatePopulationModelDetailsView::multivariatePrior(model_t::EFeature feature,
                                                            std::size_t byFieldId) const
{
    return m_Model->multivariatePrior(feature, byFieldId);
}

double CEventRatePopulationModelDetailsView::personProbability(std::size_t /*pid*/) const
{
    return 0.0;
}

double CEventRatePopulationModelDetailsView::attributeProbability(std::size_t cid) const
{
    double result;
    m_Model->m_AttributeProbabilityPrior.probability(static_cast<double>(cid), result);
    return result;
}

const maths::CTimeSeriesDecompositionInterface *
    CEventRatePopulationModelDetailsView::trend(model_t::EFeature feature,
                                                std::size_t cid) const
{
    return m_Model->trend(feature, cid).empty() ? 0 : m_Model->trend(feature, cid)[0];
}

double CEventRatePopulationModelDetailsView::minimumSeasonalVarianceScale(void) const
{
    return CEventRatePopulationModel::MINIMUM_SEASONAL_VARIANCE_SCALE;
}

const CModel &CEventRatePopulationModelDetailsView::base(void) const
{
    return *m_Model;
}

std::size_t CEventRatePopulationModelDetailsView::maxByFieldId(void) const
{
    return this->base().dataGatherer().numberAttributes();
}

bool CEventRatePopulationModelDetailsView::byFieldId(const std::string &byFieldName,
                                                     std::size_t &result) const
{
    return this->base().dataGatherer().attributeId(byFieldName, result);
}

const std::string &CEventRatePopulationModelDetailsView::byFieldName(std::size_t byFieldId) const
{
    return this->base().attributeName(byFieldId);
}

const std::string &CEventRatePopulationModelDetailsView::byFieldName(std::size_t /*pid*/,
                                                                     std::size_t cid) const
{
    return this->base().attributeName(cid);
}

bool CEventRatePopulationModelDetailsView::isByFieldIdActive(std::size_t byFieldId) const
{
    return this->base().dataGatherer().isAttributeActive(byFieldId);
}

double CEventRatePopulationModelDetailsView::seasonalVarianceScale(model_t::EFeature feature,
                                                                   std::size_t byFieldId,
                                                                   core_t::TTime time) const
{
    return m_Model->seasonalVarianceScale(feature, byFieldId, time,
                                          CModel::SEASONAL_CONFIDENCE_INTERVAL).second[0];
}

double CEventRatePopulationModelDetailsView::countVarianceScale(model_t::EFeature /*feature*/,
                                                                std::size_t /*byFieldId*/,
                                                                core_t::TTime /*time*/) const
{
    return 1.0;
}

////////// CEventRatePeersModelDetailsView Implementation //////////

CEventRatePeersModelDetailsView::CEventRatePeersModelDetailsView(const CEventRatePeersModel &model) :
        m_Model(&model)
{
}

const maths::CPrior *
    CEventRatePeersModelDetailsView::prior(model_t::EFeature /*feature*/,
                                           std::size_t /*byFieldId*/) const
{
    return 0;
}

const maths::CMultivariatePrior *
    CEventRatePeersModelDetailsView::multivariatePrior(model_t::EFeature /*feature*/,
                                                       std::size_t /*byFieldId*/) const
{
    return 0;
}

double CEventRatePeersModelDetailsView::personProbability(std::size_t /*pid*/) const
{
    return 0.0;
}

double CEventRatePeersModelDetailsView::attributeProbability(std::size_t /*cid*/) const
{
    return 0.0;
}

const maths::CTimeSeriesDecompositionInterface *
    CEventRatePeersModelDetailsView::trend(model_t::EFeature feature,
                                           std::size_t cid) const
{
    return m_Model->trend(feature, cid).empty() ? 0 : m_Model->trend(feature, cid)[0];
}

double CEventRatePeersModelDetailsView::minimumSeasonalVarianceScale(void) const
{
    return CEventRatePeersModel::MINIMUM_SEASONAL_VARIANCE_SCALE;
}

const CModel &CEventRatePeersModelDetailsView::base(void) const
{
    return *m_Model;
}

std::size_t CEventRatePeersModelDetailsView::maxByFieldId(void) const
{
    return this->base().dataGatherer().numberAttributes();
}

bool CEventRatePeersModelDetailsView::byFieldId(const std::string &byFieldName,
                                                std::size_t &result) const
{
    return this->base().dataGatherer().attributeId(byFieldName, result);
}

const std::string &CEventRatePeersModelDetailsView::byFieldName(std::size_t byFieldId) const
{
    return this->base().attributeName(byFieldId);
}

const std::string &CEventRatePeersModelDetailsView::byFieldName(std::size_t /*pid*/,
                                                                std::size_t cid) const
{
    return this->base().attributeName(cid);
}

bool CEventRatePeersModelDetailsView::isByFieldIdActive(std::size_t byFieldId) const
{
    return this->base().dataGatherer().isAttributeActive(byFieldId);
}

double CEventRatePeersModelDetailsView::seasonalVarianceScale(model_t::EFeature /*feature*/,
                                                              std::size_t /*byFieldId*/,
                                                              core_t::TTime /*time*/) const
{

    return 1.0;
}

double CEventRatePeersModelDetailsView::countVarianceScale(model_t::EFeature /*feature*/,
                                                           std::size_t /*byFieldId*/,
                                                           core_t::TTime /*time*/) const
{
    return 1.0;
}

////////// CMetricModelDetailsView Implementation //////////

CMetricModelDetailsView::CMetricModelDetailsView(const CMetricModel &model) :
        m_Model(&model)
{
}

const maths::CPrior *
    CMetricModelDetailsView::prior(model_t::EFeature feature,
                                   std::size_t byFieldId) const
{
    return m_Model->prior(feature, byFieldId);
}

const maths::CMultivariatePrior *
    CMetricModelDetailsView::multivariatePrior(model_t::EFeature feature,
                                               std::size_t byFieldId) const
{
    return m_Model->multivariatePrior(feature, byFieldId);
}

double CMetricModelDetailsView::personProbability(std::size_t /*pid*/) const
{
    return 0.0;
}

double CMetricModelDetailsView::attributeProbability(std::size_t /*cid*/) const
{
    return 0.0;
}

const maths::CTimeSeriesDecompositionInterface *
    CMetricModelDetailsView::trend(model_t::EFeature feature,
                                   std::size_t pid) const
{
    return m_Model->trend(feature, pid).empty() ? 0 : m_Model->trend(feature, pid)[0];
}

double CMetricModelDetailsView::minimumSeasonalVarianceScale(void) const
{
    return CMetricModel::MINIMUM_SEASONAL_VARIANCE_SCALE;
}

const CModel &CMetricModelDetailsView::base(void) const
{
    return *m_Model;
}

std::size_t CMetricModelDetailsView::maxByFieldId(void) const
{
    return this->base().dataGatherer().numberPeople();
}

bool CMetricModelDetailsView::byFieldId(const std::string &byFieldName,
                                        std::size_t &result) const
{
    return this->base().dataGatherer().personId(byFieldName, result);
}

const std::string &CMetricModelDetailsView::byFieldName(std::size_t byFieldId) const
{
    return this->base().personName(byFieldId);
}

const std::string &CMetricModelDetailsView::byFieldName(std::size_t pid,
                                                        std::size_t /*cid*/) const
{
    return this->base().personName(pid);
}

bool CMetricModelDetailsView::isByFieldIdActive(std::size_t byFieldId) const
{
    return this->base().dataGatherer().isPersonActive(byFieldId);
}

double CMetricModelDetailsView::seasonalVarianceScale(model_t::EFeature feature,
                                                      std::size_t byFieldId,
                                                      core_t::TTime time) const
{
    return m_Model->seasonalVarianceScale(feature, byFieldId, time,
                                          CModel::SEASONAL_CONFIDENCE_INTERVAL).second[0];
}

double CMetricModelDetailsView::countVarianceScale(model_t::EFeature feature,
                                                   std::size_t byFieldId,
                                                   core_t::TTime time) const
{
    TOptionalUInt64 count = m_Model->currentBucketCount(byFieldId, time);
    if (!count)
    {
        return 1.0;
    }
    return model_t::varianceScale(feature,
                                  m_Model->dataGatherer().effectiveSampleCount(byFieldId),
                                  static_cast<double>(*count));
}

////////// CMetricPopulationModelDetailsView Implementation //////////

CMetricPopulationModelDetailsView::CMetricPopulationModelDetailsView(const CMetricPopulationModel &model) :
        m_Model(&model)
{
}

const maths::CPrior *
    CMetricPopulationModelDetailsView::prior(model_t::EFeature feature,
                                             std::size_t byFieldId) const
{
    return m_Model->prior(feature, byFieldId);
}

const maths::CMultivariatePrior *
    CMetricPopulationModelDetailsView::multivariatePrior(model_t::EFeature feature,
                                                         std::size_t byFieldId) const
{
    return m_Model->multivariatePrior(feature, byFieldId);
}

double CMetricPopulationModelDetailsView::personProbability(std::size_t /*pid*/) const
{
    return 0.0;
}

double CMetricPopulationModelDetailsView::attributeProbability(std::size_t /*cid*/) const
{
    return 0.0;
}

const maths::CTimeSeriesDecompositionInterface *
    CMetricPopulationModelDetailsView::trend(model_t::EFeature feature,
                                             std::size_t cid) const
{
    return m_Model->trend(feature, cid).empty() ? 0 : m_Model->trend(feature, cid)[0];
}

double CMetricPopulationModelDetailsView::minimumSeasonalVarianceScale(void) const
{
    return CMetricPopulationModel::MINIMUM_SEASONAL_VARIANCE_SCALE;
}

const CModel &CMetricPopulationModelDetailsView::base(void) const
{
    return *m_Model;
}

std::size_t CMetricPopulationModelDetailsView::maxByFieldId(void) const
{
    return this->base().dataGatherer().numberAttributes();
}

bool CMetricPopulationModelDetailsView::byFieldId(const std::string &byFieldName,
                                                  std::size_t &result) const
{
    return this->base().dataGatherer().attributeId(byFieldName, result);
}

const std::string &CMetricPopulationModelDetailsView::byFieldName(std::size_t byFieldId) const
{
    return this->base().attributeName(byFieldId);
}

const std::string &CMetricPopulationModelDetailsView::byFieldName(std::size_t /*pid*/,
                                                                  std::size_t cid) const
{
    return this->base().attributeName(cid);
}

bool CMetricPopulationModelDetailsView::isByFieldIdActive(std::size_t byFieldId) const
{
    return this->base().dataGatherer().isAttributeActive(byFieldId);
}

double CMetricPopulationModelDetailsView::seasonalVarianceScale(model_t::EFeature feature,
                                                                std::size_t byFieldId,
                                                                core_t::TTime time) const
{
    return m_Model->seasonalVarianceScale(feature, byFieldId, time,
                                          CModel::SEASONAL_CONFIDENCE_INTERVAL).second[0];
}

double CMetricPopulationModelDetailsView::countVarianceScale(model_t::EFeature feature,
                                                             std::size_t byFieldId,
                                                             core_t::TTime time) const
{
    TOptionalUInt64 count = m_Model->currentBucketCount(byFieldId, time);
    if (!count)
    {
        return 1.0;
    }
    return model_t::varianceScale(feature,
                                  m_Model->dataGatherer().effectiveSampleCount(byFieldId),
                                  static_cast<double>(*count));
}

}
}
