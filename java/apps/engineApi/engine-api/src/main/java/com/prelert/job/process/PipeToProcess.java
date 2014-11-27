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
import java.nio.charset.StandardCharsets;
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
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.input.CountingInputStream;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.dateparsing.CannotParseTimestampException;
import com.prelert.job.process.dateparsing.DateFormatDateTransformer;
import com.prelert.job.process.dateparsing.DateTransformer;
import com.prelert.job.process.dateparsing.DoubleDateTransformer;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;


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

	private PipeToProcess()
	{
	}

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
	 * @param statusReporter
	 * @param logger Errors are logged to this logger
	 * @return The number of records written to the outputstream
	 * @throws IOException
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws HighProportionOfBadTimestampsException If a large proportion
	 * of the records read have missing fields
	 * @throws OutOfOrderRecordsException
	 */
	public static void pipeCsv(DataDescription dd, AnalysisConfig analysisConfig,
		InputStream is, OutputStream os, StatusReporter statusReporter,
		JobDataPersister dataPersister,	Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
				OutOfOrderRecordsException
	{
        transformAndPipeCsv(dd, analysisConfig, is, os, statusReporter, dataPersister,
                logger, new DoubleDateTransformer(false));
	}


	private static void transformAndPipeCsv(DataDescription dd, AnalysisConfig analysisConfig,
	        InputStream is, OutputStream os, StatusReporter statusReporter,
	        JobDataPersister dataPersister, Logger logger, DateTransformer dateTransformer)
	    throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
	                OutOfOrderRecordsException
	    {
	        CsvPreference csvPref = new CsvPreference.Builder(
	                dd.getQuoteCharacter(),
	                dd.getFieldDelimiter(),
	                new String(new char[] {DataDescription.LINE_ENDING})).build();

	        int recordsWritten = 0;
	        int lineCount = 0;

	        List<String> analysisFields = analysisConfig.analysisFields();
	        statusReporter.setAnalysedFieldsPerRecord(analysisFields.size());

	        CountingInputStream countingStream = new CountingInputStream(is,
	                statusReporter);


	        // Don't close the output stream as it causes the autodetect
	        // process to quit
	        LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);

	        try (CsvListReader csvReader = new CsvListReader(
	                new InputStreamReader(countingStream, StandardCharsets.UTF_8),
	                csvPref))
	        {
	            String[] header = csvReader.getHeader(true);
	            long inputFieldCount = Math.max(header.length - 1, 0); // time fields doesn't count


	            List<Pair<String, Integer>> fieldIndexes =
	                    findFieldIndexes(header, dd.getTimeField(), analysisFields,
	                    logger);

	            int maxIndex = 0;
	            Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
	            while (iter.hasNext())
	            {
	                Pair<String, Integer> p = iter.next();

	                if (p.m_Second > maxIndex)
	                {
	                    maxIndex = p.m_Second;
	                }

	                if (p.m_Second < 0)
	                {
	                    String msg = String.format("Field configured for analysis "
	                            + "'%s' is not in the CSV header '%s'",
	                            p.m_First, Arrays.toString(header));
	                    logger.error(msg);

	                    throw new MissingFieldException(p.m_First, msg);
	                }
	            }

	            String [] filteredHeader = new String [fieldIndexes.size()];
	            int i=0;
	            for (Pair<String, Integer> p : fieldIndexes)
	            {
	                filteredHeader[i++] = p.m_First;
	            }


	            int timeFieldIndex = Arrays.asList(filteredHeader).indexOf(dd.getTimeField());
	            if (timeFieldIndex < 0)
	            {
	                String message = String.format("Cannot find timestamp field '%s'"
	                        + " in CSV header '%s'", dd.getTimeField(), Arrays.toString(filteredHeader));
	                logger.error(message);
	                throw new MissingFieldException(dd.getTimeField(), message);
	            }

	            dataPersister.setFieldMappings(analysisConfig.fields(),
	                    analysisConfig.byFields(), analysisConfig.overFields(),
	                    analysisConfig.partitionFields(), filteredHeader);

	            lengthEncodedWriter.writeRecord(filteredHeader);


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

	                    Arrays.fill(record, "");
	                    for (Pair<String, Integer> p : fieldIndexes)
	                    {
	                        if (p.m_Second >= line.size())
	                        {
	                            statusReporter.reportMissingField();
	                            i++;
	                            continue;
	                        }

	                        String field = line.get(p.m_Second);
	                        record[i] = (field == null) ? "" : field;
	                        i++;
	                    }
	                }
	                else
	                {
	                    for (Pair<String, Integer> p : fieldIndexes)
	                    {
	                        String field = line.get(p.m_Second);
	                        record[i] = (field == null) ? "" : field;
	                        i++;
	                    }
	                }

	                try
	                {
	                    // parse as a double and throw away the fractional
	                    // component
	                    long epoch = dateTransformer.transform(record[timeFieldIndex]);

	                    if (epoch < lastEpoch)
	                    {
	                        // out of order
	                        statusReporter.reportOutOfOrderRecord(inputFieldCount);
	                    }
	                    else
	                    {   // write record
	                        record[timeFieldIndex] = Long.toString(epoch);
	                        lengthEncodedWriter.writeRecord(record);
	                        dataPersister.persistRecord(epoch, record);

	                        statusReporter.reportRecordWritten(inputFieldCount);

	                        recordsWritten++;
	                        lastEpoch = epoch;
	                    }

	                }
	                catch (CannotParseTimestampException e)
	                {
	                    statusReporter.reportDateParseError(inputFieldCount);
	                    logger.error(e.getMessage());
	                }
	            }

	            // This function can throw and the exceptions thrown
	            statusReporter.finishReporting();

	            lengthEncodedWriter.flush();
	        }
	        finally
	        {
	            // nothing in this finally block should throw
	            // as it would suppress any exceptions from the try block
	            dataPersister.flushRecords();
	        }

	        logger.debug(String.format("Transferred %d of %d CSV records to autodetect.",
	                recordsWritten, lineCount));
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
	 * @param statusReporter
	 * @param usageReporter
	 * @param logger Errors are logged to this logger
	 * @return The number of records written to the outputstream
	 * @throws IOException
	 * @throws MissingFieldException If any fields are missing from the CSV header
	 * @throws HighProportionOfBadTimestampsException If a large proportion
	 * of the records read have missing fields or unparseable date formats
	 * @throws OutOfOrderRecordsException
	 */
	public static void transformAndPipeCsv(DataDescription dd, AnalysisConfig analysisConfig,
			InputStream is, OutputStream os, StatusReporter statusReporter,
			JobDataPersister dataPersister,	Logger logger)
	throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
	OutOfOrderRecordsException
	{
        DateTransformer dateTransformer = dd.isEpochMs() ? new DoubleDateTransformer(true) :
                new DateFormatDateTransformer(dd.getTimeFormat());
	    transformAndPipeCsv(dd, analysisConfig, is, os, statusReporter, dataPersister, logger, dateTransformer);
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
	private static List<Pair<String, Integer>> findFieldIndexes(
			String [] header, String timeField, List<String> analysisFields,
			Logger logger)
	{
		List<String> headerList = Arrays.asList(header);  // TODO header could be empty

		List<Pair<String, Integer>> fieldIndexes = new ArrayList<>();

		// time field
		Pair<String, Integer> p = new Pair<>(timeField,
				headerList.indexOf(timeField));
		fieldIndexes.add(p);
		logger.info("Index of field " + p.m_First + " is " + p.m_Second);

		for (String field : analysisFields)
		{
			p = new Pair<>(field, headerList.indexOf(field));
			fieldIndexes.add(p);
			logger.info("Index of field " + p.m_First + " is " + p.m_Second);
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
	public static void transformAndPipeJson(DataDescription dd,
			AnalysisConfig analysisConfig, InputStream is, OutputStream os,
			StatusReporter statusReporter,
			JobDataPersister dataPersister, Logger logger)
	throws JsonParseException, IOException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		CountingInputStream countingStream = new CountingInputStream(is,
				statusReporter);

		statusReporter.setAnalysedFieldsPerRecord(analysisConfig.analysisFields().size());

		try (JsonParser parser = new JsonFactory().createParser(countingStream))
		{
			if (dd.isTransformTime())
			{
                DateTransformer dateTransformer = dd.isEpochMs() ? new DoubleDateTransformer(true) :
                        new DateFormatDateTransformer(dd.getTimeFormat());
                pipeJsonAndTransformTime(parser, dd.getTimeField(), analysisConfig, os,
                        statusReporter, dataPersister, logger, dateTransformer);
			}
			else
			{
			    pipeJsonAndTransformTime(parser, dd.getTimeField(), analysisConfig, os,
						statusReporter, dataPersister, logger, new DoubleDateTransformer(false));
			}

			os.flush();
			// this line can throw and will be propagated
			statusReporter.finishReporting();
		}
		finally
		{
			// nothing in this finally block should throw
			// as it would suppress any exceptions from the try block
			dataPersister.flushRecords();
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
	private static void pipeJsonAndTransformTime(JsonParser parser, String timeField,
			AnalysisConfig analysisConfig, OutputStream os,
			StatusReporter reporter,
			JobDataPersister dataPersister, Logger logger, DateTransformer dateTransformer)
	throws JsonParseException, IOException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException
	{
		LengthEncodedWriter lengthEncodedWriter = new LengthEncodedWriter(os);

		List<String> analysisFields = analysisConfig.analysisFields();

		// record is all the analysis fields + the time field
		String [] allFields = new String [analysisFields.size() +1];

		int i=0;
		for (String s : analysisFields)
		{
			allFields[i] = s;
			i++;
		}

		int timeFieldIndex = allFields.length -1;
		allFields[timeFieldIndex] = timeField;
		// time field is the last item

		String [] record = new String[allFields.length];
		boolean [] gotFields = new boolean[record.length];

		Map<String, Integer> fieldMap = new HashMap<>();
		for (i = 0; i < allFields.length; i++)
		{
			fieldMap.put(allFields[i], new Integer(i));
		}

		dataPersister.setFieldMappings(analysisConfig.fields(),
				analysisConfig.byFields(), analysisConfig.overFields(),
				analysisConfig.partitionFields(), allFields);


		// write header and first record
		lengthEncodedWriter.writeRecord(allFields);

		int recordsWritten = 0;
		long inputFieldCount = readJsonRecord(parser, record, fieldMap, gotFields, logger);
		int recordCount = (inputFieldCount > 0) ? 1 : 0; // if at least one field consider it a record

		inputFieldCount = Math.max(inputFieldCount - 1, 0); // time fields doesn't count

		if (gotFields[timeFieldIndex])
		{
			long missing = missingFieldCount(gotFields);
			if (missing > 0)
			{
				reporter.reportMissingFields(missing);
			}

			try
			{
				// parse as a double and throw away the fractional
				// component
				long epoch = dateTransformer.transform((record[timeFieldIndex]));
				record[timeFieldIndex] = new Long(epoch).toString();

				lengthEncodedWriter.writeRecord(record);
				dataPersister.persistRecord(epoch, record);
				recordsWritten++;
				reporter.reportRecordWritten(inputFieldCount);
			}
			catch (CannotParseTimestampException e)
			{
				reporter.reportDateParseError(inputFieldCount);
				logger.error(e.getMessage());
			}

		}
		else
		{
			logger.warn("Missing time field from JSON document");
			reporter.reportMissingField();
		}

		long lastEpoch = 0;
		inputFieldCount = readJsonRecord(parser, record, fieldMap, gotFields, logger);
		while (inputFieldCount > 0)
		{
			inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count

			if (gotFields[timeFieldIndex])
			{
				long missing = missingFieldCount(gotFields);
				if (missing > 0)
				{
					reporter.reportMissingFields(missing);
				}

				try
				{
					// parse as a double and throw away the fractional
					// component
					long epoch = dateTransformer.transform(record[timeFieldIndex]);
					if (epoch < lastEpoch)
					{
						// out of order
						reporter.reportOutOfOrderRecord(inputFieldCount);
					}
					else
					{	// write record
						record[timeFieldIndex] = Long.toString(epoch);
						lengthEncodedWriter.writeRecord(record);
						dataPersister.persistRecord(epoch, record);

						recordsWritten++;
						reporter.reportRecordWritten(inputFieldCount);
						lastEpoch = epoch;
					}
				}
				catch (CannotParseTimestampException e)
				{
					reporter.reportDateParseError(inputFieldCount);
					logger.error(e.getMessage());
				}
			}
			else
			{
				logger.warn("Missing time field from JSON document");
				reporter.reportMissingField();
			}

			++recordCount;

			inputFieldCount = readJsonRecord(parser, record, fieldMap, gotFields, logger);
		}

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
	 * @param gotFields boolean array each element is true if that field
	 * was read
	 * @param logger Errors are logged to this logger
	 *
	 * @return The number of fields in the JSON doc
	 * @throws IOException
	 * @throws JsonParseException
	 */
	private static long readJsonRecord(JsonParser parser, String [] record,
			Map<String, Integer> fieldMap,
			boolean [] gotFields, Logger logger)
	throws JsonParseException, IOException
	{
		Arrays.fill(gotFields, false);
		Arrays.fill(record, "");

		int nestedLevel = 0;
		Deque<String> stack = new ArrayDeque<String>();

		long fieldCount = 0;
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
					++fieldCount;

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

		return fieldCount;
	}


	/**
	 * Return the number of missing fields
	 *
	 * @param requiredFields
	 * @param gotFieldFlags
	 * @return
	 */
	private static long missingFieldCount(boolean [] gotFieldFlags)
	{
		long count = 0;

		for (int i=0; i<gotFieldFlags.length; i++)
		{
			if  (gotFieldFlags[i] == false)
			{
				++count;
			}
		}

		return count;
	}

	/**
	 * Generic helper class
	 *
	 * @param <T>
	 * @param <U>
	 */
	private static class Pair<T,U>
	{
	    public final T m_First;
	    public final U m_Second;
	    public Pair(T first, U second)
	    {
	    	this.m_First = first;
	    	this.m_Second = second;
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
	public static void pipe(InputStream is, OutputStream os)
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
