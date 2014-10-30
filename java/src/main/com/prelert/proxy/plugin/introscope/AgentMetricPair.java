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

package com.prelert.proxy.plugin.introscope;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.proxy.regex.RegExUtilities;

/**
 * Utility class encapsulates Agent and Metric regexs.
 * 
 * Internally the agent and metric strings are stored with properly
 * escaped regex characters.
 * 
 * The externalKey String returned by the static methods of this 
 * class is <b>not</b> regex escaped. It is simply of the form:</br>
 * agent&%&metric
 */
public class AgentMetricPair
{
	final static public String EXTERNAL_KEY_JOIN_STRING = "&%&";
	final private String m_Agent;
	final private String m_Metric;
	
	
	/**
	 * Creates an <code>AgentMetricPair</code> from Strings that are regex
	 * escaped. Internally <code>AgentMetricPair</code>'s members Agent and
	 * Metric are consistently stored regex escaped. 
	 * @param agent
	 * @param metric
	 * @return
	 */
	static public AgentMetricPair createFromEscapedRegexs(String agent, String metric)
	{
		return new AgentMetricPair(agent, metric);
	}
	
	
	/**
	 * Construct an AgentMetricPair.
	 * The arguments will NOT be regex escaped before the are assigned.
	 * @param agent
	 * @param metric
	 */
	private AgentMetricPair(String agent, String metric)
	{
		m_Agent = agent;
		m_Metric = metric;
	}
	

	/**
	 * Construct an AgentMetricPair from an external key.
	 * @param externalKey should NOT be correctly regex escaped it
	 * will be escaped here.
	 * @throws ParseException
	 */
	AgentMetricPair(String externalKey) throws ParseException
	{
		String [] split = externalKey.split(EXTERNAL_KEY_JOIN_STRING);
		if (split.length != 2)		
		{
			throw new ParseException("Could not parse external key: " 
								+ externalKey, 0);
		}

		m_Agent = RegExUtilities.escapeRegex(split[0]);
		m_Metric = RegExUtilities.escapeRegex(split[1]);
	}

	/**
	 * Construct an AgentMetricPair from a metric, host and list of Attributes.
	 * This constructor will throw an exception if any of the attributes <code>
	 * PROCESS_ATTRIBUTE, AGENT_ATTRIBUTE & METRIC_PATH_ATTRIBUTE</code> are 
	 * not defined or if they are equal to null. 
	 * 
	 * @param metric
	 * @param host
	 * @param attributes
	 */
	AgentMetricPair(String metric, String host, 
								List<Attribute> attributes)
	{
		String process = null;
		String agent = null;
		String metricPath = null;
		
		host = RegExUtilities.escapeRegex(host);
		metric = RegExUtilities.escapeRegex(metric);
		
		// Sort attributes so any named ResourcePath1, ResourcePath2, etc
		// are in the right order.
		Collections.sort(attributes);

		for (Attribute attr : attributes)
		{
			if (attr.getAttributeName().equals(IntroscopePlugin.PROCESS_ATTRIBUTE))
			{
				process = RegExUtilities.escapeRegex(attr.getAttributeValue());
			}
			else if (attr.getAttributeName().equals(IntroscopePlugin.AGENT_ATTRIBUTE))
			{
				agent = RegExUtilities.escapeRegex(attr.getAttributeValue());
			}
			else if (attr.getAttributeName().startsWith(IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE))
			{
				// Want '|' character to appear ONLY between metrics on the path
				if (metricPath == null) 
				{
					metricPath = RegExUtilities.escapeRegex(attr.getAttributeValue());
				}
				else
				{
					metricPath = metricPath + "\\|" + RegExUtilities.escapeRegex(attr.getAttributeValue());
				}
			}
				
			
		}
		
		if ((process == null) || (agent == null) || (metricPath == null))
		{
			throw new IllegalArgumentException("The attributes: " + 
									IntroscopePlugin.PROCESS_ATTRIBUTE + ", " + 
									IntroscopePlugin.AGENT_ATTRIBUTE + ", " + 
									IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE +
									" must all be defined.");
		}
		
		
		String agentPath = host + "\\|" + process + "\\|" + agent;
		metricPath = metricPath + ":" + metric;

		m_Agent = agentPath;
		m_Metric = metricPath;
	}
	

	/**
	 * Returns the agent string with regex characters correctly escaped 
	 * where appropriate.
	 * @return
	 */
	String getAgent()
	{
		return m_Agent;
	}
	
	/**
	 * Returns the metric string with regex characters correctly escaped 
	 * where appropriate.
	 * @return
	 */
	String getMetric()
	{
		return m_Metric;
	}
		
	
	/**
	 * Returns the external key for this pair WITHOUT any regex characters 
	 * escaped.
	 * @return
	 */
	static public String createExternalKey(String agent, String metric)
	{
		return agent + EXTERNAL_KEY_JOIN_STRING + metric;
	}
	
	
	/**
	 * Builds an external key.
	 * @param host
	 * @param process
	 * @param agent
	 * @param resourcePath
	 * @param metric
	 * @return
	 */
	static public String createExternalKey(String host, String process, String agent,
									String resourcePath, String metric)
	{
		String agentPath = host + "|" + process + "|" + agent;
		String metricPath;
		
		if (resourcePath != null && !resourcePath.isEmpty())
		{
			metricPath = resourcePath + ":" + metric;
		}
		else
		{
			metricPath = metric;
		}

		return agentPath + EXTERNAL_KEY_JOIN_STRING + metricPath;
	}


	/**
	 * Create an external key from a metric, host and list of Attributes.
	 * This method will throw an exception if any of the attributes <code>
	 * PROCESS_ATTRIBUTE, AGENT_ATTRIBUTE & METRIC_PATH_ATTRIBUTE</code> are
	 * not defined or if they are equal to null.
	 *
	 * @param metric
	 * @param host
	 * @param attributes
	 */
	static public String createExternalKey(String metric, String host,
								List<Attribute> attributes)
	{
		String process = null;
		String agent = null;
		String metricPath = null;

		// Sort attributes so any named ResourcePath1, ResourcePath2, etc
		// are in the right order.
		Collections.sort(attributes);

		for (Attribute attr : attributes)
		{
			if (attr.getAttributeName().equals(IntroscopePlugin.PROCESS_ATTRIBUTE))
			{
				process = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(IntroscopePlugin.AGENT_ATTRIBUTE))
			{
				agent = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().startsWith(IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE))
			{
				// Want '|' character to appear ONLY between metrics on the path
				if (metricPath == null)
				{
					metricPath = attr.getAttributeValue();
				}
				else
				{
					metricPath = metricPath + "|" + attr.getAttributeValue();
				}
			}
		}

		if ((process == null) || (agent == null) || (metricPath == null))
		{
			throw new IllegalArgumentException("The attributes: " +
									IntroscopePlugin.PROCESS_ATTRIBUTE + ", " +
									IntroscopePlugin.AGENT_ATTRIBUTE + ", " +
									IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE +
									" must all be defined.");
		}

		return createExternalKey(host, process, agent, metricPath, metric);
	}

}
