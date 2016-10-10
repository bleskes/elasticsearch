
package org.elasticsearch.xpack.prelert.job.condition.verification;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.condition.UnknownOperatorException;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public final class OperatorVerifier {
    private OperatorVerifier() {
        // Hide default constructor
    }

    /**
     * Checks that the <code>name</code> string is a string value of an Operator
     * enum
     *
     * @param name
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(String name) throws ElasticsearchParseException {
        try {
            Operator.fromString(name);
        } catch (UnknownOperatorException e) {
            throw ExceptionsHelper.parseException(Messages.getMessage(Messages.JOB_CONFIG_CONDITION_UNKNOWN_OPERATOR, name),
                    ErrorCodes.CONDITION_UNKNOWN_OPERATOR);
        }

        return true;
    }
}
