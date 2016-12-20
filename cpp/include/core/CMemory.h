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
#ifndef INCLUDED_prelert_core_CMemory_h
#define INCLUDED_prelert_core_CMemory_h

#include <core/CLogger.h>
#include <core/CMemoryUsage.h>
#include <core/CNonInstantiatable.h>

#include <boost/any.hpp>
#include <boost/array.hpp>
#include <boost/circular_buffer.hpp>
#include <boost/optional.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/shared_array.hpp>
#include <boost/type_traits/is_pod.hpp>
#include <boost/unordered_map.hpp>
#include <boost/unordered_set.hpp>
#include <boost/utility/enable_if.hpp>
#include <boost/type_traits.hpp>

#include <cstddef>
#include <map>
#include <set>
#include <string>
#include <vector>

namespace prelert
{
namespace core
{

typedef boost::reference_wrapper<const std::type_info> TTypeInfoCRef;

namespace memory_detail
{

template<typename T, std::size_t (T::*)(void) const, typename R = void>
struct enable_if_member_function
{
    typedef R type;
};

template<bool (*)(void), typename R = void>
struct enable_if_function
{
    typedef R type;
};

//! Default template declaration for CMemoryDynamicSize::dispatch
template<typename T, typename ENABLE = void>
struct SMemoryDynamicSize
{
    static std::size_t dispatch(const T &) { return 0; }
};

//! Template specialisation where T has member function "memoryUsage(void)"
template<typename T>
struct SMemoryDynamicSize<T, typename enable_if_member_function<T, &T::memoryUsage>::type>
{
    static std::size_t dispatch(const T &t) { return t.memoryUsage(); }
};

//! Default template for classes that don't sport a staticSize member
template<typename T, typename ENABLE = void>
struct SMemoryStaticSize
{
    static std::size_t dispatch(const T & /*t*/)
    {
        return sizeof(T);
    }
};

//! Template specialisation for classes having a staticSize member: used when
//! base class pointers are passed to dynamicSize()
template<typename T>
struct SMemoryStaticSize<T, typename enable_if_member_function<T, &T::staticSize>::type>
{
    static std::size_t dispatch(const T &t)
    {
        return t.staticSize();
    }
};

//! Base implementation checks for POD.
template<typename T, typename ENABLE = void>
struct SDynamicSizeAlwaysZero
{
    static inline bool value(void)
    {
        return boost::is_pod<T>::value;
    }
};

//! Checks types in pair.
template<typename U, typename V>
struct SDynamicSizeAlwaysZero<std::pair<U, V> >
{
    static inline bool value(void)
    {
        return SDynamicSizeAlwaysZero<U>::value() && SDynamicSizeAlwaysZero<V>::value();
    }
};

//! Specialisation for std::less always true.
template<typename T>
struct SDynamicSizeAlwaysZero<std::less<T> >
{
    static inline bool value(void) { return true; }
};

//! Specialisation for std::greater always true.
template<typename T>
struct SDynamicSizeAlwaysZero<std::greater<T> >
{
    static inline bool value(void) { return true; }
};

//! Checks type in optional.
template<typename T>
struct SDynamicSizeAlwaysZero<boost::optional<T> >
{
    static inline bool value(void) { return SDynamicSizeAlwaysZero<T>::value(); }
};

//! Check for member dynamicSizeAlwaysZero function.
template<typename T>
struct SDynamicSizeAlwaysZero<T, typename enable_if_function<&T::dynamicSizeAlwaysZero>::type>
{
    static inline bool value(void) { return T::dynamicSizeAlwaysZero(); }
};

//! \brief Total ordering of type_info objects.
struct STypeInfoLess
{
    template<typename T>
    bool operator()(const std::pair<TTypeInfoCRef, T> &lhs,
                    const std::pair<TTypeInfoCRef, T> &rhs) const
    {
        return boost::unwrap_ref(lhs.first).before(boost::unwrap_ref(rhs.first));
    }
    template<typename T>
    bool operator()(const std::pair<TTypeInfoCRef, T> &lhs,
                    TTypeInfoCRef rhs) const
    {
        return boost::unwrap_ref(lhs.first).before(boost::unwrap_ref(rhs));
    }
    template<typename T>
    bool operator()(TTypeInfoCRef lhs,
                    const std::pair<TTypeInfoCRef, T> &rhs) const
    {
        return boost::unwrap_ref(lhs).before(boost::unwrap_ref(rhs.first));
    }
};

} // memory_detail::


//! \brief
//! Core memory usage template class
//!
//! DESCRIPTION:\n
//! Core memory usage template class. Provides a method for determining
//! the memory used by different prelert classes and standard containers
//!
//! Prelert classes can declare a public member function:
//!     std::size_t memoryUsage(void) const;
//!
//! which should call CMemory::dynamicSize(t); on all its dynamic members
//!
//! For virtual hierarchies, the compiler can not determine the size
//! of derived classes from the base pointer, so wherever the afore-
//! mentioned memoryUsage() function is virtual, an associated function
//!
//!     std::size_t staticSize(void) const;
//!
//! should be declared, returning sizeof(*this);
//!
//! IMPLEMENTATION DECISIONS:\n
//! Template class to allow the compiler to determine the correct method
//! for arbitrary types
//!
//! Only contains static members, this should not be instantiated
//!
class CORE_EXPORT CMemory : private CNonInstantiatable
{
    private:
        static const std::string EMPTY_STRING;

