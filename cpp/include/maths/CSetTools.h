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

#ifndef INCLUDED_prelert_maths_CSetTools_h
#define INCLUDED_prelert_maths_CSetTools_h

#include <maths/ImportExport.h>

#include <boost/variant.hpp>

#include <cstddef>
#include <set>
#include <vector>

namespace prelert
{
namespace maths
{

namespace set_tools_detail
{

template<typename T, std::size_t INDEX>
struct SPlace
{};
template<typename U, typename V>
struct SPlace<std::pair<U, V>, 0>
{
    typedef U Type;
    static inline const U &get(const std::pair<U, V> &p)
    {
        return p.first;
    }
    static inline U &get(std::pair<U, V> &p)
    {
        return p.first;
    }
};
template<typename U, typename V>
struct SPlace<std::pair<U, V>, 1>
{
    typedef V Type;
    static inline const V &get(const std::pair<U, V> &p)
    {
        return p.second;
    }
    static inline V &get(std::pair<U, V> &p)
    {
        return p.second;
    }
};

}

//! \brief Collection of set utility functions not provided by the STL.
//!
//! DESCRIPTION:\n
//! This implements various set related functionality which can't be
//! implemented efficiently using the STL. For example, a function for
//! computing set difference in-place, and functions for counting
//! elements in set differences and unions. Common measures of set
//! similarity such as the Jaccard index are also implemented.
class MATHS_EXPORT CSetTools
{
    public:
        //! \brief Gets the specified element of a pair.
        template<std::size_t INDEX>
        class CPairGet
        {
            public:
                template<typename U, typename V>
                const typename set_tools_detail::SPlace<std::pair<U, V>, INDEX>::Type &
                    operator()(const std::pair<U, V> &p) const
                {
                    return set_tools_detail::SPlace<std::pair<U, V>, INDEX>::get(p);
                }

                template<typename U, typename V>
                typename set_tools_detail::SPlace<std::pair<U, V>, INDEX>::Type &
                    operator()(std::pair<U, V> &p) const
                {
                    return set_tools_detail::SPlace<std::pair<U, V>, INDEX>::get(p);
                }
        };

        //! \brief Checks if an indexed object is in a specified collection
        //! of indices.
        class CIndexInSet
        {
            public:
                typedef std::set<std::size_t> TSizeSet;

            public:
                CIndexInSet(std::size_t index) :
                        m_IndexSet(index)
                {
                }

                CIndexInSet(const TSizeSet &indexSet) :
                        m_IndexSet(indexSet)
                {
                }

                template<typename T>
                bool operator()(const T &indexedObject) const
                {
                    const std::size_t *index = boost::get<std::size_t>(&m_IndexSet);
                    if (index)
                    {
                        return indexedObject.s_Index == *index;
                    }
                    const TSizeSet &indexSet = boost::get<TSizeSet>(m_IndexSet);
                    return indexSet.count(indexedObject.s_Index) > 0;
                }

            private:
                typedef boost::variant<std::size_t, TSizeSet> TSizeOrSizeSet;

            private:
                TSizeOrSizeSet m_IndexSet;
        };

        //! Compute the difference between \p S and [\p begin, \p end).
        template<typename T, typename ITR>
        static void inplace_set_difference(std::vector<T> &S,
                                           ITR begin,
                                           ITR end)
        {
            typename std::vector<T>::iterator i = S.begin(), last = i;
            for (ITR j = begin; i != S.end() && j != end; /**/)
            {
                if (*i < *j)
                {
                    if (last != i)
                    {
                        std::iter_swap(last, i);
                    }
                    ++i; ++last;
                }
                else if (*j < *i)
                {
                    ++j;
                }
                else
                {
                    ++i; ++j;
                }
            }
            if (last != i)
            {
                S.erase(std::swap_ranges(i, S.end(), last), S.end());
            }
        }

