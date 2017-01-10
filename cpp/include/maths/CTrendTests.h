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

#ifndef INCLUDED_prelert_maths_CTrendTests_h
#define INCLUDED_prelert_maths_CTrendTests_h

#include <core/AtomicTypes.h>
#include <core/CoreTypes.h>
#include <core/CMutex.h>

#include <maths/CBasicStatistics.h>
#include <maths/CFloatStorage.h>
#include <maths/CLinearAlgebra.h>
#include <maths/CPRNG.h>
#include <maths/CRegression.h>
#include <maths/ImportExport.h>

#include <cstddef>
#include <vector>

#include <boost/random/mersenne_twister.hpp>

#include <stdint.h>

class CTrendTestsTest;

namespace prelert
{
namespace maths
{

//! \brief A collection of statistical tests for decomposing a time
//! series into a trend and seasonal components.
class MATHS_EXPORT CTrendTests
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::pair<int, core_t::TTime> TIntTimePr;
        typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
        typedef CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
        typedef std::pair<TTimeTimePr, TMeanVarAccumulator> TTimeTimePrMeanVarAccumulatorPr;
        typedef std::vector<TTimeTimePrMeanVarAccumulatorPr> TTimeTimePrMeanVarAccumulatorPrVec;
        typedef CBasicStatistics::SSampleMean<CFloatStorage>::TAccumulator TFloatMeanAccumulator;
        typedef std::vector<TFloatMeanAccumulator> TFloatMeanAccumulatorVec;
        typedef core::CSmallVector<core_t::TTime, 2> TTime2Vec;

        //! Enumeration of boolean types for three valued logic.
        enum ETernaryBool
        {
            E_False = 0,
            E_True = 1,
            E_Undetermined = 2
        };

    public:
        //! \brief Implements a simple test for whether a random
        //! process has a trend.
        //!
        //! DESCRIPTION:\n
        //! A process with a trend is defined as one which can be
        //! modeled as follows:
        //! <pre class="fragment">
        //!   \f$Y_i = f(t_i) + X_i\f$
        //! </pre>
        //! Here, \f$f(.)\f$ is some smoothly varying function and
        //! the \f$X_i = X\f$ are IID. The approach we take is to
        //! perform a variance ratio test on \f${ Y_i - f(t_i) }\f$
        //! verses \f${ Y_i }\f$. We are interested in the case that
        //! modeling f(.), using an exponentially decaying cubic
        //! regression with the current decay rate, will materially
        //! affect our results. We therefore test to see if the
        //! reduction in variance, as a proxy for the full model
        //! confidence bounds, is both large enough and statistically
        //! significant.
        class MATHS_EXPORT CTrend
        {
            public:
                //! The order of the trend regression.
                static const std::size_t ORDER = 3u;

            public:
                typedef CRegression::CLeastSquaresOnline<ORDER> TRegression;

            public:
                explicit CTrend(double decayRate = 0.0);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Set the decay rate.
                void decayRate(double decayRate);

                //! Age the state to account for \p time elapsed time.
                void propagateForwardsByTime(double time);

                //! Add a new value \p value at \p time.
                void add(core_t::TTime time, double value, double weight = 1.0);

                //! Capture the variance in the prediction error.
                void captureVariance(core_t::TTime time, double value, double weight = 1.0);

                //! Translate the trend by \p shift.
                void shift(double shift);

                //! Test whether there is a trend.
                bool test(void) const;

                //! Get the regression model of the trend.
                const TRegression &trend(void) const;

                //! Get the origin of the time coordinate system.
                core_t::TTime origin(void) const;

                //! Get the variance after removing the trend.
                double variance(void) const;

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                //! The smallest decrease in variance after removing the trend
                //! which is consider significant.
                static const double HAS_TREND_VARIANCE_RATIO;

            private:
                typedef CVectorNx1<double, 2> TVector;
                typedef CBasicStatistics::SSampleMeanVar<TVector>::TAccumulator TVectorMeanVarAccumulator;

