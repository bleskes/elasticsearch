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
import java.util.ArrayList;
import java.util.Arrays;
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
import com.prelert.job.warnings.HighProportionOfBadRecordsException;
import com.prelert.job.warnings.StatusReporter;
import com.prelert.rs.data.ErrorCodes;


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
	 * @throws HighProportionOfBadRecordsException If a large proportion 
	 * of the records read have missing fields
	 */
	static public long pipeCsv(DataDescription dd, List<String> analysisFields,
		InputStream is, OutputStream os, StatusReporter reporter, Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadRecordsException
	{	
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();
		
		int recordsWritten = 0;
		int recordsDiscarded = 0;
		
		try (CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref))
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
							ErrorCodes.MISSING_FIELD);
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

			int numFields = fieldIndexes.size();
			List<String> line;
			while ((line = csvReader.read()) != null)
			{
				if (line.size() <= maxIndex)
				{
					// if the record is incomplete don't write it
					logger.warn("Incomplete CSV record: " + line);
					reporter.reportMissingField(Arrays.toString(line.toArray()));
					recordsDiscarded++;
				}
				else
				{
					lengthEncodedWriter.writeNumFields(numFields);				
					for (Pair<String, Integer> p : fieldIndexes)
					{
						String record = line.get(p.Second);
						lengthEncodedWriter.writeField((record == null) ? "" : record);
					}
					recordsWritten++;
				}
					
				reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
			}
			
			lengthEncodedWriter.flush();
		}

		
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
	 * @throws HighProportionOfBadRecordsException If a large proportion 
	 * of the records read have missing fields or unparseable date formats
	 */
	static public void transformAndPipeCsv(DataDescription dd, List<String> analysisFields,
			InputStream is, OutputStream os, StatusReporter reporter, Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadRecordsException
	{
		String timeField = dd.getTimeField();
		
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();	
		
		int recordsWritten = 0;
		int recordsDiscarded = 0;
		
		try (CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref))
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
			
					throw new MissingFieldException(p.First, msg, ErrorCodes.MISSING_FIELD);
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
				throw new MissingFieldException(timeField, message, ErrorCodes.MISSING_FIELD);
			}	
			

			// Don't close the output stream as it causes the autodetect 
			// process to quit
			LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
			lengthEncodedWriter.writeRecord(header);

			List<String> line;
			int numFields = fieldIndexes.size();
			String [] record = new String [numFields];
			if (dd.isEpochMs())
			{
				while ((line = csvReader.read()) != null)
				{					
					if (maxIndex >= line.size())
					{
						logger.error("Not enough fields in csv record " + line);
						reporter.reportMissingField(Arrays.toString(line.toArray()));
						recordsDiscarded++;
						
						reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
						continue;
					}
					
					i = 0;
					for (Pair<String, Integer> p : fieldIndexes)
					{
						String field = line.get(p.Second);
						record[i] = (field == null) ? "" : field;	
						i++;
					}

					try
					{
						long epoch = Long.parseLong(record[timeFieldIndex]) / 1000; 
						record[timeFieldIndex] = new Long(epoch).toString();
					}
					catch (NumberFormatException e)
					{
						String date = line.get(timeFieldIndex);
						String message = String.format(
								"Cannot parse epoch ms timestamp '%s'", date);

						reporter.reportDateParseError(date);
						logger.error(message);
						
						recordsDiscarded++;
						reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
						continue;
					}		
					
					lengthEncodedWriter.writeRecord(record);
					
					recordsWritten++;
					reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
				}
			}
			else
			{
				DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());

				while ((line = csvReader.read()) != null)
				{
					if (maxIndex >= line.size())
					{
						logger.error("Not enough fields in csv record " + line);
						
						reporter.reportMissingField(Arrays.toString(line.toArray()));
						recordsDiscarded++;						
						reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
						continue;
					}
					
					i = 0;
					for (Pair<String, Integer> p : fieldIndexes)
					{
						String field = line.get(p.Second);
						record[i] = (field == null) ? "" : field;	
						i++;
					}
					
					try
					{
						record[timeFieldIndex] =  new Long(dateFormat.parse(record[timeFieldIndex]).getTime() / 1000).toString();
					}
					catch (ParseException pe)
					{
						String date = line.get(timeFieldIndex);
						String message = String.format("Cannot parse date '%s' with format string '%s'",
								date, dd.getTimeFormat());
						
						reporter.reportDateParseError(date);
						logger.error(message);
						
						recordsDiscarded++;
						reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);

						logger.error(message);
						continue;
					}		

					lengthEncodedWriter.writeRecord(record);
					
					recordsWritten++;
					reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
				}
			}
			
			logger.info(recordsWritten + " csv records written");
			
			// flush the output
			os.flush();
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
	 * @param is
	 * @param os
	 * @param reporter
	 * @param logger Errors are logged to this logger 
	 * @throws JsonParseException 
	 * @throws IOException
	 * @throws HighProportionOfBadRecordsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 */
	static public void transformAndPipeJson(DataDescription dd, 
			List<String> analysisFields, InputStream is, OutputStream os,
			StatusReporter reporter, Logger logger) 
	throws JsonParseException, IOException, HighProportionOfBadRecordsException
	{
		JsonParser parser = new JsonFactory().createParser(is);
		
		if (dd.isTransformTime())
		{
			pipeJsonAndTransformTime(parser, analysisFields, os, dd, reporter, logger);
		}
		else
		{
			pipeJson(parser, dd.getTimeField(), analysisFields, os, reporter, logger);
		}

		os.flush();

		parser.close();	
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
	 * @throws HighProportionOfBadRecordsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 */
	static private void pipeJson(JsonParser parser, String timeField,
			List<String> analysisFields, OutputStream os,
			StatusReporter reporter, Logger logger)
	throws JsonParseException, IOException, HighProportionOfBadRecordsException
	{
		LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);

		// record is all the analysis fields + the time field
		List<String> allFields = new ArrayList<String>(analysisFields);
		allFields.add(timeField);

		String [] record = new String[allFields.size()];
		boolean [] gotFields = new boolean[record.length];
		Arrays.fill(gotFields, false);

		Map<String, Integer> fieldMap = new HashMap<>();
		for (Integer i = new Integer(0); i < allFields.size(); i++)
		{
			fieldMap.put(allFields.get(i), i);
		}
		
		Integer timeFieldIndex = fieldMap.get(timeField);
				

		int recordsWritten = 0;
		int recordsDiscarded = 0;
		
		JsonToken token = parser.nextToken();
		
		// if the first toke is the start of an array ignore it
		if (token == JsonToken.START_ARRAY)
		{
			token = parser.nextToken();
			logger.debug("JSON starts with an array");
		}

		if (token != JsonToken.START_OBJECT)
		{
			logger.error("Expecting Json Start Object token");
			throw new IOException(
					"Invalid JSON should start with an array of objects or an object."
					+ "Bad token = " + token);
		}

		// write header first
		lengthEncodedWriter.writeRecord(allFields);
	
		while (token == JsonToken.START_OBJECT)
		{
			Arrays.fill(record, "");
			Arrays.fill(gotFields, false);
			
			while (token != JsonToken.END_OBJECT)
			{
				if (token == JsonToken.FIELD_NAME)
				{
					String fieldName = parser.getCurrentName();		
					token = parser.nextToken();
					String fieldValue = parser.getText();

					Integer index = fieldMap.get(fieldName);
					if (index != null)
					{
						record[index] = fieldValue;
						gotFields[index] = true;
					}
				}
				token = parser.nextToken();
			}

			if (gotFields[timeFieldIndex])
			{		
				String missing = firstMissingField(allFields, gotFields); 
				if (missing != null)
				{
					reporter.reportMissingField(missing);
				}
				
				lengthEncodedWriter.writeRecord(record);
				recordsWritten++;				
			}
			else
			{
				logger.info("Missing time field from JSON document");
				reporter.reportMissingField(timeField);				
				recordsDiscarded++;
			}
			
			reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);

			token = parser.nextToken();
		}

		logger.debug("Transferred " + recordsWritten + " Json records to autodetect.");
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
	 * @throws HighProportionOfBadRecordsException If a large proportion 
	 * of the records read have missing fields or unparsable date formats
	 */
	static private void pipeJsonAndTransformTime(JsonParser parser, 
			List<String> analysisFields, OutputStream os,
			DataDescription dd, StatusReporter reporter, Logger logger)
	throws JsonParseException, IOException, HighProportionOfBadRecordsException
	{
		String timeField = dd.getTimeField();
		List<String> allFields = new ArrayList<String>(analysisFields);
		allFields.add(timeField);

		LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
		
		// record is the size of the analysis fields + the time field
		String [] record = new String[allFields.size()];		
		boolean [] gotFields = new boolean[record.length];
		Arrays.fill(gotFields, false);

		Map<String, Integer> fieldMap = new HashMap<>();
		for (Integer i = new Integer(0); i < allFields.size(); i++)
		{
			fieldMap.put(allFields.get(i), i);
		}			
		Integer timeFieldIndex = fieldMap.get(timeField);
		
		// write header
		lengthEncodedWriter.writeRecord(allFields);
			
		int recordsWritten = 0;
		int recordsDiscarded = 0;
		
		// if the first toke is the start of an array ignore it
		JsonToken token = parser.nextToken();
		if (token == JsonToken.START_ARRAY)
		{
			token = parser.nextToken();
			logger.debug("JSON starts with an array");
		}

		if (token != JsonToken.START_OBJECT)
		{
			logger.error("Expecting Json Start Object token");
			throw new IOException(
					"Invalid JSON should start with an array of objects or an object."
					+ "Bad token = " + token);
		}
		

		// is the timestamp a format string or epoch ms.
		if (dd.isEpochMs())
		{
			while (token == JsonToken.START_OBJECT)
			{
				Arrays.fill(record, "");
				Arrays.fill(gotFields, false);

				while (token != JsonToken.END_OBJECT)
				{					
					if (token == JsonToken.FIELD_NAME)
					{
						String fieldName = parser.getCurrentName();
						token = parser.nextToken();
						String fieldValue = parser.getText();

						if (fieldName.equals(timeField))
						{
							try
							{
								fieldValue = Long.toString(Long.parseLong(fieldValue) / 1000); 
								
								record[timeFieldIndex] = fieldValue;
								gotFields[timeFieldIndex] = true;
							}
							catch (NumberFormatException e)
							{
								String message = String.format(
										"Cannot parse epoch ms timestamp '%s'",								
										fieldValue);
								logger.error(message);
								reporter.reportDateParseError(fieldValue);
								
								gotFields[timeFieldIndex] = false;
							}
						}
						else
						{
							Integer index = fieldMap.get(fieldName);
							if (index != null)
							{
								record[index] = fieldValue;
								gotFields[index] = true;
							}
						}
					}
					token = parser.nextToken();
				}

				if (gotFields[timeFieldIndex])
				{
					String missing = firstMissingField(allFields, gotFields); 
					if (missing != null)
					{
						reporter.reportMissingField(missing);
					}
					
					lengthEncodedWriter.writeRecord(record);
					recordsWritten++;				
				}
				else
				{
					logger.info("Missing time field from JSON document");
					reporter.reportMissingField(timeField);							
					recordsDiscarded++;
				}
				
				reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);
				
				token = parser.nextToken();
			}
		}
		else
		{
			DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
			
			while (token == JsonToken.START_OBJECT)
			{
				Arrays.fill(record, "");
				Arrays.fill(gotFields, false);

				while (token != JsonToken.END_OBJECT)
				{
					if (token == JsonToken.FIELD_NAME)
					{
						String fieldName = parser.getCurrentName();
						token = parser.nextToken();
						String fieldValue = parser.getText();

						if (fieldName.equals(timeField))
						{
							try
							{
								fieldValue = Long.toString(dateFormat.parse(fieldValue).getTime() / 1000);
								
								record[timeFieldIndex] = fieldValue;
								gotFields[timeFieldIndex] = true;
							}
							catch (ParseException e)
							{
								logger.error("Cannot parse '" + fieldValue +
										"' as a date using format string '" +
										dd.getTimeFormat() + "'");
																
								gotFields[timeFieldIndex] = false;
								reporter.reportDateParseError(fieldValue);
							}
						}
						else
						{
							Integer index = fieldMap.get(fieldName);
							if (index != null)
							{
								record[index] = fieldValue;
								gotFields[index] = true;
							}
						}
					}
					token = parser.nextToken();
				}

				if (gotFields[timeFieldIndex])
				{
					String missing = firstMissingField(allFields, gotFields); 
					if (missing != null)
					{
						reporter.reportMissingField(missing);
					}
					
					lengthEncodedWriter.writeRecord(record);
					recordsWritten++;				
				}
				else
				{
					logger.info("Missing time field from JSON document");
					reporter.reportMissingField(timeField);							
					recordsDiscarded++;
				}
				
				reporter.reportRecordsWritten(recordsWritten, recordsDiscarded);

				token = parser.nextToken();
			}
		}
		
		logger.debug("Transferred " + recordsWritten + " Json records to autodetect." );
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
