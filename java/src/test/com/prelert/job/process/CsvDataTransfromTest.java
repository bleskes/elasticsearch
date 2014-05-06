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

import org.apache.log4j.Logger;
import org.junit.Test;

import junit.framework.Assert;

import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.warnings.DummyStatusReporter;
import com.prelert.job.warnings.HighProportionOfBadTimestampsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;

public class CsvDataTransfromTest 
{
	static private Logger s_Logger = Logger.getLogger(CsvDataTransfromTest.class);
	
	/**
	 * Test transforming csv data with time in epoch format 
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void plainCSVToLengthEncoded() 
	throws IOException, MissingFieldException,HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getRecordsDiscardedCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void quotedCSVToLengthEncoded() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		
		Assert.assertEquals(4, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getRecordsDiscardedCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());		
		
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void csvWithDateFormat() 
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getRecordsDiscardedCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		   
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test
	public void plainCsvWithExtraFields() throws IOException, MissingFieldException, 
		HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		DummyStatusReporter reporter = new DummyStatusReporter();
		
		pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		
		Assert.assertEquals(8, reporter.getRecordsWrittenCount());
		Assert.assertEquals(0, reporter.getRecordsDiscardedCount());
		Assert.assertEquals(0, reporter.getMissingFieldErrorCount());
		Assert.assertEquals(0, reporter.getDateParseErrorsCount());
		Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
		
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test 
	public void plainCsvWithMissingTimeField()
	throws IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			DummyStatusReporter reporter = new DummyStatusReporter();		
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
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
			DummyStatusReporter reporter = new DummyStatusReporter();
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
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
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test 
	public void plainCsvWithMissingField()
	throws IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
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
		ProcessManager pm = new ProcessManager(null, null, null);
		
		ByteArrayInputStream bis = 
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		
		try 
		{
			DummyStatusReporter reporter = new DummyStatusReporter();
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
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
			DummyStatusReporter reporter = new DummyStatusReporter();
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
			Assert.assertTrue(false); // should throw
		} 
		catch (MissingFieldException e)
		{
			Assert.assertEquals(e.getMissingFieldName(), "missing_field");
		}
	}
	
	
	/**
	 * Tests writing csv records where some records have
	 * missing values. Tests for epoch, epoch_ms and timeformat
	 * 
	 * @throws IOException
	 * @throws MissingFieldException 
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 */
	@Test 
	public void plainCsvWithIncompleteRecords()
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		String epoch_data = "_time,airline,responsetime,sourcetype,airport,baggage\n" +
				"1350824400,DJA,622,flightcentre,MAN,none\n" +
				"1350824401,JQA,1742,,\n" +  // this field is't written
				"1350824402,GAL,,flightcentre,SYN,some\n" +
				"1350824403,GAL,3893,flightcentre,CHM,some\n" +
				"1350824403,\n" +   // this field is't written
				"1350824404,DJA,189,flightcentre,GAT,lost\n";
		
		String epoch_ms_data = "_time,airline,responsetime,sourcetype,airport,baggage\n" +
				"1350824400000,DJA,622,flightcentre,MAN,none\n" +
				"1350824401000,JQA,1742,,\n" + // this field is't written
				"1350824402000,GAL,,flightcentre,SYN,some\n" +
				"1350824403000,GAL,3893,flightcentre,CHM,some\n" +
				"1350824403000,\n" +   // this field is't written
				"1350824404000,DJA,189,flightcentre,GAT,lost\n";
		
		String epoch_timeformat_data = "_time,airline,responsetime,sourcetype,airport,baggage\n" +
				"2012-10-21 13:00:00 Z,DJA,622,flightcentre,MAN,none\n" +
				"2012-10-21 13:00:01 Z,JQA,1742,,\n" + // this field is't written
				"2012-10-21 13:00:02 Z,GAL,,flightcentre,SYN,some\n" +
				"2012-10-21 13:00:03 Z,GAL,3893,flightcentre,CHM,some\n" +
				"2012-10-21 13:00:03 Z,\n" +   // this field is't written
				"2012-10-21 13:00:04 Z,DJA,189,flightcentre,GAT,lost\n";		
		
		List<String> analysisFields = Arrays.asList(new String [] {"airline", "responsetime", "baggage"});
		
		String [][] lines = new String [] [] {{"_time","airline", "responsetime","baggage"},
				{"1350824400", "DJA", "622", "none"},
				{"1350824402", "GAL", "", "some"},
				{"1350824403", "GAL", "3893", "some"},
				{"1350824404", "DJA", "189", "lost"}};
		
		
		int loop = 0;
		for (String data : new String[] {epoch_data, epoch_ms_data, epoch_timeformat_data})
		{
			loop++;
			
			DataDescription dd = new DataDescription();
			dd.setFormat(DataFormat.DELINEATED);
			dd.setFieldDelimiter(',');
			if (loop == 2)
			{
				dd.setTimeFormat("epoch_ms");
			}
			else if (loop == 3)
			{
				dd.setTimeFormat("yyyy-MM-dd HH:mm:ss X");
			}
			
			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

			DummyStatusReporter reporter = new DummyStatusReporter();
			
			pm.writeToJob(dd, analysisFields, bis, bos, reporter, s_Logger);
			
			Assert.assertEquals(4, reporter.getRecordsWrittenCount());
			Assert.assertEquals(2, reporter.getRecordsDiscardedCount());
			Assert.assertEquals(2, reporter.getMissingFieldErrorCount());
			Assert.assertEquals(0, reporter.getDateParseErrorsCount());
			Assert.assertEquals(0, reporter.getOutOfOrderRecordCount());
			
			ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

			for (String [] fields : lines)
			{
				int numFields = bb.getInt();
				Assert.assertEquals(analysisFields.size() +1, numFields);

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
	
	
	
	
}
