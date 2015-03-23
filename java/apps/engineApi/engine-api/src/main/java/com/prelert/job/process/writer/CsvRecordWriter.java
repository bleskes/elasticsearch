package com.prelert.job.process.writer;

import java.io.IOException;
/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Write the records to the output stream as UTF 8 encoded CSV
 */
public class CsvRecordWriter implements RecordWriter
{
    private OutputStream m_OutputStream;

    /**
     * Create the writer on the OutputStream <code>os</code>.
     * This object will never close <code>os</code>.
     * @param os
     */
    public CsvRecordWriter(OutputStream os)
    {
        m_OutputStream = os;
    }

    @Override
    public void writeRecord(String[] record) throws IOException
    {
        for (int i=0; i<record.length -1; i++)
        {
            m_OutputStream.write(record[i].getBytes(StandardCharsets.UTF_8));
            m_OutputStream.write(',');
        }
        m_OutputStream.write(record[record.length -1].getBytes(StandardCharsets.UTF_8));
        m_OutputStream.write('\n');
    }

    @Override
    public void writeRecord(List<String> record) throws IOException
    {
        for (int i=0; i<record.size() -1; i++)
        {
            m_OutputStream.write(record.get(i).getBytes(StandardCharsets.UTF_8));
            m_OutputStream.write(',');
        }
        m_OutputStream.write(record.get(record.size()-1).getBytes(StandardCharsets.UTF_8));
        m_OutputStream.write('\n');
    }

    @Override
    public void flush() throws IOException
    {
        m_OutputStream.flush();
    }

}
