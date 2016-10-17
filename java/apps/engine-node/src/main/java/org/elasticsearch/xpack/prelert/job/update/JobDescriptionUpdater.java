package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

class JobDescriptionUpdater extends AbstractUpdater {

    public JobDescriptionUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
    }

    @Override
    void update(JsonNode node) {
        if (node.isTextual() == false) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DESCRIPTION_INVALID),
                    ErrorCodes.INVALID_VALUE);
        }
        job().setDescription(node.asText());
    }
}
