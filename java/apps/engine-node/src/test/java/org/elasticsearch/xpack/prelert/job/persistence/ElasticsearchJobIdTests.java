
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
