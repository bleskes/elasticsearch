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

package com.prelert.client.incident;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.widget.Label;

import com.prelert.data.MetricTreeNode;
import com.prelert.data.gxt.ActivityTreeModel;


/**
 * Extension of the GXT <code>Label</code> for displaying the shared metric path
 * attributes across the notifications and features in an activity.
 * @author Pete Harverson
 */
public class ActivityMetricPathLabel extends Label
{
	private List<MetricTreeNode>	m_TemplateTreeNodes;

	
	/**
	 * Creates a new label component for displaying attributes in the metric path.
	 */
	public ActivityMetricPathLabel()
	{

	}
	
	
	/**
	 * Sets list of tree nodes that make up the the metric path to use as the 
	 * template for the elements in the label. 
	 * The label will display the type, source, attributes and then metric.
	 * @param pathTreeNodes list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric path. 
	 */
	public void setMetricPathTemplate(List<MetricTreeNode> pathTreeNodes)
	{
		 m_TemplateTreeNodes = pathTreeNodes;
	}
	
	
	/**
	 * Sets the tree node representing a partial metric path whose attributes
	 * will be displayed in the label.
	 * @param treeModel tree node to display in the label. The supplied tree model
	 * 	must contain parent/child relationships.
	 */
	public void setPartialPathTreeNode(ActivityTreeModel treeModel)
	{
		ActivityTreeModel node = treeModel;
		String name;
		String value;
		BaseModelData attributeModel = new BaseModelData();
		while (node.getParent() != null)
		{
			name = node.getAttributeName();
			value = node.getAttributeValue();
			if (node.isLeaf() == true)
			{
				attributeModel.set(name, name);
			}
			else
			{
				attributeModel.set(name, value);
			}
			
			node = (ActivityTreeModel)(node.getParent());
		}
		
		
		
		StringBuilder str = new StringBuilder();
		if (m_TemplateTreeNodes != null)
		{
			for (MetricTreeNode treeNode : m_TemplateTreeNodes)
			{
				name = treeNode.getName();
				str.append(treeNode.getPrefix());
				appendAttribute(name, attributeModel.get(name), str);
			}
		}
		
		setText(str.toString());
	}
	
	
	/**
	 * Sets the partial metric path whose attributes will be displayed in the label
	 * as a list of tree nodes from root node to the level of the metric path of interest.
	 * @param pathToRoot list of nodes representing the path in the tree.
	 */
	public void setPartialPathTreeNode(List<ActivityTreeModel> pathToRoot)
	{
		String name;
		String value;
		BaseModelData attributeModel = new BaseModelData();
		for (ActivityTreeModel node : pathToRoot)
		{
			name = node.getAttributeName();
			value = node.getAttributeValue();
			attributeModel.set(name, value);
		}
		
		StringBuilder str = new StringBuilder();
		if (m_TemplateTreeNodes != null)
		{
			for (MetricTreeNode treeNode : m_TemplateTreeNodes)
			{
				name = treeNode.getName();
				str.append(treeNode.getPrefix());
				appendAttribute(name, attributeModel.get(name), str);
			}
		}
		
		setText(str.toString());
	}
	
	
	/**
	 * Appends an attribute to the label.
	 * @param name name of the attribute.
	 * @param value value of the attribute. If <code>null</code> the attribute name
	 * 	will show up as disabled in the label.
	 * @param builder <code>StringBuilder</code> being used to build up the text 
	 * 	for the label.
	 */
	protected void appendAttribute(String name, Object value, StringBuilder builder)
	{
		if (value != null)
		{
			builder.append(value);
		}
		else
		{
			builder.append("<span class=\"prl-metricPath-disabled\">");
			builder.append(name);
			builder.append("</span>");
		}
	}
	
}
