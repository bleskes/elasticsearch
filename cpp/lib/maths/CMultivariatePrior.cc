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

#include <maths/CMultivariatePrior.h>

#include <core/CLogger.h>

#include <maths/CSetTools.h>
#include <maths/CMathsFuncs.h>

#include <boost/iterator/transform_iterator.hpp>

namespace prelert
{
namespace maths
{

namespace
{

void setDecayRate(double value, double fallback, double &result)
{
    if (CMathsFuncs::isFinite(value))
    {
        result = value;
    }
    else
    {
        LOG_ERROR("Invalid decay rate " << value);
        result = fallback;
    }
}

}

CMultivariatePrior::CMultivariatePrior(void) :
        m_DataType(maths_t::E_DiscreteData),
        m_DecayRate(0.0),
        m_NumberSamples(0)
{
}

CMultivariatePrior::CMultivariatePrior(maths_t::EDataType dataType,
                                       double decayRate) :
        m_DataType(dataType),
        m_NumberSamples(0)
{
    setDecayRate(decayRate, FALLBACK_DECAY_RATE, m_DecayRate);
}

CMultivariatePrior::~CMultivariatePrior(void)
{
}

void CMultivariatePrior::swap(CMultivariatePrior &other)
{
    std::swap(m_DataType, other.m_DataType);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_NumberSamples, other.m_NumberSamples);
}

bool CMultivariatePrior::isDiscrete(void) const
{
    return    m_DataType == maths_t::E_DiscreteData
           || m_DataType == maths_t::E_IntegerData;
}

bool CMultivariatePrior::isInteger(void) const
{
    return m_DataType == maths_t::E_IntegerData;
}

maths_t::EDataType CMultivariatePrior::dataType(void) const
{
    return m_DataType;
}

double CMultivariatePrior::decayRate(void) const
{
    return m_DecayRate;
}

void CMultivariatePrior::dataType(maths_t::EDataType value)
{
    m_DataType = value;
}

void CMultivariatePrior::decayRate(double value)
{
    setDecayRate(value, FALLBACK_DECAY_RATE, m_DecayRate);
}

void CMultivariatePrior::addSamples(const TWeightStyleVec &weightStyles,
                                    const TDouble10Vec1Vec &/*samples*/,
                                    const TDouble10Vec4Vec1Vec &weights)
{
    std::size_t d = this->dimension();
    TDouble10Vec n(d, 0.0);
    try
    {
        for (std::size_t i = 0u; i < weights.size(); ++i)
        {
            TDouble10Vec wi = maths_t::countForUpdate(d, weightStyles, weights[i]);
            for (std::size_t j = 0u; j < d; ++j)
            {
                n[j] += wi[j];
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract sample counts: " << e.what());
    }
    this->addSamples(smallest(n));
}

CMultivariatePrior::TDouble10Vec
CMultivariatePrior::nearestMarginalLikelihoodMean(const TDouble10Vec &/*value*/) const
{
    return this->marginalLikelihoodMean();
}

bool CMultivariatePrior::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                        const TWeightStyleVec &weightStyles,
                                                        const TDouble10Vec1Vec &samples,
                                                        const TDouble10Vec4Vec1Vec &weights,
                                                        const TSize10Vec &coordinates,
                                                        TDouble10Vec2Vec &lowerBounds,
                                                        TDouble10Vec2Vec &upperBounds,
                                                        TTail10Vec &tail) const
{
    if (coordinates.empty())
    {
        lowerBounds.clear();
        upperBounds.clear();
        tail.clear();
        return true;
    }

    lowerBounds.assign(2, TDouble10Vec(coordinates.size(), 1.0));
    upperBounds.assign(2, TDouble10Vec(coordinates.size(), 1.0));
    tail.assign(coordinates.size(), maths_t::E_UndeterminedTail);

    if (samples.empty())
    {
        LOG_ERROR("Can't compute distribution for empty sample set");
        return false;
    }
    if (!this->check(samples, weights))
    {
        return false;
    }

    typedef core::CSmallVector<double, 1> TDouble1Vec;
    typedef core::CSmallVector<double, 4> TDouble4Vec;
    typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
    typedef core::CSmallVector<CJointProbabilityOfLessLikelySamples, 10> TJointProbabilityOfLessLikelySamplesVec;

    TJointProbabilityOfLessLikelySamplesVec lowerBounds_[2] =
        {
            TJointProbabilityOfLessLikelySamplesVec(coordinates.size()),
            TJointProbabilityOfLessLikelySamplesVec(coordinates.size())
        };
    TJointProbabilityOfLessLikelySamplesVec upperBounds_[2] =
        {
            TJointProbabilityOfLessLikelySamplesVec(coordinates.size()),
            TJointProbabilityOfLessLikelySamplesVec(coordinates.size())
        };

    std::size_t d = this->dimension();
    std::size_t n = samples.size();

    TSize10Vec marginalize(d - 1);
    TSizeDoublePr10Vec condition(d - 1);
    TDouble1Vec sc(1);
    TDouble4Vec1Vec wc(1, TDouble4Vec(weightStyles.size()));

    for (std::size_t i = 0; i < coordinates.size(); ++i)
    {
        std::size_t c = coordinates[i];

        for (std::size_t k = 0u, l = 0u; k < d; ++k)
        {
            if (k != c)
            {
                marginalize[l++] = k;
            }
        }
        boost::shared_ptr<CPrior> margin(this->univariate(marginalize, TSizeDoublePr10Vec()).first);
        if (!margin)
        {
            return false;
        }

        for (std::size_t j = 0u; j < n; ++j)
        {
            for (std::size_t k = 0u, l = 0u; k < d; ++k)
            {
                if (k != c)
                {
                    condition[l++] = std::make_pair(k, samples[j][k]);
                }
            }

            sc[0] = samples[j][c];
            for (std::size_t k = 0u; k < weights[j].size(); ++k)
            {
                wc[0][k] = weights[j][k][c];
            }

            double lb[2], ub[2];
            maths_t::ETail tc[2];

            if (!margin->probabilityOfLessLikelySamples(calculation,
                                                        weightStyles, sc, wc,
                                                        lb[0], ub[0], tc[0]))
            {
                LOG_ERROR("Failed to compute probability for coordinate " << c);
                return false;
            }
            LOG_TRACE("lb(" << c << ") = " << lb[0] << ", ub(" << c << ") = " << ub[0]);

            boost::shared_ptr<CPrior> conditional(this->univariate(TSize10Vec(), condition).first);
            if (!conditional->probabilityOfLessLikelySamples(calculation,
                                                             weightStyles, sc, wc,
                                                             lb[1], ub[1], tc[1]))
            {
                LOG_ERROR("Failed to compute probability for coordinate " << c);
                return false;
            }
            LOG_TRACE("lb(" << c << "|.) = " << lb[1] << ", ub(" << c << "|.) = " << ub[1]);

            lowerBounds_[0][i].add(lb[0]);
            upperBounds_[0][i].add(ub[0]);
            lowerBounds_[1][i].add(lb[1]);
            upperBounds_[1][i].add(ub[1]);
            tail[i] = static_cast<maths_t::ETail>(tail[i] | tc[1]);
        }
    }

    for (std::size_t i = 0; i < coordinates.size(); ++i)
    {
        if (   !lowerBounds_[0][i].calculate(lowerBounds[0][i])
            || !upperBounds_[0][i].calculate(upperBounds[0][i])
            || !lowerBounds_[1][i].calculate(lowerBounds[1][i])
            || !upperBounds_[1][i].calculate(upperBounds[1][i]))
        {
            LOG_ERROR("Failed to compute probability for coordinate " << coordinates[i]);
            return false;
        }
    }

    return true;
}

bool CMultivariatePrior::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                        const TWeightStyleVec &weightStyles,
                                                        const TDouble10Vec1Vec &samples,
                                                        const TDouble10Vec4Vec1Vec &weights,
                                                        double &lowerBound,
                                                        double &upperBound,
                                                        TTail10Vec &tail) const
{
    lowerBound = upperBound = 1.0;
    tail.assign(this->dimension(), maths_t::E_UndeterminedTail);

    if (this->isNonInformative())
    {
        return true;
    }

    CJointProbabilityOfLessLikelySamples lowerBound_[2];
    CJointProbabilityOfLessLikelySamples upperBound_[2];

    TDouble10Vec1Vec sample(1);
    TDouble10Vec4Vec1Vec weight(1);
    TSize10Vec dimensions(boost::counting_iterator<std::size_t>(0),
                          boost::counting_iterator<std::size_t>(this->dimension()));
    TDouble10Vec2Vec lbs;
    TDouble10Vec2Vec ubs;
    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        sample[0] = samples[i];
        weight[0] = weights[i];
        if (!this->probabilityOfLessLikelySamples(calculation, weightStyles,
                                                  sample, weight, dimensions,
                                                  lbs, ubs, tail))
        {
            return false;
        }

        for (std::size_t j = 0u; j < this->dimension(); ++j)
        {
            lowerBound_[0].add(lbs[0][j]);
            upperBound_[0].add(ubs[0][j]);
            lowerBound_[1].add(lbs[1][j]);
            upperBound_[1].add(ubs[1][j]);
        }
    }

