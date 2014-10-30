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

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Extension of the RpcProxy class for loading data for an activity summary tree
 * through remote procedure calls to the incident query service. It adds methods 
 * for specifying evidence ID, 'analyze by' attribute name, tree attribute names, 
 * and metric path order load criteria.
 * @author Pete Harverson
 */
public class ActivitySummaryTreeRpcProxy extends RpcProxy<List<ActivityTreeModel>>
{
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private int				m_EvidenceId;
	private List<String>	m_TreeAttributeNames = new ArrayList<String>();
	private boolean			m_IsMetricPathOrder;
	private String 			m_AnalyzeBy;
	private int 			m_MaxLeafRows = -1;
	
	
	/**
	 * Creates a new data proxy for loading the summary of an activity in a tree of
	 * attributes.
	 */
	public ActivitySummaryTreeRpcProxy()
	{
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
	}
	
	
	@Override
    protected void load(Object loadConfig,
            AsyncCallback<List<ActivityTreeModel>> callback)
    {
		ActivityTreeModel treeNode = (ActivityTreeModel)loadConfig;
		
		boolean loadFromServer = !(treeNode != null && treeNode.getChildCount() > 0);
		if (loadFromServer == true)
		{
			m_IncidentQueryService.getAnalysisTreeData(m_EvidenceId, treeNode, 
					m_TreeAttributeNames, m_IsMetricPathOrder, m_AnalyzeBy, m_MaxLeafRows, callback);
		}
		else
		{
			int childCount = treeNode.getChildCount();
			ArrayList<ActivityTreeModel> children = new ArrayList<ActivityTreeModel>();
			for (int i = 0; i < childCount; i++)
			{
				children.add((ActivityTreeModel)(treeNode.getChild(i)));
			}
			callback.onSuccess(children);
		}
    }
	
	
	/**
	 * Sets the ID of a notification or time series feature in the activity.
	 * @param id evidence id of a notification or time series feature in the activity.
	 */
	public void setEvidenceId(int id)
	{
		m_EvidenceId = id;
	}
	
	
	/**
	 * Sets the list of the names of the attributes to be analysed in the tree.
	 * @param treeAttributeNames list of activity data attribute names.
	 */
	public void setTreeAttributeNames(List<String> treeAttributeNames)
	{
		m_TreeAttributeNames = treeAttributeNames;
	}
	
	
	/**
	 * Sets whether the attributes in the activity should be analyzed in metric
	 * path order.
	 * @param metricPathOrder <code>true</code> to analyse the attributes in
	 * 	metric path order, <code>false</code> to analyze by attribute value count.
	 */
	public void setMetricPathOrder(boolean metricPathOrder)
	{
		m_IsMetricPathOrder = metricPathOrder;
	}
	
	
	/**
	 * Optionally sets the name of the attribute to force to the 'front' of
	 * the analysis.
	 * @param analyzeBy optional name of the attribute to force to the 'front' of
	 * 	the analysis, or <code>null</code> to use the natural ordering based on count.
	 */
	public void setAnalyzeBy(String analyzeBy)
	{
		m_AnalyzeBy = analyzeBy;
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
		m_MaxLeafRows = max;
	}

}
