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
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.test.ESTestCase;


public class ElasticsearchJobIdTests extends ESTestCase {
    public void testIdAndIndex() {
        ElasticsearchJobId foo = new ElasticsearchJobId("foo");
        assertEquals("foo", foo.getId());
        assertEquals("prelertresults-foo", foo.getIndex());

        ElasticsearchJobId bar = new ElasticsearchJobId("bar");
        assertEquals("bar", bar.getId());
        assertEquals("prelertresults-bar", bar.getIndex());
    }
}
