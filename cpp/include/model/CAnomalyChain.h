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

#ifndef INCLUDED_CAnomalyChain_h
#define INCLUDED_CAnomalyChain_h

#include <core/CMemoryUsage.h>

#include <maths/CBasicStatistics.h>

#include <model/CAnomalyScore.h>
#include <model/ImportExport.h>

#include <boost/optional.hpp>

#include <vector>

namespace ml
{
namespace model
{

//! \brief Builds chains of matching compound anomalies.
//!
//! DESCRIPTION:\n
//! The matching to use for anomalies comprising several
//! series is not completely straightforward. A simple
//! condition would be to require that they contain exactly
//! the same series, but this is really too restrictive.
//! For example, if two compound anomalies comprise low
//! probabilities for the collections of series {1, 2, 3, 4}
//! and {2, 3, 4} it would seem reasonable to match them.
//!
//! We ideally want the matching to be an equivalence relation.
//! In particular, the matching would ideally satisfy:
//!   -# \f$M(A_1, A_2)\f$
//!   -# \f$M(A_1, A_2) = M(A_2, A_1)\f$
//!   -# \f$M(A_1, A_2)\f$ and \f$M(A_2, A_3)\f$ => \f$M(A_1, A_3)\f$
//!
//! The standard definition of transitivity is not achievable
//! with a fuzzy matching. Since we care about matching
//! sequences of anomalies we will use a weaker condition
//! of order independence, i.e. our condition satisfies the
//! following property: if \f$M(A_1, A_2,..., A_n)\f$ then
//! for any bijection \f$\sigma : [n] \rightarrow [n]\f$
//! \f$M(A_1, A_2,..., A_n) = M(A_{\sigma(1)}, A_{\sigma(2)},..., A_{\sigma(n)})\f$
//! in place of transitivity.
//!
//! Finally, we want to ensure that the relation is reasonably
//! robust to noise. For example, if one is monitoring a large
//! number of series then typically many of them will be slightly
//! anomalous at any given time. For example, we'd expect 5%
//! to have a probability of less than 0.05 and so on. We do
//! not want the presence of a random collection of moderately
//! anomalous time series to stop us matching two anomalies
//! which are "dominated" by the probabilities of identical
//! sets of series. We can make the definition of dominated
//! precise. In particular, if \f$J : P \rightarrow [0,100]\f$
//! denotes the joint anomaly score of a collection of probabilities
//! \f$P\f$ we define the \f$\alpha\f$ atom as the smallest subset
//! \f$M_{\alpha} \subset P\f$ s.t.
//! <pre class="fragment">
//!   \f$J(P \setminus M_{\alpha}) \le (1 - \alpha) J(P)\f$
//! </pre>
//! Note that \f$alpha\f$ is in the range \f$[0,1]\f$. We compute
//! the atom for a value of \f$alpha\f$ close to one.
class MODEL_EXPORT CAnomalyChain
{
    public:
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<double> TDoubleVec;
        typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;

    public:
        //! Populate the object from part of an state document.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Persist state by passing information to the supplied inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Compute the atom of the anomaly.
        //!
        //! The atom is a subset of the smallest n probabilities
        //! that together comprise the dominant proportion of
        //! the aggregate probability.
        static TSizeVec atom(const CAnomalyScore::CComputer &computer,
                             TDoubleVec probabilities,
                             TSizeVec pids);

        //! The anomaly to test matches if and only if there is
        //! a significant intersection its atom, \p atom, and
        //! the current chain union of atoms.
        bool match(TSizeVec atom);

        //! Debug the memory used by this model.
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this model.
        std::size_t memoryUsage(void) const;

    private:
        //! The minimum fraction of the normalized score which
        //! the atom contributes.
        static const double ATOM_SCORE_FRACTION;
        //! The minimum fractional intersection to match two atoms.
        static const double ATOM_MATCH_NORMALIZED_INTERSECTION;

    private:
        //! The atom of the chain of matched bucket anomalies.
        TSizeVec m_ChainAtom;
};

}
}

#endif // INCLUDED_CAnomalyChain_h
