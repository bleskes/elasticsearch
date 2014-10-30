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

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.introscope.IntroscopeConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.proxy.encryption.PasswordEncryption;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException;
import com.prelert.proxy.plugin.introscope.ClwConnectionPool;
import com.prelert.proxy.regex.RegExUtilities;

public class CatalogueIntroscope 
{
	public static final String NOT_CUSTOM_AGENT_REGEX = "^(?!Custom Metric Host \\(Virtual\\)).+";
	
	public static final String ART_METRIC_REGEX = "^(?!Heuristics).*:Average Response Time \\(ms\\)";
	public static final String CONCURRENT_METRIC_REGEX = "^(?!Heuristics).*:Concurrent Invocations";
	public static final String EPI_METRIC_REGEX = "^(?!Heuristics).*:Errors Per Interval";
	public static final String RPI_METRIC_REGEX = "^(?!Heuristics).*:Responses Per Interval";
	public static final String STALLS_METRIC_REGEX = "^(?!Heuristics).*:Stall Count";
	
	public static final String ALL_METRICS_REGEX = ".*";
	public static final String ALL_NO_HEURISTIC_METRICS_REGEX = "^(?!Heuristics).*";
	
	public static final String ALERT_QUERIES_FILENAME =	"alert_queries.xml";
	public static final String METRIC_QUERIES_FILENAME = "metric_queries.xml";
	
	private String m_Host;
	private int m_Port;
	private String m_Username;
	private String m_Password;
	
	private ClwConnectionPool m_ClwConnectionPool;
	
	public CatalogueIntroscope(String host, int port,
							String username, String password) throws Exception
	{
		m_Host = host;
		m_Port = port;
		m_Username = username;
		m_Password = password;
		
		createClient();
	}
	

	private void createClient() throws Exception
	{
		IntroscopeConnectionConfig connectParams = new IntroscopeConnectionConfig(
							m_Host, m_Port, 
							m_Username, m_Password);
		
		m_ClwConnectionPool = new ClwConnectionPool();
		try
		{
			m_ClwConnectionPool.setConnectionConfig(connectParams);
		}
		catch (Exception e)
		{
			String msg = String.format("Could not create CLW connection with host=%1s port=%2d " +
					"username=%3s, password=%4s", m_Host, m_Port, m_Username, m_Password);
			
			System.console().printf(msg);
			System.out.println(e);
			
			throw e;
		}
	}
	
	
	/**
	 * Return a list of agents for the given regex. agentRegex can be null
	 * in which case <code>NOT_CUSTOM_AGENT_REGEX</code> is used.
	 * 
	 * The return agents are <i>not</i> escaped for regualare expressions. 
	 * 
	 * @param agentRegex If <code>null</code> then the regex defaults
	 * 		             to NOT_CUSTOM_AGENT_REGEX. 
	 * @return
	 */
	public List<String> listAgents(String agentRegex)
	{
		IntroscopeConnection connection = null;
		try 
		{
			connection = m_ClwConnectionPool.acquireConnection();
		} 
		catch (Exception e1) 
		{
			System.out.print(e1);
			return Collections.emptyList();
		}
		
		
		if (agentRegex == null)
		{
			agentRegex = NOT_CUSTOM_AGENT_REGEX;
		}
		
		try
		{
			List<String> agents = connection.listAgents(agentRegex);
			for (String agent : agents)
			{
				System.out.println(agent);
			}
			
			return agents;
		}
		catch (ConnectionException e) 
		{
			System.out.print("Connection error listing agents: " + e);
			return Collections.emptyList();
		}
		finally
		{
			m_ClwConnectionPool.releaseConnection(connection);
		}
	}
	
	
	/**
	 * Returns a regex escaped Map of Agent to a list of metrics of metrics for all the agents.
	 * Agents should be full specified host|process|agent. 
	 * 
	 * This function escapes any regex characters in the agent string.
	 * 
	 * The returned agents/metrics in the map are all escaped for regular expressions.
	 * 
	 * @param agents Should be full specified host|process|agent. This function escapes
	 * 				any regex characters in the agent string.
	 * @return
	 */
	public Map<String, List<String>> list5MainMetrics(Iterable<String> agents)
	{
		Map<String, List<String>> agentNameToMetrics = new HashMap<String, List<String>>();
		
		try 
		{
			IntroscopeConnection connection = m_ClwConnectionPool.acquireConnection();
			
			try
			{
				for (String agent : agents)
				{
					List<String> metricNames = new ArrayList<String>();

					String escapedAgent = RegExUtilities.escapeRegex(agent);

					List<String> metrics = connection.listMetrics(escapedAgent, ART_METRIC_REGEX);
					for (String metric : metrics)
					{
						String escapedMetric = RegExUtilities.escapeRegex(metric);
						metricNames.add(escapedMetric);
					}

					metrics = connection.listMetrics(escapedAgent, CONCURRENT_METRIC_REGEX);
					for (String metric : metrics)
					{
						String escapedMetric = RegExUtilities.escapeRegex(metric);
						metricNames.add(escapedMetric);
					}

					metrics = connection.listMetrics(escapedAgent, EPI_METRIC_REGEX);
					for (String metric : metrics)
					{
						String escapedMetric = RegExUtilities.escapeRegex(metric);
						metricNames.add(escapedMetric);
					}

					metrics = connection.listMetrics(escapedAgent, RPI_METRIC_REGEX);
					for (String metric : metrics)
					{
						String escapedMetric = RegExUtilities.escapeRegex(metric);
						metricNames.add(escapedMetric);
					}

					metrics = connection.listMetrics(escapedAgent, STALLS_METRIC_REGEX);
					for (String metric : metrics)
					{
						String escapedMetric = RegExUtilities.escapeRegex(metric);
						metricNames.add(escapedMetric);
					}

					Collections.sort(metricNames);
					agentNameToMetrics.put(escapedAgent, metricNames);
				}
			}
			finally
			{
				m_ClwConnectionPool.releaseConnection(connection);
			}
		} 
		catch (Exception e) 
		{
			System.out.println("list agent exception : " + e);
		}
		
		return agentNameToMetrics;
	}
	
	
	/**
	 * Returns a list of metrics for the agent.
	 * Agent should be full specified host|process|agent. This function escapes
	 * any regex characters in the agent string.
	 * 
	 * @param agent Should be full specified host|process|agent. This function
	 * 			    escapes any regex characters in the agent string.
	 * @param metricRegex Filter by metrics matching this regex. 
	 * @return List of metrics
	 */
	public List<String> listMetrics(String agent, String metricRegex)
	{
		List<String> metrics = new ArrayList<String>();
		
		try 
		{
			IntroscopeConnection connection = m_ClwConnectionPool.acquireConnection();

			try
			{
				String escapedAgent = RegExUtilities.escapeRegex(agent);
				return connection.listMetrics(escapedAgent, metricRegex);
			}
			finally
			{
				m_ClwConnectionPool.releaseConnection(connection);
			}

		} 
		catch (Exception e) 
		{
			System.out.println("list agent exception : " + e);
		}
		
		return metrics;
	}
	
