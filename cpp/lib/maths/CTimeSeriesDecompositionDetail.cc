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
#include <boost/container/flat_map.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/range.hpp>

#include <string>
#include <vector>

namespace prelert
{
namespace maths
{
namespace
{

typedef std::vector<double> TDoubleVec;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<TSizeVec> TSizeVecVec;
typedef std::vector<std::string> TStrVec;
typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
typedef boost::container::flat_map<TTimeTimePr, double> TTimeTimePrDoubleFMap;
typedef TTimeTimePrDoubleFMap::const_iterator TTimeTimePrDoubleFMapCItr;
typedef std::vector<CSeasonalComponent> TComponentVec;
typedef std::vector<CSeasonalComponent*> TComponentPtrVec;
typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef CTimeSeriesDecompositionTypedefs::TComponentVec TComponentVec;

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
        if (components[i].time().inWindow(time))
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
    TTimeTimePrDoubleFMap means;
    means.reserve(components.size());
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        core_t::TTime start = components[i].time().windowStart();
        core_t::TTime end   = components[i].time().windowEnd();
        means[std::make_pair(start, end)] += (components[i].*mean)();
    }

    TMeanAccumulator result;
    for (TTimeTimePrDoubleFMapCItr i = means.begin(); i != means.end(); ++i)
    {
        result.add(i->second, i->first.second - i->first.first);
    }

    return CBasicStatistics::mean(result);
}

//! Compute the values to add to each component.
//!
//! \param[in] components The seasonal components.
//! \param[in] deltas The delta offset to apply to the difference
//! between each component value and its mean, used to minimize
//! slope in the longer periods.
//! \param[in,out] decomposition Updated to contain the value to
//! add to each by component.
void decompose(const TComponentPtrVec &components,
               const TDoubleVec &deltas,
               core_t::TTime time,
               TDoubleVec &decomposition)
{
    std::size_t n = components.size();

    TDoubleVec x(n);
    double xhat = 0.0;
    double z = 0.0;
    for (std::size_t i = 0u; i < n; ++i)
    {
        x[i]  = CBasicStatistics::mean(components[i]->value(time, 0.0));
        xhat += x[i];
        z    += ::fabs(x[i]);
    }
    if (z == 0.0)
    {
        double lastShift = 0.0;
        for (std::size_t i = 0u; i < n; ++i)
        {
            double shift = deltas[i] * (x[i] - components[i]->meanValue());
            double d = shift - lastShift;
            lastShift = shift;
            decomposition[i] = (decomposition[i] - xhat) / static_cast<double>(n) + d;
        }
    }
    else
    {
        double lastShift = 0.0;
        for (std::size_t i = 0u; i < n; ++i)
        {
            double shift = deltas[i] * (x[i] - components[i]->meanValue());
            double d = shift - lastShift;
            lastShift = shift;
            decomposition[i] = x[i] + (decomposition[i] - xhat) * ::fabs(x[i]) / z + d;
        }
    }
}
//! \brief Compute the intersection of two seasonal components.
//!
//! DESCRIPTION:\n
//! This is defined as the smallest set of intervals which, via
//! unions, are able to reconstruct the spline intervals for the
//! two components, i.e. effectively a basis for the two sets of
//! intervals. This can be used to efficiently compute arithmetic
//! on the splines, such as their difference or slope of their
//! difference.
class CIntersection : private core::CNonCopyable
{
    public:
        typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
        typedef std::vector<TSizeSizePr> TSizeSizePrVec;

    public:
        CIntersection(const CSeasonalComponent &next,
                      const CSeasonalComponent &current)
        {
            CSeasonalComponent::TSplineCRef s1 = next.valueSpline();
            s1.coefficients(&m_A1, &m_B1, &m_C1);
            m_Knots1.assign(s1.knots().begin(), s1.knots().end());
            std::size_t n1 = m_Knots1.size();
            m_P1 = m_Knots1[n1-1] - m_Knots1[0];

            CSeasonalComponent::TSplineCRef s2 = current.valueSpline();
            s2.coefficients(&m_A2, &m_B2, &m_C2);
            m_Knots2.assign(s2.knots().begin(), s2.knots().end());
            std::size_t n2 = m_Knots2.size();
            m_P2 = m_Knots2[n2-1] - m_Knots2[0];

            std::size_t repeats = static_cast<std::size_t>(m_P1 / m_P2);
            m_Knots.reserve(n1 + repeats*(n2-1));
            m_Mapping.reserve(n1 + repeats*(n2-1) - 1);

            LOG_TRACE("n1 = " << n1 << ", n2 = " << n2);
            LOG_TRACE("p1 = " << m_P1 << ", p2 = " << m_P2);
            LOG_TRACE("repeats = " << repeats);
            LOG_TRACE("knots1 = " << core::CContainerPrinter::print(m_Knots1));
            LOG_TRACE("knots2 = " << core::CContainerPrinter::print(m_Knots2));

            double offset = 0.0;
            m_Knots.push_back(0.0);
            for (std::size_t j = 1u; j < n1; offset += m_P2)
            {
                for (std::size_t k = 1u; j < n1 && k < n2; /**/)
                {
                    m_Mapping.push_back(TSizeSizePr(j-1,k-1));
                    if (offset + m_Knots2[k] < m_Knots1[j])
                    {
                        m_Knots.push_back(offset + m_Knots2[k++]);
                    }
                    else if (m_Knots1[j] < offset + m_Knots2[k])
                    {
                        m_Knots.push_back(m_Knots1[j++]);
                    }
                    else
                    {
                        m_Knots.push_back(m_Knots1[j++]); ++k;
                    }
                }
            }
            LOG_TRACE("mapping = " << core::CContainerPrinter::print(m_Mapping));
            LOG_TRACE("knots = " << core::CContainerPrinter::print(m_Knots));
        }

        //! Compute the mean absolute slope of the spline difference.
        //!
        //! This is defined as
        //! <pre class="fragment">
        //!   \f$\frac{1}{|b-a|}\int_{[a,b]}{\left|\frac{df(s)}{ds}\right|}ds\f$
        //! </pre>
        //!
        //! Here, \f$f(.)\f$ is defined as the difference of the first
        //! spline and \p delta times the second spline, i.e.
        //! <pre class="fragment">
        //!   \f$f(t) = g(t) - \delta h(t)\f$
        //! </pre>
        double absSlope(double delta) const
        {
            double result = 0.0;
            std::size_t n = m_Knots.size();
            for (std::size_t i = 1u; i < n; ++i)
            {
                double a = m_Knots[i-1];
                double b = m_Knots[i];

                std::size_t j = m_Mapping[i-1].first;
                std::size_t k = m_Mapping[i-1].second;
                double a1 = m_A1[j];
                double b1 = m_B1[j];
                double c1 = m_C1[j];
                double x1 = m_Knots1[j];
                double a2 = delta * m_A2[k];
                double b2 = delta * m_B2[k];
                double c2 = delta * m_C2[k];
                double x2 = ::floor(a / m_P2) * m_P2 + m_Knots2[k];

                // Compute the roots of the difference equation.
                double qa =   3.0 * (a1 - a2);
                double qb =  -2.0 * (3.0 * (a1 * x1 - a2 * x2) + b2 - b1);
                double qc =   3.0 * (a1 * x1 * x1 - a2 * x2 * x2)
                            + 2.0 * (b2 * x2 - b1 * x1)
                            + c1 - c2;

                // Check if the slope can change sign in the interval.
                double descriminant = qb * qb - 4.0 * qa * qc;
                if (descriminant <= 0.0)
                {
                    result += absSlope(a, b, a1, b1, c1, x1, a2, b2, c2, x2);
                    continue;
                }

                // Split the interval by the roots of the slope and
                // compute the contribution from each subinterval.
                double rl = CTools::truncate((-qb - ::sqrt(descriminant)) / 2.0 / qa, a, b);
                double rr = CTools::truncate((-qb + ::sqrt(descriminant)) / 2.0 / qa, a, b);
                if (rl > rr)
                {
                    std::swap(rl, rr);
                }
                result +=   absSlope(a,  rl, a1, b1, c1, x1, a2, b2, c2, x2)
                          + absSlope(rl, rr, a1, b1, c1, x1, a2, b2, c2, x2)
                          + absSlope(rr,  b, a1, b1, c1, x1, a2, b2, c2, x2);

            }
            return result / (m_Knots[n-1] - m_Knots[0]);
        }

