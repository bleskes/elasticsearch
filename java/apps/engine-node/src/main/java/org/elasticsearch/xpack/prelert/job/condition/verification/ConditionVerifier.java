
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConditionVerifier
{
    private ConditionVerifier()
    {
        // Hide default constructor
    }

    /**
     * Check that the condition has an operator and the operator
     * operand is valid. In the case of numerical operators this means
     * the operand can be parsed as a number else is is a Regex match
     * so check the operand is a valid regex.
     *
     * @param condition
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(Condition condition) throws JobConfigurationException
    {
        OperatorVerifier.verify(condition.getOperator().name());
        if (condition.getOperator() == Operator.NONE)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_OPERATOR),
                    ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }

        if (condition.getValue() == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NULL),
                    ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }

        if (condition.getOperator().expectsANumericArgument())
        {
            try
            {
                Double.parseDouble(condition.getValue());
            }
            catch (NumberFormatException nfe)
            {
                String msg = Messages.getMessage(
                        Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_NUMBER, condition.getValue());
                throw new JobConfigurationException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        }
        else
        {
            try
            {
                Pattern.compile(condition.getValue());
            }
            catch (PatternSyntaxException e)
            {
                String msg = Messages.getMessage(
                                Messages.JOB_CONFIG_CONDITION_INVALID_VALUE_REGEX,
                                condition.getValue());
                throw new JobConfigurationException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        }

        return true;
    }

}
