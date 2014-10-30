/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

public class Attribute implements Serializable
{
	
	private String	m_AttributeName;
	private String 	m_AttributeValue;
	
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
		m_AttributeName = attributeName;
		m_AttributeValue = attributeValue;
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
	 * Tests this Attribute for equality with another object.
	 * @return true if the comparison object is an Attribute object with
	 * 	identical name and value.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(!(obj instanceof Attribute))
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
    	
    	return true;
    }
	
	
	/**
	 * Returns a String representation of this Attribute.
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
		strRep.append('}');
		
		return strRep.toString();
    }
}