    private:
        //! Compute on the absolute slope of the spline difference
        //! on the interval [\p a, \p b].
        double absSlope(double a, double b,
                        double a1, double b1, double c1, double x1,
                        double a2, double b2, double c2, double x2) const
        {
            if (a == b)
            {
                return 0.0;
            }
            double h  = b - a;
            double m  = a + b;
            double y1 = x1 - m / 2.0;
            double y2 = x2 - m / 2.0;
            return h * ::fabs(  (  a1 * (h * h / 4.0 + 3.0 * y1 * y1)
                                 - a2 * (h * h / 4.0 + 3.0 * y2 * y2))
                              + (  b1 * (m - 2.0 * x1)
                                 - b2 * (m - 2.0 * x2))
                              + (c1 - c2));

        }

    private:
        double m_P1;
        TDoubleVec m_A1, m_B1, m_C1, m_Knots1;
        double m_P2;
        TDoubleVec m_A2, m_B2, m_C2, m_Knots2;
        TDoubleVec m_Knots;
        TSizeSizePrVec m_Mapping;
};

//! \brief Function object wrapper for an CIntersection absSlope function.
//!
//! \see CIntersection::absSlope for more details.
class CAbsSlope
{
    public:
        CAbsSlope(const CIntersection &intersection) : m_Intersection(&intersection) {}

        double operator()(double delta) const
        {
            return m_Intersection->absSlope(delta);
        }

    private:
        const CIntersection *m_Intersection;
};

//! Minimize the r.m.s. slope in the longer components by setting
//! the deltas to apply to in decomposition.
//!
//! \param[in] components The seasonal components to work on.
//! \param[out] deltas Filled in with the delta offsets to apply
//! to the difference between each component value and its mean
//! when decomposing, used to minimize slope in the longer periods.
void minimizeAbsSlope(const TComponentVec &components, TDoubleVec &deltas)
{
    typedef std::map<core_t::TTime, TSizeVec> TTimeSizeVecMap;
    typedef TTimeSizeVecMap::const_iterator TTimeSizeVecMapCItr;

    TTimeSizeVecMap counts;
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        if (components[i].initialized())
        {
            counts[components[i].time().window()].push_back(i);
        }
    }

    for (TTimeSizeVecMapCItr i = counts.begin(); i != counts.end(); ++i)
    {
        const TSizeVec &indexes = i->second;
        if (indexes.size() < 2)
        {
            // No degeneracy.
            continue;
        }

        // For a decomposition of the form f(t) = Sum_i( f_i(t) )
        // there is an obvious degeneracy in the choice of f_i(.).
        // In particular, for any pair (f_i, f_j) and for any
        // constant c set f_i' = f_i + c and f_j' = f_j - c and
        // f is unmodified.
        //
        // From our perspective not all choices of the component
        // functions are equally desirable because one ideally
        // wants to minimize the total slope of the components with
        // longer period. This means that we can use the buckets
        // more efficiently.
        //
        // To be concrete, for a set of functions {f_1, f_2, ..., f_n}
        // ordered by increasing periodicity we'd like to minimize
        // the function:
        //   Sum_{i>1}( r.m.s. curvature of f_i )
        //
        // Clearly, adding a constant to any function doesn't affect
        // its slope. However, we can add a scaling, i.e. c * f_i(t)
        // and then subtract c * f_i((t + T_i) mod T_{i+1}) from
        // f_{i+1}, and alter the r.m.s. slope of both functions.
        //
        // Since the r.m.s. curvature is relatively expensive to
        // to compute and the objective is a summation it suggests
        // optimization via stochastic gradient descent. We cycle
        // through the periods in increasing order. We update c by
        // increment and measure the r.m.s. slope. As soon as we've
        // bracketed a minimum we switch to Brent's method for
        // minimization. It should be pretty clear that this scheme
        // necessarily eventually converges (to a local minimum)
        // since the r.m.s. slope is positive.

        LOG_TRACE("minimizing |slope|");

        static const std::size_t MAX_ITERATIONS = 20u;
        static const double STEP = 0.05;
        static const double TOLERANCE = 0.001;
        static const double MINIMUM_DECREASE = 0.005;

        for (std::size_t j = 0u, k = 0u;
             j < MAX_ITERATIONS && k+1 < indexes.size();
             ++k)
        {
            const CSeasonalComponent &current = components[indexes[j]];
            const CSeasonalComponent &next = components[indexes[j+1]];

            double slope = next.valueSpline().absSlope();
            LOG_TRACE("|slope| = " << slope);

            CIntersection intersection(next, current);

            double a = 0.0;
            double fa = intersection.absSlope(0.0);
            double f0 = fa;
            double b = STEP;
            double fb = intersection.absSlope(b);
            if (fa < fb)
            {
                std::swap(a, b);
                std::swap(fa, fb);
            }
            j += 2;
            LOG_TRACE("deltas = (" << a << "," << b << ")"
                      << ", |slopes| = (" << fa << "," << fb << ")");

            double delta = 0.0;
            for (/**/; j < MAX_ITERATIONS; ++j)
            {
                double c = b + (b - a);
                double fc = intersection.absSlope(c);
                LOG_TRACE("delta = " << c << " |slope| = " << fc);

                if (fc > fb)
                {
                    // a and c bracket a (local) minimum.

                    if (c < a)
                    {
                        std::swap(a, c);
                        std::swap(fa, fc);
                    }

                    LOG_TRACE("minimize in [" << a << "," << c << "]");

                    double x, fx;
                    std::size_t iterations = MAX_ITERATIONS - j;
                    CSolvers::minimize(a, c, fa, fc,
                                       CAbsSlope(intersection),
                                       TOLERANCE, iterations,
                                       x, fx);
                    j += iterations;
                    LOG_TRACE("x = " << x << ", |slope| = " << fx);
                    if (fx < (1.0 - MINIMUM_DECREASE) * f0)
                    {
                        delta = x;
                    }
                    break;
                }

                a = b; fa = fb;
                b = c; fb = fc;
                if (fc < (1.0 - MINIMUM_DECREASE) * f0)
                {
                    delta = c;
                }
            }

            LOG_TRACE("delta = " << delta);
            deltas[indexes[k]] = delta;
        }

        LOG_TRACE("deltas = " << core::CContainerPrinter::print(deltas))
    }
}

