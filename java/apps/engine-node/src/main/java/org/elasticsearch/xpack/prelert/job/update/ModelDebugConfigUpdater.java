package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.config.verification.ModelDebugConfigVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ModelDebugConfigWriter;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.io.StringWriter;

class ModelDebugConfigUpdater extends AbstractUpdater {

    private final StringWriter configWriter;

    public ModelDebugConfigUpdater(JobDetails job, String updateKey, StringWriter configWriter) {
        super(job, updateKey);
        this.configWriter = configWriter;
    }

    @Override
    void update(JsonNode node) {
        try {
            ModelDebugConfig newConfig = JSON_MAPPER.convertValue(node, ModelDebugConfig.class);
            if (newConfig != null) {
                ModelDebugConfigVerifier.verify(newConfig);
            }
            job().setModelDebugConfig(newConfig);
            write(newConfig);
        } catch (IllegalArgumentException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_MODEL_DEBUG_CONFIG_PARSE_ERROR),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void write(ModelDebugConfig modelDebugConfig) {
        configWriter.write("[modelDebugConfig]\n");
        if (modelDebugConfig == null) {
            modelDebugConfig = new ModelDebugConfig(null, -1.0, null);
        }
        try {
            new ModelDebugConfigWriter(modelDebugConfig, configWriter).write();
        } catch (IOException e) {
            throw ExceptionsHelper.invalidRequestException("Failed to write modelDebugConfig", ErrorCodes.UNKNOWN_ERROR, e);
        }
    }
}
