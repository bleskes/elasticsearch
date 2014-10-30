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

package com.prelert.data;

import java.io.Serializable;

public class Attribute implements Serializable, Comparable<Attribute>
{
	private static final long serialVersionUID = 5152382371830525972L;

	private String	m_AttributeName;
	private String 	m_AttributeValue;
	private String  m_AttributePrefix;
	private int 	m_AttributePosition = -1;
	
	/**
	 * Creates an empty attribute, with name and value not set.
	 */
	public Attribute()
	{
		
	}
	
	
	/**
	 * Creates an attribute with the given name and value.
	 * @param attributeName the attribute name.
	 * @param attributeValue the attribute value.
	 */
	public Attribute(String attributeName, String attributeValue)
	{
		this();
		
		m_AttributeName = attributeName;
		m_AttributeValue = attributeValue;
	}
	
	
	/**
	 * Creates an attribute with the given name, value, prefix and
	 * path position.
	 *
	 * @param attributeName the attribute name.
	 * @param attributeValue the attribute value.
	 * @param prefix the default value is <code>null</code> if no
	 * 		  prefix is required.
	 * @param position the attribute's position on the metric path.
	 * 		  A negative number indicates an unspecified position.
	 */
	public Attribute(String attributeName, String attributeValue, String prefix, int position)
	{
		this(attributeName, attributeValue);
		
		m_AttributePrefix = prefix;
		m_AttributePosition = position;
	}


	/**
	 * Returns the attribute name.
	 * @return the name of the attribute.
	 */
	public String getAttributeName()
    {
    	return m_AttributeName;
    }


	/**
	 * Sets the attribute name.
	 * @param name the attribute name.
	 */
	public void setAttributeName(String name)
    {
		m_AttributeName = name;
    }


	/**
	 * Returns the attribute value.
	 * @return the attribute value.
	 */
	public String getAttributeValue()
    {
    	return m_AttributeValue;
    }


	/**
	 * Sets the attribute value.
	 * @param attributeValue the attribute value.
	 */
	public void setAttributeValue(String value)
    {
		m_AttributeValue = value;
    }
	
	
	/**
	 * Returns the attribute path prefix. The prefix
	 * may be <code>null</code> if not set.
	 * @return the attribute path prefix. May be <code>null</code>
	 */
	public String getAttributePrefix()
	{
		return m_AttributePrefix;
	}
	
	
	/**
	 * Set the attribute path prefix
	 * @param prefix
	 */
	public void setAttributePrefix(String prefix)
	{
		m_AttributePrefix = prefix;
	}
	
	
	/**
	 * Returns the position in the metric path of the attribute.
	 * A negative value means the position has not be set.
	 * @return A +ve number if the position is set else a
	 * 		   -ve value is returned.
	 */
	public int getAttributePosition()
	{
		return m_AttributePosition;
	}
	
	
	/**
	 * Sets the attribute's position of the in the metric path.
	 * A -ve value indicates the position is unset.
	 * @param position
	 */
	public void setAttributePosition(int position)
	{
		m_AttributePosition = position;
	}
	
	
	/**
	 * Tests this Attribute for equality with another object.
	 * @return true if the comparison object is an Attribute object with
	 * 	identical name and value.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Attribute))
        {
            return false;
        }

        Attribute other = (Attribute)obj;
    	
    	// Compare names.
    	if (m_AttributeName != null)
    	{
    		if (m_AttributeName.equals(other.getAttributeName()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributeName() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare values.
    	if (m_AttributeValue != null)
    	{
    		if (m_AttributeValue.equals(other.getAttributeValue()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributeValue() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare prefix
    	if (m_AttributePrefix != null)
    	{
    		if (m_AttributePrefix.equals(other.getAttributePrefix()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributePrefix() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare position
    	if (m_AttributePosition >= 0 && other.getAttributePosition() >= 0)
    	{
    		if (m_AttributePosition != other.getAttributePosition())
    		{
    			return false;
    		}
    	}
    	else if ((m_AttributePosition < 0 && other.getAttributePosition() < 0) == false)
    	{
    		// if both numbers are not -ve or not >= 0 then the signs
    		// are different and they are not equal.
    		// all -ve values are considered the same.
    		return false;
    	}
    	
    	return true;
    }
	
	
	/**
	 * Returns a String representation of this attribute.
	 * @return a String listing the attribute name and value.
	 */
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append(m_AttributeName);
		strRep.append('=');
		strRep.append(m_AttributeValue);
		strRep.append(", prefix=" + m_AttributePrefix);
		strRep.append(", position=" + m_AttributePosition);
		strRep.append('}');
		
		return strRep.toString();
	}


	/**
	 * Returns an XML tag representation of this attribute.  For
	 * external data, the back end processes don't need to know the
	 * position and prefix for attributes.
	 * @return an XML tag containing the attribute name and value.
	 */
	public String toXmlTagExternal()
	{
		StringBuilder strRep = new StringBuilder("<attribute ");
		strRep.append("name='");
		strRep.append(m_AttributeName);
		strRep.append("'>");
		strRep.append(XmlStringEscaper.escapeXmlString(m_AttributeValue));
		strRep.append("</attribute>");
		
		return strRep.toString();
	}


	/**
	 * Returns an XML tag representation of this attribute,
	 * including the position and prefix (if any).
	 * @return an XML tag containing the attribute name and value.
	 */
	public String toXmlTagInternal()
	{
		StringBuilder strRep = new StringBuilder("<attribute ");
		if (m_AttributePrefix != null)
		{
			strRep.append("prefix='");
			strRep.append(m_AttributePrefix);
			strRep.append("' ");
		}
		if (m_AttributePosition >= 0)
		{
			strRep.append("position='");
			strRep.append(m_AttributePosition);
			strRep.append("' ");			
		}
		strRep.append("name='");
		strRep.append(m_AttributeName);
		strRep.append("'>");
		strRep.append(XmlStringEscaper.escapeXmlString(m_AttributeValue));
		strRep.append("</attribute>");

		return strRep.toString();
	}


	/**
	 * Ordering on Attributes is just done on the Attribute name.
	 */
	@Override
	public int compareTo(Attribute other)
	{
		return m_AttributeName.compareTo(other.getAttributeName());
	}

}
