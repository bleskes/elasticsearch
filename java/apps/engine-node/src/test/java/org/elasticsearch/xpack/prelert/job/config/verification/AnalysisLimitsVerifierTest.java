
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class AnalysisLimitsVerifierTest extends ESTestCase {

    public void testVerify_GivenNegativeCategorizationExamplesLimit() {

        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT, 0, -1l);

        AnalysisLimits limits = new AnalysisLimits(1L, -1L);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> AnalysisLimitsVerifier.verify(limits));
        assertEquals(errorMessage, e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
    }


    public void testVerify_GivenValid() throws JobConfigurationException {
        AnalysisLimits limits = new AnalysisLimits(0L, 0L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, null);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, 1L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
    }
}