            private:
                //! Get the time at which to evaluate the regression model
                //! of the trend.
                double time(core_t::TTime time) const;

            private:
                //! Controls the rate at which the regression model is aged.
                double m_DecayRate;

                //! The origin of the time coordinate system.
                core_t::TTime m_TimeOrigin;

                //! The current regression model.
                TRegression m_Trend;

                //! The values' mean and variance.
                TVectorMeanVarAccumulator m_Variances;
        };

        //! \brief Implements a test to detect step changes in the time
        //! series.
        //!
        //! DESCRIPTION:\n
        //! For time series with significant step changes it is best to
        //! use a piecewise constant additive model for the time series,
        //! i.e. we'd like ideally like to find a function \f$theta\f$
        //! by minimizing something like
        //! <pre class="fragment">
        //!   \f$\left \| Y-\theta \right \|_2^2+\lambda\left \| D\theta \right \|_1\f$
        //! </pre>
        //! where
        //! <pre class="fragment">
        //!   \f$[D]_{i,j} = \delta_{i,j} - \delta_{i+1,j}\f$
        //! </pre>
        //! where the value of \f$\lambda\f$ is tuned for the data set
        //! by say using an information criterion based on a probabilistic
        //! model \f$Y_i = \theta_i + N(0,\sigma_i)\f$. In practice we
        //! don't have the luxury of looking at the whole data set in
        //! order to decide the levels, we need a procedure which will
        //! decide relatively quickly after the fact if a significant
        //! shift in the values has taken place.
        //!
        //! If we imagine that values are sent to us in time order, then
        //! our task is to determine if a knot point in the function has
        //! occurred in the last fixed interval. We choose an interval
        //! such that the chance of two knot points is essentially zero,
        //! randomly down sample the function values and test to see if
        //! adding the optimum knot point both causes both a large and
        //! statistically significant decrease in the residual variance.
        class MATHS_EXPORT CStepChange
        {
            public:
                class MATHS_EXPORT CResult
                {
                    public:
                        CResult(void);
                        CResult(ETernaryBool foundShift, double level, double shift);

                        //! Get the test result.
                        ETernaryBool value(void) const;

                        //! The current level.
                        double level(void) const;

                        //! The shift in the level if any.
                        double shift(void) const;

                    private:
                        //! The result of the level shift test.
                        ETernaryBool m_FoundShift;

                        //! The current level.
                        double m_Level;

                        //! The shift in the level.
                        double m_Shift;
                };

            public:
                //! \param[in] bucketLength The bucketing interval.
                //! \param[in] n The number of values to sample.
                //! \param[in] p The probability with which to sample a value.
                //! \param[in] decayRate The rate at which information is lost.
                CStepChange(core_t::TTime bucketLength,
                            std::size_t n,
                            double p,
                            double decayRate = 0.0);

                //! Efficiently swap the contents of this and \p other.
                void swap(CStepChange &other);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Clear the samples.
                void clear(void);

                //! Set the decay rate.
                void decayRate(double decayRate);

                //! Age the state to account for \p time elapsed time.
                void propagateForwardsByTime(double time);

                //! Add a new value \p value at \p time.
                void add(core_t::TTime time, double value);

                //! Test whether there is a periodic trend.
                CResult captureVarianceAndTest(void);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                typedef boost::circular_buffer<CFloatStorage> TFloatCBuf;
                typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

            private:
                //! The random number generator used for sampling.
                CPRNG::CXorOShiro128Plus m_Rng;

                //! The rate at which variance is aged.
                double m_DecayRate;

                //! The bucketing interval.
                core_t::TTime m_BucketLength;

                //! The sample probability.
                CFloatStorage m_P;

                //! The count until the next sample.
                core_t::TTime m_NextSampleTime;

                //! The samples.
                TFloatCBuf m_Samples;

                //! The current level.
                double m_Level;

                //! The within level variance.
                TMeanAccumulator m_Variance;
        };

