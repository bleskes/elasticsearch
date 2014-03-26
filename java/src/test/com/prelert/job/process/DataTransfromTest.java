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

package com.prelert.job.process;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import junit.framework.Assert;

import org.junit.Test;

import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;

public class DataTransfromTest 
{
	/**
	 * Test transforming csv data with time in epoch format 
	 */
	@Test
	public void plainCSVToLengthEncoded() throws IOException
	{
		String data = "airline,responsetime,sourcetype,_time\n" +
					"DJA,622,flightcentre,1350824400\n" +
					"JQA,1742,flightcentre,1350824401\n" +
					"GAL,5339,flightcentre,1350824402\n" +
					"GAL,3893,flightcentre,1350824403\n" +
					"JQA,9,flightcentre,1350824403\n" +
					"DJA,189,flightcentre,1350824404\n" +
					"JQA,8,flightcentre,1350824404\n" +
					"DJA,1200,flightcentre,1350824404";
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(",");
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		String [] lines = data.split("\\n");
		
		for (String line : lines)
		{
			int numFields = bb.getInt();
			String [] fields = line.split(",");
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[i].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);				
				Assert.assertEquals(fields[i], value);
				
			}
		}
	}
	
	/**
	 * Test transforming csv data with time in epoch format 
	 * and a non-standard quote character
	 */
	@Test
	public void quotedCSVToLengthEncoded() throws IOException
	{
		// ? is the quote char
		String data = "airline,responsetime,sourcetype,_time\n" +
					"DJA,622,?flight,centre?,1350824400\n" +
					"JQA,1742,?flightcentre?,1350824401\n" +
					"GAL,5339,?flight\ncentre?,1350824402\n" +
					"GAL,3893,flightcentre,1350824403";
		
		// same as the data above but split into fields
		// this is to test escaping the newline char in the quoted field
		String [][] lines = new String [] [] {{"airline","responsetime","sourcetype","_time"},
				{"DJA","622","flight,centre","1350824400"}, 
				{"JQA","1742","flightcentre","1350824401"},
				{"GAL","5339","flight\ncentre","1350824402"}, 
				{"GAL","3893","flightcentre","1350824403"}};
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(",");
		dd.setQuoteCharacter('?');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		for (int l=0; l<lines.length; l++)
		{
			String [] fields = lines[l];
			int numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[i].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[i], value);
				
			}
		}
	}	
	
	/**
	 * Test transforming csv data with a time format
	 */
	@Test
	public void csvWithDateFormat() throws IOException
	{
		// ? is the quote char
		String data = "date,airline,responsetime,sourcetype\n" +
					"2014-01-28 00:00:00,AAL,132.2046,farequote\n" +
					"2014-01-28 00:00:00,JZA,990.4628,farequote\n" +
					"2014-01-28 00:00:00,JBU,877.5927,farequote\n" +
					"2014-01-28 00:00:00,KLM,1355.4812,farequote\n" +
					"2014-01-28 01:00:00,NKS,9991.3981,farequote\n" +
					"2014-01-28 01:00:00,TRS,412.1948,farequote\n" +
					"2014-01-28 01:00:00,DAL,401.4948,farequote\n" +
					"2014-01-28 01:00:00,FFT,181.5529,farequote";
		
		String [] epochTimes = new String [] {"", "1390867200", "1390867200", 
				"1390867200", "1390867200", "1390870800", "1390870800", 
				"1390870800", "1390870800"};
				
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(",");
		dd.setTimeField("date");
		dd.setTimeFormat("yyyy-MM-dd HH:mm:ss");

		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		String [] lines = data.split("\\n");
		
		boolean isHeader = true;
		for (int currentLine=0; currentLine<lines.length; currentLine++)
		{
			String [] fields = lines[currentLine].split(",");
			int numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);			
			
			final int DATE_FIELD = 0;
			for (int i=0; i<numFields; i++)
			{				
				int recordSize = bb.getInt();

				if (isHeader == false && i == DATE_FIELD)
				{
					Assert.assertEquals(epochTimes[currentLine].length(), recordSize);

					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
					}
					
					String value = new String(charBuff, StandardCharsets.UTF_8);
					Assert.assertEquals(epochTimes[currentLine], value);	
				}
				else
				{
					Assert.assertEquals(fields[i].length(), recordSize);

					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
					}
					
					String value = new String(charBuff, StandardCharsets.UTF_8);
					Assert.assertEquals(fields[i], value);					
				}
			}

			isHeader = false;
		}
	}	
	
	
	/**
	 * Test transforming JSON without a time format to length encoded.
	 *  
	 * @throws IOException
	 */
	@Test
	public void plainJsonToLengthEncoded() throws IOException
	{
		String data = "{\"timestamp\": \"1350824400\", \"airline\": \"DJA\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824401\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824402\", \"airline\": \"GAL\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824403\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824403\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 
				
		String header [] = new String [] {"timestamp", "airline", "responsetime", "sourcetype"};
		String records [][] = new String [][] {{"1350824400", "DJA", "622", "flightcentre"},
												{"1350824401", "JQA", "1742", "flightcentre"},
												{"1350824402", "GAL", "5339", "flightcentre"},
												{"1350824403", "GAL", "3893", "flightcentre"},
												{"1350824403", "JQA", "9", "flightcentre"},
												{"1350824404", "DJA", "189", "flightcentre"},
												{"1350824404", "JQA", "8", "flightcentre"},
												{"1350824404", "DJA", "1200", "flightcentre"}}; 
				
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		
		// check header
		int numFields = bb.getInt();		
		Assert.assertEquals(header.length, numFields);
		
		for (int i=0; i<numFields; i++)
		{
			int recordSize = bb.getInt();
			Assert.assertEquals(header[i].length(), recordSize);
			byte [] charBuff = new byte[recordSize];
			for (int j=0; j<recordSize; j++)
			{
				charBuff[j] = bb.get();
			}
			
			String value = new String(charBuff, StandardCharsets.UTF_8);				
			Assert.assertEquals(header[i], value);			
		}
		
		
		// check records
		for (String [] fields : records)
		{
			numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[i].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);					
				Assert.assertEquals(fields[i], value);
			}
		}
	}

	
	/**
	 * Test transforming JSON without a time format to length encoded.
	 *  
	 * @throws IOException
	 */
	@Test
	public void jsonWithDateFormatToLengthEncoded() throws IOException
	{
		String data = "{\"timestamp\": \"2012-10-21T14:00:00\", \"airline\": \"DJA\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:01\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:02\", \"airline\": \"GAL\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 
				
		String header [] = new String [] {"timestamp", "airline", "responsetime", "sourcetype"};
		String records [][] = new String [][] {{"1350824400", "DJA", "622", "flightcentre"},
												{"1350824401", "JQA", "1742", "flightcentre"},
												{"1350824402", "GAL", "5339", "flightcentre"},
												{"1350824403", "GAL", "3893", "flightcentre"},
												{"1350824403", "JQA", "9", "flightcentre"},
												{"1350824404", "DJA", "189", "flightcentre"},
												{"1350824404", "JQA", "8", "flightcentre"},
												{"1350824404", "DJA", "1200", "flightcentre"}}; 
				
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		
		// check header
		int numFields = bb.getInt();		
		Assert.assertEquals(header.length, numFields);
		
		for (int i=0; i<numFields; i++)
		{
			int recordSize = bb.getInt();
			Assert.assertEquals(header[i].length(), recordSize);
			byte [] charBuff = new byte[recordSize];
			for (int j=0; j<recordSize; j++)
			{
				charBuff[j] = bb.get();
			}
			
			String value = new String(charBuff, StandardCharsets.UTF_8);				
			Assert.assertEquals(header[i], value);			
		}
		
		
		// check records
		for (String [] fields : records)
		{
			numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[i].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[i], value);
			}
		}
	}

}
