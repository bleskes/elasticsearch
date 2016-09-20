
package org.elasticsearch.xpack.prelert.job.transform.verification;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexPatternVerifier implements ArgumentVerifier
{
    @Override
    public void verify(String arg, TransformConfig tc) throws JobConfigurationException
    {
        try
        {
            Pattern.compile(arg);
        }
        catch (PatternSyntaxException e)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT,
                    tc.getTransform(), arg);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT);
        }
    }
}
