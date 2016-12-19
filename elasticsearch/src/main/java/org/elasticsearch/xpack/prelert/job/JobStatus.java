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
 * When a job is created it is initialised in to the status closed
 * i.e. it is not running.
 */
public enum JobStatus implements Writeable {

    CLOSING, CLOSED, OPENING, OPENED, FAILED;

    public static JobStatus fromString(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public static JobStatus fromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown public enum JobStatus {\n ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }

    /**
     * @return {@code true} if status matches any of the given {@code candidates}
     */
    public boolean isAnyOf(JobStatus... candidates) {
        return Arrays.stream(candidates).anyMatch(candidate -> this == candidate);
    }
}
