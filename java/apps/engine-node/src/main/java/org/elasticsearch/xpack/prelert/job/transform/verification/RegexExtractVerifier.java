
package org.elasticsearch.xpack.prelert.job.transform.verification;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.util.List;
import java.util.regex.Pattern;

public class RegexExtractVerifier implements ArgumentVerifier
{
    @Override
    public void verify(String arg, TransformConfig tc) throws JobConfigurationException
    {
        new RegexPatternVerifier().verify(arg, tc);

        Pattern pattern = Pattern.compile(arg);
        int groupCount = pattern.matcher("").groupCount();
        List<String> outputs = tc.getOutputs();
        int outputCount = outputs == null ? 0 : outputs.size();
        if (groupCount != outputCount)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_EXTRACT_GROUPS_SHOULD_MATCH_OUTPUT_COUNT,
                    tc.getTransform(), outputCount, arg, groupCount);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT);
        }
    }
}
