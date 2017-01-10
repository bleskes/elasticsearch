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

#include <model/CSeriesClassifier.h>

#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <maths/CIntegerTools.h>

#include <numeric>


namespace ml
{
namespace model
{

namespace
{
std::string EMPTY_STRING;
}

const std::string CSeriesClassifier::IS_INTEGER_TAG("a");

CSeriesClassifier::CSeriesClassifier(void) : m_IsInteger(true)
{
}

void CSeriesClassifier::add(model_t::EFeature feature,
                            double value,
                            unsigned int count)
{
    if (!m_IsInteger)
    {
        return;
    }

    if (model_t::isMeanFeature(feature))
    {
        value *= count;
    }
    m_IsInteger = maths::CIntegerTools::isInteger(
                      value, 10.0 * std::numeric_limits<double>::epsilon() * value);
}

void CSeriesClassifier::add(model_t::EFeature feature,
                            const TDouble1Vec &values,
                            unsigned int count)
{
    if (!m_IsInteger)
    {
        return;
    }

    for (std::size_t i = 0u; m_IsInteger && i < values.size(); ++i)
    {
        this->add(feature, values[i], count);
    }
}

bool CSeriesClassifier::isInteger(void) const
{
    return m_IsInteger;
}

void CSeriesClassifier::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(IS_INTEGER_TAG, static_cast<int>(m_IsInteger));
}

bool CSeriesClassifier::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == IS_INTEGER_TAG)
        {
            int isInteger;
            if (core::CStringUtils::stringToType(traverser.value(), isInteger) == false)
            {
                LOG_ERROR("Invalid is integer flag in " << traverser.value());
                return false;
            }
            m_IsInteger = (isInteger != 0);
        }
    }
    while (traverser.next());

    return true;
}


}
}
