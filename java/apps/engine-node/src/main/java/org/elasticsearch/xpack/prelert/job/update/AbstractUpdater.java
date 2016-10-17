package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

abstract class AbstractUpdater {
    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final JobDetails m_Job;
    private final String m_UpdateKey;

    AbstractUpdater(JobDetails job, String updateKey) {
        m_Job = Objects.requireNonNull(job);
        m_UpdateKey = Objects.requireNonNull(updateKey);
    }

    protected JobDetails job() {
        return m_Job;
    }

    protected String jobId() {
        return m_Job.getId();
    }

    protected String updateKey() {
        return m_UpdateKey;
    }

    protected final Map<String, Object> convertToMap(JsonNode node, Supplier<String> errorMessageSupplier) {
        try {
            return JSON_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw ExceptionsHelper.invalidRequestException(errorMessageSupplier.get(), ErrorCodes.INVALID_VALUE, e);
        }
    }

    protected void checkJobIsClosed() {
        JobStatus jobStatus = m_Job.getStatus();
        if (jobStatus != JobStatus.CLOSED) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_JOB_IS_NOT_CLOSED, m_UpdateKey, jobStatus), ErrorCodes.JOB_NOT_CLOSED);
        }
    }

    abstract void update(JsonNode node);
}
