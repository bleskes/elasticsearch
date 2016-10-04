
package org.elasticsearch.xpack.prelert.job.config.verification;


import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public final class AnalysisLimitsVerifier
{
    private AnalysisLimitsVerifier()
    {
    }

    /**
    /**
     * Checks the limits configuration is valid
     * <ol>
     * <li>CategorizationExamplesLimit cannot be &lt 0</li>
     * </ol>
     *
     * @param al
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(AnalysisLimits al) throws JobConfigurationException
    {
        if (al.getCategorizationExamplesLimit() != null && al.getCategorizationExamplesLimit() < 0)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                    AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT, 0, al.getCategorizationExamplesLimit());
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return true;
    }
}