    double lb[2], ub[2];
    if (   !lowerBound_[0].calculate(lb[0])
        || !upperBound_[0].calculate(ub[0])
        || !lowerBound_[1].calculate(lb[1])
        || !upperBound_[1].calculate(ub[1]))
    {
        return false;
    }
    LOG_TRACE("lb = " << core::CContainerPrinter::print(lb)
              << ", ub = " << core::CContainerPrinter::print(ub));

    lowerBound = ::sqrt(lb[0] * lb[1]);
    upperBound = ::sqrt(ub[0] * ub[1]);
    return true;
}

std::string CMultivariatePrior::printMarginalLikelihoodFunction(std::size_t x, std::size_t y) const
{
    // We'll plot the marginal likelihood function over a range where
    // most of the mass is, i.e. the 99% confidence interval.

    typedef std::pair<double, double> TDoubleDoublePr;

    static const double RANGE = 99.0;
    static const unsigned int POINTS = 51;

    std::size_t d = this->dimension();

    TSize10Vec xm;
    TSize10Vec ym;
    TSize10Vec xym;
    xm.reserve(d - 1);
    ym.reserve(d - 1);
    xym.reserve(d - 2);
    for (std::size_t i = 0u; i < d; ++i)
    {
        if (i != x && i != y)
        {
            xm.push_back(i);
            ym.push_back(i);
            xym.push_back(i);
        }
        else if (i != x)
        {
            xm.push_back(i);
        }
        else if (i != y)
        {
            ym.push_back(i);
        }
    }

    boost::shared_ptr<CPrior> xMargin(this->univariate(xm, TSizeDoublePr10Vec()).first);

    if (x == y)
    {
        return xMargin != 0 ? xMargin->printMarginalLikelihoodFunction() : std::string();
    }

    boost::shared_ptr<CPrior> yMargin(this->univariate(ym, TSizeDoublePr10Vec()).first);
    boost::shared_ptr<CMultivariatePrior> xyMargin(this->bivariate(xym, TSizeDoublePr10Vec()).first);

    TDoubleDoublePr xRange = xMargin->marginalLikelihoodConfidenceInterval(RANGE);
    TDoubleDoublePr yRange = yMargin->marginalLikelihoodConfidenceInterval(RANGE);

    double dx = (xRange.second - xRange.first) / (POINTS - 1.0);
    double dy = (yRange.second - yRange.first) / (POINTS - 1.0);

    std::ostringstream xabscissa;
    std::ostringstream yabscissa;
    std::ostringstream likelihood;

    xabscissa << "x = [";
    yabscissa << "y = [";
    double x_ = xRange.first;
    double y_ = yRange.first;
    for (std::size_t i = 0u; i < POINTS; ++i, x_ += dx, y_ += dy)
    {
        xabscissa << x_ << " ";
        yabscissa << y_ << " ";
    }
    xabscissa << "];" << core_t::LINE_ENDING;
    yabscissa << "];" << core_t::LINE_ENDING;

    likelihood << "likelihood = [";
    TDouble10Vec1Vec sample(1, TDouble10Vec(2));
    TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
    x_ = xRange.first;
    for (std::size_t i = 0u; i < POINTS; ++i, x_ += dx)
    {
        y_ = yRange.first;
        for (std::size_t j = 0u; j < POINTS; ++j, y_ += dy)
        {
            sample[0][0] = x_;
            sample[0][1] = y_;
            double l;
            xyMargin->jointLogMarginalLikelihood(CConstantWeights::COUNT, sample, weight, l);
            likelihood << ::exp(l) << " ";
        }
        likelihood << core_t::LINE_ENDING;
    }
    likelihood << "];" << core_t::LINE_ENDING << "contour(x, y, likelihood', 20);";

    return xabscissa.str() + yabscissa.str() + likelihood.str();

}

