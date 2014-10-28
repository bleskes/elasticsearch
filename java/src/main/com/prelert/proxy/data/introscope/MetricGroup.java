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
package com.prelert.proxy.data.introscope;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.prelert.proxy.regex.RegExUtilities;

/**
 * Utility class which defines the Metric Groups used in Introscope.
 */
public class MetricGroup implements Serializable
{
	private static final long serialVersionUID = -5261716621933537642L;
	
	final static private String METRIC_GROUP = "metric_group";
	final static private String METRIC_GROUPS = "metric_groups";
	final static private String MODULE = "module";
	final static private String NAME = "name";
	final static private String AGENT = "agent";
	final static private String METRIC = "metric";

	final private String m_Module;
	final private String m_Name;
	final private String m_Agent;
	final private String m_Metric;
	final private boolean m_MetricIsARegex;
	
	public MetricGroup(String module, String metricGroupName, String agent, String metric,
					boolean metricIsARegex)
	{
		m_Module = module;
		m_Name = metricGroupName;
		m_Agent = agent;
		m_Metric = metric;
		m_MetricIsARegex = metricIsARegex;
	}
	
	public MetricGroup(String agent, String metric, boolean metricIsARegex)
	{
		m_Module = null;
		m_Name = null;
		m_Agent = agent;
		m_Metric = metric;
		m_MetricIsARegex = metricIsARegex;
	}

	public String getModule()
	{
		return m_Module;
	}
	
	public String getMetricGroupName()
	{
		return m_Name;
	}
	
	public String getAgent()
	{
		return m_Agent;
	}
	
	public String getMetric()
	{
		return m_Metric;
	}
	
	public boolean isMetricIsARegex()
	{
		return m_MetricIsARegex;
	}

	/**
	 * Only compares the Agent and Metric strings.
	 * 
	 * @param obj
	 * @return true if the agent and metric strings match.
	 */
	@Override
	public boolean equals(Object obj)
	{
	 	if (this == obj)
	 	{
	 		return true;
	 	}
	 	
	 	if (!(obj instanceof MetricGroup))
	 	{
	 		return false;
	 	}
	 	
	 	MetricGroup other = (MetricGroup)obj;
	 	
	 	return this.m_Metric.equals(other.m_Metric) && this.m_Agent.equals(other.m_Agent);
	}
	
	@Override 
	public int hashCode()
	{
		return (m_Agent + m_Metric).hashCode();
	}
	
	@Override 
	public String toString()
	{
		return m_Agent + "  " + m_Metric;
	}
	
	
	/**
	 * Static function which reads a number of <code>MetricGroup</code> definitions
	 * from the .xml file pointed to by the inputstream.
	 * @param inputStream Input stream is an stream to an .xml file containing
	 * 		<code>MetricGroup</code> definitions.
	 * @return List<MetricGroup> loaded from xml or throws an exception.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParseException
	 */
	static public List<MetricGroup> loadMetricGroups(InputStream inputStream) 
	throws ParserConfigurationException, SAXException, IOException, ParseException
	{
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse (inputStream);
		
		List<MetricGroup> result = new ArrayList<MetricGroup>();

		NodeList nodes = doc.getElementsByTagName(METRIC_GROUP);
		for (int i=0; i<nodes.getLength(); i++)
		{
			String module = null;
			String groupName = null;
			String agent = null;
			String metricPath = null;
			
			Node node = nodes.item(i);
			NodeList groupNodes = node.getChildNodes();
			for (int j=0; j<groupNodes.getLength(); j++)
			{
				Node nd = groupNodes.item(j);
				String nodeName = nd.getNodeName();
			
				if (nodeName.equals(MODULE))
				{
					module = nd.getTextContent();
				}
				else if (nodeName.equals(NAME))
				{
					groupName = nd.getTextContent();
				}
				else if (nodeName.equals(AGENT))
				{
					agent = nd.getTextContent();
				}
				else if (nodeName.equals(METRIC))
				{
					metricPath = nd.getTextContent();
				}
			}
			
			if (agent == null || metricPath == null)
			{
				throw new ParseException("Invalid file: Agent & Metric" +
						" must be defined for each metric grouping", 0);
			}
			
			int lastIndex = metricPath.lastIndexOf(":");
			String metric = metricPath.substring(lastIndex + 1);	
			boolean metricIsARegex = RegExUtilities.stringIsARegex(metric);

			result.add(new MetricGroup(module, groupName, agent, metricPath, metricIsARegex));
		}

		return result;
	}
	
	
	/**
	 * Write a collection of <code>MetricGroup</code> to a DOM Document then
	 * save that to a file with the name <code>filename</code> 
	 * 
	 * @param filename Path to file, doesn't have to exist.
	 * @param metricGroups Collection of groups to write.
	 * @throws ParserConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	static public void writeAsXml(String filename, Collection<List<MetricGroup>> metricGroups)
	throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		File file = new File(filename);
		Result result = new StreamResult(file);

		writeAsXml(metricGroups, result);
	}
	
	
	/**
	 * Write a collection of <code>MetricGroup</code> to a DOM Document then
	 * transform the document into the transform result <code>result</code>.
	 * 
	 * @param metricGroups
	 * @param result
	 * @throws ParserConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	static public void writeAsXml(Collection<List<MetricGroup>> metricGroups, Result result)
	throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement(METRIC_GROUPS);
		doc.appendChild(root);
		
		for (List<MetricGroup> groups : metricGroups)
		{
			for (MetricGroup group : groups)
			{
				Element e = doc.createElement(METRIC_GROUP);

				Element ae = doc.createElement("agent");
				Text agent = doc.createTextNode(group.getAgent());
				ae.appendChild(agent);
				e.appendChild(ae);
				
				Element mte = doc.createElement("metric");
				Text metric = doc.createTextNode(group.getMetric());
				mte.appendChild(metric);
				e.appendChild(mte);
				
				root.appendChild(e);
			}
		}
		
		
		Source source = new DOMSource(doc);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	}
}
