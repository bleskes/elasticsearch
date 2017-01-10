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

#include "CHierarchicalResultsLevelSetTest.h"

#include <model/CModelConfig.h>
#include <model/CHierarchicalResults.h>
#include <model/CHierarchicalResultsLevelSet.h>
#include <model/CAnnotatedProbability.h>

#include <boost/make_shared.hpp>
#include <boost/shared_ptr.hpp>


CppUnit::Test *CHierarchicalResultsLevelSetTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CHierarchicalResultsLevelSetTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CHierarchicalResultsLevelSetTest>(
                                   "CHierarchicalResultsLevelSetTest::testElements_withPerPartitionNormalisation",
                                   &CHierarchicalResultsLevelSetTest::testElements_withPerPartitionNormalisation) );

    return suiteOfTests;
}

struct TestNode
{
    TestNode(const std::string &name) : s_Name(name)
    {
    }

    std::string s_Name;
};

class CTestNodeFactory
{
    public:
        CTestNodeFactory()
        {
        }

        TestNode make(const std::string &name1,
                         const std::string &name2,
                         const std::string &name3,
                         const std::string &name4) const
        {
            return make(name1 + ' ' + name2 + ' ' + name3 + ' ' + name4);
        }

        TestNode make(const std::string &name1, const std::string &name2) const
        {
            return make(name1 + ' ' + name2);
        }

        TestNode make(const std::string &name) const
        {
            return TestNode(name);
        }
};

class CConcreteHierarchicalResultsLevelSet : public ml::model::CHierarchicalResultsLevelSet<TestNode>
{
public:
    CConcreteHierarchicalResultsLevelSet(const TestNode &root)
    :ml::model::CHierarchicalResultsLevelSet<TestNode>(root)
    {

    }

    //! Visit a node.
    virtual void visit(const ml::model::CHierarchicalResults &/*results*/, const TNode &/*node*/,
                        bool /*pivot*/)
    {
    }

    // make public
    using ml::model::CHierarchicalResultsLevelSet<TestNode>::elements;
};

void print(const TestNode *node)
{
    std::cout << "'" << node->s_Name << "'" << std::endl;
}

void CHierarchicalResultsLevelSetTest::testElements_withPerPartitionNormalisation(void)
{
    ml::model::hierarchical_results_detail::TStrPtr UNSET = boost::make_shared<std::string>("");
    ml::model::hierarchical_results_detail::TStrPtr PARTITION_A = boost::make_shared<std::string>("pA");
    ml::model::hierarchical_results_detail::TStrPtr PARTITION_B = boost::make_shared<std::string>("pB");
    ml::model::hierarchical_results_detail::TStrPtr PARTITION_C = boost::make_shared<std::string>("pC");

    ml::model::hierarchical_results_detail::TStrPtr PARTITION_VALUE_1 = boost::make_shared<std::string>("v1");
    ml::model::hierarchical_results_detail::TStrPtr PARTITION_VALUE_2 = boost::make_shared<std::string>("v2");
    ml::model::hierarchical_results_detail::TStrPtr PARTITION_VALUE_3 = boost::make_shared<std::string>("v3");



    TestNode root("root");

    ml::model::hierarchical_results_detail::SResultSpec spec;
    spec.s_PartitionFieldName = PARTITION_A;
    spec.s_PartitionFieldValue = PARTITION_VALUE_1;
    ml::model::SAnnotatedProbability emptyAnnotatedProb;

    ml::model::hierarchical_results_detail::SResultSpec unsetSpec;

    CConcreteHierarchicalResultsLevelSet::TNode parent(unsetSpec, emptyAnnotatedProb);
    CConcreteHierarchicalResultsLevelSet::TNode child(spec, emptyAnnotatedProb);
    CConcreteHierarchicalResultsLevelSet::TNode node(spec, emptyAnnotatedProb);
    node.s_Parent = &parent;
    node.s_Children.push_back(&child);


    std::vector<TestNode *> result;

    // without per partition normalization
    {
        CConcreteHierarchicalResultsLevelSet levelSet(root);
        levelSet.elements(node, false, CTestNodeFactory(), result, false);
        std::for_each(result.begin(), result.end(), print);
        CPPUNIT_ASSERT_EQUAL(size_t(1), result.size());
        CPPUNIT_ASSERT_EQUAL(std::string("pA"), result[0]->s_Name);
    }

    // with per partition normalization
    {
        CConcreteHierarchicalResultsLevelSet levelSet(root);
        levelSet.elements(node, false, CTestNodeFactory(), result, true);

        CPPUNIT_ASSERT_EQUAL(size_t(1), result.size());
        CPPUNIT_ASSERT_EQUAL(std::string("pAv1"), result[0]->s_Name);


        ml::model::hierarchical_results_detail::SResultSpec specB;
        specB.s_PartitionFieldName = PARTITION_B;
        specB.s_PartitionFieldValue = PARTITION_VALUE_1;

        CConcreteHierarchicalResultsLevelSet::TNode nodeB(specB, emptyAnnotatedProb);
        nodeB.s_Parent = &parent;
        nodeB.s_Children.push_back(&child);

        levelSet.elements(nodeB, false, CTestNodeFactory(), result, true);

        std::for_each(result.begin(), result.end(), print);
        CPPUNIT_ASSERT_EQUAL(size_t(1), result.size());
        CPPUNIT_ASSERT_EQUAL(std::string("pBv1"), result[0]->s_Name);
    }



}
