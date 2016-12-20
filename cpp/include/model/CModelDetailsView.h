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

#ifndef INCLUDED_prelert_model_CModelDetailsView_h
#define INCLUDED_prelert_model_CModelDetailsView_h

#include <model/CBoxPlotData.h>
#include <model/CModel.h>
#include <model/ImportExport.h>

#include <cstddef>
#include <set>
#include <string>
#include <utility>
#include <vector>

namespace prelert
{
namespace model
{
class CEventRateModel;
class CEventRateOnlineModel;
class CEventRatePeersModel;
class CEventRatePopulationModel;
class CMetricModel;
class CMetricOnlineModel;
class CMetricPopulationModel;

//! \brief A view into the model details.
//!
//! DESCRIPTION:\n
//! This defines the interface to extract various details of the CModel to
//! avoid cluttering the CModel interface. The intention is to expose all
//! aspects of the mathematical models of both individual and population
//! models for visualization purposes.
class MODEL_EXPORT CModelDetailsView
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef std::vector<std::string> TStrVec;
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
        typedef std::vector<model_t::EFeature> TFeatureVec;
        typedef std::vector<model_t::EFeature>::const_iterator TFeatureVecCItr;
        typedef CModel::TOptionalDouble TOptionalDouble;
        typedef CModel::TOptionalUInt64 TOptionalUInt64;
        typedef CBoxPlotData::SByFieldData SByFieldData;
        typedef std::set<std::string> TStrSet;
        typedef TStrSet::const_iterator TStrSetCItr;

    public:
        virtual ~CModelDetailsView(void);

        //! Get the identifier of the person called \p name if they exist.
        bool personId(const std::string &person, std::size_t &result) const;

        //! Get the identifier of the person called \p name if they exist.
        bool categoryId(const std::string &attribute, std::size_t &result) const;

        //! Get the collection of features for which data is being gathered.
        const TFeatureVec &features(void) const;

        //! Get data for creating a box plot error bar at \p time for the
        //! confidence interval \p boundsPercentile and the by fields identified
        //! by \p terms.
        //!
        //! \note If \p terms is empty all by field error bars are returned.
        void boxPlot(core_t::TTime time,
                     double boundsPercentile,
                     const TStrSet &terms,
                     CBoxPlotData &boxPlotData) const;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const = 0;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const = 0;

        //! Get the trend for \p feature and \p pid.
        //! For delta models a NULL pointer is returned.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
            trend(model_t::EFeature feature, std::size_t pid) const = 0;

        //! Get the probability for the person identified by \p pid.
        virtual double personProbability(std::size_t pid) const = 0;

        //! Get the probability for the attribute identified by \p cid.
        virtual double attributeProbability(std::size_t cid) const = 0;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const = 0;

        //! Get the model.
        virtual const CModel &base(void) const = 0;

    private:
        void addCurrentBucketValues(core_t::TTime time,
                                    model_t::EFeature feature,
                                    const TStrSet &terms,
                                    CBoxPlotData &boxPlotData) const;
        void boxPlotForByFieldId(core_t::TTime,
                                 model_t::EFeature feature,
                                 std::size_t byFieldId,
                                 double boundsPercentile,
                                 CBoxPlotData &boxPlotData) const;

        virtual std::size_t maxByFieldId(void) const = 0;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const = 0;
        virtual const std::string &byFieldName(std::size_t byFieldId) const = 0;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const = 0;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const = 0;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const = 0;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const = 0;

        //! Returns true if the terms are empty or they contain the key.
        static bool contains(const TStrSet &terms, const std::string &key);
};

//! \brief A view into the details of a CEventRateModel object.
//!
//! \sa CModelDetailsView.
class MODEL_EXPORT CEventRateModelDetailsView : public CModelDetailsView
{
    public:
        CEventRateModelDetailsView(const CEventRateModel &model);

        //! Get the feature prior for the specified by field id.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const;

        //! Get the probability for the person identified by \p pid.
        virtual double personProbability(std::size_t pid) const;

        //! Returns 0.0. (These models don't have attributes.)
        virtual double attributeProbability(std::size_t cid) const;

        //! Returns NULL for Delta models
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
            trend(model_t::EFeature feature, std::size_t pid) const;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const;

        //! Get the model.
        virtual const CModel &base(void) const;

    private:
        virtual std::size_t maxByFieldId(void) const;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const;
        virtual const std::string &byFieldName(std::size_t byFieldId) const;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const;

    private:
        //! The model.
        const CEventRateModel *m_Model;
};

//! \brief A view into the details of a CEventRatePopulationModel object.
//!
//! \sa CModelDetailsView.
class MODEL_EXPORT CEventRatePopulationModelDetailsView : public CModelDetailsView
{
    public:
        CEventRatePopulationModelDetailsView(const CEventRatePopulationModel &model);

