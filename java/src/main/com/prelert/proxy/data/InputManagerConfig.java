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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.prelert.proxy.inputmanager.InputManagerType;

/**
 * Class contains all the config to create and setup an inputmanager.
 */
public class InputManagerConfig implements Serializable 
{
	private static final long serialVersionUID = -8253961963586733687L;
	
	/**
	 * Constant strings used in the class Xml representation.
	 */
	private static final String INPUTMANAGERCONFIG = "InputManagerConfig"; 
	private static final String TYPE = "Type"; 
	private static final String HOST = "Host"; 
	private static final String PORT = "Port"; 
	private static final String QUERYLENGTH = "QueryLength"; 
	private static final String UPDATEINTERVAL = "UpdateInterval"; 
	private static final String STARTDATE = "Startdate"; 
	private static final String ENDDATE = "Enddate"; 
	private static final String DELAY = "Delay";
	
	private InputManagerType m_InputManagerType;
	
	private String m_Host;
	private Integer m_Port;
	
	private Date m_Start;
	private Date m_End;
	
	private Integer m_QueryLengthSecs;
	private Integer m_UpdateIntervalSecs;
	
	private Integer m_Delay;
	
	public InputManagerConfig()
	{
		
	}
	
	/**
	 * Copy constructor performs defensive (deep) copy
	 * @param other 
	 */
	public InputManagerConfig(InputManagerConfig other)
	{
		this();
		
		if (other != null)
		{
			this.m_InputManagerType = other.m_InputManagerType;
			this.m_Host = other.m_Host;
			this.m_Port = (other.m_Port != null) ? new Integer(other.m_Port) : null;
			this.m_Start = (other.m_Start != null) ? new Date(other.m_Start.getTime()) : null;
			this.m_End = (other.m_End != null) ? new Date(other.m_End.getTime()) : null;
			this.m_QueryLengthSecs = (other.m_QueryLengthSecs != null) ? new Integer(other.m_QueryLengthSecs) : null;
			this.m_UpdateIntervalSecs = (other.m_UpdateIntervalSecs != null) ? new Integer(other.m_UpdateIntervalSecs) : null;
			this.m_Delay = (other.m_Delay != null) ? new Integer(other.m_Delay) : null;
		}
	}
	
	/**
	 * The input manager type.	
	 * @return
	 */
	public InputManagerType getInputManagerType()
	{
		return m_InputManagerType;
	}
	
	public void setInputManagerType(InputManagerType value)
	{
		m_InputManagerType = value;
	}
	
	
	/**
	 * The host machine the Prelert processes are running on 
	 * or <code>null</code> if not set.
	 * 
	 * @return The host or <code>null</code> if not set.
	 */
	public String getHost()
	{
		return m_Host;
	}
	
	public void setHost(String value)
	{
		m_Host = value;
	}
	
	
	/**
	 * The port of the Prelert process the input manager will 
	 * connect to.
	 * 
	 * @return The port number or <code>null</code> if not set.
	 */
	public Integer getPort()
	{
		return m_Port;
	}
	
	public void setPort(Integer value)
	{
		m_Port = value;
	}
	
	/**
	 * If for a historical InputManager this returns the 
	 * collection periods start date or <code>null</code> if 
	 * a real-time inputmanager. 
	 * 
	 * @return The start date or <code>null</code>
	 */
	public Date getStartDate()
	{
		return m_Start;
	}
	
	public void setStartDate(Date value)
	{
		m_Start = value;
	}
	
	
	/**
	 * If for a historical InputManager this returns the 
	 * collection periods end date or <code>null</code> if 
	 * there is no specified end date or it is a real-time
	 * inputmanager.
	 * 
	 * @return The end date or <code>null</code>
	 */
	public Date getEndDate()
	{
		return m_End;
	}
	
	public void setEndDate(Date value)
	{
		m_End = value;
	}
	
	
	/**
	 * Get the query length in seconds.
	 * 
	 * @return The query length or <code>null</code>.
	 */
	public Integer getQueryLengthSecs()
	{
		return m_QueryLengthSecs;
	}
	
	public void setQueryLengthSecs(Integer value)
	{
		m_QueryLengthSecs = value;
	}
	
	
	/**
	 * The update interval.
	 * @return The interval or <code>null</code>.
	 */
	public Integer getUpdateIntervalSecs()
	{
		return m_UpdateIntervalSecs;
	}
	
	public void setUpdateIntervalSecs(Integer value)
	{
		m_UpdateIntervalSecs = value;
	}
	
	
	/**
	 * The offset in seconds behind the current time that real-time queries 
	 * run in. To make real-time queries run X seconds behind the 
	 * current time set this property to X.
	 * 
	 * This property only applies to InputManagers in REALTIME data collection mode.
	 * @return The delay value or <code>null</code>.
	 */
	public Integer getDelaySecs()
	{
		return m_Delay;
	}
	
	public void setDelaySecs(Integer value)
	{
		m_Delay = value;
	}
	
	
	@Override 
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append("Type =" + m_InputManagerType + "; ");
		strRep.append("Host = " + m_Host + "; ");
		strRep.append("Port = " + m_Port + "; ");
		strRep.append("Start = " + m_Start + "; ");
		strRep.append("End = " + m_End + "; ");
		strRep.append("QueryLengthSecs = " + m_QueryLengthSecs + "; ");
		strRep.append("UpdateIntervalSecs = " + m_UpdateIntervalSecs + "; ");
		strRep.append("Delay = " + m_Delay + "; ");
		strRep.append('}');
		
