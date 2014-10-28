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

package demo.app.client;

import java.util.HashMap;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.layout.FlowData;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;

import demo.app.data.gxt.ClassificationTreeNode;


/**
 * Entry point for the ClassificationTree application which displays a
 * classification tree using a GXT TreePanel component.
 * <p>
 * The simple UI contains a text area for entering the tree data, a button
 * for building the tree, and a GXT TreePanel component for displaying the tree
 * which has built from the supplied data.
 * <p>
 * The data format for a tree node is:
 * <pre>
 * connector_id left_node_id right_node_id significance
 * </pre>
 * For example:
 * <pre>
 * -1 system_udp_received_20:system_udp_received_10 0.562264
 * -2 system_udp_received_17:system_udp_received_1 0.619772
 * -3 -1:-2 0.663821
 * -4 -3:system_udp_received_0 0.772526
 * -5 system_udp_received_33:system_udp_received_21 0.853333
 * -6 system_udp_received_25:-5 0.861249
 * -7 system_udp_received_43:-6 0.939989
 * -8 -7:-4 0.97214
 * </pre>
 */
public class ClassificationTree implements EntryPoint
{
	private TextArea m_TreeDataInput;
	
	private TreeStore<ClassificationTreeNode> 	m_TreeStore;
	private TreePanel<ClassificationTreeNode>	m_TreePanel;

	/**
	 * The entry point method for the ClassificationTree application.
	 */
	public void onModuleLoad()
	{
		m_TreeDataInput = new TextArea();    
		m_TreeDataInput.setWidth(400);
		m_TreeDataInput.setHeight(200);
		m_TreeDataInput.setFieldLabel("Tree data");   
	    
	    Button buildTreeBtn = new Button("Build Tree");

	    // Add a handler to close the DialogBox
	    buildTreeBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
            public void componentSelected(ButtonEvent be)
            {
            	try
            	{
            		buildTree();
            	}
            	catch (Exception e)
            	{
            	    MessageBox box = new MessageBox();
            	    box.setTitle("Error");
            	    box.setMessage("An error occurred trying to build the tree.\n" +
            				"Please check the format of the data.");
            	    box.setButtons(MessageBox.OK);
            	    box.setIcon(MessageBox.ERROR);
            	    box.show();
            	}
            }
		});
	    
	    m_TreeStore = new TreeStore<ClassificationTreeNode>();    
	  
	    m_TreePanel = new TreePanel<ClassificationTreeNode>(m_TreeStore);   
	    m_TreePanel.setDisplayProperty("label");   
	    m_TreePanel.setWidth(500); 
	    
	    
	    LayoutContainer container = new LayoutContainer();
	    Image logo = new Image("classificationtree/images/logo.png");
	    logo.setWidth("194px"); 
	    logo.setHeight("46px"); 
	    container.add(logo, new FlowData(10));

	    LabelField label = new LabelField(
	    		"Paste the classification tree data in the field below:");
	    label.addStyleName("labels");
	    container.add(label, new FlowData(10));
	    container.add(m_TreeDataInput, new FlowData(10));
	    
	    container.add(buildTreeBtn, new FlowData(10));
	    
	    container.add(m_TreePanel, new FlowData(10));
	    
		RootPanel.get().add(container);
	}
	
	
	/**
	 * Builds the tree from the text that has been entered into the input area.
	 * @return the root node of the tree.
	 * @throws Exception if the text is not in the correct format.
	 */
	public ClassificationTreeNode buildTree() throws Exception
	{
		String inputText = m_TreeDataInput.getValue();
		String[] tokens = inputText.split("\\r?\\n");
		GWT.log("Number of tokens: " + tokens.length, null);

		String[] nodeProperties;
		ClassificationTreeNode treeNode = null;
		ClassificationTreeNode childNode;
		
		String id;
		String leftId;
		String rightId;

		float significance;
		HashMap<String, ClassificationTreeNode> nodesMap = 
			new HashMap<String, ClassificationTreeNode>();
		
		for (String token : tokens)
		{
			// Format of each token is:
			// <connector id> <left node id> <right node id> <significance> 
			// -3 p2pslog_261:-2 0.635135
			nodeProperties = token.split("[: ]");
			
			try
			{
				id = nodeProperties[0];
				leftId = nodeProperties[1];
				rightId = nodeProperties[2];
				significance = Float.parseFloat(nodeProperties[3]);
				
				treeNode = nodesMap.get(id);
				if (treeNode == null)
				{
					treeNode = new ClassificationTreeNode(id);
					nodesMap.put(id, treeNode);
				}
				treeNode.setSignificance(significance);
				treeNode.setLabel("" + significance);
				
				childNode = nodesMap.get(leftId);
				if (childNode == null)
				{
					childNode = new ClassificationTreeNode(leftId);
					nodesMap.put(leftId, childNode);
				}
				treeNode.add(childNode);
				
				childNode = nodesMap.get(rightId);
				if (childNode == null)
				{
					childNode = new ClassificationTreeNode(rightId);
					nodesMap.put(rightId, childNode);
				}
				treeNode.add(childNode);	
			}
			catch (Exception e)
			{
		        GWT.log("Error parsing input to tree node", e);
		        throw e;
			}
		}
		
		GWT.log("Number of nodes created: " + nodesMap.size(), null);
		
		// Replace the old tree with the new root node, and expand all nodes.
		m_TreeStore.removeAll();
		m_TreeStore.add(treeNode, true);
		m_TreePanel.expandAll();
		
		return treeNode;
	}
}
