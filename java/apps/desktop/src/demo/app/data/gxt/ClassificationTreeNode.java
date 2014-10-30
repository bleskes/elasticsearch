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

package demo.app.data.gxt;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseTreeModel;

/**
 * Extension of BaseTreeModel for nodes in the classification tree see {@link ClassificationTree}.
 * @author Pete Harverson
 */
public class ClassificationTreeNode extends BaseTreeModel implements Serializable
{
	
	/**
	 * Creates a new ClassificationTreeNode with the given id and significance.
	 * @param id ID of the node.
	 * @param significance significance, a value between 0 and 1.
	 */
	public ClassificationTreeNode(String id, float significance)
	{
		this(id);
		setSignificance(significance);
	}
	
	
	/**
	 * Creates a new ClassificationTreeNode with the given id.
	 * @param id ID of the node.
	 */
	public ClassificationTreeNode(String id)
	{
		set("id", id);
		setLabel("" + id);
	}
	
	
	/**
	 * Returns the id of the node.
	 * @return the id.
	 */
	public String getId()
	{
		return get("id");
	}
	
	
	/**
	 * Sets the significance of the tree node.
	 * @param significance significance, a value between 0 and 1.
	 */
	public void setSignificance(float significance)
	{
		set("significance", significance);
	}
	
	
	/**
	 * Returns the significance of the tree node.
	 * @return significance, a value between 0 and 1.
	 */
	public float getSignificance()
	{
		return ((Float)get("significance")).floatValue();
	}
	
	
	/**
	 * Sets the text that will be used as the label for this node in the tree.
	 * @param label the text label.
	 */
	public void setLabel(String label)
	{
		set("label", label);
	}
	
	
	/**
	 * Returns the label for this tree node.
	 * @return the text label.
	 */
	public String getLabel()
	{
		String label = get("label");
		if (label == null)
		{
			label = this.toString();
		}
		return label;
	}
	
	
	public String toString()
	{
		return ("" + getId());
	}
}
