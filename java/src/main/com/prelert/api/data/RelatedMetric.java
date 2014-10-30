/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.prelert.data.Attribute;
import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;

/**
 * Details of a metric or piece of evidence related to an Activity. 
 * Activities can have a number of related metric paths each of 
 * which has a significance score and an associated time series.   
 */
public class RelatedMetric 
{
	/**
	 * Attribute Names
	 */
	public static final String HOST_ATTRIBUTE = "Host";
	public static final String AGENT_ATTRIBUTE = "Agent";
	public static final String PROCESS_ATTRIBUTE = "Process";
	public static final String RESOURCE_PATH_ATTRIBUTE = "ResourcePath";
	public static final String PATH_SEPARATOR = "|";
	public static final String METRIC_SEPARATOR = ":";
	
	private static final String EXTERNAL_KEY_JOIN_STRING = "&%&";
	
	private String m_SourceType;
	private int m_EvidenceId;
	private DateTime m_Date;
	private String m_Description;
	private int m_Count;
	private String m_MetricPath;
	private int m_Significance;
	private double m_Magnitude;
	
	/**
	 * The source type of the headline feature. 
	 * @return
	 */
	public String getSourceType() 
	{
		return m_SourceType;
	}
	
	public void setSourceType(String sourceType) 
	{
		this.m_SourceType = sourceType;
	}
	
	/**
	 * The Id of the piece of evidence (or time series feature) 
	 * mapped to this RelatedMetric
	 * @return
	 */
	@EntityKey
	public int getEvidenceId() 
	{
		return m_EvidenceId;
	}
	
	public void setEvidenceId(int evidenceId) 
	{
		this.m_EvidenceId = evidenceId;
	}
	
	/**
	 * Time of the Activity
	 * @return
	 */
	public DateTime getDateTime() 
	{
		return m_Date;
	}
	
	public void setDateTime(DateTime epochDate) 
	{
		this.m_Date = epochDate;
	}
	
	/**
	 * Brief description of the probable cause
	 * @return
	 */
	public String getDescription()
	{
		return m_Description;
	}
	
	public void setDescription(String description) 
	{
		this.m_Description = description;
	}
	
	/**
	 * The occurrence count for this probable cause
	 * @return
	 */
	public int getCount() 
	{
		return m_Count;
	}
	
	public void setCount(int count) 
	{
		this.m_Count = count;
	}
	
	/**
	 * The metric path to the time series which is the 
	 * source of this RelatedMetric
	 * @return
	 */
	public String getMetricPath() 
	{
		return m_MetricPath;
	}
	
	public void setMetricPath(String metricPath) 
	{
		this.m_MetricPath = metricPath;
	}
	
	/**
	 * The importance of this RelatedMetric in this activity 
	 * as a percentage. More important (or influential) metrics 
	 * have higher percentages.
	 * @return
	 */
	public int getSignificance() 
	{
		return m_Significance;
	}
	
	public void setSignificance(int significance) 
	{
		this.m_Significance = significance;
	}
	
	/**
	 * The magnitude of the RelatedMetic is defined as the change in 
	 * metric value as a percentage of the higher of the metric values 
	 * immediately before and after the anomaly (there is an alternative 
	 * definition where the magnitude is the rate of change of the metric,
	 * but this alternative has to be set when the database is installed)
	 * @return
	 */
	public double getMagnitude() 
	{
		return m_Magnitude;
	}
	