        //! \brief A low memory footprint randomized test for probability.
        //!
        //! This is based on the idea of random projection in a Hilbert
        //! space.
        //!
        //! If we choose a direction uniformly at random, \f$r\f$,
        //! verses in the subspace of periodic functions, \f$p\$, we
        //! expect, with probability 1, that the inner product with
        //! a periodic function with matched period, \$f\f$, will be
        //! larger. In particular, if we imagine taking the expectation
        //! over a each family of functions then with probability 1:
        //! <pre class="fragment">
        //!     \f$E\left[\frac{\|r^t f\|}{\|r\|}\right] < E\left[\frac{\|p^t f\|}{\|p\|}\right]\f$
        //! </pre>
        //!
        //! Therefore, if we sample independently many such random
        //! projections of the function and the function is periodic
        //! then we can test the for periodicity by comparing means.
        //! The variance of the means will tend to zero as the number
        //! of samples grows so the significance for rejecting the
        //! null hypothesis (that the function is a-periodic) will
        //! shrink to zero.
        class MATHS_EXPORT CRandomizedPeriodicity
        {
            public:
                //! The size of the projection sample coefficients
                static const std::size_t N = 5;

            public:
                CRandomizedPeriodicity(void);

                //! \name Persistence
                //@{
                //! Restore the static members by reading state from \p traverser.
                static bool staticsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist the static members by passing information to \p inserter.
                static void staticsAcceptPersistInserter(core::CStatePersistInserter &inserter);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;
                //@}

                //! Add a new value \p value at \p time.
                void add(core_t::TTime time, double value);

                //! Test whether there is a periodic trend.
                bool test(void) const;

                //! Reset the test static random vectors.
                //!
                //! \note For unit testing only.
                static void reset(void);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                typedef std::vector<double> TDoubleVec;
                typedef CVectorNx1<CFloatStorage, 2> TVector2;
                typedef CBasicStatistics::SSampleMean<TVector2>::TAccumulator TVector2MeanAccumulator;
                typedef CVectorNx1<CFloatStorage, 2*N> TVector2N;
                typedef CBasicStatistics::SSampleMean<TVector2N>::TAccumulator TVector2NMeanAccumulator;
                typedef atomic_t::atomic<core_t::TTime> TAtomicTime;

            private:
                //! The length over which the periodic random projection decoheres.
                static const core_t::TTime SAMPLE_INTERVAL;
                //! The time between day resample events.
                static const core_t::TTime DAY_RESAMPLE_INTERVAL;
                //! The time between week resample events.
                static const core_t::TTime WEEK_RESAMPLE_INTERVAL;
                //! The random number generator.
                static boost::random::mt19937_64 ms_Rng;
                //! The permutations daily projections.
                static TDoubleVec ms_DayRandomProjections[N];
                //! The daily periodic projections.
                static TDoubleVec ms_DayPeriodicProjections[N];
                //! The time at which we re-sampled day projections.
                static TAtomicTime ms_DayResampled;
                //! The permutations weekly projections.
                static TDoubleVec ms_WeekRandomProjections[N];
                //! The weekly periodic projections.
                static TDoubleVec ms_WeekPeriodicProjections[N];
                //! The time at which we re-sampled week projections.
                static TAtomicTime ms_WeekResampled;
                //! The mutex for protecting state update.
                static core::CMutex ms_Lock;

            private:
                //! Refresh \p projections and update \p statistics.
                static void updateStatistics(TVector2NMeanAccumulator &projections,
                                             TVector2MeanAccumulator &statistics);

                //! Re-sample the projections.
                static void resample(core_t::TTime time);

                //! Re-sample the specified projections.
                static void resample(core_t::TTime period,
                                     core_t::TTime resampleInterval,
                                     TDoubleVec (&periodicProjections)[N],
                                     TDoubleVec (&randomProjections)[N]);

