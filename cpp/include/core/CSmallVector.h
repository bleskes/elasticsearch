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

#ifndef INCLUDED_prelert_core_CSmallVector_h
#define INCLUDED_prelert_core_CSmallVector_h

#include <core/CContainerPrinter.h>
#include <core/CMemory.h>
#include <core/CStringUtils.h>

#include <boost/array.hpp>
#include <boost/operators.hpp>
#include <boost/type_traits/remove_const.hpp>
#include <boost/variant.hpp>
#include <boost/variant/get.hpp>

#include <algorithm>
#include <cstddef>
#include <functional>
#include <iterator>
#include <utility>
#include <vector>

namespace prelert
{
namespace core
{

namespace small_vector_detail
{

template<typename MAYBE_CONST_SMALL_VECTOR, typename ITERATOR>
class CIterator
{
    public:
        typedef typename MAYBE_CONST_SMALL_VECTOR::difference_type difference_type;

    public:
        CIterator(void) : m_I(0), m_Vector(0) {}
        CIterator(difference_type i, MAYBE_CONST_SMALL_VECTOR &vector) :
            m_I(i), m_Vector(&vector)
        {}

        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        const CIterator &operator=(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                                   OTHER_ITERATOR> &rhs)
        {
            m_I = rhs.index();
            m_Vector = &rhs.vector();
            return *this;
        }

        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator==(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                        OTHER_ITERATOR> &rhs) const
        {
            return m_I == rhs.index() && m_Vector == &rhs.vector();
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator!=(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                        OTHER_ITERATOR> &rhs) const
        {
            return m_I != rhs.index() || m_Vector != &rhs.vector();
        }
        const ITERATOR &operator++(void)
        {
            ++m_I;
            return static_cast<const ITERATOR&>(*this);
        }
        ITERATOR operator++(int)
        {
            ITERATOR result(*this);
            ++m_I;
            return result;
        }
        const ITERATOR &operator--(void)
        {
            --m_I;
            return static_cast<const ITERATOR&>(*this);
        }
        ITERATOR operator--(int)
        {
            CIterator result(*this);
            --m_I;
            return result;
        }
        const ITERATOR &operator+=(difference_type n)
        {
            m_I += n;
            return static_cast<const ITERATOR&>(*this);
        }
        const ITERATOR &operator-=(difference_type n)
        {
            m_I -= n;
            return static_cast<const ITERATOR&>(*this);
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        difference_type operator-(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                                  OTHER_ITERATOR> &rhs) const
        {
            return m_I - rhs.index();
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator<(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                       OTHER_ITERATOR> &rhs) const
        {
            return m_I < rhs.index();
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator<=(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                        OTHER_ITERATOR> &rhs) const
        {
            return m_I <= rhs.index();
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator>(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                       OTHER_ITERATOR> &rhs) const
        {
            return m_I > rhs.index();
        }
        template<typename OTHER_MAYBE_CONST_SMALL_VECTOR, typename OTHER_ITERATOR>
        bool operator>=(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                        OTHER_ITERATOR> &rhs) const
        {
            return m_I >= rhs.index();
        }

        inline MAYBE_CONST_SMALL_VECTOR &vector(void) const { return *m_Vector; }
        inline difference_type index(void) const { return m_I; }

    private:
        difference_type m_I;
        MAYBE_CONST_SMALL_VECTOR *m_Vector;
};

}

//! \brief (Mostly) standards compliant vector with stack storage
//! for specified number of elements.
//!
//! DESCRIPTION:\n
//! This implements most of C++98 standard vector. Full allocator
//! support has not been provided because elements are not always
//! allocated. Also, I haven't bothered to provide support for
//! allocators which aren't default constructible since I don't
//! envision needing this. The number of elements such that this
//! vector uses stack storage is specified by a template parameter.
//! If fewer than this are ever stored in the vector then it uses
//! an boost::array to store the elements; otherwise, it switches
//! to using a std::vector.
//!
//! IMPLEMENTATION:\n
//! Uses boost variant for the storage so that no space is wasted.
//! The switching strategy ensures that the data memory is always
//! in a contiguous block, which is needed for standards compliance.
//!
//! \tparam T The element type.
//! \tparam N The maximum number of elements which are stored on
//! the stack.
//! \tparam ALLOC The allocator used by the vector implementation.
template<typename T, std::size_t N, typename ALLOC = std::allocator<T> >
class CSmallVector
{
    private:
        typedef boost::array<T, N> TArray;
        typedef std::vector<T, ALLOC> TVec;
        typedef boost::variant<TArray, TVec> TStorage;

        template<typename MAYBE_CONST_SMALL_VECTOR,
                 typename REFERENCE_TYPE,
                 typename POINTER_TYPE>
        class CIterator : public std::iterator<std::random_access_iterator_tag,
                                               typename MAYBE_CONST_SMALL_VECTOR::value_type,
                                               typename MAYBE_CONST_SMALL_VECTOR::difference_type,
                                               POINTER_TYPE,
                                               REFERENCE_TYPE>,
                          public small_vector_detail::CIterator<MAYBE_CONST_SMALL_VECTOR,
                                                                CIterator<MAYBE_CONST_SMALL_VECTOR,
                                                                          REFERENCE_TYPE,
                                                                          POINTER_TYPE> >,
                          private boost::addable2< CIterator<MAYBE_CONST_SMALL_VECTOR, REFERENCE_TYPE, POINTER_TYPE>,
                                                   typename MAYBE_CONST_SMALL_VECTOR::difference_type,
                                  boost::subtractable2< CIterator<MAYBE_CONST_SMALL_VECTOR, REFERENCE_TYPE, POINTER_TYPE>,
                                                        typename MAYBE_CONST_SMALL_VECTOR::difference_type > >
        {
            private:
                typedef small_vector_detail::CIterator<MAYBE_CONST_SMALL_VECTOR, CIterator> TBase;

            public:
                typedef typename MAYBE_CONST_SMALL_VECTOR::difference_type difference_type;

            public:
                CIterator(void) {}
                CIterator(difference_type i, MAYBE_CONST_SMALL_VECTOR &vector) :
                        TBase(i, vector)
                {}
                template<typename OTHER_MAYBE_CONST_SMALL_VECTOR,
                         typename OTHER_REFERENCE_TYPE,
                         typename OTHER_POINTER_TYPE>
                CIterator(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                          OTHER_REFERENCE_TYPE,
                                          OTHER_POINTER_TYPE> &other) :
                        TBase(other.index(), other.vector())
                {}
                CIterator(const TBase &base) : TBase(base) {}

                REFERENCE_TYPE operator*(void) const
                {
                    return this->vector()[static_cast<size_type>(this->index())];
                }
                POINTER_TYPE operator->(void) const
                {
                    return &(this->vector()[static_cast<size_type>(this->index())]);
                }
                REFERENCE_TYPE operator[](difference_type i) const
                {
                    return this->vector()[static_cast<size_type>(this->index() + i)];
                }
        };

    public:
        typedef typename std::vector<T, ALLOC>::value_type value_type;
        typedef typename std::vector<T, ALLOC>::allocator_type allocator_type;
        typedef typename std::vector<T, ALLOC>::reference reference;
        typedef typename std::vector<T, ALLOC>::const_reference const_reference;
        typedef typename std::vector<T, ALLOC>::pointer pointer;
        typedef typename std::vector<T, ALLOC>::const_pointer const_pointer;
        typedef typename std::vector<T, ALLOC>::difference_type difference_type;
        typedef typename std::vector<T, ALLOC>::size_type size_type;
        typedef CIterator<CSmallVector, reference, pointer> iterator;
        typedef CIterator<const CSmallVector, const_reference, const_pointer> const_iterator;
        typedef std::reverse_iterator<iterator> reverse_iterator;
        typedef std::reverse_iterator<const_iterator> const_reverse_iterator;

    public:
        //! \name Constructors
        //@{
        CSmallVector(void) : m_Size(0), m_Storage(boost::array<T, N>()) {}
        explicit CSmallVector(size_type n, const value_type &val = value_type()) : m_Size(0)
        {
            this->initializeStorage(n);
            this->resize(n, val);
        }
        template<class ITR>
        CSmallVector(ITR first, ITR last) : m_Size(0)
        {
            this->initializeStorage(std::distance(first, last));
            this->assign(first, last);
        }
        template<typename U, std::size_t M>
        CSmallVector(const CSmallVector<U, M> &other) : m_Size(0)
        {
            this->initializeStorage(other.size());
            this->assign(other.begin(), other.end());
        }
        template<typename U>
        CSmallVector(const std::vector<U> &other) : m_Size(0)
        {
            this->initializeStorage(other.size());
            this->assign(other.begin(), other.end());
        }

    private:
        //! Initialize the storage for \p n elements.
        void initializeStorage(size_type n)
        {
            if (n <= N)
            {
                m_Storage = boost::array<T, N>();
            }
            else
            {
                m_Storage = TVec();
            }
        }
        //@}

    public:
        //! Stop gap implicit conversion to std::vector until I have
        //! rolled it out fully.
        inline operator std::vector<T> (void) const
        {
            return std::vector<T>(this->begin(), this->end());
        }

        //! \name Iterators
        //@{
        iterator begin(void)
        {
            return iterator(0, *this);
        }
        const_iterator begin(void) const
        {
            return const_iterator(0, *this);
        }
        const_iterator cbegin(void) const
        {
            return const_iterator(0, *this);
        }
        iterator end(void)
        {
            return iterator(static_cast<difference_type>(m_Size), *this);
        }
        const_iterator end(void) const
        {
            return const_iterator(static_cast<difference_type>(m_Size), *this);
        }
        const_iterator cend(void) const
        {
            return const_iterator(static_cast<difference_type>(m_Size), *this);
        }
        reverse_iterator rbegin(void)
        {
            return reverse_iterator(this->end());
        }
        const_reverse_iterator rbegin(void) const
        {
            return const_reverse_iterator(this->end());
        }
        const_reverse_iterator crbegin(void) const
        {
            return const_reverse_iterator(this->end());
        }
        reverse_iterator rend(void)
        {
            return reverse_iterator(this->begin());
        }
        const_reverse_iterator rend(void) const
        {
            return const_reverse_iterator(this->begin());
        }
        const_reverse_iterator crend(void) const
        {
            return const_reverse_iterator(this->begin());
        }
        //@}

        //! \name Capacity
        //@{
        inline size_type size(void) const
        {
            return m_Size;
        }
        void resize(size_type n)
        {
            this->reserve(n);
            const value_type &val = value_type();
            boost::apply_visitor(CResize(m_Size, n, val), m_Storage);
            m_Size = n;
        }
        void resize(size_type n, const value_type &val)
        {
            this->reserve(n);
            boost::apply_visitor(CResize(m_Size, n, val), m_Storage);
            m_Size = n;
        }
        size_type capacity(void) const
        {
            return boost::apply_visitor(CCapacity(), m_Storage);
        }
        bool empty(void) const
        {
            return m_Size == 0;
        }
        void reserve(size_type n)
        {
            if (n > N)
            {
                TArray *array = boost::get<TArray>(&m_Storage);
                if (array)
                {
                    TVec values;
                    values.reserve(n);
                    values.assign(array->begin(), array->begin() + m_Size);
                    m_Storage = TVec();
                    TVec &vector = boost::get<TVec>(m_Storage);
                    vector.swap(values);
                }
                else
                {
                    TVec &vector = boost::get<TVec>(m_Storage);
                    vector.reserve(n);
                }
            }
        }
        //@}

        //! \name Element Access
        //@{
        reference operator[](size_type i)
        {
            return boost::apply_visitor(CElement(i), m_Storage);
        }
        const_reference operator[](size_type i) const
        {
            return boost::apply_visitor(CConstElement(i), m_Storage);
        }
        reference at(size_type i)
        {
            if (i >= m_Size)
            {
                throw std::out_of_range(CStringUtils::typeToString(i)
                                        + " out of range "
                                        + CStringUtils::typeToString(m_Size));
            }
            return boost::apply_visitor(CElement(i), m_Storage);
        }
        const_reference at(size_type i) const
        {
            if (i >= m_Size)
            {
                throw std::out_of_range(CStringUtils::typeToString(i)
                                        + " out of range "
                                        + CStringUtils::typeToString(m_Size));
            }
            return boost::apply_visitor(CConstElement(i), m_Storage);
        }
        reference front(void)
        {
            return boost::apply_visitor(CElement(0), m_Storage);
        }
        const_reference front(void) const
        {
            return boost::apply_visitor(CConstElement(0), m_Storage);
        }
        reference back(void)
        {
            return boost::apply_visitor(CElement(m_Size - 1), m_Storage);
        }
        const_reference back(void) const
        {
            return boost::apply_visitor(CConstElement(m_Size - 1), m_Storage);
        }
        //@}

        //! \name Modifiers
        //@{
        template<typename ITR>
        void assign(ITR first, ITR last)
        {
            size_type n = std::distance(first, last);
            this->resize(n);
            m_Size = n;
            boost::apply_visitor(CAssign<ITR>(first, last, n), m_Storage);
        }
        void assign(size_type n, const value_type &val)
        {
            this->resize(n);
            m_Size = n;
            boost::apply_visitor(CFill(val, n), m_Storage);
        }
        void push_back(const value_type &val)
        {
            if (m_Size == N)
            {
                this->reserve(N + 1);
            }
            boost::apply_visitor(CInsert(this->end(), m_Size, 1, val), m_Storage);
            ++m_Size;
        }
        void pop_back(void)
        {
            boost::apply_visitor(CErase(this->end() - 1, this->end(), m_Size), m_Storage);
            --m_Size;
        }
        iterator insert(iterator pos, const value_type &val)
        {
            if (m_Size == N)
            {
                this->reserve(N + 1);
            }
            boost::apply_visitor(CInsert(pos, m_Size, 1, val), m_Storage);
            ++m_Size;
            return pos;
        }
        iterator insert(iterator pos, size_type n, const value_type &val)
        {
            if (m_Size + n > N)
            {
                this->reserve(m_Size + n);
            }
            boost::apply_visitor(CInsert(pos, m_Size, n, val), m_Storage);
            m_Size += n;
            return pos;
        }
        template<typename ITR>
        iterator insert(iterator pos, ITR first, ITR last)
        {
            size_type n = std::distance(first, last);
            if (m_Size + n > N)
            {
                this->reserve(m_Size + n);
            }
            boost::apply_visitor(CInsertRange<ITR>(pos, first, last, m_Size, n), m_Storage);
            m_Size += n;
            return pos;
        }
        iterator erase(iterator pos)
        {
            boost::apply_visitor(CErase(pos, pos + 1, m_Size), m_Storage);
            --m_Size;
            return pos;
        }
        iterator erase(iterator first, iterator last)
        {
            boost::apply_visitor(CErase(first, last, m_Size), m_Storage);
            m_Size -= last - first;
            return first;
        }
        template<typename A>
        void swap(CSmallVector<T, N, A> &other)
        {
            // Solaris can't build variant swap.
            if (this != &other)
            {
                std::swap(m_Size, other.m_Size);
                TArray *thisArray  = boost::get<TArray>(&m_Storage);
                TArray *otherArray = boost::get<TArray>(&other.m_Storage);
                if (thisArray && otherArray)
                {
                    std::swap(*thisArray, *otherArray);
                }
                else if (thisArray)
                {
                    TVec &otherVec = boost::get<TVec>(other.m_Storage);
                    TVec tmp;
                    tmp.swap(otherVec);
                    other.m_Storage = *thisArray;
                    m_Storage = TVec();
                    boost::get<TVec>(m_Storage).swap(tmp);
                }
                else if (otherArray)
                {
                    TVec thisVec = boost::get<TVec>(m_Storage);
                    TVec tmp;
                    tmp.swap(thisVec);
                    m_Storage = *otherArray;
                    other.m_Storage = TVec();
                    boost::get<TVec>(other.m_Storage).swap(tmp);
                }
                else
                {
                    boost::get<TVec>(m_Storage).swap(boost::get<TVec>(other.m_Storage));
                }
            }
        }
        void clear(void)
        {
            boost::apply_visitor(CClear(), m_Storage);
            m_Size = 0u;
        }
        //@}

        //! \name Non-Standard
        //@{
        std::size_t memoryUsage(void) const
        {
            return boost::apply_visitor(CMemoryUsage(), m_Storage);
        }
        template<std::size_t M>
        const CSmallVector &operator+=(const CSmallVector<T, M, ALLOC> &other)
        {
            if (m_Size == other.size())
            {
                for (size_type i = 0u; i < m_Size; ++i)
                {
                    (*this)[i] += other[i];
                }
            }
            else
            {
                LOG_ERROR("Cannot add vectors of different size: " << other.size()
                          << " != " << m_Size);
            }
            return *this;
        }
        template<std::size_t M>
        const CSmallVector &operator-=(const CSmallVector<T, M, ALLOC> &other)
        {
            if (m_Size == other.size())
            {
                for (size_type i = 0u; i < m_Size; ++i)
                {
                    (*this)[i] -= other[i];
                }
            }
            else
            {
                LOG_ERROR("Cannot subtract vectors of different size: " << other.size()
                          << " != " << m_Size);
            }
            return *this;
        }
        bool getBool(size_type i) const
        {
            return boost::apply_visitor(CGetBool(i), m_Storage);
        }
        void setBool(size_type i, bool value)
        {
            boost::apply_visitor(CSetBool(i, value), m_Storage);
        }
        //@}

    private:
        //! \brief Resizes the vector implementation.
        class CResize : public boost::static_visitor<void>
        {
            public:
                CResize(size_type m, size_type n, const T &value) :
                    m_M(m), m_N(n), m_Value(&value)
                {}

                void operator()(TArray &a) const
                {
                    for (size_type i = m_M; i < m_N; ++i)
                    {
                        a[i] = *m_Value;
                    }
                }
                void operator()(TVec &v) const
                {
                    v.resize(m_N, *m_Value);
                }

            private:
                size_type m_M, m_N;
                const T *m_Value;
        };

        //! \brief Gets the capacity of the vector implementation.
        class CCapacity : public boost::static_visitor<size_type>
        {
            public:
                size_type operator()(const TArray &/*a*/) const
                {
                    return N;
                }
                size_type operator()(const TVec &v) const
                {
                    return v.capacity();
                }
        };

        //! \brief Retrieves a constant element from the vector implementation.
        class CConstElement : public boost::static_visitor<const T &>
        {
            public:
                CConstElement(size_type i) : m_I(i) {}

                const T &operator()(const TArray &a) const
                {
                    return a[m_I];
                }
                const T &operator()(const TVec &v) const
                {
                    return v[m_I];
                }

            private:
                size_type m_I;
        };

        //! \brief Retrieves an element from the vector implementation.
        class CElement : public boost::static_visitor<T &>
        {
            public:
                CElement(size_type i) : m_I(i) {}

                T &operator()(TArray &a) const
                {
                    return a[m_I];
                }
                T &operator()(TVec &v) const
                {
                    return v[m_I];
                }

            private:
                size_type m_I;
        };

        //! \brief Gets a value in a boolean small vector.
        class CGetBool : public boost::static_visitor<bool>
        {
            public:
                CGetBool(size_type i) : m_I(i) {}

                bool operator()(const TArray &a) const
                {
                    return a[m_I];
                }
                bool operator()(const TVec &v) const
                {
                    return v[m_I];
                }

            private:
                size_type m_I;
        };

        //! \brief Sets a value in a boolean small vector.
        class CSetBool : public boost::static_visitor<void>
        {
            public:
                CSetBool(size_type i, bool value) : m_I(i), m_Value(value) {}

                void operator()(TArray &a) const
                {
                    a[m_I] = m_Value;
                }
                void operator()(TVec &v) const
                {
                    v[m_I] = m_Value;
                }

            private:
                size_type m_I;
                bool m_Value;
        };

        //! \brief Assigns some values to the vector implementation.
        template<typename ITR>
        class CAssign : public boost::static_visitor<void>
        {
            public:
                CAssign(ITR begin, ITR end, size_type n) :
                    m_First(begin), m_Last(end), m_N(n)
                {}

                void operator()(TArray &a) const
                {
                    size_type i = 0u;
                    for (ITR j = m_First; j != m_Last; ++j)
                    {
                        a[i++] = *j;
                    }
                }
                void operator()(TVec &v) const
                {
                    v.assign(m_First, m_Last);
                }

            private:
                ITR m_First, m_Last;
                size_type m_N;
        };

        //! \brief Fills the vector implementation.
        class CFill : public boost::static_visitor<void>
        {
            public:
                CFill(const T &value, size_type n) : m_Value(&value), m_N(n) {}

                void operator()(TArray &a) const
                {
                    for (size_type i = 0u; i < m_N; ++i)
                    {
                        a[i] = *m_Value;
                    }
                }
                void operator()(TVec &v) const
                {
                    v.assign(m_N, *m_Value);
                }

            private:
                const T *m_Value;
                size_type m_N;
        };

        //! \brief Inserts some values into the vector implementation.
        class CInsert : public boost::static_visitor<void>
        {
            public:
                CInsert(iterator pos, size_type m, size_type n, const T &value) :
                    m_Pos(pos), m_M(m), m_N(n), m_Value(&value)
                {}

                void operator()(TArray &a) const
                {
                    std::copy_backward(a.begin() + m_Pos.index(),
                                       a.begin() + m_M,
                                       a.begin() + m_M + m_N);
                    for (size_type i = m_Pos.index(); i < m_Pos.index() + m_N; ++i)
                    {
                        a[i] = *m_Value;
                    }
                }
                void operator()(TVec &v) const
                {
                    v.insert(v.begin() + m_Pos.index(), m_N, *m_Value);
                }

            private:
                iterator m_Pos;
                size_type m_M, m_N;
                const T *m_Value;
        };

        //! \brief Inserts a range into the vector implementation.
        template<typename ITR>
        class CInsertRange : public boost::static_visitor<void>
        {
            public:
                CInsertRange(iterator pos, ITR first, ITR last, size_type m, size_type n) :
                    m_Pos(pos), m_First(first), m_Last(last), m_M(m), m_N(n)
                {}

                void operator()(TArray &a) const
                {
                    std::copy_backward(a.begin() + m_Pos.index(),
                                       a.begin() + m_M,
                                       a.begin() + m_M + m_N);
                    size_type i = m_Pos.index();
                    for (ITR j = m_First; j != m_Last; ++j)
                    {
                        a[i++] = *j;
                    }
                }
                void operator()(TVec &v) const
                {
                    v.insert(v.begin() + m_Pos.index(), m_First, m_Last);
                }

            private:
                iterator m_Pos;
                ITR m_First, m_Last;
                size_type m_M, m_N;
        };

        //! \brief Erases some elements from the vector implementation.
        class CErase : public boost::static_visitor<void>
        {
            public:
                CErase(const iterator &first, const iterator &last, std::size_t m) :
                    m_First(first), m_Last(last), m_M(m)
                {}

                void operator()(TArray &a) const
                {
                    std::copy(a.begin() + m_Last.index(),
                              a.begin() + m_M,
                              a.begin() + m_First.index());
                }
                void operator()(TVec &v) const
                {
                    v.erase(v.begin() + m_First.index(), v.begin() + m_Last.index());
                }

            private:
                iterator m_First;
                iterator m_Last;
                size_type m_M;
        };

        //! \brief Clears the vector implementation.
        class CClear : public boost::static_visitor<void>
        {
            public:
                void operator()(TArray &/*a*/) const
                {
                }
                void operator()(TVec &v) const
                {
                    v.clear();
                }
        };

        //! \brief Get the dynamic memory usage of the object.
        class CMemoryUsage : public boost::static_visitor<std::size_t>
        {
            public:
                std::size_t operator()(const TArray &a) const
                {
                    return CMemory::dynamicSize(a);
                }
                std::size_t operator()(const TVec &v) const
                {
                    return CMemory::dynamicSize(v);
                }
        };

    private:
        //! The number of elements in the small vector.
        size_type m_Size;
        //! The vector storage.
        TStorage m_Storage;
};

//! \brief A small vector of boolean values.
//!
//! DESCRIPTION:\n
//! I have just opted to implement this as a custom type to cut down on
//! the amount of code needed to implement it as a partial specialisation.
template<std::size_t N, typename ALLOC = std::allocator<bool> >
class CSmallVectorBool : protected CSmallVector<bool, N, ALLOC>
{
    public:
        typedef typename std::vector<bool, ALLOC>::value_type value_type;
        typedef typename std::vector<bool, ALLOC>::allocator_type allocator_type;
        typedef typename std::vector<bool, ALLOC>::difference_type difference_type;
        typedef typename std::vector<bool, ALLOC>::size_type size_type;

    private:
        typedef CSmallVector<bool, N, ALLOC> TBase;

        struct SSmallVectorBoolCRef
        {
            SSmallVectorBoolCRef(const CSmallVectorBool &vector, size_type i) :
                    s_Vector(&vector.base()), s_I(i)
            {}

            operator bool () const { return s_Vector->getBool(s_I); }

            const CSmallVector<bool, N, ALLOC> *s_Vector;
            std::size_t s_I;
        };

        struct SSmallVectorBoolRef
        {
            SSmallVectorBoolRef(CSmallVectorBool &vector, size_type i) :
                    s_Vector(&vector.base()), s_I(i)
            {}

            SSmallVectorBoolRef &operator=(const SSmallVectorBoolRef &value)
            {
                s_Vector->setBool(s_I, bool(value));
                return *this;
            }
            SSmallVectorBoolRef &operator=(const SSmallVectorBoolCRef &value)
            {
                s_Vector->setBool(s_I, bool(value));
                return *this;
            }
            const SSmallVectorBoolRef &operator=(bool value)
            {
                s_Vector->setBool(s_I, value);
                return *this;
            }

            operator bool () const { return s_Vector->getBool(s_I); }

            CSmallVector<bool, N, ALLOC> *s_Vector;
            size_type s_I;
        };

        template<typename MAYBE_CONST_SMALL_VECTOR,
                 typename REFERENCE_TYPE>
        class CIterator : public std::iterator<std::random_access_iterator_tag,
                                               typename MAYBE_CONST_SMALL_VECTOR::value_type,
                                               typename MAYBE_CONST_SMALL_VECTOR::difference_type,
                                               REFERENCE_TYPE>,
                          public small_vector_detail::CIterator<MAYBE_CONST_SMALL_VECTOR,
                                                                CIterator<MAYBE_CONST_SMALL_VECTOR, REFERENCE_TYPE> >,
                          private boost::addable2< CIterator<MAYBE_CONST_SMALL_VECTOR, REFERENCE_TYPE>,
                                                             typename MAYBE_CONST_SMALL_VECTOR::difference_type,
                                  boost::subtractable2< CIterator<MAYBE_CONST_SMALL_VECTOR, REFERENCE_TYPE>,
                                                        typename MAYBE_CONST_SMALL_VECTOR::difference_type > >

        {
            private:
                typedef small_vector_detail::CIterator<MAYBE_CONST_SMALL_VECTOR, CIterator> TBase;

            public:
                typedef typename MAYBE_CONST_SMALL_VECTOR::difference_type difference_type;

            public:
                CIterator(void) {}
                CIterator(difference_type i, MAYBE_CONST_SMALL_VECTOR &vector) :
                        TBase(i, vector)
                {}
                template<typename OTHER_MAYBE_CONST_SMALL_VECTOR,
                         typename OTHER_REFERENCE_TYPE>
                CIterator(const CIterator<OTHER_MAYBE_CONST_SMALL_VECTOR,
                                          OTHER_REFERENCE_TYPE> &other) :
                        TBase(other.index(), other.vector())
                {}
                CIterator(const TBase &base) : TBase(base) {}

                REFERENCE_TYPE operator*(void) const
                {
                    return this->vector()[static_cast<size_type>(this->index())];
                }
                REFERENCE_TYPE operator[](difference_type i) const
                {
                    return this->vector()[static_cast<size_type>(this->index() + i)];
                }
        };

    public:
        typedef SSmallVectorBoolRef reference;
        typedef SSmallVectorBoolCRef const_reference;
        typedef CIterator<CSmallVectorBool, reference> iterator;
        typedef CIterator<const CSmallVectorBool, const_reference> const_iterator;
        typedef std::reverse_iterator<iterator> reverse_iterator;
        typedef std::reverse_iterator<const_iterator> const_reverse_iterator;
        typedef iterator pointer;
        typedef const_iterator const_pointer;

    public:
        //! \name Constructors
        //@{
        CSmallVectorBool(void) {}
        explicit CSmallVectorBool(size_type n, const value_type &val = value_type()) :
                TBase(n, val)
        {}
        template<class ITR>
        CSmallVectorBool(ITR first, ITR last) : TBase(first, last)
        {}
        template<typename U, std::size_t M, typename A>
        CSmallVectorBool(const CSmallVector<U, M, A> &other) : TBase(other)
        {}
        template<typename U>
        CSmallVectorBool(const std::vector<U> &other) : TBase(other)
        {}
        //@}

        //! \name Iterators
        //@{
        iterator begin(void) { return iterator(0, *this); }
        const_iterator begin(void) const { return const_iterator(0, *this); }
        const_iterator cbegin(void) const { return const_iterator(0, *this); }
        iterator end(void) { return iterator(this->TBase::size(), *this); }
        const_iterator end(void) const { return const_iterator(this->TBase::size(), *this); }
        const_iterator cend(void) const { return const_iterator(this->TBase::size(), *this); }
        const_reverse_iterator rbegin(void) const { return const_reverse_iterator(this->end()); }
        const_reverse_iterator crbegin(void) const { return const_reverse_iterator(this->end()); }
        reverse_iterator rend(void) { return reverse_iterator(this->begin()); }
        const_reverse_iterator rend(void) const { return const_reverse_iterator(this->begin()); }
        const_reverse_iterator crend(void) const { return const_reverse_iterator(this->begin()); }
        //@}

        //! \name Capacity
        //@{
        inline size_type size(void) const { return this->TBase::size(); }
        void resize(size_type n) { this->TBase::resize(n); }
        void resize(size_type n, const value_type &val) { this->TBase::resize(n, val); }
        size_type capacity(void) const { return this->TBase::capacity(); }
        bool empty(void) const { return this->TBase::empty(); }
        void reserve(size_type n) { this->TBase::reserve(n); }
        //@}

        //! \name Element Access
        //@{
        reference operator[](size_type i) { return SSmallVectorBoolRef(*this, i); }
        const_reference operator[](size_type i) const
        {
            return SSmallVectorBoolCRef(*this, i);
        }
        reference at(size_type i)
        {
            if (i >= this->TBase::size())
            {
                throw std::out_of_range(CStringUtils::typeToString(i)
                                        + " out of range "
                                        + CStringUtils::typeToString(this->TBase::size()));
            }
            return SSmallVectorBoolRef(*this, i);
        }
        const_reference at(size_type i) const
        {
            if (i >= this->TBase::size())
            {
                throw std::out_of_range(CStringUtils::typeToString(i)
                                        + " out of range "
                                        + CStringUtils::typeToString(this->TBase::size()));
            }
            return SSmallVectorBoolCRef(*this, i);
        }
        reference front(void) { return SSmallVectorBoolRef(*this, 0); }
        const_reference front(void) const { return SSmallVectorBoolCRef(*this, 0); }
        reference back(void) { return SSmallVectorBoolRef(*this, this->TBase::size() - 1); }
        const_reference back(void) const
        {
            return SSmallVectorBoolCRef(*this, this->TBase::size() - 1);
        }
        //@}

        //! \name Modifiers
        //@{
        template<typename ITR>
        void assign(ITR first, ITR last) { this->TBase::assign(first, last); }
        void assign(size_type n, const value_type &val) { this->TBase::assign(n, val); }
        void push_back(const value_type &val) { this->TBase::push_back(val); }
        void pop_back(void) { this->TBase::pop_back(); }
        iterator insert(iterator pos, const value_type &val)
        {
            return this->fromBase(this->TBase::insert(this->base(pos), val));
        }
        iterator insert(iterator pos, size_type n, const value_type &val)
        {
            return this->fromBase(this->TBase::insert(this->base(pos), n, val));
        }
        template<typename ITR>
        iterator insert(iterator pos, ITR first, ITR last)
        {
            return this->fromBase(this->TBase::insert(this->base(pos), first, last));
        }
        iterator erase(iterator pos)
        {
            return this->fromBase(this->TBase::erase(this->base(pos)));
        }
        iterator erase(iterator first, iterator last)
        {
            return this->fromBase(this->TBase::erase(this->base(first), this->base(last)));
        }
        template<typename A>
        void swap(CSmallVectorBool<N, A> &other) { this->TBase::swap(other.base()); }
        void clear(void) { this->TBase::clear(); }
        //@}

        //! Non-Standard
        //@{
        const TBase &base(void) const { return *this; }
        TBase &base(void) { return *this; }
        //@}

    private:
        inline typename TBase::iterator base(const iterator &pos) const
        {
            return typename TBase::iterator(pos.index(), pos.vector().base());
        }
        inline iterator fromBase(const typename TBase::iterator &pos)
        {
            return iterator(pos.index(), *this);
        }
};

template<typename T, std::size_t N, typename ALLOC>
void swap(const CSmallVector<T, N, ALLOC> &lhs,
          const CSmallVector<T, N, ALLOC> &rhs)
{
    lhs.swap(rhs);
}
template<std::size_t N, typename ALLOC>
void swap(const CSmallVectorBool<N, ALLOC> &lhs,
          const CSmallVectorBool<N, ALLOC> &rhs)
{
    lhs.swap(rhs);
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator==(const CSmallVector<T, M, ALLOC> &lhs,
                const CSmallVector<T, N, ALLOC> &rhs)
{
    return    lhs.size() == rhs.size()
           && std::equal(lhs.begin(), lhs.end(), rhs.begin());
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator==(const CSmallVectorBool<M, ALLOC> &lhs,
                const CSmallVectorBool<N, ALLOC> &rhs)
{
    return    lhs.size() == rhs.size()
           && std::equal(lhs.begin(), lhs.end(), rhs.begin());
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator!=(const CSmallVector<T, M, ALLOC> &lhs,
                const CSmallVector<T, N, ALLOC> &rhs)
{
    return !(lhs == rhs);
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator!=(const CSmallVectorBool<M, ALLOC> &lhs,
                const CSmallVectorBool<N, ALLOC> &rhs)
{
    return !(lhs == rhs);
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator<(const CSmallVector<T, M, ALLOC> &lhs,
               const CSmallVector<T, N, ALLOC> &rhs)
{
    return std::lexicographical_compare(lhs.begin(), lhs.end(),
                                        rhs.begin(), rhs.end(),
                                        std::less<T>());
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator<(const CSmallVectorBool<M, ALLOC> &lhs,
               const CSmallVectorBool<N, ALLOC> &rhs)
{
    return std::lexicographical_compare(lhs.begin(), lhs.end(),
                                        rhs.begin(), rhs.end(),
                                        std::less<bool>());
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator<=(const CSmallVector<T, M, ALLOC> &lhs,
                const CSmallVector<T, N, ALLOC> &rhs)
{
    typedef std::pair<typename CSmallVector<T, M, ALLOC>::const_iterator,
                      typename CSmallVector<T, N, ALLOC>::const_iterator> TConstItrConstItrPr;

    if (lhs.size() < rhs.size())
    {
        return true;
    }
    if (rhs.size() < lhs.size())
    {
        return false;
    }
    TConstItrConstItrPr mismatch = std::mismatch(lhs.begin(), lhs.end(), rhs.begin());
    if (mismatch.first == lhs.end())
    {
        return true;
    }
    return *mismatch.first <= *mismatch.second;
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator<=(const CSmallVectorBool<M, ALLOC> &lhs,
                const CSmallVectorBool<N, ALLOC> &rhs)
{
    typedef std::pair<typename CSmallVectorBool<M, ALLOC>::const_iterator,
                      typename CSmallVectorBool<N, ALLOC>::const_iterator> TConstItrConstItrPr;

    if (lhs.size() < rhs.size())
    {
        return true;
    }
    if (rhs.size() < lhs.size())
    {
        return false;
    }
    TConstItrConstItrPr mismatch = std::mismatch(lhs.begin(), lhs.end(), rhs.begin());
    if (mismatch.first == lhs.end())
    {
        return true;
    }
    return *mismatch.first <= *mismatch.second;
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator>(const CSmallVector<T, M, ALLOC> &lhs,
               const CSmallVector<T, N, ALLOC> &rhs)
{
    return !(lhs <= rhs);
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator>(const CSmallVectorBool<M, ALLOC> &lhs,
               const CSmallVectorBool<N, ALLOC> &rhs)
{
    return !(lhs <= rhs);
}

template<typename T, std::size_t M, std::size_t N, typename ALLOC>
bool operator>=(const CSmallVector<T, M, ALLOC> &lhs,
                const CSmallVector<T, N, ALLOC> &rhs)
{
    return !(lhs < rhs);
}
template<std::size_t M, std::size_t N, typename ALLOC>
bool operator>=(const CSmallVectorBool<M, ALLOC> &lhs,
                const CSmallVectorBool<N, ALLOC> &rhs)
{
    return !(lhs < rhs);
}

template<typename T, std::size_t N, typename ALLOC>
std::ostream &operator<<(std::ostream &o, const CSmallVector<T, N, ALLOC> &v)
{
    return o << CContainerPrinter::print(v);
}
template<std::size_t N, typename ALLOC>
std::ostream &operator<<(std::ostream &o, const CSmallVectorBool<N, ALLOC> &v)
{
    return o << CContainerPrinter::print(v);
}

}
}

#endif // INCLUDED_prelert_core_CSmallVector_h
