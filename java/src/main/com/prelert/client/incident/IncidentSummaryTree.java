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

import static com.prelert.data.PropertyNames.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.store.TreeStoreEvent;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartSymbolCellRenderer;
import com.prelert.client.list.CausalityEvidenceDialog;
import com.prelert.data.Attribute;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Extension of the GXT <code>TreeGrid</code> component for displaying the summary
 * of an incident as a tree of attributes in the metric path, showing the distribution 
 * of attribute  values across the time series features and notifications within that incident.
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Store.BeforeDataChanged</b> : StoreEvent(treeStore)<br>
 * <div>Fires just before the data cache in the tree grid is changed.</div>
 * <ul>
 * <li>treeStore : the summary grid's tree store</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Store.DataChanged</b> : StoreEvent(treeStore)<br>
 * <div>Fires when the data cache in the tree grid has changed, and a widget which 
 * is using the store as a ModelData cache should refresh its view.</div>
 * <ul>
 * <li>treeStore : the summary grid's tree store</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class IncidentSummaryTree extends TreeGrid<ActivityTreeModel>
{
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private IncidentModel				m_Incident;
	private List<String>				m_TreeAttributeNames;
	private int 						m_MaxLeafRows = -1;
	
	private boolean						m_AddLinkToAnalysis;
	
	private static final String			LINK_TO_ANALYSIS_TEXT =  
		"<span style=\"vertical-align: -3px; margin-right:5px;\">" + 
		AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_explore()).getHTML() +
		"</span><span class=\"prl-textLink prl-analysis-link\">" + 
		ClientUtil.CLIENT_CONSTANTS.detailedAnalysis() +
		"</span>";
	
	
	/**
	 * Creates a new incident summary <code>TreeGrid</code> component.
	 * @param inExplorer <code>true</code> if the grid is to be used in the Causality
	 * 	Explorer. If <code>false</code>, symbols are displayed for leaf nodes, using a 
	 * 	ChartSymbolCellRenderer to indicate the colour and shape of symbols used for 
	 * 	plotting causality data for each row in a chart.
	 * @see ChartSymbolCellRenderer
	 */
	public IncidentSummaryTree(boolean inExplorer)
	{
		super(new TreeStore<ActivityTreeModel>(), 
				new ColumnModel(new ArrayList<ColumnConfig>()));

		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		m_AddLinkToAnalysis = !inExplorer;
		
	    ColumnConfig value = new ColumnConfig("value", "value", 100);   
	    value.setRenderer(new ValueCellRenderer(!inExplorer));

	    ColumnConfig count = new ColumnConfig("count", "count", 100);   
	    ColumnModel cm = new ColumnModel(Arrays.asList(value, count));   
	    this.reconfigure(treeStore, cm);
  
	    getStyle().setLeafIcon(null);	// Chart symbols for leaf nodes rendered in text.
	    setAutoExpandColumn("value");   
	    setTrackMouseOver(false); 
	    
	    // This reference to QuickTip is needed to enable tooltips.
	    @SuppressWarnings("unused")
	    QuickTip qtip = new QuickTip(this);
	    
	    // Double-click / click on link to open the Causality Evidence dialog.
		addListener(Events.CellClick, new Listener<GridEvent<ActivityTreeModel>>(){
	    	public void handleEvent(GridEvent<ActivityTreeModel> ge) 
	    	{
	    		if(ge.getTarget(".prl-label-link", 1) != null)
	    		{
	    			showEvidenceDialog(ge.getModel());
	    		}
	    	}
		});
		
		addListener(Events.RowDoubleClick, new Listener<GridEvent<ActivityTreeModel>>(){

			@Override
            public void handleEvent(GridEvent<ActivityTreeModel> ge)
            {
				showEvidenceDialog(ge.getModel());
            }
	    	
	    });
	}
	
	
	/**
	 * Sets the maximum number of leaf rows for the tree grid. Once set, if the  
	 * number of leaves exceeds this limit, any remaining results are aggregated  
	 * into an 'Others' row.
	 * @param maxRows maximum number of rows to show in the grid, or <code>-1</code>
	 * 	(default) to place no limit on the number of leaf rows that can be added.
	 */
	public void setMaxLeafRows(int maxRows)
	{
		m_MaxLeafRows = maxRows;
	}
	
	
	/**
	 * Sets the incident whose causality data is to be summarised in the tree.
	 * <p>The following properties of the incident model are used:
     * <ul>
     * <li>evidenceId - to load the causality data</li>
     * <li>time - for the heading of the popup evidence dialog</li>
     * </ul>
	 * @param incident incident for which to display a summary.
	 */
	public void setIncident(IncidentModel incident)
	{
		m_Incident = incident;
	}
	
	
	/**
	 * Sets the names of attributes to be analysed in the tree.
	 * @param attributeNames <code>List</code> of attribute names.
	 */
	public void setTreeAttributeNames(List<String> treeAttributeNames)
	{
		m_TreeAttributeNames = treeAttributeNames;
	}
	
	
	/**
	 * Loads a summary tree of the causality data for an incident, analyzed by 
	 * the specified attribute.
	 * @param analyzeBy the name of the attribute by which causality data should be 
	 * 		analysed, or {@link com.prelert.client.ClientMessages#analyseOptionMostCommon()} 
	 * 		to use the default analysis based on count, or
	 * 		{@link com.prelert.client.ClientMessages#analyseOptionPathOrder()} to analyze the
	 * 		attributes in metric path order.
	 */
	public void load(String analyzeBy)
	{
		// TODO - use a custom TreeLoader tied to ActivitySummaryWidget?
		
		mask(GXT.MESSAGES.loadMask_msg());
		treeStore.fireEvent(Store.BeforeDataChanged, 
				new TreeStoreEvent<ActivityTreeModel>(treeStore));
		treeStore.removeAll();
		
		boolean metricPathOrder = analyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
		String useAnalyzeBy = null;
		if ( (metricPathOrder == false) && 
				(analyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon()) == false) )
		{
			useAnalyzeBy = analyzeBy;
		}
		
		m_IncidentQueryService.getSummaryTree(m_Incident.getEvidenceId(), m_TreeAttributeNames, 
				metricPathOrder, useAnalyzeBy, m_MaxLeafRows, new TreeLoadQueryCallback());
	}
	
	
	/**
	 * Returns the top items of aggregated causality data according to the field
	 * by which the grid is currently sorted.
	 * @param limit maximum number of items to return.
	 * @param evidenceId evidence ID of a notification or time series feature
	 * 	which should be included in the returned list if available, or 0 to not
	 * 	include any specific item.
	 * @return the top items of aggregated causality data from the summary tree.
	 */
	public List<CausalityDataModel> getTopCausalityData(int limit, int evidenceId)
	{
		// TODO - include headline evidence ID.
		
		List<CausalityDataModel> topItems = new ArrayList<CausalityDataModel>();
		List<ActivityTreeModel> allItems = treeStore.getAllItems();
		
		CausalityDataModel causalityData;
		for (ActivityTreeModel model : allItems)
		{
			if (model.isLeaf())
			{
				causalityData = model.getTopCausalityData();
				if (causalityData != null)
				{
					topItems.add(causalityData);
					
					if (topItems.size() == limit)
					{
						break;
					}
				}
				
			}
		}
		
		return topItems;
	}
	
	
	/**
	 * Returns the first leaf node in the summary tree.
	 * @return the <code>TreeModel</code> for the first leaf node.
	 */
	public ActivityTreeModel getFirstLeaf()
	{
		ActivityTreeModel leaf = null;
		
		List<ActivityTreeModel> allItems = treeStore.getAllItems();
		for (ActivityTreeModel model : allItems)
		{
			if (model.isLeaf())
			{
				leaf = model;
				break;
			}
		}
		
		return leaf;
	}
	
	
	/**
	 * Opens the dialog which pages through the notifications and features which 
	 * are represented under the specified node in the tree.
	 * @param treeModel node in the tree for which to show the corresponding evidence.
	 */
	protected void showEvidenceDialog(ActivityTreeModel treeModel)
	{	
		String name = treeModel.getAttributeName();
		String value = treeModel.getAttributeValue();
		
		// Do not open for 'Others' row or when the value of the attribute is null,
		// as back end procs do not support 'IN' or 'is null' type queries.
		if (value != null)
		{
			ActivityTreeModel node = treeModel;
			String type = null;
			
			ArrayList<Attribute> filterAttributes = new ArrayList<Attribute>();
			while (node.getParent() != null)
			{
				name = node.getAttributeName();
				value = node.getAttributeValue();

				if (name.equals(TYPE))
				{
					type = value;
				}
				
				filterAttributes.add(new Attribute(name, value));
				
				node = (ActivityTreeModel)(node.getParent());
			}
			
			String heading = "";
			String attrName = treeModel.getAttributeName();
			if (attrName == null)
			{
				Date incidentTime = m_Incident.getTime();
	        	String formattedTime = ClientUtil.formatTimeField(incidentTime, TimeFrame.SECOND);
	        	heading = ClientUtil.CLIENT_CONSTANTS.notificationDataForActivityHeading(
	        			formattedTime);
			}
			else
			{
				heading = ClientUtil.CLIENT_CONSTANTS.notificationDataHeading(treeModel.getAttributeValue());
			}
			
			CausalityEvidenceDialog evidenceDialog = CausalityEvidenceDialog.getInstance();
			evidenceDialog.setHeading(heading);
			evidenceDialog.setDataSourceName(type);
			evidenceDialog.setEvidenceId(m_Incident.getEvidenceId());
			evidenceDialog.setSingleDescription(false);
			evidenceDialog.setFilter(filterAttributes);
			
			evidenceDialog.load();
			evidenceDialog.show();
		}
	}
	
	
    @Override
    protected void onDoubleClick(GridEvent<ActivityTreeModel> e)
    {
    	// Override to stop expand/collapse node action.
		if (e.getRowIndex() != -1)
		{
			fireEvent(Events.RowDoubleClick, e);
			if (e.getColIndex() != -1)
			{
				fireEvent(Events.CellDoubleClick, e);
			}
		}
    }


	/**
 	 * Response handler for load queries. 
     */
    class TreeLoadQueryCallback extends ApplicationResponseHandler<ActivityTreeModel>
    {
    	@Override
        public void uponSuccess(ActivityTreeModel rootNode)
        {
    		treeStore.add(rootNode, true);
			treeStore.fireEvent(Store.DataChanged, new TreeStoreEvent<ActivityTreeModel>(treeStore));
			
			// Add a node at the bottom to act as a link to the Analysis View.
			if (m_AddLinkToAnalysis == true)
			{
				treeStore.add(new ActivityTreeModel(), false);
			}
			
			expandAll();
			unmask();
        }

		@Override
        public void uponFailure(Throwable caught)
        {
			GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
					ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
			unmask();
        }
    	
    }
	
	
	/**
	 * Custom GXT TreeGridCellRenderer to render a tooltip for the value column
	 * and symbols for leaf nodes.
	 */
	protected class ValueCellRenderer extends TreeGridCellRenderer<ActivityTreeModel>
	{
		private boolean m_ShowSymbol;
		
		protected ValueCellRenderer(boolean showSymbol)
		{
			m_ShowSymbol = showSymbol;
		}
		
		
        @Override
        protected String getText(TreeGrid<ActivityTreeModel> grid, ActivityTreeModel model,
                String property, int rowIndex, int colIndex)
        {
        	String attributeName = model.getAttributeName();
        	if (model.isLeaf() && attributeName == null)
        	{
        		// At the bottom a link to the Analysis View is rendered.
        		return LINK_TO_ANALYSIS_TEXT;
        	}
        	
        	// Override to display the chart symbol 
        	// and a tooltip showing attribute name and value.
        	StringBuilder sb = new StringBuilder();
        	if ( (m_ShowSymbol == true) && (model.isLeaf() == true) )
        	{
        		sb.append(getSymbolHtml(model)).append("&nbsp;&nbsp;");
        	}
        	
        	String displayValue = model.getDisplayValue();
            sb.append("<span qtip=\"");
            sb.append(displayValue);
            sb.append("\"");
            if ( (attributeName != null) && (attributeName.length() > 0) )
            {
            	sb.append(" qtitle=\"");
            	sb.append(attributeName);
            	sb.append("\"");
            }
            
            if (model.getAttributeValue() != null)
            {
            	sb.append(" class=\"prl-textLink prl-label-link\"");
            }
            
            sb.append(">");
            sb.append(displayValue);
			sb.append("</span>");
			
			return sb.toString();
        }
        
        
        /**
         * Returns the HTML used to render a symbol to act as a key for the 'top'
         * item of causality data for the specified tree model.
         * @param model <code>ActivityTreeModel</code> whose top item of causality
         * 	data is rendered in a chart.
         * @return HTML to render the key symbol.
         */
        protected String getSymbolHtml(ActivityTreeModel model)
        {
        	CausalityDataModel causalityData = model.getTopCausalityData();
        	
        	String symbolHtml = "";
	
    		if (causalityData != null)
    		{
    			// Render the chart symbol for this row.
    			String symbolColor = causalityData.get(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY);
    			Shape symbolShape = causalityData.get(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY);
    			symbolHtml = CSSSymbolChart.getInstance().getImageTag(symbolShape, symbolColor);
    		}
    		else
    		{
    			// Render transparent symbol for 'Others' row.
    			// Use the symbol shape for the first leaf, to correctly align text.
    			ActivityTreeModel firstLeaf = (ActivityTreeModel)(model.getParent().getChild(0));
    			causalityData = firstLeaf.getTopCausalityData();
    			if (causalityData != null)
    			{
    				Shape symbolShape = causalityData.get(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY);
    				symbolHtml = CSSSymbolChart.getInstance().getTransparentImageTag(symbolShape);
    			}
    			else
    			{
    				symbolHtml = CSSSymbolChart.getInstance().getTransparentImageTag(Shape.DIAMOND);
    			}	
    		}

    		return symbolHtml;

        }
    	
    }
}