            private:
                //! The day projections.
                TVector2NMeanAccumulator m_DayProjections;
                //! The sample mean of the square day projections.
                TVector2MeanAccumulator m_DayStatistics;
                //! The last time the day projections were updated.
                core_t::TTime m_DayRefreshedProjections;
                //! The week projections.
                TVector2NMeanAccumulator m_WeekProjections;
                //! The sample mean of the square week projections.
                TVector2MeanAccumulator m_WeekStatistics;
                //! The last time the day projections were updated.
                core_t::TTime m_WeekRefreshedProjections;

                friend class ::CTrendTestsTest;
        };

        //! \brief Implements test for defined periodic components.
        //!
        //! DESCRIPTION:\n
        //! A class to test whether there are specified periodic components
        //! in a time series. It looks for a short and long period (which
        //! should be a multiple of the short period) and also, optionally,
        //! a partition of the time series in to two disjoint periodic
        //! intervals. Tests include various forms of analysis of variance
        //! and a test of the amplitude.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This class uses float storage for the bucket mean values. This
        //! is because it is intended for use in cases where space is at a
        //! premium. *DO NOT* use floats unless doing so gives a significant
        //! overall space improvement to the *program* footprint. Note also
        //! that the interface to this class is double precision. If floats
        //! are used they should be used for storage only and transparent to
        //! the rest of the code base.
        class MATHS_EXPORT CPeriodicity
        {
            public:
                typedef core::CSmallVector<TTimeTimePr, 2> TTimeTimePr2Vec;

            public:
                //! Enumeration of the possible partition subsets.
                enum EInterval
                {
                    E_FullInterval   = 0x0,
                    E_FirstInterval  = 0x1,
                    E_SecondInterval = 0x2
                };

                //! Enumeration of the possible periods.
                enum EPeriod
                {
                    E_NoPeriod    = 0x0,
                    E_ShortPeriod = 0x1,
                    E_LongPeriod  = 0x2,
                    E_BothPeriods = 0x3
                };

                //! \brief Represents the result of running a test.
                class MATHS_EXPORT CResult : boost::equality_comparable<CResult,
                                             boost::addable<CResult> >
                {
                    public:
                        CResult(void);

                        //! Initialize by reading state from \p traverser.
                        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                        //! Persist state by passing information to \p inserter.
                        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                        //! Check if this is equal to \p other.
                        bool operator==(const CResult &other) const;

                        //! Sets to the union of the periodic components present.
                        //!
                        //! \warning This only makes sense if the this and the other result
                        //! share the start of the partition time.
                        const CResult &operator+=(const CResult &other);

                        //! Add \p period for \p interval iff \p hasPeriod is true.
                        void addIf(bool hasPeriod, EInterval interval, EPeriod period);

                        //! Set the start time of the trend partition pattern to \p time.
                        void startOfPartition(core_t::TTime time);

                        //! Get the start time of the trend partition pattern.
                        core_t::TTime startOfPartition(void) const;

                        //! Check if there are any periodic components.
                        bool periodic(void) const;

                        //! The periods short, long or both for \p partition.
                        EPeriod periods(EInterval interval) const;

                        //! Get an index in to an array of trends for \p partition
                        //! and \p period.
                        std::size_t index(EInterval partition, EPeriod period) const;

                        //! Get a human readable description of this result.
                        std::string print(const std::string (&intervals)[2],
                                          const std::string (&periods)[2]) const;

                        //! Get a checksum for this object.
                        uint64_t checksum(void) const;

                    private:
                        //! Set to the start of the partition.
                        core_t::TTime m_StartOfPartition;

                        //! The periods present in the trend.
                        int m_Periods;
                };

            public:
                //! An empty collection of bucket values.
                static const TFloatMeanAccumulatorVec NO_BUCKET_VALUES;

            public:
                explicit CPeriodicity(double decayRate = 0.0);

                //! Efficiently swap the contents of two tests.
                void swap(CPeriodicity &other);

                //! \name Persistence
                //@{
                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;
                //@}

