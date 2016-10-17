package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.config.verification.AnalysisConfigVerifier;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class CategorizationFiltersUpdater extends AbstractUpdater {

    private List<String> m_NewCategorizationFilters;

    public CategorizationFiltersUpdater(JobDetails job, String updateKey) {
        super(job, updateKey);
        m_NewCategorizationFilters = null;
    }

    @Override
    void update(JsonNode node) {
        if (!node.isNull()) {
            if (!node.isArray()) {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_CATEGORIZATION_FILTERS_INVALID,
                        node.toString()), ErrorCodes.INVALID_VALUE);
            }
            parseStringArray(node);
            verifyNewCategorizationFilters();
        }
        job().getAnalysisConfig().setCategorizationFilters(m_NewCategorizationFilters);
    }

    private void parseStringArray(JsonNode arrayNode) {
        Iterator<JsonNode> iterator = arrayNode.elements();
        m_NewCategorizationFilters = iterator.hasNext() ? new ArrayList<>() : null;
        while (iterator.hasNext()) {
            JsonNode elementNode = iterator.next();
            if (elementNode.isTextual()) {
                m_NewCategorizationFilters.add(elementNode.asText());
            } else {
                throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_UPDATE_CATEGORIZATION_FILTERS_INVALID,
                        arrayNode.toString()), ErrorCodes.INVALID_VALUE);
            }
        }
    }

    private void verifyNewCategorizationFilters() {
        AnalysisConfig analysisConfig = job().getAnalysisConfig();
        analysisConfig.setCategorizationFilters(m_NewCategorizationFilters);
        AnalysisConfigVerifier.verify(analysisConfig);
    }
}