    public:
        //! Implements a visitor pattern for computing the size of types
        //! stored in boost::any.
        //!
        //! DESCRIPTION:\n
        //! The idea of this class is that the user of dynamicSize should
        //! register call backs to compute the size of objects which are
        //! stored in boost::any and pass this visitor class to the call
        //! to dynamicSize. Provided they have registered all types which
        //! will be visited then this should correctly compute the dynamic
        //! size used by objects stored in boost::any. It will warn if a
        //! type is visited which is not registered. Example usage is as
        //! follows:
        //! \code{cpp}
        //! CAnyVisitor visitor;
        //! typedef std::vector<double> TDoubleVec;
        //! visitor.insertCallback<TDoubleVec>();
        //! typedef std::vector<boost::any> TAnyVec;
        //! TAnyVec variables;
        //! variables.push_back(TDoubleVec(10));
        //! std::size_t size = CMemory::dynamicSize(variables, visitor);
        //! \endcode
        class CORE_EXPORT CAnyVisitor
        {
            public:
                typedef std::size_t (*TDynamicSizeFunc)(const boost::any &any);
                typedef std::pair<TTypeInfoCRef, TDynamicSizeFunc> TTypeInfoDynamicSizeFuncPr;
                typedef std::vector<TTypeInfoDynamicSizeFuncPr> TTypeInfoDynamicSizeFuncPrVec;

                //! Insert a callback to compute the size of the type T
                //! if it is stored in boost::any.
                template<typename T>
                bool registerCallback(void)
                {
                    TTypeInfoDynamicSizeFuncPrVec::iterator itr =
                            std::lower_bound(m_Callbacks.begin(),
                                             m_Callbacks.end(),
                                             boost::cref(typeid(T)),
                                             memory_detail::STypeInfoLess());
                    if (itr == m_Callbacks.end())
                    {
                        m_Callbacks.push_back(TTypeInfoDynamicSizeFuncPr(
                                                  boost::cref(typeid(T)),
                                                  &CAnyVisitor::dynamicSizeCallback<T>));
                        return true;
                    }
                    else if (itr->first.get() != typeid(T))
                    {
                        m_Callbacks.insert(itr, TTypeInfoDynamicSizeFuncPr(
                                                    boost::cref(typeid(T)),
                                                    &CAnyVisitor::dynamicSizeCallback<T>));
                        return true;
                    }
                    return false;
                }

                //! Calculate the dynamic size of x if a callback has been
                //! registered for its type.
                std::size_t dynamicSize(const boost::any &x) const
                {
                    if (x.empty())
                    {
                        return 0;
                    }

                    TTypeInfoDynamicSizeFuncPrVec::const_iterator itr =
                            std::lower_bound(m_Callbacks.begin(),
                                             m_Callbacks.end(),
                                             boost::cref(x.type()),
                                             memory_detail::STypeInfoLess());
                    if (itr != m_Callbacks.end() && itr->first.get() == x.type())
                    {
                        return (*itr->second)(x);
                    }

                    LOG_ERROR("No callback registered for " << x.type().name());
                    return 0;
                }

            private:
                //! Wraps up call to any_cast and dynamicSize.
                template<typename T>
                static std::size_t dynamicSizeCallback(const boost::any &any)
                {
                    try
                    {
                        return sizeof(T) + CMemory::dynamicSize(boost::any_cast<const T &>(any));
                    }
                    catch (const std::exception &e)
                    {
                        LOG_ERROR("Failed to calculate size " << e.what());
                    }
                    return 0;
                }

