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

package com.prelert.splash;

import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.ModelStringProvider;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.store.TreeStoreEvent;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.selection.AbstractStoreSelectionModel;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanelView;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel.Joint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

import com.prelert.client.ClientUtil;
import com.prelert.data.DataSourceType;
import com.prelert.data.gxt.DataSourceTreeModel;
import com.prelert.service.DataSourceQueryServiceAsync;
import com.prelert.service.AsyncServiceLocator;


/**
 * Component containing a tree which displays the sources of data analysed by
 * the Prelert engine.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>SelectionChange</b> : SelectionEvent(source, selection)<br>
 * <div>Fires after the selection changes in the tree.</div>
 * <ul>
 * <li>source : this</li>
 * <li>selection : the selected DataSourceModel object</li>
 * </ul>
 * </dd>
 * @author Pete Harverson
 */
public class DataSourceTree extends VerticalPanel 
{
	
	private DataSourceQueryServiceAsync 	m_DataSourceQueryService;
	
	private DataSourceTreePanel 			m_DataSourcesTree;
	
	
	/**
	 * Creates a new tree for displaying the sources of data analysed by the 
	 * Prelert engine.
	 */
	public DataSourceTree()
	{	
		super();

		m_DataSourceQueryService = AsyncServiceLocator.getInstance().getDataSourceQueryService();

		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components of the tree.
	 */
	protected void initComponents()
	{
		// Create the tree store.
		final TreeStore<DataSourceTreeModel> dataSourcesTreeStore = createTreeStore();
		
		// Set a key provider, which reportedly improves performance.
		dataSourcesTreeStore.setKeyProvider(new ModelKeyProvider<DataSourceTreeModel>(){

			@Override
            public String getKey(DataSourceTreeModel model)
            {
				if (model.isSourceType())
				{
					return model.getDataSourceName();
				}
				else
				{
					return model.getDataSourceName() + "_" + model.getSource();
				}
            }
			
		});
		
		
		// Create a TreePanel to display the source name.
		m_DataSourcesTree = new DataSourceTreePanel(dataSourcesTreeStore);  
		m_DataSourcesTree.setBorders(false);
		m_DataSourcesTree.setStyleAttribute("outline", "none");	// Removes dotted focus outline on FF.
		m_DataSourcesTree.setView(new DataSourceTreePanelView());
		m_DataSourcesTree.setLabelProvider(new ModelStringProvider<DataSourceTreeModel>(){

			@Override
            public String getStringValue(DataSourceTreeModel model,
                    String property)
            {
				if (model.getDataSourceCategory() != null)
				{
					return model.getText();
				}
				else
				{
					return ClientUtil.CLIENT_CONSTANTS.analysedData();
				}
	            
            }
			
		});
		
		m_DataSourcesTree.getStyle().setNodeCloseIcon(null);
		m_DataSourcesTree.getStyle().setNodeOpenIcon(null);
		
	    
	    // Expand the first 'Analysed Data' node when the data is first loaded.
	    dataSourcesTreeStore.addListener(Store.DataChanged, new Listener<TreeStoreEvent<DataSourceTreeModel>>(){

            public void handleEvent(TreeStoreEvent<DataSourceTreeModel> be)
            { 
            	if (m_DataSourcesTree.getStore().getChildCount() > 0)
            	{
            		m_DataSourcesTree.getStore().removeListener(Store.DataChanged, this);
            		m_DataSourcesTree.setExpanded(dataSourcesTreeStore.getChild(0), true);
            	}
            }
			
		});
		
		add(m_DataSourcesTree);
		
	}

	
	/**
	 * Creates the TreeStore and its loader which retrieves the tree of data sources
	 * via GWT RPC.
	 * @return the TreeStore.
	 */
	protected TreeStore<DataSourceTreeModel> createTreeStore()
	{
		// Create the DataProxy and tree loader that retrieves data using GWT RPC.
		RpcProxy<List<DataSourceTreeModel>> proxy = new RpcProxy<List<DataSourceTreeModel>>()
		{
			@Override
			protected void load(Object loadConfig,
			        AsyncCallback<List<DataSourceTreeModel>> callback)
			{
				m_DataSourceQueryService.getDataSourceTreeModels(
				        (DataSourceTreeModel) loadConfig, false, callback);
			}
		};	

		DataSourceTreeLoader loader = new DataSourceTreeLoader(proxy);
		
		// Create the Tree Store itself.
		return new TreeStore<DataSourceTreeModel>(loader);	
	}
	
	
	/**
	 * Returns the data source tree's selection model.
	 * @return the selection model.
	 */
	public AbstractStoreSelectionModel<DataSourceTreeModel> getSelectionModel()
	{
		return m_DataSourcesTree.getSelectionModel();
	}
	
	
	/**
	 * Return the data source tree's TreeStore.
	 * @return the TreeStore used by the tree.
	 */
	public TreeStore<DataSourceTreeModel> getTreeStore()
	{
		return m_DataSourcesTree.getStore();
	}
	
	
	/**
	 * Selects the model for the specified data source type and source name
	 * and scrolls the tree node into view.
	 * @param dsType the data source type to select e.g. system_udp, p2pslogs.
	 * @param source the name of the source (server) to select, or <code>null</code>
	 * 		to select the data source type node itself.
	 */
	public void selectModel(DataSourceType dsType, String source)
	{	
		DataSourceTreeModel modelToSelect = null;

		DataSourceTreeModel dsTypeModel = findModel(dsType, null);
		
		if (dsTypeModel == null)
		{
			// The type node is not loaded!!
			// TO DO: load node and carry on.
			return;
		}
		
		
		boolean isDsExpanded = m_DataSourcesTree.isExpanded(dsTypeModel);
		
		if (source != null)
		{
			// Possibilities:
			// 1. Source node is in store, data source type node expanded.
			// 2. Source node is in store, data source type node not expanded.
			// 3. Source node not in store (data source type node not expanded).
			// 4. Data source type node is expanded, source does not exist for this data source type.
			
			modelToSelect = findModel(dsType, source);
			if ( (isDsExpanded == false) && (modelToSelect == null) )
			{
				// i.e. Option 3 - source node not in store.
				
				// Expand the data source type node, and then select the source node
				// once the TreeStore is loaded.
				final DataSourceType finalDsType = dsType;
				final String finalSource = source;
				
				m_DataSourcesTree.getStore().addStoreListener(new StoreListener<DataSourceTreeModel>(){

                    @Override
                    public void storeDataChanged(StoreEvent<DataSourceTreeModel> se)
                    {
                    	m_DataSourcesTree.getStore().removeStoreListener(this);
                    	selectModel(finalDsType, finalSource);
                    }	
				});
				
				m_DataSourcesTree.setExpanded(dsTypeModel, true);
				return;
				
			}
			else
			{
				// i.e. Options 1 and 2 - source node is in store.
				m_DataSourcesTree.setExpanded(dsTypeModel, true);
			}
			
		}
		else
		{
			modelToSelect = dsTypeModel;
		}
		
		
		if (modelToSelect != null)
		{	
			getSelectionModel().select(modelToSelect, false);
			m_DataSourcesTree.scrollIntoView(modelToSelect);
		}
	}
	
	
	/**
	 * Sets the width of the data sources tree component.
	 * @param width the new width to set.
	 */
	public void setTreeWidth(int width)
	{
		m_DataSourcesTree.setWidth(width);
	}
	
	
	/**
	 * Expands the node for the specified data source type.
	 * @param dsType the data source type to expand e.g. system_udp, p2pslogs.
	 */
	public void expandDataSource(DataSourceType dsType)
	{
		DataSourceTreeModel dsTypeModel = findModel(dsType, null);
		if (dsTypeModel != null)
		{
			m_DataSourcesTree.setExpanded(dsTypeModel, true);
		}
	}
	
	
	/**
	 * Finds the model in the tree for the specified data source type and source name.
	 * @param dsType the data source type to match e.g. system_udp, p2pslogs.
	 * @param source the name of the source (server) to match, or <code>null</code>
	 * 		if searching for the data source type node itself.
	 * @return the matching DataSourceTreeModel, or <code>null</code> if there
	 * 		is no matching node in the current TreeStore.
	 */
	public DataSourceTreeModel findModel(DataSourceType dsType, String source)
	{
		DataSourceTreeModel matching = null;
		
		if (dsType != null)
		{
			List<DataSourceTreeModel> models = 
				m_DataSourcesTree.getStore().findModels("dataSourceName", dsType.getName());
	
			String modelSource;
			
			for (DataSourceTreeModel model : models)
			{
				modelSource = model.getSource();
	
				if  ( ( (source != null) && (source.equals(modelSource)) )   ||
						 ( (source == null) && (modelSource == null) ) )
				{
						matching = model;
						break;
				}
			}
		}
		else
		{
			// Return the 'Analysed Data' root node.
			matching = m_DataSourcesTree.getStore().getChild(0);
		}
	
		return matching;
	}
	
	
	/**
	 * Custom TreePanelView which styles the nodes according to whether they 
	 * represent a data source type or a source (server).
	 */
	class DataSourceTreePanelView extends TreePanelView<DataSourceTreeModel>
	{

		/**
		 * Returns the HTML markup used to render an item in the tree.
         */
        @Override
        public String getTemplate(ModelData m, String id, String text,
		        AbstractImagePrototype icon, boolean checkable,
		        boolean checked, Joint joint, int level,
		        TreeViewRenderMode renderMode)
		{	
			if (renderMode == TreeViewRenderMode.CONTAINER)
			{
				return "<div class=\"x-tree3-node-ct\" role=\"group\"></div>";
			}
			
			StringBuilder sb = new StringBuilder();
			if (renderMode == TreeViewRenderMode.ALL || renderMode == TreeViewRenderMode.MAIN)
			{
				sb.append("<div id=\"");
				sb.append(id);
				sb.append("\"");

				sb.append(" class=\"prl-dataSourceTree-node x-tree3-node\">");
				sb.append("<div class=\"x-tree3-el\" id=\"" + tree.getId() + "__" + 
						id + "\" role=\"treeitem\" ");
				sb.append(" aria-level=\"" + (level + 1) + "\">");
			}
			if (renderMode == TreeViewRenderMode.ALL || renderMode == TreeViewRenderMode.BODY)
			{
				Element jointElement;
				switch (joint)
				{
					case COLLAPSED:
						jointElement = (Element) tree.getStyle().getJointCollapsedIcon().createElement().cast();
						break;
					case EXPANDED:
						jointElement = (Element) tree.getStyle().getJointExpandedIcon().createElement().cast();
						break;
					default:
						Image jointImage = new Image(GXT.BLANK_IMAGE_URL);
						jointImage.setWidth("16px");
						jointElement = jointImage.getElement();
				}

				El.fly(jointElement).addStyleName("x-tree3-node-joint");

				sb.append("<img src=\"");
				sb.append(GXT.BLANK_IMAGE_URL);
				sb.append("\" style=\"height: 18px; width: ");
				sb.append(level * 18);
				sb.append("px;\" />");
				sb.append(DOM.toString(jointElement));
				if (checkable)
				{
					Element e = (Element) (checked ? GXT.IMAGES.checked().createElement().cast() : 
						GXT.IMAGES.unchecked().createElement().cast());
					El.fly(e).addStyleName("x-tree3-node-check");
					sb.append(DOM.toString(e));
				}
				else
				{
					sb.append("<span class=\"x-tree3-node-check\"></span>");
				}
				if (icon != null)
				{
					Element e = icon.createElement().cast();
					El.fly(e).addStyleName("x-tree3-node-icon");
					sb.append(DOM.toString(e));
				}
				else
				{
					sb.append("<span class=\"x-tree3-node-icon\"></span>");
				}
				
				DataSourceTreeModel treeModel = (DataSourceTreeModel)m;
				if (treeModel.getDataSourceCategory() != null)
				{
					if (treeModel.isSourceType() == true)
					{
						sb.append("<span class=\"prl-dataSourceTree-sourceType-text x-tree3-node-text\">");
					}
					else
					{
						sb.append("<span class=\"prl-dataSourceTree-source-text x-tree3-node-text\">");
					}  
				}
				else
				{
					sb.append("<span class=\"prl-dataSourceTree-root-text x-tree3-node-text\">");
				//	sb.append(ClientUtil.CLIENT_CONSTANTS.analysedData());
				}
				
				sb.append(text);
				sb.append("</span>");
				
			}

			if (renderMode == TreeViewRenderMode.ALL || renderMode == TreeViewRenderMode.MAIN)
			{
				sb.append("</div>");
				sb.append("</div>");
			}
			return sb.toString();
		}
		
	}
	
	
	/**
	 * Extension of the standard TreePanel to work around a bug in scrolling into
	 * view branch nodes whose children off the height of the screen.
	 */
	class DataSourceTreePanel extends TreePanel<DataSourceTreeModel>
	{

		public DataSourceTreePanel(TreeStore<DataSourceTreeModel> store)
        {
	        super(store);
        }


		/**
		 * Overrides TreePanel.scrollIntoView() to improve scrolling for source
		 * type branch nodes.
		 */
        @Override
        public void scrollIntoView(DataSourceTreeModel model)
        {
        	TreeNode node = findNode(model);
        	
        	if (node != null) 
    	    {
	        	if (model.isSourceType() == true)
	        	{	// We need to get at the child <div> which displays the source type node
	        		// text and icon. If the branch node itself was used, then the bottom of 
	        		// the branch is scrolled into view.
        			com.google.gwt.dom.client.Element sourceNodeElem = 
        				node.getElement().getFirstChildElement();
        			
        			if (sourceNodeElem != null)
        			{
        				sourceNodeElem.scrollIntoView();
        			}
        			else
        			{
        				node.getElement().scrollIntoView();
        			}
	        	}
	        	else
	        	{
	        		node.getElement().scrollIntoView();
	        	}
    	    }
        }
		
	}
}