	public void setMagnitude(double magnitude) 
	{
		this.m_Magnitude = magnitude;
	}	
	
	
	/**
	 * Create a RelatedMetric from the ProbableCause.
	 * @param pc
	 * @return
	 */
	static public RelatedMetric createFromProbableCause(ProbableCause pc)
	{
		RelatedMetric rm = new RelatedMetric();		
		rm.setSourceType(pc.getDataSourceType().getName());
		rm.setEvidenceId(pc.getEvidenceId());
		rm.setDateTime(new DateTime(pc.getTime()));
		rm.setDescription(pc.getDescription());
		rm.setCount(pc.getCount());
		rm.setSignificance(pc.getSignificance());
		rm.setMagnitude(pc.getMagnitude());

		String metricPath = "";
		
		// Try to get the metric path from the external key.
		String externalKey = pc.getExternalKey();
		if (externalKey != null && externalKey.isEmpty() == false)
		{
			String [] split = externalKey.split(EXTERNAL_KEY_JOIN_STRING);
			if (split.length == 2)
			{
				// This external key was created by the Introscope plugin
				// remove the '&%&' string
				
				if (split[1].startsWith(METRIC_SEPARATOR))
				{
					// is of the form host|process|agent:metric' 
					// so don't add the path separator
					externalKey = split[0] + split[1];
				}
				else 
				{
					externalKey = split[0] + PATH_SEPARATOR + split[1];
				}
			}
			
			metricPath = externalKey;
		}
		else  
		{
			// try to build the metric path from attributes.
			metricPath = metricPathFromAttributes(pc.getSource(), pc.getMetric(),
					pc.getAttributes());
		}
		rm.setMetricPath(metricPath);
		
		return rm;
	}

	
	/**
	 * Create a RelatedMetric from the Evidence.
	 * @param evidence
	 * @return
	 */
	static public RelatedMetric createFromEvidence(Evidence evidence)
	{
		RelatedMetric rm = new RelatedMetric();	
		rm.setSourceType(evidence.getDataType());
		rm.setEvidenceId(evidence.getId());
		rm.setDateTime(new DateTime(evidence.getTime()));
		rm.setDescription(evidence.getDescription());
		
		
		Map<String, Object> props = evidence.getProperties();
		
		if (props.containsKey("count"))
		{
			rm.setCount((Integer)props.get("count"));
		}
		
		List<Attribute> attrs = new ArrayList<Attribute>();
		if (props.containsKey(AGENT_ATTRIBUTE))
		{
			attrs.add(new Attribute(AGENT_ATTRIBUTE, (String)props.get(AGENT_ATTRIBUTE)));
		}
		if (props.containsKey(PROCESS_ATTRIBUTE))
		{
			attrs.add(new Attribute(PROCESS_ATTRIBUTE, (String)props.get(PROCESS_ATTRIBUTE)));
		}
		
		int count = 0;
		String resource = String.format("%s%d", RESOURCE_PATH_ATTRIBUTE, count);
		while (props.containsKey(resource))
		{
			attrs.add(new Attribute(resource, (String)props.get(resource)));
			resource = String.format("%s%d", RESOURCE_PATH_ATTRIBUTE, ++count);
		}
		
		String metric = (String)props.get("metric");
		
		String metricPath = metricPathFromAttributes(evidence.getSource(), metric, attrs);
		rm.setMetricPath(metricPath);
		
		return rm;
	}
	
	
	/**
	 * Build the CA APM metric path from attributes.
	 * 
	 * @param source
	 * @param metric
	 * @param attrs
	 * @return
	 */
	static private String metricPathFromAttributes(String source, String metric, 
			List<Attribute> attrs)
	{
		if (attrs == null)
		{
			return "";
		}
		
		// Sort attributes (by attribute name) so any named 
		// ResourcePath1, ResourcePath2, etc are in the right order.
		Collections.sort(attrs);
		
		// Build the metric path 
		String agent = null;
		String process = null;
		String resourcePath = "";
		
		for (Attribute attr : attrs)
		{
			if (PROCESS_ATTRIBUTE.equals(attr.getAttributeName()))
			{
				process = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(AGENT_ATTRIBUTE))
			{
				agent = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().startsWith(RESOURCE_PATH_ATTRIBUTE))
			{
				resourcePath = resourcePath + PATH_SEPARATOR + attr.getAttributeValue();
			}
		}
		
		if ((process == null) || (agent == null))
		{
			String msg = "The attributes: " + PROCESS_ATTRIBUTE + " and " + 
							AGENT_ATTRIBUTE + " must all be defined.";

			throw new IllegalArgumentException(msg);
		}
		
		StringBuilder sb = new StringBuilder(source);
		sb.append(PATH_SEPARATOR).append(process);
		sb.append(PATH_SEPARATOR).append(agent);
		sb.append(resourcePath);
		sb.append(METRIC_SEPARATOR).append(metric);
		
		return sb.toString();
	}
	
	
}
