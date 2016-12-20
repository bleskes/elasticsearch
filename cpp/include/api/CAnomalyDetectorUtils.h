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
#ifndef INCLUDED_prelert_api_CAnomalyDetectorUtils_h
#define INCLUDED_prelert_api_CAnomalyDetectorUtils_h

#include <core/CNonInstantiatable.h>
#include <core/CoreTypes.h>

#include <model/CAnomalyDetector.h>
#include <model/CModelFactory.h>
#include <model/CSearchKey.h>
#include <model/CSimpleCountDetector.h>

#include <api/ImportExport.h>

#include <boost/unordered_map.hpp>

#include <string>


namespace prelert
{
namespace model
{
class CLimits;
class CModelConfig;
class CResourceMonitor;
}
namespace api
{
class CFieldConfig;

//! \brief
//! Utility functions to avoid cut-and-paste between anomaly detectors.
//!
//! DESCRIPTION:\n
//! The CAnomalyDetector class in the api library, and the CAnomalyDetector
//! and CReversingAnomalyDetector classes in the splunk library share a lot
//! of the same functionality.  This class factors out some of the shared
//! code to avoid duplicating it between source files.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The parts that vary between the different anomaly detectors are handled
//! using template arguments.
//!
class API_EXPORT CAnomalyDetectorUtils : private core::CNonInstantiatable
{
    public:
        typedef std::vector<std::string> TStrVec;
        typedef boost::unordered_map<std::string, std::string> TStrStrUMap;
        typedef TStrStrUMap::iterator TStrStrUMapItr;
        typedef TStrStrUMap::const_iterator TStrStrUMapCItr;
        typedef boost::unordered_map<std::string, TStrVec> TStrStrVecUMap;
        typedef TStrStrVecUMap::iterator TStrStrVecUMapItr;
        typedef TStrStrVecUMap::const_iterator TStrStrVecUMapCItr;
        typedef std::vector<model::CSearchKey> TKeyVec;
        typedef boost::unordered_map<std::string, TKeyVec> TStrKeyVecUMap;
        typedef TStrKeyVecUMap::iterator TStrKeyVecUMapItr;
        typedef TStrKeyVecUMap::const_iterator TStrKeyVecUMapCItr;

    public:
        static const std::string EMPTY_STRING;

    public:
        //! Given a detector key, create and return the appropriate anomaly
        //! detector.  This will never return a NULL pointer.
        static model::CAnomalyDetector::TAnomalyDetectorPtr
                makeDetector(int identifier,
                             const model::CModelConfig &modelConfig,
                             model::CLimits &limits,
                             const std::string &partitionFieldValue,
                             core_t::TTime firstTime,
                             const model::CAnomalyDetector::TModelFactoryCPtr &modelFactory);

        //! Make the detector keys for any newly observed source type in
        //! the record with fields \p dataRowFields.
        static const TKeyVec &detectorKeys(const std::string &sourceName,
                                           const CFieldConfig &fieldConfig,
                                           const TStrStrUMap &dataRowFields,
                                           TStrKeyVecUMap &keys,
                                           model::CResourceMonitor &resourceMonitor);

        //! Extract the field called \p fieldName from \p dataRowFields.
        static const std::string *fieldValue(const std::string &fieldName,
                                             const TStrStrUMap &dataRowFields)
        {
            TStrStrUMapCItr itr = fieldName.empty() ?
                                  dataRowFields.end() :
                                  dataRowFields.find(fieldName);
            const std::string &fieldValue(itr == dataRowFields.end() ?
                                          EMPTY_STRING :
                                          itr->second);
            return !fieldName.empty() && fieldValue.empty() ?
                   static_cast<const std::string*>(0) : &fieldValue;
        }

        //! Loop over all field configs, extract the relevant fields and pass
        //! them on to the relevant detector method.  The exact details of what
        //! to do with the extracted fields is delegated to the function passed
        //! as a template argument.  This code used to be more-or-less
        //! duplicated in three different source files but is now factored out
        //! into this single method.
        template <typename DETECT_FUNC>
        static void detect(const DETECT_FUNC &detectFunc,
                           const TStrVec &fieldNames,
                           const TStrStrUMap &dataRowFields)
        {
            model::CAnomalyDetector::TStrCPtrVec fieldValues;
            fieldValues.reserve(fieldNames.size());
            for (std::size_t i = 0u; i < fieldNames.size(); ++i)
            {
                fieldValues.push_back(fieldValue(fieldNames[i], dataRowFields));
            }
            detectFunc(fieldValues);
        }
};

}
}

#endif // INCLUDED_prelert_api_CAnomalyDetectorUtils_h

