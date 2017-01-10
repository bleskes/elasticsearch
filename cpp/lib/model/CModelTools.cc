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

#include <model/CModelTools.h>

#include <maths/CBasicStatistics.h>
#include <maths/CMultinomialConjugate.h>
#include <maths/CTools.h>

#include <boost/bind.hpp>

namespace ml
{
namespace model
{

namespace
{

typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1> TMinAccumulator;

//! \brief Visitor to add a probability to variant of possible
//! aggregation styles.
struct SAddProbability : public boost::static_visitor<void>
{
    void operator()(double probability,
                    double weight,
                    maths::CJointProbabilityOfLessLikelySamples &aggregator) const
    {
        aggregator.add(probability, weight);
    }
    void operator()(double probability,
                    double /*weight*/,
                    maths::CProbabilityOfExtremeSample &aggregator) const
    {
        aggregator.add(probability);
    }
};

//! \brief Visitor to read aggregate probability from a variant
//! of possible aggregation styles.
struct SReadProbability : public boost::static_visitor<bool>
{
    template<typename T>
    bool operator()(double weight,
                    double &result,
                    const T &aggregator) const
    {
        double probability;
        if (!aggregator.calculate(probability))
        {
            LOG_ERROR("Failed to compute probability");
            return false;
        }
        result *= weight < 1.0 ? ::pow(probability, std::max(weight, 0.0)) : probability;
        return true;
    }
    template<typename T>
    bool operator()(TMinAccumulator &result, const T &aggregator) const
    {
        double probability;
        if (!aggregator.calculate(probability))
        {
            LOG_ERROR("Failed to compute probability");
            return false;
        }
        result.add(probability);
        return true;
    }
};

}

CModelTools::CProbabilityAggregator::CProbabilityAggregator(EStyle style) :
        m_Style(style),
        m_TotalWeight(0.0),
        m_Aggregators()
{
}

bool CModelTools::CProbabilityAggregator::empty(void) const
{
    return m_TotalWeight == 0.0;
}

void CModelTools::CProbabilityAggregator::add(const TAggregator &aggregator, double weight)
{
    switch (m_Style)
    {
    case E_Sum:
        if (weight > 0.0)
        {
            m_Aggregators.push_back(TAggregatorDoublePr(aggregator, weight));
        }
        break;

    case E_Min:
        m_Aggregators.push_back(TAggregatorDoublePr(aggregator, 1.0));
        break;
    }
}

void CModelTools::CProbabilityAggregator::add(double probability, double weight)
{
    m_TotalWeight += weight;
    for (std::size_t i = 0u; i < m_Aggregators.size(); ++i)
    {
        boost::apply_visitor(boost::bind<void>(SAddProbability(),
                                               probability, weight, _1),
                             m_Aggregators[i].first);
    }
}

bool CModelTools::CProbabilityAggregator::calculate(double &result) const
{
    result = 1.0;

    if (m_TotalWeight == 0.0)
    {
        LOG_TRACE("No samples");
        return true;
    }

    if (m_Aggregators.empty())
    {
        LOG_ERROR("No probability aggregators specified");
        return false;
    }

    double p = 1.0;

    switch (m_Style)
    {
    case E_Sum:
    {
        double n = 0.0;
        for (std::size_t i = 0u; i < m_Aggregators.size(); ++i)
        {
            n += m_Aggregators[i].second;
        }
        for (std::size_t i = 0u; i < m_Aggregators.size(); ++i)
        {
            if (!boost::apply_visitor(boost::bind<bool>(SReadProbability(),
                                                        m_Aggregators[i].second / n,
                                                        boost::ref(p), _1),
                                      m_Aggregators[i].first))
            {
                return false;
            }
        }
        break;
    }
    case E_Min:
    {
        TMinAccumulator p_;
        for (std::size_t i = 0u; i < m_Aggregators.size(); ++i)
        {
            if (!boost::apply_visitor(boost::bind<bool>(SReadProbability(),
                                                        boost::ref(p_), _1),
                                      m_Aggregators[i].first))
            {
                return false;
            }
        }
        if (p_.count() > 0)
        {
            p = p_[0];
        }
        break;
    }
    }

    if (p < 0.0 || p > 1.001)
    {
        LOG_ERROR("Unexpected probability = " << p);
    }
    result = maths::CTools::truncate(p, maths::CTools::smallestProbability(), 1.0);

    return true;
}


CModelTools::CLessLikelyProbability::CLessLikelyProbability(void) : m_Prior(0)
{
}

CModelTools::CLessLikelyProbability::CLessLikelyProbability(const maths::CMultinomialConjugate &prior) : m_Prior(&prior)
{
}

bool CModelTools::CLessLikelyProbability::lookup(std::size_t attribute, double &result) const
{
    result = 1.0;
    if (!m_Prior || m_Prior->isNonInformative())
    {
        return false;
    }

    if (m_Cache.empty())
    {
        TDoubleVec lb;
        TDoubleVec ub;
        m_Prior->probabilitiesOfLessLikelyCategories(maths_t::E_TwoSided, lb, ub);
        LOG_TRACE("P({c}) >= " << core::CContainerPrinter::print(lb));
        LOG_TRACE("P({c}) <= " << core::CContainerPrinter::print(ub));

        m_Cache.swap(lb);
        for (std::size_t i = 0u; i < ub.size(); ++i)
        {
            m_Cache[i] += ub[i];
            m_Cache[i] /= 2.0;
        }
    }

    std::size_t i;
    if (!m_Prior->index(static_cast<double>(attribute), i)
        || i >= m_Cache.size())
    {
        LOG_ERROR("No probability for attribute = " << attribute);
        return false;
    }

    result = m_Cache[i];

    return true;
}

void CModelTools::CLessLikelyProbability::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CTools::CLessLikelyProbability");
    core::CMemoryDebug::dynamicSize("m_Cache", m_Cache, mem->addChild());

    if (m_Prior)
    {
        m_Prior->debugMemoryUsage(mem->addChild());
    }
}

std::size_t CModelTools::CLessLikelyProbability::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Cache);
    if (m_Prior)
    {
        mem += m_Prior->memoryUsage();
    }
    return mem;
}

}
}
