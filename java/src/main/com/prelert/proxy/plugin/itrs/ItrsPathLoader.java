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

package com.prelert.proxy.plugin.itrs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;


/**
 * Class containing static methods for reading the ITRS path
 * config from file.
 */
public class ItrsPathLoader 
{
	public static String FILTERS_TAG = "filters";
	public static String POINTFILTERS_TAG = "pointFilter";
	
	private static Logger s_Logger = Logger.getLogger(ItrsPathLoader.class);
	
	
	/**
	 * Read the ITRS path config from the Xml file.
	 * 
	 * @see ItrsPathLoader#loadPaths(InputStream inputStream)
	 * @param filename - Name of the file containing the Xml config
	 * @return The list of ITRS paths.
	 * @throws FileNotFoundException
	 */
	static List<String> loadPaths(String filename)
	throws FileNotFoundException
	{
		InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
		if (inputStream == null)
		{
			throw new FileNotFoundException(filename);
		}
		return loadPaths(inputStream);		
	}
	
	
	/**
	 * Load the Xml config from the stream.
	 * 
	 * The Xml should contain a filter element with a list of pointfilters like 
	 * this:
	 * 
	 * 	 <filters> 
	 * 		<pointFilter>/geneos/gateway/directory/etc</pointFilter>
	 *          ...
	 *   </filters>
	 * 
	 * @param inputStream
	 * @return
	 */
	static List<String> loadPaths(InputStream inputStream)
	{
		List<String> paths = new ArrayList<String>(); 
        
		try 
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (inputStream);
			
			NodeList filterNodes = doc.getElementsByTagName(FILTERS_TAG);
			for (int i=0; i < filterNodes.getLength(); i++)
			{
				Node filterNode = filterNodes.item(i);
				NodeList pointFilterNodes = filterNode.getChildNodes();
				
				for (int j=0; j<pointFilterNodes.getLength(); j++)
				{
					Node nd = pointFilterNodes.item(j);
				
					if (POINTFILTERS_TAG.equals(nd.getNodeName()))
					{
						paths.add(nd.getTextContent());
					}
				}
								
			}
		}
		catch (ParserConfigurationException e) 
		{
			s_Logger.error("Cannot read the ITRS paths", e);
		}
		catch (SAXException e) 
		{
			s_Logger.error("Cannot read the ITRS paths", e);						
		}
		catch (IOException e) 
		{
			s_Logger.error("Cannot read the ITRS paths", e);
		}
				
		return paths;
	}
	
	
	/**
	 * Write the list of ITRS paths to file in the ITRS config format.
	 * 
	 * <filters> 
	 *     <pointFilter>/geneos/gateway/directory/etc</pointFilter>
	 *     ...
	 * </filters>
	 *  
	 * @param filename
	 * @param paths
	 * @throws ParserConfigurationException 
	 * @throws TransformerFactoryConfigurationError 
	 * @throws TransformerException 
	 */
	static public void toXml(String filename, List<String> paths) 
	throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		File file = new File(filename);
		Result result = new StreamResult(file);
		
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement(FILTERS_TAG);
		doc.appendChild(root);
		
		for (String path : paths)
		{
			Element el = doc.createElement(POINTFILTERS_TAG);
			Text pointFilter = doc.createTextNode(path);
			el.appendChild(pointFilter);
			root.appendChild(el);
		}
		
		
		Source source = new DOMSource(doc);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	}
	
}
