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
package com.prelert.job.usage.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.ScriptService;

import com.prelert.job.usage.Usage;
import com.prelert.job.usage.UsageReporter;

import static com.prelert.job.persistence.elasticsearch.ElasticsearchJobProvider.PRELERT_USAGE_INDEX;

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
	private String m_DocId;
	private long m_CurrentHour;
	
	private Map<String, Object> m_UpsertMap;
	
	public ElasticsearchUsageReporter(Client client, String jobId, Logger logger) 
	{
		super(jobId, logger);
		m_Client = client;
		m_CurrentHour = 0;		
		m_SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX");
		m_UpsertMap = new HashMap<>();
		
		m_UpsertMap.put(Usage.TIMESTAMP, "");
		m_UpsertMap.put(Usage.INPUT_BYTES, null);
	}

	@Override
	public boolean persistUsageCounts() 
	{
		long hour =  System.currentTimeMillis() / 3600000; // ms in the hour
		
		if (m_CurrentHour != hour)
		{
			Date date = new Date(hour * 3600000);
			m_DocId = "usage-" + m_SimpleDateFormat.format(date);
			m_UpsertMap.put(Usage.TIMESTAMP, date);
		}
		
		// update global count		
		updateDocument(PRELERT_USAGE_INDEX,  m_DocId, getBytesReadSinceLastReport(),
				getFieldsReadSinceLastReport(), getRecordsReadSinceLastReport());
		updateDocument(getJobId(), m_DocId, getBytesReadSinceLastReport(),
				getFieldsReadSinceLastReport(), getRecordsReadSinceLastReport());
			
		return true;
	}

	
	/**
	 * Update the metering document in the given index/id.
	 * Uses a script to update the volume field and 'upsert'
	 * to create the doc if it doesn't exist.
	 * 
	 * @param index
	 * @param id Doc id is also its timestamp
	 * @param additionalBytes Add this value to the running total
	 * @param additionalFields Add this value to the running total
	 * @param additionalRecords Add this value to the running total
	 */
	private void updateDocument(String index, String id, 
			long additionalBytes, long additionalFields, long additionalRecords)
	{
		m_UpsertMap.put(Usage.INPUT_BYTES, new Long(additionalBytes));
		m_UpsertMap.put(Usage.INPUT_FIELD_COUNT, new Long(additionalFields));
		m_UpsertMap.put(Usage.INPUT_RECORD_COUNT, new Long(additionalRecords));
		
		UpdateResponse response = m_Client.prepareUpdate(index, Usage.TYPE, id)
				.setScript("update-usage", ScriptService.ScriptType.FILE)
				.addScriptParam("bytes", additionalBytes)
				.addScriptParam("fieldCount", additionalFields)
				.addScriptParam("recordCount", additionalRecords)
				.setUpsert(m_UpsertMap)
				.setRetryOnConflict(3).get();
	}		
}
