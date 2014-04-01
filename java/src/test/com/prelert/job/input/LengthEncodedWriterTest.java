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
 ***********************************************************/

package com.prelert.job.input;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class LengthEncodedWriterTest 
{
	/**
	 * Simple test push a list of records through the writer and
	 * check the output
	 * @throws IOException 
	 */
	@Test	
	public void testLengthEncodedWriter() throws IOException
	{
		{
			String [] header = {"one", "two", "three", "four", "five"};
			String [] record = {"r1", "r2", "rr3", "rrr4", "r5"};
		
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			LengthEncodedWriter writer = new LengthEncodedWriter(bos);
			writer.writeRecord(header);
			final int NUM_RECORDS = 5;
			for (int i=0; i<NUM_RECORDS; i++)
			{
				writer.writeRecord(record);
			}

			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

			// read header
			int numFields = bb.getInt();
			Assert.assertEquals(numFields, header.length);		
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(header[i], value);
			}

			// read records
			for (int n=0; n<NUM_RECORDS; n++) 
			{
				numFields = bb.getInt();
				Assert.assertEquals(numFields, record.length);		
				for (int i=0; i<numFields; i++)
				{
					int recordSize = bb.getInt();
					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
					}

					String value = new String(charBuff, StandardCharsets.UTF_8);
					Assert.assertEquals(value, record[i]);
				}
			}
		}
		
		// same again but using lists
		{
			List<String> header = Arrays.asList(new String [] {"one", "two", "three", "four", "five"});
			List<String> record = Arrays.asList(new String [] {"r1", "r2", "rr3", "rrr4", "r5"});
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			LengthEncodedWriter writer = new LengthEncodedWriter(bos);
			writer.writeRecord(header);
			final int NUM_RECORDS = 5;
			for (int i=0; i<NUM_RECORDS; i++)
			{
				writer.writeRecord(record);
			}
			

			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

			// read header
			int numFields = bb.getInt();
			Assert.assertEquals(numFields, header.size());		
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				
				Assert.assertEquals(header.get(i), value);
			}

			// read records
			for (int n=0; n<NUM_RECORDS; n++) 
			{
				numFields = bb.getInt();
				Assert.assertEquals(numFields, record.size());		
				for (int i=0; i<numFields; i++)
				{
					int recordSize = bb.getInt();
					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
	 				}
					
					String value = new String(charBuff, StandardCharsets.UTF_8);
					
					Assert.assertEquals(record.get(i), value);
				}
			}
		}
	}

}
