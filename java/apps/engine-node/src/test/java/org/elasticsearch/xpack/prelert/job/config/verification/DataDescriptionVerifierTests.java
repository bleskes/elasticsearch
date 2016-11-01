/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

public class DataDescriptionVerifierTests extends ESTestCase {

    public void testVerify_GivenNullTimeFormat(){
        DataDescription description = new DataDescription();
        description.setTimeFormat(null);

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenEmptyTimeFormat() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsEpoch() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsEpochMs() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch_ms");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsValidDateFormat() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("yyyy-MM-dd HH");

        assertTrue(DataDescriptionVerifier.verify(description));
    }


    public void testVerify_GivenTimeFormatIsInvalidDateFormat() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("invalid");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> DataDescriptionVerifier.verify(description));

        assertEquals(ErrorCodes.INVALID_DATE_FORMAT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT, "invalid"), e.getMessage());
    }


    public void testVerify_GivenTimeFormatIsValidButDoesNotContainTime() {
        DataDescription description = new DataDescription();
        description.setTimeFormat("y-M-dd");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> DataDescriptionVerifier.verify(description));

        assertEquals(ErrorCodes.INVALID_DATE_FORMAT.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT, "y-M-dd"), e.getMessage());
    }


    public void testVerify_GivenTimeFormatIsInvalidDateParseFormat() {
        String badFormat = "YYY-mm-UU hh:mm:ssY";
        DataDescription dd = new DataDescription();

        dd.setTimeFormat(badFormat);
        try {
            DataDescriptionVerifier.verify(dd);
            // shouldn't get here
            assertTrue("Invalid format should throw", false);
        } catch (ElasticsearchStatusException e) {
        }

        String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
        dd.setTimeFormat(goodFormat);

        assertTrue("Good time format", DataDescriptionVerifier.verify(dd));
    }
}
