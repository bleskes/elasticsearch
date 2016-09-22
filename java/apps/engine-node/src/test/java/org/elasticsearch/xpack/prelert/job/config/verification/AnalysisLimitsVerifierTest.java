
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class AnalysisLimitsVerifierTest extends ESTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public void testVerify_GivenNegativeCategorizationExamplesLimit()
            throws JobConfigurationException {

        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT, 0, -1l);
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        AnalysisLimits limits = new AnalysisLimits(1L, -1L);
        AnalysisLimitsVerifier.verify(limits);
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