uint64_t CMultivariatePrior::checksum(uint64_t seed) const
{
    seed = maths::CChecksum::calculate(seed, m_DataType);
    seed = maths::CChecksum::calculate(seed, m_DecayRate);
    return maths::CChecksum::calculate(seed, m_NumberSamples);
}

std::string CMultivariatePrior::print(void) const
{
    std::string result;
    this->print("--", result);
    return result;
}

double CMultivariatePrior::offsetMargin(void) const
{
    return 0.2;
}

double CMultivariatePrior::numberSamples(void) const
{
    return m_NumberSamples;
}

void CMultivariatePrior::numberSamples(double numberSamples)
{
    m_NumberSamples = numberSamples;
}

bool CMultivariatePrior::participatesInModelSelection(void) const
{
    return true;
}

double CMultivariatePrior::unmarginalizedParameters(void) const
{
    return 0.0;
}

double CMultivariatePrior::scaledDecayRate(void) const
{
    return ::pow(0.5, static_cast<double>(this->dimension())) * this->decayRate();
}

void CMultivariatePrior::addSamples(double n)
{
    m_NumberSamples += n;
}

bool CMultivariatePrior::check(const TDouble10Vec1Vec &samples,
                               const TDouble10Vec4Vec1Vec &weights) const
{
    if (samples.size() != weights.size())
    {
        LOG_ERROR("Mismatch in samples '" << core::CContainerPrinter::print(samples)
                  << "' and weights '" << core::CContainerPrinter::print(weights) << "'");
        return false;
    }
    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        if (samples[i].size() != this->dimension())
        {
            LOG_ERROR("Invalid sample '" << core::CContainerPrinter::print(samples[i]) << "'");
            return false;
        }
        for (std::size_t j = 0u; j < weights[i].size(); ++j)
        {
            if (weights[i][j].size() != this->dimension())
            {
                LOG_ERROR("Invalid weight '" << core::CContainerPrinter::print(weights[i][j]) << "'");
                return false;
            }
        }
    }
    return true;
}