	/**
	 * Overloaded version uses the default metric regex of ".*" and returns 
	 * all metrics.
	 * @param agent Should be full specified host|process|agent. 
	 * 				This function escapes any regex characters in the agent string.
	 * @return List of metrics
	 */
	public List<String> listMetrics(String agent)
	{
		return listMetrics(agent, ".*");
	}
	
	/**
	 * Returns a map of metrics lists to agents. 
	 * Agent should be full specified host|process|agent. This function escapes
	 * any regex characters in the agent string.
	 * 
	 * @param agents Should be full specified host|process|agent. This function escapes
	 * 					any regex characters in the agent string.
	 * @return
	 */
	public Map<String, List<String>> listMetricsByAgent(Iterable<String> agents)
	{
		Map<String, List<String>> agentNameToMetrics = new HashMap<String, List<String>>();
		
		try 
		{
			IntroscopeConnection connection = m_ClwConnectionPool.acquireConnection();
			
			try
			{
				for (String agent : agents)
				{
					String escapedAgent = RegExUtilities.escapeRegex(agent);

					List<String> metricNames = connection.listMetrics(escapedAgent, ".*");

					Collections.sort(metricNames);
					agentNameToMetrics.put(escapedAgent, metricNames);
				}
			}
			finally
			{
				m_ClwConnectionPool.releaseConnection(connection);
			}
		} 
		catch (Exception e) 
		{
			System.out.println("list agent exception : " + e);
		}
		
		return agentNameToMetrics;
	}
		
	
	/**
	 * Returns a list of <code>AlertGroup</code>s which are understood
	 * by the introscope plugin and used to define the plugin's alert
	 * queries.
	 * 
	 * Simple implementation just lists all the management modules 
	 * 
	 * @param moduleRegex The regex to list management modules. If 
	 * 			<code>null</code> the default of '.*' is used.
	 * @param alertRegex The regex to match alerts. If 
	 * 			<code>null</code> the default of '.*' is used.
	 * 
	 * @return
	 */
	public List<AlertGroup> getAlertQueries(String moduleRegex, String alertRegex)
	{
		List<AlertGroup> alertQueries = new ArrayList<AlertGroup>();
		
		if (alertRegex == null)
		{
			alertRegex = ".*";
		}
		
		if (moduleRegex == null)
		{
			moduleRegex = ".*";
		}
		
		System.out.println("List modules for " + moduleRegex + " and alerts matching " + alertRegex);
				
		try
		{
			IntroscopeConnection connection = m_ClwConnectionPool.acquireConnection();
			
			try
			{
				List<String> modules = connection.listManagementModules(moduleRegex);

				for (String module : modules)
				{
					alertQueries.add(new AlertGroup(module, null, alertRegex));
				}
			}
			finally
			{
				m_ClwConnectionPool.releaseConnection(connection);
			}
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

		
		return alertQueries;
	}

	
	public static Document agentsToXml(List<String> agents) throws ParserConfigurationException
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		Element root = doc.createElement("Agents");
		doc.appendChild(root);
		
		for (String agent: agents)
		{
			Element ae = doc.createElement("Agent");
			Text text = doc.createTextNode(agent);
			ae.appendChild(text);

			root.appendChild(ae);
		}

		return doc;
	}
	
	
	public static Document alertsToXml(Map<String, List<String>> alertsByModule) throws ParserConfigurationException
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		Element root = doc.createElement("Alerts");
		doc.appendChild(root);
		