//! Convert the propagation decay rate into the corresponding regular
//! periodicity test decay rate.
double regularTestDecayRate(double decayRate)
{
    return CSeasonalComponent::TTime::scaleDecayRate(
               decayRate,
               CSeasonalComponentAdaptiveBucketing::timescale(),
               core::constants::WEEK);
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

// Daily + Weekly Test State Machine

// States
const std::size_t DW_INITIAL      = 0;
const std::size_t DW_SMALL_TEST   = 1;
const std::size_t DW_REGULAR_TEST = 2;
const std::size_t DW_NEVER_TEST   = 3;
const std::size_t DW_ERROR        = 4;
const std::string DW_STATES_[] =
    {
        "INITIAL",
        "SMALL_TEST",
        "REGULAR_TEST",
        "NEVER_TEST",
        "ERROR"
    };
const TStrVec DW_STATES(boost::begin(DW_STATES_), boost::end(DW_STATES_));
// Alphabet
const std::size_t DW_NEW_VALUE                    = 0;
const std::size_t DW_SMALL_TEST_TRUE              = 1;
const std::size_t DW_REGULAR_TEST_TIMED_OUT       = 2;
const std::size_t DW_FINISHED_TEST                = 3;
const std::size_t DW_DETECTED_INCOMPATIBLE_PERIOD = 4;
const std::size_t DW_RESET                        = 5;
const std::string DW_ALPHABET_[] =
    {
        "NEW_VALUE",
        "SMALL_TEST_TRUE",
        "REGULAR_TEST_TIMED_OUT",
        "FINISHED_TEST",
        "DETECTED_INCOMPATIBLE_PERIOD",
        "RESET"
    };
const TStrVec DW_ALPHABET(boost::begin(DW_ALPHABET_), boost::end(DW_ALPHABET_));
// Transition Function
const std::size_t DW_TRANSITION_FUNCTION_[][5] =
    {
        { DW_REGULAR_TEST, DW_SMALL_TEST,   DW_REGULAR_TEST, DW_NEVER_TEST, DW_ERROR   },
        { DW_ERROR,        DW_REGULAR_TEST, DW_ERROR,        DW_NEVER_TEST, DW_ERROR   },
        { DW_ERROR,        DW_ERROR,        DW_SMALL_TEST,   DW_NEVER_TEST, DW_ERROR   },
        { DW_ERROR,        DW_ERROR,        DW_NEVER_TEST,   DW_NEVER_TEST, DW_ERROR   },
        { DW_NEVER_TEST,   DW_NEVER_TEST,   DW_NEVER_TEST,   DW_NEVER_TEST, DW_ERROR   },
        { DW_INITIAL,      DW_INITIAL,      DW_INITIAL,      DW_NEVER_TEST, DW_INITIAL }
    };
const TSizeVecVec DW_TRANSITION_FUNCTION(function(DW_TRANSITION_FUNCTION_));

// Level Shift Test State Machine

// States
const std::size_t LS_INITIAL  = 0;
const std::size_t LS_SHIFT    = 1;
const std::size_t LS_NO_SHIFT = 2;
const std::size_t LS_WAIT     = 3;
const std::size_t LS_ERROR    = 4;
const std::string LS_STATES_[] =
    {
        "INITIAL",
        "SHIFT",
        "NO_SHIFT",
        "WAIT",
        "ERROR"
    };
const TStrVec LS_STATES(boost::begin(LS_STATES_), boost::end(LS_STATES_));
// Symbols
const std::size_t LS_NEW_VALUE         = 0;
const std::size_t LS_DETECTED_SHIFT    = 1;
const std::size_t LS_NO_DETECTED_SHIFT = 2;
const std::size_t LS_TIMED_OUT         = 3;
const std::size_t LS_RESET             = 4;
const std::string LS_ALPHABET_[] =
    {
        "NEW_VALUE",
        "DETECTED_SHIFT",
        "NO_DETECTED_SHIFT",
        "TIMED_OUT",
        "RESET"
    };
const TStrVec LS_ALPHABET(boost::begin(LS_ALPHABET_), boost::end(LS_ALPHABET_));
// Transition Function
const std::size_t LS_TRANSITION_FUNCTION_[][5] =
    {
        { LS_WAIT,    LS_SHIFT,    LS_NO_SHIFT, LS_WAIT,     LS_ERROR   },
        { LS_ERROR,   LS_SHIFT,    LS_SHIFT,    LS_ERROR,    LS_ERROR   },
        { LS_ERROR,   LS_NO_SHIFT, LS_NO_SHIFT, LS_ERROR,    LS_ERROR   },
        { LS_ERROR,   LS_WAIT,     LS_ERROR,    LS_NO_SHIFT, LS_ERROR   },
        { LS_INITIAL, LS_INITIAL,  LS_INITIAL,  LS_INITIAL,  LS_INITIAL }
    };
const TSizeVecVec LS_TRANSITION_FUNCTION(function(LS_TRANSITION_FUNCTION_));

// Components State Machine

// States
const std::size_t SC_NEW_COMPONENTS      = 0;
const std::size_t SC_NORMAL              = 1;
const std::size_t SC_SHIFTING_LEVEL      = 2;
const std::size_t SC_CAPTURE_LEVEL_SHIFT = 3;
const std::size_t SC_FORECASTING         = 4;
const std::size_t SC_DISABLED            = 5;
const std::size_t SC_ERROR               = 6;
const std::string SC_STATES_[] =
    {
        "NEW_COMPONENTS",
        "NORMAL",
        "SHIFTING_LEVEL",
        "CAPTURE_LEVEL_SHIFT",
        "FORECASTING",
        "DISABLED",
        "ERROR"
    };
const TStrVec SC_STATES(boost::begin(SC_STATES_), boost::end(SC_STATES_));
// Alphabet
const std::size_t SC_ADDED_COMPONENTS     = 0;
const std::size_t SC_START_SHIFTING_LEVEL = 1;
const std::size_t SC_STOP_SHIFTING_LEVEL  = 2;
const std::size_t SC_INTERPOLATED         = 3;
const std::size_t SC_FORECAST             = 4;
const std::size_t SC_RESET                = 5;
const std::string SC_ALPHABET_[] =
    {
        "ADDED_COMPONENTS",
        "START_SHIFTING_LEVEL",
        "STOP_SHIFTING_LEVEL",
        "INTERPOLATED",
        "FORECAST",
        "RESET"
    };
const TStrVec SC_ALPHABET(boost::begin(SC_ALPHABET_), boost::end(SC_ALPHABET_));
// Transition Function
const std::size_t SC_TRANSITION_FUNCTION_[][7] =
    {
        { SC_NEW_COMPONENTS, SC_NEW_COMPONENTS, SC_NEW_COMPONENTS,      SC_NEW_COMPONENTS,      SC_ERROR,       SC_DISABLED, SC_ERROR  },
        { SC_NEW_COMPONENTS, SC_SHIFTING_LEVEL, SC_SHIFTING_LEVEL,      SC_SHIFTING_LEVEL,      SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_NEW_COMPONENTS, SC_NORMAL,         SC_CAPTURE_LEVEL_SHIFT, SC_CAPTURE_LEVEL_SHIFT, SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_NORMAL,         SC_NORMAL,         SC_SHIFTING_LEVEL,      SC_NORMAL,              SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_ERROR,          SC_FORECASTING,    SC_CAPTURE_LEVEL_SHIFT, SC_FORECASTING,         SC_FORECASTING, SC_DISABLED, SC_ERROR  },
        { SC_NORMAL,         SC_NORMAL,         SC_NORMAL,              SC_NORMAL,              SC_NORMAL,      SC_DISABLED, SC_NORMAL }
    };
const TSizeVecVec SC_TRANSITION_FUNCTION(function(SC_TRANSITION_FUNCTION_));

// Daily+Weekly Test Tags
const std::string MACHINE_TAG("a");
const std::string LAST_TEST_TIME_TAG("b");
const std::string TIME_OUT_REGULAR_TEST_TAG("c");
const std::string REGULAR_TEST_TAG("d");
const std::string SMALL_TEST_TAG("e");

// Level Shift Test Tags
//const std::string MACHINE_TAG("a");
//const std::string LAST_TEST_TIME_TAG("b");
const std::string MEAN_PREDICTION_ERRORS_TAG("c");
const std::string LONG_TERM_MEAN_PREDICTION_ERROR_TAG("d");

// Seasonal Components Tags
//const std::string MACHINE_TAG("a");
const std::string LONGEST_PERIOD_TAG("b");
const std::string LAST_INTERPOLATE_TIME_TAG("c");
const std::string INTERPOLATION_WEIGHT_TAG("d");
const std::string LEVEL_TAG("e");
const std::string PERIODS_TAG("f");
const std::string COMPONENTS_TAG("g");
const std::string COMPONENT_TAG("h");
const std::string DELTA_TAG("i");
const std::string HISTORY_LENGTHS_TAG("j");
const std::string MOMENTS_TAG("k");

const core_t::TTime FORECASTING_INTERPOLATE_INTERVAL(core::constants::HOUR);
const core_t::TTime FORECASTING_PROPAGATION_FORWARDS_BY_TIME_INTERVAL(core::constants::HOUR);
const core_t::TTime FOREVER = boost::numeric::bounds<core_t::TTime>::highest();
const int APPLY_LEVEL_SHIFT_INTERVAL = 14;
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
                                                                   double mean) :
        SMessage(time),
        s_Value(value),
        s_WeightStyles(weightStyles),
        s_Weights(weights),
        s_Mean(mean)
{}

