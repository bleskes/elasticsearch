/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job.process.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class CsvRecordWriterTest
{

    @Test
    public void testWriteArray() throws IOException
    {
        String [] header = {"one", "two", "three", "four", "five"};
        String [] record1 = {"r1", "r2", "", "rrr4", "r5"};
        String [] record2 = {"y1", "y2", "yy3", "yyy4", "y5"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        CsvRecordWriter writer = new CsvRecordWriter(bos);
        writer.writeRecord(header);

        // write the same record this number of times
        final int NUM_RECORDS = 1;
        for (int i=0; i<NUM_RECORDS; i++)
        {
            writer.writeRecord(record1);
            writer.writeRecord(record2);
        }
        writer.flush();

        String output = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        String [] lines = output.split("\\r?\\n");
        Assert.assertEquals(1 + NUM_RECORDS * 2 , lines.length);

        String [] fields = lines[0].split(",");
        Assert.assertArrayEquals(fields, header);
        for (int i=1; i<NUM_RECORDS; )
        {
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record1);
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record2);
        }
    }

    @Test
    public void testWriteList() throws IOException
    {
        String [] header = {"one", "two", "three", "four", "five"};
        String [] record1 = {"r1", "r2", "", "rrr4", "r5"};
        String [] record2 = {"y1", "y2", "yy3", "yyy4", "y5"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        CsvRecordWriter writer = new CsvRecordWriter(bos);
        writer.writeRecord(Arrays.asList(header));

        // write the same record this number of times
        final int NUM_RECORDS = 1;
        for (int i=0; i<NUM_RECORDS; i++)
        {
            writer.writeRecord(Arrays.asList(record1));
            writer.writeRecord(Arrays.asList(record2));
        }
        writer.flush();

        String output = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        String [] lines = output.split("\\r?\\n");
        Assert.assertEquals(1 + NUM_RECORDS *2 , lines.length);

        String [] fields = lines[0].split(",");
        Assert.assertArrayEquals(fields, header);
        for (int i=1; i<NUM_RECORDS; )
        {
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record1);
            fields = lines[i++].split(",");
            Assert.assertArrayEquals(fields, record2);
        }
    }

}
