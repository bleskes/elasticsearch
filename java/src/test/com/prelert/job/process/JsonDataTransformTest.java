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

import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.warnings.DummyStatusReporter;
import com.prelert.job.warnings.HighProportionOfBadTimestampsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;

public class JsonDataTransformTest 
{
	static private Logger s_Logger = Logger.getLogger(JsonDataTransformTest.class);
	
	
	/**
	 * Test transforming JSON without a time format to length encoded
	 * with the extra fields not used in the analysis filtered out
	 *  
	 * @throws IOException
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void plainJsonToLengthEncoded() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.sumTotalRecords());
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
				byte [] charBuff = new byte[recordSize];
				for (int j=0; j<recordSize; j++)
				{
					charBuff[j] = bb.get();
 				}
				
				String value = new String(charBuff, StandardCharsets.UTF_8);

				Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
				Assert.assertEquals(fields[fieldMap[i]], value);
			}
		}
	}

	
	/**
	 * Test transforming JSON with a time format to length encoded.
	 *  
	 * @throws IOException
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void jsonWithDateFormatToLengthEncoded() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		// The json docs are have different field orders
		String data = "{\"airline\": \"DJA\", \"timestamp\": \"2012-10-21T14:00:00\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:01\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:02\", \"airline\": \"GAL\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"responsetime\": \"3893\", \"airline\": \"GAL\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
					"{\"sourcetype\": \"flightcentre\", \"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"responsetime\": \"189\" }" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
					"{\"airline\": \"DJA\", \"timestamp\": \"2012-10-21T14:00:04\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 
				
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.sumTotalRecords());
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void jsonWithDateFormatAndExtraFieldsToLengthEncoded() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		// Document fields are not in the same order
		String data = "{\"extra_field\": \"extra\", \"timestamp\": \"2012-10-21T14:00:00\", \"airline\": \"DJA\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"timestamp\": \"2012-10-21T14:00:01\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:02\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:03\", \"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"extra_field\": \"extra\", \"responsetime\": \"189\", \"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\"}" +
					"{\"airline\": \"JQA\", \"timestamp\": \"2012-10-21T14:00:04\", \"extra_field\": \"extra\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\", \"timestamp\": \"2012-10-21T14:00:04\", \"extra_field\": \"extra\", \"airline\": \"DJA\", \"responsetime\": \"1200\"}"; 
				
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.sumTotalRecords());
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
	 * In this test the input JSON documents have their fields in
	 * an inconsistent changing order. 
	 * 
	 * @throws IOException
	 * @throws MissingFieldException
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void differentFieldsOrderJsonToLengthEncoded() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		String data = "{\"timestamp\": \"1350824400\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
					"{\"junk_field\": \"nonsense\", \"airline\": \"JQA\", \"timestamp\": \"1350824401\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824402\", \"responsetime\": \"5339\", \"airline\": \"GAL\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
					"{\"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\", \"timestamp\": \"1350824403\"}" +
					"{\"airline\": \"JQA\", \"timestamp\": \"1350824403\", \"junk_field\": \"nonsense\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
					"{\"timestamp\": \"1350824404\", \"junk_field\": \"nonsense\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\", \"airline\": \"DJA\"}" +
					"{\"responsetime\": \"8\", \"timestamp\": \"1350824404\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\"}" +
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.sumTotalRecords());
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
	 * JSON documents have missing fields. Test conversion is robust and 
	 * passes records with empty strings for the missing fields.
	 *  
	 * @throws IOException
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void jsonMissingFieldsToLengthEncoded() 
			throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
			OutOfOrderRecordsException
			{
		// Document fields are not in the same order
		String dateFormatData = "{\"timestamp\": \"2012-10-21T14:00:00\", \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:01\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:02\", \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:03\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:03\", 				         \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": \"2012-10-21T14:00:04\", \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 

		String epochFormatData = "{\"timestamp\": 1350824400, \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
				"{\"timestamp\": 1350824401, \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824402, \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824403, \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824403, 				         \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404, \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404, \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404, \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 	

		String epochMsFormatData = "{\"timestamp\": 1350824400000, \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
				"{\"timestamp\": 1350824401000, \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824402000, \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824403000, \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824403000, 				         \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404500, \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404400, \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
				"{\"timestamp\": 1350824404200, \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}"; 	


		String header [] = new String [] {"timestamp", "airline", "responsetime", "sourcetype"};
		String records [][] = new String [][] {{"1350824400", "DJA", "622", ""},
				{"1350824401", "JQA", "1742", "flightcentre"},
				{"1350824402", "GAL", "", "flightcentre"},
				{"1350824403", "GAL", "3893", "flightcentre"},
				{"1350824403", "", "9", "flightcentre"},
				{"1350824404", "DJA", "189", "flightcentre"},
				{"1350824404", "JQA", "8", "flightcentre"},
				{"1350824404", "DJA", "1200", "flightcentre"}}; 

		List<String> analysisFields = Arrays.asList(new String [] {
				"responsetime", "sourcetype", "airline"});		

		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {2, 3, 1, 0};		



		DataDescription dateFormatDD = new DataDescription();
		dateFormatDD.setFormat(DataFormat.JSON);
		dateFormatDD.setTimeField("timestamp");
		dateFormatDD.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss");

		DataDescription epochFormatDD = new DataDescription();
		epochFormatDD.setFormat(DataFormat.JSON);
		epochFormatDD.setTimeField("timestamp");

		DataDescription epochMsFormatDD = new DataDescription();
		epochMsFormatDD.setFormat(DataFormat.JSON);
		epochMsFormatDD.setTimeField("timestamp");
		epochMsFormatDD.setTimeFormat("epoch_ms");

		DataDescription [] dds = new DataDescription [] {dateFormatDD, epochFormatDD,
				epochMsFormatDD};

		// can create with null
		ProcessManager pm = new ProcessManager(null, null, null);

		int count = 0;
		for (String data : new String [] {dateFormatData, epochFormatData, epochMsFormatData})
		{
			ByteArrayInputStream bis = 
					new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			DummyStatusReporter reporter = new DummyStatusReporter();

			DataDescription dd = dds[count++];

			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

			Assert.assertEquals(8, reporter.sumTotalRecords());
			Assert.assertEquals(8, reporter.getRecordsWrittenCount());
			Assert.assertEquals(3, reporter.getMissingFieldErrorCount());
			Assert.assertEquals(0, reporter.getDateParseErrorsCount());
			Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());

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
	

	/**
	 * JSON docs can contain nested documents. Test they are parsed properly
	 * 
	 * @throws IOException 
	 * @throws MissingFieldException 
	 * @throws JsonParseException 
	 * @throws OutOfOrderRecordsException 
	 * @throws HighProportionOfBadTimestampsException 
	 */
	@Test
	public void nestedObjectTest() throws JsonParseException,
		MissingFieldException, IOException, HighProportionOfBadTimestampsException, 
		OutOfOrderRecordsException
	{
		String epochData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":1350824402,\"value\":12345.678}";
		
		String timeFormatData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":\"2012-10-21T14:00:00\",\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":\"2012-10-21T14:00:01\",\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":\"2012-10-21T14:00:02\",\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":\"2012-10-21T14:00:02\",\"value\":12345.678}";
		
		String epochMsData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400000,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401000,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402000,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":1350824402000,\"value\":12345.678}";
		
		String header [] = new String [] {"time", "name", "value", "tags.tag2"};
		String records [][] = new String [][] {{"1350824400", "my.test.metric1", "12345.678", "boo"},
												{"1350824401", "my.test.metric2", "12345.678", "booo"},
												{"1350824402", "my.test.metric3", "12345.678", "boooo"},
												{"1350824402", "my.test.metric4", "12345.678", ""}};
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {1, 2, 3, 0};
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("time");
		dd.setTimeFormat("epoch");
		
		List<String> analysisFields = Arrays.asList(new String [] {"name", "value", "tags.tag2"});	
		
		int loop = 0;
		for (String data : new String [] {epochData, timeFormatData, epochMsData})
		{
			if (loop == 1)
			{
				dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
			}
			else if (loop == 2)
			{
				dd.setTimeFormat("epoch_ms");
			}
			loop++;						

			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			
			DummyStatusReporter reporter = new DummyStatusReporter();
			
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
			
			
			Assert.assertEquals(4, reporter.sumTotalRecords());
			Assert.assertEquals(4, reporter.getRecordsWrittenCount());
			Assert.assertEquals(1, reporter.getMissingFieldErrorCount());
			Assert.assertEquals(0, reporter.getDateParseErrorsCount());
			Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());

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
	
	/**
	 * Test deeply nested docs
	 * @throws OutOfOrderRecordsException 
	 * @throws HighProportionOfBadTimestampsException 
	 */
	@Test
	public void moreNestedFieldsTest() 
	throws JsonParseException, MissingFieldException, IOException,
		HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		String data = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"boo\"},\"time\":1350824400,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"booo\"},\"time\":1350824401,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"boooo\"},\"time\":1350824402,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":1350824402,\"value\":12345.678}";
		
		
		String header [] = new String [] {"time", "name", "value", "tags.tag2", "tags.tag1.key1"};
		String records [][] = new String [][] {{"1350824400", "my.test.metric1", "12345.678", "boo", "value1"},
												{"1350824401", "my.test.metric2", "12345.678", "booo", "value1"},
												{"1350824402", "my.test.metric3", "12345.678", "boooo",  "value1"},
												{"1350824402", "my.test.metric4", "12345.678", "", ""}};
		
		List<String> analysisFields = Arrays.asList(new String [] {"name", "value", "tags.tag2", "tags.tag1.key1"});	
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {1, 2, 3, 4, 0};
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("time");
		dd.setTimeFormat("epoch");
		
		// can create with null
		ProcessManager pm = new ProcessManager(null, null, null);

		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(4, reporter.sumTotalRecords());
		Assert.assertEquals(4, reporter.getRecordsWrittenCount());
		Assert.assertEquals(1, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());

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
	 * Test converting epoch times with a fraction of a second component.
	 * 
	 * @throws JsonParseException
	 * @throws MissingFieldException
	 * @throws IOException
	 * @throws OutOfOrderRecordsException 
	 * @throws HighProportionOfBadTimestampsException 
	 */
	@Test
	public void epochWithFractionTest() throws JsonParseException,
		MissingFieldException, IOException,
		HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		String epochData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400.4543154,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401.834431,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402.4864133,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":1350824402.4846513,\"value\":12345.678}";
	
		String epochMsData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400000.484313,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401000.486138,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402000.4831844,\"value\":12345.678}"
				+ "{\"name\":\"my.test.metric4\",\"time\":1350824402000.45633447,\"value\":12345.678}";
		
		String header [] = new String [] {"time", "name", "value", "tags.tag2"};
		String records [][] = new String [][] {{"1350824400", "my.test.metric1", "12345.678", "boo"},
												{"1350824401", "my.test.metric2", "12345.678", "booo"},
												{"1350824402", "my.test.metric3", "12345.678", "boooo"},
												{"1350824402", "my.test.metric4", "12345.678", ""}};
		
		// data is written in the order of the required fields
		// then the time field
		int [] fieldMap = new int [] {1, 2, 3, 0};
		
		DataDescription dd = new DataDescription();
		dd.setFormat(DataFormat.JSON);
		dd.setTimeField("time");
		dd.setTimeFormat("epoch");
		
		List<String> analysisFields = Arrays.asList(new String [] {"name", "value", "tags.tag2"});	
		
		int loop = 0;
		for (String data : new String [] {epochData,  epochMsData})
		{
			if (loop == 0)
			{
				dd.setTimeFormat("epoch");
			}
			else if (loop == 1)
			{
				dd.setTimeFormat("epoch_ms");
			}
			loop++;						

			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			DummyStatusReporter reporter = new DummyStatusReporter();
			
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
			
			
			Assert.assertEquals(4, reporter.sumTotalRecords());
			Assert.assertEquals(4, reporter.getRecordsWrittenCount());
			Assert.assertEquals(1, reporter.getMissingFieldErrorCount());
			Assert.assertEquals(0, reporter.getDateParseErrorsCount());
			Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());

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
					byte [] charBuff = new byte[recordSize];
					for (int j=0; j<recordSize; j++)
					{
						charBuff[j] = bb.get();
					}

					String value = new String(charBuff, StandardCharsets.UTF_8);
					//Assert.assertEquals(fields[fieldMap[i]].length(), recordSize);
					Assert.assertEquals(fields[fieldMap[i]], value);
				}
			}		
		}
	}
}
