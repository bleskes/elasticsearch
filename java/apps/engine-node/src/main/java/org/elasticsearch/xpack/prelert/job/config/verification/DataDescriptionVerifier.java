
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.time.DateTimeFormatterTimestampConverter;

public final class DataDescriptionVerifier
{
    private DataDescriptionVerifier()
    {
    }

    /**
     * Verify the data description configuration
     * @param dd DataDescription
     * <ol>
     * <li>Check the timeFormat - if set - is either {@value DataDescription#EPOCH},
     * {@value DataDescription#EPOCH_MS} or a valid format string</li>
     * <li></li>
     * </ol>
     */
    public static boolean verify(DataDescription dd) {
        if (dd.getTimeFormat() != null && dd.getTimeFormat().isEmpty() == false) {
            if (dd.getTimeFormat().equals(DataDescription.EPOCH) || dd.getTimeFormat().equals(DataDescription.EPOCH_MS)) {
                return true;
            }

            try {
                DateTimeFormatterTimestampConverter.ofPattern(dd.getTimeFormat());
            } catch (IllegalArgumentException e) {
                String message = Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT, dd.getTimeFormat());
                throw ExceptionsHelper.invalidRequestException(message,  ErrorCodes.INVALID_DATE_FORMAT, e);
            }
        }

        return true;
    }
}
