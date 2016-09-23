
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;


public class ConditionVerifierTest extends ESTestCase {

    public void testVerifyArgsNumericArgs() throws JobConfigurationException {
        Condition c = new Condition(Operator.LTE, "100");
        assertTrue(ConditionVerifier.verify(c));
        c = new Condition(Operator.GT, "10.0");
        assertTrue(ConditionVerifier.verify(c));
    }


    public void testVerify_GivenUnsetOperator() {
        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> ConditionVerifier.verify(new Condition()));

        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_OPERATOR), e.getMessage());
    }


    public void testVerify_GivenOperatorIsNone() {
        Condition condition = new Condition();
        condition.setOperator(Operator.NONE);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> ConditionVerifier.verify(condition));

        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_OPERATOR), e.getMessage());
    }


    public void testVerify_GivenEmptyValue() {
        Condition condition = new Condition();
        condition.setOperator(Operator.LT);
        condition.setValue("");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> ConditionVerifier.verify(condition));

        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, ""), e.getMessage());
    }


    public void testVerify_GivenInvalidRegex() {
        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);
        condition.setValue("[*");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> ConditionVerifier.verify(condition));

        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_REGEX, "[*"), e.getMessage());
    }


    public void testVerify_GivenNullRegex() {
        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> ConditionVerifier.verify(condition));

        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NULL, "[*"), e.getMessage());
    }
}
