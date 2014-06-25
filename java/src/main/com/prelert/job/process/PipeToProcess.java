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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.DataDescription;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.warnings.HighProportionOfBadTimestampsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;
import com.prelert.job.warnings.StatusReporter;
import com.prelert.rs.data.ErrorCode;


/**
 * Static utility methods for transforming and piping 
 * CSV or JSON data from an inputstream to outputstream.
 * The data writtin to output is length encoded each record 
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more 
 * detailed description.
 */
public class PipeToProcess 
{
	/**
	 * Read the csv input, transform to length encoded values and pipe
	 * to the OutputStream. 
	 * No transformation is applied to the data the timestamp is expected
	 * in seconds from the epoch.
	 * If any of the fields in <code>analysisFields</code> or the 
	 * <code>DataDescription</code>s timeField is missing from the CSV header
	 * a <code>MissingFieldException</code> is thrown
	 * 
	 * @param dd
	 * @param analysisFields
	 * @param is
	 * @param os
	 * @param reporter
	 * @param logger Errors are logged to this logger
	 * @return The number of records written to the outputstream
	 * @throws IOException
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws HighProportionOfBadTimestampsException If a large proportion 
	 * of the records read have missing fields
	 * @throws OutOfOrderRecordsException 
	 */
	static public long pipeCsv(DataDescription dd, List<String> analysisFields,
		InputStream is, OutputStream os, StatusReporter reporter, Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{	
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();
		
		int recordsWritten = 0;
		int lineCount = 0;
		
		CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref);
		try 
		{
			String[] header = csvReader.getHeader(true);
			
			List<Pair<String, Integer>> fieldIndexes = 
					findFieldIndexes(header, dd.getTimeField(), analysisFields);
			
			int maxIndex = 0;
			Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
			while (iter.hasNext())
			{
				Pair<String, Integer> p = iter.next();
				
				if (p.Second > maxIndex)
				{
					maxIndex = p.Second;
				}
				
				if (p.Second < 0)
				{
					String msg = String.format("Field configured for analysis " 
							+ "'%s' is not in the CSV header '%s'", 
							p.First, Arrays.toString(header));
					logger.error(msg);
					
					reporter.reportMissingField(p.First);

					throw new MissingFieldException(p.First, msg, 
							ErrorCode.MISSING_FIELD);
				}
			}
			
			String [] filteredHeader = new String [fieldIndexes.size()];
			int i=0;
			for (Pair<String, Integer> p : fieldIndexes)
			{
				filteredHeader[i++] = p.First;
			}
			
			// Don't close the output stream as it causes the autodetect 
			// process to quit
			LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
			lengthEncodedWriter.writeRecord(filteredHeader);
			
			
			int timeFieldIndex = Arrays.asList(filteredHeader).indexOf(dd.getTimeField());
			if (timeFieldIndex < 0)
			{
				String message = String.format("Cannot find timestamp field '%s'"
						+ " in CSV header '%s'", dd.getTimeField(), Arrays.toString(filteredHeader));
				logger.error(message);
				throw new MissingFieldException(dd.getTimeField(), message, 
						ErrorCode.MISSING_FIELD);
			}	
			

			int numFields = fieldIndexes.size();
			List<String> line;
			
			String [] record = new String [numFields]; 
			
			long lastEpoch = 0;
			while ((line = csvReader.read()) != null)
			{
				lineCount++;
								
				i = 0;
				if (maxIndex >= line.size())
				{
					logger.warn("Not enough fields in csv record " + line);
					reporter.reportMissingField(Arrays.toString(line.toArray()));
					
					Arrays.fill(record, "");
					for (Pair<String, Integer> p : fieldIndexes)
					{
						if (p.Second >= line.size())
						{
							continue;
						}
						
						String field = line.get(p.Second);
						record[i] = (field == null) ? "" : field;	
						i++;
					}
				}
				else
				{
					for (Pair<String, Integer> p : fieldIndexes)
					{
						String field = line.get(p.Second);
						record[i] = (field == null) ? "" : field;	
						i++;
					}
				}
				
				try
				{
					// parse as a double and throw away the fractional 
					// component
					long epoch = Double.valueOf(record[timeFieldIndex]).longValue();
					
					if (epoch < lastEpoch)
					{
						// out of order 
						reporter.reportOutOfOrderRecord(epoch, lastEpoch);
					}
					else
					{	// write record
						record[timeFieldIndex] = Long.toString(epoch);	
						lengthEncodedWriter.writeRecord(record);
						
						reporter.reportRecordWritten();
						recordsWritten++;
						lastEpoch = epoch;
					}
				}
				catch (NumberFormatException e)
				{
					String message = String.format(
							"Cannot parse timestamp '%s' as epoch value",								
							record[timeFieldIndex]);

					reporter.reportDateParseError(record[timeFieldIndex]);
					logger.error(message);
				}	
			}
			
			lengthEncodedWriter.flush();
		}
		finally
		{
			csvReader.close();
		}

		
		reporter.finishReporting();
		logger.debug(String.format("Transferred %d of %d CSV records to autodetect.", 
				recordsWritten, lineCount));
		
		return recordsWritten;
	}
	

