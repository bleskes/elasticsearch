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

//!
class MATHS_EXPORT CTimeSeriesDecompositionDetail : virtual public CTimeSeriesDecompositionTypedefs
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<core_t::TTime> TTimeVec;
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
                             double mean);

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
            //! The mean of the time series.
            double s_Mean;
        };

        //! \brief The message passed to indicate a trend has been detected.
        struct MATHS_EXPORT SDetectedTrendMessage : public SMessage
        {
            SDetectedTrendMessage(core_t::TTime time);
        };

        //! \brief The message passed to indicate periodic components have
        //! been detected.
        struct MATHS_EXPORT SDetectedPeriodicMessage : public SMessage
        {
            SDetectedPeriodicMessage(core_t::TTime time,
                                     const CTrendTests::CPeriodicity::CResult &result,
                                     const CTrendTests::CPeriodicity &test);

            //! The result of the test.
            const CTrendTests::CPeriodicity::CResult s_Result;
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

        // \brief The messaged passed to indicate a new level shift.
        struct MATHS_EXPORT SHasLevelShiftMessage : public SMessage
        {
            SHasLevelShiftMessage(core_t::TTime time, int intervals, double size);

            //! The number of consecutive test intervals with a shift.
            int s_Intervals;
            //! The normalized step size.
            double s_Size;
        };

        // \brief The messaged passed to indicate a level shift was transient.
        struct MATHS_EXPORT SNoLongerHasLevelShiftMessage : public SMessage
        {
            SNoLongerHasLevelShiftMessage(core_t::TTime time);
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

                //! Handle when a level shift is detected.
                virtual void handle(const SHasLevelShiftMessage &message);

                //! Handle when a level shift is no longer detected.
                virtual void handle(const SNoLongerHasLevelShiftMessage &message);

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

                //! Set the decay rate.
                void decayRate(double decayRate);

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

                //! The time of the last test for periodic components.
                core_t::TTime m_LastTestTime;

                //! The time at which we switch to the small test to save memory.
                core_t::TTime m_TimeOutRegularTest;

                //! The test for periodic components.
                TPeriodicityTestPtr m_RegularTest;

                //! The small periodicity test that is used in memory-constrained
                //! situations.
                TRandomizedPeriodicityTestPtr m_SmallTest;

                //! The result of the last test for periodic components.
                CTrendTests::CPeriodicity::CResult m_Periods;
        };

        //! \brief Tests for a level shift in the trend.
        //!
        //! DESCRIPTION:\n
        //! If there is a level shift - either a large step, relative to the
        //! prediction errors, up or down - it is inappropriate to interpolate
        //! between the components before and after the step. We detect this
        //! case by looking for a large signed mean error in our predictions
        //! over an interval between interpolation events.
        class MATHS_EXPORT CLevelShiftTest : public CHandler
        {
            public:
                CLevelShiftTest(double decayRate);
                CLevelShiftTest(const CLevelShiftTest &other);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to the supplied inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Efficiently swap the state of this and \p other.
                void swap(CLevelShiftTest &other);

                //! Update the test with a new value.
                virtual void handle(const SAddValueMessage &message);

                //! Reset the test.
                virtual void handle(const SDetectedTrendMessage &message);

                //! Reset the test.
                virtual void handle(const SDetectedPeriodicMessage &message);

                //! Check if the time series has shifted level.
                void test(const SMessage &message);

                //! Update the test to account for \p time elapsed.
                void propagateForwardsByTime(double time);

                //! Roll time forwards by \p skipInterval.
                void skipTime(core_t::TTime skipInterval);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
                typedef CVectorNx1<double, 2> TVector;
                typedef maths::CBasicStatistics::SSampleMean<TVector>::TAccumulator TVectorMeanAccumulator;

            private:
                //! A small but non-negligible shift as a multiple of the prediction error.
                static const double SMALL_SHIFT;

                //! A large shift as a multiple of the prediction error.
                static const double LARGE_SHIFT;

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

                //! Controls the rate at which information is lost.
                double m_DecayRate;

                //! The last time the level shift test was run.
                core_t::TTime m_LastTestTime;

                //! The time after which we time out the current state.
                int m_TimeOutState;

                //! The mean signed and unsigned differences between the
                //! predictions and values in this interpolation interval.
                TVectorMeanAccumulator m_MeanPredictionErrors;

                //! The long term mean unsigned differences between the
                //! predictions and values excluding shift intervals.
                TMeanAccumulator m_LongTermMeanPredictionError;
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

                //! Start shifting the components.
                virtual void handle(const SHasLevelShiftMessage &message);

                //! Stop shifting the components.
                virtual void handle(const SNoLongerHasLevelShiftMessage &message);

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

                //! Get a constant reference to the components.
                const TComponentVec &seasonal(void) const;

                //! Get the mean value of the baseline.
                double meanValue(void) const;

                //! Get the mean value of the baseline at \p time.
                double meanValue(core_t::TTime time) const;

                //! Get the mean variance of the baseline.
                double meanVariance(void) const;

                //! Get the level of the time series.
                double level(void) const;

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

                //! \brief The seasonal components of the decomposition.
                struct SSeasonal
                {
                    //! Initialize by reading state from \p traverser.
                    bool acceptRestoreTraverser(double decayRate,
                                                core_t::TTime bucketLength,
                                                core::CStateRestoreTraverser &traverser);

                    //! Persist state by passing information to the supplied inserter.
                    void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                    //! Set whether to measure the prediction error variances.
                    void resetVariance(double variance);

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

                    //! The length of history in each component.
                    TDoubleVec s_HistoryLengths;
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

                //! The time of the last interpolation of value and variance trends.
                core_t::TTime m_LastInterpolateTime;

                //! The weight to use when interpolating.
                double m_InterpolationWeight;

                //! The current trend level.
                double m_Level;

                //! The seasonal components.
                TSeasonalPtr m_Seasonal;

                //! The mean and variance of the value moments.
                TMeanVarAccumulator m_Moments;

                //! Set to true if non-null when the seasonal components change.
                bool *m_Watcher;
        };
};

}
}

#endif // INCLUDED_prelert_maths_CTimeSeriesDecompositionDetail_h
