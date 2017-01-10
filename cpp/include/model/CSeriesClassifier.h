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

#ifndef INCLUDED_ml_model_CSeriesClassifier_h
#define INCLUDED_ml_model_CSeriesClassifier_h

#include <core/CSmallVector.h>

#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <string>
#include <vector>

namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}

namespace model
{

//! \brief Classifies a collection of values.
//!
//! DESCRIPTION:\n
//! Currently, this checks whether the values are all integers.
class MODEL_EXPORT CSeriesClassifier
{
    public:
        typedef core::CSmallVector<double, 1> TDouble1Vec;

    public:
        //! \name XML Tag Names
        //!
        //! These tag the metric gatherer member variables for model persistence.
        //@{
        static const std::string IS_INTEGER_TAG;
        //@}

    public:
        CSeriesClassifier(void);

        //! Update the classification with \p value.
        void add(model_t::EFeature feature,
                 double value,
                 unsigned int count);

        //! Update the classification with \p value.
        void add(model_t::EFeature feature,
                 const TDouble1Vec &value,
                 unsigned int count);

        //! Check if the values are all integers.
        bool isInteger(void) const;

        // Consider adding function to check if the values live
        // on a lattice: i.e. x = {a + b*i} for integer i. This
        // would need to convert x(i) to integers and find the
        // g.c.d. of x(2) - x(1), x(3) - x(2) and so on.

        //! \name Persistence
        //@{
        //! Persist state by passing information to the supplied inserter
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Create from part of an XML document.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);
        //@}

    private:
        //! Set to false if the series contains non-integer values.
        bool m_IsInteger;
};

}
}

#endif // INCLUDED_ml_model_CSeriesClassifier_h
