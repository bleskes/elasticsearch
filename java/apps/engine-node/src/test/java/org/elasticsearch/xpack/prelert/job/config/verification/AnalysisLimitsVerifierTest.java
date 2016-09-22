
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class AnalysisLimitsVerifierTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenNegativeCategorizationExamplesLimit()
            throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(
                "categorizationExamplesLimit cannot be less than 0. Value = -1");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        AnalysisLimits limits = new AnalysisLimits(1L, -1L);

        System.out.print(limits.hashCode());

        AnalysisLimitsVerifier.verify(limits);
    }

    @Test
    public void testVerify_GivenValid() throws JobConfigurationException {
        AnalysisLimits limits = new AnalysisLimits(0L, 0L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, null);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, 1L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
    }
}
