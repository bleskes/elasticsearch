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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.data.gxt;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class for a single attribute.
 * @author Pete Harverson
 */
public class AttributeModel extends BaseModelData implements Serializable
{
	
	/**
	 * Creates an empty attribute, with name and value not set.
	 */
	public AttributeModel()
	{
	}
	
	
	/**
	 * Creates an attribute with the given name and value.
	 * @param attributeName the attribute name.
	 * @param attributeValue the attribute value.
	 */
	public AttributeModel(String attributeName, String attributeValue)
	{	
		setAttributeName(attributeName);
		setAttributeValue(attributeValue);
	}
	
	
	/**
	 * Returns the attribute name.
	 * @return the name of the attribute.
	 */
	public String getAttributeName()
    {
    	return get("attributeName");
    }


	/**
	 * Sets the attribute name.
	 * @param name the attribute name.
	 */
	public void setAttributeName(String name)
    {
    	set("attributeName", name);
    }


	/**
	 * Returns the attribute value.
	 * @return the attribute value.
	 */
	public String getAttributeValue()
    {
    	return get("attributeValue");
    }


	/**
	 * Sets the attribute value.
	 * @param attributeValue the attribute value.
	 */
	public void setAttributeValue(String value)
    {
    	set("attributeValue", value);
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
		strRep.append(getAttributeName());
		strRep.append('=');
		strRep.append(getAttributeValue());
		strRep.append('}');
		
		return strRep.toString();
    }
}