                //! Initialize the bucket values.
                bool initialize(core_t::TTime bucketLength,
                                core_t::TTime window,
                                TTime2Vec periods,
                                TTime2Vec partition,
                                const TFloatMeanAccumulatorVec &initial = NO_BUCKET_VALUES);

                //! Check if the test is initialized.
                bool initialized(void) const;

                //! Set the decay rate.
                void decayRate(double decayRate);

                //! Age the bucket values to account for \p time elapsed time.
                void propagateForwardsByTime(double time);

                //! Add \p value at \p time.
                void add(core_t::TTime time, double value, double weight = 1.0);

                //! Get the periods under test.
                const TTime2Vec &periods(void) const;

                //! Get the fraction of populated test slots
                double populatedRatio(void) const;

                //! Check we've seen sufficient data to test accurately.
                bool seenSufficientData(void) const;

                //! Check if there periodic components.
                CResult test(void) const;

                //! Get the periodic trends corresponding to \p periods.
                void trends(const CResult &periods,
                            TTimeTimePrMeanVarAccumulatorPrVec (&result)[6]) const;

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

                //! Debug the memory used by this object.
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by this object.
                std::size_t memoryUsage(void) const;

            private:
                typedef std::vector<TMeanVarAccumulator> TMeanVarAccumulatorVec;

            private:
                //! The permitted bucket lengths in seconds.
                static const core_t::TTime PERMITTED_BUCKET_LENGTHS[12];
                //! The minimum proportion of populated buckets for an
                //! accurate test.
                static const double ACCURATE_TEST_POPULATED_FRACTION;
                //! The minimum coefficient of variation for which we'll
                //! identify a trend.
                static const double MINIMUM_COEFFICIENT_OF_VARIATION;
                //! The threshold, for the variance ratio, used to test
                //! for the presence of a period.
                static const double HAS_PERIOD_VARIANCE_RATIO;
                //! The threshold, for the variance ratio, used to test
                //! for the presence of a periodic partition.
                static const double HAS_PARTITION_VARIANCE_RATIO;
                //! The threshold for the maximum amplitude of the trend,
                //! in standard deviations of the trend residuals, used to
                //! test for the presence of periodic spikes.
                static const double HAS_PERIOD_AMPLITUDE_IN_SDS;
                //! The minimum permitted autocorrelation for a signal to
                //! be periodic.
                static const double MINIMUM_AUTOCORRELATION;

            private:
                //! \brief A collection of test statistics.
                //!
                //! DESCRIPTION:\n
                //! Wraps up the test statistics and some convenience functions
                //! for computing test statistics.
                struct MATHS_EXPORT SStatistics
                {
                    //! Initialize the test statistics.
                    bool initialize(const TFloatMeanAccumulatorVec &values,
                                    core_t::TTime bucketLength,
                                    const TTimeTimePr2Vec &windows,
                                    const TTime2Vec &periods,
                                    const TTime2Vec &partition,
                                    double populated,
                                    double count);

                    //! Commit the statistics related to the period under test.
                    void commitCandidates(bool commit);

                    //! Check if we have enough data to test for the short period.
                    bool canTestForShort(core_t::TTime bucketLength) const;
                    //! Check if we have enough data to test for the partition.
                    bool canTestForPartition(core_t::TTime bucketLength) const;
                    //! Check if we have enough data to test for the long period.
                    bool canTestForLong(core_t::TTime bucketLength) const;

                    //! Compute the F test statistic for the unexplained variance.
                    double F(void) const;
                    //! Get the minimum permitted amplitude.
                    double amplitudeThreshold(void) const;
                    //! Get the maximum permitted unexplained variance.
                    double varianceThreshold(void) const;

