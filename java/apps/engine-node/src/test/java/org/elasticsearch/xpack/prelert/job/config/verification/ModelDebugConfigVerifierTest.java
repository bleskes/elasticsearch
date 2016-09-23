
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class ModelDebugConfigVerifierTest extends ESTestCase {


    public void testVerify_GivenBoundPercentileLessThanZero() {
        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> ModelDebugConfigVerifier.verify(new ModelDebugConfig(-1.0, "")));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }


    public void testVerify_GivenBoundPercentileGreaterThan100() {
        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> ModelDebugConfigVerifier.verify(new ModelDebugConfig(100.1, "")));

        assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }


    public void testVerify_GivenValid() throws JobConfigurationException {
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig()));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "")));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "foo,bar")));
    }
}
