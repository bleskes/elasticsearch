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

#ifndef INCLUDED_prelert_maths_CTimeSeriesDecompositionDetail_h
#define INCLUDED_prelert_maths_CTimeSeriesDecompositionDetail_h

#include <core/CoreTypes.h>
#include <core/CSmallVector.h>
#include <core/CStateMachine.h>

#include <maths/CSeasonalComponent.h>
#include <maths/CTimeSeriesDecompositionInterface.h>
#include <maths/CTrendTests.h>
#include <maths/ImportExport.h>

#include <boost/shared_ptr.hpp>

#include <cstddef>
#include <vector>

namespace prelert
{
namespace maths
{

//! \brief Utilities for computing the decomposition.
class MATHS_EXPORT CTimeSeriesDecompositionDetail : virtual public CTimeSeriesDecompositionTypedefs
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef CRegression::CLeastSquaresOnline<3, double> TRegression;
        class CMediator;

        //! \brief The base message passed.
        struct MATHS_EXPORT SMessage
        {
            SMessage(void);
            SMessage(core_t::TTime time);

            //! The message time.
            core_t::TTime s_Time;
        };

        //! \brief The message passed to add a point.
        struct MATHS_EXPORT SAddValueMessage : public SMessage
        {
            SAddValueMessage(core_t::TTime time,
                             double value,
                             const TWeightStyleVec &weightStyles,
                             const TDouble4Vec &weights,
                             double trend,
                             double seasonal);

            //! The value to add.
            double s_Value;
            //! The styles of the weights. Both the count and the Winsorisation
            //! weight styles have an effect. See maths_t::ESampleWeightStyle
            //! for more details.
            const TWeightStyleVec &s_WeightStyles;
            //! The weights of associated with the value. The smaller the count
            //! weight the less influence the value has on the trend and it's
            //! local variance.
            const TDouble4Vec &s_Weights;
            //! The trend component prediction at the value's time.
            double s_Trend;
            //! The seasonal component prediction at the value's time.
            double s_Seasonal;
        };

        //! \brief The message passed to indicate a trend has been detected.
        struct MATHS_EXPORT SDetectedTrendMessage : public SMessage
        {
            SDetectedTrendMessage(core_t::TTime time, const CTrendTests::CTrend &test);

            //! The test.
            const CTrendTests::CTrend &s_Test;
        };

        //! \brief The message passed to indicate periodic components have
        //! been detected.
        struct MATHS_EXPORT SDetectedPeriodicMessage : public SMessage
        {
            SDetectedPeriodicMessage(core_t::TTime time,
                                     const CTrendTests::CPeriodicity::CResult &result,
                                     const CTrendTests::CPeriodicity &test);

            //! The result of the test.
            CTrendTests::CPeriodicity::CResult s_Result;
            //! The test.
            const CTrendTests::CPeriodicity &s_Test;
        };

        //! \brief The message passed to indicate periodic components related
        //! to a specific test should be discarded.
        struct MATHS_EXPORT SDiscardPeriodicMessage : public SMessage
        {
            SDiscardPeriodicMessage(core_t::TTime time, const CTrendTests::CPeriodicity &test);

            //! The test.
            const CTrendTests::CPeriodicity &s_Test;
        };

        //! \brief The basic interface for one aspect of the modeling of a time
        //! series decomposition.
        class MATHS_EXPORT CHandler : core::CNonCopyable
        {
            public:
                CHandler(void);
                virtual ~CHandler(void);

                //! Add a value.
                virtual void handle(const SAddValueMessage &message);

                //! Handle when a trend is detected.
                virtual void handle(const SDetectedTrendMessage &message);

                //! Handle when periods are detected.
                virtual void handle(const SDetectedPeriodicMessage &message);

                //! Update to discard specific periodic components.
                virtual void handle(const SDiscardPeriodicMessage &message);

                //! Set the mediator.
                void mediator(CMediator *mediator);

                //! Get the mediator.
                CMediator *mediator(void) const;