        //! Compute the number of elements in the intersection of the
        //! ranges [\p beginLhs, \p endLhs) and [\p beginRhs, \p endRhs).
        template<typename ITR1, typename ITR2>
        static std::size_t setIntersectSize(ITR1 beginLhs,
                                            ITR1 endLhs,
                                            ITR2 beginRhs,
                                            ITR2 endRhs)
        {
            std::size_t result = 0u;
            while (beginLhs != endLhs && beginRhs != endRhs)
            {
                if (*beginLhs < *beginRhs)
                {
                    ++beginLhs;
                }
                else if (*beginRhs < *beginLhs)
                {
                    ++beginRhs;
                }
                else
                {
                    ++beginLhs; ++beginRhs; ++result;
                }
            }
            return result;
        }

        //! Compute the number of elements in the union of the ranges
        //! [\p beginLhs, \p endLhs) and [\p beginRhs, \p endRhs).
        template<typename ITR1, typename ITR2>
        static std::size_t setUnionSize(ITR1 beginLhs,
                                        ITR1 endLhs,
                                        ITR2 beginRhs,
                                        ITR2 endRhs)
        {
            std::size_t result = 0u;
            while (beginLhs != endLhs && beginRhs != endRhs)
            {
                if (*beginLhs < *beginRhs)
                {
                    ++beginLhs;
                }
                else if (*beginRhs < *beginLhs)
                {
                    ++beginRhs;
                }
                else
                {
                    ++beginLhs; ++beginRhs;
                }
                ++result;
            }
            return   result
                   + std::distance(beginLhs, endLhs)
                   + std::distance(beginRhs, endRhs);
        }

        //! Compute the Jaccard index of the elements of the ranges
        //! [\p beginLhs, \p endLhs) and [\p beginRhs, \p endRhs).
        //!
        //! This is defined as \f$\frac{|A\cap B|}{|A\cup B|}\f$.
        template<typename ITR1, typename ITR2>
        static double jaccard(ITR1 beginLhs,
                              ITR1 endLhs,
                              ITR2 beginRhs,
                              ITR2 endRhs)
        {
            std::size_t numer = 0u;
            std::size_t denom = 0u;
            while (beginLhs != endLhs && beginRhs != endRhs)
            {
                if (*beginLhs < *beginRhs)
                {
                    ++beginLhs;
                }
                else if (*beginRhs < *beginLhs)
                {
                    ++beginRhs;
                }
                else
                {
                    ++beginLhs; ++beginRhs; ++numer;
                }
                ++denom;
            }
            denom +=  std::distance(beginLhs, endLhs)
                    + std::distance(beginRhs, endRhs);
            return denom == 0 ? 0.0 :  static_cast<double>(numer)
                                     / static_cast<double>(denom);
        }

        //! Compute the overlap coefficient (or, Szymkiewicz-Simpson
        //! coefficient) of the elements of the ranges
        //! [\p beginLhs, \p endLhs) and [\p beginRhs, \p endRhs).
        //!
        //! This is defined as \f$\frac{|A\cap B|}{\min(|A|,|B|)}\f$.
        template<typename ITR1, typename ITR2>
        static double overlap(ITR1 beginLhs,
                              ITR1 endLhs,
                              ITR2 beginRhs,
                              ITR2 endRhs)
        {
            std::size_t numer = 0u;
            std::size_t nl = 0u;
            std::size_t nr = 0u;
            while (beginLhs != endLhs && beginRhs != endRhs)
            {
                if (*beginLhs < *beginRhs)
                {
                    ++beginLhs; ++nl;
                }
                else if (*beginRhs < *beginLhs)
                {
                    ++beginRhs; ++nr;
                }
                else
                {
                    ++beginLhs; ++beginRhs; ++numer; ++nl; ++nr;
                }
            }
            nl += std::distance(beginLhs, endLhs);
            nr += std::distance(beginRhs, endRhs);
            double denom = static_cast<double>(std::min(nl, nr));
            return denom == 0 ? 0.0 :  static_cast<double>(numer)
                                     / static_cast<double>(denom);
        }
};

}
}

#endif // INCLUDED_prelert_maths_CSetTools_h
