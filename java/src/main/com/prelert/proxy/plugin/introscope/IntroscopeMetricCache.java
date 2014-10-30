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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;


/**
 * Cache for Introscope metrics.
 * 
 * The cache is thread safe. It will block on calls to <code>getMetrics()</code>
 * until the cache has been populated for the first time. After that
 * <code>getMetrics()</code> will always return immediately even if the cache 
 * is updating itself.  
 */
public class IntroscopeMetricCache 
{
	static Logger s_Logger = Logger.getLogger(IntroscopeMetricCache.class);
	
	static final public int HOST_LEVEL = 1;
	static final public int PROCESS_LEVEL = 2;
	static final public int AGENT_LEVEL = 3;
	static final public int FIRST_RESOURCE_LEVEL = 4;
	
	static final private int CACHE_EXPIRATION_TIME_MS = 30 * 60 * 1000;
	
	volatile private Date m_LastUpdated;
	
	private DefaultMutableTreeNode m_MetricTree;
	
	public IntroscopeMetricCache() 
	{
		m_MetricTree = new DefaultMutableTreeNode();
	}
	
	
	/**
	 * Returns true if the cache should be updated with a call 
	 * to updateMetricTree(). If the cache is empty or it hasn't
	 * been updated for CACHE_EXPIRATION_TIME_MS.
	 * 
	 * @return
	 */
	public boolean cacheNeedsUpdating()
	{
		synchronized(m_MetricTree)
		{
			if (m_MetricTree.getChildCount() == 0)
			{
				return true;
			}

		}
		
		if (m_LastUpdated == null)
		{
			return true;
		}
		
		long diff = new Date().getTime() - m_LastUpdated.getTime();
		if (diff >= CACHE_EXPIRATION_TIME_MS)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Update the metric cache with the results of Introscope
	 * metric queries. The old cache is over-written <em>not</em>
	 * merged with these points.
	 * 
	 * @param datas
	 * @return
	 */
	public boolean updateMetricTree(Collection<TimeSeriesData> datas)
	{
		DefaultMutableTreeNode newMetricTree = new DefaultMutableTreeNode();

		for (TimeSeriesData data : datas)
		{
			TimeSeriesConfig config = data.getConfig();
			String process = null;
			String agent = null;
			List<String> resourcePaths = new ArrayList<String>(); 

			// Sort attributes so resource paths are in order.
			Collections.sort(config.getAttributes());

			for (Attribute attr : config.getAttributes())
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
					resourcePaths.add(attr.getAttributeValue());
				}

			}

			// Add the metric to the tree.
			try
			{
				addMetricToTree(config.getSource(), process, agent,
						resourcePaths, config.getMetric(),
						newMetricTree);
			}
			catch (IllegalArgumentException e)
			{
				s_Logger.error("updateMetricTree exception = " + e);
				continue;
			}

			// Now set the new paths
			synchronized(m_MetricTree)
			{
				m_MetricTree = newMetricTree;
				m_LastUpdated = new Date();
			}
		}
		