                TTypeInfoDynamicSizeFuncPrVec m_Callbacks;
        };

    public:
        //! Default template
        template<typename T>
        static std::size_t dynamicSize(const T &t, typename boost::disable_if<typename boost::is_pointer<T> >::type * = 0)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                mem += memory_detail::SMemoryDynamicSize<T>::dispatch(t);
            }
            return mem;
        }

        //! Overload for pointer
        template<typename T>
        static std::size_t dynamicSize(const T &t, typename boost::enable_if<typename boost::is_pointer<T> >::type * = 0)
        {
            if (t == 0)
            {
                return 0;
            }
            return staticSize(*t) + dynamicSize(*t);
        }

        //! Overload for boost::shared_ptr
        template<typename T>
        static std::size_t dynamicSize(const boost::shared_ptr<T> &t)
        {
            if (!t)
            {
                return 0;
            }
            return (staticSize(*t) + dynamicSize(*t)) / t.use_count();
        }

        //! Overload for boost::array
        template<typename T, std::size_t N>
        static std::size_t dynamicSize(const boost::array<T, N> &t)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                for (std::size_t i = 0; i < t.size(); ++i)
                {
                    mem += dynamicSize(t[i]);
                }
            }
            return mem;
        }

        //! Overload for std::vector
        template<typename T>
        static std::size_t dynamicSize(const std::vector<T> &t)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                for (std::size_t i = 0; i < t.size(); ++i)
                {
                    mem += dynamicSize(t[i]);
                }
            }
            return mem + sizeof(T) * t.capacity();
        }

        //! Overload for std::string
        static std::size_t dynamicSize(const std::string &t)
        {
            std::size_t capacity = t.capacity();
            // The different STLs we use on various platforms all have different
            // allocation strategies for strings
            // These are hard-coded here, on the assumption that they will not
            // change frequently - but checked by unittests that do runtime
            // verification
            // See http://linux/wiki/index.php/Technical_design_issues#std::string
#ifdef MacOSX
            // For lengths up to 22 bytes there is no allocation
            if (capacity <= 22)
            {
                return 0;
            }
            return capacity + 1;

#elif (defined(Linux) || defined(SOLARIS)) && (!defined(_GLIBCXX_USE_CXX11_ABI) || _GLIBCXX_USE_CXX11_ABI == 0)
            // All sizes > 0 use the heap, and the string structure is
            // 1 pointer + 2 sizes + 1 null terminator
            // We don't handle the reference counting, so may overestimate
            // Even some 0 length strings may use the heap - see
            // http://info.prelert.com/blog/clearing-strings
            if (capacity == 0 &&
                t.data() == EMPTY_STRING.data())
            {
                return 0;
            }
            return capacity + sizeof(void *) + (2 * sizeof(std::size_t)) + 1;

#else // Linux with C++11 ABI and Windows
            // For lengths up to 15 bytes there is no allocation
            if (capacity <= 15)
            {
                return 0;
            }
            return capacity + 1;
#endif
        }

        //! Overload for boost::unordered_map
        template<typename K, typename V, typename H, typename P, typename A>
        static std::size_t dynamicSize(const boost::unordered_map<K, V, H, P, A> &t)
        {
            std::size_t mem = 0;
            if (!(memory_detail::SDynamicSizeAlwaysZero<K>::value() &&
                  memory_detail::SDynamicSizeAlwaysZero<V>::value()))
            {
                for (typename boost::unordered_map<K, V, H, P, A>::const_iterator i = t.begin();
                        i != t.end(); ++i)
                {
                    mem += dynamicSize(*i);
                }
            }
            return mem + (t.bucket_count() * sizeof(std::size_t) * 2)
                       + (t.size() * (sizeof(K) + sizeof(V) + 2 * sizeof(std::size_t)));
        }

        //! Overload for std::map
        template<typename K, typename V, typename C, typename A>
        static std::size_t dynamicSize(const std::map<K, V, C, A> &t)
        {
            // std::map appears to use 4 pointers/ints per data item
            // (colour, parent, left and right child pointers)
            std::size_t mem = 0;
            if (!(memory_detail::SDynamicSizeAlwaysZero<K>::value() &&
                  memory_detail::SDynamicSizeAlwaysZero<V>::value()))
            {
                for (typename std::map<K, V, C, A>::const_iterator i = t.begin();
                        i != t.end(); ++i)
                {
                    mem += dynamicSize(*i);
                }
            }
            return mem + t.size() * (sizeof(K) + sizeof(V) + 4 * sizeof(std::size_t));
        }

        //! Overload for boost::unordered_set
        template<typename T, typename H, typename P, typename A>
        static std::size_t dynamicSize(const boost::unordered_set<T, H, P, A> &t)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                for (typename boost::unordered_set<T, H, P, A>::const_iterator i = t.begin();
                        i != t.end(); ++i)
                {
                    mem += dynamicSize(*i);
                }
            }
            return mem + (t.bucket_count() * sizeof(std::size_t) * 2)
                       + (t.size() * (sizeof(T) + 2 * sizeof(std::size_t)));
        }

        //! Overload for std::set
        template<typename T, typename C, typename A>
        static std::size_t dynamicSize(const std::set<T, C, A> &t)
        {
            // std::set appears to use 4 pointers/ints per data item
            // (colour, parent, left and right child pointers)
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                for (typename std::set<T, C, A>::const_iterator i = t.begin();
                        i != t.end(); ++i)
                {
                    mem += dynamicSize(*i);
                }
            }
            return mem + t.size() * (sizeof(T) + 4 * sizeof(std::size_t));
        }

        //! Overload for boost::circular_buffer
        template<typename T, typename A>
        static std::size_t dynamicSize(const boost::circular_buffer<T, A> &t)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                for (std::size_t i = 0; i < t.size(); ++i)
                {
                    mem += dynamicSize(t[i]);
                }
            }
            return mem + t.capacity() * (sizeof(T));
        }

        //! Overload for boost::optional
        template<typename T>
        static std::size_t dynamicSize(const boost::optional<T> &t)
        {
            if (!t)
            {
                return 0;
            }
            return dynamicSize(*t);
        }

        //! Overload for boost::reference_wrapper
        template<typename T>
        static std::size_t dynamicSize(const boost::reference_wrapper<T> &/*t*/)
        {
            return 0;
        }

        //! Overload for std::pair
        template<typename T, typename V>
        static std::size_t dynamicSize(const std::pair<T, V> &t)
        {
            std::size_t mem = 0;
            if (!memory_detail::SDynamicSizeAlwaysZero<T>::value())
            {
                mem += dynamicSize(t.first);
            }
            if (!memory_detail::SDynamicSizeAlwaysZero<V>::value())
            {
                mem += dynamicSize(t.second);
            }
            return mem;
        }

        //! Overload for boost::any
        static std::size_t dynamicSize(const boost::any &t)
        {
            // boost::any holds a pointer to a new'd item
            return ms_AnyVisitor.dynamicSize(t);
        }

        //! Default template
        template<typename T>
        static std::size_t staticSize(const T &t)
        {
            return memory_detail::SMemoryStaticSize<T>::dispatch(t);
        }

        //! Get the any visitor singleton.
        static CAnyVisitor &anyVisitor(void)
        {
            return ms_AnyVisitor;
        }

    private:
        static CAnyVisitor ms_AnyVisitor;
};


