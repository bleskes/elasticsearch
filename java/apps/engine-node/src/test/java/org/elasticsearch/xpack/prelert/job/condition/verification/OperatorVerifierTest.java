
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class OperatorVerifierTest extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public void testVerify() throws JobConfigurationException {
        assertTrue(OperatorVerifier.verify(Operator.EQ.name()));
        assertTrue(OperatorVerifier.verify("matCh"));
    }


    public void testVerify_unknownOp() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(
                "Unknown condition operator 'bad_op'");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_UNKNOWN_OPERATOR));

        OperatorVerifier.verify("bad_op");
    }
}