		return strRep.toString();
	}
	
	
	/**
	 * The start and end date members are converted to strings in the same
	 * format as they are saved to xml before they are compared so the comparison
	 * is only accurate to the minute.
	 */
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
	    
	    InputManagerConfig otherConfig = (InputManagerConfig)other;
	   
	    boolean result = bothNullOrEqual(this.getInputManagerType(), otherConfig.getInputManagerType());
	    result = result && bothNullOrEqual(this.getHost(), otherConfig.getHost());
	    result = result && bothNullOrEqual(this.getPort(), otherConfig.getPort());
	    result = result && bothNullOrEqual(this.getQueryLengthSecs(), otherConfig.getQueryLengthSecs());
	    result = result && bothNullOrEqual(this.getUpdateIntervalSecs(), otherConfig.getUpdateIntervalSecs());
	    result = result && bothNullOrEqual(this.getDelaySecs(), otherConfig.getDelaySecs());
	    result = result && compareDates(this.getStartDate(), otherConfig.getStartDate());
	    result = result && compareDates(this.getEndDate(), otherConfig.getEndDate());
	    
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
	 * Performs null tests before converting the 2 Date arguments to 
	 * strings and testing them for equality.
	 * 
	 * The dates are compared as strings in the yyyy/MM/dd HH:mm format
	 * because if they have previoulsy been converted from a string 
	 * the ms element would have been lost.
	 *  
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private boolean compareDates(Date arg1, Date arg2)
	{
		if (arg1 == null && arg2 == null)
		{
			return true;
		}
		
		if (arg1 != null && arg2 != null)
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			return dateFormat.format(arg1).equals(dateFormat.format(arg2));
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
		
		Element root = doc.createElement(INPUTMANAGERCONFIG);
		doc.appendChild(root);

		if (getInputManagerType() != null)
		{
			Element type = doc.createElement(TYPE);
			type.appendChild(doc.createTextNode(getInputManagerType().toString()));
			root.appendChild(type);
		}

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
		
		if (getUpdateIntervalSecs() != null)
		{
			Element e = doc.createElement(UPDATEINTERVAL);
			e.appendChild(doc.createTextNode(getUpdateIntervalSecs().toString()));
			root.appendChild(e);
		}

		if (getQueryLengthSecs() != null)
		{
			Element e = doc.createElement(QUERYLENGTH);
			e.appendChild(doc.createTextNode(getQueryLengthSecs().toString()));
			root.appendChild(e);
		}
		
		if (getDelaySecs() != null)
		{
			Element e = doc.createElement(DELAY);
			e.appendChild(doc.createTextNode(getDelaySecs().toString()));
			root.appendChild(e);
		}
		
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		
		if (getStartDate() != null)
		{
			Element e = doc.createElement(STARTDATE);
			e.appendChild(doc.createTextNode(dateFormat.format(getStartDate())));
			root.appendChild(e);
		}

		if (getEndDate() != null)
		{
			Element e = doc.createElement(ENDDATE);
			e.appendChild(doc.createTextNode(dateFormat.format(getEndDate())));
			root.appendChild(e);
		}
		
		
		return doc;
	}
	
	
	/**
	 * Returns a new InputManagerConfig populated from the Xml document
	 * or <code>null</code> if a INPUTMANAGERCONFIG element cannot be found or is 
	 * empty.
	 * 
	 * @param doc The xml document should contain a INPUTMANAGERCONFIG element.
	 * @return A new InputManagerConfig or <code>null</code>.
	 */
	static public InputManagerConfig fromXml(Document doc)
	{
		NodeList nodes = doc.getElementsByTagName(INPUTMANAGERCONFIG);
		if (nodes.getLength() == 0)
		{
			return null;
		}
		
		NodeList groupNodes = nodes.item(0).getChildNodes();
		if (groupNodes.getLength() == 0)
		{
			// Empty item.
			return null;
		}
		
		InputManagerConfig result = new InputManagerConfig();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

		for (int i=0; i<groupNodes.getLength(); i++)
		{
			Node nd = groupNodes.item(i);
			String nodeName = nd.getNodeName();

			if (nodeName.equals(TYPE))
			{
				InputManagerType type = InputManagerType.enumValue(nd.getTextContent());
				result.setInputManagerType(type);
			}
			else if (nodeName.equals(HOST))
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
			else if (nodeName.equals(UPDATEINTERVAL))
			{
				try
				{
					Integer interval = Integer.parseInt(nd.getTextContent());
					result.setUpdateIntervalSecs(interval);
				}
				catch (NumberFormatException e)
				{
				}
			}
			else if (nodeName.equals(QUERYLENGTH))
			{
				try
				{
					Integer length = Integer.parseInt(nd.getTextContent());
					result.setQueryLengthSecs(length);
				}
				catch (NumberFormatException e)
				{
				}
			}
			else if (nodeName.equals(DELAY))
			{
				try
				{
					Integer length = Integer.parseInt(nd.getTextContent());
					result.setDelaySecs(length);
				}
				catch (NumberFormatException e)
				{
				}
			}
			else if (nodeName.equals(STARTDATE))
			{
				try
				{
					Date start = dateFormat.parse(nd.getTextContent());
					result.setStartDate(start);
				}
				catch (ParseException e)
				{
				}
			}
			else if (nodeName.equals(ENDDATE))
			{
				try
				{
					Date end = dateFormat.parse(nd.getTextContent());
					result.setEndDate(end);
				}
				catch (ParseException e)
				{
				}
			}
		}
		
		return result;
	}
}