namespace memory_detail
{

template<typename T, void (T::*)(CMemoryUsage::TMemoryUsagePtr) const, typename R = void>
struct enable_if_member_debug_function
{
    typedef R type;
};

//! Default template declaration for SDebugMemoryDynamicSize::dispatch
template<typename T, typename ENABLE = void>
struct SDebugMemoryDynamicSize
{
    static void dispatch(const char *name, const T &t, CMemoryUsage::TMemoryUsagePtr mem)
    {
        std::size_t used = CMemory::dynamicSize(t);
        std::string description(name);
        description += "::";
        description += typeid(T).name();
        if (used)
        {
            mem->addItem(description, used);
        }
    }
};

//! Template specialisation where T has member function "debugMemoryUsage(CMemoryUsage::TMemoryUsagePtr)"
template<typename T>
struct SDebugMemoryDynamicSize<T, typename enable_if_member_debug_function<T, &T::debugMemoryUsage>::type>
{
    static void dispatch(const char *, const T &t, CMemoryUsage::TMemoryUsagePtr mem)
    {
        t.debugMemoryUsage(mem->addChild());
    }
};

} // memory_detail


//! \brief
//! Core memory debug usage template class
//!
//! DESCRIPTION:\n
//! Core memory debug usage template class. Provides an extension to the
//! CMemory class for creating a detailed breakdown of memory used by
//! classes and containers, utilising the CMemoryUsage class.
//!
//! Prelert classes can declare a public member function:
//!     void debugMemoryUsage(CMemoryUsage::TMemoryUsagePtr) const;
//!
//! which should call CMemoryDebug::dynamicSize("t_name", t, memUsagePtr); on all its dynamic members
//!
//! IMPLEMENTATION DECISIONS:\n
//! Template class to allow the compiler to determine the correct method
//! for arbitrary types
//!
//! Only contains static members, this should not be instantiated
//!
class CORE_EXPORT CMemoryDebug : private CNonInstantiatable
{
    private:
        static const std::string EMPTY_STRING;