		return true;
	}
	
	
	/**
	 * Parses and splits the agent/metric paths and adds them to
	 * the metric tree creating any nodes that don't already exist.
	 * Nodes are added to the metric tree parameter.
	 * 
	 * @param host - cannot be <code>null</code>
	 * @param process - cannot be <code>null</code>
	 * @param agent - cannot be <code>null</code>
	 * @param resourcePath 
	 * @param metric - cannot be <code>null</code>
	 * @param metricTree - The tree to add the metric to.
	 * @throws ParseException
	 */
	private void addMetricToTree(String host, String process, String agent,
								List<String> resourcePaths, String metric,
								DefaultMutableTreeNode metricTree) 
	throws IllegalArgumentException
	{
		// ResourcePaths can be empty but everything else should be non-null.
		if (host == null || process == null || agent == null || metric == null)
		{
			throw new IllegalArgumentException(
					String.format("addMetricToTree - %s, %s, %s and %s cannot be null",
									host, process, agent, metric));
		}
		
		String [] fullPath = new String[3 + resourcePaths.size() + 1];
		fullPath[0] = host;
		fullPath[1] = process;
		fullPath[2] = agent;
		
		for (int i=0; i<resourcePaths.size(); i++)
		{
			fullPath[i + 3] = resourcePaths.get(i);
		}
		fullPath[fullPath.length -1] = metric;
		
		DefaultMutableTreeNode node = metricTree;
		for (String pathElement : fullPath)
		{			
			boolean pathPresent = false;
			for (int j=0; j<node.getChildCount(); j++)
			{
				String childString = (String) ((DefaultMutableTreeNode)node.getChildAt(j)).getUserObject();
				if (pathElement.equals(childString))
				{
					pathPresent = true;
					node = (DefaultMutableTreeNode)node.getChildAt(j);
					break;
				}
			}

			if (!pathPresent)
			{
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(pathElement);
				node.add(newNode);

				node = newNode;
			}
		}
	}
	
	
	/**
	 * Returns the list of metrics in the cache.
	 * 
	 * This function will block if the cache has not been populated once 
	 * else it will return immediately even if the cache is concurrently 
	 * updating.
	 * 
	 * @return
	 */
	public List<String> getMetrics()
	{
		s_Logger.debug("Metric Cache getMetrics()");
		
		Set<String> metrics = new HashSet<String>();

		synchronized (m_MetricTree)
		{
			@SuppressWarnings("unchecked")
			Enumeration<Object> e = m_MetricTree.depthFirstEnumeration();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
				if (node.isLeaf())
				{
					metrics.add((String)node.getUserObject());
				}
			}
		}
		
		// Return sorted unique values.
		List<String> results = new ArrayList<String>(metrics);
		Collections.sort(results);
		
		return results;
	}
	
	
	/**
	 * Returns the number of metrics in this metric cache.
	 * 
	 * This function will block if the cache has not been populated once 
	 * else it will return immediately even if the cache is concurrently 
	 * updating.
	 * @return 
	 */
	public int getNumberOfMetrics()
	{
		s_Logger.debug("Metric Cache getNumberOfMetrics()");
		
		int metricCount = 0;
		synchronized (m_MetricTree)
		{
			@SuppressWarnings("unchecked")
			Enumeration<Object> e = m_MetricTree.depthFirstEnumeration();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
				if (node.isLeaf())
				{
					metricCount++;
				}
			}
		}
		
		return metricCount;
	}
	
	
	/**
	 * Returns the values of the child nodes of the node specified
	 * by <code>paths</code>
	 *  
	 * @param paths -List of Strings that are a path through the
	 * 	             metric tree.
	 * @return
	 */
	public List<MetricNode> childValuesForPath(List<String> paths)
	{
		s_Logger.debug("Metric Cache childValuesForPath(" + paths + ")");
		
		List<MetricNode> children = new ArrayList<MetricNode>();
		
		synchronized(m_MetricTree)
		{
			DefaultMutableTreeNode node = m_MetricTree;

			for (String pathElement : paths)
			{			
				boolean pathPresent = false;
				for (int i=0; i<node.getChildCount(); i++)
				{
					String childString = (String) ((DefaultMutableTreeNode)node.getChildAt(i)).getUserObject();
					if (pathElement.equals(childString))
					{
						pathPresent = true;
						node = (DefaultMutableTreeNode)node.getChildAt(i);
						break;
					}
				}

				if (!pathPresent)
				{
					throw new IllegalArgumentException("Invalid Path=" + paths + " bad value=" + pathElement);
				}
			}

			// Now get the children of the node
			for (int i=0; i<node.getChildCount(); i++)
			{ 
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getChildAt(i);
				String childString = (String) childNode.getUserObject();

				children.add(new MetricNode(childString, childNode.isLeaf()));
			}
		}
		
		return children;	
	}
	

	/**
	 * Return all the metric path node values for a particular 
	 * level in the tree.
	 * 
	 * level can be one of the constants:
	 * <ul>
	 * <li>HOST_LEVEL</li>
	 * <li>PROCESS_LEVEL</li>
	 * <li>AGENT_LEVEL</li>
	 * </ul>
	 * 
	 * @param level
	 * @return Sorted list of metric path node values.
	 */
	public List<String> getMetricPathValuesAtLevel(int level)
	{
		s_Logger.debug("Metric Cache getMetriPathValuesAtLevel(" + level + ")");
		
		List<String> result = new ArrayList<String>();

		synchronized (m_MetricTree)
		{
			@SuppressWarnings("unchecked")
			Enumeration<Object> e = m_MetricTree.breadthFirstEnumeration();
			while (e.hasMoreElements())
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
				int treeLevel = node.getLevel();
				if (treeLevel == level)
				{
					result.add((String)node.getUserObject());
				}

				if (treeLevel > level)
				{
					break;
				}
			}

		}

		return result;
	}
	
	
	/**
	 * Clears the Metric Cached of all stored metrics.
	 */
	public void reset()
	{
		m_MetricTree = new DefaultMutableTreeNode();
	}


	/**
	 * Utility class for nodes pairs the node
	 * value with a bool which is true is the 
	 * mode is a leaf node. 
	 */
	public class MetricNode implements Comparable<MetricNode>
	{
		private String m_Value;
		private boolean m_IsLeaf;
		
		public MetricNode(String value, boolean isLeaf)
		{
			m_Value = value;
			m_IsLeaf = isLeaf;
		}
		
		public String getValue()
		{
			return m_Value;
		}
		
		public boolean isLeaf()
		{
			return m_IsLeaf;
		}

		@Override
		public int compareTo(MetricNode other) 
		{
			return m_Value.compareTo(other.getValue());
		}
	}
}
