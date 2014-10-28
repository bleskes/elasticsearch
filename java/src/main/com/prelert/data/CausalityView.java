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

package com.prelert.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Configuration data for a Causality View.
 * @author Pete Harverson
 */
public class CausalityView extends View implements Serializable
{
	private static final long serialVersionUID = 7859168268222646276L;

	private int						m_EvidenceId;
	private List<MetricTreeNode>	m_PathTreeNodes;
	private List<String>			m_Attributes;
	private Map<Integer, Double> 	m_PeakValuesByTypeId;
	private int						m_AnomalyScore = 1;
	

	/**
	 * Returns the id of a notification or time series from the incident whose
	 * causality data is being viewed.
     * @return the id of the item of evidence.
     */
    public int getEvidenceId()
    {
    	return m_EvidenceId;
    }


	/**
	 * Sets the id of a notification or time series from the incident whose
	 * causality data is being viewed.
     * @param evidenceId the id of the item of evidence.
     */
    public void setEvidenceId(int evidenceId)
    {
    	m_EvidenceId = evidenceId;
    }
    
    
    /**
	 * Returns a list of the names and prefixes for the constituents of the
	 * longest metric path associated with the time series causality data.
	 * An empty list will be returned if the causality data is for an incident
	 * that contains more than one type of data, because in that case it doesn't 
	 * make sense to return the attributes for a single metric path within it.
	 * @return list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric
     *         path.
	 */
    public List<MetricTreeNode> getMetricPathTreeNodes()
    {
    	return m_PathTreeNodes;
    }
    
    
    /**
     * Sets the list of the names and prefixes for the constituents of the
	 * longest metric path associated with the time series causality data.
	 * A non-zero length list should only be supplied if the causality data is for an 
	 * incident that contains more than one type of data, because in that case it doesn't 
	 * make sense to return the attributes for a single metric path within it.
     * @param pathTreeNodes list of partially populated <code>MetricTreeNode</code> 
     * 	objects containing the name and prefix of each constituent of the metric path.
     */
    public void setMetricPathTreeNodes(List<MetricTreeNode> pathTreeNodes)
    {
    	m_PathTreeNodes = pathTreeNodes;
    	
    	// Generate the list of attribute names from the path tree nodes.
    	m_Attributes = new ArrayList<String>();
    	for (MetricTreeNode treeNode : pathTreeNodes)
    	{
    		m_Attributes.add(treeNode.getName());
    	}
    }


	/**
	 * For incidents containing just a single time series type, returns a list of all the 
	 * attributes associated with the data, including source, type and metric.
     * @return list of all the attributes found across the time series features that
     * 	form part of the causality data, including type, source and metric, in the
     * 	order they are found in the metric path.
     */
    public List<String> getAttributes()
    {
    	return m_Attributes;
    }


	/**
	 * Returns a map of peak values against time series type id for the time series
	 * type causality data in the incident.
     * @return a map of peak values against time series type id.
     */
    public Map<Integer, Double> getPeakValuesByTypeId()
    {
    	return m_PeakValuesByTypeId;
    }


	/**
	 * Sets a map of peak values against time series type id for the time series
	 * type causality data in the incident.
     * @param peakValuesByTypeId a map of peak values against time series type id.
     */
    public void setPeakValuesByTypeId(
            Map<Integer, Double> peakValuesByTypeId)
    {
    	m_PeakValuesByTypeId = peakValuesByTypeId;
    }
    
    
    /**
	 * Returns the anomaly score of the activity displayed in the view.
     * @return the anomaly score, between 1 and 100.
     */
    public int getActivityAnomalyScore()
    {
    	return m_AnomalyScore;
    }
    
    
    /**
     * Sets the anomaly score of the activity displayed in the view.
     * @param anomalyScore the anomaly score, between 1 and 100.
     */
    public void setActivityAnomalyScore(int anomalyScore)
    {
    	m_AnomalyScore = anomalyScore;
    }


	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("CausalityView evidenceId=");
	   strRep.append(m_EvidenceId);
	   
	   strRep.append(",attributes=");
	   strRep.append(m_Attributes);
	   
	   strRep.append(",peakValues=");
	   strRep.append(m_PeakValuesByTypeId);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }

}
