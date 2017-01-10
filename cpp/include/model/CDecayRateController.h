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

#ifndef INCLUDED_ml_model_CDecayRateController_h
#define INCLUDED_ml_model_CDecayRateController_h

#include <core/CoreTypes.h>
#include <core/CSmallVector.h>

#include <maths/CBasicStatistics.h>

#include <model/ImportExport.h>

#include <stdint.h>

namespace ml
{
namespace model
{

//! \brief Manages the decay rate based on the data characteristics.
//!
//! DESCRIPTION:\n
//! We can use estimates of the prediction errors to understand if our
//! models are capturing the time varying components of a time series
//! and if there has recently been a significant change from the time
//! series behavior. In particular, we look at
//!   -# The ratio of the prediction bias to the prediction error.
//!   -# The ratio of the recent absolute to the long term prediction
//!      error.
//!
//! If there is a significant bias in our predictions then our model is
//! failing to capture some time varying component of the time series
//! and the best we can do is to remember less history. If the short term
//! and prediction error is large compared to the long term prediction
//! error then the system has recently undergone some state change and
//! we should re-learn the model parameters as fast as possible.
class MODEL_EXPORT CDecayRateController
{
    public:
        typedef core::CSmallVector<double, 1> TDouble1Vec;
        typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
        typedef core::CSmallVector<TMeanAccumulator, 1> TMeanAccumulator1Vec;

    public:
        CDecayRateController(void);
        explicit CDecayRateController(std::size_t dimension);

        //! Reset the errors.
        void reset(void);

        //! Restore by reading state from \p traverser.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Persist by passing state to \p inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Get the decay rate multiplier to apply and update the relevant
        //! prediction errors.
        double multiplier(const TDouble1Vec &prediction,
                          const TDouble1Vec &residual,
                          core_t::TTime bucketLength,
                          double learnRate,
                          double decayRate);

        //! Debug the memory used by this controller.
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this controller.
        std::size_t memoryUsage(void) const;

        //! Get a checksum of this object.
        uint64_t checksum(uint64_t seed) const;

    private:
        //! Get the count of residuals added so far.
        double count(void) const;

        //! Get the coefficient of variation of the prediction error.
        double cov(void) const;

    private:
        //! The current cumulative multiplier.
        double m_Multiplier;

        //! The mean predicted value.
        TMeanAccumulator1Vec m_PredictionMean;

        //! The mean bias in the model predictions.
        TMeanAccumulator1Vec m_Bias;

        //! The short term absolute errors in the model predictions.
        TMeanAccumulator1Vec m_ShortTermAbsResidual;

        //! The long term absolute errors in the model predictions.
        TMeanAccumulator1Vec m_LongTermAbsResidual;
};

}
}

#endif // INCLUDED_ml_model_CDecayRateController_h
