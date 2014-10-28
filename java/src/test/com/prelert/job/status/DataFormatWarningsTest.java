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
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                  
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.persistence.none.NoneJobDataPersister;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.usage.DummyUsageReporter;

public class DataFormatWarningsTest 
{
	Logger s_Logger = Logger.getLogger(DataFormatWarningsTest.class);
	
	/**
	 * Writes to nowhere
	 */
	public class NullOutputStream extends OutputStream 
	{
	  @Override
	  public void write(int b) throws IOException 
	  {
	  }
	}
	
	
	/**
	 * Test writing csv data with unparseble dates throws a
	 * HighProportionOfBadTimestampsException 
	 * 
	 * @throws JsonParseException
	 * @throws MissingFieldException
	 * @throws IOException
	 * @throws OutOfOrderRecordsException
	 */
	@Test
	public void highProportionOfBadTimestampsCsvTest()
	throws JsonParseException, MissingFieldException, IOException, 
			OutOfOrderRecordsException
	{
		final String HEADER = "time,responsetime,sourcetype,airline\n";
		final String RECORD_TEMPLATE = "\"%s\",0.35,Farequote,AAL\n"; 
		
		final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

		final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
		final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;
		
		// do for epoch, epochms, date format
				
		AnalysisConfig ac = new AnalysisConfig();
		Detector det = new Detector();
		det.setFieldName("responsetime");
		det.setByFieldName("airline");
		det.setPartitionFieldName("sourcetype");
		ac.setDetectors(Arrays.asList(det));
		
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		
		String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
		for (String apiDateFormat : apiDateFormats)
		{
			// create data
			boolean goodRecord = true;
			long startEpoch = new Date().getTime();

			StringBuilder sb = new StringBuilder(HEADER);
			for (long i=0; i<1000; i++)
			{
				if (goodRecord == false)
				{
					sb.append(String.format(RECORD_TEMPLATE, ""));
				}
				else
				{
					Date d = new Date(startEpoch + i * 1000);
					
					String record;
					if (apiDateFormat.equals("epoch"))
					{
						record = String.format(RECORD_TEMPLATE, 
								Long.toString(d.getTime() / 1000));
					}
					else if (apiDateFormat.equals("epoch_ms"))
					{
						record = String.format(RECORD_TEMPLATE, 
								Long.toString(d.getTime()));
					}
					else
					{
						record = String.format(RECORD_TEMPLATE, 
								dateFormat.format(d));
					}
					
					sb.append(record);
				}

				goodRecord = !goodRecord;
			}


			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

			DataDescription dd = new DataDescription();
			dd.setFieldDelimiter(',');
			dd.setTimeField("time");
			dd.setTimeFormat(apiDateFormat);



			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP, 
					Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
					Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

			DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", s_Logger);
			DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
			JobDataPersister dp = new NoneJobDataPersister();
			
			Assert.assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS, 
					statusReporter.getAcceptablePercentDateParseErrors());
			Assert.assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
					statusReporter.getAcceptablePercentOutOfOrderErrors());

