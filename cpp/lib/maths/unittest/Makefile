#
# ELASTICSEARCH CONFIDENTIAL
#
# Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
#
# Notice: this software, and all information contained
# therein, is the exclusive property of Elasticsearch BV
# and its licensors, if any, and is protected under applicable
# domestic and foreign law, and international treaties.
#
# Reproduction, republication or distribution without the
# express written consent of Elasticsearch BV is
# strictly prohibited.
#
include $(CPP_SRC_HOME)/mk/defines.mk

TARGET=pre_test$(EXE_EXT)

USE_BOOST=1

LIBS:=$(LIB_PRE_MATHS)

all: build

SRCS=\
	Main.cc \
	CAgglomerativeClustererTest.cc \
	CAnomalyFactorCalculatorTest.cc \
	CAssignmentTest.cc \
	CBasicStatisticsTest.cc \
	CBjkstUniqueValuesTest.cc \
	CBootstrapClustererTest.cc \
	CBoundingBoxTest.cc \
	CCategoricalToolsTest.cc \
	CChecksumTest.cc \
	CChiSquaredTestTest.cc \
	CClustererTest.cc \
	CCounterTest.cc \
	CCountMinSketchTest.cc \
	CDynamicMeanTest.cc \
	CEntropySketchTest.cc \
	CEqualWithToleranceTest.cc \
	CEuclideanDistanceTest.cc \
	CForecastTest.cc \
	CGammaRateConjugateTest.cc \
	CGramSchmidtTest.cc \
	CInformationCriteriaTest.cc \
	CIntegerToolsTest.cc \
	CIntegrationTest.cc \
	CKdTreeTest.cc \
	CKMeansFastTest.cc \
	CKMeansOnlineTest.cc \
	CLassoLogisticRegressionTest.cc \
	CLinearAlgebraTest.cc \
	CLogNormalMeanPrecConjugateTest.cc \
	CLogTDistributionTest.cc \
	CMathsFuncsTest.cc \
	CMathsMemoryTest.cc \
	CMixtureDistributionTest.cc \
	CKMostCorrelatedTest.cc \
	CMultimodalPriorTest.cc\
	CMultinomialConjugateTest.cc \
	CMultivariateConstantPriorTest.cc \
	CMultivariateMultimodalPriorTest.cc \
	CMultivariateNormalConjugateTest.cc \
	CMultivariateOneOfNPriorTest.cc \
	CNaturalBreaksClassifierTest.cc \
	CNormalMeanPrecConjugateTest.cc \
	COneOfNPriorTest.cc \
	COrderingsTest.cc \
	COrdinalTest.cc \
	CPackedBitVectorTest.cc \
	CPoissonMeanConjugateTest.cc \
	CPriorTest.cc \
	CPRNGTest.cc \
	CProbabilityAggregatorsTest.cc \
	CProbabilityCalibratorTest.cc \
	CQDigestTest.cc \
	CQuantileSketchTest.cc \
	CRegressionTest.cc \
	CRadialBasisFunctionTest.cc \
	CRandomProjectionClustererTest.cc \
	CSamplingTest.cc \
	CSeasonalComponentTest.cc \
	CSeasonalComponentAdaptiveBucketingTest.cc \
	CSetToolsTest.cc \
	CSignalTest.cc \
	CSolversTest.cc \
	CSplineTest.cc \
	CStatisticalTestsTest.cc \
	CStudentTTestTest.cc \
	CTimeSeriesDecompositionTest.cc \
	CToolsTest.cc \
	CTrendTestsTest.cc \
	CXMeansOnlineTest.cc \
	CXMeansOnline1dTest.cc \
	CXMeansTest.cc \
	TestUtils.cc \

include $(CPP_SRC_HOME)/mk/stdcppunit.mk
