
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class ConditionVerifierTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testVerifyArgsNumericArgs() throws JobConfigurationException {
        Condition c = new Condition(Operator.LTE, "100");
        assertTrue(ConditionVerifier.verify(c));
        c = new Condition(Operator.GT, "10.0");
        assertTrue(ConditionVerifier.verify(c));
    }

    @Test
    public void testVerify_GivenUnsetOperator() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage("Invalid operator for condition");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        ConditionVerifier.verify(new Condition());
    }

    @Test
    public void testVerify_GivenOperatorIsNone() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage("Invalid operator for condition");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.NONE);

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenEmptyValue() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(
                "Invalid condition value: cannot parse a double from string ''");
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.LT);
        condition.setValue("");

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenInvalidRegex() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);
        condition.setValue("[*");

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenNullRegex() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);

        ConditionVerifier.verify(condition);
    }
}
