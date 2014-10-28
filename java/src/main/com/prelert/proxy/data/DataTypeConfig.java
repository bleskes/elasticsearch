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

package com.prelert.proxy.data;

import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class defines all the configuration parameters for a data type. 
 * This defines everything required to start collecting data for that 
 * type including configuring the InputManager, the connection to the 
 * data source, the plugin to use and the plugin specific properties.
 * 
 * The following properties should be configured:<br/>
 * <dl>
 * <dt>DataSourceType</dt><dd>The data type e.g. Introscope, SCOM, etc</dd> 
 * <dt>PluginName</dt><dd>The plugin identifier. This should match the plugins bean name.</dd> 
 * <dt>InputManagerConfig</dt><dd>The InputManager configuration parameters.</dd> 
 * <dt>SourceConnectionConfig</dt><dd>The connection parameters.</dd> 
 * <dt>PluginProperties</dt><dd>A set of plugin specific properties which will be set on 
 * the plugin with bean id <code>PluginName</code>.</dd> 
 * </dl>
 */
public class DataTypeConfig implements Serializable 
{
	private static final long serialVersionUID = 7681305066515870203L;
	
	/**
	 * Constant strings used in the class Xml representation.
	 */
	private static final String DATATYPECONFIG = "DataTypeConfig"; 
	private static final String DATATYPE = "DataType"; 
	private static final String PLUGINNAME = "PluginName"; 
	private static final String PLUGINPROPERTIES = "PluginProperties";
	private static final String PROPERTY = "Property";
	private static final String KEY = "Key";
	private static final String VALUE = "Value";
	
	private String m_DataType;
	private String m_PluginName;
	
	private InputManagerConfig m_InputManagerConfig; 
	private SourceConnectionConfig m_ConnectionConfig;
	private Map<String, String> m_PluginProperties;
	
	
	public DataTypeConfig()
	{
		m_PluginProperties = new HashMap<String, String>();
	}
	
	
	public DataTypeConfig(String datatype, String pluginName)
	{
		this();
		
		m_DataType = datatype;
		m_PluginName = pluginName;
	}

	
	/**
	 * Copy constructor. 
	 * @param other
	 */
	public DataTypeConfig(DataTypeConfig other)
	{
		this(other.getDataType(), other.getPluginName());
		
		this.m_InputManagerConfig = new InputManagerConfig(other.getInputManagerConfig());
		this.m_ConnectionConfig = new SourceConnectionConfig(other.getSourceConnectionConfig());
		this.m_PluginProperties = new HashMap<String, String>();
		for (String key : other.getPluginProperties().keySet())
		{
			this.m_PluginProperties.put(key, other.getPluginProperties().get(key));
		}
		
	}
	
	/**
	 * Return the data source type. 
	 * This is the actual type of data being collected e.g. Introscope,
	 * Scom, etc.
	 * @return
	 */
	public String getDataType()
	{
		return m_DataType;
	}
	
	public void setDataType(String value)
	{
		m_DataType = value;
	}
	
	
	/**
	 * The plugin identifer. This should match the plugin's bean
	 * id in the plugins.xml file.
	 * @return
	 */
	public String getPluginName()
	{
		return m_PluginName;
	}
	
	public void setPluginName(String value)
	{
		m_PluginName = value;
	}
	
	
	/**
	 * Returns the Inputmanager configuration object or
	 * <code>null</code> if not set.
	 * @return
	 */
	public InputManagerConfig getInputManagerConfig()
	{
		return m_InputManagerConfig;
	}
	
	public void setInputManagerConfig(InputManagerConfig value)
	{
		m_InputManagerConfig = value;
	}
	
	
	/**
	 * Returns the data source connection configuration object or
	 * <code>null</code> if not set.
	 * @return
	 */
	public SourceConnectionConfig getSourceConnectionConfig()
	{
		return m_ConnectionConfig;
	}
	
	public void setSourceConnectionConfig(SourceConnectionConfig value)
	{
		m_ConnectionConfig = value;
	}
	
	
	/**
	 * A map of property values that should be set on the plugin.
	 * The properties are arbitrary and only need to be understood 
	 * by the plugin they are set on.
	 *   
	 * @return
	 */
	public Map<String, String> getPluginProperties()
	{
		return m_PluginProperties;
	}
	
	public void setPluginProperties(Map<String, String> value)
	{
		m_PluginProperties = value;
	}
	
	public void addPluginProperty(String key, String value)
	{
		m_PluginProperties.put(key, value);
	}
	
	
	@Override 
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("DataType = " + m_DataType + "; ");
		strRep.append("PluginName = " + m_PluginName + "; ");
		strRep.append("PluginProperties = " + m_PluginProperties + "; ");
		strRep.append("Connection = " + m_ConnectionConfig + "; ");
		strRep.append("InputManager = " + m_InputManagerConfig + "; ");
		strRep.append('}');
		
