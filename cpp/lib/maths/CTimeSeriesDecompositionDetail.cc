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

#include <maths/CTimeSeriesDecompositionDetail.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/Constants.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CBasicStatistics.h>
#include <maths/CChecksum.h>
#include <maths/CIntegerTools.h>
#include <maths/CSeasonalComponentAdaptiveBucketing.h>
#include <maths/CTools.h>

#include <boost/config.hpp>
#include <boost/bind.hpp>
#include <boost/config.hpp>
#include <boost/container/flat_map.hpp>
#include <boost/container/flat_set.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/range.hpp>

#include <string>
#include <vector>

namespace ml
{
namespace maths
{
namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<TSizeVec> TSizeVecVec;
typedef std::vector<std::string> TStrVec;
typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef std::vector<CSeasonalComponent> TComponentVec;
typedef std::vector<CSeasonalComponent*> TComponentPtrVec;
typedef CTimeSeriesDecompositionDetail::CTrendCRef TTrendCRef;

const core_t::TTime DAY = core::constants::DAY;
const core_t::TTime WEEK = core::constants::WEEK;
const core_t::TTime WEEKEND = core::constants::WEEKEND;

//! Scale \p interval to account for \p bucketLength.
core_t::TTime scale(core_t::TTime interval, core_t::TTime bucketLength)
{
    return interval * std::max(bucketLength / core::constants::HOUR, core_t::TTime(1));
}

//! Compute the mean value of the \p components which overlap \p time.
double meanValueAt(core_t::TTime time, const TComponentVec &components)
{
    double result = 0.0;
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        if (components[i].initialized() && components[i].time().inWindow(time))
        {
            result += components[i].meanValue();
        }
    }
    return result;
}

//! Compute the mean of \p mean of \p components.
template<typename MEAN_FUNCTION>
double meanOf(MEAN_FUNCTION mean, const TComponentVec &components)
{
    typedef boost::container::flat_map<TTimeTimePr, double> TTimeTimePrDoubleFMap;
    typedef TTimeTimePrDoubleFMap::const_iterator TTimeTimePrDoubleFMapCItr;

    TTimeTimePrDoubleFMap means;
    means.reserve(components.size());
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        if (components[i].initialized())
        {
            core_t::TTime start = components[i].time().windowStart();
            core_t::TTime end   = components[i].time().windowEnd();
            means[std::make_pair(start, end)] += (components[i].*mean)();
        }
    }

    TMeanAccumulator result;
    for (TTimeTimePrDoubleFMapCItr i = means.begin(); i != means.end(); ++i)
    {
        result.add(i->second, i->first.second - i->first.first);
    }

    return CBasicStatistics::mean(result);
}

//! Compute the values to add to the trend and each component.
//!
//! \param[in] trend The long term trend.
//! \param[in] components The seasonal components.
//! \param[in] deltas The delta offset to apply to the difference
//! between each component value and its mean, used to minimize
//! slope in the longer periods.
//! \param[in] time The time of value to decompose.
//! \param[in,out] decomposition Updated to contain the value to
//! add to each by component.
void decompose(const TTrendCRef &trend,
               const TComponentPtrVec &components,
               const TDoubleVec &deltas,
               core_t::TTime time,
               TDoubleVec &decomposition)
{
    std::size_t n = components.size();

    double x0 = CBasicStatistics::mean(trend.prediction(time, 0.0));
    TDoubleVec x(n);
    double xhat = x0;
    double z    = ::fabs(x0);
    for (std::size_t i = 0u; i < n; ++i)
    {
        x[i]  = CBasicStatistics::mean(components[i]->value(time, 0.0));
        xhat += x[i];
        z    += ::fabs(x[i]);
    }
    if (z == 0.0)
    {
        z = static_cast<double>(n + 1);
        decomposition[0] = (decomposition[0] - xhat) / z;
        double lastDelta = 0.0;
        for (std::size_t i = 0u; i < n; ++i)
        {
            double d = deltas[i] - lastDelta;
            lastDelta = deltas[i];
            decomposition[i + 1] = (decomposition[i + 1] - xhat) / z + d;
        }
    }
    else
    {
        decomposition[0] = x0 + (decomposition[0] - xhat) * ::fabs(x0) / z;
        double lastDelta = 0.0;
        for (std::size_t i = 0u; i < n; ++i)
        {
            double d = deltas[i] - lastDelta;
            lastDelta = deltas[i];
            decomposition[i + 1] = x[i] + (decomposition[i + 1] - xhat) * ::fabs(x[i]) / z + d;
        }
    }
}

//! Convert the propagation decay rate into the corresponding regular
//! periodicity test decay rate.
double regularTestDecayRate(double decayRate)
{
    return CSeasonalComponent::TTime::scaleDecayRate(
               decayRate, CSeasonalComponentAdaptiveBucketing::timescale(), WEEK);
}

//! Get the time of \p time suitable for use in a trend regression model.
double regressionTime(core_t::TTime time, core_t::TTime origin)
{
    return static_cast<double>(time - origin) / static_cast<double>(WEEK);
}

//! Check whether to test at \p time.
bool shouldTestImpl(core_t::TTime time, core_t::TTime interval, core_t::TTime &nextTestTime)
{
    if (time >= nextTestTime)
    {
        nextTestTime = CIntegerTools::ceil(time + 1, interval);
        return true;
    }
    return false;
}

//! Get the transition function in vector form.
template<std::size_t M, std::size_t N>
TSizeVecVec function(const std::size_t (&f)[M][N])
{
    TSizeVecVec result(M);
    for (std::size_t i = 0u; i < M; ++i)
    {
        result[i].assign(f[i], f[i] + N);
    }
    return result;
}

// Long Term Trend Test State Machine

// States
const std::size_t LT_INITIAL     = 0;
const std::size_t LT_LEARN       = 1;
const std::size_t LT_TEST        = 2;
const std::size_t LT_NOT_TESTING = 3;
const std::size_t LT_ERROR       = 4;
const std::string LT_STATES_[] =
    {
        "INITIAL",
        "LEARN",
        "TEST",
        "NOT_TESTING",
        "ERROR"
    };
const TStrVec LT_STATES(boost::begin(LT_STATES_), boost::end(LT_STATES_));
// Alphabet
const std::size_t LT_NEW_VALUE   = 0;
const std::size_t LT_START_TEST  = 1;
const std::size_t LT_FINISH_TEST = 2;
const std::size_t LT_RESET       = 3;
const std::string LT_ALPHABET_[] =
    {
        "NEW_VALUE",
        "START_TEST",
        "FINISH_TEST",
        "RESET"
    };
const TStrVec LT_ALPHABET(boost::begin(LT_ALPHABET_), boost::end(LT_ALPHABET_));
// Transition Function
const std::size_t LT_TRANSITION_FUNCTION_[][5] =
    {
        { LT_LEARN,       LT_LEARN,       LT_TEST,        LT_NOT_TESTING, LT_ERROR   },
        { LT_ERROR,       LT_TEST,        LT_TEST,        LT_NOT_TESTING, LT_ERROR   },
        { LT_NOT_TESTING, LT_NOT_TESTING, LT_NOT_TESTING, LT_NOT_TESTING, LT_ERROR   },
        { LT_INITIAL,     LT_INITIAL,     LT_INITIAL,     LT_INITIAL,     LT_INITIAL }
    };
const TSizeVecVec LT_TRANSITION_FUNCTION(function(LT_TRANSITION_FUNCTION_));

// Daily + Weekly Test State Machine

