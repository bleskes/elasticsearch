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

#ifndef INCLUDED_ml_model_visualiser_CModelVisualiser_h
#define INCLUDED_ml_model_visualiser_CModelVisualiser_h

#include <core/CoreTypes.h>

#include <api/CAnomalyDetector.h>

#include <string>

namespace ml
{
namespace api
{
class CFieldConfig;
class COutputHandler;
}
namespace model
{
class CLimits;
class CModelConfig;
}
namespace model_visualiser
{

//! \brief Wrapper around the api::CAnomalyDetector which
//! loads and visualizes the models.
class CModelVisualiser : private api::CAnomalyDetector
{
    public:
        CModelVisualiser(model::CLimits &limits,
                         const api::CFieldConfig &fieldConfig,
                         const model::CModelConfig &modelConfig,
                         api::COutputHandler &outputHandler);

        //! Load the model file.
        bool load(const std::string &filename);

        //! Visualise the models at \p time.
        void visualise(core_t::TTime time,
                       const std::string &byClause,
                       const std::string &overClause) const;
};


}
}

#endif // INCLUDED_ml_model_visualiser_CModelVisualiser_h
