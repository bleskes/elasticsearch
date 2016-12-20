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
#ifndef INCLUDED_prelert_core_CJsonDocUtils_h
#define INCLUDED_prelert_core_CJsonDocUtils_h

#include <core/CLogger.h>
#include <core/CNonInstantiatable.h>
#include <core/CoreTypes.h>
#include <core/ImportExport.h>

#include <rapidjson/document.h>

#include <boost/math/special_functions/fpclassify.hpp>

#include <string>
#include <utility>
#include <vector>

#include <stdint.h>


namespace prelert
{
namespace core
{

//! \brief
//! Utility functions for adding fields to JSON objects.
//!
//! DESCRIPTION:\n
//! Wraps up the code needed to add various types of values to JSON
//! objects.  Note that if a JSON document is an object then these methods
//! can be used to add fields to the RapidJSON document object too.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Field names are not copied - the field name strings MUST outlive the
//! JSON document they are being added to, or else memory corruption will
//! occur.
//!
//! Empty string fields are not written to the output unless specifically
//! requested.
//!
//! Memory for values added to the output documents is allocated from a pool (to
//! reduce allocation cost and memory fragmentation).  The user of this class
//! is responsible for managing this pool.
//!
class CORE_EXPORT CJsonDocUtils : private CNonInstantiatable
{
    public:
        typedef std::vector<core_t::TTime>           TTimeVec;
        typedef std::vector<std::string>             TStrVec;
        typedef std::vector<double>                  TDoubleVec;
        typedef std::pair<double, double>            TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr>         TDoubleDoublePrVec;
        typedef std::pair<double, TDoubleDoublePr>   TDoubleDoubleDoublePrPr;
        typedef std::vector<TDoubleDoubleDoublePrPr> TDoubleDoubleDoublePrPrVec;

    public:
        //! Adds a string field with the name fieldname to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addStringFieldToObj(const std::string &fieldName,
                                        const std::string &value,
                                        rapidjson::Value &obj,
                                        rapidjson::MemoryPoolAllocator<> &allocator,
                                        bool allowEmptyString = false);

        //! Adds a double field with the name fieldname to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addDoubleFieldToObj(const std::string &fieldName,
                                        double value,
                                        rapidjson::Value &obj,
                                        rapidjson::MemoryPoolAllocator<> &allocator);

        //! Adds a bool field with the name fieldname to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addBoolFieldToObj(const std::string &fieldName,
                                      bool value,
                                      rapidjson::Value &obj,
                                      rapidjson::MemoryPoolAllocator<> &allocator);

        //! Adds a signed integer field with the name fieldname to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addIntFieldToObj(const std::string &fieldName,
                                     int64_t value,
                                     rapidjson::Value &obj,
                                     rapidjson::MemoryPoolAllocator<> &allocator);

        //! Adds an unsigned integer field with the name fieldname to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addUIntFieldToObj(const std::string &fieldName,
                                      uint64_t value,
                                      rapidjson::Value &obj,
                                      rapidjson::MemoryPoolAllocator<> &allocator);

        //! Add an array of strings to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addStringArrayFieldToObj(const std::string &fieldName,
                                             const TStrVec &values,
                                             rapidjson::Value &obj,
                                             rapidjson::MemoryPoolAllocator<> &allocator);

        //! Add an array of doubles to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        template <typename CONTAINER>
        static void addDoubleArrayFieldToObj(const std::string &fieldName,
                                             const CONTAINER &values,
                                             rapidjson::Value &obj,
                                             rapidjson::MemoryPoolAllocator<> &allocator)
        {
            rapidjson::Value array(rapidjson::kArrayType);
            array.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);

            bool considerLogging(true);
            typedef typename CONTAINER::const_iterator TContainerCItr;
            for (TContainerCItr iter = values.begin();
                 iter != values.end();
                 ++iter)
            {
                double val = *iter;
                CJsonDocUtils::checkArrayNumberFinite(val, fieldName, considerLogging);
                array.PushBack(val, allocator);
            }

            obj.AddMember(fieldName.c_str(), array, allocator);
        }

        //! Add an array of pair double, pair double double to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addDoubleDoubleDoublePrPrArrayFieldToObj(const std::string &fieldName,
                                                             const TDoubleDoubleDoublePrPrVec &values,
                                                             rapidjson::Value &obj,
                                                             rapidjson::MemoryPoolAllocator<> &allocator);

        //! Add an array of TTimes to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addTimeArrayFieldToObj(const std::string &fieldName,
                                           const TTimeVec &values,
                                           rapidjson::Value &obj,
                                           rapidjson::MemoryPoolAllocator<> &allocator);

        //! Add an array of pair double double to an object.
        //! \p fieldName must outlive \p obj or memory corruption will occur.
        static void addDoubleDoublePrArrayFieldToObj(const std::string &firstFieldName,
                                                     const std::string &secondFieldName,
                                                     const TDoubleDoublePrVec &values,
                                                     rapidjson::Value &obj,
                                                     rapidjson::MemoryPoolAllocator<> &allocator);

        //! Checks if the \p obj has a member named \p fieldName and
        //! removes it if it does.
        static void removeMemberIfPresent(const std::string &fieldName, rapidjson::Value &obj);

    private:
        //! Log a message if we're trying to add nan/infinity to a JSON array
        template <typename NUMBER>
        static void checkArrayNumberFinite(NUMBER val,
                                           const std::string &fieldName,
                                           bool &considerLogging)
        {
            if (considerLogging && !(boost::math::isfinite)(val))
            {
                LOG_ERROR("Adding " << val << " to the \"" <<
                          fieldName << "\" array in a JSON document");
                // Don't return an error - RapidJSON will defend itself by converting to 0
                considerLogging = false;
            }
        }
};


}
}

#endif // INCLUDED_prelert_core_CJsonDocUtils_h

