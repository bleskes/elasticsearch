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
#ifndef INCLUDED_ml_maths_CAnomalyFactorCalculator_h
#define INCLUDED_ml_maths_CAnomalyFactorCalculator_h

#include <maths/ImportExport.h>

#include <stdint.h>


namespace ml
{
namespace maths
{

//! \brief
//! Encapsulates methods for calculating anomaly factors
//!
//! DESCRIPTION:\n
//! Methods for converting various inputs to anomaly factors,
//! which must be in the range 0 - 100, should be centralised in
//! this class.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Static class - not to be instantiated
//!
class MATHS_EXPORT CAnomalyFactorCalculator
{
    public:
        //! Calculate an anomaly factor from the the probability of a
        //! statistical distribution taking a given value
        static int32_t fromDistributionProb(double deviation);

    private:
        //! Hide constructors
        CAnomalyFactorCalculator(void);
        CAnomalyFactorCalculator(const CAnomalyFactorCalculator &);
};


}
}

#endif // INCLUDED_ml_maths_CAnomalyFactorCalculator_h

