
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class ModelDebugConfigVerifierTest extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public void testVerify_GivenBoundPercentileLessThanZero() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(
                "Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        ModelDebugConfigVerifier.verify(new ModelDebugConfig(-1.0, ""));
    }


    public void testVerify_GivenBoundPercentileGreaterThan100() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(
                "Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        ModelDebugConfigVerifier.verify(new ModelDebugConfig(100.1, ""));
    }


    public void testVerify_GivenValid() throws JobConfigurationException {
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig()));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "")));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "foo,bar")));
    }
}