            private:
                //! The controller responsible for forwarding messages.
                CMediator *m_Mediator;
        };

        //! \brief Manages communication between handlers.
        class MATHS_EXPORT CMediator : core::CNonCopyable
        {
            public:
                //! Forward \p message to all registered models.
                template<typename M>
                void forward(const M &message) const;

                //! Register \p handler.
                void registerHandler(CHandler &handler);

            private:
                typedef std::vector<CHandler*> THandlerPtrVec;

            private:
                //! The handlers which have added by registration.
                THandlerPtrVec m_Handlers;
        };

        //! \brief Tests for a long term trend.
        class MATHS_EXPORT CLongTermTrendTest : public CHandler
        {
            public:
                CLongTermTrendTest(double decayRate);
                CLongTermTrendTest(const CLongTermTrendTest &other);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to the supplied inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Efficiently swap the state of this and \p other.
                void swap(CLongTermTrendTest &other);

                //! Update the test with a new value.
                virtual void handle(const SAddValueMessage &message);

                //! Reset the test if still testing.
                virtual void handle(const SDetectedPeriodicMessage &message);

                //! Check if the time series has shifted level.
                void test(const SMessage &message);

                //! Set the decay rate.
                void decayRate(double decayRate);

                //! Update the test to account for \p time elapsed.
                void propagateForwardsByTime(double time);

                //! Roll time forwards by \p skipInterval.
                void skipTime(core_t::TTime skipInterval);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                typedef boost::shared_ptr<CTrendTests::CTrend> TTrendTestPtr;

            private:
                //! Handle \p symbol.
                void apply(std::size_t symbol, const SMessage &message);

                //! Check if we should run a test.
                bool shouldTest(core_t::TTime time);

                //! Get the interval between tests.
                core_t::TTime testInterval(void) const;

            private:
                //! The state machine.
                core::CStateMachine m_Machine;

                //! The maximum rate at which information is lost.
                double m_MaximumDecayRate;

                //! The next time to test for a long term trend.
                core_t::TTime m_NextTestTime;

                //! The test for a long term trend.
                TTrendTestPtr m_Test;
        };

        //! \brief Tests for daily and weekly periodic components and weekend
        //! weekday splits of the time series.
        class MATHS_EXPORT CDailyWeeklyTest : public CHandler
        {
            public:
                CDailyWeeklyTest(double decayRate, core_t::TTime bucketLength);
                CDailyWeeklyTest(const CDailyWeeklyTest &other);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to the supplied inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Efficiently swap the state of this and \p other.
                void swap(CDailyWeeklyTest &other);

                //! Update the test with a new value.
                virtual void handle(const SAddValueMessage &message);

                //! Reset the test.
                virtual void handle(const SDetectedTrendMessage &message);

                //! Either reset or disable the test.
                virtual void handle(const SDetectedPeriodicMessage &message);

                //! Test to see whether any seasonal components are present.
                void test(const SMessage &message);

                //! Get the result of the last periodicity test.
                const CTrendTests::CPeriodicity::CResult &periods(void) const;

                //! Update the test to account for \p time elapsed.
                void propagateForwardsByTime(double time);

                //! Roll time forwards by \p skipInterval.
                void skipTime(core_t::TTime skipInterval);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

                //! Debug the memory used by this object.
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by this object.
                std::size_t memoryUsage(void) const;

            private:
                typedef boost::shared_ptr<CTrendTests::CRandomizedPeriodicity> TRandomizedPeriodicityTestPtr;
                typedef boost::shared_ptr<CTrendTests::CPeriodicity> TPeriodicityTestPtr;

            private:
                //! Handle \p symbol.
                void apply(std::size_t symbol, const SMessage &message);

                //! Check if we should run a test.
                bool shouldTest(core_t::TTime time);

                //! Get the interval between tests.
                core_t::TTime testInterval(void) const;

                //! Get the time at which to time out the regular test.
                core_t::TTime timeOutRegularTest(void) const;

