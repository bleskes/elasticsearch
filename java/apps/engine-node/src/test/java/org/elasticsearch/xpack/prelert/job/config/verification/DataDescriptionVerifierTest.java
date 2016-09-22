
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class DataDescriptionVerifierTest extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


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
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage("Invalid Time format string 'invalid'");

        DataDescription description = new DataDescription();
        description.setTimeFormat("invalid");

        DataDescriptionVerifier.verify(description);
    }


    public void testVerify_GivenTimeFormatIsValidButDoesNotContainTime() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage("Invalid Time format string 'y-M-dd'");

        DataDescription description = new DataDescription();
        description.setTimeFormat("y-M-dd");

        DataDescriptionVerifier.verify(description);
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
