package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

class CustomSettingsUpdater extends AbstractUpdater {

    public CustomSettingsUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
    }

    @Override
    void update(JsonNode node) {
        job().setCustomSettings(convertToMap(node, () -> Messages.getMessage(Messages.JOB_CONFIG_UPDATE_CUSTOM_SETTINGS_INVALID)));
    }
}