            private:
                //! The state machine.
                core::CStateMachine m_Machine;

                //! Controls the rate at which information is lost.
                double m_DecayRate;

                //! The raw data bucketing interval.
                core_t::TTime m_BucketLength;

                //! The next time to test for periodic components.
                core_t::TTime m_NextTestTime;

                //! The time at which we began regular testing.
                core_t::TTime m_StartedRegularTest;

                //! The time at which we switch to the small test to save memory.
                core_t::TTime m_TimeOutRegularTest;

                //! The test for periodic components.
                TPeriodicityTestPtr m_RegularTest;

                //! A small but slower test for periodic components that is used
                //! after a while if the regular test is inconclusive.
                TRandomizedPeriodicityTestPtr m_SmallTest;

                //! The result of the last test for periodic components.
                CTrendTests::CPeriodicity::CResult m_Periods;
        };

        //! \brief A reference to the long term trend.
        class MATHS_EXPORT CTrendCRef
        {
            public:
                typedef CSymmetricMatrixNxN<double, 4> TMatrix;

            public:
                CTrendCRef(void);
                CTrendCRef(const TRegression &regression,
                           double variance,
                           core_t::TTime timeOrigin,
                           core_t::TTime horizon);

                //! Get the count of values added to the trend.
                double count(void) const;

                //! Predict the long term trend at \p time with confidence
                //! interval \p confidence.
                TDoubleDoublePr prediction(core_t::TTime time, double confidence) const;

                //! Get the mean of the long term trend in [\p a, \p b].
                double mean(core_t::TTime a, core_t::TTime b) const;

                //! Get the variance about the long term trend.
                double variance(void) const;

                //! Get the covariance matrix of the regression parameters'
                //! at \p time.
                //!
                //! \param[out] result Filled in with the regression parameters'
                //! covariance matrix.
                bool covariances(TMatrix &result) const;

                //! Get the time at which to evaluate the regression model
                //! of the trend.
                double time(core_t::TTime time) const;

            private:
                //! The regression model of the trend.
                const TRegression *m_Trend;

                //! The variance of the prediction residuals.
                double m_Variance;

                //! The origin of the time coordinate system.
                core_t::TTime m_TimeOrigin;

                //! The maximum time at which which we'll predict the trend.
                core_t::TTime m_Horizon;
        };

        //! \brief Holds and updates the components of the decomposition.
        class MATHS_EXPORT CComponents : public CHandler
        {
            public:
                CComponents(double decayRate,
                            core_t::TTime bucketLength,
                            std::size_t componentSize);
                CComponents(const CComponents &other);

                //! \brief Watches to see if the seasonal components state changes.
                class MATHS_EXPORT CScopeNotifyOnStateChange : core::CNonCopyable
                {
                    public:
                        CScopeNotifyOnStateChange(CComponents &components);
                        ~CScopeNotifyOnStateChange(void);

                        //! Check if the seasonal component's state changed.
                        bool changed(void) const;

                    private:
                        //! The seasonal components this is watching.
                        CComponents &m_Components;

                        //! The flag used to watch for changes.
                        bool m_Watcher;
                };

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to the supplied inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Efficiently swap the state of this and \p other.
                void swap(CComponents &other);

                //! Update the components with a new value.
                virtual void handle(const SAddValueMessage &message);

                //! Create a new trend component.
                virtual void handle(const SDetectedTrendMessage &message);

                //! Create new seasonal components.
                virtual void handle(const SDetectedPeriodicMessage &message);

                //! Discard the matching seasonal components.
                virtual void handle(const SDiscardPeriodicMessage &message);

                //! Maybe re-interpolate the components.
                void interpolate(const SMessage &message);

                //! Set the decay rate.
                void decayRate(double decayRate);

                //! Update the components to account for the time elapsed
                //! between \p start and \p end.
                void propagateForwards(core_t::TTime start, core_t::TTime end);

                //! Check if we're forecasting.
                bool forecasting(void) const;

