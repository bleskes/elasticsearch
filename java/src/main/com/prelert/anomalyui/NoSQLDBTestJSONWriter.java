/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.anomalyui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;




/**
 * Class to write out test anomaly score data into the JSON formats used by
 * the varous NoSQL databases used in tests.
 * @author Pete Harverson
 */
public class NoSQLDBTestJSONWriter
{
	
	static Logger s_Logger = Logger.getLogger(NoSQLDBTestJSONWriter.class);
	
	//public static String[] TEST_PROJECT_CODES = {"en-us", "es-es", "fr-ca","zh-ma"};
	public static String[] TEST_PROJECT_CODES = {"en", "es", "fr","zh_ma"};
	
	
	/**
	 * Writes out random anomaly data at hourly intervals to the JSON format 
	 * used by Elasticsearch.
	 * @param numPoints number of data points to write.
	 */
	@SuppressWarnings("unchecked")
	public void writeElasticsearchFormat(int numPoints)
	{
		ArrayList<JSONObject> indexPoints = new ArrayList<JSONObject>(numPoints);
		ArrayList<JSONObject> bucketPoints = new ArrayList<JSONObject>(numPoints);
		
		// { "index" : { "_index" : "wikipedia", "_type" : "proj1", "_id" : "1" } }
		// { "buckettime": "2013-09-01T01:00:00", "detectors": [ { "name": "individual metric/hitcount/project_code", "records": [ { "fieldname": "project_code", "fieldvalue": "lij.b", "probability": 3.4051, "anomalyfactor": 0.03} ] }] }

		
		JSONObject indexMap;
		JSONObject indexObj;
		
		JSONObject bucketsMap;
		
		JSONArray detectorsList;
		JSONObject detectorsObj;
		
		JSONArray recordsList;
		JSONObject recordsObj;
		
		DecimalFormat df = new DecimalFormat("0.00");
		
		// For the bucket time, space at hourly intervals from 01-01-13 (1356998400000 ms)
		// Put the date in a suitable format to be read by elasticsearch/kibana.
		Date bucketTime = new Date(1356998400000l);
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		for (int i = 1; i <= numPoints; i++)
		{
			indexMap = new JSONObject();
			
			indexObj = new JSONObject();
			indexObj.put("_index", "wikipedia");
			indexObj.put("_type", "proj1");
			indexObj.put("_id", "" + i);
			
			indexMap.put("index", indexObj);
			indexPoints.add(indexMap);
			
			
			bucketsMap = new JSONObject();
			
			// Add one detectors object, containing one record, for each bucket.
			detectorsList = new JSONArray();
			detectorsObj = new JSONObject();
			
			recordsList = createElasticsearchRecords(2);
			
			detectorsObj.put("name", "hitcount");
			detectorsObj.put("records", recordsList);
			detectorsList.add(detectorsObj);
			
			bucketsMap.put("buckettime", dateFormatter.format(bucketTime));
			bucketsMap.put("detectors", detectorsList);
			
			// TODO - work out how to query for nested values in elasticsearch.
			// For now, add probabilty and anomalyfactor at top-level too.
			// probability is the min prob of the records, anomalyfactor is the sum.
			double overallProb = Double.MAX_VALUE;
			double overallAnomalyfactor = 0;
			for (int j = 0; j < recordsList.size(); j++)
			{
				recordsObj = (JSONObject)(recordsList.get(j));
				overallProb = Math.min(overallProb, (Double)(recordsObj.get("probability")));
				overallAnomalyfactor += ((Double) recordsObj.get("anomalyfactor"));
			}

			overallProb = Double.parseDouble(df.format(overallProb));
			overallAnomalyfactor = Double.parseDouble(df.format(overallAnomalyfactor));
			
			bucketsMap.put("probability", overallProb);
			bucketsMap.put("anomalyfactor", overallAnomalyfactor);
			
			bucketPoints.add(bucketsMap);
			
			
			bucketTime = new Date(bucketTime.getTime() + (3600000l));
		}
	 
		try 
		{
			FileWriter file = new FileWriter("c:\\work\\hadoop\\elasticsearch_test.json");
			
			for (int i = 0; i < numPoints; i++)
			{
				file.write(indexPoints.get(i).toJSONString());
				file.write("\r\n");
				file.write(bucketPoints.get(i).toJSONString());
				file.write("\r\n");
				file.flush();
			}
			
			file.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		s_Logger.debug("Written " + numPoints + " rows of JSON data to c:\\work\\hadoop\\elasticsearch_test.json");
	}
	
	
	@SuppressWarnings("unchecked")
	private JSONArray createElasticsearchRecords(int numRecords)
	{
		JSONArray recordsList = new JSONArray();
		
		JSONObject recordsObj;
		DecimalFormat df = new DecimalFormat("0.00");
		Random r = new Random();
		
		for (int i = 0; i < numRecords; i++)
		{
			recordsObj = new JSONObject();
			
			
			int randomInt = r.nextInt(4);
			
			recordsObj.put("fieldname", "project_code");
			recordsObj.put("fieldvalue", TEST_PROJECT_CODES[randomInt]);
			
			double prob = Double.parseDouble(df.format(Math.random()));
			double anomalyfactor = Double.parseDouble(df.format(Math.random()));
			
			recordsObj.put("probability", prob);
			recordsObj.put("anomalyfactor", anomalyfactor);
			
			recordsList.add(recordsObj);
		}
		
		return recordsList;
	}
	
	
	/**
	 * Writes out random anomaly data at hourly intervals to the JSON format 
	 * used by Apache CouchDB.
	 * @param numPoints number of data points to write.
	 */
	@SuppressWarnings("unchecked")
	public void writeCouchDBFormat(int numPoints)
	{
		DecimalFormat df = new DecimalFormat("0.00");
		
		JSONObject anomalyRecord; 
		JSONArray records = new JSONArray();
		
		// For the bucket time, space at hourly intervals from 01-01-13 (1356998400000 ms)
		long timestamp = 1356998400000l;
		for (int i = 0; i < numPoints; i++)
		{
			double score = Double.parseDouble(df.format(Math.random()));
			
			anomalyRecord = new JSONObject();
			anomalyRecord.put("_id", Integer.toString(i));
			anomalyRecord.put("timestamp", timestamp);
			anomalyRecord.put("score", score);
			records.add(anomalyRecord);
			
			timestamp += 3600000l;
		}
		
		
		JSONObject jsonData = new JSONObject();
		jsonData.put("docs", records);
		
		
		try 
		{
			FileWriter file = new FileWriter("c:\\work\\hadoop\\couchdb_test.json");
			file.write(jsonData.toJSONString());
			file.write("\r\n");
			file.flush();
			file.close();
			s_Logger.debug("Written " + numPoints + " rows of JSON data to c:\\work\\hadoop\\couchdb_test.json");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Writes out random anomaly data at hourly intervals to the JSON format 
	 * used by the Prelert API results endpoint.
	 * @param numPoints number of data points to write.
	 */
	public void writePrelertAPIFormat(int numPoints)
	{
		DecimalFormat df = new DecimalFormat("0.00");
		
		ArrayList<AnomalyData> records = new ArrayList<AnomalyData>();
		
		// For the bucket time, space at hourly intervals from 01-01-13 (1356998400000 ms)
		AnomalyData record;
		long timestamp = 1356998400000l;
		Random countGenerator = new Random();
		for (int i = 0; i < numPoints; i++)
		{
			record = new AnomalyData();
			
			float score = Float.parseFloat(df.format(Math.random() * 100d));
			
			record.setTimestamp(timestamp);
			record.setScore(score);
			record.setRecordsCount(countGenerator.nextInt(100));
			records.add(record);
			record.setRecordsLink("http://pete-vaio:8080/api/job/20131024010002/results/" + timestamp/1000l + "?expand=records");
			
			timestamp += 3600000l;
		}
		
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			mapper.writeValue(new File("c:\\work\\hadoop\\prelertapi_test.json"), records);
			s_Logger.debug("Written " + numPoints + " rows of JSON data to c:\\work\\hadoop\\prelertapi_test.json");
		}
		catch (IOException e)
		{
			s_Logger.error("Error writing JSON data to c:\\work\\hadoop\\prelertapi_test.json", e);
		}

	}
	
	
	/**
	 * Reads in and parses a JSON input file in the original Wiki results format, and
	 * then outputs in the format used by the Prelert API.
	 * @param inputFilename name of the input JSON file.
	 * @param outputFilename name of the output JSON file.
	 */
	public void convertWikiToPrelertAPIFormat(String inputFilename, String outputFilename)
	{
		// Try and read the JSON contents of the file.
		JSONArray jsonArray = null;
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(inputFilename));
			JSONParser parser = new JSONParser();
			jsonArray = (JSONArray)(parser.parse(in));
			s_Logger.debug("convertWikiToPrelertAPIFormat() number of records parsed from file: " + jsonArray.size());
		}
		catch (IOException e)
		{
			s_Logger.error("convertWikiToPrelertAPIFormat() IO error reading anomaly file.", e);
		}
		catch (ParseException e)
		{
			s_Logger.error("convertWikiToPrelertAPIFormat() error parsing anomaly file. " +
					"Please check the file is valid JSON format.", e);
		}
		
		// Create AnomalyData objects from the items in the JSONArray.
		ArrayList<AnomalyData> chartData = new ArrayList<AnomalyData>();
		if (jsonArray != null)
		{
			JSONObject anomalyObj;
			AnomalyData anomalyData;
			for (int i = 0; i < jsonArray.size(); i++)
			{
				anomalyObj = (JSONObject)(jsonArray.get(i));
				anomalyData = processJSONWikiData(anomalyObj);
		        chartData.add(anomalyData);
			}
			
			s_Logger.debug("convertWikiToPrelertAPIFormat() number of anomaly points successfully processed: " + chartData.size());
		}

		// Sort the anomaly records in each AnomalyData object in ascending order of probability.
		for (AnomalyData anomalyData : chartData)
		{
	        Collections.sort(anomalyData.getRecords(), new Comparator<AnomalyRecord>(){
	
				@Override
				public int compare(AnomalyRecord record1, AnomalyRecord record2)
				{
					return Float.compare(record1.getProbability(), record2.getProbability());
				}
	        	
	        });
		}
		
		// Convert list of AnomalyData objects into JSON formatted string and save to file.
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			mapper.writeValue(new File(outputFilename), chartData);
			s_Logger.debug("convertWikiToPrelertAPIFormat() saved JSON data in Prelert API format to " + outputFilename);
		}
		catch (Exception e)
		{
			s_Logger.error("convertWikiToPrelertAPIFormat() error saving JSON data to " + outputFilename, e);
		}

	}
	
	
	/**
	 * Processes the specified <code>JSONObject</code> containing properties from
	 * the Wiki tests into an <code>AnomalyData</code> object.
	 * @param anomalyObj <code>JSONObject</code> to process.
	 * @return <code>AnomalyData</code> object.
	 */
	private AnomalyData processJSONWikiData(JSONObject anomalyObj)
	{
		AnomalyData anomalyData = new AnomalyData();
		
		float anomalyScore = 0;
		
		Long bucketTimeSecs = (Long)(anomalyObj.get("buckettime"));
		Date bucketTime = new Date(bucketTimeSecs * 1000l);
		anomalyData.setTime(bucketTime);
		JSONArray detectors = (JSONArray)(anomalyObj.get("detectors"));
		
		JSONObject detector;
		JSONArray jsonRecords;
		JSONObject jsonRecord;
		AnomalyRecord record;
		String fieldName;
		ArrayList<AnomalyRecord> records = new ArrayList<AnomalyRecord>();
		
		// Sum the anomalyfactor over all records in all detectors.
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> detectorsIterator = detectors.iterator();
        while (detectorsIterator.hasNext()) 
        {
        	detector = detectorsIterator.next();
        	jsonRecords = (JSONArray)(detector.get("records"));
        	
        	@SuppressWarnings("unchecked")
			Iterator<JSONObject> recordIterator = jsonRecords.iterator();
	        while (recordIterator.hasNext()) 
	        {
	        	jsonRecord = recordIterator.next();
	        	fieldName = (String)(jsonRecord.get("fieldname"));
	        	
	        	// Don't process the count records.
	        	// They are there to guarantee a record in every bucket.
	        	if (fieldName != null && fieldName.equals("count") == false)
	        	{
	        		try
		        	{
			        	record = processWikiAnomalyRecord(jsonRecord);
			        	record.setTime(bucketTime);
			        	anomalyScore += record.getAnomalyFactor();
			        	records.add(record);
		        	}
		        	catch (NumberFormatException e)
		        	{
		        		s_Logger.warn("processJSONAnomalyData() error parsing fields that are " +
		        				"expected to hold numeric contents", e);
		        	}
	        	}
	        }
        }
        
        anomalyData.setScore(anomalyScore);
        anomalyData.setRecords(records);
        anomalyData.setRecordsCount(records.size());
        
        return anomalyData;
	}
	
	
	/**
	 * Processes the specified <code>JSONObject</code> containing fields from the
	 * Wiki data test into an <code>AnomalyRecord</code> object.
	 * @param jsonRecord <code>JSONObject</code> to process.
	 * @return <code>AnomalyRecord</code> object.
	 * @throws NumberFormatException if expected numeric fields do not contain numeric values.
	 */
	private AnomalyRecord processWikiAnomalyRecord(JSONObject jsonRecord) throws NumberFormatException
	{
		AnomalyRecord record = new AnomalyRecord();
		
		record.setFieldName((String)(jsonRecord.get("fieldname")));
		record.setFieldValue((String)(jsonRecord.get("fieldvalue")));
		
		Number anomalyFactor = (Number)(jsonRecord.get("anomalyfactor"));
    	record.setAnomalyFactor(anomalyFactor.floatValue());
    	
    	Number prob = (Number)(jsonRecord.get("probability"));
    	record.setProbability(prob.floatValue());
    	
    	record.setMetricField((String)(jsonRecord.get("metricfield")));
    	
    	Number currentMean = (Number)(jsonRecord.get("currentmean"));
    	record.setCurrentMean(currentMean.floatValue());

    	Number baselineMean = (Number)(jsonRecord.get("baselinemean"));
    	record.setBaselineMean(baselineMean.floatValue());
		
		return record;
	}
	
	
	public static void main(String[] args)
	{
    	NoSQLDBTestJSONWriter jsonWriter = new NoSQLDBTestJSONWriter();
    	//jsonWriter.writeElasticsearchFormat(1700);
    	//jsonWriter.writeCouchDBFormat(1700);
    	//jsonWriter.writePrelertAPIFormat(1700);
    	jsonWriter.convertWikiToPrelertAPIFormat("C:\\Work\\hadoop\\hitcountbyurl_day.json\\output_6hours.json",
    			"C:\\Work\\hadoop\\hitcountbyurl_day.json\\prelertapi_6hours.json");
	}
}
