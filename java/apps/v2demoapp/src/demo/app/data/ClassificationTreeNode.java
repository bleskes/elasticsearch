/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package demo.app.data;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseTreeModel;

public class ClassificationTreeNode extends BaseTreeModel implements Serializable
{
	private int m_LeftId;
	private int m_RightId;
	
	public ClassificationTreeNode()
	{
		
	}
	
	
	public ClassificationTreeNode(int id, float significance)
	{
		this(id);
		setSignificance(significance);
	}
	
	
	public ClassificationTreeNode(int id)
	{
		set("id", id);
	}
	
	
	public int getId()
	{
		return ((Integer)get("id")).intValue();
	}
	
	
	public void setSignificance(float significance)
	{
		set("significance", significance);
	}
	
	
	public float getSignificance()
	{
		return ((Float)get("significance")).floatValue();
	}
	
	
	public String toString()
	{
		return ("" + getId());
	}
}
