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

package com.prelert.data.gxt;


/**
 * Extension of ModelDatePagingLoadConfig for paging through activity data, adding
 * an anomaly threshold property.
 * @author Pete Harverson
 */
public class ActivityPagingLoadConfig extends ModelDatePagingLoadConfig
{
    private static final long serialVersionUID = 6065144491109425639L;
    
    
    /**
	 * Sets the anomaly threshold of activities to load.
	 * @param anomalyThreshold minimum anomaly score threshold of activities to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all activities,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') activities.
	 */
    public void setAnomalyThreshold(int threshold)
    {
    	set("anomalyThreshold", threshold);
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
    	int threshold = get("anomalyThreshold", new Integer(1));
    	return threshold;
    }
}
