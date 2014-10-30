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

package com.prelert.anomalyui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Spring <code>Controller</code> for handling requests from the Anomaly UI
 * web application, processing anomaly data held in uploaded JSON files.
 * @author Pete Harverson
 */
@Controller
public class AnomalyUIController
{
	static Logger s_Logger = Logger.getLogger(AnomalyUIController.class);
	
	/** Maximum number of records before adjacent buckets will be aggregated
	 * before returning to the client.
	 */
	public static final int MAXIMUM_RECORDS_NO_AGGREGATION = 500;
	
	private ElasticsearchClient m_ElasticsearchClient;
	
	
	@Autowired
	public void setElasticsearchClient(ElasticsearchClient esClient) 
	{
		m_ElasticsearchClient = esClient;
		s_Logger.info("Using elasticsearchclient on host " + 
				m_ElasticsearchClient.getHostname() + ":" + m_ElasticsearchClient.getPort());
	}
	
	
	/**
	 * Processes request of an uploaded JSON file containing fields
	 * from the Wiki data tests.
	 * @param uploadFile JSON file containing anomaly data.
	 * @return <code>ModelAndView</code> for the JSP displaying the results
	 * contained in the uploaded JSON file.
	 */
	@RequestMapping(value = "/processWikiData", method = RequestMethod.POST)
    public ModelAndView processWikiData(UploadAnomalyFile uploadFile) 
	{
		s_Logger.debug("processWikiData()");
		
		CommonsMultipartFile anomalyFile = uploadFile.getFileData();
		
		s_Logger.debug("processWikiData(), processing uploaded file " + anomalyFile.getOriginalFilename());
		
		ModelAndView model = new ModelAndView("AnomalyResultsChart");
		model.addObject("fileName", anomalyFile.getOriginalFilename());
		
		
		// Try and read the JSON contents of the file.
		JSONArray jsonArray = null;
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(anomalyFile.getInputStream()));
			JSONParser parser = new JSONParser();
			jsonArray = (JSONArray)(parser.parse(in));
			s_Logger.debug("processWikiData() number of records parsed from file: " + jsonArray.size());
		}
		catch (IOException e)
		{
			// TODO - return error message / redirect to error page.
			s_Logger.error("processWikiData() IO error reading anomaly file.", e);
		}
		catch (ParseException e)
		{
			// TODO - return error message / redirect to error page.
			s_Logger.error("processWikiData() error parsing anomaly file. " +
					"Please check the file is valid JSON format.", e);
		}
		
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
			
			s_Logger.debug("processWikiData() number of anomaly points successfully processed: " + chartData.size());
			
			// Aggregate data if total size of JSON data is greater than limit.
			if (chartData.size() > MAXIMUM_RECORDS_NO_AGGREGATION)
			{
				chartData = aggregateChartData(chartData, false);
				s_Logger.debug("processWikiData() number of aggregated anomaly points: " + chartData.size());
			}
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
		
		model.addObject("chartData", chartData);
		if (chartData.size() > 1)
		{
			// Store the span between adjacent point. Assume regular spacing.
			long span = chartData.get(1).getTimestamp() - chartData.get(0).getTimestamp();
			model.addObject("span", span);
		}
 
		return model;
    }
	
	
	/**
	 * Processes request of an uploaded JSON file in the format
	 * generated by the Prelert API results endpoint.
	 * @param uploadFile JSON file containing anomaly data.
	 * @param viewName name of the View to render, to be resolved by 
	 * the DispatcherServlet's ViewResolver.
	 * @return <code>ModelAndView</code> for the JSP displaying the results
	 * contained in the uploaded JSON file.
	 */
	@RequestMapping(value = "/processAPIData", method = RequestMethod.POST)
    public ModelAndView processAPIData(UploadAnomalyFile uploadFile, 
    		@RequestParam(value = "viewName", required=false, defaultValue="AnomalyResultsChart") String viewName) 
	{
		s_Logger.debug("processAPIData()");
		
		CommonsMultipartFile anomalyFile = uploadFile.getFileData();
		
		s_Logger.debug("processAPIData(), processing uploaded file " + anomalyFile.getOriginalFilename());
		
		ModelAndView model = new ModelAndView(viewName);
		model.addObject("fileName", anomalyFile.getOriginalFilename());
		
		ArrayList<AnomalyData> chartData = new ArrayList<AnomalyData>();
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			chartData = mapper.readValue(anomalyFile.getInputStream(), 
					new TypeReference<ArrayList<AnomalyData>>() { });
			s_Logger.debug("processAPIData() number of anomaly points read from JSON file: " + chartData.size());
		}

		catch (IOException e)
		{
			s_Logger.error("processAPIData() error reading anomaly records from JSON file. " +
					"Please check the file is valid JSON format for the Prelert API.", e);
		}
		
		// Aggregate data if total size of JSON data is greater than limit.
		if (chartData.size() > MAXIMUM_RECORDS_NO_AGGREGATION)
		{
			chartData = aggregateChartData(chartData, false);
			s_Logger.debug("processAPIData() number of aggregated anomaly points: " + chartData.size());
		}
		
		model.addObject("chartData", chartData);
		if (chartData.size() > 1)
		{
			// Store the span between adjacent point. Assume regular spacing.
			long span = chartData.get(1).getTimestamp() - chartData.get(0).getTimestamp();
			model.addObject("span", span);
		}
 
		return model;
    }
	
	
	/**
	 * Processes a request to view the results from the job whose 
	 * anomaly data are stored in a JSON file on the server.
	 * @param fileName the name of the JSON file containing data in the Prelert API
	 * 	format whose results are to be viewed.
	 * @param viewName name of the View to render, to be resolved by 
	 * the DispatcherServlet's ViewResolver.
	 * @return <code>ModelAndView</code> for the JSP displaying the results
	 * contained in the requested JSON file.
	 */
	@RequestMapping(value = "/jobResults")
    public ModelAndView getJobResults(@RequestParam(value = "fileName", required=true) String fileName, 
    		@RequestParam(value = "viewName", required=false, defaultValue="AnomalyResultsChart") String viewName) 
	{
		s_Logger.debug("getJobResults(), view results from file " + fileName);
		
		ModelAndView model = new ModelAndView(viewName);
		model.addObject("fileName", fileName);
		
		ArrayList<AnomalyData> chartData = new ArrayList<AnomalyData>();
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			
			chartData = mapper.readValue(new File("C:\\Work\\prelert_api\\" + fileName), 
					new TypeReference<ArrayList<AnomalyData>>() { });
			s_Logger.debug("getJobResults() number of anomaly points read from JSON file: " + chartData.size());
		}

		catch (IOException e)
		{
			s_Logger.error("getJobResults() error reading anomaly records from JSON file. " +
					"Please check the file is valid JSON format for the Prelert API.", e);
		}
		
		// Aggregate data if total size of JSON data is greater than limit.
		if (chartData.size() > MAXIMUM_RECORDS_NO_AGGREGATION)
		{
			chartData = aggregateChartData(chartData, false);
			s_Logger.debug("getJobResults() number of aggregated anomaly points: " + chartData.size());
		}
		
		model.addObject("chartData", chartData);
		if (chartData.size() > 1)
		{
			// Store the span between adjacent point. Assume regular spacing.
			long span = chartData.get(1).getTimestamp() - chartData.get(0).getTimestamp();
			model.addObject("span", span);
		}
 
		return model;
    }
	
	
	/**
	 * 
	 * @param jobId
	 * @param viewName
	 * @return
	 */
	@RequestMapping(value = "/viewElasticsearchJob")
    public ModelAndView getElasticsearchJobResults(@RequestParam(value = "jobId", required=true) String jobId, 
    		@RequestParam(value = "viewName", required=false, defaultValue="ElasticsearchJobResults") String viewName) 
	{
		s_Logger.debug("getElasticsearchJobResults(), view results for job ID " + jobId);
		
		ModelAndView model = new ModelAndView(viewName);
		model.addObject("jobId", jobId);
		
		ArrayList<AnomalyData> chartData = new ArrayList<AnomalyData>();
		try
		{
			chartData = m_ElasticsearchClient.getJobResults(jobId);
			s_Logger.debug("getElasticsearchJobResults() number of anomaly points obtained for jobId " + 
					jobId + " : " + chartData.size());
		}

		catch (Exception e)
		{
			s_Logger.error("getElasticsearchJobResults() querying elasticsearch for results for job ID " + jobId, e);
		}
		
		// Aggregate data if total size of JSON data is greater than limit.
		if (chartData.size() > MAXIMUM_RECORDS_NO_AGGREGATION)
		{
			chartData = aggregateChartData(chartData, false);
			s_Logger.debug("getJobResults() number of aggregated anomaly points: " + chartData.size());
		}
		
		// Store the time of the first point and the span between adjacent points.
		// NB. This assumes regular spacing between buckets with no gaps.
		model.addObject("chartData", chartData);
		if (chartData.size() > 0)
		{
			model.addObject("startTimeMs", chartData.get(0).getTimestamp());
		}
		
		if (chartData.size() > 1)
		{
			long span = chartData.get(1).getTimestamp() - chartData.get(0).getTimestamp();
			model.addObject("span", span);
		}
 
		return model;
    }
	
	
	@RequestMapping(value="/viewElasticsearchBucket/{jobId}/{bucketTime}")
    @ResponseBody
    public List<AnomalyRecord> getElasticsearchBucketRecords(@PathVariable("jobId") String jobId,
    		@PathVariable("bucketTime") Long bucketTime)
	{
		// http://prelert-server:port/api/jobs/<jobId>/results/<buckettime>/records
		s_Logger.debug("getElasticsearchBucketRecords() for jobId: " + jobId + ", bucketTime: " + bucketTime);
		
		List<AnomalyRecord> records = new ArrayList<AnomalyRecord>();
		try
		{
			String bucketId = "" + bucketTime/1000l;
			records = m_ElasticsearchClient.getBucketRecords(jobId, "" + bucketId);
			s_Logger.debug("getElasticsearchJobResults() number of anomaly records obtained for jobId: " + 
					jobId + ", bucketId : " + bucketId + " : " + records.size());
		}

		catch (Exception e)
		{
			s_Logger.error("getElasticsearchBucketRecords() querying elasticsearch for bucket records for job ID " + jobId, e);
		}
		
		// Sort the anomaly records in ascending order of probability.
        Collections.sort(records, new Comparator<AnomalyRecord>(){

			@Override
			public int compare(AnomalyRecord record1, AnomalyRecord record2)
			{
				return Float.compare(record1.getProbability(), record2.getProbability());
			}
        	
        });
		
        return records;
    }
	
	
	@RequestMapping(value="/anomalyRecords/{bucketTime}")
    @ResponseBody
    public ArrayList<AnomalyRecord> getAnomalyRecords(@PathVariable("bucketTime") Long bucketTime)
	{
		s_Logger.debug("getAnomalyRecords() for bucketTime: " + bucketTime);
		
		// Just return some dummy hardcoded data for now.
		String[] dummyFieldValues = {"Australia_Day", "The_Land_Before_Time", "Chechnya", "Million_Dollar_Baby", "Survivor_(TV_series)",
				"Mischa_Barton", "Moscow", "Yosemite_National_Park", "REM", "Benedict_Cumberbatch"};
		ArrayList<AnomalyRecord> records = new ArrayList<AnomalyRecord>();
		for (int i = 0; i < 10; i++)
		{
			AnomalyRecord testRecord = new AnomalyRecord();
			testRecord.setTime(new Date(bucketTime));
			testRecord.setFieldName("url");
			testRecord.setFieldValue(dummyFieldValues[i]);
			
			testRecord.setProbability((float)(Math.random() * 4d));
			testRecord.setAnomalyFactor(0.025f);
			testRecord.setMetricField("hitcount");
			records.add(testRecord);
		}
		
		// Sort the anomaly records in ascending order of probability.
        Collections.sort(records, new Comparator<AnomalyRecord>(){

			@Override
			public int compare(AnomalyRecord record1, AnomalyRecord record2)
			{
				return Float.compare(record1.getProbability(), record2.getProbability());
			}
        	
        });
		
        return records;
    }

	
	/**
	 * Aggregates the supplied chart data so that the number of points in the final
	 * list is within the {@link #MAXIMUM_RECORDS_NO_AGGREGATION} limit defined in this class.
	 * A simple summation of the scores in the individual records is performed to
	 * provide the score in the aggregated data.
	 * @param chartData list of AnomalyData points to be aggregated.
	 * @param aggregateRecords flag to indicate whether the supplied 
	 * 	data contains records that need to be aggregated.
	 * @return list of aggregated data.
	 */
	protected ArrayList<AnomalyData> aggregateChartData(ArrayList<AnomalyData> chartData, boolean aggregateRecords)
	{
		// TODO - initially assume points in the JSON file are all at regular intervals.
		// Later can handle irregular intervals between records if necessary.
		int totalDataSize = chartData.size();
		int numBucketsPerPoint = (int)(Math.ceil((double)totalDataSize/(double)MAXIMUM_RECORDS_NO_AGGREGATION));
		
		int counter = 0;
		AnomalyData anomalyData;
		ArrayList<AnomalyData> aggregateData = new ArrayList<AnomalyData>();
		AnomalyData firstBucket;
		float aggScore;
		ArrayList<AnomalyRecord> records = new ArrayList<AnomalyRecord>();
		int numLoops;
		while (counter < totalDataSize)
		{
			firstBucket = chartData.get(counter);
			if (aggregateRecords == true)
			{
				records = firstBucket.getRecords();
			}
			aggScore = firstBucket.getScore();
			numLoops = Math.min(numBucketsPerPoint, (totalDataSize - counter));
			
			for (int i = 1; i < numLoops; i++)
			{
				anomalyData = chartData.get(counter + i);
				aggScore += anomalyData.getScore();
				if (aggregateRecords == true)
				{
					records.addAll(anomalyData.getRecords());
				}
			}
			firstBucket.setScore(aggScore);
			counter+=numLoops;
			aggregateData.add(firstBucket);
		}
		
		return aggregateData;
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
	
	
	
	
}
