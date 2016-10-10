
package org.elasticsearch.xpack.prelert.job.transform.verification;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public class RegexPatternVerifier implements ArgumentVerifier
{
    @Override
    public void verify(String arg, TransformConfig tc) throws ElasticsearchParseException
    {
        try
        {
            Pattern.compile(arg);
        }
        catch (PatternSyntaxException e)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_INVALID_ARGUMENT,
                    tc.getTransform(), arg);
            throw ExceptionsHelper.parseException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT);
        }
    }
}
