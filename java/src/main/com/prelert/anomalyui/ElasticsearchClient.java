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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;



/**
 * Elasticsearch client which connects remotely to an elasticsearch cluster 
 * using the elasticsearch transport module.
 * @author Pete Harverson
 */
public class ElasticsearchClient
{

	static Logger s_Logger = Logger.getLogger(ElasticsearchClient.class);
	
	private String m_Hostname;
	private int m_Port;
	
	private Client m_Client;
	
	
	public ElasticsearchClient()
	{
		
	}
	
	
	public ElasticsearchClient(String hostname, int port)
	{
		setHostname(hostname);
		setPort(port);
		
		initializeClient();
	}
	
	
	public String getHostname()
	{
		return m_Hostname;
	}

	
	public void setHostname(String hostname)
	{
		m_Hostname = hostname;
	}

	
	public int getPort()
	{
		return m_Port;
	}

	
	public void setPort(int port)
	{
		m_Port = port;
	}
	
	
	public void initializeClient()
	{
		m_Client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(m_Hostname, m_Port));
		s_Logger.info("Initialized elasticsearch TransportClient to: host " + m_Hostname + 
				" on port " + m_Port);
	}
	

	public ArrayList<AnomalyData> getJobResults(String jobId)
	{
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes("bucket")		
				.setFrom(0).setSize(100000)
				//.addSort("timestamp", SortOrder.ASC)
				.get();
		
		ArrayList<AnomalyData> jobResults = new ArrayList<AnomalyData>();
		AnomalyData anomalyData;
		String id;
		Number score;
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			id = hit.getId();
			Map<String, Object> m  = hit.getSource();
			score = (Number)(m.get("anomalyScore"));
			m.put("id", id);
			
			anomalyData = new AnomalyData();
			anomalyData.setId(id);
			anomalyData.setTimestamp(Long.parseLong(id)*1000l);
			anomalyData.setScore(score.floatValue());
			jobResults.add(anomalyData);
		}
		
		// Sort by time.
		// TODO - must be a way of getting elasticsearch to sort by timestamp.
		Collections.sort(jobResults, new Comparator<AnomalyData>(){
			
			@Override
			public int compare(AnomalyData record1, AnomalyData record2)
			{
				return Float.compare(record1.getTimestamp(), record2.getTimestamp());
			}
        	
        });

		return jobResults;
	}
	
	/**
	 * This only works if there is a maximum of 1 detector other than the
	 *  count detector
	 * 
	 * @param jobId
	 * @param bucketId
	 * @return
	 */
	public List<AnomalyRecord> getBucketRecords(String jobId, String bucketId)
	{
		FilterBuilder filterBuilder = FilterBuilders.termFilter("bucketId", bucketId);
		
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes("detector")		
				.setFrom(0).setSize(100)
				.setFilter(filterBuilder)
				.get();
		
		List<String> detectors = new ArrayList<String>();
		
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			// ignore the count detectors
			String id = hit.getId();
			if (id.contains("count") == false)
			{
				detectors.add(id);
			}
		}
		
		if (detectors.size() == 0)
		{
			return Collections.emptyList();
		}
		
		String detectorId = detectors.get(0);
		s_Logger.info("Querying for detector " + detectorId + " for bucket time: " + new Date(Long.parseLong(bucketId)*1000l));
		
		// Now search for the records
		filterBuilder = FilterBuilders.termFilter("detectorId", detectorId);
		
		searchResponse = m_Client.prepareSearch(jobId)
				.setTypes("record")		
				.setFrom(0).setSize(100)
				.setFilter(filterBuilder)
				.get();
		
		List<AnomalyRecord> records = new ArrayList<AnomalyRecord>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			AnomalyRecord record = new AnomalyRecord();
			Map<String, Object> source  = hit.getSource();
			
			if (source.containsKey("anomalyScore"))
			{
				Number score = (Number)(source.get("anomalyScore"));
				record.setAnomalyFactor(score.floatValue());
			}		
			if (source.containsKey("probability"))
			{
				Number prob = (Number)(source.get("probability"));
				record.setProbability(prob.floatValue());
			}				
			if (source.containsKey("fieldname"))
			{
				record.setFieldName(source.get("fieldname").toString());
			}			
			if (source.containsKey("fieldvalue"))
			{
				record.setFieldValue(source.get("fieldvalue").toString());
			}

			try
			{
				record.setTime(new Date(Long.parseLong(bucketId)*1000l));
			}
			catch (NumberFormatException nfe)
			{
				s_Logger.error("Cannot convert " + bucketId +  " to a number");
			}
			
			records.add(record);
		}
		
		return records;		
	}
	
	
	public void closeClient()
	{
		m_Client.close();
		s_Logger.info("Closed elasticsearch TransportClient");
	}


	public static void main(String[] args)
	{
		ElasticsearchClient clientTest = new ElasticsearchClient("vm-centos-62-64-1", 9300);
		
		//String jobId = "20131213114610-00001";
		String jobId = "alertlogictest_3";
		ArrayList<AnomalyData> jobResults = clientTest.getJobResults(jobId);
		s_Logger.info("Number of results for jobId: " + jobResults.size());
		
		// 13th bucket has results
		if (jobResults.size() > 13)
		{
			String bucketId = jobResults.get(13).getId();
			List<AnomalyRecord> records = clientTest.getBucketRecords(jobId, bucketId);
			s_Logger.info(String.format("Number of records for bucket %s : %d",
					bucketId, records.size()));
		}
		
		clientTest.closeClient();
	}

}
