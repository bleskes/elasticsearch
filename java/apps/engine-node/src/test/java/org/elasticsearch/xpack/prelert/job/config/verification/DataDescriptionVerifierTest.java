
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class DataDescriptionVerifierTest extends ESTestCase {

    public void testVerify_GivenNullTimeFormat() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat(null);

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenEmptyTimeFormat() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsEpoch() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsEpochMs() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch_ms");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsValidDateFormat() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("yyyy-MM-dd HH");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsInvalidDateFormat() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("invalid");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> DataDescriptionVerifier.verify(description));

        assertEquals(ErrorCodes.INVALID_DATE_FORMAT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT, "invalid"), e.getMessage());
    }


    public void testVerify_GivenTimeFormatIsValidButDoesNotContainTime() throws JobConfigurationException {
        DataDescription description = new DataDescription();
        description.setTimeFormat("y-M-dd");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class, () -> DataDescriptionVerifier.verify(description));

        assertEquals(ErrorCodes.INVALID_DATE_FORMAT, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT, "y-M-dd"), e.getMessage());
    }


    public void testVerify_GivenTimeFormatIsInvalidDateParseFormat()
            throws JobConfigurationException {
        String badFormat = "YYY-mm-UU hh:mm:ssY";
        DataDescription dd = new DataDescription();

        dd.setTimeFormat(badFormat);
        try {
            DataDescriptionVerifier.verify(dd);
            // shouldn't get here
            assertTrue("Invalid format should throw", false);
        } catch (JobConfigurationException e) {
        }

        String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
        dd.setTimeFormat(goodFormat);

        assertTrue("Good time format", DataDescriptionVerifier.verify(dd));
    }
}
