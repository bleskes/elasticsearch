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

package demo.app.splash.gxt;

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

import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.gxt.DataSourceTreeModel;
import demo.app.splash.service.DataSourceQueryServiceAsync;
import demo.app.splash.service.QueryServiceLocator;


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
	
	//private TreeGrid<DataSourceTreeModel> 	m_DataSourcesTree;
	private TreePanel<DataSourceTreeModel> 	m_DataSourcesTree;
	//private DataSourceTreeGridView			m_TreeGridView;
	
	
	/**
	 * Creates a new tree for displaying the sources of data analysed by the 
	 * Prelert engine.
	 */
	public DataSourceTree()
	{	
		super();

		m_DataSourceQueryService = QueryServiceLocator.getInstance().getDataSourceQueryService();
		
		//initComponents();
		initTreePanelComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components of the tree.
	 */
	/*
	protected void initComponents()
	{
		// Create the tree store.
	//	final TreeStore<DataSourceTreeModel> dataSourcesTreeStore = createTreeStore();
		final TreeStore<DataSourceTreeModel> dataSourcesTreeStore = getTestDataSourcesTree();
		
		dataSourcesTreeStore.setKeyProvider(new ModelKeyProvider<DataSourceTreeModel>(){

			@Override
            public String getKey(DataSourceTreeModel model)
            {
	            return model.getDataSourceName() + "_" + model.getSource();
            }
			
		});
		
		
		// Create a TreeGrid to display source and total number of data points.
		
		// Create the source name column and add a WidgetTreeGridCellRenderer
		// so that branch nodes pick up a different style to leaf nodes.
	    ColumnConfig source = new ColumnConfig("text", "text", 190);   
	    
	    source.setRenderer(new WidgetTreeGridCellRenderer<DataSourceTreeModel>()
		{
			@Override
			public Widget getWidget(DataSourceTreeModel model, String property,
			        ColumnData config, int rowIndex, int colIndex,
			        ListStore<DataSourceTreeModel> store, Grid<DataSourceTreeModel> grid)
			{
				String text = String.valueOf(model.get(property));
				Label nodeLabel = new Label(text);
				if (model.isSourceType() == false)
				{	
					nodeLabel.addStyleName("prl-dataSourceTree-source-text");
				}
				else
				{
					nodeLabel.addStyleName("prl-dataSourceTree-sourceType-text");
				}

				return nodeLabel;
			}

		});

	    ColumnConfig count = new ColumnConfig("count", "Total", 60);  
	    count.setRenderer(new GridCellRenderer<BaseTreeModel>()
		{
            public Object render(BaseTreeModel model, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<BaseTreeModel> store,
                    Grid<BaseTreeModel> grid)
            {
                String text = String.valueOf(model.get(property));
                
                StringBuffer sb = new StringBuffer("<span unselectable=\"on\" class=\"prl-dataSourceTree-text \">");
                sb.append(text);
                sb.append("</span>");
                
                return sb.toString();
            }
	
		});
	    ColumnModel columnModel = new ColumnModel(Arrays.asList(source, count));   
		
	    m_DataSourcesTree = 
	    	new TreeGrid<DataSourceTreeModel>(dataSourcesTreeStore, columnModel);  
	    m_DataSourcesTree.setBorders(false); 
	    m_DataSourcesTree.setHideHeaders(true);
	//    m_DataSourcesTree.setAutoExpandColumn("text");   
	    m_DataSourcesTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);  
	    
	//    m_DataSourcesTree.getView().setAutoFill(true);
	//    m_DataSourcesTree.setAutoHeight(true);
	    m_DataSourcesTree.getStyle().setNodeCloseIcon(null);
	    m_DataSourcesTree.getStyle().setNodeOpenIcon(null);
	    
	    m_DataSourcesTree.addListener(Events.BeforeExpand, new Listener<TreeGridEvent>(){

			@Override
            public void handleEvent(TreeGridEvent be)
            {
	            GWT.log("DataSourcesTree.BeforeExpand " + new Date(),  null);
            }
	    	
	    });
	    
	    m_DataSourcesTree.addListener(Events.Expand, new Listener<TreeGridEvent>(){

			@Override
            public void handleEvent(TreeGridEvent be)
            {
	            GWT.log("DataSourcesTree.Expand" + new Date() + ", total count: " + 
	            		dataSourcesTreeStore.getAllItems().size(),  null);
            }
	    	
	    });
		
	    m_TreeGridView = new DataSourceTreeGridView();
	    m_DataSourcesTree.setView(m_TreeGridView);
	    m_TreeGridView.setBufferEnabled(false);	// Workaround for known problem with blank rows.
	    
	    // Select the first node when the data is first loaded.
	    dataSourcesTreeStore.addListener(Store.DataChanged, new Listener<TreeStoreEvent<DataSourceTreeModel>>(){

            public void handleEvent(TreeStoreEvent<DataSourceTreeModel> be)
            { 
            	if (m_DataSourcesTree.getTreeStore().getChildCount() > 0)
            	{
            		m_DataSourcesTree.getSelectionModel().select(0, false);
            		m_DataSourcesTree.getTreeStore().removeListener(Store.DataChanged, this);
            	}
            }
			
		});
	    
		
		add(m_DataSourcesTree);
		
	}
	*/
	
	/**
	 * Creates and initialises the graphical components of the tree.
	 */
	protected void initTreePanelComponents()
	{
		// Create the tree store.
		final TreeStore<DataSourceTreeModel> dataSourcesTreeStore = createTreeStore();
	//	final TreeStore<DataSourceTreeModel> dataSourcesTreeStore = getTestDataSourcesTree();
		
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
		m_DataSourcesTree = new TreePanel<DataSourceTreeModel>(dataSourcesTreeStore);  
		m_DataSourcesTree.setDisplayProperty("text");   
		m_DataSourcesTree.setBorders(false);
		m_DataSourcesTree.setView(new DataSourceTreePanelView());
		m_DataSourcesTree.setLabelProvider(new ModelStringProvider<DataSourceTreeModel>(){

			@Override
            public String getStringValue(DataSourceTreeModel model,
                    String property)
            {
				String val = model.getText();
	            if (model.isSourceType() == true)
	            {
	            	val += " (<small><i>";
	            	val += model.getCount();
	            	val += "</i></small>)";
	            }
	            
	            return val;
            }
			
		});
		
		m_DataSourcesTree.getStyle().setNodeCloseIcon(null);
		m_DataSourcesTree.getStyle().setNodeOpenIcon(null);
		
	    
	    // Select the first node when the data is first loaded.
	    dataSourcesTreeStore.addListener(Store.DataChanged, new Listener<TreeStoreEvent<DataSourceTreeModel>>(){

            public void handleEvent(TreeStoreEvent<DataSourceTreeModel> be)
            { 
            	
            	if (m_DataSourcesTree.getStore().getChildCount() > 0)
            	{
            		m_DataSourcesTree.getSelectionModel().select(dataSourcesTreeStore.getChild(0), false);
            		m_DataSourcesTree.getStore().removeListener(Store.DataChanged, this);
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
				m_DataSourceQueryService.getDataSources(
				        (DataSourceTreeModel) loadConfig, callback);
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
		//return m_DataSourcesTree.getTreeStore();
		return m_DataSourcesTree.getStore();
	}
	
	
	/**
	 * Selects the model in the tree with the specified data source type and source name.
	 * @param dsType the data source type of the model to select 
	 * 		e.g. system_udp, p2pslogs.
	 * @param source the name of the source (server) to select, or <code>null</code>
	 * 		if selecting the data source type node itself.
	 */
	/*
	public void selectModel(DataSourceType dsType, String source)
	{	
		DataSourceTreeModel modelToSelect = null;

		DataSourceTreeModel dsTypeModel = findModel(dsType, null);
		
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
				
				m_DataSourcesTree.getTreeStore().addStoreListener(new StoreListener<DataSourceTreeModel>(){

                    @Override
                    public void storeDataChanged(
                            StoreEvent<DataSourceTreeModel> se)
                    {
                    	m_DataSourcesTree.getTreeStore().removeStoreListener(this);
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
			
			Element row = m_TreeGridView.getRow(modelToSelect);
			int rowIndex = m_TreeGridView.findRowIndex(row);
			
			// TO DO: bug with this if row is off bottom of scroll panel as
			// does not scroll so it is fully in view.
			m_TreeGridView.focusRow(rowIndex);
		}
	}
	*/
	
	
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
	//	List<DataSourceTreeModel> models = 
	//		m_DataSourcesTree.getTreeStore().findModels("dataSourceName", dsType.getName());
		
		List<DataSourceTreeModel> models = 
			m_DataSourcesTree.getStore().findModels("dataSourceName", dsType.getName());

		String modelSource;
		DataSourceTreeModel matching = null;
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
	
		return matching;
	}
	
	
	/**
	 * Creates a tree of test data.
	 * @return
	 */
	protected TreeStore<DataSourceTreeModel> getTestDataSourcesTree()
	{
		// Just hardcode a simple data sources tree.
		
		
		// mdh logs data.
		DataSourceTreeModel mdhlogs = new DataSourceTreeModel();	
		mdhlogs.setDataSourceType(new DataSourceType("mdhlog", DataSourceCategory.NOTIFICATION));
		mdhlogs.setText("mdhlog");
		mdhlogs.setCount(965281);
		
		// Create 1 genuine leaf node, then 99 dummy ones.
		DataSourceTreeModel mdh8502 = new DataSourceTreeModel();
		mdh8502.setSource("lnl00m-8502");
		mdh8502.setCount(213212);
		mdh8502.setDataSourceType(new DataSourceType("mdhlog", DataSourceCategory.NOTIFICATION));
		mdh8502.setText("lnl00m-8502");
		mdhlogs.add(mdh8502);
		
		DataSourceTreeModel mdhlogModel;
		for (int i = 0; i < 99; i++)
		{
			mdhlogModel = new DataSourceTreeModel();
			mdhlogModel.setSource("lnl00m-820" + i);
			mdhlogModel.setCount(i*100);
			mdhlogModel.setDataSourceType(new DataSourceType("mdhlog", DataSourceCategory.NOTIFICATION));
			mdhlogModel.setText("lnl00m-820" + i);
			mdhlogs.add(mdhlogModel);
		}

		// p2ps logs data.
		DataSourceTreeModel p2pslogs = new DataSourceTreeModel();	
		p2pslogs.setDataSourceType(new DataSourceType("p2pslog", DataSourceCategory.NOTIFICATION));
		p2pslogs.setText("p2pslog");
		p2pslogs.setCount(965281);
		
		// Create 1 genuine leaf node, then 99 dummy ones.
		DataSourceTreeModel logs8201 = new DataSourceTreeModel();
		logs8201.setSource("lnl00m-8201");
		logs8201.setCount(213212);
		logs8201.setDataSourceType(new DataSourceType("p2pslog", DataSourceCategory.NOTIFICATION));
		logs8201.setText("lnl00m-8201");
		p2pslogs.add(logs8201);
		
		DataSourceTreeModel p2pslogModel;
		for (int i = 0; i < 99; i++)
		{
			p2pslogModel = new DataSourceTreeModel();
			p2pslogModel.setSource("lnl00m-850" + i);
			p2pslogModel.setCount(i*100);
			p2pslogModel.setDataSourceType(new DataSourceType("p2pslog", DataSourceCategory.NOTIFICATION));
			p2pslogModel.setText("lnl00m-850" + i);
			p2pslogs.add(p2pslogModel);
		}
		
		// p2psmon user usage data.
		DataSourceTreeModel userUsage = new DataSourceTreeModel();	
		userUsage.setDataSourceType(new DataSourceType("p2psmon_users", DataSourceCategory.TIME_SERIES));
		userUsage.setText("p2psmon_users");
		userUsage.setCount(513672);
		
		// Create 1 genuine leaf node, then 99 dummy ones.
		DataSourceTreeModel user8201 = new DataSourceTreeModel();
		user8201.setSource("lnl00m-8201");
		user8201.setCount(67121);
		user8201.setDataSourceType(new DataSourceType("p2psmon_users", DataSourceCategory.TIME_SERIES));
		user8201.setText("lnl00m-8201");
		userUsage.add(user8201);
		
		DataSourceTreeModel usersModel;
		for (int i = 0; i < 99; i++)
		{
			usersModel = new DataSourceTreeModel();
			usersModel.setSource("lnl00m-850" + i);
			usersModel.setCount(i*100);
			usersModel.setDataSourceType(new DataSourceType("p2psmon_users", DataSourceCategory.TIME_SERIES));
			usersModel.setText("lnl00m-850" + i);
			userUsage.add(usersModel);
		}
		
		
		TreeStore<DataSourceTreeModel> dataSourcesTreeStore = new TreeStore<DataSourceTreeModel>();
		dataSourcesTreeStore.add(mdhlogs, true);
		dataSourcesTreeStore.add(p2pslogs, true);
		dataSourcesTreeStore.add(userUsage, true);
		
		
		return dataSourcesTreeStore;
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

				sb.append(" class=\"x-tree3-node\">");
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
				if (treeModel.isSourceType() == true)
				{
					sb.append("<span class=\"prl-dataSourceTree-sourceType-text x-tree3-node-text\">");
				}
				else
				{
					sb.append("<span class=\"prl-dataSourceTree-source-text x-tree3-node-text\">");
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
}
