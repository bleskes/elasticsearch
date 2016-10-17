package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.Arrays;

class IgnoreDowntimeUpdater extends AbstractUpdater {

    public IgnoreDowntimeUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
    }

    @Override
    void update(JsonNode node) {
        try {
            IgnoreDowntime ignoreDowntime = JSON_MAPPER.convertValue(node, IgnoreDowntime.class);
            job().setIgnoreDowntime(ignoreDowntime);
        } catch (IllegalArgumentException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_IGNORE_DOWNTIME_PARSE_ERROR,
                    Arrays.toString(IgnoreDowntime.values()), node.toString()), ErrorCodes.INVALID_VALUE);
        }
    }
}