			try
			{
				pm.writeToJob(dd, ac, bis, new NullOutputStream(), 
						statusReporter, dp, s_Logger);
				Assert.assertTrue(false); // should throw
			}
			catch (HighProportionOfBadTimestampsException e)
			{
				long percentBad = (e.getNumberBad() * 100 )/ e.getTotalNumber();
				
				Assert.assertEquals(statusReporter.getBytesRead(), 
						usageReporter.getBytesReadSinceLastReport() );
				Assert.assertTrue(percentBad >= MAX_PERCENT_DATE_PARSE_ERRORS);
			}
		}
	}
	
	
	/**
	 * Test writing JSON data with unparseble dates throws a
	 * HighProportionOfBadTimestampsException 
	 * 
	 * @throws JsonParseException
	 * @throws MissingFieldException
	 * @throws IOException
	 * @throws OutOfOrderRecordsException
	 */
	@Test
	public void highProportionOfBadTimestampsJsonTest()
	throws JsonParseException, MissingFieldException, IOException, 
			OutOfOrderRecordsException
	{
		final String RECORD_TEMPLATE = "{\"time\":\"%s\","
				+ "\"responsetime\":0.35,"
				+ "\"sourcetype\":\"Farequote\","
				+ "\"airline\":\"AAL\"}";
		
		final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

		final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
		final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;
		
		// do for epoch, epochms, date format

		
		AnalysisConfig ac = new AnalysisConfig();
		Detector det = new Detector();
		det.setFieldName("responsetime");
		det.setByFieldName("airline");
		det.setPartitionFieldName("sourcetype");
		ac.setDetectors(Arrays.asList(det));
		
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		
		String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
		for (String apiDateFormat : apiDateFormats)
		{
			// create data
			boolean goodRecord = true;
			long startEpoch = new Date().getTime();

			StringBuilder sb = new StringBuilder();
			for (long i=0; i<1000; i++)
			{
				if (goodRecord == false)
				{
					sb.append(String.format(RECORD_TEMPLATE, ""));
				}
				else
				{
					Date d = new Date(startEpoch + i * 1000);
					
					String record;
					if (apiDateFormat.equals("epoch"))
					{
						record = String.format(RECORD_TEMPLATE, 
								Long.toString(d.getTime() / 1000));
					}
					else if (apiDateFormat.equals("epoch_ms"))
					{
						record = String.format(RECORD_TEMPLATE, 
								Long.toString(d.getTime()));
					}
					else
					{
						record = String.format(RECORD_TEMPLATE, 
								dateFormat.format(d));
					}
					
					sb.append(record);
				}

				goodRecord = !goodRecord;
			}


			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

			DataDescription dd = new DataDescription();
			dd.setFormat(DataFormat.JSON);
			dd.setTimeField("time");
			dd.setTimeFormat(apiDateFormat);

			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP, 
					Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
					Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

			DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", s_Logger);
			DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
			JobDataPersister dp = new NoneJobDataPersister();
			
			Assert.assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS, 
					statusReporter.getAcceptablePercentDateParseErrors());
			Assert.assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
					statusReporter.getAcceptablePercentOutOfOrderErrors());

			try
			{
				pm.writeToJob(dd, ac, bis, new NullOutputStream(),
						statusReporter, dp, s_Logger);
				Assert.assertTrue(false); // should throw
			}
			catch (HighProportionOfBadTimestampsException e)
			{
				long percentBad = (e.getNumberBad() * 100 )/ e.getTotalNumber();
				
				Assert.assertEquals(statusReporter.getBytesRead(), 
						usageReporter.getBytesReadSinceLastReport());
				Assert.assertTrue(percentBad >= MAX_PERCENT_DATE_PARSE_ERRORS);
			}
		}
	}	
	
	
	/**
	 * Test writing CSV with out of order records should throw an exception
	 * 
	 * @throws JsonParseException
	 * @throws MissingFieldException
	 * @throws IOException
	 * @throws OutOfOrderRecordsException
	 */
	@Test
	public void OutOfOrderRecondsCsvTest()
	throws JsonParseException, MissingFieldException, IOException, 
		HighProportionOfBadTimestampsException
	{
		final String HEADER = "time,responsetime,sourcetype,airline\n";
		final String RECORD_TEMPLATE = "\"%s\",0.35,Farequote,AAL\n"; 
		
		final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

		final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
		final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 8;
		
		// do for epoch, epochms, date format
		
		
		AnalysisConfig ac = new AnalysisConfig();
		Detector det = new Detector();
		det.setFieldName("responsetime");
		det.setByFieldName("airline");
		det.setPartitionFieldName("sourcetype");
		ac.setDetectors(Arrays.asList(det));
		
		
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		
		String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
		for (String apiDateFormat : apiDateFormats)
		{
			// create data
			long startEpoch = new Date().getTime();

			StringBuilder sb = new StringBuilder(HEADER);
			for (long i=0; i<1000; i++)
			{
				// make 1 in 10 records a bad un
				boolean badRecord = i % 10 == 0;
				
				Date d;
				if (badRecord)
				{
					d = new Date(startEpoch);
				}
				else
				{
					d = new Date(startEpoch + i * 1000);
				}
					

				String record;
				if (apiDateFormat.equals("epoch"))
				{
					record = String.format(RECORD_TEMPLATE, 
							Long.toString(d.getTime() / 1000));
				}
				else if (apiDateFormat.equals("epoch_ms"))
				{
					record = String.format(RECORD_TEMPLATE, 
							Long.toString(d.getTime()));
				}
				else
				{
					record = String.format(RECORD_TEMPLATE, 
							dateFormat.format(d));
				}

				sb.append(record);

			}

			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

			DataDescription dd = new DataDescription();
			dd.setFormat(DataFormat.DELINEATED);
			dd.setTimeField("time");
			dd.setFieldDelimiter(',');
			dd.setTimeFormat(apiDateFormat);

			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP, 
					Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
					Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

			DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", s_Logger);
			DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
			JobDataPersister dp = new NoneJobDataPersister();
			
			Assert.assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS, 
					statusReporter.getAcceptablePercentDateParseErrors());
			Assert.assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
					statusReporter.getAcceptablePercentOutOfOrderErrors());

			try
			{
				pm.writeToJob(dd, ac, bis, new NullOutputStream(), 
						statusReporter, dp, s_Logger);
				Assert.assertTrue(false); // should throw
			}
			catch (OutOfOrderRecordsException e)
			{
				long percentBad = (e.getNumberOutOfOrder() * 100 )/ e.getTotalNumber();
				
				Assert.assertEquals(statusReporter.getBytesRead(), 
						usageReporter.getBytesReadSinceLastReport());
				Assert.assertTrue(percentBad >= MAX_PERCENT_OUT_OF_ORDER_ERRORS);
			}
		}
	}
	
	
	/**
	 * Test writing JSON with out of order records should throw an exception
	 * 
	 * @throws JsonParseException
	 * @throws MissingFieldException
	 * @throws IOException
	 * @throws OutOfOrderRecordsException
	 */
	@Test
	public void outOfOrderRecordsJsonTest()
	throws JsonParseException, MissingFieldException, IOException, 
		HighProportionOfBadTimestampsException
	{
		final String RECORD_TEMPLATE = "{\"time\":\"%s\","
				+ "\"responsetime\":0.35,"
				+ "\"sourcetype\":\"Farequote\","
				+ "\"airline\":\"AAL\"}";
		
		final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

		final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
		final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 8;
		
		// do for epoch, epochms, date format
				
		AnalysisConfig ac = new AnalysisConfig();
		Detector det = new Detector();
		det.setFieldName("responsetime");
		det.setByFieldName("airline");
		det.setPartitionFieldName("sourcetype");
		ac.setDetectors(Arrays.asList(det));		
		
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		
		String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
		for (String apiDateFormat : apiDateFormats)
		{
			// create data
			long startEpoch = new Date().getTime();

			StringBuilder sb = new StringBuilder();
			for (long i=0; i<1000; i++)
			{
				// make 1 in 10 records a bad un
				boolean badRecord = i % 10 == 0;
				
				Date d;
				if (badRecord)
				{
					d = new Date(startEpoch);
				}
				else
				{
					d = new Date(startEpoch + i * 1000);
				}
					

				String record;
				if (apiDateFormat.equals("epoch"))
				{
					record = String.format(RECORD_TEMPLATE, 
							Long.toString(d.getTime() / 1000));
				}
				else if (apiDateFormat.equals("epoch_ms"))
				{
					record = String.format(RECORD_TEMPLATE, 
							Long.toString(d.getTime()));
				}
				else
				{
					record = String.format(RECORD_TEMPLATE, 
							dateFormat.format(d));
				}

				sb.append(record);

			}

			// can create with null
			ProcessManager pm = new ProcessManager(null, null, null, null, null);

			ByteArrayInputStream bis = 
					new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

			DataDescription dd = new DataDescription();
			dd.setFormat(DataFormat.JSON);
			dd.setTimeField("time");
			dd.setTimeFormat(apiDateFormat);

			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP, 
					Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
			System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
					Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

			DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", s_Logger);
			DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
			JobDataPersister dp = new NoneJobDataPersister();
			
			Assert.assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS, 
					statusReporter.getAcceptablePercentDateParseErrors());
			Assert.assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
					statusReporter.getAcceptablePercentOutOfOrderErrors());

			try
			{
				pm.writeToJob(dd, ac, bis, new NullOutputStream(), 
						statusReporter, dp, s_Logger);
				Assert.assertTrue(false); // should throw
			}
			catch (OutOfOrderRecordsException e)
			{
				long percentBad = (e.getNumberOutOfOrder() * 100 )/ e.getTotalNumber();
				
				Assert.assertEquals(statusReporter.getBytesRead(), 
						usageReporter.getBytesReadSinceLastReport());
				Assert.assertTrue(percentBad >= MAX_PERCENT_OUT_OF_ORDER_ERRORS);
			}
		}
	}		
}
