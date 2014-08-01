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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.persistence.AlertPersister;

public class ElasticsearchAlertPersister implements AlertPersister 
{
	private Client m_Client;
	
	public ElasticsearchAlertPersister(Client client)
	{
		m_Client = client;
	}

	@Override
	public void persistAlert(String jobId, Alert alert) 
	throws IOException
	{
		XContentBuilder content = serialiseAlert(alert);

		m_Client.prepareIndex(jobId, Alert.TYPE)
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

}
