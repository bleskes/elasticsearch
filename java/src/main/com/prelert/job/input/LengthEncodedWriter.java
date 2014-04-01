/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.job.input;

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
public class LengthEncodedWriter 
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
	public void writeRecord(String [] record)
	throws IOException
	{		
		byte[] utf8Bytes;
		
		// number fields
		m_LengthBuffer.clear(); 
		m_LengthBuffer.putInt(record.length);
		m_OutputStream.write(m_LengthBuffer.array());
		
		for (String fieldname : record)
		{
			utf8Bytes = fieldname.getBytes(StandardCharsets.UTF_8);
			m_LengthBuffer.clear();			
			m_LengthBuffer.putInt(utf8Bytes.length);
			m_OutputStream.write(m_LengthBuffer.array());
			m_OutputStream.write(utf8Bytes);			
		}		
	}
	
	/**
	 * Convert each String in the record list to a length/value encoded pair
	 * and write to the outputstream. 
	 * @param record
	 * @throws IOException
	 * @see {@link #writeRecord(String [])}
	 */
	public void writeRecord(List<String> record)
	throws IOException
	{		
		byte[] utf8Bytes;
		
		// number fields
		m_LengthBuffer.clear(); 
		m_LengthBuffer.putInt(record.size());
		m_OutputStream.write(m_LengthBuffer.array());
		
		for (String fieldname : record)
		{
			utf8Bytes = fieldname.getBytes(StandardCharsets.UTF_8);
			m_LengthBuffer.clear();			
			m_LengthBuffer.putInt(utf8Bytes.length);
			m_OutputStream.write(m_LengthBuffer.array());
			m_OutputStream.write(utf8Bytes);			
		}		
	}
	
	/**
	 * Flush the output stream.
	 * @throws IOException
	 */
	public void flush() throws IOException
	{
		m_OutputStream.flush();
	}
}
