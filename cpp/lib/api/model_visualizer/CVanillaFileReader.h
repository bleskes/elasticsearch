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

#ifndef INCLUDED_ml_model_visualiser_CVanillaFileReader_h
#define INCLUDED_ml_model_visualiser_CVanillaFileReader_h

#include <model/CDataSearcher.h>

#include <string>

namespace ml
{
namespace model_visualiser
{

//! \brief A file reader for use with the api::CAnomalyDetector
//! restoreState function.
class CVanillaFileReader : public model::CDataSearcher
{
    public:
        CVanillaFileReader(std::string filename);

        //! Load the file
        //!
        //! \param[in] search Not used
        //! \param[out] results The file contents are returned in
        //! this map in the '_raw' field.
        virtual bool search(const std::string &search,
                            TStrStrUMapList &results);

    private:
        //! Name of the file to containing the serialized models.
        std::string m_Filename;
};

}
}

#endif // INCLUDED_ml_model_visualiser_CVanillaFileReader_h
