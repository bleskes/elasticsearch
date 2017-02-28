/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.messages;

import org.elasticsearch.test.ESTestCase;

public class MessagesTests extends ESTestCase {

    public void testGetMessage_NoFormatArgs () {
        assertEquals(Messages.DATAFEED_CONFIG_CANNOT_USE_SCRIPT_FIELDS_WITH_AGGS,
                Messages.getMessage(Messages.DATAFEED_CONFIG_CANNOT_USE_SCRIPT_FIELDS_WITH_AGGS));
    }

    public void testGetMessage_WithFormatStrings()  {
        String formattedMessage = Messages.getMessage(Messages.DATAFEED_CONFIG_INVALID_OPTION_VALUE, "field-name", "field-value");
        assertEquals("Invalid field-name value 'field-value' in datafeed configuration", formattedMessage);

        formattedMessage = Messages.getMessage(Messages.JOB_AUDIT_SNAPSHOT_DELETED, "foo-job");
        assertEquals("Job model snapshot 'foo-job' deleted", formattedMessage);
    }
}
