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

package com.prelert.job.persistence.elasticsearch;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.prelert.job.persistence.JobDataPersister;


public class ElasticsearchJobDataPersister implements JobDataPersister
{
	static public String PERSISTED_RECORD_TYPE = "saved-data";
	
	public static String FIELDS = "fields";
	public static String BY_FIELDS = "byFields";
	public static String OVER_FIELDS = "overFields";
	public static String PARTITION_FIELDS = "partitionFields";
	
	public static String TYPE = "saved-data";
	
	private Client m_Client;
	private String m_IndexName;
	
	private String [] m_FieldNames;
	private int [] m_FieldMappings;
	private int [] m_ByFieldMappings;
	private int [] m_OverFieldMappings;
	private int [] m_PartitionFieldMappings;
	
	
	private Logger m_Logger;
	
	public ElasticsearchJobDataPersister(String jobId, Client client, Logger logger)
	{
		m_IndexName = jobId + "_raw";
		m_Client = client;
		m_Logger = logger;

		m_Logger.info("Data will be persisted in the index " + m_IndexName);
		createIndex();
	}

	private void createIndex() 
	{
		if (m_Client.admin().indices().prepareExists(m_IndexName).get().isExists()
				== false)
		{
			try
			{
				XContentBuilder inputDataMapping = ElasticsearchMappings.inputDataMapping();

				m_Client.admin().indices()
						.prepareCreate(m_IndexName)					
						.addMapping(ElasticsearchJobDataPersister.TYPE, inputDataMapping)
						.get();
			}
			catch (IOException e)
			{
				m_Logger.error("Error creating the raw data index " + m_IndexName, e);
			}
		}
	}
	

	@Override
	public void setFieldMappings(List<String> fields, 
			List<String> byFields, List<String> overFields,
			List<String> partitionFields, String [] header)
	{
		List<String> headerList = Arrays.asList(header);
		
		m_FieldNames = new String [fields.size()];
		m_FieldNames = fields.<String>toArray(m_FieldNames);	
		m_FieldMappings = new int [fields.size()];
		m_ByFieldMappings = new int [byFields.size()];
		m_OverFieldMappings = new int [overFields.size()];
		m_PartitionFieldMappings = new int [partitionFields.size()];
		
		List<List<String>> allFieldTypes = Arrays.asList(fields, byFields,
				overFields, partitionFields);
		
		int [][] allFieldMappings = new int [][] {m_FieldMappings, m_ByFieldMappings,
				m_OverFieldMappings, m_PartitionFieldMappings};
		
		int i = 0;
		for (List<String> fieldType : allFieldTypes)
		{	
			int j = 0;
			for (String f : fieldType)
			{
				int index = headerList.indexOf(f);
				if (index >= 0)
				{
					allFieldMappings[i][j] = index;
				}
				else
				{  
					// not found in header
					int [] tmp = new int [allFieldMappings[i].length -1];
					System.arraycopy(allFieldMappings[i], 0, tmp, 0, j);
					System.arraycopy(allFieldMappings[i], j+1, tmp, j, tmp.length - j);
				}

				j++;
			}
			
			i++;
		}
		
	}
		
	@Override
	public void persistRecord(long epoch, String [] record)
	{
		try
		{
			XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
			try 
			{
				// epoch in ms
				jsonBuilder.startObject().field("epoch", epoch * 1000);
				

				for (int i=0; i<m_FieldNames.length; i++)
				{
					try
					{
						jsonBuilder.field(m_FieldNames[i], Double.parseDouble(record[m_FieldMappings[i]]));
					}
					catch (NumberFormatException e)
					{
						jsonBuilder.field(m_FieldNames[i], record[m_FieldMappings[i]]);
					}
				}

				jsonBuilder.startArray(BY_FIELDS);
				for (int i=0; i<m_ByFieldMappings.length; i++)
				{
					jsonBuilder.value(record[m_ByFieldMappings[i]]);
				}
				jsonBuilder.endArray();

				jsonBuilder.startArray(OVER_FIELDS);
				for (int i=0; i<m_OverFieldMappings.length; i++)
				{
					jsonBuilder.value(record[m_OverFieldMappings[i]]);
				}
				jsonBuilder.endArray();			

				jsonBuilder.startArray(PARTITION_FIELDS);
				for (int i=0; i<m_PartitionFieldMappings.length; i++)
				{
					jsonBuilder.value(record[m_PartitionFieldMappings[i]]);
				}
				jsonBuilder.endArray();	

				m_Client.prepareIndex(m_IndexName, PERSISTED_RECORD_TYPE)
								.setSource(jsonBuilder)
								.get();

			}
			finally
			{
				jsonBuilder.close();
			}
		}
		catch (IOException e)
		{
			m_Logger.error("Error persisted raw record", e);
		}
	}
	
	@Override
	public boolean deleteData()
	{
		m_Logger.debug("Deleting the raw data index " + m_IndexName);

		// we don't care about errors here as the 
		// the index may not exist anyway
		try
		{
			DeleteIndexResponse response = m_Client.admin()
					.indices().delete(new DeleteIndexRequest(m_IndexName)).get();

			return response.isAcknowledged();
		}
		catch (InterruptedException|ExecutionException e) 
		{
			m_Logger.warn("Error deleting the raw data index", e);
			return false;
		}
	}

}
