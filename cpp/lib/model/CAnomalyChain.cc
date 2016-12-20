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

#include <model/CAnomalyChain.h>

#include <core/CLogger.h>
#include <core/CMemory.h>
#include <core/CPersistUtils.h>

#include <maths/COrderings.h>

namespace prelert
{
namespace model
{

namespace
{
const std::string CHAIN_ATOM_TAG("a");
}

bool CAnomalyChain::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == CHAIN_ATOM_TAG)
        {
            if (core::CPersistUtils::fromString(traverser.value(), m_ChainAtom) == false)
            {
                LOG_ERROR("Invalid chain in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

void CAnomalyChain::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(CHAIN_ATOM_TAG, core::CPersistUtils::toString(m_ChainAtom));
}

CAnomalyChain::TSizeVec
CAnomalyChain::atom(const CAnomalyScore::CComputer &compute,
                    TDoubleVec probabilities,
                    TSizeVec pids)
{
    if (!maths::COrderings::simultaneousSort(probabilities, pids))
    {
        LOG_ERROR("Inconsistent person identifiers and probabilities: "
                  << " # people = " << pids.size()
                  << ", # probabilities = " << probabilities.size());
        return TSizeVec();
    }

    double score;
    double probability;
    if (!compute(probabilities, score, probability))
    {
        LOG_ERROR("Failed to compute joint anomaly score for "
                  << core::CContainerPrinter::print(probabilities));
        return TSizeVec();
    }
    if (score == 0.0)
    {
        return TSizeVec();
    }
    LOG_TRACE("score = " << score);

    TSizeVec result;
    result.push_back(pids[0]);

    // Exponential expansion followed by binary search to find
    // the smallest set of probabilities which dominate the
    // anomaly. This can use a binary search because the score
    // of the forced probabilities is monotonic decreasing with
    // the number of forced probabilities.

    double maxAtomScore = (1.0 - ATOM_SCORE_FRACTION) * score;

    TDoubleVec forcedProbabilities(probabilities);
    forcedProbabilities[0] = 1.0;

    double atomScore;
    double atomProbability;
    if (!compute(forcedProbabilities, atomScore, atomProbability))
    {
        LOG_ERROR("Failed to compute joint anomaly score for "
                  << core::CContainerPrinter::print(forcedProbabilities));
        return TSizeVec();
    }
    LOG_TRACE("atomScore = " << atomScore << ", threshold = " << maxAtomScore);
    if (atomScore < maxAtomScore)
    {
        return result;
    }

    std::size_t atom = 1;
    do
    {
        atom = std::min(2 * atom, forcedProbabilities.size());
        for (std::size_t i = atom / 2; i < atom; ++i)
        {
            forcedProbabilities[i] = 1.0;
        }
        if (!compute(forcedProbabilities, atomScore, atomProbability))
        {
            LOG_ERROR("Failed to compute joint anomaly score for "
                      << core::CContainerPrinter::print(forcedProbabilities));
            return TSizeVec();
        }
        LOG_TRACE("atomScore = " << atomScore << ", threshold = " << maxAtomScore);
        if (atomScore < maxAtomScore)
        {
            break;
        }
    }
    while (atom < probabilities.size());
    LOG_TRACE("atom = " << atom);

    // We know that [atom / 2, atom] brackets the true atom.

    for (std::size_t lower = atom / 2; atom - lower > 1; /**/)
    {
        std::size_t mid = (lower + atom) / 2;
        LOG_TRACE("mid = " << mid);

        if (atomScore < maxAtomScore)
        {
            for (std::size_t i = mid; i < atom; ++i)
            {
                LOG_TRACE("setting " << i << " to " << probabilities[i]);
                forcedProbabilities[i] = probabilities[i];
            }
        }
        else
        {
            for (std::size_t i = lower; i <= mid; ++i)
            {
                forcedProbabilities[i] = 1.0;
            }
        }

        if (!compute(forcedProbabilities, atomScore, atomProbability))
        {
            LOG_ERROR("Failed to compute joint anomaly score for "
                      << core::CContainerPrinter::print(probabilities));
            return TSizeVec();
        }

        LOG_TRACE("atomScore = " << atomScore << ", threshold = " << maxAtomScore);
        (atomScore < maxAtomScore ? atom : lower) = mid;
    }

    result.reserve(atom);
    for (std::size_t i = 1u; i < atom; ++i)
    {
        result.push_back(pids[i]);
    }

    LOG_TRACE("atom = " << core::CContainerPrinter::print(result));

    return result;
}

bool CAnomalyChain::match(TSizeVec atom)
{
    std::sort(atom.begin(), atom.end());

    bool newChain = m_ChainAtom.empty();

    TSizeVec setIntersection;
    if (!newChain)
    {
        setIntersection.reserve(std::max(atom.size(), m_ChainAtom.size()));
        std::set_intersection(atom.begin(), atom.end(),
                              m_ChainAtom.begin(), m_ChainAtom.end(),
                              std::back_inserter(setIntersection));

        TSizeVec setUnion;
        setUnion.reserve(atom.size() + m_ChainAtom.size());
        std::set_union(atom.begin(), atom.end(),
                       m_ChainAtom.begin(), m_ChainAtom.end(),
                       std::back_inserter(setUnion));
        m_ChainAtom.swap(setUnion);
    }
    else
    {
        m_ChainAtom.swap(atom);
    }

    LOG_TRACE(setIntersection.size() << "/" << m_ChainAtom.size());

    bool result =    !setIntersection.empty()
                  && (   static_cast<double>(setIntersection.size())
                      >= ATOM_MATCH_NORMALIZED_INTERSECTION
                         * static_cast<double>(m_ChainAtom.size()));

    if (!newChain && !result)
    {
        m_ChainAtom.clear();
    }

    return result;
}

void CAnomalyChain::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CMetricModel");
    core::CMemoryDebug::dynamicSize("m_ChainAtom", m_ChainAtom, mem);
}

std::size_t CAnomalyChain::memoryUsage(void) const
{
    return core::CMemory::dynamicSize(m_ChainAtom);
}

const double CAnomalyChain::ATOM_SCORE_FRACTION(0.95);
const double CAnomalyChain::ATOM_MATCH_NORMALIZED_INTERSECTION(0.5);

}
}