//////// SDetectedTrendMessage ////////

CTimeSeriesDecompositionDetail::SDetectedTrendMessage::SDetectedTrendMessage(core_t::TTime time) :
        SMessage(time)
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

//////// SHasLevelShiftMessage ////////

CTimeSeriesDecompositionDetail::SHasLevelShiftMessage::SHasLevelShiftMessage(core_t::TTime time,
                                                                             int intervals,
                                                                             double size) :
        SMessage(time), s_Intervals(intervals), s_Size(size)
{}

//////// SNoLongerHasLevelShiftMessage ////////

CTimeSeriesDecompositionDetail::SNoLongerHasLevelShiftMessage::SNoLongerHasLevelShiftMessage(core_t::TTime time) :
        SMessage(time)
{}

//////// CHandler ////////

CTimeSeriesDecompositionDetail::CHandler::CHandler(void) : m_Mediator(0) {}
CTimeSeriesDecompositionDetail::CHandler::~CHandler(void) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SAddValueMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDetectedTrendMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDetectedPeriodicMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SDiscardPeriodicMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SHasLevelShiftMessage &/*message*/) {}

void CTimeSeriesDecompositionDetail::CHandler::handle(const SNoLongerHasLevelShiftMessage &/*message*/) {}

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

//////// CDailyWeeklyTest ////////

CTimeSeriesDecompositionDetail::CDailyWeeklyTest::CDailyWeeklyTest(double decayRate,
                                                                   core_t::TTime bucketLength) :
        m_Machine(core::CStateMachine::create(DW_ALPHABET, DW_STATES,
                                              DW_TRANSITION_FUNCTION,
                                              DW_INITIAL)),
        m_DecayRate(decayRate),
        m_BucketLength(bucketLength),
        m_LastTestTime(),
        m_TimeOutRegularTest()
{}

CTimeSeriesDecompositionDetail::CDailyWeeklyTest::CDailyWeeklyTest(const CDailyWeeklyTest &other) :
        m_Machine(other.m_Machine),
        m_DecayRate(other.m_DecayRate),
        m_BucketLength(other.m_BucketLength),
        m_LastTestTime(other.m_LastTestTime),
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
        RESTORE(MACHINE_TAG,
                traverser.traverseSubLevel(
                              boost::bind(&core::CStateMachine::acceptRestoreTraverser, &m_Machine, _1)));
        RESTORE_BUILT_IN(LAST_TEST_TIME_TAG, m_LastTestTime)
        RESTORE_BUILT_IN(TIME_OUT_REGULAR_TEST_TAG, m_TimeOutRegularTest)
        if (name == REGULAR_TEST_TAG)
        {
            m_RegularTest.reset(new CTrendTests::CPeriodicity);
            double scaledDecayRate = CSeasonalComponent::TTime::scaleDecayRate(
                                         m_DecayRate,
                                         CSeasonalComponentAdaptiveBucketing::timescale(),
                                         core::constants::WEEK);
            if (traverser.traverseSubLevel(
                    boost::bind(&CTrendTests::CPeriodicity::acceptRestoreTraverser,
                                m_RegularTest.get(), scaledDecayRate, _1)) == false)
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
    inserter.insertValue(LAST_TEST_TIME_TAG, m_LastTestTime);
    inserter.insertValue(TIME_OUT_REGULAR_TEST_TAG, m_TimeOutRegularTest);
    if (m_RegularTest)
    {
        inserter.insertLevel(REGULAR_TEST_TAG,
                             boost::bind(&CTrendTests::CPeriodicity::acceptPersistInserter,
                                         m_RegularTest.get(), _1));
    }
    if (m_SmallTest)
    {
        inserter.insertLevel(SMALL_TEST_TAG,
                             boost::bind(&CTrendTests::CRandomizedPeriodicity::acceptPersistInserter,
                                         m_SmallTest.get(), _1));
    }
    inserter.insertLevel(PERIODS_TAG, boost::bind(&CTrendTests::CPeriodicity::CResult::acceptPersistInserter,
                                                  &m_Periods, _1));
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::swap(CDailyWeeklyTest &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_LastTestTime, other.m_LastTestTime);
    std::swap(m_TimeOutRegularTest, other.m_TimeOutRegularTest);
    m_RegularTest.swap(other.m_RegularTest);
    m_SmallTest.swap(other.m_SmallTest);
    std::swap(m_Periods, other.m_Periods);
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::handle(const SAddValueMessage &message)
{
    core_t::TTime time = message.s_Time;
    double value = message.s_Value;
    const TWeightStyleVec &weightStyles = message.s_WeightStyles;
    const TDouble4Vec &weights = message.s_Weights;

    this->test(message);

    switch (m_Machine.state())
    {
    case DW_NEVER_TEST:
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
    this->apply(DW_RESET, message);
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
            if (core::constants::DAY % periods[i] != 0 || periods[i] % core::constants::WEEK != 0)
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
    case DW_NEVER_TEST:
    case DW_INITIAL:
        break;
    case DW_SMALL_TEST:
        if (this->shouldTest(time))
        {
            LOG_TRACE("Small test at " << time);
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
            CTrendTests::CPeriodicity::CResult result = m_RegularTest->test();
            if (result.periodic() && result != m_Periods)
            {
                this->mediator()->forward(SDetectedPeriodicMessage(time, result, *m_RegularTest));
                m_Periods = result;
            }
            if (result.periodic() && m_RegularTest->seenSufficientData())
            {
                LOG_TRACE("Finished testing");
                this->apply(DW_FINISHED_TEST, message);
            }
        }
        break;
    default:
        LOG_ERROR("Test in a bad state: " << m_Machine.state());
        this->apply(DW_RESET, message);
        break;
    }
}

const CTrendTests::CPeriodicity::CResult &
CTimeSeriesDecompositionDetail::CDailyWeeklyTest::periods(void) const
{
    return m_Periods;
}

void CTimeSeriesDecompositionDetail::CDailyWeeklyTest::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
    if (m_RegularTest)
    {
        m_RegularTest->decayRate(decayRate);
    }
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
    m_LastTestTime = CIntegerTools::floor(m_LastTestTime + skipInterval + testInterval, testInterval);
    m_TimeOutRegularTest += skipInterval;
}

