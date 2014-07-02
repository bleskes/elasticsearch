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
package com.prelert.job.usage.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import com.prelert.job.usage.Usage;
import com.prelert.job.usage.UsageReporter;

import static com.prelert.job.manager.JobManager.PRELERT_METERING_INDEX;

/**
 * Persist Usage (metering) data to elasticsearch.
 * Data is written per job to the job index and summed for 
 * all jobs to the {@value com.prelert.job.manager.JobManager.PRELERT_METERING_INDEX} 
 * index.
 */
public class ElasticsearchUsageReporter extends UsageReporter 
{
	private Client m_Client;
	private SimpleDateFormat m_SimpleDateFormat;
	private String m_DateStr;
	private long m_CurrentHour;
	
	public ElasticsearchUsageReporter(Client client, String jobId, Logger logger) 
	{
		super(jobId, logger);
		m_Client = client;
		m_CurrentHour = 0;		
		m_SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX");
	}

	@Override
	public boolean persistUsageCounts() 
	{
		long hour =  System.currentTimeMillis() / 3600000; // ms in the hour
		
		if (m_CurrentHour != hour)
		{
			Date date = new Date(hour * 3600000);
			m_DateStr = m_SimpleDateFormat.format(date);
		}
		
		// update global count
		updateDocument(PRELERT_METERING_INDEX, m_DateStr, getBytesReadSinceLastReport());
		updateDocument(getJobId(), m_DateStr, getBytesReadSinceLastReport());
			
		return true;
	}
	
	/**
	 * Update the metering document in the given index/id
	 * 
	 * @param index
	 * @param id Doc id is also its timestamp
	 * @param additionalVolume Add this value to the running total
	 */
	private void updateDocument(String index, String id, long additionalVolume)
	{
		Map<String, Object> source;
		GetResponse response = m_Client.prepareGet(index, Usage.TYPE, id)
				.setPreference("_primary") // get primary version as about to update
				.get();
		
		long lastVersion = response.getVersion();
		if (response.isExists())
		{
			source = response.getSource();
			
			// Volume is returned as an Integer not Long is 
			// this a bug in Elasticsearch - the mapping is set to long.
			Number volume = (Number)source.get(Usage.VOLUME);
			if (volume == null)
			{
				source.put(Usage.VOLUME, additionalVolume);
			}
			else
			{
				source.put(Usage.VOLUME, (volume.longValue() + additionalVolume));
			}
			
			IndexResponse indexResponse = m_Client.prepareIndex(index, Usage.TYPE, id)
					.setSource(source)
					.setVersion(lastVersion)
					.get();
			if (indexResponse.getVersion() <= lastVersion)
			{
				getLogger().error("Conflict saving metering doc " + id);
			}
		}		
		else 
		{
			getLogger().debug("Creating new metering doc " +  id);
			
			source = new HashMap<>();
			source.put(Usage.TIMESTAMP, id); //
			source.put(Usage.VOLUME, additionalVolume);
			
			m_Client.prepareIndex(index, Usage.TYPE, id)
					.setSource(source).get();
		}
	}
}
