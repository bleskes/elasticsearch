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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * A generic class for holding connection and login information.
 * This can be used to store database login details. 
 * 
 * The username, host and password variables are all trimmed 
 * before they are set.
 */
public class SourceConnectionConfig implements Serializable 
{
	private static final long serialVersionUID = -2440640606324389171L;

	/**
	 * Constant strings used in the class Xml representation.
	 */
	private static final String SOURCECONNECTIONCONFIG = "SourceConnectionConfig"; 
	private static final String HOST = "Host"; 
	private static final String PORT = "Port"; 
	private static final String USERNAME = "User"; 
	private static final String PASSWORD = "Password"; 


	private String m_Host;
	private Integer m_Port;
	private String m_Username; 
	private String m_Password;

	
	public SourceConnectionConfig()
	{
	}
	
	
	/**
	 * Copy constructor performs defensive copy
	 * @param config
	 */
	public SourceConnectionConfig(SourceConnectionConfig config)
	{
		m_Host = config.getHost();
		m_Port = (config.getPort() == null) ? null : new Integer(config.getPort());
		m_Username = config.getUsername();
		m_Password = config.getPassword();
	}

	public SourceConnectionConfig(String host, Integer port, String username, String password)
	{
		m_Host = (host == null) ? null : host.trim();
		m_Port = port;
		m_Username = (username == null) ? null : username.trim();
		m_Password = (password == null) ? null : password.trim();
	}
	
	
	/**
	 * Return the host machine name or <code>null</code> if not set. 
	 * @return
	 */
	public String getHost()
	{
		return m_Host;
	}

	public void setHost(String host) 
	{
		this.m_Host = (host == null) ? null : host.trim();
	}

	
	/**
	 * Return the port number or <code>null</code> if not set. 
	 * @return
	 */
	public Integer getPort() 
	{
		return m_Port;
	}

	public void setPort(Integer port) 
	{
		this.m_Port = port;
	}

	
	/**
	 * Return the user login name or <code>null</code> if not set. 
	 * @return
	 */
	public String getUsername() 
	{
		return m_Username;
	}

	public void setUsername(String username) 
	{
		this.m_Username = (username == null) ? null : username.trim();
	}

	
	/**
	 * Return the password or <code>null</code> if not set. 
	 * @return
	 */
	public String getPassword() 
	{
		return m_Password;
	}

	public void setPassword(String password) 
	{
		this.m_Password = (password == null) ? null : password.trim();
	}
	
	
	
	@Override 
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("Host = " + m_Host + "; ");
		strRep.append("Port = " + m_Port + "; ");
		strRep.append("User = " + m_Username + "; ");
		strRep.append('}');
		
		return strRep.toString();
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
	    
	    if ((other instanceof SourceConnectionConfig) == false)
	    {
	    	return false;
	    }
	    
	    SourceConnectionConfig otherConfig = (SourceConnectionConfig)other;
	   
	    boolean result = bothNullOrEqual(this.m_Host, otherConfig.getHost());
	    result = result && bothNullOrEqual(this.m_Port, otherConfig.getPort());
	    result = result && bothNullOrEqual(this.m_Username, otherConfig.getUsername());
	    result = result && bothNullOrEqual(this.m_Password, otherConfig.getPassword());
	    
	    return result;
	}
		
	@Override 
	public int hashCode()
	{
		return this.toString().hashCode();
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
	 * Serialise this object to Xml.
	 * 
	 * @return A new Xml document.
	 * @throws ParserConfigurationException
	 */
	public Document toXml() throws ParserConfigurationException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.newDocument();
		
		Element root = doc.createElement(SOURCECONNECTIONCONFIG);
		doc.appendChild(root);

		if (getHost() != null)
		{
			Element e = doc.createElement(HOST);
			e.appendChild(doc.createTextNode(getHost()));
			root.appendChild(e);
		}
		
		if (getPort() != null)
		{
			Element e = doc.createElement(PORT);
			e.appendChild(doc.createTextNode(getPort().toString()));
			root.appendChild(e);
		}
		
		if (getUsername() != null)
		{
			Element e = doc.createElement(USERNAME);
			e.appendChild(doc.createTextNode(getUsername()));
			root.appendChild(e);
		}

		if (getPassword() != null)
		{
			Element e = doc.createElement(PASSWORD);
			e.appendChild(doc.createCDATASection(getPassword()));
			root.appendChild(e);
		}
		
		
		return doc;
	}
	
	
	/**
	 * Returns a new SourceConnectionConfig populated from the Xml document
	 * or <code>null</code> if no SOURCECONNECTIONCONFIG element is found or
	 * it is empty.
	 * 
	 * @param doc The xml document should contain a SOURCECONNECTIONCONFIG element.
	 * @return A new SourceConnectionConfig or <code>null</code>.
	 */
	static public SourceConnectionConfig fromXml(Document doc)
	{
		NodeList nodes = doc.getElementsByTagName(SOURCECONNECTIONCONFIG);
		if (nodes.getLength() == 0)
		{
			return null;
		}
		
		NodeList groupNodes = nodes.item(0).getChildNodes();
		if (groupNodes.getLength() == 0)
		{
			return null;
		}
		
		SourceConnectionConfig result = new SourceConnectionConfig();

		for (int i=0; i<groupNodes.getLength(); i++)
		{
			Node nd = groupNodes.item(i);
			String nodeName = nd.getNodeName();

			if (nodeName.equals(HOST))
			{
				result.setHost(nd.getTextContent());
			}
			else if (nodeName.equals(PORT))
			{
				try
				{
					Integer port = Integer.parseInt(nd.getTextContent());
					result.setPort(port);
				}
				catch (NumberFormatException e)
				{
				}
			}
			else if (nodeName.equals(USERNAME))
			{
				result.setUsername(nd.getTextContent());
			}
			else if (nodeName.equals(PASSWORD))
			{
				result.setPassword(nd.getTextContent());
			}
		}
		
		return result;
	}

}
