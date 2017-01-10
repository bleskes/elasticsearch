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
#ifndef INCLUDED_ml_maths_CMathsFuncs_h
#define INCLUDED_ml_maths_CMathsFuncs_h

#include <core/CNonInstantiatable.h>

#include <maths/CLinearAlgebra.h>
#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

#include <functional>
#include <iterator>

namespace ml
{
namespace maths
{

//! \brief
//! Portable maths functions
//!
//! DESCRIPTION:\n
//! Portable maths functions
//!
//! IMPLEMENTATION DECISIONS:\n
//! Uses double - it's best that we DON'T use long double, as its size varies
//! between platforms and compilers, e.g. on SPARC long double is 128 bits
//! but has to be manipulated in software (which is slow), on Intel long
//! double is 80 bits natively, but Visual Studio treats long double as 64
//! bits, i.e. the same as double, whereas in gcc long double maps to the 80
//! bit CPU type.
//!
//! Where maths functions have different names on different platforms,
//! they should be added to this file.
//!
class MATHS_EXPORT CMathsFuncs : private core::CNonInstantiatable
{
    public:
        //! Wrapper around boost::math::isnan() which avoids the need to add
        //! cryptic brackets everywhere to deal with macros.
        static bool isNan(double val);

        //! Wrapper around boost::math::isinf() which avoids the need to add
        //! cryptic brackets everywhere to deal with macros.
        static bool isInf(double val);

        //! Neither infinite nor NaN.
        static bool isFinite(double val);

    private:
        //! Check if any of the components return true for \p f.
        template<typename VECTOR, typename F>
        static bool aComponent(const F &f, const VECTOR &val)
        {
            for (std::size_t i = 0u; i < val.dimension(); ++i)
            {
                if (f(val(i)))
                {
                    return true;
                }
            }
            return false;
        }
        //! Check if all the components return true for \p f.
        template<typename VECTOR, typename F>
        static bool everyComponent(const F &f, const VECTOR &val)
        {
            for (std::size_t i = 0u; i < val.dimension(); ++i)
            {
                if (!f(val(i)))
                {
                    return false;
                }
            }
            return true;
        }

