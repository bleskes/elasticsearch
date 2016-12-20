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
#ifndef INCLUDED_CDynamicMeanTest_h
#define INCLUDED_CDynamicMeanTest_h

#include <cppunit/extensions/HelperMacros.h>

class CDynamicMeanTest : public CppUnit::TestFixture
{
    public:
        void    testAll(void);

        static CppUnit::Test *suite();

    private:
        typedef std::vector<double>         TDoubleVec;
        typedef TDoubleVec::const_iterator  TDoubleVecCItr;

        static void expectedValues(const TDoubleVec &sample,
                                    double &mean,
                                    double &var);
};

#endif // INCLUDED_CDynamicMeanTest_h