bool CMultivariatePrior::check(const TSize10Vec &marginalize,
                               const TSizeDoublePr10Vec &condition) const
{
    typedef boost::transform_iterator<CSetTools::CPairGet<0>,
                                      TSizeDoublePr10Vec::const_iterator,
                                      std::size_t> TFirstCItr;

    std::size_t d = this->dimension();

    if (   (marginalize.size() > 0 && marginalize.back() >= d)
        || (condition.size() > 0 && condition.back().first >= d)
        ||  CSetTools::setIntersectSize(marginalize.begin(),
                                        marginalize.end(),
                                        TFirstCItr(condition.begin(), CSetTools::CPairGet<0>()),
                                        TFirstCItr(condition.end(), CSetTools::CPairGet<0>())) != 0)
    {
        LOG_ERROR("Invalid variables for computing univariate distribution: "
                  << "marginalize '" << core::CContainerPrinter::print(marginalize) << "'"
                  << ", condition '" << core::CContainerPrinter::print(condition) << "'");
        return false;
    }
    return true;
}

void CMultivariatePrior::remainingVariables(const TSize10Vec &marginalize,
                                            const TSizeDoublePr10Vec &condition,
                                            TSize10Vec &result) const
{
    std::size_t d = this->dimension();
    result.reserve(d - marginalize.size() - condition.size());
    for (std::size_t i = 0u, j = 0u, k = 0u; k < d; ++k)
    {
        if (i < marginalize.size() && k == marginalize[i])
        {
            ++i;
            continue;
        }
        if (j < condition.size() && k == condition[j].first)
        {
            ++j;
            continue;
        }
        result.push_back(k);
    }
}

void CMultivariatePrior::unpack(const TSizeDoublePr10Vec &condition,
                                TSize10Vec &condition_,
                                Eigen::VectorXd &x) const
{
    for (std::size_t i = 0u; i < condition.size(); ++i)
    {
        condition_.push_back(condition[i].first);
        x(i) = condition[i].second;
    }
}

double CMultivariatePrior::smallest(const TDouble10Vec &x) const
{
    return *std::min_element(x.begin(), x.end());
}

const double CMultivariatePrior::FALLBACK_DECAY_RATE = 0.001;
const std::string CMultivariatePrior::LOG_NORMAL_TAG("a");
const std::string CMultivariatePrior::MULTIMODAL_TAG("b");
const std::string CMultivariatePrior::NORMAL_TAG("c");
const std::string CMultivariatePrior::ONE_OF_N_TAG("d");
const std::string CMultivariatePrior::CONSTANT_TAG("e");

}
}
