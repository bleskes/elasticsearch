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

package com.prelert.job.alert.persistence.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.job.alert.persistence.AlertPersister;
import com.prelert.rs.data.Pagination;

public class ElasticsearchAlertPersister implements AlertPersister 
{
	private Client m_Client;
	private ObjectMapper m_ObjectMapper;
	
	public ElasticsearchAlertPersister(Client client)
	{
		m_Client = client;
		
		m_ObjectMapper = new ObjectMapper();
		m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			
	}

	@Override
	public void persistAlert(String alertId, String jobId, Alert alert) 
	throws IOException
	{
		XContentBuilder content = serialiseAlert(alert);

		m_Client.prepareIndex(jobId, Alert.TYPE, alertId)
							.setSource(content)
							.execute().actionGet();
	}
	

	private XContentBuilder serialiseAlert(Alert alert) 
	throws IOException
	{
		XContentBuilder builder = jsonBuilder().startObject()
				.field(Alert.JOB_ID, alert.getTimestamp())
				.field(Alert.TIMESTAMP, alert.getTimestamp())
				.field(Alert.SEVERTIY, alert.getSeverity())
				.field(Alert.REASON, alert.getReason())
				.endObject();

		return builder;
	}

	@Override
	public Pagination<Alert> alerts(int skip, int take, long startEpoch,
			long endEpoch) 
	{
		return queryAlerts("_all", skip, take, startEpoch, endEpoch);
	}

	@Override
	public Pagination<Alert> alertsForJob(String jobId, int skip, int take,
			long startEpoch, long endEpoch)
	throws UnknownJobException
	{
		return queryAlerts(jobId, skip, take, startEpoch, endEpoch);
	}
	
	
	private Pagination<Alert> queryAlerts(String index, int skip, int take,
			long startEpoch, long endEpoch) 
	{
		FilterBuilder fb;		
		if (startEpoch > 0 || endEpoch > 0)
		{
			RangeFilterBuilder rfb = FilterBuilders.rangeFilter(Alert.TIMESTAMP);
			if (startEpoch > 0)
			{
				rfb.gte(new Date(startEpoch * 1000));
			}
			if (endEpoch > 0)
			{
				rfb.lt(new Date(endEpoch * 1000));
			}
			
			fb = rfb;
		}
		else
		{
			fb = FilterBuilders.matchAllFilter();
		}
		
		
		SortBuilder sb = new FieldSortBuilder(Alert.ID) 
							.order(SortOrder.ASC);
		
		SearchResponse searchResponse = m_Client.prepareSearch(index)
				.setTypes(Alert.TYPE)		
				.addSort(sb)
				.setPostFilter(fb)
				.setFrom(skip).setSize(take)
				.get();
		
		List<Alert> results = new ArrayList<>();
		
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Alert alert = m_ObjectMapper.convertValue(hit.getSource(), Alert.class);
			results.add(alert);
		}
		
		
		Pagination<Alert> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		
		return page;
	}	

	@Override
	public String lastAlertId()
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		SortBuilder sb = new FieldSortBuilder(Alert.ID)
							.ignoreUnmapped(true)
							.order(SortOrder.DESC);
		
		SearchResponse searchResponse = m_Client.prepareSearch("_all")
				.setTypes(Alert.TYPE)		
				.addSort(sb)
				.setPostFilter(fb)
				.setSize(1)
				.get();
		
		if (searchResponse.getHits().getTotalHits() == 0)
		{
			return null;
		}
		
		return searchResponse.getHits().getHits()[0].getId();
	}
	

	@Override
	public List<Alert> alertsAfter(String alertId) 
	{
		return getAlertsAfter(alertId, "_all");
	}
	
	
	@Override
	public List<Alert> alertsAfter(String alertId, String jobId) 
	{
		return getAlertsAfter(alertId, jobId);
	}
	
	private List<Alert> getAlertsAfter(String alertId, String index) 
	{
		RangeFilterBuilder fb = FilterBuilders.rangeFilter(Alert.ID);
		fb.gt(alertId);

		SortBuilder sb = new FieldSortBuilder(Alert.ID)
							.ignoreUnmapped(true)
							.order(SortOrder.ASC);

		int from = 0;
		int size = 1000;
		List<Alert> results = new ArrayList<>();

		SearchRequestBuilder searchBuilder =  m_Client.prepareSearch(index)
											.setTypes(Alert.TYPE)		
											.addSort(sb)
											.setPostFilter(fb);
		while (true)
		{
			SearchResponse searchResponse = searchBuilder
												.setFrom(from)
												.setSize(size)
												.get();

			for (SearchHit hit : searchResponse.getHits().getHits())
			{
				Alert alert = m_ObjectMapper.convertValue(hit.getSource(), 
						Alert.class);
				results.add(alert);
			}

			if (searchResponse.getHits().getTotalHits() > from + size)
			{
				break;
			}
			
			from += size;
		}

		return results;
	}
}