// States
const std::size_t DW_INITIAL      = 0;
const std::size_t DW_SMALL_TEST   = 1;
const std::size_t DW_REGULAR_TEST = 2;
const std::size_t DW_NOT_TESTING  = 3;
const std::size_t DW_ERROR        = 4;
const std::string DW_STATES_[] =
    {
        "INITIAL",
        "SMALL_TEST",
        "REGULAR_TEST",
        "NOT_TESTING",
        "ERROR"
    };
const TStrVec DW_STATES(boost::begin(DW_STATES_), boost::end(DW_STATES_));
// Alphabet
const std::size_t DW_NEW_VALUE                    = 0;
const std::size_t DW_SMALL_TEST_TRUE              = 1;
const std::size_t DW_REGULAR_TEST_TIMED_OUT       = 2;
const std::size_t DW_FINISH_TEST                  = 3;
const std::size_t DW_DETECTED_INCOMPATIBLE_PERIOD = 4;
const std::size_t DW_RESET                        = 5;
const std::string DW_ALPHABET_[] =
    {
        "NEW_VALUE",
        "SMALL_TEST_TRUE",
        "REGULAR_TEST_TIMED_OUT",
        "FINISH_TEST",
        "DETECTED_INCOMPATIBLE_PERIOD",
        "RESET"
    };
const TStrVec DW_ALPHABET(boost::begin(DW_ALPHABET_), boost::end(DW_ALPHABET_));
// Transition Function
const std::size_t DW_TRANSITION_FUNCTION_[][5] =
    {
        { DW_REGULAR_TEST, DW_SMALL_TEST,   DW_REGULAR_TEST, DW_NOT_TESTING, DW_ERROR   },
        { DW_ERROR,        DW_REGULAR_TEST, DW_ERROR,        DW_NOT_TESTING, DW_ERROR   },
        { DW_ERROR,        DW_ERROR,        DW_SMALL_TEST,   DW_NOT_TESTING, DW_ERROR   },
        { DW_ERROR,        DW_ERROR,        DW_NOT_TESTING,  DW_NOT_TESTING, DW_ERROR   },
        { DW_NOT_TESTING,  DW_NOT_TESTING,  DW_NOT_TESTING,  DW_NOT_TESTING, DW_ERROR   },
        { DW_INITIAL,      DW_INITIAL,      DW_INITIAL,      DW_NOT_TESTING, DW_INITIAL }
    };
const TSizeVecVec DW_TRANSITION_FUNCTION(function(DW_TRANSITION_FUNCTION_));

// Components State Machine

// States
const std::size_t SC_NEW_COMPONENTS      = 0;
const std::size_t SC_NORMAL              = 1;
const std::size_t SC_FORECASTING         = 2;
const std::size_t SC_DISABLED            = 3;
const std::size_t SC_ERROR               = 4;
const std::string SC_STATES_[] =
    {
        "NEW_COMPONENTS",
        "NORMAL",
        "FORECASTING",
        "DISABLED",
        "ERROR"
    };
const TStrVec SC_STATES(boost::begin(SC_STATES_), boost::end(SC_STATES_));
// Alphabet
const std::size_t SC_ADDED_COMPONENTS     = 0;
const std::size_t SC_INTERPOLATED         = 1;
const std::size_t SC_FORECAST             = 2;
const std::size_t SC_RESET                = 3;
const std::string SC_ALPHABET_[] =
    {
        "ADDED_COMPONENTS",
        "INTERPOLATED",
        "FORECAST",
        "RESET"
    };
const TStrVec SC_ALPHABET(boost::begin(SC_ALPHABET_), boost::end(SC_ALPHABET_));
// Transition Function
const std::size_t SC_TRANSITION_FUNCTION_[][5] =
    {
        { SC_NEW_COMPONENTS, SC_NEW_COMPONENTS, SC_ERROR,       SC_DISABLED, SC_ERROR  },
        { SC_NORMAL,         SC_NORMAL,         SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_ERROR,          SC_FORECASTING,    SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_NORMAL,         SC_NORMAL,         SC_NORMAL,      SC_NORMAL,   SC_NORMAL }
    };
const TSizeVecVec SC_TRANSITION_FUNCTION(function(SC_TRANSITION_FUNCTION_));

// Long Term Trend Test Tags
const std::string MACHINE_TAG("a");
const std::string NEXT_TEST_TIME_TAG("b");
const std::string TEST_TAG("c");

// Daily+Weekly Test Tags
//const std::string MACHINE_TAG("a");
//const std::string NEXT_TEST_TIME_TAG("b");
const std::string STARTED_REGULAR_TEST_TAG("c");
const std::string TIME_OUT_REGULAR_TEST_TAG("d");
const std::string REGULAR_TEST_TAG("e");
const std::string SMALL_TEST_TAG("f");
const std::string PERIODS_TAG("g");

// Level Shift Test Tags
//const std::string MACHINE_TAG("a");
//const std::string NEXT_TEST_TIME_TAG("b");
//const std::string TEST_TAG("c");
const std::string START_TESTING_TIME_TAG("d");

// Seasonal Components Tags
//const std::string MACHINE_TAG("a");
const std::string LONGEST_PERIOD_TAG("b");
const std::string LAST_INTERPOLATED_TIME_TAG("c");
const std::string NEXT_INTERPOLATE_TIME_TAG("d");
const std::string TREND_TAG("e");
const std::string COMPONENTS_TAG("f");
const std::string COMPONENT_TAG("g");
const std::string REGRESSION_TAG("h");
const std::string VARIANCE_TAG("i");
const std::string TIME_ORIGIN_TAG("j");
const std::string HISTORY_LENGTH_TAG("k");

const core_t::TTime FORECASTING_INTERPOLATE_INTERVAL(core::constants::HOUR);
const core_t::TTime FORECASTING_PROPAGATION_FORWARDS_BY_TIME_INTERVAL(core::constants::HOUR);
const core_t::TTime FOREVER = boost::numeric::bounds<core_t::TTime>::highest();
const TTrendCRef NO_TREND;
const TComponentVec NO_COMPONENTS;

}

//////// SMessage ////////

CTimeSeriesDecompositionDetail::SMessage::SMessage(void) : s_Time() {}
CTimeSeriesDecompositionDetail::SMessage::SMessage(core_t::TTime time) : s_Time(time) {}

//////// SAddValueMessage ////////

CTimeSeriesDecompositionDetail::SAddValueMessage::SAddValueMessage(core_t::TTime time,
                                                                   double value,
                                                                   const TWeightStyleVec &weightStyles,
                                                                   const TDouble4Vec &weights,
                                                                   double trend,
                                                                   double seasonal) :
        SMessage(time),
        s_Value(value),
        s_WeightStyles(weightStyles),
        s_Weights(weights),
        s_Trend(trend),
        s_Seasonal(seasonal)
{}

//////// SDetectedTrendMessage ////////

CTimeSeriesDecompositionDetail::SDetectedTrendMessage::SDetectedTrendMessage(core_t::TTime time, const CTrendTests::CTrend &test) :
        SMessage(time),
        s_Test(test)
{}

//////// SDetectedPeriodicMessage ////////

CTimeSeriesDecompositionDetail::SDetectedPeriodicMessage::SDetectedPeriodicMessage(core_t::TTime time,
                                                                                   const CTrendTests::CPeriodicity::CResult &result,
                                                                                   const CTrendTests::CPeriodicity &test) :
        SMessage(time), s_Result(result), s_Test(test)
{}