uint64_t CTimeSeriesDecompositionDetail::CDailyWeeklyTest::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Machine);
    seed = CChecksum::calculate(seed, m_LastTestTime);
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
            m_LastTestTime = time;
            m_TimeOutRegularTest = time + scale(4 * core::constants::WEEK, m_BucketLength);
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
                m_TimeOutRegularTest = time + scale(8 * core::constants::WEEK, m_BucketLength);
                m_SmallTest.reset();
            }
            if (!m_RegularTest)
            {
                m_RegularTest.reset(CTrendTests::dailyAndWeekly(m_BucketLength, regularTestDecayRate(m_DecayRate)));
            }
            break;
        case DW_NEVER_TEST:
        case DW_INITIAL:
            m_LastTestTime = core_t::TTime();
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
    core_t::TTime interval = this->testInterval();
    if (time - m_LastTestTime >= interval)
    {
        m_LastTestTime = CIntegerTools::floor(time, interval);
        return true;
    }
    return false;
}

core_t::TTime CTimeSeriesDecompositionDetail::CDailyWeeklyTest::testInterval(void) const
{
    switch (m_Machine.state())
    {
    case DW_SMALL_TEST:
        return core::constants::DAY;
    case DW_REGULAR_TEST:
        return m_RegularTest->seenSufficientData() ? core::constants::WEEK : core::constants::DAY;
    default:
        break;
    }
    return FOREVER;
}

core_t::TTime CTimeSeriesDecompositionDetail::CDailyWeeklyTest::timeOutRegularTest(void) const
{
    return m_TimeOutRegularTest + static_cast<core_t::TTime>(
                                      6.0 * static_cast<double>(core::constants::WEEK)
                                          * (1.0 - m_RegularTest->populatedRatio()));
}

//////// CLevelShiftTest ////////

CTimeSeriesDecompositionDetail::CLevelShiftTest::CLevelShiftTest(double decayRate) :
        m_Machine(core::CStateMachine::create(LS_ALPHABET, LS_STATES,
                                              LS_TRANSITION_FUNCTION,
                                              LS_INITIAL)),
        m_DecayRate(decayRate),
        m_LastTestTime(),
        m_TimeOutState()
{}

CTimeSeriesDecompositionDetail::CLevelShiftTest::CLevelShiftTest(const CLevelShiftTest &other) :
        m_Machine(core::CStateMachine::create(LS_ALPHABET, LS_STATES,
                                              LS_TRANSITION_FUNCTION,
                                              LS_INITIAL)),
        m_DecayRate(other.m_DecayRate),
        m_LastTestTime(other.m_LastTestTime),
        m_TimeOutState(other.m_TimeOutState),
        m_MeanPredictionErrors(other.m_MeanPredictionErrors),
        m_LongTermMeanPredictionError(other.m_LongTermMeanPredictionError)
{}