                    //! The periods being tested.
                    TTime2Vec s_Periods;
                    //! The number of time buckets in the short period.
                    double s_BucketsPerShort;
                    //! The number of time buckets in the long period.
                    double s_BucketsPerLong;
                    //! The number of populated buckets.
                    double s_PopulatedBuckets;
                    //! The average number of values received per bucket.
                    double s_ValuesPerBucket;
                    //! The unexplained variance after subtracting the candidate
                    //! periodic component.
                    double s_CandidateUnexplainedVariance;
                    //! The degrees of freedom in the unexplained variance for
                    //! the candidate periodic component.
                    double s_CandidateDegreesOfFreedom;
                    //! The unexplained variance after subtracting the currently
                    //! accepted periodic component.
                    double s_UnexplainedVariance;
                    //! The degrees of freedom in the unexplained variance for
                    //! the currently accepted periodic component.
                    double s_DegreesOfFreedom;
                    //! The time of the start of the partition pattern.
                    core_t::TTime s_StartOfPartition;
                    //! The shortest interval in the partition pattern.
                    core_t::TTime s_ShortestInterval;
                };

                //! Test to see if the unexplained variance is significantly
                //! reduced by a component with \p period.
                bool testComponentUsingUnexplainedVariance(std::size_t period, SStatistics &statistics) const;

                //! Test to see if \p trend contributes significant variance.
                bool testComponentUsingExplainedVariance(const TTimeTimePrMeanVarAccumulatorPrVec &trend,
                                                         const TTimeTimePrMeanVarAccumulatorPrVec &remainderTrend,
                                                         const TTimeTimePr2Vec &windows,
                                                         const SStatistics &statistics) const;

                //! Test to see if the amplitude of the component with \p period
                //! is significant w.r.t. the variance.
                bool testComponentUsingAmplitude(std::size_t period, const SStatistics &statistics) const;

                //! Test to see if partitioning the data into a periodic pattern
                //! significantly reduces the unexplained variance.
                bool testForPartitionUsingUnexplainedVariance(SStatistics &statistics) const;

                //! Test the autocorrelation of the component with \p period.
                bool testAutocorrelation(std::size_t period, const SStatistics &statistics) const;

                //! Get the mean variance assuming there is a partition of the
                //! trend into two intervals of the long period.
                void partitionVariance(double meanCount,
                                       double &variance,
                                       core_t::TTime &startOfPartition) const;

                //! Get the total measurement count.
                double count(void) const;

                //! Get the trend(s) for \p periods.
                void periodicBucketing(int periods,
                                       const TTimeTimePr2Vec &windows,
                                       TTimeTimePrMeanVarAccumulatorPrVec &shortTrend,
                                       TTimeTimePrMeanVarAccumulatorPrVec &longTrend) const;

                //! Get the trend for period \p period.
                void periodicBucketing(core_t::TTime period,
                                       const TTimeTimePr2Vec &windows,
                                       TTimeTimePrMeanVarAccumulatorPrVec &trend) const;

                //! Get the decomposition into a short and long period trend.
                void periodicBucketing(const TTimeTimePr2Vec &windows,
                                       TTimeTimePrMeanVarAccumulatorPrVec &shortTrend,
                                       TTimeTimePrMeanVarAccumulatorPrVec &longTrend) const;

                //! Compute the windows for \p interval and \p start.
                TTimeTimePr2Vec windows(EInterval interval, core_t::TTime start = 0) const;

                //! Initialize the buckets in \p trend.
                void initializeBuckets(std::size_t period,
                                       const TTimeTimePr2Vec &windows,
                                       TTimeTimePrMeanVarAccumulatorPrVec &trend) const;

            private:
                //! The rate at which information is aged out of the bucket values.
                double m_DecayRate;

                //! The bucketing interval.
                core_t::TTime m_BucketLength;

                //! The window for which to maintain bucket values.
                core_t::TTime m_Window;

                //! The candidate periods.
                TTime2Vec m_Periods;

                //! The candidate partition of the long period.
                TTime2Vec m_Partition;

                //! The mean bucket values.
                TFloatMeanAccumulatorVec m_BucketValues;
        };

