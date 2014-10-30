/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ************************************************************/

package com.prelert.api.data;

import org.apache.log4j.Logger;
import org.odata4j.expression.EntitySimpleProperty;

/**
 * OrderBy expressions only consist of SimplePropertyExpressions 
 * so only visit the simple property. Anything else throws an
 * UnsupportedOperationException. 
 */
public class OrderByExpressionVisitor extends org.odata4j.expression.FilterExpressionVisitor 
{
	static final private Logger s_Logger = Logger.getLogger(OrderByExpressionVisitor.class);

	private String m_PropertyName;
	
	@Override
	public void visit(EntitySimpleProperty expr)
	{
		s_Logger.debug("simpleprop=" + expr.getPropertyName());

		m_PropertyName = expr.getPropertyName();
	}
	
	/**
	 * Get the name of the property if parsed. 
	 * 
	 * @return The property name or <code>null</code>
	 */
	public String getPropertyName()
	{
		return m_PropertyName;
	}
	
}