		return strRep.toString();
	}
	
	
	@Override 
	public int hashCode()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append(m_DataType);
		strRep.append(m_PluginName);
		strRep.append(m_PluginProperties.toString());
		strRep.append(m_ConnectionConfig);
		strRep.append(m_InputManagerConfig);
		strRep.append('}');
		
		return strRep.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null) 
		{
			return false;
		}
		
	    if (other == this) 
	    {
	    	return true;
	    }
	    
	    if (this.getClass() != other.getClass())
	    {
	    	return false;
	    }
	    
	    DataTypeConfig otherConfig = (DataTypeConfig)other;
	   
	    boolean result = bothNullOrEqual(this.m_DataType, otherConfig.getDataType());
	    result = result && bothNullOrEqual(this.m_PluginName, otherConfig.getPluginName());
	    result = result && bothNullOrEqual(this.m_InputManagerConfig, otherConfig.getInputManagerConfig());
	    result = result && bothNullOrEqual(this.m_ConnectionConfig, otherConfig.getSourceConnectionConfig());
	    result = result && bothNullOrEqual(this.m_PluginProperties, otherConfig.getPluginProperties());
	    
	    return result;
	}
		
	
	/**
	 * Returns true if both arg1 and arg2 are null or are of the
	 * same type and equal to each other.
	 * 
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private boolean bothNullOrEqual(Object arg1, Object arg2)
	{
		if (arg1 == null && arg2 == null)
		{
			return true;
		}
		
		if (arg1 != null)
		{
			return arg1.equals(arg2);
		}
		else 
		{
			return false;
		}
	}

	
	/**
	 * Returns a XML representation of this object.
	 * @return
	 * @throws ParserConfigurationException
	 */
	public Document toXml() throws ParserConfigurationException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement(DATATYPECONFIG);
		doc.appendChild(root);

		if (getDataType() != null)
		{
			Element e = doc.createElement(DATATYPE);
			e.appendChild(doc.createTextNode(getDataType()));
			root.appendChild(e);
		}
		
		if (m_ConnectionConfig != null)
		{
			Document connectParams = m_ConnectionConfig.toXml();
			Node connectNode = doc.importNode(connectParams.getFirstChild(), true);
			root.appendChild(connectNode);
		}
		
		if (m_InputManagerConfig != null)
		{
			Document imParams = m_InputManagerConfig.toXml();
			Node imNode = doc.importNode(imParams.getFirstChild(), true);
			root.appendChild(imNode);
		}
		
		if (getPluginName() != null)
		{
			Element e = doc.createElement(PLUGINNAME);
			e.appendChild(doc.createTextNode(getPluginName()));
			root.appendChild(e);
		}
		
		if (getPluginProperties() != null)
		{
			Element properties = doc.createElement(PLUGINPROPERTIES);
			root.appendChild(properties);
			
			Set<String> keyset = m_PluginProperties.keySet();
			for (String key : keyset)
			{
				Element property = doc.createElement(PROPERTY);
				properties.appendChild(property);
				
				Element e = doc.createElement(KEY);
				e.appendChild(doc.createTextNode(key));
				property.appendChild(e);
				
				e = doc.createElement(VALUE);
				e.appendChild(doc.createTextNode(m_PluginProperties.get(key)));
				property.appendChild(e);
			}
		}
	
		return doc;
	}
	
	
	/**
	 * Constructs a <code>DataSourceConfig</code> from its 
	 * XML representation.
	 * 
	 * @param doc The xml document should contain a DATASOURCECONFIG element.
	 * @return A new DataSourceConfig or <code>null</code>.
	 * @throws ParseException
	 */
	static public DataTypeConfig fromXml(Document doc)
	throws ParseException
	{
		NodeList nodes = doc.getElementsByTagName(DATATYPECONFIG);
		if (nodes.getLength() == 0)
		{
			return null;
		}
		
		NodeList groupNodes = nodes.item(0).getChildNodes();
		if (groupNodes.getLength() == 0)
		{
			return null;
		}
		
		
		DataTypeConfig result = new DataTypeConfig();
		result.m_ConnectionConfig = SourceConnectionConfig.fromXml(doc);
		result.m_InputManagerConfig = InputManagerConfig.fromXml(doc);
		
		for (int i=0; i<groupNodes.getLength(); i++)
		{
			Node nd = groupNodes.item(i);
			String nodeName = nd.getNodeName();

			if (nodeName.equals(DATATYPE))
			{
				result.setDataType(nd.getTextContent());
			}
			else if (nodeName.equals(PLUGINNAME))
			{
				result.setPluginName(nd.getTextContent());
			}
			else if (nodeName.equals(PLUGINPROPERTIES))
			{
				NodeList propNodes = nd.getChildNodes();
				for (int j=0; j<propNodes.getLength(); j++)
				{
					String key = null;
					String value = null;
					Node propNode = propNodes.item(j);
					if (propNode.getNodeName() == PROPERTY) 
					{
						NodeList keyValue = propNode.getChildNodes();
						for (int k=0; k<keyValue.getLength(); k++)
						{
							if (keyValue.item(k).getNodeName().equals(KEY))
							{
								key = keyValue.item(k).getTextContent();
							}
							
							if (keyValue.item(k).getNodeName().equals(VALUE))
							{
								value = keyValue.item(k).getTextContent();
							}
						}
						
						if (key != null && value != null)
						{
							result.addPluginProperty(key, value);
						}
					}
					
				}
				
			}

		}
		
		
		return result;
	}
}
