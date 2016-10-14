
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
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
        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> ConditionVerifier.verify(Condition.NONE));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals("Unexpected ErrorCode: " + ErrorCodes.fromCode(Long.valueOf(e.getHeader("errorCode").get(0))),
                ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_OPERATOR), e.getMessage());
    }


    public void testVerify_GivenOperatorIsNone() {
        Condition condition = Condition.NONE;

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> ConditionVerifier.verify(condition));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_OPERATOR), e.getMessage());
    }


    public void testVerify_GivenEmptyValue() {
        Condition condition = new Condition(Operator.LT, "");

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> ConditionVerifier.verify(condition));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, ""), e.getMessage());
    }


    public void testVerify_GivenInvalidRegex() {
        Condition condition = new Condition(Operator.MATCH, "[*");

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> ConditionVerifier.verify(condition));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_REGEX, "[*"), e.getMessage());
    }


    public void testVerify_GivenNullRegex() {
        Condition condition = new Condition(Operator.MATCH, null);

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class,
                () -> ConditionVerifier.verify(condition));

        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(ErrorCodes.CONDITION_INVALID_ARGUMENT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NULL, "[*"), e.getMessage());
    }
}
