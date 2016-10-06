
package org.elasticsearch.xpack.prelert.job.process.autodetect.output;

/**
 * Simple class to parse and store a flush ID.
 */
public class FlushAcknowledgement {
    /**
     * Field Names
     */
    public static final String FLUSH = "flush";

    private String id;

    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }
}

