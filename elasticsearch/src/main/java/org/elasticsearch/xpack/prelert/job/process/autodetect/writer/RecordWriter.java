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
     * code.
     */
    String CONTROL_FIELD_NAME = ".";

    /**
     * Write each String in the record array
     */
    void writeRecord(String[] record) throws IOException;

    /**
     * Write each String in the record list
     */
    void writeRecord(List<String> record) throws IOException;

    /**
     * Flush the outputIndex stream.
     */
    void flush() throws IOException;

}