    public:

        //! Default template
        template<typename T>
        static void dynamicSize(const char *name,
                                const T &t,
                                CMemoryUsage::TMemoryUsagePtr mem,
                                typename boost::disable_if<typename boost::is_pointer<T> >::type * = 0)
        {
            memory_detail::SDebugMemoryDynamicSize<T>::dispatch(name, t, mem);
        }

        //! Overload for pointer
        template<typename T>
        static void dynamicSize(const char *name,
                                const T &t,
                                CMemoryUsage::TMemoryUsagePtr mem,
                                typename boost::enable_if<typename boost::is_pointer<T> >::type * = 0)
        {
            if (t != 0)
            {
                mem->addItem("ptr", CMemory::staticSize(*t));
                memory_detail::SDebugMemoryDynamicSize<T>::dispatch(name, *t, mem);
            }
        }

        //! Overload for boost::shared_ptr
        template<typename T>
        static void dynamicSize(const char *name,
                                const boost::shared_ptr<T> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            if (t)
            {
                std::ostringstream ss;
                ss << "shared_ptr";
                long uc = t.use_count();
                // If the pointer is shared by multiple users, each one
                // might count it, so divide by the number of users.
                // However, if only 1 user has it, do a full debug
                if (uc == 1)
                {
                    mem->addItem(ss.str(), CMemory::staticSize(*t));
                    dynamicSize(name, *t, mem);
                }
                else
                {
                    ss << " name " << " (x" << uc << ")";
                    mem->addItem(ss.str(), (CMemory::staticSize(*t) + CMemory::dynamicSize(*t)) / uc);
                }
            }
        }

        //! Overload for boost::reference_wrapper
        template<typename T>
        static void dynamicSize(const char * /*name*/,
                                const boost::reference_wrapper<T> &/*t*/,
                                CMemoryUsage::TMemoryUsagePtr /*mem*/)
        {
            return;
        }

        //! Overload for std::vector
        template<typename T>
        static void dynamicSize(const char * name,
                                const std::vector<T> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string componentName(name);

            std::size_t items = t.size();
            std::size_t capacity = t.capacity();
            CMemoryUsage::SMemoryUsage usage(componentName + "::" + typeid(T).name(), capacity * sizeof(T), (capacity - items) * sizeof(T));
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            componentName += "_item";
            for (std::size_t i = 0; i < items; ++i)
            {
                dynamicSize(componentName.c_str(), t[i], ptr);
            }
        }

