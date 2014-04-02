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
import java.util.Arrays;
import java.util.List;

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
	public void plainCSVToLengthEncoded() 
	throws IOException, MissingFieldException
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
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});
		
		// data is written in the order of the required fields
		// with time the first element not the same as it is input
		int [] fieldMap = new int [] {3, 1, 2, 0};
		
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
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
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);				
				Assert.assertEquals(fields[fieldMap[i]], value);
				
			}
		}
	}
	
	/**
	 * Test transforming csv data with time in epoch format 
	 * and a non-standard quote character
	 */
	@Test
	public void quotedCSVToLengthEncoded() throws IOException, MissingFieldException
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
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"airline", "responsetime", "sourcetype"});
		
		// data is written in the order of the required fields
		// with time the first element not the same as it is input
		int [] fieldMap = new int [] {3, 0, 1, 2};
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		dd.setQuoteCharacter('?');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		for (int l=0; l<lines.length; l++)
		{
			String [] fields = lines[l];
			int numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[fieldMap[i]], value);
				
			}
		}
	}	
	
	/**
	 * Test transforming csv data with a time format
	 */
	@Test
	public void csvWithDateFormat() throws IOException, MissingFieldException
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
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "airline", "sourcetype"});
		
		// data is written in the order of the required fields
		// with time the first element not the same as it is input
		int [] fieldMap = new int [] {0, 2, 1, 3};
		
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		dd.setTimeField("date");
		dd.setTimeFormat("yyyy-MM-dd HH:mm:ss");

		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
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
					Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);

					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
					}
					
					String value = new String(charBuff, StandardCharsets.UTF_8);
					Assert.assertEquals(fields[fieldMap[i]], value);					
				}
			}

			isHeader = false;
		}
	}	
	
	/**
	 * Write CSV data with extra fields that should be filtered out
	 * 
	 * @throws IOException
	 * @throws MissingFieldException
	 */
	@Test
	public void plainCsvWithExtraFields() throws IOException, MissingFieldException
	{
		String data = "airline,responsetime,airport,sourcetype,_time,baggage\n" +
					"DJA,622,flightcentre,MAN,1350824400,none\n" +
					"JQA,1742,flightcentre,GAT,1350824401,none\n" +
					"GAL,5339,flightcentre,SYN,1350824402,some\n" +
					"GAL,3893,flightcentre,CHM,1350824403,some\n" +
					"JQA,9,flightcentre,CHM,1350824403,none\n" +
					"DJA,189,flightcentre,GAT,1350824404,lost\n" +
					"JQA,8,flightcentre,GAT,1350824404,none\n" +
					"DJA,1200,flightcentre,MAN,1350824404,none";		
		
		// empty strings and null should be ignored.
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});
		
		// data is written in the order of the required fields
		// with time the first element not the same as it is input
		int [] fieldMap = new int [] {4, 1, 3, 0};
		
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		String [] lines = data.split("\\n");
		
		for (String line : lines)
		{
			int numFields = bb.getInt();
			String [] fields = line.split(",");
			Assert.assertEquals(analysisFields.size() +1, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);				
				Assert.assertEquals(fields[fieldMap[i]], value);
			}
		}		
	}
	
	/**
	 * Write CSV data with the time field missing this should throw
	 * a MissingFieldException 
	 * 
	 * @throws IOException
	 */
	@Test 
	public void plainCsvWithMissingTimeField()
	throws IOException
	{
		// no time field
		String data = "airline,responsetime,airport,sourcetype,baggage\n" +
					"DJA,622,flightcentre,MAN,none\n" +
					"JQA,1742,flightcentre,GAT,none\n" +
					"GAL,5339,flightcentre,SYN,some\n" +
					"GAL,3893,flightcentre,CHM,some\n" +
					"JQA,9,flightcentre,CHM,none\n" +
					"DJA,189,flightcentre,GAT,lost\n" +
					"JQA,8,flightcentre,GAT,none\n" +
					"DJA,1200,flightcentre,MAN,none";		
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			pm.writeToJob(dd, analysisFields, bis, bos);
			Assert.assertTrue(false); // should throw
		} 
		catch (MissingFieldException e)
		{
			Assert.assertEquals(e.getMissingFieldName(), "_time");
		}
		
		// Do the same again but with a time format configured
		// so a different code path will be taken
		// 
		dd.setTimeField("timestamp");
		dd.setTimeFormat(DataDescription.EPOCH_MS);
		analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});
		
		bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			pm.writeToJob(dd, analysisFields, bis, bos);
			Assert.assertTrue(false); // should throw
		} 
		catch (MissingFieldException e)
		{
			Assert.assertEquals(e.getMissingFieldName(), "timestamp");
		}		
	}
	
	
	
	/**
	 * Write CSV data with an analysis field missing this should throw
	 * a MissingFieldException 
	 * 
	 * @throws IOException
	 */
	@Test 
	public void plainCsvWithMissingField()
	throws IOException
	{
		String data = "airline,responsetime,airport,sourcetype,_time,baggage\n" +
				"DJA,622,flightcentre,MAN,1350824400,none\n" +
				"JQA,1742,flightcentre,GAT,1350824401,none\n" +
				"GAL,5339,flightcentre,SYN,1350824402,some\n" +
				"GAL,3893,flightcentre,CHM,1350824403,some\n" +
				"JQA,9,flightcentre,CHM,1350824403,none\n" +
				"DJA,189,flightcentre,GAT,1350824404,lost\n" +
				"JQA,8,flightcentre,GAT,1350824404,none\n" +
				"DJA,1200,flightcentre,MAN,1350824404,none";			
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline", "missing_field"});
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.DELINEATED);
		dd.setFieldDelimiter(',');
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			pm.writeToJob(dd, analysisFields, bis, bos);
			Assert.assertTrue(false); // should throw
		} 
		catch (MissingFieldException e)
		{
			Assert.assertEquals(e.getMissingFieldName(), "missing_field");
		}
		
		// Do the same again but with a time format configured
		// so a different code path will be taken
		// 
		dd.setTimeFormat(DataDescription.EPOCH_MS);
		
		bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			pm.writeToJob(dd, analysisFields, bis, bos);
			Assert.assertTrue(false); // should throw
		} 
		catch (MissingFieldException e)
		{
			Assert.assertEquals(e.getMissingFieldName(), "missing_field");
		}
	}
	
	
	/**
	 * Test transforming JSON without a time format to length encoded
	 * with the extra fields not used in the analysis filtered out
	 *  
	 * @throws IOException
	 */
	@Test
	public void plainJsonToLengthEncoded() 
	throws IOException, MissingFieldException
	{
		String data = "{\"timestamp\": \"1350824400\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824401\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824402\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824403\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824403\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 
				
		String header [] = new String [] {"timestamp", "airline", "responsetime", "sourcetype"};
		String records [][] = new String [][] {{"1350824400", "DJA", "622", "flightcentre"},
												{"1350824401", "JQA", "1742", "flightcentre"},
												{"1350824402", "GAL", "5339", "flightcentre"},
												{"1350824403", "GAL", "3893", "flightcentre"},
												{"1350824403", "JQA", "9", "flightcentre"},
												{"1350824404", "DJA", "189", "flightcentre"},
												{"1350824404", "JQA", "8", "flightcentre"},
												{"1350824404", "DJA", "1200", "flightcentre"}}; 
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {3, 1, 2, 0};		
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"sourcetype", "airline", "responsetime"});
				
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		
		// check header
		int numFields = bb.getInt();		
		Assert.assertEquals(header.length, numFields);
		
		for (int i=0; i<numFields; i++)
		{
			int recordSize = bb.getInt();
			Assert.assertEquals(header[fieldMap[i]].length(), recordSize);
			byte [] charBuff = new byte[recordSize];
			for (int j=0; j<recordSize; j++)
			{
				charBuff[j] = bb.get();
			}
			
			String value = new String(charBuff, StandardCharsets.UTF_8);				
			Assert.assertEquals(header[fieldMap[i]], value);			
		}
		
		
		// check records
		for (String [] fields : records)
		{
			numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[fieldMap[i]], value);
			}
		}
	}

	
	/**
	 * Test transforming JSON with a time format to length encoded.
	 *  
	 * @throws IOException
	 */
	@Test
	public void jsonWithDateFormatToLengthEncoded() 
	throws IOException, MissingFieldException
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
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"airline", "responsetime", "sourcetype"});		
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {1, 2, 3, 0};		
				
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		
		// check header
		int numFields = bb.getInt();		
		Assert.assertEquals(header.length, numFields);
		
		for (int i=0; i<numFields; i++)
		{
			int recordSize = bb.getInt();
			Assert.assertEquals(header[fieldMap[i]].length(), recordSize);
			byte [] charBuff = new byte[recordSize];
			for (int j=0; j<recordSize; j++)
			{
				charBuff[j] = bb.get();
			}
			
			String value = new String(charBuff, StandardCharsets.UTF_8);				
			Assert.assertEquals(header[fieldMap[i]], value);			
		}
		
		
		// check records
		for (String [] fields : records)
		{
			numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[fieldMap[i]], value);
			}
		}
	}
	
	
	/**
	 * Test transforming JSON with a time format 
	 * and extra fields to length encoded.
	 *  
	 * @throws IOException
	 */
	@Test
	public void jsonWithDateFormatAndExtraFieldsToLengthEncoded() 
	throws IOException, MissingFieldException
	{
		String data = "{\"timestamp\": \"2012-10-21T14:00:00\", \"extra_field\": \"extra\", \"airline\": \"DJA\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:01\", \"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:02\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"extra_field\": \"extra\", \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"extra_field\": \"extra\", \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}"; 
				
		String header [] = new String [] {"timestamp", "airline", "responsetime", "sourcetype"};
		String records [][] = new String [][] {{"1350824400", "DJA", "622", "flightcentre"},
												{"1350824401", "JQA", "1742", "flightcentre"},
												{"1350824402", "GAL", "5339", "flightcentre"},
												{"1350824403", "GAL", "3893", "flightcentre"},
												{"1350824403", "JQA", "9", "flightcentre"},
												{"1350824404", "DJA", "189", "flightcentre"},
												{"1350824404", "JQA", "8", "flightcentre"},
												{"1350824404", "DJA", "1200", "flightcentre"}}; 
		
		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});		
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {2, 3, 1, 0};		
				
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("timestamp");
		dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		pm.writeToJob(dd, analysisFields, bis, bos);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		
		// check header
		int numFields = bb.getInt();		
		Assert.assertEquals(header.length, numFields);
		
		for (int i=0; i<numFields; i++)
		{
			int recordSize = bb.getInt();
			Assert.assertEquals(header[fieldMap[i]].length(), recordSize);
			byte [] charBuff = new byte[recordSize];
			for (int j=0; j<recordSize; j++)
			{
				charBuff[j] = bb.get();
			}
			
			String value = new String(charBuff, StandardCharsets.UTF_8);				
			Assert.assertEquals(header[fieldMap[i]], value);			
		}
		
		
		// check records
		for (String [] fields : records)
		{
			numFields = bb.getInt();
			Assert.assertEquals(fields.length, numFields);
			
			for (int i=0; i<numFields; i++)
			{
				int recordSize = bb.getInt();
				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);
				Assert.assertEquals(fields[fieldMap[i]], value);
			}
		}
	}

}