bool CTimeSeriesDecompositionDetail::CLevelShiftTest::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(MACHINE_TAG, traverser.traverseSubLevel(
                                 boost::bind(&core::CStateMachine::acceptRestoreTraverser, &m_Machine, _1)));
        RESTORE_BUILT_IN(LAST_TEST_TIME_TAG, m_LastTestTime)
        RESTORE(MEAN_PREDICTION_ERRORS_TAG, m_MeanPredictionErrors.fromDelimited(traverser.value()))
        RESTORE(LONG_TERM_MEAN_PREDICTION_ERROR_TAG, m_LongTermMeanPredictionError.fromDelimited(traverser.value()))
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(MACHINE_TAG, boost::bind(&core::CStateMachine::acceptPersistInserter, &m_Machine, _1));
    inserter.insertValue(LAST_TEST_TIME_TAG, m_LastTestTime);
    inserter.insertValue(MEAN_PREDICTION_ERRORS_TAG, m_MeanPredictionErrors.toDelimited());
    inserter.insertValue(LONG_TERM_MEAN_PREDICTION_ERROR_TAG, m_LongTermMeanPredictionError.toDelimited());
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::swap(CLevelShiftTest &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_LastTestTime, other.m_LastTestTime);
    std::swap(m_TimeOutState, other.m_TimeOutState);
    std::swap(m_MeanPredictionErrors, other.m_MeanPredictionErrors);
    std::swap(m_LongTermMeanPredictionError, other.m_LongTermMeanPredictionError);
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::handle(const SAddValueMessage &message)
{
    double value = message.s_Value;
    double mean = message.s_Mean;
    const TWeightStyleVec &weightStyles = message.s_WeightStyles;
    const TDouble4Vec &weights = message.s_Weights;

    double error = value - mean;
    double weight = maths_t::countForUpdate(weightStyles, weights);

    TVector errors;
    errors(0) = error;
    errors(1) = ::fabs(error);
    m_MeanPredictionErrors.add(errors, weight);

    this->test(message);
    this->apply(LS_NEW_VALUE, message);
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::handle(const SDetectedTrendMessage &message)
{
    this->apply(LS_RESET, message);
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::handle(const SDetectedPeriodicMessage &message)
{
    this->apply(LS_RESET, message);
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::test(const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    if (this->shouldTest(time))
    {
        double count = CBasicStatistics::count(m_MeanPredictionErrors);
        TVector errors = CBasicStatistics::mean(m_MeanPredictionErrors);
        m_MeanPredictionErrors = TVectorMeanAccumulator();

        bool captureError = true;
        std::size_t state = m_Machine.state();

        switch (state)
        {
        case LS_SHIFT:
            if (--m_TimeOutState == 0)
            {
                captureError = false;
                this->mediator()->forward(SNoLongerHasLevelShiftMessage(time));
                this->apply(LS_TIMED_OUT, message);
                break;
            }
            BOOST_FALLTHROUGH;
        case LS_NO_SHIFT:
            if (CBasicStatistics::count(m_LongTermMeanPredictionError) > 0.0)
            {
                int intervals = (APPLY_LEVEL_SHIFT_INTERVAL - m_TimeOutState)
                               % APPLY_LEVEL_SHIFT_INTERVAL;
                double m = 1.0 / (LARGE_SHIFT - SMALL_SHIFT);
                double c = m * SMALL_SHIFT;
                double shift = ::fabs(errors(0));
                double error = CBasicStatistics::mean(m_LongTermMeanPredictionError);
                double size = error == 0.0 ? (shift > 0.0 ? 1.0 : 0.0) : m * shift / error - c;
                LOG_TRACE("shift = " << errors(0) << ", error = " << error << ", size = " << size);

                if (size > 0.0)
                {
                    captureError = false;
                    this->mediator()->forward(SHasLevelShiftMessage(time, intervals, size));
                    this->apply(LS_DETECTED_SHIFT, message);
                }
                else if (state == LS_SHIFT)
                {
                    this->mediator()->forward(SNoLongerHasLevelShiftMessage(time));
                    this->apply(LS_NO_DETECTED_SHIFT, message);
                }
            }
            break;
        case LS_WAIT:
            if (--m_TimeOutState == 0)
            {
                this->apply(LS_TIMED_OUT, message);
            }
            break;
        case LS_INITIAL:
            break;
        default:
            LOG_ERROR("Test in a bad state: " << state);
            this->apply(LS_RESET, message);
            break;
        }
        if (captureError)
        {
            m_LongTermMeanPredictionError.add(errors(1), count);
        }
    }
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::propagateForwardsByTime(double time)
{
    time *= m_Machine.state() == LS_SHIFT ? 10.0 : 1.0;
    m_LongTermMeanPredictionError.age(::exp(-m_DecayRate * time));
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::apply(std::size_t symbol, const SMessage &message)
{
    core_t::TTime time = message.s_Time;

    std::size_t old = m_Machine.state();
    m_Machine.apply(symbol);
    std::size_t state = m_Machine.state();

    if (state != old)
    {
        LOG_TRACE(LS_STATES[old] << "," << LS_ALPHABET[symbol] << " -> " << LS_STATES[state]);

        if (old == LS_INITIAL)
        {
            m_LastTestTime = CIntegerTools::floor(time, this->testInterval());
        }

        switch (state)
        {
        case LS_WAIT:
        case LS_SHIFT:
            m_TimeOutState = APPLY_LEVEL_SHIFT_INTERVAL;
            break;
        case LS_NO_SHIFT:
            break;
        case LS_INITIAL:
            m_LastTestTime = core_t::TTime();
            m_MeanPredictionErrors = TVectorMeanAccumulator();
            m_LongTermMeanPredictionError = TMeanAccumulator();
            break;
        default:
            LOG_ERROR("Test in a bad state: " << state);
            this->apply(LS_RESET, message);
            break;
        }
    }
}

void CTimeSeriesDecompositionDetail::CLevelShiftTest::skipTime(core_t::TTime skipInterval)
{
    core_t::TTime testInterval = this->testInterval();
    m_LastTestTime = CIntegerTools::floor(m_LastTestTime + skipInterval + testInterval, testInterval);
}

uint64_t CTimeSeriesDecompositionDetail::CLevelShiftTest::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Machine);
    seed = CChecksum::calculate(seed, m_LastTestTime);
    seed = CChecksum::calculate(seed, m_MeanPredictionErrors);
    return CChecksum::calculate(seed, m_LongTermMeanPredictionError);
}

bool CTimeSeriesDecompositionDetail::CLevelShiftTest::shouldTest(core_t::TTime time)
{
    core_t::TTime interval = this->testInterval();
    if (time - m_LastTestTime >= interval)
    {
        m_LastTestTime = CIntegerTools::floor(time, interval);
        return true;
    }
    return false;
}

core_t::TTime CTimeSeriesDecompositionDetail::CLevelShiftTest::testInterval(void) const
{
    return CSeasonalComponentAdaptiveBucketing::timescale();
}

const double CTimeSeriesDecompositionDetail::CLevelShiftTest::SMALL_SHIFT = 2.0;
const double CTimeSeriesDecompositionDetail::CLevelShiftTest::LARGE_SHIFT = 3.0;

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
        m_LastInterpolateTime(),
        m_InterpolationWeight(1.0),
        m_Level(0.0),
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
        m_LastInterpolateTime(other.m_LastInterpolateTime),
        m_InterpolationWeight(other.m_InterpolationWeight),
        m_Level(other.m_Level),
        m_Seasonal(other.m_Seasonal ? new SSeasonal(*other.m_Seasonal) : 0),
        m_Moments(other.m_Moments),
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
        RESTORE_BUILT_IN(LAST_INTERPOLATE_TIME_TAG, m_LastInterpolateTime)
        RESTORE_BUILT_IN(INTERPOLATION_WEIGHT_TAG, m_InterpolationWeight)
        RESTORE_BUILT_IN(LEVEL_TAG, m_Level)
        RESTORE_SETUP_TEARDOWN(COMPONENTS_TAG,
                               m_Seasonal.reset(new SSeasonal),
                               traverser.traverseSubLevel(boost::bind(&SSeasonal::acceptRestoreTraverser,
                                                                      m_Seasonal.get(),
                                                                      m_DecayRate, m_BucketLength, _1)),
                               /**/)
        RESTORE(MOMENTS_TAG, m_Moments.fromDelimited(traverser.value()))
    }
    while (traverser.next());

    return true;
}

void CTimeSeriesDecompositionDetail::CComponents::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(MACHINE_TAG, boost::bind(&core::CStateMachine::acceptPersistInserter, &m_Machine, _1));
    inserter.insertValue(LONGEST_PERIOD_TAG, m_LongestPeriod);
    inserter.insertValue(LAST_INTERPOLATE_TIME_TAG, m_LastInterpolateTime);
    inserter.insertValue(INTERPOLATION_WEIGHT_TAG, m_InterpolationWeight);
    inserter.insertValue(LEVEL_TAG, m_Level);
    if (m_Seasonal)
    {
        inserter.insertLevel(COMPONENTS_TAG, boost::bind(&SSeasonal::acceptPersistInserter, m_Seasonal.get(), _1));
    }
    inserter.insertValue(MOMENTS_TAG, m_Moments.toDelimited());
}

