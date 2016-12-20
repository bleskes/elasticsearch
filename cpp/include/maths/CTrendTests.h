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
        typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
        typedef std::vector<TMeanAccumulator> TMeanAccumulatorVec;
        typedef CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
        typedef std::vector<TMeanVarAccumulator> TMeanVarAccumulatorVec;
        typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
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
        //! the \f$X_i = X\f$ are IID. Clearly, \f$Y_i\f$ is not a
        //! stationary process, so we could use some sort of regression
        //! based test such as augmented Dickey-Fuller. However, that
        //! test requires maintaining regression coefficients and the
        //! idea of this test is to reduce the memory footprint when
        //! the process doesn't have a trend, so that we don't bother
        //! to try and estimate the trend. In particular, we are
        //! interested for anomaly detection in determining when
        //! \f$f(.)\f$ has little impact on our estimation of the
        //! distribution of \f$X\f$.
        //!
        //! A good assumption for our use case is that for small
        //! \f$n\f$ then \f$f(.)\f$ is approximately constant on
        //! the interval \f$[t_i, t_{i+n}]\f$. Therefore, if we
        //! compute the sample variance \f$V_i\f$ of the samples
        //! \f$\{Y_j : j \in [i, i+n)\}\f$ for \f$i \in \{0, n, 2n, ...\}\f$
        //! and \f$V\f$ for all the samples and if:
        //! <pre class="fragment">
        //!   \f$\displaystyle T = \frac{V_i}{V}\f$
        //! </pre>
        //! is close to one then \f$f(.)\f$ has little impact
        //! on the estimate of the spread of \f$X\f$.
        //!
        //! Since the distributions we will try an fit to \f$X\f$
        //! have a mean parameter, from the point of view of detecting
        //! an anomaly, it doesn't matter if we model \f$X\f$ or
        //! \f$X - E[X]\f$ and test \f$y\f$ or \f$y - E[X]\f$,
        //! respectively. Therefore, if and only if \f$T\f$ is
        //! small should we try and estimate the trend. This is
        //! the basis of the test hasTrend.
        class MATHS_EXPORT CTrend
        {
            public:
                //! Create by reading state from \p traverser.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Add the point \p to the test statistics.
                void add(double x, double weight = 1.0);

                //! Age the test statistics.
                //!
                //! \note \p factor should be in the range (0,1].
                void age(double factor);

                //! Check if the process is approximately stationary.
                //!
                //! \note This can return true, false of undetermined.
                //! It returns undetermined in the case that there is
                //! not a high degree of confidence that there is or
                //! isn't a trend.
                ETernaryBool hasTrend(void) const;

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                //! The number of points in the local variance.
                static const double LOCAL_SIZE;
                //! The threshold, for the variance ratio, used to
                //! test for the presence of a trend.
                static const double HAS_TREND_VARIANCE_RATIO;
                //! The margin relative to TREND_VARIANCE_RATIO at
                //! which the test positively identifies no trend.
                static const double TEST_HYSTERESIS;

            private:
                //! The current local variance estimate.
                TMeanVarAccumulator m_LocalVariance;
                //! The mean local variance.
                TMeanAccumulator m_MeanLocalVariance;
                //! The current period variance estimate.
                TMeanVarAccumulator m_Variance;
        };

        //! A low memory footprint randomized test for probability.
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

                //! Add a new value \p value at \p time.
                void add(core_t::TTime time, double value);

                //! Test whether there is a periodic trend.
                bool test(void) const;

                //! Reset the test static random vectors.
                //!
                //! \note For unit testing only.
                static void reset(void);

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

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed) const;

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

                        //! Compute this object's checksum.
                        uint64_t checksum(void) const;

                    private:
                        //! Set to the start of the partition.
                        core_t::TTime m_StartOfPartition;

                        //! The periods present in the trend.
                        int m_Periods;
                };

            public:
                explicit CPeriodicity(double decayRate = 0.0);

                //! Efficiently swap the contents of two tests.
                void swap(CPeriodicity &other);

                //! \name Persistence
                //@{
                //! Initialize by reading state from \p traverser.
                bool acceptRestoreTraverser(double decayRate, core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;
                //@}

                //! Initialize the bucket values.
                bool initialize(core_t::TTime bucketLength,
                                core_t::TTime window,
                                TTime2Vec periods,
                                TTime2Vec partition);

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

                //! Get a checksum for this test.
                uint64_t checksum(uint64_t seed) const;

                //! Debug the memory used by this component.
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by this component.
                std::size_t memoryUsage(void) const;

            private:
                //! The permitted bucket lengths in seconds.
                static const core_t::TTime PERMITTED_BUCKET_LENGTHS[12];
                //! The minimum proportion of populated buckets for an
                //! accurate test.
                static const double ACCURATE_TEST_POPULATED_FRACTION;
                //! The variance confidence intervals used in the tests.
                static const double CONFIDENCE_INTERVAL;
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
                //! The maximum significance of unexplained to explained
                //! variance in the variance ratio F-test.
                static const double MAXIMUM_VARIANCE_RATIO_SIGNIFICANCE;
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
        //!   \(\frac{1}{(n-k)\sigma^2}\sum_{k=1}{n-k}{(f(k) - \mu)(f(k+p) - \mu)}\)
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
