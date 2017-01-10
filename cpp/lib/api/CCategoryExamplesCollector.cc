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

#include <core/CContainerPrinter.h>

#include <core/CLogger.h>
#include <core/CStringUtils.h>

#include <api/CCategoryExamplesCollector.h>

#include <boost/bind.hpp>

namespace ml
{
namespace api
{

namespace
{

const std::string EXAMPLES_BY_CATEGORY_TAG("a");
const std::string CATEGORY_TAG("b");
const std::string EXAMPLE_TAG("c");

const CCategoryExamplesCollector::TStrSet EMPTY_EXAMPLES;

} // unnamed


CCategoryExamplesCollector::CCategoryExamplesCollector(std::size_t maxExamples)
    : m_MaxExamples(maxExamples)
{
}

CCategoryExamplesCollector::CCategoryExamplesCollector(std::size_t maxExamples, core::CStateRestoreTraverser &traverser)
    : m_MaxExamples(maxExamples)
{
    traverser.traverseSubLevel(
            boost::bind(&CCategoryExamplesCollector::acceptRestoreTraverser, this, _1));
}

bool CCategoryExamplesCollector::add(std::size_t category, const std::string &example)
{
    if (m_MaxExamples == 0)
    {
        return false;
    }
    TStrSet &examplesForCategory = m_ExamplesByCategory[category];
    if (examplesForCategory.size() >= m_MaxExamples)
    {
        return false;
    }
    return examplesForCategory.insert(example).second;
}

std::size_t CCategoryExamplesCollector::numberOfExamplesForCategory(std::size_t category) const
{
    TSizeStrSetUMapCItr iterator = m_ExamplesByCategory.find(category);
    return (iterator == m_ExamplesByCategory.end()) ? 0 : iterator->second.size();
}

const CCategoryExamplesCollector::TStrSet &CCategoryExamplesCollector::examples(
                                                                        std::size_t category) const
{
    TSizeStrSetUMapCItr iterator = m_ExamplesByCategory.find(category);
    if (iterator == m_ExamplesByCategory.end())
    {
        return EMPTY_EXAMPLES;
    }
    return iterator->second;
}

void CCategoryExamplesCollector::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    for (TSizeStrSetUMapCItr itr = m_ExamplesByCategory.begin();
         itr != m_ExamplesByCategory.end(); ++itr)
    {
        inserter.insertLevel(EXAMPLES_BY_CATEGORY_TAG, boost::bind(
                &CCategoryExamplesCollector::persistExamples, this, itr->first, itr->second, _1));
    }
}

void CCategoryExamplesCollector::persistExamples(std::size_t category,
                                                 const TStrSet &examples,
                                                 core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(CATEGORY_TAG, category);
    for (TStrSetCItr itr = examples.begin(); itr != examples.end(); ++itr)
    {
        inserter.insertValue(EXAMPLE_TAG, *itr);
    }
}

bool CCategoryExamplesCollector::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    m_ExamplesByCategory.clear();
    do
    {
        const std::string &name = traverser.name();
        if (name == EXAMPLES_BY_CATEGORY_TAG)
        {
            if (traverser.traverseSubLevel(
                    boost::bind(&CCategoryExamplesCollector::restoreExamples, this, _1)) == false)
            {
                LOG_ERROR("Error restoring examples by category");
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

bool CCategoryExamplesCollector::restoreExamples(core::CStateRestoreTraverser &traverser)
{
    std::size_t category = 0;
    TStrSet examples;
    do
    {
        const std::string &name = traverser.name();
        if (name == CATEGORY_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(), category) == false)
            {
                LOG_ERROR("Error restoring category: " << traverser.value());
                return false;
            }
        }
        else if (name == EXAMPLE_TAG)
        {
            examples.insert(traverser.value());
        }
    }
    while (traverser.next());

    LOG_TRACE("Restoring examples for category " << category << ": " << core::CContainerPrinter::print(examples));
    m_ExamplesByCategory[category].swap(examples);

    return true;
}

void CCategoryExamplesCollector::clear(void)
{
    m_ExamplesByCategory.clear();
}


}
}