void CTimeSeriesDecompositionDetail::CComponents::swap(CComponents &other)
{
    std::swap(m_Machine, other.m_Machine);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_LongestPeriod, other.m_LongestPeriod);
    std::swap(m_SeasonalComponentSize, other.m_SeasonalComponentSize);
    std::swap(m_LastInterpolateTime, other.m_LastInterpolateTime);
    std::swap(m_InterpolationWeight, other.m_InterpolationWeight);
    std::swap(m_Level, other.m_Level);
    m_Seasonal.swap(other.m_Seasonal);
    std::swap(m_Moments, other.m_Moments);
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SAddValueMessage &message)
{
    m_Moments.add(message.s_Value);

    switch (m_Machine.state())
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    case SC_SHIFTING_LEVEL:
    case SC_CAPTURE_LEVEL_SHIFT:
        if (m_Seasonal)
        {
            this->interpolate(message);

            double value = message.s_Value;
            core_t::TTime time = message.s_Time;
            const TWeightStyleVec &weightStyles = message.s_WeightStyles;
            const TDouble4Vec &weights = message.s_Weights;

            TComponentVec &components = m_Seasonal->s_Components;
            TComponentPtrVec componentsToUpdate;
            TDoubleVec deltas;

            if (!components.empty())
            {
                componentsToUpdate.reserve(components.size());
                deltas.reserve(components.size());
                for (std::size_t i = 0u; i < components.size(); ++i)
                {
                    CSeasonalComponent *component = &components[i];
                    if (component->time().inWindow(time))
                    {
                        componentsToUpdate.push_back(component);
                        deltas.push_back(m_Seasonal->s_Deltas[i]);
                    }
                }
            }

            double weight = maths_t::countForUpdate(weightStyles, weights);
            std::size_t n = componentsToUpdate.size();

            if (n > 0)
            {
                TDoubleVec values(n, value);
                decompose(componentsToUpdate, deltas, time, values);
                for (std::size_t i = 0u; i < n; ++i)
                {
                    CSeasonalComponent *component = componentsToUpdate[i];
                    double wi = weight * static_cast<double>(m_LongestPeriod)
                                       / static_cast<double>(component->time().window());
                    LOG_TRACE("Adding " << values[i]
                              << " to component with period " << component->time().period());
                    component->add(time, values[i], wi);
                }
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

void CTimeSeriesDecompositionDetail::CComponents::handle(const SDetectedTrendMessage &/*message*/)
{
    // TODO support when we are modeling a trend.
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
            CTrendTests::SELECT_DAILY, CTrendTests::SELECT_WEEKLY
        };
    static core_t::TTime PERIODS[] = { core::constants::DAY, core::constants::WEEK };
    static core_t::TTime WINDOWS[][2] =
        {
            { core::constants::WEEKEND, core::constants::WEEK    },
            { 0,                        core::constants::WEEKEND },
            { 0,                        core::constants::WEEK    }
        };

    switch (m_Machine.state())
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    case SC_SHIFTING_LEVEL:
    case SC_CAPTURE_LEVEL_SHIFT:
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
        TDoubleVec &deltas = m_Seasonal->s_Deltas;
        TDoubleVec &history = m_Seasonal->s_HistoryLengths;
        components.clear();
        deltas.clear();
        history.clear();

        CTrendTests::TTimeTimePrMeanVarAccumulatorPrVec trends[6];
        message.s_Test.trends(periods, trends);

        LOG_DEBUG("Detected " << CTrendTests::printDailyAndWeekly(periods));
        LOG_DEBUG("Start of week " << startOfWeek);
        LOG_DEBUG("Estimated new periods at '" << time << "'");

        double bucketLength = static_cast<double>(m_BucketLength);
        std::size_t sizes[][2] =
            {
                { m_SeasonalComponentSize, m_SeasonalComponentSize     },
                { m_SeasonalComponentSize, m_SeasonalComponentSize / 2 },
                { m_SeasonalComponentSize, m_SeasonalComponentSize * 2 }
            };
        core_t::TTime times[][2] =
            {
                { time - core::constants::WEEK, time - core::constants::WEEK },
                { time - core::constants::WEEK, time - core::constants::WEEK },
                { time - core::constants::DAY,  time - core::constants::WEEK }
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
        deltas.resize(components.size(), 0.0);
        history.resize(components.size(), 0.0);

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
        TDoubleVec &deltas = m_Seasonal->s_Deltas;
        TDoubleVec &history = m_Seasonal->s_HistoryLengths;

        std::size_t last = 0u;
        for (std::size_t i = 0u; i < components.size(); ++i)
        {
            if (last != i)
            {
                components[i].swap(components[last]);
                std::swap(deltas[i], deltas[last]);
                std::swap(history[i], history[last]);
            }
            if (!std::binary_search(periods.begin(), periods.end(), components[i].time().period()))
            {
                ++last;
            }
            else if (m_Watcher)
            {
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
            deltas.erase(deltas.begin() + last, deltas.end());
            history.erase(history.begin() + last, history.end());
        }
    }
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SHasLevelShiftMessage &message)
{
    m_InterpolationWeight = 1.0 - std::min(message.s_Size, 1.0);
    this->apply(SC_START_SHIFTING_LEVEL, message);
    if (m_Seasonal)
    {
        double history = *std::max_element(m_Seasonal->s_HistoryLengths.begin(),
                                           m_Seasonal->s_HistoryLengths.end());
        if (history < 1.0)
        {
            this->apply(SC_STOP_SHIFTING_LEVEL, message);
        }
    }
}

void CTimeSeriesDecompositionDetail::CComponents::handle(const SNoLongerHasLevelShiftMessage &message)
{
    this->apply(SC_STOP_SHIFTING_LEVEL, message);
}

void CTimeSeriesDecompositionDetail::CComponents::interpolate(const SMessage &message)
{
    std::size_t state = m_Machine.state();
    switch (state)
    {
    case SC_NORMAL:
    case SC_NEW_COMPONENTS:
    case SC_SHIFTING_LEVEL:
    case SC_CAPTURE_LEVEL_SHIFT:
    case SC_FORECASTING:
        if (m_Seasonal)
        {
            core_t::TTime time = message.s_Time;
            TComponentVec &components = m_Seasonal->s_Components;
            TDoubleVec &deltas = m_Seasonal->s_Deltas;
            TDoubleVec &history = m_Seasonal->s_HistoryLengths;

            if (this->shouldInterpolate(time))
            {
                LOG_TRACE("Interpolating values at " << time);
                TTimeTimePrDoubleFMap shifts;
                for (std::size_t i = 0u; i < components.size(); ++i)
                {
                    CSeasonalComponent &component = components[i];
                    double mean = component.meanValue();
                    component.interpolate(m_LastInterpolateTime,
                                          m_InterpolationWeight,
                                          state != SC_FORECASTING);
                    if (state == SC_CAPTURE_LEVEL_SHIFT)
                    {
                        double shift = component.meanValue() - mean;
                        shifts[std::make_pair(component.time().windowStart(),
                                              component.time().windowEnd())] += shift;
                    }
                    if (component.time().inWindow(time))
                    {
                        history[i] += 1.0;
                    }
                }

                minimizeAbsSlope(components, deltas);

                TMeanAccumulator shift;
                for (TTimeTimePrDoubleFMapCItr i = shifts.begin(); i < shifts.end(); ++i)
                {
                    shift.add(i->second, i->first.second - i->first.first);
                }
                m_Level += CBasicStatistics::mean(shift);

                this->apply(SC_INTERPOLATED, message);
            }
        }
        break;
    case SC_DISABLED:
        break;
    default:
        LOG_ERROR("Components in a bad state: " << m_Machine.state());
        this->apply(SC_RESET, message);
        break;
    }
}

void CTimeSeriesDecompositionDetail::CComponents::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
    if (m_Seasonal)
    {
        for (std::size_t i = 0u; i < m_Seasonal->s_Components.size(); ++i)
        {
            m_Seasonal->s_Components[i].decayRate(decayRate);
        }
    }
}

void CTimeSeriesDecompositionDetail::CComponents::propagateForwards(core_t::TTime start,
                                                                    core_t::TTime end)
{
    double time =  static_cast<double>(end - start)
                 / static_cast<double>(this->propagationForwardsByTimeInterval());

    time *= m_Machine.state() == SC_SHIFTING_LEVEL ? 5.0 : 1.0;
    double factor = ::exp(-m_DecayRate * time);

    if (m_Seasonal)
    {
        for (std::size_t i = 0u; i < m_Seasonal->s_Components.size(); ++i)
        {
            CSeasonalComponent &component = m_Seasonal->s_Components[i];
            component.propagateForwardsByTime(time, m_Machine.state() == SC_SHIFTING_LEVEL);
            if (component.time().inWindow((start + end) / 2))
            {
                m_Seasonal->s_HistoryLengths[i] *= factor;
            }
        }
    }

    m_Moments.age(factor);
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
    const TComponentVec &components = this->seasonal();
    for (std::size_t i = 0u; components.size(); ++i)
    {
        if (components[i].initialized())
        {
            return true;
        }
    }
    return false;
}

const TComponentVec &CTimeSeriesDecompositionDetail::CComponents::seasonal(void) const
{
    return m_Seasonal ? m_Seasonal->s_Components : NO_COMPONENTS;
}

double CTimeSeriesDecompositionDetail::CComponents::meanValue(void) const
{
    return this->initialized() ? meanOf(&CSeasonalComponent::meanValue, this->seasonal()) :
                                 CBasicStatistics::mean(m_Moments);
}

double CTimeSeriesDecompositionDetail::CComponents::meanValue(core_t::TTime time) const
{
    return this->initialized() ? meanValueAt(time, this->seasonal()) :
                                 CBasicStatistics::mean(m_Moments);
}

double CTimeSeriesDecompositionDetail::CComponents::meanVariance(void) const
{
    return this->initialized() ? meanOf(&CSeasonalComponent::meanVariance, this->seasonal()) :
                                 CBasicStatistics::maximumLikelihoodVariance(m_Moments);
}

double CTimeSeriesDecompositionDetail::CComponents::level(void) const
{
    return m_Level;
}

void CTimeSeriesDecompositionDetail::CComponents::skipTime(core_t::TTime skipInterval)
{
    core_t::TTime interpolateInterval = this->interpolateInterval();
    m_LastInterpolateTime = CIntegerTools::floor(  m_LastInterpolateTime
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
    seed = CChecksum::calculate(seed, m_LastInterpolateTime);
    seed = CChecksum::calculate(seed, m_InterpolationWeight);
    seed = CChecksum::calculate(seed, m_Level);
    seed = CChecksum::calculate(seed, m_Seasonal);
    return CChecksum::calculate(seed, m_Moments);
}

void CTimeSeriesDecompositionDetail::CComponents::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CComponents");
    core::CMemoryDebug::dynamicSize("m_Seasonal", m_Seasonal, mem);
}

std::size_t CTimeSeriesDecompositionDetail::CComponents::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(m_Seasonal);
}

void CTimeSeriesDecompositionDetail::CComponents::apply(std::size_t symbol, const SMessage &message)
{
    if (symbol == SC_RESET)
    {
        // TODO broadcast recover message.
        m_Seasonal.reset();
    }

    std::size_t old = m_Machine.state();

    m_Machine.apply(symbol);
    std::size_t state = m_Machine.state();

    if (state != old)
    {
        LOG_TRACE(SC_STATES[old] << "," << SC_ALPHABET[symbol] << " -> " << SC_STATES[state]);

        if (old == SC_SHIFTING_LEVEL)
        {
            if (m_Seasonal)
            {
                m_Seasonal->resetVariance(this->meanVariance());
            }
            m_InterpolationWeight = 1.0;
        }

        switch (state)
        {
        case SC_NORMAL:
        case SC_FORECASTING:
            break;
        case SC_SHIFTING_LEVEL:
            break;
        case SC_CAPTURE_LEVEL_SHIFT:
            this->interpolate(message);
            this->apply(symbol, message);
            break;
        case SC_NEW_COMPONENTS:
            this->interpolate(message);
            break;
        case SC_DISABLED:
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

    if (state == SC_NEW_COMPONENTS || state == SC_CAPTURE_LEVEL_SHIFT)
    {
        m_LastInterpolateTime = CIntegerTools::floor(time, interpolateInterval);
        return true;
    }

    if (time - m_LastInterpolateTime >= interpolateInterval)
    {
        m_LastInterpolateTime = CIntegerTools::floor(time, interpolateInterval);
        return m_InterpolationWeight > 0.0;
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

bool CTimeSeriesDecompositionDetail::CComponents::SSeasonal::acceptRestoreTraverser(double decayRate,
                                                                                    core_t::TTime bucketLength,
                                                                                    core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_NO_ERROR(COMPONENT_TAG,
                         s_Components.push_back(CSeasonalComponent(decayRate,
                                                                   static_cast<double>(bucketLength),
                                                                   traverser)))
        RESTORE_SETUP_TEARDOWN(DELTA_TAG,
                               double delta,
                               core::CStringUtils::stringToType(traverser.value(), delta),
                               s_Deltas.push_back(delta))
        RESTORE_SETUP_TEARDOWN(HISTORY_LENGTHS_TAG,
                               double history,
                               core::CStringUtils::stringToType(traverser.value(), history),
                               s_HistoryLengths.push_back(history))
    }
    while (traverser.next());
    return true;
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        inserter.insertLevel(COMPONENT_TAG, boost::bind(&CSeasonalComponent::acceptPersistInserter, &s_Components[i], _1));
        inserter.insertValue(DELTA_TAG, s_Deltas[i], core::CIEEE754::E_SinglePrecision);
        inserter.insertValue(HISTORY_LENGTHS_TAG, s_HistoryLengths[i], core::CIEEE754::E_SinglePrecision);
    }
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::resetVariance(double variance)
{
    for (std::size_t i = 0u; i < s_Components.size(); ++i)
    {
        s_Components[i].resetVariance(variance);
    }
}

uint64_t CTimeSeriesDecompositionDetail::CComponents::SSeasonal::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, s_Components);
    seed = CChecksum::calculate(seed, s_Deltas);
    return CChecksum::calculate(seed, s_HistoryLengths);
}

void CTimeSeriesDecompositionDetail::CComponents::SSeasonal::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("SSeasonal");
    core::CMemoryDebug::dynamicSize("s_Components", s_Components, mem);
    core::CMemoryDebug::dynamicSize("s_Deltas", s_Deltas, mem);
}

std::size_t CTimeSeriesDecompositionDetail::CComponents::SSeasonal::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(s_Components) + core::CMemory::dynamicSize(s_Deltas);
}

}
}