	/**
	 * Parse the contents from input stream, transform dates and write to 
	 * the output stream as length encoded values.
	 * If any of the fields in <code>analysisFields</code> or the 
	 * <code>DataDescription</code>s timeField is missing from the CSV header
	 * a <code>MissingFieldException</code> is thrown
	 * Flushes the outputstream once all data is written.
	 * 
	 * @param dd 
	 * @param analysisFields
	 * @param is
	 * @param os
	 * @param reporter
	 * @param logger Errors are logged to this logger
	 * @return The number of records written to the outputstream
	 * @throws IOException 
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws HighProportionOfBadTimestampsException If a large proportion 
	 * of the records read have missing fields or unparseable date formats
	 * @throws OutOfOrderRecordsException 
	 */
	static public void transformAndPipeCsv(DataDescription dd, List<String> analysisFields,
			InputStream is, OutputStream os, StatusReporter reporter, Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException
	{
		String timeField = dd.getTimeField();
		
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();	
		
		int recordsWritten = 0;
		int lineCount = 0;
		
		CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref);
		try
		{
			String[] header = csvReader.getHeader(true);	
			
			List<Pair<String, Integer>> fieldIndexes = 
					findFieldIndexes(header, timeField, analysisFields);
							
			int maxIndex = 0;
			Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
			while (iter.hasNext())
			{
				Pair<String, Integer> p = iter.next();
				
				if (p.Second > maxIndex)
				{
					maxIndex = p.Second;
				}
				
				if (p.Second < 0)
				{				
					String msg = String.format("Field configured for analysis " 
							+ "'%s' is not in the CSV header '%s'", 
								p.First, Arrays.toString(header));
					logger.error(msg);
					
					reporter.reportMissingField(timeField);
			
					throw new MissingFieldException(p.First, msg, ErrorCode.MISSING_FIELD);
				}
			}			
			
			// first write the header
			header = new String [fieldIndexes.size()];
			int i=0;
			for (Pair<String, Integer> p : fieldIndexes)
			{
				header[i++] = p.First;
			}
			
			int timeFieldIndex = Arrays.asList(header).indexOf(timeField);
			if (timeFieldIndex < 0)
			{
				String message = String.format("Cannot find timestamp field '%s'"
						+ " in CSV header '%s'", timeField, Arrays.toString(header));
				logger.error(message);
				
				reporter.reportMissingField(timeField);
				throw new MissingFieldException(timeField, message, ErrorCode.MISSING_FIELD);
			}	
			

			// Don't close the output stream as it causes the autodetect 
			// process to quit
			LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
			lengthEncodedWriter.writeRecord(header);

			List<String> line;
			int numFields = fieldIndexes.size();
			String [] record = new String [numFields];
			long lastEpoch = 0;
			
			if (dd.isEpochMs())
			{
				while ((line = csvReader.read()) != null)
				{			
					lineCount++;
					
					i = 0;
					if (maxIndex >= line.size())
					{
						logger.warn("Not enough fields in csv record " + line);
						reporter.reportMissingField(Arrays.toString(line.toArray()));
						
						Arrays.fill(record, "");
						for (Pair<String, Integer> p : fieldIndexes)
						{
							if (p.Second >= line.size())
							{
								continue;
							}
							
							String field = line.get(p.Second);
							record[i] = (field == null) ? "" : field;	
							i++;
						}
					}
					else
					{
						for (Pair<String, Integer> p : fieldIndexes)
						{
							String field = line.get(p.Second);
							record[i] = (field == null) ? "" : field;	
							i++;
						}
					}

					try
					{
						// parse as a double and throw away the fractional 
						// component
						long epoch = Double.valueOf(record[timeFieldIndex]).longValue() / 1000;
						if (epoch < lastEpoch)
						{
							// out of order 
							reporter.reportOutOfOrderRecord(epoch, lastEpoch);
						}
						else
						{	// write record
							record[timeFieldIndex] = Long.toString(epoch);	
							lengthEncodedWriter.writeRecord(record);
							
							recordsWritten++;
							reporter.reportRecordWritten();
							lastEpoch = epoch;
						}
					}
					catch (NumberFormatException e)
					{
						String message = String.format(
								"Cannot parse epoch ms timestamp '%s'",	record[timeFieldIndex]);							

						reporter.reportDateParseError(record[timeFieldIndex]);
						logger.error(message);
					}						
				}
			}
			else
			{
				DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());

				while ((line = csvReader.read()) != null)
				{
					lineCount++;
					
					i = 0;
					if (maxIndex >= line.size())
					{
						logger.error("Not enough fields in csv record " + line);
						reporter.reportMissingField(Arrays.toString(line.toArray()));
						
						Arrays.fill(record, "");
						for (Pair<String, Integer> p : fieldIndexes)
						{
							if (p.Second >= line.size())
							{
								continue;
							}
							String field = line.get(p.Second);
							record[i] = (field == null) ? "" : field;	
							i++;
						}
					}
					else
					{
						for (Pair<String, Integer> p : fieldIndexes)
						{
							String field = line.get(p.Second);
							record[i] = (field == null) ? "" : field;	
							i++;
						}
					}
					
					try
					{
						long epoch = dateFormat.parse(record[timeFieldIndex]).getTime() / 1000;
						if (epoch < lastEpoch)
						{
							// out of order 
							reporter.reportOutOfOrderRecord(epoch, lastEpoch);
						}
						else
						{	// write record
							record[timeFieldIndex] = Long.toString(epoch);	
							lengthEncodedWriter.writeRecord(record);
							
							recordsWritten++;
							reporter.reportRecordWritten();
							lastEpoch = epoch;
						}
					}
					catch (ParseException pe)
					{
						String date = record[timeFieldIndex];
						String message = String.format("Cannot parse date '%s' with format string '%s'",
								date, dd.getTimeFormat());
						
						reporter.reportDateParseError(date);
						logger.error(message);
					}		
				}
			}
		
			reporter.finishReporting();
			
			logger.debug(String.format("Transferred %d of %d CSV records to autodetect.", 
					recordsWritten, lineCount));
			
			// flush the output
			os.flush();
		}
		finally
		{
			csvReader.close();
		}		
	}
	
	
	/**
	 * Finds the indexes of the analysis fields and the 
	 * timestamp field in <code>header</code>.
	 * 
	 * @param header
	 * @param timeField
	 * @param analysisFields
	 * @return
	 */
	static private List<Pair<String, Integer>> findFieldIndexes(
			String [] header, String timeField, List<String> analysisFields)
	{
		List<String> headerList = Arrays.asList(header);  // TODO header could be empty
		
		List<Pair<String, Integer>> fieldIndexes = new ArrayList<>();
		
		// time field
		Pair<String, Integer> p = new Pair<>(timeField, 
				headerList.indexOf(timeField));	
		fieldIndexes.add(p);
		
		for (String field : analysisFields)
		{
			p = new Pair<>(field, headerList.indexOf(field));			
			fieldIndexes.add(p);
		}

		return fieldIndexes;		
	}		
	
		
	/**
	 *
	 * Flushes the outputstream once all data is written.
	 * 
	 * @param dd 
	 * @param analysisFields
	 * @param is Closed at the end of this function
	 * @param os
	 * @param reporter
	 * @param logger Errors are logged to this logger 
	 * @throws JsonParseException 
	 * @throws IOException
	 * @throws HighProportionOfBadTimestampsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 * @throws OutOfOrderRecordsException 
	 */
	static public void transformAndPipeJson(DataDescription dd, 
			List<String> analysisFields, InputStream is, OutputStream os,
			StatusReporter reporter, Logger logger) 
	throws JsonParseException, IOException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		try (JsonParser parser = new JsonFactory().createParser(is))
		{
			if (dd.isTransformTime())
			{
				pipeJsonAndTransformTime(parser, analysisFields, os, dd, reporter, logger);
			}
			else
			{
				pipeJson(parser, dd.getTimeField(), analysisFields, os, reporter, logger);
			}

			os.flush();
		}
	}


	/**
	 * Parse the Json objects and write to output stream.
	 *
	 * @param parser
	 * @param timeField
	 * @param analysisFields
	 * @param os
	 * @param reporter
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws HighProportionOfBadTimestampsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 * @throws OutOfOrderRecordsException 
	 */
	static private void pipeJson(JsonParser parser, String timeField,
			List<String> analysisFields, OutputStream os,
			StatusReporter reporter, Logger logger)
	throws JsonParseException, IOException, HighProportionOfBadTimestampsException, 
		OutOfOrderRecordsException
	{
		LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);

		// record is all the analysis fields + the time field
		List<String> allFields = new ArrayList<String>(analysisFields);
		allFields.add(timeField);
		// time field is the last item 
		int timeFieldIndex = allFields.size() -1;

		String [] record = new String[allFields.size()];
		boolean [] gotFields = new boolean[record.length];

		Map<String, Integer> fieldMap = new HashMap<>();
		for (Integer i = new Integer(0); i < allFields.size(); i++)
		{
			fieldMap.put(allFields.get(i), i);
		}
				
		// write header and first record
		lengthEncodedWriter.writeRecord(allFields);
				
		int recordsWritten = 0;
		int recordCount = 
				readJsonRecord(parser, record, fieldMap, allFields, gotFields,
						logger) ? 1 : 0;
				
		if (gotFields[timeFieldIndex])
		{
			String missing = firstMissingField(allFields, gotFields); 
			if (missing != null)
			{
				reporter.reportMissingField(missing);
			}
			
			try
			{
				// parse as a double and throw away the fractional 
				// component
				long epoch = Double.valueOf(record[timeFieldIndex]).longValue();
				record[timeFieldIndex] = new Long(epoch).toString();
				
				lengthEncodedWriter.writeRecord(record);
				recordsWritten++;
				reporter.reportRecordWritten();
			}
			catch (NumberFormatException e)
			{
				String message = String.format(
						"Cannot parse timestamp '%s' as epoch value",								
						record[timeFieldIndex]);

				reporter.reportDateParseError(record[timeFieldIndex]);
				logger.error(message);						
			}
			
		}
		else
		{
			logger.warn("Missing time field from JSON document");
			reporter.reportMissingField(timeField);							
		}		
		
		long lastEpoch = 0;
		while (readJsonRecord(parser, record, fieldMap, allFields, gotFields,
				logger))
		{		
			if (gotFields[timeFieldIndex])
			{
				String missing = firstMissingField(allFields, gotFields); 
				if (missing != null)
				{
					reporter.reportMissingField(missing);
				}
				
				try
				{
					// parse as a double and throw away the fractional 
					// component
					long epoch = Double.valueOf(record[timeFieldIndex]).longValue();
					if (epoch < lastEpoch)
					{
						// out of order 
						reporter.reportOutOfOrderRecord(epoch, lastEpoch);
					}
					else
					{	// write record
						record[timeFieldIndex] = Long.toString(epoch);	
						lengthEncodedWriter.writeRecord(record);
						
						recordsWritten++;
						reporter.reportRecordWritten();
						lastEpoch = epoch;
					}
				}
				catch (NumberFormatException e)
				{
					String message = String.format(
							"Cannot parse timestamp '%s' as epoch value",								
							record[timeFieldIndex]);
					
					reporter.reportDateParseError(record[timeFieldIndex]);
					logger.error(message);						
				}
			}
			else
			{
				logger.warn("Missing time field from JSON document");
				reporter.reportMissingField(timeField);							
			}
			
			++recordCount;			
		}

		reporter.finishReporting();
		logger.debug(String.format("Transferred %d of %d Json records to autodetect.", 
				recordsWritten, recordCount));
	}

	/**
	 * Parse the Json objects convert the timestamp to epoch time
	 * and write to output stream. This shares a lot of code with
	 * {@linkplain #pipeJson(JsonParser, OutputStream)} repeated
	 * for the sake of efficiency.
	 *
	 * @param parser
	 * @param analysisFields, 
	 * @param os
	 * @param dd
	 * @param reporter
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws HighProportionOfBadTimestampsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 * @throws OutOfOrderRecordsException 
	 */
	static private void pipeJsonAndTransformTime(JsonParser parser, 
			List<String> analysisFields, OutputStream os,
			DataDescription dd, StatusReporter reporter, Logger logger)
	throws JsonParseException, IOException, HighProportionOfBadTimestampsException, 
		OutOfOrderRecordsException
	{
		String timeField = dd.getTimeField();
		List<String> allFields = new ArrayList<String>(analysisFields);
		allFields.add(timeField);

		LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
		
		// record is the size of the analysis fields + the time field
		String [] record = new String[allFields.size()];		
		boolean [] gotFields = new boolean[record.length];

		Map<String, Integer> fieldMap = new HashMap<>();
		for (Integer i = new Integer(0); i < allFields.size(); i++)
		{
			fieldMap.put(allFields.get(i), i);
		}
		// time field is the last item always
		int timeFieldIndex = record.length -1;
			
			
		int recordsWritten = 0;
		int recordCount = 
				readJsonRecord(parser, record, fieldMap, allFields, gotFields, 
						logger) ? 1 : 0;
		
		if (dd.isEpochMs())
		{
			try
			{
				long epoch = Double.valueOf(record[timeFieldIndex]).longValue() / 1000;
				record[timeFieldIndex] = Long.toString(epoch);
			}
			catch (NumberFormatException e)
			{
				String message = String.format(
						"Cannot parse epoch ms timestamp '%s'",								
						record[timeFieldIndex]);
				logger.error(message);
				
				reporter.reportDateParseError(record[timeFieldIndex]);
			}
		}
		else
		{
			try
			{
				DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
				record[timeFieldIndex] = Long.toString(dateFormat.parse(record[timeFieldIndex]).getTime() / 1000);
			}
			catch (ParseException e)
			{
				logger.error("Cannot parse '" + record[timeFieldIndex] +
						"' as a date using format string '" +
						dd.getTimeFormat() + "'");
				
				reporter.reportDateParseError(record[timeFieldIndex]);
			}
		}
		
		// write header
		lengthEncodedWriter.writeRecord(allFields);
		
		//write record
		if (gotFields[timeFieldIndex])
		{				
			String missing = firstMissingField(allFields, gotFields); 
			if (missing != null)
			{
				reporter.reportMissingField(missing);
			}
			
			lengthEncodedWriter.writeRecord(record);
			reporter.reportRecordWritten();
			recordsWritten++;				
		}
		else
		{
			logger.info("Missing time field from JSON document");
			reporter.reportMissingField(timeField);							
		}
		
		long lastEpoch = 0;

		// is the timestamp a format string or epoch ms.
		if (dd.isEpochMs())
		{
			while (readJsonRecord(parser, record, fieldMap, allFields, gotFields, logger))
			{

				if (gotFields[timeFieldIndex])
				{
					String missing = firstMissingField(allFields, gotFields); 
					if (missing != null)
					{
						reporter.reportMissingField(missing);
					}

					try
					{
						long epoch = Double.valueOf(record[timeFieldIndex]).longValue() / 1000;
						if (epoch < lastEpoch)
						{
							// out of order 
							reporter.reportOutOfOrderRecord(epoch, lastEpoch);
						}
						else
						{	// write record
							record[timeFieldIndex] = Long.toString(epoch);	
							lengthEncodedWriter.writeRecord(record);
							
							recordsWritten++;
							reporter.reportRecordWritten();
							lastEpoch = epoch;
						}
					}
					catch (NumberFormatException e)
					{
						String message = String.format(
								"Cannot parse epoch ms timestamp '%s'",								
								record[timeFieldIndex]);
						logger.error(message);
						
						reporter.reportDateParseError(record[timeFieldIndex]);
					}					
				}
				else
				{
					logger.info("Missing time field from JSON document");
					reporter.reportMissingField(timeField);							
				}
				
				recordCount++;
			}
		}
		else
		{
			DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
			
			while (readJsonRecord(parser, record, fieldMap, allFields, gotFields, logger))
			{
				if (gotFields[timeFieldIndex])
				{
					String missing = firstMissingField(allFields, gotFields); 
					if (missing != null)
					{
						reporter.reportMissingField(missing);
					}

					try
					{
						long epoch = dateFormat.parse(record[timeFieldIndex]).getTime() / 1000;
						if (epoch < lastEpoch)
						{
							// out of order 
							reporter.reportOutOfOrderRecord(epoch, lastEpoch);
						}
						else
						{	// write record
							record[timeFieldIndex] = Long.toString(epoch);	
							lengthEncodedWriter.writeRecord(record);
							
							recordsWritten++;
							reporter.reportRecordWritten();
							lastEpoch = epoch;
						}
					}
					catch (ParseException e)
					{
						logger.error("Cannot parse '" + record[timeFieldIndex] +
								"' as a date using format string '" +
								dd.getTimeFormat() + "'");
						
						reporter.reportDateParseError(record[timeFieldIndex]);
					}

				}
				else
				{
					logger.info("Missing time field from JSON document");
					reporter.reportMissingField(timeField);							
				}
	
				recordCount++;
			}
		}
		
		reporter.finishReporting();
		
		logger.debug(String.format("Transferred %d of %d Json records to autodetect.", 
				recordsWritten, recordCount));
	}
	
	
	/**
	 * Read the JSON object and write to the record array.
	 * Nested objects are flattened with the field names separated by
	 * a '.'. 
	 * e.g. for a record with a nested 'tags' object:
	 *  "{"name":"my.test.metric1","tags":{"tag1":"blah","tag2":"boo"},"time":1350824400,"value":12345.678}"
	 * use 'tags.tag1' to reference the tag1 field in the nested object
	 * 
	 * Array fields in the JSON are ignored
	 *
	 * @param parser
	 * @param record Read fields are written to this array
	 * @param fieldMap Map to field name to record array index position
	 * @param allFields All the required fields
	 * @param gotFields boolean array each element is true if that field
	 * was read
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws JsonParseException
	 */
	static private boolean readJsonRecord(JsonParser parser, String [] record,
			Map<String, Integer> fieldMap,  List<String> allFields,
			boolean [] gotFields, Logger logger) 
	throws JsonParseException, IOException
	{
		Arrays.fill(gotFields, false);
		Arrays.fill(record, "");
		
		int nestedLevel = 0;		
		Deque<String> stack = new ArrayDeque<String>();

		boolean readRecord = false;
		String nestedSuffix = "";

		JsonToken token = parser.nextToken();
		while (!(token == JsonToken.END_OBJECT && nestedLevel == 0))
		{
			if (token == null)
			{
				break;
			}
			if (token == JsonToken.END_OBJECT)
			{
				nestedLevel--;
				String objectFieldName = stack.pop();
				
				int lastIndex = nestedSuffix.length() - objectFieldName.length() -1;
				nestedSuffix = nestedSuffix.substring(0, lastIndex);
			}
			else if (token == JsonToken.FIELD_NAME)
			{
				String fieldName = parser.getCurrentName();				
				token = parser.nextToken();
				
				readRecord = true; // got a field so consider this a proper record

				if (token == JsonToken.START_OBJECT)
				{
					nestedLevel++;
					stack.push(fieldName);
					
					nestedSuffix = nestedSuffix + fieldName + ".";
				}
				else if (token == JsonToken.START_ARRAY)
				{
					// consume the whole array but do nothing with it
					while (token != JsonToken.END_ARRAY)
					{
						token = parser.nextToken();
					}
					logger.warn("Ignoring array field");
				}
				else
				{
					String fieldValue = parser.getText();
					
					Integer index = fieldMap.get(nestedSuffix + fieldName);
					if (index != null)
					{
						record[index] = fieldValue;
						gotFields[index] = true;
					}
				}
			}

			token = parser.nextToken();
		}

		
		return readRecord;
	}
	
	
	/**
	 * Return the first missing field name or null if there are 
	 * no missing fields.
	 * 
	 * @param requiredFields
	 * @param gotFieldFlags
	 * @return null if all fields are present or the first missing field name
	 */
	static private String firstMissingField(List<String> requiredFields, 
			boolean [] gotFieldFlags)
	{		
		String result = null;
		
		for (int i=0; i<gotFieldFlags.length; i++)
		{
			if (gotFieldFlags[i] == false)
			{
				result = requiredFields.get(i);
			}
		}
		
		return result;
	}
	
	/**
	 * Generic helper class 
	 *
	 * @param <T>
	 * @param <U>
	 */
	static private class Pair<T,U>
	{
	    public final T First;
	    public final U Second;
	    public Pair(T first, U second)
	    {
	    	this.First = first;
	    	this.Second = second;
	    }
	}
	
	
	/**
	 * Pipes the raw data from the input to the output in 128kB chunks
	 * without any encoding or transformations
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	static public void pipe(InputStream is, OutputStream os) 
	throws IOException 
	{
		int n;
		byte[] buffer = new byte[131072]; // 128kB
		while ((n = is.read(buffer)) > -1)
		{
			// os is not wrapped in a BufferedOutputStream because we're copying
			// big chunks of data anyway
			os.write(buffer, 0, n);
		}
		os.flush();
	}

}
