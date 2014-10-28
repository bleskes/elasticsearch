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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
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

/**
 * Utility class which defines the parameters used in Alert queries.
 * Has static methods for parsing the XML configuration files 
 * for Introscope Alert queries.
 */
public class AlertGroup
{
	static final private String ALERT_QUERIES = "alert_queries";
	static final private String ALERT_QUERY = "alert_query";
	static final private String MODULE = "module";
	static final private String AGENT = "agent";
	static final private String ALERT = "alert";

	final private String m_Module;
	final private String m_Agent;
	final private String m_Alert;
	
	public AlertGroup(String module, String agentIdentifier, String alertIdentifier)
	{
		m_Module = module;
		m_Agent = agentIdentifier;
		m_Alert = alertIdentifier;
	}
	
	public String getModule()
	{
		return m_Module;
	}
	
	public String getAgentIdentifier ()
	{
		return m_Agent;
	}
		
	public String getAlertIdentifier()
	{
		return m_Alert;
	}
	
	
	
	/**
	 * Static function which reads a number of <code>AlertGroup</code> definitions
	 * from the .xml file pointed to by the inputstream.
	 * @param inputStream Input stream is an stream to an .xml file containing
	 * 		<code>AlertGroup</code> definitions.
	 * @return List<AlertGroup> loaded from xml or throws an exception.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParseException
	 */
	static public List<AlertGroup> loadAlertGroups(InputStream inputStream) 
	throws ParserConfigurationException, SAXException, IOException, ParseException
	{
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse (inputStream);
		
		List<AlertGroup> result = new ArrayList<AlertGroup>();

		NodeList nodes = doc.getElementsByTagName(ALERT_QUERY);
		for (int i=0; i<nodes.getLength(); i++)
		{
			String module = null;
			String agent = null;
			String alert = null;
			
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
				else if (nodeName.equals(AGENT))
				{
					agent = nd.getTextContent();
				}
				else if (nodeName.equals(AGENT))
				{
					agent = nd.getTextContent();
				}
				else if (nodeName.equals(ALERT))
				{
					alert = nd.getTextContent();
				}
			}
			
			if (module == null)
			{
				throw new ParseException("Invalid file: Module, and Agent " +
						"must be defined for each alert query", 0);
			}
			
			result.add(new AlertGroup(module, agent, alert));
		}

		return result;
	}
	
	
	/**
	 * Write a collection of <code>AlertGroup</code> to a DOM Document then
	 * save that to a file with the name <code>filename</code> 
	 * 
	 * @param filename Path to file, doesn't have to exist.
	 * @param alertGroups Collection of groups to write.
	 * @throws ParserConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	static public void writeAsXml(String filename, List<AlertGroup> alertGroups)
	throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		File file = new File(filename);
		Result result = new StreamResult(file);
		
		writeAsXml(alertGroups, result);
	}
	
	
	/**
	 * Write a collection of <code>AlertGroup</code> to a DOM Document then
	 * transform the document into the transform result <code>result</code>.
	 * 
	 * @param alertGroups
	 * @param result
	 * @throws ParserConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	static public void writeAsXml(List<AlertGroup> alertGroups, Result result)
	throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement(ALERT_QUERIES);
		doc.appendChild(root);
		
		
		for (AlertGroup group : alertGroups)
		{
			Element e = doc.createElement(ALERT_QUERY);

			Element me = doc.createElement(MODULE);
			Text module = doc.createTextNode(group.getModule());
			me.appendChild(module);
			e.appendChild(me);
			
			if (group.getAgentIdentifier() != null)
			{
				Element ge = doc.createElement(AGENT);
				Text agent = doc.createTextNode(group.getAgentIdentifier());
				ge.appendChild(agent);
				e.appendChild(ge);
			}

			if (group.getAlertIdentifier() != null)
			{
				Element ae = doc.createElement(ALERT);
				Text alert = doc.createTextNode(group.getAlertIdentifier());
				ae.appendChild(alert);
				e.appendChild(ae);
			}

			root.appendChild(e);
		}

		Source source = new DOMSource(doc);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	}
}
