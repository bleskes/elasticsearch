
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public final class ModelDebugConfigVerifier
{
    private static final double MAX_PERCENTILE = 100.0;

    private ModelDebugConfigVerifier()
    {
    }

    /**
     * Checks the ModelDebugConfig is valid
     * <ol>
     * <li>If BoundsPercentile is set it must be $gt= 0.0 and &lt 100.0</li>
     * </ol>
     * @param config
     * @return
     */
    public static boolean verify(ModelDebugConfig config) {
        if (config.isEnabled() && (config.getBoundsPercentile() < 0.0 || config.getBoundsPercentile() > MAX_PERCENTILE)) {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
        return true;
    }
}
