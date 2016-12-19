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
    NUMERICAL_ACTUAL("numerical_actual"),
    NUMERICAL_TYPICAL("numerical_typical"),
    NUMERICAL_DIFF_ABS("numerical_diff_abs");

    private String name;

    private RuleConditionType(String name) {
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
     * @return The condition type
     */
    public static RuleConditionType forString(String value) {
        return RuleConditionType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static RuleConditionType readFromStream(StreamInput in) throws IOException {
        int ordinal = in.readVInt();
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IOException("Unknown RuleConditionType ordinal [" + ordinal + "]");
        }
        return values()[ordinal];
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(ordinal());
    }
}