//////// SDiscardPeriodicMessage ////////

CTimeSeriesDecompositionDetail::SDiscardPeriodicMessage::SDiscardPeriodicMessage(core_t::TTime time,
                                                                                 const CTrendTests::CPeriodicity &test) :
        SMessage(time), s_Test(test)
{}

//////// CHandler ////////

CTimeSeriesDecompositionDetail::CHandler::CHandler(void) : m_Mediator(0) {}
CTimeSeriesDecompositionDetail::CHandler::~CHandler(void) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SAddValueMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDetectedTrendMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDetectedPeriodicMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDiscardPeriodicMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::mediator(CMediator *mediator)
{
    m_Mediator = mediator;
}

CTimeSeriesDecompositionDetail::CMediator *CTimeSeriesDecompositionDetail::CHandler::mediator(void) const
{
    return m_Mediator;
}

//////// CMediator ////////

template<typename M>
void CTimeSeriesDecompositionDetail::CMediator::forward(const M &message) const
{
    for (std::size_t i = 0u; i < m_Handlers.size(); ++i)
    {
        m_Handlers[i]->handle(message);
    }
}

void CTimeSeriesDecompositionDetail::CMediator::registerHandler(CHandler &handler)
{
    m_Handlers.push_back(&handler);
    handler.mediator(this);
}

//////// CLongTermTrendTest ////////

CTimeSeriesDecompositionDetail::CLongTermTrendTest::CLongTermTrendTest(double decayRate) :
        m_Machine(core::CStateMachine::create(LT_ALPHABET, LT_STATES,
                                              LT_TRANSITION_FUNCTION,
                                              LT_INITIAL)),
        m_MaximumDecayRate(decayRate),
        m_NextTestTime(),
        m_Test(new CTrendTests::CTrend(decayRate))
{}

CTimeSeriesDecompositionDetail::CLongTermTrendTest::CLongTermTrendTest(const CLongTermTrendTest &other) :
        m_Machine(core::CStateMachine::create(LT_ALPHABET, LT_STATES,
                                              LT_TRANSITION_FUNCTION,
                                              LT_INITIAL)),
        m_MaximumDecayRate(other.m_MaximumDecayRate),
        m_NextTestTime(other.m_NextTestTime),
        m_Test(other.m_Test ? new CTrendTests::CTrend(*other.m_Test) : 0)
{}

