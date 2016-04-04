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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the data records to the output stream as length encoded pairs.
 * Each record consists of number of fields followed by length/value
 * pairs. The first call to one the of the <code>writeRecord() </code> methods
 * should be with the header fields, once the headers are written records
 * can be written sequentially.
 *
 * <p>See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
public class LengthEncodedWriter implements RecordWriter
{
    private OutputStream m_OutputStream;
    private ByteBuffer m_LengthBuffer;

    /**
     * Create the writer on the OutputStream <code>os</code>.
     * This object will never close <code>os</code>.
     * @param os
     */
    public LengthEncodedWriter(OutputStream os)
    {
        m_OutputStream = os;
        // This will be used to convert 32 bit integers to network byte order
        m_LengthBuffer = ByteBuffer.allocate(4); // 4 == sizeof(int)
    }


    /**
     * Convert each String in the record array to a length/value encoded pair
     * and write to the outputstream.
     * @param record
     * @throws IOException
     * @see {@link #writeRecord(List)}
     */
    @Override
    public void writeRecord(String[] record)
    throws IOException
    {
        writeNumFields(record.length);

        for (String field : record)
        {
            writeField(field);
        }
    }

    /**
     * Convert each String in the record list to a length/value encoded
     * pair and write to the outputstream.
     *
     * @param record
     * @throws IOException
     * @see {@link #writeRecord(String[])}
     */
    @Override
    public void writeRecord(List<String> record)
    throws IOException
    {
        writeNumFields(record.size());

        for (String field : record)
        {
            writeField(field);
        }
    }


    /**
     * Lower level functions to write records individually.
     * After this function is called {@link #writeField(String)}
     * must be called <code>numFields</code> times.
     *
     * @param numFields
     * @throws IOException
     * @see {@link #writeField(String)}
     */
    public void writeNumFields(int numFields) throws IOException
    {
        // number fields
        m_LengthBuffer.clear();
        m_LengthBuffer.putInt(numFields);
        m_OutputStream.write(m_LengthBuffer.array());
    }


    /**
     * Lower level functions to write record fields individually.
     * {@linkplain #writeNumFields(int)} must be called first
     *
     * @param numFields
     * @throws IOException
     * @see {@link #writeNumFields(int)}
     */
    public void writeField(String field) throws IOException
    {
        byte[] utf8Bytes = field.getBytes(StandardCharsets.UTF_8);
        m_LengthBuffer.clear();
        m_LengthBuffer.putInt(utf8Bytes.length);
        m_OutputStream.write(m_LengthBuffer.array());
        m_OutputStream.write(utf8Bytes);
    }

    @Override
    public void flush() throws IOException
    {
        m_OutputStream.flush();
    }
}
