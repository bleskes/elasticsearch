package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.config.verification.AnalysisLimitsVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

class AnalysisLimitsUpdater extends AbstractUpdater {

    public AnalysisLimitsUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
    }

    @Override
    void update(JsonNode node) {
        checkJobIsClosed();
        AnalysisLimits newLimits = parseAnalysisLimits(node);
        checkNotNull(newLimits);
        AnalysisLimitsVerifier.verify(newLimits);
        checkModelMemoryLimitIsNotDecreased(newLimits);
        job().setAnalysisLimits(newLimits);
    }

    private AnalysisLimits parseAnalysisLimits(JsonNode node) {
        try {
            return JSON_MAPPER.convertValue(node, AnalysisLimits.class);
        } catch (IllegalArgumentException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_PARSE_ERROR),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkNotNull(AnalysisLimits limits) {
        if (limits == null) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_CANNOT_BE_NULL),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkModelMemoryLimitIsNotDecreased(AnalysisLimits newLimits) {
        AnalysisLimits analysisLimits = job().getAnalysisLimits();
        if (analysisLimits == null) {
            return;
        }
        long oldMemoryLimit = analysisLimits.getModelMemoryLimit();
        long newMemoryLimit = newLimits.getModelMemoryLimit();
        if (newMemoryLimit < oldMemoryLimit) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_MODEL_MEMORY_LIMIT_CANNOT_BE_DECREASED,
                            oldMemoryLimit, newMemoryLimit), ErrorCodes.INVALID_VALUE);
        }
    }
}