bool CTimeSeriesDecompositionDetail::CLongTermTrendTest::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(MACHINE_TAG, traverser.traverseSubLevel(
                                 boost::bind(&core::CStateMachine::acceptRestoreTraverser, &m_Machine, _1)));
        RESTORE_BUILT_IN(NEXT_TEST_TIME_TAG, m_NextTestTime)
        RESTORE_SETUP_TEARDOWN(TEST_TAG,
                               m_Test.reset(new CTrendTests::CTrend(m_MaximumDecayRate)),
                               traverser.traverseSubLevel(
                                   boost::bind(&CTrendTests::CTrend::acceptRestoreTraverser, m_Test.get(), _1)),
                               /**/)
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(MACHINE_TAG, boost::bind(&core::CStateMachine::acceptPersistInserter, &m_Machine, _1));
    inserter.insertValue(NEXT_TEST_TIME_TAG, m_NextTestTime);
    if (m_Test)
    {
        inserter.insertLevel(TEST_TAG, boost::bind(&CTrendTests::CTrend::acceptPersistInserter, m_Test.get(), _1));
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::swap(CLongTermTrendTest &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_MaximumDecayRate, other.m_MaximumDecayRate);
    std::swap(m_NextTestTime, other.m_NextTestTime);
    m_Test.swap(other.m_Test);
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::handle(const SAddValueMessage &message)
{
    core_t::TTime time = message.s_Time;
    double value = message.s_Value - message.s_Seasonal;
    double count = maths_t::count(message.s_WeightStyles, message.s_Weights);

    this->test(message);

    switch (m_Machine.state())
    {
    case LT_NOT_TESTING:
        break;
    case LT_TEST:
        m_Test->add(time, value, count);
        m_Test->captureVariance(time, value, count);
        break;
    case LT_LEARN:
        m_Test->add(time, value, count);
        break;
    case LT_INITIAL:
        this->apply(LT_NEW_VALUE, message);
        this->handle(message);
        break;
    default:
        LOG_ERROR("Test in a bad state: " << m_Machine.state());
        this->apply(LT_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::handle(const SDetectedPeriodicMessage &message)
{
    if (m_Machine.state() != LT_NOT_TESTING)
    {
        this->apply(LT_RESET, message);
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::test(const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    if (this->shouldTest(time))
    {
        switch (m_Machine.state())
        {
        case LT_NOT_TESTING:
        case LT_INITIAL:
            break;
        case LT_LEARN:
            this->apply(LT_START_TEST, message);
            this->test(message);
            break;
        case LT_TEST:
            if (m_Test->test())
            {
                this->mediator()->forward(SDetectedTrendMessage(time, *m_Test));
                this->apply(LT_FINISH_TEST, message);
            }
            break;
        default:
            LOG_ERROR("Test in a bad state: " << m_Machine.state());
            this->apply(LT_RESET, message);
            break;
        }
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::decayRate(double decayRate)
{
    if (m_Test)
    {
        m_Test->decayRate(std::max(decayRate, m_MaximumDecayRate));
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::propagateForwardsByTime(double time)
{
    if (m_Test)
    {
        m_Test->propagateForwardsByTime(time);
    }
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::skipTime(core_t::TTime skipInterval)
{
    core_t::TTime testInterval = this->testInterval();
    m_NextTestTime = CIntegerTools::floor(m_NextTestTime + skipInterval + testInterval, testInterval);
}

uint64_t CTimeSeriesDecompositionDetail::CLongTermTrendTest::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Machine);
    seed = CChecksum::calculate(seed, m_MaximumDecayRate);
    seed = CChecksum::calculate(seed, m_NextTestTime);
    return CChecksum::calculate(seed, m_Test);
}

void CTimeSeriesDecompositionDetail::CLongTermTrendTest::apply(std::size_t symbol, const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    std::size_t old = m_Machine.state();
    m_Machine.apply(symbol);
    std::size_t state = m_Machine.state();

    if (state != old)
    {
        LOG_TRACE(LT_STATES[old] << "," << LT_ALPHABET[symbol] << " -> " << LT_STATES[state]);

        if (old == LT_INITIAL)
        {
            m_NextTestTime = time + 2 * WEEK;
        }

        switch (state)
        {
        case LT_LEARN:
        case LT_TEST:
            break;
        case LT_NOT_TESTING:
            m_NextTestTime = core_t::TTime();
            m_Test.reset();
            break;
        case LT_INITIAL:
            m_NextTestTime = core_t::TTime();
            m_Test.reset(new CTrendTests::CTrend(m_MaximumDecayRate));
            break;
        default:
            LOG_ERROR("Test in a bad state: " << state);
            this->apply(LT_RESET, message);
            break;
        }
    }
}

bool CTimeSeriesDecompositionDetail::CLongTermTrendTest::shouldTest(core_t::TTime time)
{
    return shouldTestImpl(time, this->testInterval(), m_NextTestTime);
}

core_t::TTime CTimeSeriesDecompositionDetail::CLongTermTrendTest::testInterval(void) const
{
    return CSeasonalComponentAdaptiveBucketing::timescale();
}


//////// CDailyWeeklyTest ////////

CTimeSeriesDecompositionDetail::CDailyWeeklyTest::CDailyWeeklyTest(double decayRate,
                                                                   core_t::TTime bucketLength) :
        m_Machine(core::CStateMachine::create(DW_ALPHABET, DW_STATES,
                                              DW_TRANSITION_FUNCTION,
                                              DW_INITIAL)),
        m_DecayRate(decayRate),
        m_BucketLength(bucketLength),
        m_NextTestTime(),
        m_StartedRegularTest(),
        m_TimeOutRegularTest()
{}

CTimeSeriesDecompositionDetail::CDailyWeeklyTest::CDailyWeeklyTest(const CDailyWeeklyTest &other) :
        m_Machine(other.m_Machine),
        m_DecayRate(other.m_DecayRate),
        m_BucketLength(other.m_BucketLength),
        m_NextTestTime(other.m_NextTestTime),
        m_StartedRegularTest(other.m_StartedRegularTest),
        m_TimeOutRegularTest(other.m_TimeOutRegularTest),
        m_RegularTest(other.m_RegularTest ? new CTrendTests::CPeriodicity(*other.m_RegularTest) : 0),
        m_SmallTest(other.m_SmallTest ? new CTrendTests::CRandomizedPeriodicity(*other.m_SmallTest) : 0),
        m_Periods(other.m_Periods)
{}

bool CTimeSeriesDecompositionDetail::CDailyWeeklyTest::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(MACHINE_TAG, traverser.traverseSubLevel(
                                 boost::bind(&core::CStateMachine::acceptRestoreTraverser, &m_Machine, _1)));
        RESTORE_BUILT_IN(NEXT_TEST_TIME_TAG, m_NextTestTime)
        RESTORE_BUILT_IN(STARTED_REGULAR_TEST_TAG, m_StartedRegularTest)
        RESTORE_BUILT_IN(TIME_OUT_REGULAR_TEST_TAG, m_TimeOutRegularTest)
        if (name == REGULAR_TEST_TAG)
        {
            m_RegularTest.reset(new CTrendTests::CPeriodicity(regularTestDecayRate(m_DecayRate)));
            if (traverser.traverseSubLevel(
                    boost::bind(&CTrendTests::CPeriodicity::acceptRestoreTraverser, m_RegularTest.get(), _1)) == false)
            {
                return false;
            }
            continue;
        }
        RESTORE_SETUP_TEARDOWN(SMALL_TEST_TAG,
                               m_SmallTest.reset(new CTrendTests::CRandomizedPeriodicity),
                               traverser.traverseSubLevel(
                                   boost::bind(&CTrendTests::CRandomizedPeriodicity::acceptRestoreTraverser,
                                               m_SmallTest.get(), _1)),
                               /**/)
        RESTORE(PERIODS_TAG, traverser.traverseSubLevel(
                                 boost::bind(&CTrendTests::CPeriodicity::CResult::acceptRestoreTraverser, &m_Periods, _1)))
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(MACHINE_TAG, boost::bind(&core::CStateMachine::acceptPersistInserter, &m_Machine, _1));
    inserter.insertValue(NEXT_TEST_TIME_TAG, m_NextTestTime);
    inserter.insertValue(STARTED_REGULAR_TEST_TAG, m_StartedRegularTest);
    inserter.insertValue(TIME_OUT_REGULAR_TEST_TAG, m_TimeOutRegularTest);
    if (m_RegularTest)
    {
        inserter.insertLevel(REGULAR_TEST_TAG, boost::bind(
                                 &CTrendTests::CPeriodicity::acceptPersistInserter, m_RegularTest.get(), _1));
    }
    if (m_SmallTest)
    {
        inserter.insertLevel(SMALL_TEST_TAG, boost::bind(
                                 &CTrendTests::CRandomizedPeriodicity::acceptPersistInserter, m_SmallTest.get(), _1));
    }
    inserter.insertLevel(PERIODS_TAG, boost::bind(
                             &CTrendTests::CPeriodicity::CResult::acceptPersistInserter, &m_Periods, _1));
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::swap(CDailyWeeklyTest &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_NextTestTime, other.m_NextTestTime);
    std::swap(m_StartedRegularTest, other.m_StartedRegularTest);
    std::swap(m_TimeOutRegularTest, other.m_TimeOutRegularTest);
    m_RegularTest.swap(other.m_RegularTest);
    m_SmallTest.swap(other.m_SmallTest);
    std::swap(m_Periods, other.m_Periods);
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::handle(const SAddValueMessage &message)
{
    core_t::TTime time = message.s_Time;
    double value = message.s_Value - message.s_Trend;
    const TWeightStyleVec &weightStyles = message.s_WeightStyles;
    const TDouble4Vec &weights = message.s_Weights;

    this->test(message);

    switch (m_Machine.state())
    {
    case DW_NOT_TESTING:
        break;
    case DW_SMALL_TEST:
        m_SmallTest->add(time, value);
        break;
    case DW_REGULAR_TEST:
        if (time < this->timeOutRegularTest())
        {
            m_RegularTest->add(time, value, maths_t::count(weightStyles, weights));
        }
        else
        {
            LOG_TRACE("Switching to small test at " << time);
            this->mediator()->forward(SDiscardPeriodicMessage(time, *m_RegularTest));
            this->apply(DW_REGULAR_TEST_TIMED_OUT, message);
            this->handle(message);
        }
        break;
    case DW_INITIAL:
        this->apply(DW_NEW_VALUE, message);
        this->handle(message);
        break;
    default:
        LOG_ERROR("Test in a bad state: " << m_Machine.state());
        this->apply(DW_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::handle(const SDetectedTrendMessage &message)
{
    if (m_Machine.state() != DW_NOT_TESTING)
    {
        this->apply(DW_RESET, message);
    }
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::handle(const SDetectedPeriodicMessage &message)
{
    if (&message.s_Test == m_RegularTest.get())
    {
        // No-op.
    }
    else if (!message.s_Test.seenSufficientData())
    {
        this->apply(DW_RESET, message);
    }
    else
    {
        const TTimeVec &periods = message.s_Test.periods();
        for (std::size_t i = 0u; i < periods.size(); ++i)
        {
            if (DAY % periods[i] != 0 || periods[i] % WEEK != 0)
            {
                this->apply(DW_DETECTED_INCOMPATIBLE_PERIOD, message);
            }
        }
        this->apply(DW_RESET, message);
    }
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::test(const SMessage &message)
{
    core_t::TTime time = message.s_Time;
    switch (m_Machine.state())
    {
    case DW_NOT_TESTING:
    case DW_INITIAL:
        break;
    case DW_SMALL_TEST:
        if (this->shouldTest(time))
        {
            LOG_TRACE("Small testing at " << time);
            if (m_SmallTest->test())
            {
                LOG_TRACE("Switching to full test at " << time);
                this->apply(DW_SMALL_TEST_TRUE, message);
            }
        }
        break;
    case DW_REGULAR_TEST:
        if (this->shouldTest(time))
        {
            LOG_TRACE("Regular testing at " << time);
            CTrendTests::CPeriodicity::CResult result = m_RegularTest->test();
            if (result.periodic() && result != m_Periods)
            {
                this->mediator()->forward(SDetectedPeriodicMessage(time, result, *m_RegularTest));
                m_Periods = result;
            }
            if (result.periodic())
            {
                if (m_RegularTest->seenSufficientData())
                {
                    LOG_TRACE("Finished testing");
                    this->apply(DW_FINISH_TEST, message);
                }
                else
                {
                    m_NextTestTime = maths::CIntegerTools::ceil(
                            m_StartedRegularTest + 2 * WEEK, this->testInterval());
                }
            }
        }
        break;
    default:
        LOG_ERROR("Test in a bad state: " << m_Machine.state());
        this->apply(DW_RESET, message);
        break;
    }
}

const CTrendTests::CPeriodicity::CResult &CTimeSeriesDecompositionDetail::CDailyWeeklyTest::periods(void) const
{
    return m_Periods;
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::propagateForwardsByTime(double time)
{
    if (m_RegularTest)
    {
        m_RegularTest->propagateForwardsByTime(time);
    }
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::skipTime(core_t::TTime skipInterval)
{
    core_t::TTime testInterval = this->testInterval();
    m_NextTestTime = CIntegerTools::floor(m_NextTestTime + skipInterval + testInterval, testInterval);
    m_TimeOutRegularTest += skipInterval;
}

uint64_t CTimeSeriesDecompositionDetail::CDailyWeeklyTest::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Machine);
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_BucketLength);
    seed = CChecksum::calculate(seed, m_NextTestTime);
    seed = CChecksum::calculate(seed, m_StartedRegularTest);
    seed = CChecksum::calculate(seed, m_TimeOutRegularTest);
    seed = CChecksum::calculate(seed, m_RegularTest);
    seed = CChecksum::calculate(seed, m_SmallTest);
    return CChecksum::calculate(seed, m_Periods);
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CDailyWeeklyTest");
    core::CMemoryDebug::dynamicSize("m_RegularTest", m_RegularTest, mem);
    core::CMemoryDebug::dynamicSize("m_SmallTest", m_SmallTest, mem);
}

std::size_t CTimeSeriesDecompositionDetail::CDailyWeeklyTest::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(m_RegularTest) + core::CMemory::dynamicSize(m_SmallTest);
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::apply(std::size_t symbol, const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    std::size_t old = m_Machine.state();
    m_Machine.apply(symbol);
    std::size_t state = m_Machine.state();

    if (state != old)
    {
        LOG_TRACE(DW_STATES[old] << "," << DW_ALPHABET[symbol] << " -> " << DW_STATES[state]);

        if (old == DW_INITIAL)
        {
            m_NextTestTime = time;
            m_TimeOutRegularTest = time + scale(5 * WEEK, m_BucketLength);
        }

        switch (state)
        {
        case DW_SMALL_TEST:
            if (m_RegularTest)
            {
                m_RegularTest.reset();
            }
            if (!m_SmallTest)
            {
                m_SmallTest.reset(new CTrendTests::CRandomizedPeriodicity);
            }
            break;
        case DW_REGULAR_TEST:
            if (m_SmallTest)
            {
                m_TimeOutRegularTest = time + scale(9 * WEEK, m_BucketLength);
                m_SmallTest.reset();
            }
            if (!m_RegularTest)
            {
                m_StartedRegularTest = time;
                m_RegularTest.reset(CTrendTests::dailyAndWeekly(m_BucketLength, regularTestDecayRate(m_DecayRate)));
                if (!m_RegularTest)
                {
                    this->apply(DW_NOT_TESTING, message);
                }
            }
            break;
        case DW_NOT_TESTING:
        case DW_INITIAL:
            m_NextTestTime = core_t::TTime();
            m_TimeOutRegularTest = core_t::TTime();
            m_SmallTest.reset();
            m_RegularTest.reset();
            break;
        default:
            LOG_ERROR("Test in a bad state: " << state);
            this->apply(DW_RESET, message);
            break;
        }
    }
}

bool CTimeSeriesDecompositionDetail::CDailyWeeklyTest::shouldTest(core_t::TTime time)
{
    return shouldTestImpl(time, this->testInterval(), m_NextTestTime);
}

core_t::TTime CTimeSeriesDecompositionDetail::CDailyWeeklyTest::testInterval(void) const
{
    switch (m_Machine.state())
    {
    case DW_SMALL_TEST:
        return DAY;
    case DW_REGULAR_TEST:
        return m_RegularTest->seenSufficientData() ? 2 * WEEK : DAY;
    default:
        break;
    }
    return FOREVER;
}

core_t::TTime CTimeSeriesDecompositionDetail::CDailyWeeklyTest::timeOutRegularTest(void) const
{
    return m_TimeOutRegularTest + static_cast<core_t::TTime>(
                                      6.0 * static_cast<double>(WEEK)
                                          * (1.0 - m_RegularTest->populatedRatio()));
}


//////// CTrendCRef ////////

CTimeSeriesDecompositionDetail::CTrendCRef::CTrendCRef(void) :
        m_Trend(0), m_Variance(0.0), m_TimeOrigin(0), m_Horizon(0)
{}

CTimeSeriesDecompositionDetail::CTrendCRef::CTrendCRef(const TRegression &trend,
                                                       double variance,
                                                       core_t::TTime timeOrigin,
                                                       core_t::TTime horizon) :
        m_Trend(&trend),
        m_Variance(variance),
        m_TimeOrigin(timeOrigin),
        m_Horizon(horizon)
{}

double CTimeSeriesDecompositionDetail::CTrendCRef::count(void) const
{
    return m_Trend ? m_Trend->count() : 0.0;
}

TDoubleDoublePr CTimeSeriesDecompositionDetail::CTrendCRef::prediction(core_t::TTime time, double confidence) const
{
    if (!m_Trend)
    {
        return TDoubleDoublePr(0, 0);
    }

    time = std::min(time, m_Horizon);

    double m  = CRegression::predict(*m_Trend, this->time(time));
    double sd = ::sqrt(m_Variance);

    CRegression::CLeastSquaresOnline<3, double>::TArray params;
    m_Trend->parameters(params);

    if (confidence > 0.0 && sd > 0.0)
    {
        try
        {
            boost::math::normal_distribution<> normal(m, sd);
            double ql = boost::math::quantile(normal, (100.0 - confidence) / 200.0);
            double qu = boost::math::quantile(normal, (100.0 + confidence) / 200.0);
            return TDoubleDoublePr(ql, qu);
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Failed calculating confidence interval: " << e.what()
                      << ", m = " << m
                      << ", sd = " << sd
                      << ", confidence = " << confidence);
        }
    }
    return TDoubleDoublePr(m, m);
}

double CTimeSeriesDecompositionDetail::CTrendCRef::mean(core_t::TTime a, core_t::TTime b) const
{
    a = std::min(a, m_Horizon);
    b = std::min(b, m_Horizon);
    return m_Trend ? m_Trend->mean(this->time(a), this->time(b)) : 0.0;
}

double CTimeSeriesDecompositionDetail::CTrendCRef::variance(void) const
{
    return m_Variance;
}

bool CTimeSeriesDecompositionDetail::CTrendCRef::covariances(TMatrix &result) const
{
    return m_Trend ? m_Trend->covariances(m_Variance, result) : false;
}

double CTimeSeriesDecompositionDetail::CTrendCRef::time(core_t::TTime time) const
{
    return m_Trend ? regressionTime(time, m_TimeOrigin) : 0.0;
}


//////// CComponents ////////

CTimeSeriesDecompositionDetail::CComponents::CComponents(double decayRate,
                                                         core_t::TTime bucketLength,
                                                         std::size_t seasonalComponentSize) :
        m_Machine(core::CStateMachine::create(SC_ALPHABET, SC_STATES,
                                              SC_TRANSITION_FUNCTION,
                                              SC_NORMAL)),
        m_DecayRate(decayRate),
        m_BucketLength(bucketLength),
        m_LongestPeriod(0),
        m_SeasonalComponentSize(seasonalComponentSize),
        m_LastInterpolatedTime(),
        m_NextInterpolateTime(),
        m_Watcher(0)
{}

CTimeSeriesDecompositionDetail::CComponents::CComponents(const CComponents &other) :
        m_Machine(core::CStateMachine::create(SC_ALPHABET, SC_STATES,
                                              SC_TRANSITION_FUNCTION,
                                              SC_NORMAL)),
        m_DecayRate(other.m_DecayRate),
        m_BucketLength(other.m_BucketLength),
        m_LongestPeriod(0),
        m_SeasonalComponentSize(other.m_SeasonalComponentSize),
        m_LastInterpolatedTime(other.m_NextInterpolateTime),
        m_NextInterpolateTime(other.m_LastInterpolatedTime),
        m_Trend(other.m_Trend ? new STrend(*other.m_Trend) : 0),
        m_Seasonal(other.m_Seasonal ? new SSeasonal(*other.m_Seasonal) : 0),
        m_Watcher(0)
{}

bool CTimeSeriesDecompositionDetail::CComponents::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(MACHINE_TAG, traverser.traverseSubLevel(
                                 boost::bind(&core::CStateMachine::acceptRestoreTraverser, &m_Machine, _1)));
        RESTORE_BUILT_IN(LONGEST_PERIOD_TAG, m_LongestPeriod)
        RESTORE_BUILT_IN(LAST_INTERPOLATED_TIME_TAG, m_LastInterpolatedTime)
        RESTORE_BUILT_IN(NEXT_INTERPOLATE_TIME_TAG, m_NextInterpolateTime)
        RESTORE_SETUP_TEARDOWN(TREND_TAG,
                               m_Trend.reset(new STrend),
                               traverser.traverseSubLevel(boost::bind(&STrend::acceptRestoreTraverser,
                                                                      m_Trend.get(), _1)),
                               /**/)
        RESTORE_SETUP_TEARDOWN(COMPONENTS_TAG,
                               m_Seasonal.reset(new SSeasonal),
                               traverser.traverseSubLevel(boost::bind(&SSeasonal::acceptRestoreTraverser,
                                                                      m_Seasonal.get(),
                                                                      m_DecayRate, m_BucketLength, _1)),
                               /**/)
    }
    while (traverser.next());

    return true;
}

void CTimeSeriesDecompositionDetail::CComponents::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(MACHINE_TAG, boost::bind(&core::CStateMachine::acceptPersistInserter, &m_Machine, _1));
    inserter.insertValue(LONGEST_PERIOD_TAG, m_LongestPeriod);
    inserter.insertValue(LAST_INTERPOLATED_TIME_TAG, m_LastInterpolatedTime);
    inserter.insertValue(NEXT_INTERPOLATE_TIME_TAG, m_NextInterpolateTime);
    if (m_Trend)
    {
        inserter.insertLevel(TREND_TAG, boost::bind(&STrend::acceptPersistInserter, m_Trend.get(), _1));
    }
    if (m_Seasonal)
    {
        inserter.insertLevel(COMPONENTS_TAG, boost::bind(&SSeasonal::acceptPersistInserter, m_Seasonal.get(), _1));
    }
}

void CTimeSeriesDecompositionDetail::CComponents::swap(CComponents &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_LongestPeriod, other.m_LongestPeriod);
    std::swap(m_SeasonalComponentSize, other.m_SeasonalComponentSize);
    std::swap(m_LastInterpolatedTime, other.m_LastInterpolatedTime);
    std::swap(m_NextInterpolateTime, other.m_NextInterpolateTime);
    m_Trend.swap(other.m_Trend);
    m_Seasonal.swap(other.m_Seasonal);
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SAddValueMessage &message)
{
    switch (m_Machine.state())
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
        if (m_Trend || m_Seasonal)
        {
            this->interpolate(message);

            double value = message.s_Value;
            core_t::TTime time = message.s_Time;
            const TWeightStyleVec &weightStyles = message.s_WeightStyles;
            const TDouble4Vec &weights = message.s_Weights;

            TComponentPtrVec componentsToUpdate;
            TDoubleVec deltas;
            if (m_Seasonal)
            {
                m_Seasonal->componentsAndDeltas(time, componentsToUpdate, deltas);
            }

            double weight = maths_t::countForUpdate(weightStyles, weights);
            std::size_t n = componentsToUpdate.size();

            CTrendCRef trend = this->trend();
            TDoubleVec values(n + 1, value);
            decompose(trend, componentsToUpdate, deltas, time, values);

            if (m_Trend)
            {
                TMeanVarAccumulator moments =
                        CBasicStatistics::accumulator(trend.count(),
                                                      CBasicStatistics::mean(trend.prediction(time, 0.0)),
                                                      m_Trend->s_Variance);
                moments.add(values[0], weight);
                LOG_TRACE("Adding " << values[0]
                          << " at " << trend.time(time) << " to long term trend");
                m_Trend->s_Regression.add(trend.time(time), values[0], weight);
                m_Trend->s_Variance = CBasicStatistics::maximumLikelihoodVariance(moments);
            }
            for (std::size_t i = 1u; i <= n; ++i)
            {
                CSeasonalComponent *component = componentsToUpdate[i - 1];
                double wi = weight * static_cast<double>(m_LongestPeriod)
                                   / static_cast<double>(component->time().window());
                LOG_TRACE("Adding " << values[i]
                          << " to component with period " << component->time().period());
                component->add(time, values[i], wi);
            }
        }
        break;
    case SC_FORECASTING:
    case SC_DISABLED:
        break;
    default:
        LOG_ERROR("Components in a bad state: " << m_Machine.state());
        this->apply(SC_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SDetectedTrendMessage &message)
{
    switch (m_Machine.state())
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    {
        if (m_Watcher && !m_Seasonal)
        {
            *m_Watcher = true;
        }

        LOG_DEBUG("Detected long term trend at '" << message.s_Time << "'");

        const TRegression &trend = message.s_Test.trend();
        double variance = message.s_Test.variance();
        core_t::TTime origin = message.s_Test.origin();

        m_Trend.reset(new STrend);
        m_Trend->s_Regression = m_Seasonal ? TRegression() : trend;
        m_Trend->s_Variance = variance;
        m_Trend->s_TimeOrigin = origin;

        this->apply(SC_ADDED_COMPONENTS, message);
        break;
    }
    case SC_FORECASTING:
    case SC_DISABLED:
        break;
    default:
        LOG_ERROR("Components in a bad state: " << m_Machine.state());
        this->apply(SC_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SDetectedPeriodicMessage &message)
{
    static CTrendTests::CPeriodicity::EInterval SELECT_PARTITION[] =
        {
            CTrendTests::SELECT_WEEKDAYS,
            CTrendTests::SELECT_WEEKEND,
            CTrendTests::SELECT_WEEK
        };
    static CTrendTests::CPeriodicity::EPeriod SELECT_PERIOD[]  =
        {
            CTrendTests::SELECT_DAILY,
            CTrendTests::SELECT_WEEKLY
        };
    static core_t::TTime PERIODS[] = { DAY, WEEK };
    static core_t::TTime WINDOWS[][2] =
        {
            { WEEKEND, WEEK    },
            { 0,       WEEKEND },
            { 0,       WEEK    }
        };

    switch (m_Machine.state())
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    {
        core_t::TTime time = message.s_Time;
        CTrendTests::CPeriodicity::CResult periods = message.s_Result;

        if (m_Watcher)
        {
            *m_Watcher = true;
        }

        // TODO reset won't work when there are multiple tests.
        core_t::TTime startOfWeek = periods.startOfPartition();
        m_Seasonal.reset(new SSeasonal);
        TComponentVec &components = m_Seasonal->s_Components;
        components.clear();

        LOG_DEBUG("Detected " << CTrendTests::printDailyAndWeekly(periods));
        LOG_DEBUG("Start of week " << startOfWeek);
        LOG_DEBUG("Estimated new periods at '" << time << "'");

        CTrendTests::TTimeTimePrMeanVarAccumulatorPrVec trends[6];
        message.s_Test.trends(periods, trends);

        double bucketLength = static_cast<double>(m_BucketLength);
        std::size_t sizes[][2] =
            {
                { m_SeasonalComponentSize, m_SeasonalComponentSize     },
                { m_SeasonalComponentSize, m_SeasonalComponentSize / 2 },
                { m_SeasonalComponentSize, m_SeasonalComponentSize * 2 }
            };
        core_t::TTime times[][2] =
            {
                { time - WEEK, time - WEEK },
                { time - WEEK, time - WEEK },
                { time - DAY,  time - WEEK }
            };

        for (std::size_t i = 0u; i < 2; ++i)
        {
            for (std::size_t j = 0u; j < 3; ++j)
            {
                if (periods.periods(SELECT_PARTITION[j]) & SELECT_PERIOD[i])
                {
                    std::size_t index = periods.index(SELECT_PARTITION[j], SELECT_PERIOD[i]);
                    components.push_back(CSeasonalComponent(
                            CSeasonalComponent::TTime(startOfWeek, WINDOWS[j][0], WINDOWS[j][1], PERIODS[i]),
                            sizes[j][i], m_DecayRate, bucketLength, CSplineTypes::E_Natural));
                    components.back().initialize(times[j][i], time, trends[index]);
                    m_LongestPeriod = std::max(m_LongestPeriod, PERIODS[i]);
                }
            }
        }

        this->apply(SC_ADDED_COMPONENTS, message);

        break;
    }
    case SC_FORECASTING:
    case SC_DISABLED:
        break;
    default:
        LOG_ERROR("Components in a bad state: " << m_Machine.state());
        this->apply(SC_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SDiscardPeriodicMessage &message)
{
    if (m_Seasonal)
    {
        TTimeVec periods = message.s_Test.periods();
        TComponentVec &components = m_Seasonal->s_Components;

        std::size_t last = 0u;
        double shift = 0.0;

        for (std::size_t i = 0u; i < components.size(); ++i)
        {
            if (last != i)
            {
                components[i].swap(components[last]);
            }
            if (!std::binary_search(periods.begin(), periods.end(), components[i].time().period()))
            {
                ++last;
            }
            else if (m_Watcher)
            {
                shift += components[i].meanValue();
                *m_Watcher = true;
            }
        }

        if (last == 0)
        {
            m_Seasonal.reset();
        }
        else
        {
            components.erase(components.begin() + last, components.end());
        }

        if (m_Trend)
        {
            m_Trend->s_Regression.shiftOrdinate(shift);
        }
        else if (m_Seasonal)
        {
            m_Seasonal->shiftValue(shift);
        }
    }
}

void CTimeSeriesDecompositionDetail::CComponents::interpolate(const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    std::size_t state = m_Machine.state();

    switch (state)
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    case SC_FORECASTING:
        if (this->shouldInterpolate(time))
        {
            LOG_TRACE("Interpolating values at " << time);

            m_LastInterpolatedTime = CIntegerTools::floor(time, this->interpolateInterval());

            if (m_Trend)
            {
                m_Trend->shiftTime(m_NextInterpolateTime);
            }
            if (m_Seasonal)
            {
                m_Seasonal->interpolate(m_LastInterpolatedTime, !this->forecasting());
            }

            this->apply(SC_INTERPOLATED, message);
        }
        break;
    case SC_DISABLED:
        break;
    default:
        LOG_ERROR("Components in a bad state: " << state);
        this->apply(SC_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CComponents::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
    // TODO should we control LTT decay rate?
    if (m_Seasonal)
    {
        m_Seasonal->decayRate(decayRate);
    }
}

void CTimeSeriesDecompositionDetail::CComponents::propagateForwards(core_t::TTime start, core_t::TTime end)
{
    LOG_TRACE("Aging at " << end);

    double time =  static_cast<double>(end - start)
                 / static_cast<double>(this->propagationForwardsByTimeInterval());

    if (m_Trend)
    {
        double factor = ::exp(-m_DecayRate * time);
        m_Trend->s_Regression.age(factor);
    }
    if (m_Seasonal)
    {
        m_Seasonal->propagateForwardsByTime(time);
    }
}

bool CTimeSeriesDecompositionDetail::CComponents::forecasting(void) const
{
    return m_Machine.state() == SC_FORECASTING;
}

void CTimeSeriesDecompositionDetail::CComponents::forecast(void)
{
    this->apply(SC_FORECAST, SMessage());
}

bool CTimeSeriesDecompositionDetail::CComponents::initialized(void) const
{
    return m_Trend ? true : (m_Seasonal ? m_Seasonal->initialized() : false);
}

TTrendCRef CTimeSeriesDecompositionDetail::CComponents::trend(void) const
{
    return m_Trend ? CTrendCRef(m_Trend->s_Regression,
                                m_Trend->s_Variance,
                                m_Trend->s_TimeOrigin,
                                this->forecasting() ? FOREVER :  m_LastInterpolatedTime
                                                               + this->interpolateInterval()) : NO_TREND;
}

const TComponentVec &CTimeSeriesDecompositionDetail::CComponents::seasonal(void) const
{
    return m_Seasonal ? m_Seasonal->s_Components : NO_COMPONENTS;
}

double CTimeSeriesDecompositionDetail::CComponents::meanValue(void) const
{
    return this->initialized() ? (  this->trend().mean(m_LastInterpolatedTime,
                                                       m_LastInterpolatedTime + this->interpolateInterval())
                                  + meanOf(&CSeasonalComponent::meanValue, this->seasonal())) : 0.0;
}

double CTimeSeriesDecompositionDetail::CComponents::meanValue(core_t::TTime time) const
{
    return this->initialized() ?  this->trend().mean(m_LastInterpolatedTime,
                                                     m_LastInterpolatedTime + this->interpolateInterval())
                                + meanValueAt(time, this->seasonal()) : 0.0;
}

double CTimeSeriesDecompositionDetail::CComponents::meanVariance(void) const
{
    return this->initialized() ?  this->trend().variance()
                                + meanOf(&CSeasonalComponent::meanVariance, this->seasonal()) : 0.0;
}

void CTimeSeriesDecompositionDetail::CComponents::skipTime(core_t::TTime skipInterval)
{
    core_t::TTime interpolateInterval = this->interpolateInterval();
    m_NextInterpolateTime = CIntegerTools::floor(  m_NextInterpolateTime
                                                 + skipInterval
                                                 + interpolateInterval, interpolateInterval);
}

core_t::TTime CTimeSeriesDecompositionDetail::CComponents::interpolateInterval(void) const
{
    return this->forecasting() ? FORECASTING_INTERPOLATE_INTERVAL :
                                 CSeasonalComponentAdaptiveBucketing::timescale();
}

core_t::TTime CTimeSeriesDecompositionDetail::CComponents::propagationForwardsByTimeInterval(void) const
{
    return this->forecasting() ? FORECASTING_PROPAGATION_FORWARDS_BY_TIME_INTERVAL :
                                 CSeasonalComponentAdaptiveBucketing::timescale();
}

uint64_t CTimeSeriesDecompositionDetail::CComponents::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Machine);
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_BucketLength);
    seed = CChecksum::calculate(seed, m_SeasonalComponentSize);
    seed = CChecksum::calculate(seed, m_LastInterpolatedTime);
    seed = CChecksum::calculate(seed, m_NextInterpolateTime);
    seed = CChecksum::calculate(seed, m_Trend);
    return CChecksum::calculate(seed, m_Seasonal);
}

void CTimeSeriesDecompositionDetail::CComponents::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CComponents");
    core::CMemoryDebug::dynamicSize("m_Trend", m_Trend, mem);
    core::CMemoryDebug::dynamicSize("m_Seasonal", m_Seasonal, mem);
}

std::size_t CTimeSeriesDecompositionDetail::CComponents::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(m_Trend) + core::CMemory::dynamicSize(m_Seasonal);
}

void CTimeSeriesDecompositionDetail::CComponents::apply(std::size_t symbol, const SMessage &message)
{
    if (symbol == SC_RESET)
    {
        // TODO broadcast recover message.
        m_Trend.reset();
        m_Seasonal.reset();
    }

    std::size_t old = m_Machine.state();
    m_Machine.apply(symbol);
    std::size_t state = m_Machine.state();

    if (state != old)
    {
        LOG_TRACE(SC_STATES[old] << "," << SC_ALPHABET[symbol] << " -> " << SC_STATES[state]);

        switch (state)
        {
        case SC_NORMAL:
        case SC_FORECASTING:
        case SC_NEW_COMPONENTS:
            this->interpolate(message);
            break;
        case SC_DISABLED:
            m_Trend.reset();
            m_Seasonal.reset();
            break;
        default:
            LOG_ERROR("Components in a bad state: " << m_Machine.state());
            this->apply(SC_RESET, message);
            break;
        }
    }
}

bool CTimeSeriesDecompositionDetail::CComponents::shouldInterpolate(core_t::TTime time)
{
    core_t::TTime interpolateInterval = this->interpolateInterval();
    std::size_t state = m_Machine.state();
    if (state == SC_NEW_COMPONENTS)
    {
        m_NextInterpolateTime = CIntegerTools::ceil(time + 1, interpolateInterval);
        return true;
    }
    if (time >= m_NextInterpolateTime)
    {
        m_NextInterpolateTime = CIntegerTools::ceil(time + 1, interpolateInterval);
        return true;
    }
    return false;
}

void CTimeSeriesDecompositionDetail::CComponents::notifyOnNewComponents(bool *watcher)
{
    m_Watcher = watcher;
}

CTimeSeriesDecompositionDetail::CComponents::CScopeNotifyOnStateChange::CScopeNotifyOnStateChange(CComponents &components) :
        m_Components(components), m_Watcher(false)
{
    m_Components.notifyOnNewComponents(&m_Watcher);
}

CTimeSeriesDecompositionDetail::CComponents::CScopeNotifyOnStateChange::~CScopeNotifyOnStateChange(void)
{
    m_Components.notifyOnNewComponents(0);
}

bool CTimeSeriesDecompositionDetail::CComponents::CScopeNotifyOnStateChange::changed(void) const
{
    return m_Watcher;
}

CTimeSeriesDecompositionDetail::CComponents::STrend::STrend(void) :
        s_Variance(0.0),
        s_TimeOrigin(boost::numeric::bounds<core_t::TTime>::lowest())
{}

bool CTimeSeriesDecompositionDetail::CComponents::STrend::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(REGRESSION_TAG,
                traverser.traverseSubLevel(
                    boost::bind(&TRegression::acceptRestoreTraverser, &s_Regression, _1)))
        RESTORE_BUILT_IN(VARIANCE_TAG, s_Variance)
        RESTORE_BUILT_IN(TIME_ORIGIN_TAG, s_TimeOrigin)
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CComponents::STrend::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(REGRESSION_TAG, boost::bind(&TRegression::acceptPersistInserter, &s_Regression, _1));
    inserter.insertValue(VARIANCE_TAG, s_Variance);
    inserter.insertValue(TIME_ORIGIN_TAG, s_TimeOrigin);
}

void CTimeSeriesDecompositionDetail::CComponents::STrend::shiftTime(core_t::TTime time)
{
    if (time - 3 * WEEK >= s_TimeOrigin)
    {
        LOG_TRACE("shifting");
        s_Regression.shiftAbscissa(-regressionTime(time, s_TimeOrigin));
        s_TimeOrigin = time;
    }
}

uint64_t CTimeSeriesDecompositionDetail::CComponents::STrend::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, s_Regression);
    seed = CChecksum::calculate(seed, s_Variance);
    return CChecksum::calculate(seed, s_TimeOrigin);
}

bool CTimeSeriesDecompositionDetail::CComponents::SSeasonal::acceptRestoreTraverser(double decayRate,
                                                                                    core_t::TTime bucketLength,
                                                                                    core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_NO_ERROR(COMPONENT_TAG, s_Components.push_back(
                CSeasonalComponent(decayRate, static_cast<double>(bucketLength), traverser)))
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        inserter.insertLevel(COMPONENT_TAG, boost::bind(
                &CSeasonalComponent::acceptPersistInserter, &s_Components[i], _1));
    }
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::decayRate(double decayRate)
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        s_Components[i].decayRate(decayRate);
    }
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::propagateForwardsByTime(double time)
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        CSeasonalComponent &component = s_Components[i];
        component.propagateForwardsByTime(time);
    }
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::componentsAndDeltas(core_t::TTime time,
                                                                                 TComponentPtrVec &components,
                                                                                 TDoubleVec &deltas)
{
    components.clear();
    components.reserve(s_Components.size());
    deltas.clear();
    deltas.reserve(s_Components.size());

    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        if (s_Components[i].time().inWindow(time))
        {
            components.push_back(&s_Components[i]);
        }
    }

    deltas.resize(s_Components.size(), 0.0);
    for (std::size_t i = 1u; i < components.size(); ++i)
    {
        deltas[i-1] = 0.2 * components[i]->differenceFromMean(time, s_Components[i-1].time().period());
    }
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::interpolate(core_t::TTime time, bool refine)
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        s_Components[i].interpolate(time, refine);
    }
}

bool CTimeSeriesDecompositionDetail::CComponents::SSeasonal::initialized(void) const
{
    for (std::size_t i = 0u; s_Components.size(); ++i)
    {
        if (s_Components[i].initialized())
        {
            return true;
        }
    }
    return false;
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::shiftValue(double shift)
{
    typedef boost::container::flat_set<TTimeTimePr> TTimeTimePrFSet;

    TTimeTimePrFSet processed;
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        core_t::TTime start = s_Components[i].time().windowStart();
        core_t::TTime end   = s_Components[i].time().windowEnd();
        if (processed.insert(std::make_pair(start, end)).second)
        {
            s_Components[i].shift(shift);
        }
    }
}

uint64_t CTimeSeriesDecompositionDetail::CComponents::SSeasonal::checksum(uint64_t seed) const
{
    return CChecksum::calculate(seed, s_Components);
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("SSeasonal");
    core::CMemoryDebug::dynamicSize("s_Components", s_Components, mem);
}

std::size_t CTimeSeriesDecompositionDetail::CComponents::SSeasonal::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(s_Components);
}

}
}