        //! Check if any of the elements return true for \p f.
        template<typename SYMMETRIC_MATRIX, typename F>
        static bool anElement(const F &f, const SYMMETRIC_MATRIX &val)
        {
            for (std::size_t i = 0u; i < val.rows(); ++i)
            {
                for (std::size_t j = i; j < val.columns(); ++j)
                {
                    if (f(val(i, j)))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        //! Check if all the elements return true for \p f.
        template<typename SYMMETRIC_MATRIX, typename F>
        static bool everyElement(const F &f, const SYMMETRIC_MATRIX &val)
        {
            for (std::size_t i = 0u; i < val.rows(); ++i)
            {
                for (std::size_t j = i; j < val.columns(); ++j)
                {
                    if (!f(val(i, j)))
                    {
                        return false;
                    }
                }
            }
            return true;
        }

    public:
        //! Check if any of the components are NaN.
        template<std::size_t N>
        static bool isNan(const CVectorNx1<double, N> &val)
        {
            return aComponent(static_cast<bool (*)(double)>(&isNan), val);
        }
        //! Check if any of the components are NaN.
        static bool isNan(const CVector<double> &val)
        {
            return aComponent(static_cast<bool (*)(double)>(&isNan), val);
        }
        //! Check if any of the elements are NaN.
        template<std::size_t N>
        static bool isNan(const CSymmetricMatrixNxN<double, N> &val)
        {
            return anElement(static_cast<bool (*)(double)>(&isNan), val);
        }
        //! Check if any of the elements are NaN.
        static bool isNan(const CSymmetricMatrix<double> &val)
        {
            return anElement(static_cast<bool (*)(double)>(&isNan), val);
        }
        //! Check if an element is NaN.
        template<std::size_t N>
        static bool isNan(const core::CSmallVector<double, N> &val)
        {
            for (std::size_t i = 0u; i < val.size(); ++i)
            {
                if (isNan(val[i]))
                {
                    return true;
                }
            }
            return false;
        }

        //! Check if any of the components are infinite.
        template<std::size_t N>
        static bool isInf(const CVectorNx1<double, N> &val)
        {
            return aComponent(static_cast<bool (*)(double)>(&isInf), val);
        }
        //! Check if any of the components are infinite.
        static bool isInf(const CVector<double> &val)
        {
            return aComponent(static_cast<bool (*)(double)>(&isInf), val);
        }
        //! Check if any of the elements are infinite.
        template<std::size_t N>
        static bool isInf(const CSymmetricMatrixNxN<double, N> &val)
        {
            return anElement(static_cast<bool (*)(double)>(&isInf), val);
        }
        //! Check if any of the elements are infinite.
        static bool isInf(const CSymmetricMatrix<double> &val)
        {
            return anElement(static_cast<bool (*)(double)>(&isInf), val);
        }
        //! Check if an element is NaN.
        template<std::size_t N>
        static bool isInf(const core::CSmallVector<double, N> &val)
        {
            for (std::size_t i = 0u; i < val.size(); ++i)
            {
                if (isInf(val[i]))
                {
                    return true;
                }
            }
            return false;
        }

        //! Check if all of the components are finite.
        template<std::size_t N>
        static bool isFinite(const CVectorNx1<double, N> &val)
        {
            return everyComponent(static_cast<bool (*)(double)>(&isFinite), val);
        }
        //! Check if all of the components are finite.
        static bool isFinite(const CVector<double> &val)
        {
            return everyComponent(static_cast<bool (*)(double)>(&isFinite), val);
        }
        //! Check if all of the components are NaN.
        template<std::size_t N>
        static bool isFinite(const CSymmetricMatrixNxN<double, N> &val)
        {
            return everyElement(static_cast<bool (*)(double)>(&isFinite), val);
        }
        //! Check if all of the components are NaN.
        static bool isFinite(const CSymmetricMatrix<double> &val)
        {
            return everyElement(static_cast<bool (*)(double)>(&isFinite), val);
        }
        //! Check if an element is NaN.
        template<std::size_t N>
        static bool isFinite(const core::CSmallVector<double, N> &val)
        {
            for (std::size_t i = 0u; i < val.size(); ++i)
            {
                if (!isFinite(val[i]))
                {
                    return false;
                }
            }
            return true;
        }

        //! Check the floating point status of \p value.
        static maths_t::EFloatingPointErrorStatus fpStatus(double val);

        //! Unary function object to check if a value is finite.
        struct SIsFinite : std::unary_function<double, bool>
        {
            bool operator()(double val) const { return isFinite(val); }
        };

        //! \brief Wrapper around an iterator over a collection of doubles,
        //! which must implement the forward iterator concepts, that skips
        //! non-finite values.
        template<typename ITR>
        class CFiniteIterator
        {
            public:
                typedef std::forward_iterator_tag iterator_category;
                typedef typename std::iterator_traits<ITR>::value_type value_type;
                typedef typename std::iterator_traits<ITR>::difference_type difference_type;
                typedef typename std::iterator_traits<ITR>::pointer pointer;
                typedef typename std::iterator_traits<ITR>::reference reference;

            public:
                CFiniteIterator(void) : m_Base(), m_End() {}
                CFiniteIterator(const ITR &base, const ITR &end) :
                        m_Base(base),
                        m_End(end)
                {
                    if (m_Base != m_End && !isFinite(*m_Base))
                    {
                        this->increment();
                    }
                }

                //! Equal.
                bool operator==(const CFiniteIterator &rhs) const { return m_Base == rhs.m_Base; }
                //! Different.
                bool operator!=(const CFiniteIterator &rhs) const { return m_Base != rhs.m_Base; }

                //! Dereference.
                reference operator*(void) const { return *m_Base; }
                //! Pointer.
                pointer operator->(void) const { return m_Base.operator->(); }

                //! Prefix increment.
                const CFiniteIterator &operator++(void)
                {
                    this->increment();
                    return *this;
                }
                //! Post-fix increment.
                CFiniteIterator operator++(int)
                {
                    CFiniteIterator result(*this);
                    this->increment();
                    return result;
                }

            private:
                //! Implements increment.
                void increment(void)
                {
                    while (++m_Base != m_End)
                    {
                        if (isFinite(*m_Base))
                        {
                            break;
                        }
                    }
                }

            private:
                ITR m_Base;
                ITR m_End;
        };

        //! Get an iterator over the finite values of a double container.
        template<typename T>
        static CFiniteIterator<typename T::iterator> beginFinite(T &container)
        {
            return CFiniteIterator<typename T::iterator>(container.begin(), container.end());
        }

        //! Get a const_iterator over the finite values of a double container.
        template<typename T>
        static CFiniteIterator<typename T::const_iterator> beginFinite(const T &container)
        {
            return CFiniteIterator<typename T::const_iterator>(container.begin(), container.end());
        }

        //! Get a finite values iterator at the end of a double container.
        template<typename T>
        static CFiniteIterator<typename T::iterator> endFinite(T &container)
        {
            return CFiniteIterator<typename T::iterator>(container.end(), container.end());
        }

        //! Get a finite values const_iterator at the end of a double container.
        template<typename T>
        static CFiniteIterator<typename T::const_iterator> endFinite(const T &container)
        {
            return CFiniteIterator<typename T::const_iterator>(container.end(), container.end());
        }
};


}
}

#endif // INCLUDED_ml_maths_CMathsFuncs_h