        //! \brief Implements a test that scans through a range of frequencies
        //! looking for different periodic components in the data.
        //!
        //! DESCRIPTION:\n
        //! This performs a scan for increasingly low frequency periodic
        //! components maintaining a fixed size buffer. We find the most
        //! promising candidate periods using linear autocorrelation and
        //! then test them using our standard periodicity test.
        //!
        //! In order to maintain a fixed space the bucket length is increased
        //! as soon as the observed data span exceeds the test size multiplied
        //! by the current bucket span.
        class MATHS_EXPORT CScanningPeriodicity
        {
            public:
                typedef std::pair<CPeriodicity, CPeriodicity::CResult> TPeriodicityResultPr;

            public:
                CScanningPeriodicity(std::size_t size, core_t::TTime bucketLength);

                //! Efficiently swap the contents of this and \p other.
                void swap(CScanningPeriodicity &other);

                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Set the start time to \p time.
                void initialize(core_t::TTime time);

                //! Add \p value at \p time.
                void add(core_t::TTime time, double value, double weight = 1.0);

                //! Check if we need to compress by increasing the bucket span.
                bool needToCompress(core_t::TTime) const;

                //! Check if there periodic components.
                TPeriodicityResultPr test(void) const;

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                //! Resize the bucket values to \p size.
                void resize(std::size_t size, TFloatMeanAccumulatorVec &values) const;

                //! Compute the mean of the autocorrelation at integer multiples
                //! of \p period.
                double meanForPeriodicOffsets(const TDoubleVec &correlations, std::size_t period) const;

                //! Correct the autocorrelation calculated on padded data.
                double correctForPad(double correlation, std::size_t offset) const;

            private:
                //! The bucket length.
                core_t::TTime m_BucketLength;

                //! The time of the first data point.
                core_t::TTime m_StartTime;

                //! The bucket values.
                TFloatMeanAccumulatorVec m_BucketValues;
        };

        //! \name Daily weekly custom test.
        //!@{
        //! Selects the day period.
        static const CPeriodicity::EPeriod   SELECT_DAILY;
        //! Selects the week period.
        static const CPeriodicity::EPeriod   SELECT_WEEKLY;

        //! Selects the full week.
        static const CPeriodicity::EInterval SELECT_WEEK;
        //! Selects the weekend interval of a week.
        static const CPeriodicity::EInterval SELECT_WEEKEND;
        //! Selects the weekdays interval of a week.
        static const CPeriodicity::EInterval SELECT_WEEKDAYS;

        //! The names of the periodic components.
        static const std::string DAILY_WEEKLY_NAMES[2];
        //! The names of the partition intervals.
        static const std::string WEEKEND_WEEKDAY_NAMES[2];

        //! Get a test for daily and weekly periodic components.
        static CPeriodicity *dailyAndWeekly(core_t::TTime bucketLength, double decayRate = 0.0);

        //! Print the result of a test for daily and weekly periodicity.
        static std::string printDailyAndWeekly(const CPeriodicity::CResult &result);
        //@}

        //! \name Utilities
        //@{
        //! Compute the discrete autocorrelation of \p values for the offset
        //! \p offset.
        //!
        //! This is just
        //! <pre class="fragment">
        //!   \f$\(\frac{1}{(n-k)\sigma^2}\sum_{k=1}^{n-k}{(f(k) - \mu)(f(k+p) - \mu)}\)\f$
        //! </pre>
        //!
        //! \param[in] offset The offset as a distance in \p values.
        //! \param[in] values The values for which to compute the autocorrelation.
        static double autocorrelation(std::size_t offset, const TFloatMeanAccumulatorVec &values);

        //! Get linear autocorrelations for all offsets up to the length of
        //! \p values.
        //!
        //! \param[in] values The values for which to compute autocorrelation.
        //! \param[in] result Filled in with the autocorrelations of \p values for
        //! offsets 1, 2, ..., length \p values - 1.
        static void autocorrelations(const TFloatMeanAccumulatorVec &values, TDoubleVec &result);
        //@}
};

}
}

#endif // INCLUDED_prelert_maths_CTrendTests_h
