
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class ModelDebugConfigVerifierTests extends ESTestCase {


    public void testVerify_GivenBoundPercentileLessThanZero() {
        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class,
                        () -> ModelDebugConfigVerifier.verify(new ModelDebugConfig(-1.0, "")));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }


    public void testVerify_GivenBoundPercentileGreaterThan100() {
        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class,
                        () -> ModelDebugConfigVerifier.verify(new ModelDebugConfig(100.1, "")));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE, ""), e.getMessage());
    }


    public void testVerify_GivenValid() {
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig()));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "")));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "foo,bar")));
    }
}
