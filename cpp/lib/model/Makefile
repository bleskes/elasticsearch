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

TARGET=$(OBJS_DIR)/libPreModel$(DYNAMIC_LIB_EXT)

USE_BOOST=1

LIBS:=$(LIB_PRE_CORE) $(LIB_PRE_MATHS)

all: build

SRCS= \
CAnnotatedProbability.cc \
CAnnotatedProbabilityBuilder.cc \
CAnomalyChain.cc \
CAnomalyDetector.cc \
CAnomalyScore.cc \
CBucketGatherer.cc \
CBoxPlotData.cc \
CCountingModel.cc \
CCountingModelFactory.cc \
CDataGatherer.cc \
CDecayRateController.cc \
CDetectionRule.cc \
CDetectorEqualizer.cc \
CDynamicStringIdRegistry.cc \
CEventData.cc \
CEventRateBucketGatherer.cc \
CEventRateModel.cc \
CEventRateModelFactory.cc \
CEventRatePeersModel.cc \
CEventRatePeersModelFactory.cc \
CEventRatePopulationModel.cc \
CEventRatePopulationModelFactory.cc \
CFeatureData.cc \
CGathererTools.cc \
CHierarchicalResults.cc \
CHierarchicalResultsAggregator.cc \
CHierarchicalResultsNormalizer.cc \
CHierarchicalResultsPopulator.cc \
CHierarchicalResultsProbabilityFinalizer.cc \
CIndividualModel.cc \
CInterimBucketCorrector.cc \
CLimits.cc \
CMemoryUsageEstimator.cc \
CMetricBucketGatherer.cc \
CMetricModel.cc \
CMetricModelFactory.cc \
CMetricPopulationModel.cc \
CMetricPopulationModelFactory.cc \
CModel.cc \
CModelConfig.cc \
CModelDetailsView.cc \
CModelEnsemble.cc \
CModelFactory.cc \
CModelParams.cc \
CModelTools.cc \
CPartitioningFields.cc \
CPopulationModel.cc \
CProbabilityAndInfluenceCalculator.cc \
CResourceMonitor.cc \
CResultsQueue.cc \
CRuleCondition.cc \
CSample.cc \
CSampleCounts.cc \
CSearchKey.cc \
CSeriesClassifier.cc \
CSimpleCountDetector.cc \
CStringStore.cc \
FrequencyPredicates.cc \
FunctionTypes.cc \
ModelTypes.cc

NO_BENCH_MARKS=1

include $(CPP_SRC_HOME)/mk/dynamiclib.mk