        //! Overload for std::string
        static void dynamicSize(const char *name,
                                const std::string &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string componentName(name);
            componentName += "_string";
            std::size_t length = t.size();
            std::size_t capacity = t.capacity();
            std::size_t unused = 0;
#ifdef MacOSX
            // For lengths up to 22 bytes there is no allocation
            if (capacity > 22)
            {
                unused = capacity - length;
                ++capacity;
            }
            else
            {
                capacity = 0;
            }

#elif (defined(Linux) || defined(SOLARIS)) && (!defined(_GLIBCXX_USE_CXX11_ABI) || _GLIBCXX_USE_CXX11_ABI == 0)
            // All sizes > 0 use the heap, and the string structure is
            // 1 pointer + 2 sizes + 1 null terminator
            // We don't handle the reference counting, so may overestimate
            // Even some 0 length strings may use the heap - see
            // http://info.prelert.com/blog/clearing-strings
            if (capacity > 0 ||
                t.data() != EMPTY_STRING.data())
            {
                unused = capacity - length;
                capacity += sizeof(void *) + (2 * sizeof(std::size_t)) + 1;
            }

#else // Linux with C++11 ABI and Windows
            // For lengths up to 15 bytes there is no allocation
            if (capacity > 15)
            {
                unused = capacity - length;
                ++capacity;
            }
            else
            {
                capacity = 0;
            }
#endif
            CMemoryUsage::SMemoryUsage usage(componentName, capacity, unused);
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);
        }

        //! Overload for boost::unordered_map
        template<typename K, typename V, typename H, typename P, typename A>
        static void dynamicSize(const char *name,
                                const boost::unordered_map<K, V, H, P, A> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string componentName(name);
            componentName += "_umap";

            std::size_t mapSize = (t.bucket_count() * sizeof(std::size_t) * 2)
                       + (t.size() * (sizeof(K) + sizeof(V) + 2 * sizeof(std::size_t)));

            CMemoryUsage::SMemoryUsage usage(componentName, mapSize);
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            for (typename boost::unordered_map<K, V, H, P, A>::const_iterator i = t.begin();
                     i != t.end(); ++i)
            {
                dynamicSize("key", i->first, ptr);
                dynamicSize("value", i->second, ptr);
            }
        }

        //! Overload for std::map
        template<typename K, typename V, typename C, typename A>
        static void dynamicSize(const char *name,
                                const std::map<K, V, C, A> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            //  std::map appears to use 4 pointers/ints per data item
            // (colour, parent, left and right child pointers)
            std::string componentName(name);
            componentName += "_map";

            std::size_t mapSize = t.size() * (sizeof(K) + sizeof(V) + 4 * sizeof(std::size_t));

            CMemoryUsage::SMemoryUsage usage(componentName, mapSize);
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            for (typename std::map<K, V, C, A>::const_iterator i = t.begin();
                     i != t.end(); ++i)
            {
                dynamicSize("key", i->first, ptr);
                dynamicSize("value", i->second, ptr);
            }
        }

        //! Overload for boost::unordered_set
        template<typename T, typename H, typename P, typename A>
        static void dynamicSize(const char *name,
                                const boost::unordered_set<T, H, P, A> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string componentName(name);
            componentName += "_uset";

            std::size_t mapSize = (t.bucket_count() * sizeof(std::size_t) * 2)
                       + (t.size() * (sizeof(T) + 2 * sizeof(std::size_t)));

            CMemoryUsage::SMemoryUsage usage(componentName, mapSize);
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            for (typename boost::unordered_set<T, H, P, A>::const_iterator i = t.begin();
                     i != t.end(); ++i)
            {
                dynamicSize("value", *i, ptr);
            }
        }

        //! Overload for std::set
        template<typename T, typename C, typename A>
        static void dynamicSize(const char *name,
                                const std::set<T, C, A> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            //  std::set appears to use 4 pointers/ints per data item
            // (colour, parent, left and right child pointers)
            std::string componentName(name);
            componentName += "_set";

            std::size_t mapSize = t.size() * (sizeof(T) + 4 * sizeof(std::size_t));

            CMemoryUsage::SMemoryUsage usage(componentName, mapSize);
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            for (typename std::set<T, C, A>::const_iterator i = t.begin();
                     i != t.end(); ++i)
            {
                dynamicSize("value", *i, ptr);
            }
        }

        //! Overload for boost::circular_buffer
        template<typename T, typename A>
        static void dynamicSize(const char * name,
                                const boost::circular_buffer<T, A> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string componentName(name);

            std::size_t items = t.size();
            std::size_t capacity = t.capacity();
            CMemoryUsage::SMemoryUsage usage(componentName + "::" + typeid(T).name(),
                    capacity * sizeof(T), (capacity - items) * sizeof(T));
            CMemoryUsage::TMemoryUsagePtr ptr = mem->addChild();
            ptr->setName(usage);

            componentName += "_item";
            for (std::size_t i = 0; i < items; ++i)
            {
                dynamicSize(componentName.c_str(), t[i], ptr);
            }
        }

        //! Overload for boost::optional
        template<typename T>
        static void dynamicSize(const char *name,
                                const boost::optional<T> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            if (t)
            {
                dynamicSize(name, *t, mem);
            }
        }

        //! Overload for std::pair
        template<typename T, typename V>
        static void dynamicSize(const char *name,
                                const std::pair<T, V> &t,
                                CMemoryUsage::TMemoryUsagePtr mem)
        {
            std::string keyName(name);
            keyName += "_key";
            std::string valueName(name);
            valueName += "_value";
            dynamicSize(keyName.c_str(), t.first, mem);
            dynamicSize(valueName.c_str(), t.second, mem);
        }
};

} // core
} // prelert

#endif // INCLUDED_prelert_core_CMemory_h
