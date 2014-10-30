/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/

package com.prelert.client.incident;

import com.prelert.client.list.ModelDatePagingLoader;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.data.gxt.ActivityPagingLoadConfig;


/**
 * Extension of ModelDatePagingLoader for paging through activity data, adding
 * an anomaly threshold property.
 * @author Pete Harverson
 */
public class ActivityPagingLoader extends ModelDatePagingLoader<IncidentModel>
{
	private int m_AnomalyThreshold = 1;

	
	/**
	 * Creates a new activity loader instance with the given RPC data proxy.
	 * @param proxy the data proxy used for loading activity data from the server.
	 */
	public ActivityPagingLoader(ActivityPagingRpcProxy proxy)
    {
	    super(proxy);
    }
	
	
	/**
	 * Sets the anomaly threshold of activities to load.
	 * @param anomalyThreshold minimum anomaly score threshold of activities to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all activities,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') activities.
	 */
    public void setAnomalyThreshold(int threshold)
    {
    	m_AnomalyThreshold =  threshold;
    }
    
 
    /**
     * Returns the anomaly threshold of activities to load.
     * @return minimum anomaly score threshold of activities to return, which
	 * 	should be a value between 1 and 100. A value of 1 will return all 
	 *  activities, whilst a value of 100 will return only the most infrequent 
	 *  (most 'anomalous') activities.
     */
    public int getAnomalyThreshold()
    {
    	return m_AnomalyThreshold;
    }


    @Override
    protected Object newLoadConfig()
    {
	    return new ActivityPagingLoadConfig();
    }


    @Override
    protected Object prepareLoadConfig(Object config)
    {
    	super.prepareLoadConfig(config);
    	
    	ActivityPagingLoadConfig loadConfig = (ActivityPagingLoadConfig)config;
    	loadConfig.setAnomalyThreshold(m_AnomalyThreshold);
    	
    	return loadConfig;
    }

	
    @Override
    public void useLoadConfig(Object loadConfig)
    {
	    super.useLoadConfig(loadConfig);
	    
	    ActivityPagingLoadConfig pagingLoadConfig = (ActivityPagingLoadConfig)loadConfig;
	    m_AnomalyThreshold = pagingLoadConfig.getAnomalyThreshold();
    }
	
}
