
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.test.ESTestCase;import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class OperatorVerifierTest extends ESTestCase {

    public void testVerify() throws JobConfigurationException {
        assertTrue(OperatorVerifier.verify(Operator.EQ.name()));
        assertTrue(OperatorVerifier.verify("matCh"));
    }


    public void testVerify_unknownOp() {
        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> OperatorVerifier.verify("bad_op"));

        assertEquals(ErrorCodes.CONDITION_UNKNOWN_OPERATOR, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_UNKNOWN_OPERATOR, "bad_op"), e.getMessage());
    }
}
