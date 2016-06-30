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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.prelert.job.DataDescription;

public class CsvParserTest {

    /**
     * Test parsing CSV with the NUL character code point (\0 or \u0000)
     * @throws IOException
     */
    @Test
    public void test() throws IOException
    {
        String data = "1422936876.262044869, 1422936876.262044869, 90, 2, 10.132.0.1, 0, 224.0.0.5, 0, 1, 1, 268435460, null, null, null, null, null, null, null, null, null, null, null\n"
                + "1422943772.875342698, 1422943772.875342698, 90, 2, 10.132.0.1, 0, 224.0.0.5, 0, 1, 1, 268435460,,,,,\0,\u0000,,,,,\u0000\n"
                + "\0";
        InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        CsvPreference csvPref = new CsvPreference.Builder(
                DataDescription.DEFAULT_QUOTE_CHAR,
                ',',
                new String(new char[] {DataDescription.LINE_ENDING})).build();

        try (CsvListReader csvReader = new CsvListReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                csvPref))
        {
            String[] header = csvReader.getHeader(true);
            assertEquals(22, header.length);

            List<String> line = csvReader.read();
             assertEquals(22, line.size());

            // last line is \0
            line = csvReader.read();
            assertEquals(1, line.size());
        }
    }
}