		for (String module : alertsByModule.keySet())
		{
			Element ae = doc.createElement("Module");
			Element name = doc.createElement("Name");
			Text text = doc.createTextNode(module);
			name.appendChild(text);
			
			ae.appendChild(name);

			Element alerts = doc.createElement("Alerts");
			List<String> alertnames = alertsByModule.get(module);
			for (String alertname : alertnames)
			{
				Element an = doc.createElement("Alert");
				Text value = doc.createTextNode(alertname);
				an.appendChild(value);

				alerts.appendChild(an);
			}
			ae.appendChild(alerts);		
			
			
			root.appendChild(ae);
		}

		return doc;
	}
	
	
	public static Document managementModulesToXml(List<String> modules) throws ParserConfigurationException
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		Element root = doc.createElement("Modules");
		doc.appendChild(root);
		
		for (String module: modules)
		{
			Element ae = doc.createElement("Module");
			Text text = doc.createTextNode(module);
			ae.appendChild(text);

			root.appendChild(ae);
		}

		return doc;
	}
	
	
	
	/**
	 * Return a collection of <code>MetricGroup</code>s which are used
	 * for the Introscope plugin's metric queries.
	 * 
	 * This just returns a regex for each of the 5 main metrics for 
	 * each agent.
	 * 
	 * @param agentRegex If <code>null</code> then the regex defaults
	 * 		             to NOT_CUSTOM_AGENT_REGEX. 
	 * @return
	 */
	public Collection<List<MetricGroup>> getFiveMainMetricQueries(String agentRegex)
	{
		Collection<List<MetricGroup>> result = new ArrayList<List<MetricGroup>>();
		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		
		List<String> agents = listAgents(agentRegex);
		
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, ART_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, CONCURRENT_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, EPI_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, RPI_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, STALLS_METRIC_REGEX, true));
		}

		result.add(queries);
		return result;
	}
	
	
	
	/**
	 * Lists all the metrics in the system for the given <code>agentRegex</code>
	 * and returns a collection of <code>MetricGroup</code>s for the common 
	 * metrics in Introscope. ie. if a metric string containing JDBC is found 
	 * then all the JDBC metrics will be added. 
	 * 
	 * @param agentRegex If <code>null</code> then the regex defaults
	 * 		             to NOT_CUSTOM_AGENT_REGEX. 
	 * @return
	 */
	public Collection<List<MetricGroup>> getCommonMetricQueries(String agentRegex)
	{
		Collection<List<MetricGroup>> result = new ArrayList<List<MetricGroup>>();
		Set<MetricGroup> queries = new LinkedHashSet<MetricGroup>();
		
		List<String> agents = listAgents(agentRegex);
		
		for (String agent : agents)
		{			
			String escapedAgent = RegExUtilities.escapeRegex(agent);
			
			System.out.println("Listing all metrics for " +  agent);

			
			List<String> metrics = listMetrics(agent);
			// join strings
			StringBuilder builder = new StringBuilder();
			for (String metric : metrics)
			{
				builder.append(metric);
				builder.append(",");
			}
			
	
			// Do 5 main metrics
			addMetricGroup(queries, escapedAgent, "Average Response Time \\(ms\\)", builder);
			addMetricGroup(queries, escapedAgent, "Concurrent Invocations", builder);
			addMetricGroup(queries, escapedAgent, "Errors Per Interval", builder);
			addMetricGroup(queries, escapedAgent, "Responses Per Interval", builder);
			addMetricGroup(queries, escapedAgent, "Stall Count", builder);
			
			
			if (patternMatches("GC Heap", builder))
			{
				addMetricGroup(queries, escapedAgent, "Bytes In Use", builder);
				addMetricGroup(queries, escapedAgent, "Bytes Total", builder);
				addMetricGroup(queries, escapedAgent, "Heap Used \\(%\\)", builder);
			}
			
			if (patternMatches("CPU", builder))
			{
				addMetricGroup(queries, escapedAgent, "Utilization % \\(process\\)", builder);
				addMetricGroup(queries, escapedAgent, "Utilization % \\(aggregate\\)", builder);
			}

			if (patternMatches("File System", builder))
			{
				addMetricGroup(queries, escapedAgent, "File input rate \\(bytes per second\\)", builder);
				addMetricGroup(queries, escapedAgent, "File output rate \\(bytes per second\\)", builder);
			}
			
			if (patternMatches("UDP", builder))
			{
				addMetricGroup(queries, escapedAgent, "Output bandwidth \\(bytes per second\\)", builder);
				addMetricGroup(queries, escapedAgent, "Input bandwidth \\(bytes per second\\)", builder);
			}
			
			if (patternMatches("Socket", builder))
			{
				addMetricGroup(queries, escapedAgent, "Accepts Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Closes Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Readers", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Writers", builder);
				addMetricGroup(queries, escapedAgent, "Opens Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Input Bandwidth \\(Bytes Per Second\\)", builder);
				addMetricGroup(queries, escapedAgent, "Output Bandwidth \\(Bytes Per Second\\)", builder);				
			}
			
			if (patternMatches("Thread", builder))
			{
				addMetricGroup(queries, escapedAgent, "Active Threads", builder);
				addMetricGroup(queries, escapedAgent, "Available Threads", builder);
				addMetricGroup(queries, escapedAgent, "Maximum Idle Threads", builder);
				addMetricGroup(queries, escapedAgent, "Minimum Idle Threads", builder);
				addMetricGroup(queries, escapedAgent, "Threads in Use", builder);
				addMetricGroup(queries, escapedAgent, "Thread Creates", builder);
				addMetricGroup(queries, escapedAgent, "Thread Destroys", builder);
				addMetricGroup(queries, escapedAgent, "OpenSessionsCurrentCount", builder);
			}
			
			
			
			if (patternMatches("EJB", builder))
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
			}
			
			if (patternMatches("JDBC", builder))
			{
				addMetricGroup(queries, escapedAgent, "Average Result Processing Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Average Query Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Queries Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Average Update Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Updates Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Commit Count", builder);
				addMetricGroup(queries, escapedAgent, "Commits Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Connection Count", builder);
				addMetricGroup(queries, escapedAgent, "Connections Count", builder);
			}
			
			if (patternMatches("JSP", builder))
			{
				addMetricGroup(queries, escapedAgent, "Average Response Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Responses Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Responses Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods by class name and by method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Invocations", builder);
			}
			
			if (patternMatches("TagLib", builder))
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods over 30 seconds by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
			}

			if (patternMatches("TagLibrary", builder))
			{
				addMetricGroup(queries, escapedAgent, "Warning Count", builder);
				addMetricGroup(queries, escapedAgent, "Exception Count", builder);
			}
			
			if (patternMatches("RMI", builder))
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods over 30 seconds", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
			}
			
			if (patternMatches("SQL", builder))
			{
				// + 5 basic metrics
				addMetricGroup(queries, escapedAgent, "Connection Count", builder);
				addMetricGroup(queries, escapedAgent, "Average Result Processing Time \\(ms\\)", builder);
			}
	
			if (patternMatches("J2EE", builder)) // TODO this string may be wrong
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Method count over 30 seconds by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
			}

			if (patternMatches("JTA", builder)) // TODO this string may be wrong
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods over 30 seconds by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
			}
			
			if (patternMatches("JMS", builder)) 
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods over 30 seconds by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
			}	
			
			if (patternMatches("CORBA", builder)) 
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Stalled methods in any class over 30 seconds", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
			}
			
			if (patternMatches("Struts", builder)) 
			{
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Average Method Invocation Time \\(ms\\) by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Interval by class name", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second", builder);
				addMetricGroup(queries, escapedAgent, "Method Invocations Per Second by class name", builder);
				addMetricGroup(queries, escapedAgent, "Stalled Methods over 30 seconds by class name and method name", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations", builder);
				addMetricGroup(queries, escapedAgent, "Concurrent Method Invocations by class name", builder);
			}
			
			if (patternMatches("Web Servers", builder)) 
			{
				addMetricGroup(queries, escapedAgent, "Availability Status", builder);
				// Apache, IBM & Oracle metrics Web Servers
				addMetricGroup(queries, escapedAgent, "Bytes Transferred Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Current Number of Busy Workers", builder);
				addMetricGroup(queries, escapedAgent, "Current Number of Idle Workers", builder);
				addMetricGroup(queries, escapedAgent, "Current Percentage CPU Load", builder);
				addMetricGroup(queries, escapedAgent, "Requests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "Closing connection", builder);
				addMetricGroup(queries, escapedAgent, "DNS Lookup", builder);
				addMetricGroup(queries, escapedAgent, "Gracefully finishing", builder);
				addMetricGroup(queries, escapedAgent, "Idle cleanup of worker", builder);
				addMetricGroup(queries, escapedAgent, "Keepalive \\(read\\)", builder);
				addMetricGroup(queries, escapedAgent, "Logging", builder);
				addMetricGroup(queries, escapedAgent, "Open slot with no current process", builder);
				addMetricGroup(queries, escapedAgent, "Reading Request", builder);
				addMetricGroup(queries, escapedAgent, "Sending Reply", builder);
				addMetricGroup(queries, escapedAgent, "Starting up", builder);
				addMetricGroup(queries, escapedAgent, "Waiting for Connection", builder);
			
				//  Microsoft IIS Web Server
				addMetricGroup(queries, escapedAgent, "AnonymousUsers Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "CurrentAnonymousUsers", builder);
				addMetricGroup(queries, escapedAgent, "NonAnonymousUsers", builder);
				addMetricGroup(queries, escapedAgent, "LogonAttempts Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "MaximumAnonymousUsers", builder);
				addMetricGroup(queries, escapedAgent, "MaximumNonAnonymousUsers", builder);
				addMetricGroup(queries, escapedAgent, "NonAnonymousUsers Per Interval", builder);
				
				addMetricGroup(queries, escapedAgent, "BytesReceived Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "BytesSent Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "BytesTransfered Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "FilesTransfered Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "FilesReceived Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "FilesSent Per Interval", builder);
				
				addMetricGroup(queries, escapedAgent, "ConnectionAttemptsallinstances Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "CurrentConnections", builder);
				addMetricGroup(queries, escapedAgent, "MaximumConnections", builder);
				
				addMetricGroup(queries, escapedAgent, "CopyRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "CurrentISAPIExtensionRequests", builder);
				addMetricGroup(queries, escapedAgent, "DeleteRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "CGIRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "CurrentCGIRequests", builder);
				addMetricGroup(queries, escapedAgent, "GetRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "HeadRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "ISAPIExtensionRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "LockRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "MaximumCGIRequests", builder);
				addMetricGroup(queries, escapedAgent, "MaximumISAPIExtensionRequests", builder);
				addMetricGroup(queries, escapedAgent, "MkcolRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "MoveRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "OptionsRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "OtherRequestMethods Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "PostRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "PropfindRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "ProppatchRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "PutRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "SearchRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "TraceRequests Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "UnlockRequests Per Interval", builder);

				addMetricGroup(queries, escapedAgent, "LockedErrors Per Interval", builder);
				addMetricGroup(queries, escapedAgent, "NotFoundErrors Per Interval", builder);
				
				addMetricGroup(queries, escapedAgent, "BLOBCacheFlushes", builder);
				addMetricGroup(queries, escapedAgent, "BLOBCacheHits", builder);
				addMetricGroup(queries, escapedAgent, "BLOBCacheHits Percent", builder);
				addMetricGroup(queries, escapedAgent, "BLOBCacheMisses", builder);
				addMetricGroup(queries, escapedAgent, "CurrentBLOBsCached", builder);
				
				addMetricGroup(queries, escapedAgent, "CurrentFileCacheMemoryUsage", builder);
				addMetricGroup(queries, escapedAgent, "CurrentFilesCached", builder);
				addMetricGroup(queries, escapedAgent, "FileCacheFlushes", builder);
				addMetricGroup(queries, escapedAgent, "FileCacheHits", builder);
				addMetricGroup(queries, escapedAgent, "FileCacheHitsPercent", builder);
				addMetricGroup(queries, escapedAgent, "FileCacheMisses", builder);
				addMetricGroup(queries, escapedAgent, "MaximumFileCacheMemoryUsage", builder);
				addMetricGroup(queries, escapedAgent, "CurrentURIsCached", builder);
				addMetricGroup(queries, escapedAgent, "URICacheFlushes", builder);
				addMetricGroup(queries, escapedAgent, "URICacheHits", builder);
				addMetricGroup(queries, escapedAgent, "URICacheHitsPercent", builder);
				addMetricGroup(queries, escapedAgent, "URICacheMisses", builder);
				addMetricGroup(queries, escapedAgent, "CurrentBlockedAsyncIORequests", builder);
				addMetricGroup(queries, escapedAgent, "MeasuredAsyncIOBandwidthUsage", builder);
				
				// iPlanet Sun ONE Web Server.
				//addMetricGroup(queries, escapedAgent, ".*Performance.*:", builder);
			}
			
			if (patternMatches("WebSphereMQ", builder)) 
			{
				addMetricGroup(queries, escapedAgent, "Commit/Rollback - Average Response Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Connect - Average Response Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Disconnect - Average Response Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Get - Average Response Time \\(ms\\)", builder);
				addMetricGroup(queries, escapedAgent, "Put - Average Response Time \\(ms\\)", builder);
			}
			
			
			// TODO XML
			// TODO JNDI
			// TODO Java Mail					
		}
		
		result.add(new ArrayList<MetricGroup>(queries));
		
		return result;
	}
	
	
	/**
	 * If the metric occurs in the <code>builder</code> then a new MetricGroup
	 * is created for that metric and added to <code>queries</code>.
	 *  
	 * @param queries
	 * @param agent
	 * @param metric
	 * @param builder Comma separated list of metrics ie [metricpath|]*:metric,[metricpath|]*:metric,[metricpath|]*:metric
	 */
	private void addMetricGroup(Set<MetricGroup> queries, String agent, String metric, StringBuilder builder)
	{
		String match = getMatchedPattern(metric + ",", builder);
		
		if (match != null)
		{
			queries.add(new MetricGroup(agent, ".*:" + metric, true));			
		}
	}
	
	/**
	 * Returns true if <code>pattern</code> occurs in <code>source</code>. 
	 * 
	 * @param pattern
	 * @param source
	 * @return the matched String or null.
	 */
	private boolean patternMatches(String pattern, StringBuilder source)
	{
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(source);
		
		return matcher.find();
	}
	
	/**
	 * Returns a String if <code>pattern</code> occurs in <code>source</code>
	 * or <code>null</code>.
	 *  
	 * @param pattern
	 * @param source
	 * @return The matched String or null
	 */
	private String getMatchedPattern(String pattern, StringBuilder source)
	{
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(source);
		
		if (matcher.find())
		{
			return matcher.group();
		}
		else
		{
			return null;
		}
	}
	
	
	/**
	 * Lists all the metrics matching <code>metricRegex</code> for each agent
	 * matching <code>agenttRegex</code>
	 * 
	 * @param metricRegex If <code>null</code> then the regex defaults
	 * 		             to ALL_METRICS_REGEX.
	 * @param agentRegex If <code>null</code> then the regex defaults
	 * 		             to NOT_CUSTOM_AGENT_REGEX. 
	 * @return
	 */
	public Collection<List<MetricGroup>> getAllMetricQueriesForRegex(String agentRegex, String metricRegex)
	{
		Collection<List<MetricGroup>> result = new ArrayList<List<MetricGroup>>();
		Set<MetricGroup> queries = new LinkedHashSet<MetricGroup>();
		
		if (metricRegex == null)
		{
			metricRegex = ALL_METRICS_REGEX; 
		}
		List<String> agents = listAgents(agentRegex);
		
		for (String agent : agents)
		{			
			System.out.println("Listing all metrics for " +  agent);

			String escapedAgent = RegExUtilities.escapeRegex(agent);
			
		
			List<String> metrics = listMetrics(agent, metricRegex);
			
			for (String metric : metrics)
			{
				queries.add(new MetricGroup(escapedAgent, RegExUtilities.escapeRegex(metric), false));	
			}
		}
		
		result.add(new ArrayList<MetricGroup>(queries));
		
		return result;
	}
	
	/**
	 * Gets a list of all agents matching agentRegex and returns 
	 * metric group to match all metrics ie. 'agent|.*'
	 *
	 * @param agentRegex If <code>null</code> then the regex defaults
	 * 		             to NOT_CUSTOM_AGENT_REGEX. 
	 * @param metricRegex If <code>null</code> then the regex defaults
	 * 		             to ALL_METRICS_REGEX.
	 *  
	 * @return A collection containing a list of Metric Groups.
	 */
	public Collection<List<MetricGroup>> getSimpleMetricQueries(String agentRegex, String metricRegex)
	{
		Collection<List<MetricGroup>> result = new ArrayList<List<MetricGroup>>();
		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		
		if (metricRegex == null)
		{
			metricRegex = ALL_METRICS_REGEX;
		}
		
		List<String> agents = listAgents(agentRegex);

		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, metricRegex, true));
		}

		result.add(queries);
		return result;
	}
	
	
	public static Document metricsToXml(Map<String, List<String>> metricsByAgent) throws ParserConfigurationException
	{
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		Element root = doc.createElement("Agents");
		doc.appendChild(root);
		
		for (String agent : metricsByAgent.keySet())
		{
			Element ae = doc.createElement("Agent");
			Element name = doc.createElement("Name");
			Text text = doc.createTextNode(agent);
			name.appendChild(text);
			
			ae.appendChild(name);

			Element metrics = doc.createElement("Metrics");
			List<String> metricNames = metricsByAgent.get(agent);
			for (String metric : metricNames)
			{
				Element mn = doc.createElement("Metric");
				Text value = doc.createTextNode(metric);
				mn.appendChild(value);

				metrics.appendChild(mn);
			}
			ae.appendChild(metrics);		
			
			
			root.appendChild(ae);
		}

		return doc;
	}
	
	
	public static void writeXmlFile(String filename, Document doc) throws TransformerFactoryConfigurationError, TransformerException
	{
		Source source = new DOMSource(doc);

		File file = new File(filename);
		Result result = new StreamResult(file);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(source, result);
	}

	
	public static void writeToScreen(Document doc)
	{
		try 
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			
			transformer.transform(source, result);
			
			System.out.println(result.getWriter().toString());
		}
		catch(TransformerException ex) 
		{
			System.out.println(ex);
		}
	}

	
	static private void updateDataTypeConfigFile(String outputDirectory, LoginDetails login)
	{
		File xmlFile = null;
		List<String> files = Arrays.asList(new String[]{"Introscope.xml", "IntroscopeAlerts.xml"});
		for (String filename: files)
		{
			System.out.println("Updating the connection details in file " + filename);
			
			try 
			{
		        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder docBuilder;
				try 
				{
					docBuilder = docBuilderFactory.newDocumentBuilder();
				}
				catch (ParserConfigurationException e) 
				{
					System.out.println("Cannot create DocumentBuilder" + e);
					continue;
				}
				
				xmlFile = new File(outputDirectory + "/datatypes", filename);
				Document doc = docBuilder.parse(xmlFile);
				DataTypeConfig config = DataTypeConfig.fromXml(doc);
	
				if (config == null)
				{
					System.out.println("Cannot parse configuration file " + xmlFile);
					continue;
				}
				
				config.getSourceConnectionConfig().setHost(login.m_Host);
				config.getSourceConnectionConfig().setPort(login.m_Port);
				config.getSourceConnectionConfig().setUsername(login.m_Username);
				// Encrypt the password
				try
				{
					String encrypted = PasswordEncryption.encryptPassword(login.m_Password);
					config.getSourceConnectionConfig().setPassword(encrypted);
				}
				catch(Exception e)
				{
				}
				
				doc = config.toXml();
				
				Source domSource = new DOMSource(doc);
				Result result = new StreamResult(xmlFile);
				
				
				// Write the DOM document to the file
				Transformer xformer;
				try 
				{
					xformer = TransformerFactory.newInstance().newTransformer();
				}
				catch (TransformerConfigurationException e) 
				{
					System.out.println("saveConfigurations() Cannot create Document Transformer" + e);
					continue;
				}
				catch (TransformerFactoryConfigurationError e) 
				{
					System.out.println("saveConfigurations() Cannot configure Document Transformer" + e);
					continue;
				}
				
				// Pretty print
				xformer.setOutputProperty(OutputKeys.INDENT, "yes");
				xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				
				xformer.transform(domSource, result);
			}
			catch (SAXException e) 
			{
				String msg = "Cannot parse configuration file " + xmlFile;
				System.out.println(msg + ". " + e);
			} 
			catch (FileNotFoundException e)
			{
				System.out.println("WARNING: Cannot find file " + xmlFile);
			}
			catch (IOException e) 
			{
				String msg = "Cannot parse configuration file " + xmlFile;
				System.out.println(msg + ". " + e);
			}
			catch (ParseException e) 
			{
				String msg = "Cannot parse configuration file " + xmlFile;
				System.out.println(msg + ". " + e);
			} 
			catch (ParserConfigurationException e) 
			{
				String msg = "Cannot parse configuration file " + xmlFile;
				System.out.println(msg + ". " + e);
			}
			catch (TransformerException e) 
			{
				String msg = "Cannot save configuration file " + xmlFile;
				System.out.println(msg + ". " + e);
			}
		}
	}


	
	static private class LoginDetails
	{
		String m_Host;
		int m_Port;
		String m_Username;
		String m_Password;
	}
	
	public static LoginDetails getLogin()
	{
		LoginDetails login = new LoginDetails();
		
		Console console = System.console();
		
		login.m_Host = console.readLine("Enter the host machine name or IP address: ");
		login.m_Port = 5001;
		while (true)
		{
			String portstr = null;
			try
			{
				portstr = console.readLine("Service Port number [" + login.m_Port +"]: ");
				if (portstr == null || portstr.isEmpty())
				{
					// use the default.
					break; 
				}
				
				login.m_Port = Integer.parseInt(portstr);
				break;
			}
			catch (NumberFormatException e)
			{
				console.printf("Invalid port number = %1s\n", portstr);
			}
		}
		
		
		login.m_Username = console.readLine("Enter the Username: ");

		char [] password = console.readPassword("Enter " + login.m_Username + "'s password: ");
		login.m_Password = new String(password);

		console.printf("Using connection options:\n");
		console.printf("Host = %1$s\n", login.m_Host);
		console.printf("Port = %1$d\n", login.m_Port);
		console.printf("Username = %1$s\n", login.m_Username);	
		
		return login;
	}
	
	public static LoginDetails loadProperties(String propertiesFile) throws Exception
	{
		LoginDetails login = new LoginDetails();
		
		// Load the properties file
		
		Properties props = new Properties();
		try 
		{
			InputStream inputStream = new FileInputStream(new File(propertiesFile));

			props.load(inputStream);
		}
		catch (IOException e)
		{
			System.out.println("Could not load properties file '" + propertiesFile + "'");
			System.out.println(e);
			
			throw e;
		}
		
		login.m_Host = props.getProperty("host"); 
		login.m_Port = Integer.parseInt((String)props.get("port"));
		login.m_Username = props.getProperty("username");
		login.m_Password = props.getProperty("password");
		
		return login;
	}
	
	
	static private void createLoginPropertiesFile(LoginDetails login, String outputDirectory)
	{
		Properties props = new Properties();
		props.setProperty("host", login.m_Host);
		props.setProperty("port", new Integer(login.m_Port).toString());
		props.setProperty("username", login.m_Username);
		props.setProperty("password", login.m_Password);
		
		File file = new File(outputDirectory, "login.properties");
		
		System.out.println("Writing " + file.toString());

		FileOutputStream os;
		try 
		{
			os = new FileOutputStream(file);
			props.store(os, "");
			os.close();
		}
		catch (FileNotFoundException e) 
		{
			System.out.println(e);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
	}
	
	private static Options createOptions()
	{
		Options options = new Options();
		options.addOption(new Option("h", "help", false, "Show this help"));
		options.addOption(new Option("l", "login", true, "Name of a java properties file containing " +
				"the login details host, port, username & password"));
		options.addOption(new Option("c", "createLogin", false, "Create a login properties file which can be used with -l option."));
		options.addOption(new Option("o", "outputDir", true, "The proxy config directory for the config files." +
														"i.e. $PRELERT_HOME/proxy/config/"));
		options.addOption(new Option("a", "allMetrics", false, "Create queries for every individual metric of each agent."));
		options.addOption(new Option("5", "fiveBasicMetrics", false, "Create queries for the 5 basic metrics " +
										"(Average Response Time (ms), Concurrent Invocations, " +
										"Errors Per Interval, Responses Per Interval, Stall Count)"));
		options.addOption(new Option("C", "commonMetrics", false, "Create queries for all the common metrics " +
										"(The 5 basic metrics & others)."));
		options.addOption(new Option("g", "listAgents", false, "List Agents"));
		options.addOption(new Option("r", "agentRegex", true, "List only Agents matching this regex."));
		options.addOption(new Option("m", "metricRegex", true, "List only Metrics matching this regex."));
		options.addOption(new Option("M", "moduleRegex", true, "List only Alerts in Modules matching this regex."));
		options.addOption(new Option("A", "alertRegex", true, "List only Alerts matching this regex."));
		
		return options;		
	}
	
	
	public static void main(String[] args) throws Exception
	{
		final String USAGE_MESSAGE = 
			"catalogueIntroscope [--allMetrics | --commonMetrics | --fiveBasicMetrics] [--agentRegex --metricRegex]";
		final String HELP_MESSAGE = 
			"CatalogueIntroscope generates xml files containing queries for CA APM agent " + 
			"data and alerts. The files are used as input to the Prelert CA APM product.\n " +
			"If none of --allMetrics, --commonMetrics or --fiveBasicMetrics are specified " + 
			"the default metric regex '.*' is used unless the --metricRegex option is specified " +
			"in which case that parameter is used.\n\n" +
			"The default --agentRegex is '^(?!Custom Metric Host \\(Virtual\\)).+' which excludes all " +
			"Custom metric agents.\n\n" +
			"Use --moduleRegex and --alertRegex to specify which alerts should be queried.\n" + 
			"On *nix type systems the regex parameters should be inside single quotes '' " +
			"so they are not evaluated by the shell. On Windows the parameters should be enclosed " +
			"in double quotes \"\"\n" +
			"Examples:\n" +
			"	./catalogueIntroscope.sh --metricRegex='^(?!Heuristics).*'\n" +
			"	./catalogueIntroscope.sh --agentRegex='WebServer.*' --metricRegex='^(?!Heuristics).*'\n" +
			"	./catalogueIntroscope.sh --moduleRegex='WebSphere.*' --alertRegex='Alert.*'\n"; 
		
		Options clOptions = createOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try
		{
			cmd = parser.parse(clOptions, args);
		}
		catch (org.apache.commons.cli.ParseException pe)
		{
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(USAGE_MESSAGE, HELP_MESSAGE, clOptions, "");
			return;
		}
		
		if (cmd.hasOption("help"))
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(USAGE_MESSAGE, HELP_MESSAGE, clOptions, "");
			return;
		}
		
		// Check for a login file
		LoginDetails login;
		if (cmd.hasOption("l"))
		{
			login = loadProperties(cmd.getOptionValue("l"));
		}
		else
		{
			login = getLogin();
		}
		
		CatalogueIntroscope catalogueIntroscope = new CatalogueIntroscope(login.m_Host, login.m_Port, 
																login.m_Username, login.m_Password);
		
	
		String outputDirectory = cmd.getOptionValue("o"); 
		
		
		if (cmd.hasOption("c"))
		{
			createLoginPropertiesFile(login, outputDirectory);
			return;
		}
		
		String agentRegex = null;
		if (cmd.hasOption("agentRegex"))
		{
			agentRegex = cmd.getOptionValue("agentRegex");
		}
		String metricRegex = null;
		if (cmd.hasOption("metricRegex"))
		{
			metricRegex = cmd.getOptionValue("metricRegex");			
		}
		
		
		Collection<List<MetricGroup>> metricQueries = Collections.emptyList();
		

		if (cmd.hasOption("fiveBasicMetrics"))
		{
			metricQueries = catalogueIntroscope.getFiveMainMetricQueries(agentRegex);
		}	
		else if (cmd.hasOption("commonMetrics"))
		{
			metricQueries = catalogueIntroscope.getCommonMetricQueries(agentRegex);
		}
		else if (cmd.hasOption("allMetrics"))
		{
			metricQueries = catalogueIntroscope.getAllMetricQueriesForRegex(agentRegex, metricRegex);
		}
		else
		{
			metricQueries = catalogueIntroscope.getSimpleMetricQueries(agentRegex, 
															metricRegex);
		}
		
		
		// Get the alerts
		String moduleRegex = null;
		if (cmd.hasOption("moduleRegex"))
		{
			moduleRegex = cmd.getOptionValue("moduleRegex");
		}
		String alertRegex = ".*";
		if (cmd.hasOption("alertRegex"))
		{
			alertRegex = cmd.getOptionValue("alertRegex");			
		}
		
		List<AlertGroup> alertQueries = catalogueIntroscope.getAlertQueries(moduleRegex, alertRegex);		
		Collections.sort(alertQueries, new Comparator<AlertGroup>() {
				@Override
				public int compare(AlertGroup a, AlertGroup b)
				{
					return a.getModule().compareTo(b.getModule());
				} });
		
		File metricFile = new File(outputDirectory + File.separator + "plugins", METRIC_QUERIES_FILENAME);
		MetricGroup.writeAsXml(metricFile.toString(), metricQueries);
		
		File alertFile = new File(outputDirectory + File.separator + "plugins", ALERT_QUERIES_FILENAME);
		AlertGroup.writeAsXml(alertFile.toString(), alertQueries);
		
		// Load the data type config file in the data type directory 
		// and save the connection details.
		updateDataTypeConfigFile(outputDirectory, login);
		
		// Call exit to quit the CLW threads
		System.exit(0);
	}

}
