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
/**
 * Write the records to the outputIndex stream as UTF 8 encoded CSV
 */
public class CsvRecordWriter implements RecordWriter {
    private final CsvListWriter writer;

    /**
     * Create the writer on the OutputStream <code>os</code>.
     * This object will never close <code>os</code>.
     */
    public CsvRecordWriter(OutputStream os) {
        writer = new CsvListWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), CsvPreference.STANDARD_PREFERENCE);
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
        writer.write(record);
    }

    @Override
    public void writeRecord(List<String> record) throws IOException {
        writer.write(record);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

}
