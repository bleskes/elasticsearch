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

    private String name;

    private Connective(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Case-insensitive from string method.
     *
     * @param value
     *            String representation
     * @return The connective type
     */
    public static Connective fromString(String value) {
        return Connective.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static Connective readFromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown Connective ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
