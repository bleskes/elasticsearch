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

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.prelert.data.gxt.ActivityTreeModel;


/**
 * Extension of the GXT <code>BaseTreeLoader</code> for loading data for the
 * Activity Summary Tree. It adds methods for setting the activity evidence ID,
 * 'analyze by' attribute name, tree attribute names, and whether the tree should
 * be loaded in metric path order.
 * @author Pete Harverson
 */
public class ActivitySummaryTreeLoader extends BaseTreeLoader<ActivityTreeModel>
{
	private ActivitySummaryTreeRpcProxy m_Proxy;
	
	
	/**
	 * Creates a new loader for a tree displaying the summary of an activity.
	 */
	public ActivitySummaryTreeLoader()
	{
		super(new ActivitySummaryTreeRpcProxy());
		m_Proxy = (ActivitySummaryTreeRpcProxy)proxy;
	}
	
	
	/**
	 * Sets the ID of a notification or time series feature in the activity.
	 * @param id evidence id of a notification or time series feature in the activity.
	 */
	public void setEvidenceId(int id)
	{
		m_Proxy.setEvidenceId(id);
	}
	
	
	/**
	 * Sets the list of the names of the attributes to be analysed in the tree.
	 * @param treeAttributeNames list of activity data attribute names.
	 */
	public void setTreeAttributeNames(List<String> treeAttributeNames)
	{
		m_Proxy.setTreeAttributeNames(treeAttributeNames);
	}
	
	
	/**
	 * Sets whether the attributes in the activity should be analyzed in metric
	 * path order.
	 * @param metricPathOrder <code>true</code> to analyse the attributes in
	 * 	metric path order, <code>false</code> to analyze by attribute value count.
	 */
	public void setLoadInMetricPathOrder(boolean metricPathOrder)
	{
		m_Proxy.setMetricPathOrder(metricPathOrder);
	}
	
	
	/**
	 * Optionally sets the name of the attribute to force to the 'front' of
	 * the analysis.
	 * @param analyzeBy optional name of the attribute to force to the 'front' of
	 * 	the analysis, or <code>null</code> to use the natural ordering based on count.
	 */
	public void setAnalyzeBy(String analyzeBy)
	{
		m_Proxy.setAnalyzeBy(analyzeBy);
	}
	
	
	/**
	 * Sets the maximum number of leaf rows to return.
	 * @param max maximum number of leaf rows to return, or <code>-1</code> 
	 *  to return the complete list of data. If a limit is specified and the number 
	 *  of leaf rows exceeds this limit, the last leaf returned will
	 * 	represent the remaining items aggregated into an 'Others' object.
	 */
	public void setMaximumLeafRows(int max)
	{
		m_Proxy.setMaximumLeafRows(max);
	}
}