        //! Get the feature prior for the specified by field id.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const;

        //! Returns 0.0. (These models don't compute a person probability.)
        virtual double personProbability(std::size_t pid) const;

        //! Get the probability for the attribute identified by \p cid.
        virtual double attributeProbability(std::size_t cid) const;

        //! Get the trend for \p feature and \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
            trend(model_t::EFeature feature, std::size_t cid) const;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const;

        //! Get the model.
        virtual const CModel &base(void) const;

    private:
        virtual std::size_t maxByFieldId(void) const;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const;
        virtual const std::string &byFieldName(std::size_t byFieldId) const;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const;

    private:
        //! The model.
        const CEventRatePopulationModel *m_Model;
};

//! \brief A view into the details of a CEventRatePopulationModel object.
//!
//! \sa CModelDetailsView.
class MODEL_EXPORT CEventRatePeersModelDetailsView : public CModelDetailsView
{
    public:
        CEventRatePeersModelDetailsView(const CEventRatePeersModel &model);

        //! Get the feature prior for the specified by field id.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const;

        //! Returns null.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const;

        //! Returns 0.0. (These models don't compute a person probability.)
        virtual double personProbability(std::size_t pid) const;

        //! Get the probability for the attribute identified by \p cid.
        virtual double attributeProbability(std::size_t cid) const;

        //! Get the trend for \p feature and \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
            trend(model_t::EFeature feature, std::size_t cid) const;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const;

        //! Get the model.
        virtual const CModel &base(void) const;

    private:
        virtual std::size_t maxByFieldId(void) const;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const;
        virtual const std::string &byFieldName(std::size_t byFieldId) const;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const;

    private:
        //! The model.
        const CEventRatePeersModel *m_Model;
};

//! \brief A view into the details of a CMetricModel object.
//!
//! \sa CModelDetailsView.
class MODEL_EXPORT CMetricModelDetailsView : public CModelDetailsView
{
    public:
        CMetricModelDetailsView(const CMetricModel &model);

        //! Get the feature prior for the specified by field id.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const;

        //! Returns 0.0. (These models don't compute a person probability.)
        virtual double personProbability(std::size_t pid) const;

        //! Returns 0.0. (These models don't have attributes.)
        virtual double attributeProbability(std::size_t cid) const;

        //! Returns NULL for Delta models
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
            trend(model_t::EFeature feature, std::size_t pid) const;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const;

        //! Get the model.
        virtual const CModel &base(void) const;

    private:
        virtual std::size_t maxByFieldId(void) const;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const;
        virtual const std::string &byFieldName(std::size_t byFieldId) const;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const;

    private:
        //! The model.
        const CMetricModel *m_Model;
};

//! \brief A view into the details of a CMetricPopulationModel object.
//!
//! \sa CModelDetailsView.
class MODEL_EXPORT CMetricPopulationModelDetailsView : public CModelDetailsView
{
    public:
        CMetricPopulationModelDetailsView(const CMetricPopulationModel &model);

        //! Get the feature prior for the specified by field id.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t byFieldId) const;

        //! Get the feature prior for the specified by field \p byFieldId.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t byFieldId) const;

        //! Returns 0.0. (These models don't compute a person probability.)
        virtual double personProbability(std::size_t pid) const;

        //! Returns 0.0. (These models don't compute an attribute probability.)
        virtual double attributeProbability(std::size_t cid) const;

        //! Get the trend for \p feature and \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the person of interest.
        virtual const maths::CTimeSeriesDecompositionInterface *
                          trend(model_t::EFeature feature, std::size_t cid) const;

        //! Get the minimum seasonal variance scale.
        virtual double minimumSeasonalVarianceScale(void) const;

        //! Get the model.
        virtual const CModel &base(void) const;

    private:
        virtual std::size_t maxByFieldId(void) const;
        virtual bool byFieldId(const std::string &byFieldName, std::size_t &result) const;
        virtual const std::string &byFieldName(std::size_t byFieldId) const;
        virtual const std::string &byFieldName(std::size_t pid, std::size_t cid) const;
        virtual bool isByFieldIdActive(std::size_t byFieldId) const;
        virtual double seasonalVarianceScale(model_t::EFeature feature,
                                             std::size_t byFieldId,
                                             core_t::TTime time) const;
        virtual double countVarianceScale(model_t::EFeature feature,
                                          std::size_t byFieldId,
                                          core_t::TTime time) const;

    private:
        //! The model.
        const CMetricPopulationModel *m_Model;
};

}
}

#endif // INCLUDED_prelert_model_CModelDetailsView_h
