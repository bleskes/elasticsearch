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
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 */
	static public void pipeCsv(DataDescription dd, List<String> analysisFields,
		InputStream is, OutputStream os, Logger logger)
	throws IOException, MissingFieldException
	{	
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();	
		
		try (CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref))
		{
			String[] header = csvReader.getHeader(true);
			
			List<Pair<String, Integer>> fieldIndexes = 
					findFieldIndexes(header, dd.getTimeField(), analysisFields);
			
			Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
			while (iter.hasNext())
			{
				Pair<String, Integer> p = iter.next();
				if (p.Second < 0)
				{
					String msg = String.format("Field configured for analysis " 
							+ "'%s' is not in the CSV header '%s'", 
							p.First, Arrays.toString(header));
					logger.error(msg);

					throw new MissingFieldException(p.First, msg);
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
				lengthEncodedWriter.writeNumFields(numFields);				
				for (Pair<String, Integer> p : fieldIndexes)
				{
					lengthEncodedWriter.writeField(line.get(p.Second));
				}
			}
			
			lengthEncodedWriter.flush();
		}

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
	 * @param logger Errors are logged to this logger
	 * @throws IOException 
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 */
	static public void transformAndPipeCsv(DataDescription dd, List<String> analysisFields,
			InputStream is, OutputStream os, Logger logger)
	throws IOException, MissingFieldException
	{
		String timeField = dd.getTimeField();
		
		CsvPreference csvPref = new CsvPreference.Builder(
				dd.getQuoteCharacter(),
				dd.getFieldDelimiter(),
				new String(new char[] {DataDescription.LINE_ENDING})).build();	
		
		try (CsvListReader csvReader = new CsvListReader(new InputStreamReader(is), csvPref))
		{
			String[] header = csvReader.getHeader(true);			
			List<Pair<String, Integer>> fieldIndexes = 
					findFieldIndexes(header, timeField, analysisFields);
							
			Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
			while (iter.hasNext())
			{
				Pair<String, Integer> p = iter.next();
				if (p.Second < 0)
				{				
					String msg = String.format("Field configured for analysis " 
							+ "'%s' is not in the CSV header '%s'", 
								p.First, Arrays.toString(header));
					logger.error(msg);
			
					throw new MissingFieldException(p.First, msg);
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
				throw new MissingFieldException(timeField, message);
			}	
			

			// Don't close the output stream as it causes the autodetect 
			// process to quit
			LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);
			lengthEncodedWriter.writeRecord(header);

			List<String> line;
			String [] record = new String [fieldIndexes.size()];
			if (dd.isEpochMs())
			{
				while ((line = csvReader.read()) != null)
				{
					i = 0;
					for (Pair<String, Integer> p : fieldIndexes)
					{
						try
						{
							record[i] = line.get(p.Second);
						}
						catch (IndexOutOfBoundsException e)
						{
							logger.error("Not enough fields in csv record " 
									+ line, e);
							record[i] = "";
						}
						
						i++;
					}
					
					try
					{
						long epoch = Long.parseLong(record[timeFieldIndex]) / 1000; 
						record[timeFieldIndex] = new Long(epoch).toString();
					}
					catch (NumberFormatException e)
					{
						String message = String.format(
								"Cannot parse epoch ms timestamp '%s'",								
								line.get(timeFieldIndex));

						logger.error(message);						
						continue;
					}		
					
					lengthEncodedWriter.writeRecord(record);
				}
			}
			else
			{
				DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());

				while ((line = csvReader.read()) != null)
				{
					i = 0;
					for (Pair<String, Integer> p : fieldIndexes)
					{
						try
						{
							record[i] = line.get(p.Second);
						}
						catch (IndexOutOfBoundsException e)
						{
							logger.error("Not enough fields in csv record " 
									+ line, e);
							record[i] = "";
						}
						
						i++;
					}
					
					try
					{
						record[timeFieldIndex] =  new Long(dateFormat.parse(record[timeFieldIndex]).getTime() / 1000).toString();
					}
					catch (ParseException pe)
					{
						String message = String.format("Cannot parse date '%s' with format string '%s'",
								line.get(timeFieldIndex), dd.getTimeFormat());

						logger.error(message);
						continue;
					}		

					lengthEncodedWriter.writeRecord(record);
				}
			}
			
			
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
		List<String> headerList = Arrays.asList(header);
		
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
	 * @param logger Errors are logged to this logger 
	 * @throws JsonParseException 
	 * @throws IOException
	 */
	static public void transformAndPipeJson(DataDescription dd, 
			List<String> analysisFields, InputStream is, OutputStream os,
			Logger logger) 
	throws JsonParseException, IOException
	{
		JsonParser parser = new JsonFactory().createParser(is);
		
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

		if (dd.isTransformTime())
		{
			pipeJsonAndTransformTime(parser, analysisFields, os, dd, logger);
		}
		else
		{
			pipeJson(parser, dd.getTimeField(), analysisFields, os, logger);
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
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws JsonParseException
	 */
	static private void pipeJson(JsonParser parser, String timeField,
			List<String> analysisFields, OutputStream os,
			Logger logger)
	throws JsonParseException, IOException
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
				

		int recordCount = 0;
		JsonToken token = parser.nextToken();
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
				
				
				recordCount = 0;
			}
			token = parser.nextToken();
		}

		logMissingFields(allFields, gotFields, logger);

		// Each record consists of number of fields followed by length/value
		// pairs.  See CLengthEncodedInputParser.h in the C++ code for a more
		// detailed description.
		
		// write header and first record
		lengthEncodedWriter.writeRecord(allFields);
		lengthEncodedWriter.writeRecord(record);
		
		token = parser.nextToken();
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

			logMissingFields(allFields, gotFields, logger);
			
			lengthEncodedWriter.writeRecord(record);
			++recordCount;
			token = parser.nextToken();
		}

		logger.debug("Transferred " + recordCount + " Json records to autodetect.");
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
	 * @param logger Errors are logged to this logger
	 * @throws IOException
	 * @throws JsonParseException
	 */
	static private void pipeJsonAndTransformTime(JsonParser parser, 
			List<String> analysisFields, OutputStream os,
			DataDescription dd, Logger logger)
	throws JsonParseException, IOException
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
			
			
		int recordCount = 0;
		JsonToken token = parser.nextToken();
		while (token != JsonToken.END_OBJECT)
		{
			if (token == JsonToken.FIELD_NAME)
			{
				recordCount = 1;
				
				String fieldName = parser.getCurrentName();
				token = parser.nextToken();
				String fieldValue = parser.getText();

				if (timeField.equals(fieldName))
				{
					if (dd.isEpochMs())
					{
						try
						{
							fieldValue = Long.toString(Long.parseLong(fieldValue) / 1000); 
						}
						catch (NumberFormatException e)
						{
							String message = String.format(
									"Cannot parse epoch ms timestamp '%s'",								
									fieldValue);
							logger.error(message);
						}
					}
					else
					{
						try
						{
							DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
							fieldValue = Long.toString(dateFormat.parse(fieldValue).getTime() / 1000);
						}
						catch (ParseException e)
						{
							logger.error("Cannot parse '" + fieldValue +
									"' as a date using format string '" +
									dd.getTimeFormat() + "'");
						}
					}
				}
			
				Integer index = fieldMap.get(fieldName);
				if (index != null)
				{
					record[index] = fieldValue;
					gotFields[index] = true;
				}
			}
			
			token = parser.nextToken();
		}
		logMissingFields(allFields, gotFields, logger);


		// Each record consists of number of fields followed by length/value
		// pairs.  See CLengthEncodedInputParser.h in the C++ code for a more
		// detailed description.
		
		lengthEncodedWriter.writeRecord(allFields);
		lengthEncodedWriter.writeRecord(record);
		
		

		// is the timestamp a format string or epoch ms.
		if (dd.isEpochMs())
		{
			token = parser.nextToken();
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
							}
							catch (NumberFormatException e)
							{
								String message = String.format(
										"Cannot parse epoch ms timestamp '%s'",								
										fieldValue);
								logger.error(message);
							}
						}
						
						Integer index = fieldMap.get(fieldName);
						if (index != null)
						{
							record[index] = fieldValue;
							gotFields[index] = true;
						}
					}
					token = parser.nextToken();
				}

				logMissingFields(allFields, gotFields, logger);
				
				lengthEncodedWriter.writeRecord(record);
				++recordCount;
				token = parser.nextToken();
			}
		}
		else
		{
			DateFormat dateFormat = new SimpleDateFormat(dd.getTimeFormat());
			
			token = parser.nextToken();
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
							}
							catch (ParseException e)
							{
								logger.error("Cannot parse '" + fieldValue +
										"' as a date using format string '" +
										dd.getTimeFormat() + "'");
							}
						}

						Integer index = fieldMap.get(fieldName);
						if (index != null)
						{
							record[index] = fieldValue;
							gotFields[index] = true;
						}
					}
					token = parser.nextToken();
				}

				logMissingFields(allFields, gotFields, logger);
				
				lengthEncodedWriter.writeRecord(record);
				++recordCount;
				token = parser.nextToken();
			}
		}
		
		logger.debug("Transferred " + recordCount + " Json records to autodetect." );
	}
	
	/**
	 * Log an error when all the required fields could not be found
	 * 
	 * @param requiredFields
	 * @param gotFieldFlags
	 * @param logger
	 */
	static private void logMissingFields(List<String> requiredFields, 
			boolean [] gotFieldFlags, Logger logger)
	{		
		for (int i=0; i<gotFieldFlags.length; i++)
		{
			if (gotFieldFlags[i] == false)
			{
				logger.error("Missing field " + requiredFields.get(i));
			}
		}
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