                //! Start forecasting.
                void forecast(void);

                //! Check if the decomposition has any initialized components.
                bool initialized(void) const;

                //! Get the long term trend.
                CTrendCRef trend(void) const;

                //! Get the components.
                const TComponentVec &seasonal(void) const;

                //! Get the mean value of the baseline.
                double meanValue(void) const;

                //! Get the mean value of the baseline at \p time.
                double meanValue(core_t::TTime time) const;

                //! Get the mean variance of the baseline.
                double meanVariance(void) const;

                //! Roll time forwards by \p skipInterval.
                void skipTime(core_t::TTime skipInterval);

                //! Get the interval to use between interpolates of the trends.
                core_t::TTime interpolateInterval(void) const;

                //! Get the interval to use between aging the trend.
                core_t::TTime propagationForwardsByTimeInterval(void) const;

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

                //! Debug the memory used by this object.
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by this object.
                std::size_t memoryUsage(void) const;

            private:
                typedef boost::optional<double> TOptionalDouble;
                typedef CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;

                //! \brief The long term trend.
                struct STrend
                {
                    STrend(void);

                    //! Initialize by reading state from \p traverser.
                    bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                    //! Persist state by passing information to the supplied inserter.
                    void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                    //! Refresh the current trend.
                    void refresh(core_t::TTime time);

                    //! Get a checksum for this object.
                    uint64_t checksum(uint64_t seed = 0) const;

                    //! The regression model of the trend.
                    TRegression s_Regression;

                    //! The variance of the trend.
                    double s_Variance;

                    //! The origin of the time coordinate system.
                    core_t::TTime s_TimeOrigin;
                };

                typedef boost::shared_ptr<STrend> TTrendPtr;

                //! \brief The seasonal components of the decomposition.
                struct SSeasonal
                {
                    //! Initialize by reading state from \p traverser.
                    bool acceptRestoreTraverser(double decayRate,
                                                core_t::TTime bucketLength,
                                                core::CStateRestoreTraverser &traverser);

                    //! Persist state by passing information to the supplied inserter.
                    void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                    //! Shift the components by \p shift.
                    void shift(double shift);

                    //! Get a checksum for this object.
                    uint64_t checksum(uint64_t seed = 0) const;

                    //! Debug the memory used by this object.
                    void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                    //! Get the memory used by this object.
                    std::size_t memoryUsage(void) const;

                    //! The seasonal components.
                    TComponentVec s_Components;

                    //! The delta offset to apply to the difference between each
                    //! component value and its mean which are used to minimize
                    //! slope in the longer periods.
                    TDoubleVec s_Deltas;
                };

                typedef boost::shared_ptr<SSeasonal> TSeasonalPtr;

            private:
                //! Handle \p symbol.
                void apply(std::size_t symbol, const SMessage &message);

                //! Check if we should interpolate.
                bool shouldInterpolate(core_t::TTime time);

                //! Set a watcher for state changes.
                void notifyOnNewComponents(bool *watcher);

            private:
                //! The state machine.
                core::CStateMachine m_Machine;

                //! Controls the rate at which information is lost.
                double m_DecayRate;

                //! The raw data bucketing length.
                core_t::TTime m_BucketLength;

                //! Get the length of the longest period.
                core_t::TTime m_LongestPeriod;

                //! The number of buckets to use to estimate a periodic component.
                std::size_t m_SeasonalComponentSize;

                //! The last time at which the various components *were* interpolated.
                core_t::TTime m_LastInterpolatedTime;

                //! The next time at which to interpolate the various components.
                core_t::TTime m_NextInterpolateTime;

                //! The long term trend.
                TTrendPtr m_Trend;

                //! The seasonal components.
                TSeasonalPtr m_Seasonal;

                //! Set to true if non-null when the seasonal components change.
                bool *m_Watcher;
        };
};

}
}

#endif // INCLUDED_prelert_maths_CTimeSeriesDecompositionDetail_h
