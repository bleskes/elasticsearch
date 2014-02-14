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

package com.prelert.client.list;

import java.util.List;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.DataSourceQueryServiceAsync;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * A GXT panel for searching for evidence matching criteria to be entered by the
 * user. The panel consists of a toolbar for entering the search criteria, a 
 * toolbar with paging controls, and a grid component for listing the results
 * of the search.

 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the probable cause view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a data view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * </dl>
 * 
 * @author Pete Harverson
 */
public class EvidenceSearchPanel extends EvidenceGridPanel
{
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService;
	private DataSourceQueryServiceAsync m_DataSourceQueryService;
	
	private ComboBox<DataSourceModel> 	m_DataTypeCombo;
	
	
	/**
	 * Constructs a new panel for searching for evidence matching criteria to
	 * be entered by the user.
	 */
	public EvidenceSearchPanel()
	{	
		// Create the RpcProxy and PagingLoader to populate the results grid.
		super(new EvidencePagingLoader(new SearchEvidenceDataRpcProxy()));
		
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
		m_DataSourceQueryService = AsyncServiceLocator.getInstance().getDataSourceQueryService();
		
		setHeading(ClientUtil.CLIENT_CONSTANTS.searchAnalysedData());
		
		// Add 'Show Analysis' and 'Show Data' items to the context menu.
		final MenuItem probCauseMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showAnalysis());
	    probCauseMenuItem.addSelectionListener(new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				EvidenceModel selectedRow = getSelectedEvidence();
				if (selectedRow != null)
				{
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(EvidenceSearchPanel.this);
					rve.setModel(selectedRow);	
					fireEvent(GXTEvents.OpenCausalityViewClick, rve);
				}
            }
			
	    });
	    m_GridContextMenu.insert(probCauseMenuItem, 0);
	    
	    MenuItem showDataMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showData());    
	    showDataMenuItem.addSelectionListener(new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				EvidenceModel selectedRow = getSelectedEvidence();
				if (selectedRow != null)
				{
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(EvidenceSearchPanel.this);
					rve.setModel(selectedRow);	
					fireEvent(GXTEvents.OpenViewClick, rve);
				}
            }
			
	    });
	    m_GridContextMenu.add(showDataMenuItem);
	    
	    // When the menu is shown, enable or disable the 'Show Probable Cause' 
	    // item depending on what is selected.
	    m_GridContextMenu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
		{
			@Override
            public void handleEvent(MenuEvent be)
            {	
				boolean hasProbableCause = false;
				EvidenceModel selectedRow = getSelectedEvidence();
				if (selectedRow != null)
				{
					hasProbableCause = selectedRow.get(
							Evidence.COLUMN_NAME_PROBABLE_CAUSE, false);
				}
				probCauseMenuItem.setEnabled(hasProbableCause);
            }
	
		});
		
		
		// Create and populate the Data Types Combo box.
		createDataTypesControl();
		

		// Get the list of columns to use for the results grid.
		ApplicationResponseHandler<List<String>> callback = 
			new ApplicationResponseHandler<List<String>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading display columns for search results", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(List<String> columns)
			{
				setColumns(columns);
			}
		};
		
		
		m_EvidenceQueryService.getAllColumns(null, callback);
	}
	

	/**
	 * Creates and populates the Data Types Combo box and adds it to the toolbar.
	 */
    private void createDataTypesControl()
    {	
		// Initialise the data types store with an 'All data types' value.
		final ListStore<DataSourceModel> typesStore = new ListStore<DataSourceModel>();
		DataSourceModel allTypes = new DataSourceModel();
		allTypes.setDataSourceName(ClientUtil.CLIENT_CONSTANTS.allDataTypes());
		typesStore.add(allTypes);
		
		
		// Run the query for the to populate the types Combo.
		ApplicationResponseHandler<List<DataSourceModel>> callback = 
			new ApplicationResponseHandler<List<DataSourceModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading list of data types", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
			}


			@Override
            public void uponSuccess(List<DataSourceModel> models)
			{
				typesStore.add(models);	
			}
		};
		
		m_DataSourceQueryService.getDataSourceTypes(callback);
		
		
		// Add the 'Refine results to data type' combo box to the toolbar.
		m_DataTypeCombo = new ComboBox<DataSourceModel>();
		m_DataTypeCombo.setStore(typesStore);
		m_DataTypeCombo.setEditable(false);
		m_DataTypeCombo.setListStyle("prl-combo-list");
		m_DataTypeCombo.setDisplayField("dataSourceName");
		m_DataTypeCombo.setWidth(120);
		m_DataTypeCombo.setTriggerAction(TriggerAction.ALL); 
		m_DataTypeCombo.setValue(typesStore.getAt(0));
		m_DataTypeCombo.addSelectionChangedListener(new SelectionChangedListener<DataSourceModel>() {
		      @Override
		      public void selectionChanged(SelectionChangedEvent<DataSourceModel> se) 
		      {
		    	  DataSourceModel selectedItem = se.getSelectedItem();
		    	  
		    	  String dataType = null;
		    	  DataSourceType dsType = selectedItem.getDataSourceType();
		    	  if (dsType != null)
		    	  {
		    		  dataType = dsType.getName();
		    	  }
		    	  
		    	  m_Loader.setDataType(dataType);
		    	  m_Loader.load();
		    	  
		      }
		});
		
		m_ToolBar.add(new FillToolItem());
		m_ToolBar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.refineResults()));
		m_ToolBar.add(m_DataTypeCombo);

		
		/*
	    // Create the type Combo which displays an icon and text for each data type.
	    BaseModelData allTypes = new BaseModelData();
	    allTypes.set("type", "All data types");
	    allTypes.set("imagename", "all_types16");
	    
	    BaseModelData evidenceType = new BaseModelData();
	    evidenceType.set("type", "Notifications");
	    evidenceType.set("imagename", "evidence16");
	    
	    BaseModelData timeSeriesType = new BaseModelData();
	    timeSeriesType.set("type", "Time series");
	    timeSeriesType.set("imagename", "time_series16");
	    
	    ListStore<BaseModelData> typesStore = new ListStore<BaseModelData>();   
	    typesStore.add(allTypes);
	    typesStore.add(evidenceType);
	    typesStore.add(timeSeriesType);
	  
	    ComboBox<BaseModelData> typesCombox = new ComboBox<BaseModelData>(); 
	    typesCombox.setEditable(false);
	    typesCombox.setListStyle("prl-combo-list");
	    typesCombox.setWidth(100);   
	    typesCombox.setStore(typesStore);   
	    typesCombox.setTemplate(getComboTemplate());   
	    typesCombox.setDisplayField("type");   
	    typesCombox.setTypeAhead(true);   
	    typesCombox.setTriggerAction(TriggerAction.ALL);   
	    m_ToolBar.add(typesCombox);
	*/
		
    }


    @Override
    protected void onLoad(LoadEvent le)
    {
	    super.onLoad(le);
	    
	    String searchText = m_Loader.getContainsText();
		if (searchText == null)
		{
			searchText = "";
		}
		setHeading(ClientUtil.CLIENT_CONSTANTS.searchResultsFor(searchText));
    }


	/**
     * Sets the String to search for within one or more of the evidence 
     * attribute values.
     * @param containsText the text to search for within attribute values.
     */
	public void setContainsText(String containsText)
	{
		m_Loader.setContainsText(containsText);
	}
	
	
	/**
	 * Sets the name of the data type which will be searched for attribute values
	 * containing the search text specified by the user.
	 * @param dataTypeName the name of the data type to be searched, 
	 * or <code>null</code> to search across all data types.
	 */
	@Override
	public void setDataType(String dataTypeName)
	{
		super.setDataType(dataTypeName);
		
		// Set the Data Type Combo to the matching option.
		ListStore<DataSourceModel> typesStore = m_DataTypeCombo.getStore();
		if (typesStore.getCount() > 0)
		{
			m_DataTypeCombo.disableEvents(true);
			
			if (dataTypeName == null)
			{
				m_DataTypeCombo.setValue(typesStore.getAt(0));
			}
			else
			{
				List<DataSourceModel> selectedValues = m_DataTypeCombo.getSelection();
				String currentlySelectedVal = null;
				if (selectedValues.size() > 0)
				{
					DataSourceModel selectedModel = selectedValues.get(0);
					if (m_DataTypeCombo.getStore().indexOf(selectedModel) != 0)
			  	  	{
						currentlySelectedVal = selectedModel.getDataSourceName();
			  	  	}
				}
				
				if (dataTypeName.equals(currentlySelectedVal) == false)
				{
					DataSourceModel dataSource;
					for (int i = 0; i < typesStore.getCount(); i++)
					{
						dataSource = typesStore.getAt(i);
						if (dataSource.getDataSourceName().equals(dataTypeName))
						{
							m_DataTypeCombo.setValue(dataSource);
							break;
						}
					}
				}
			}
			
			m_DataTypeCombo.enableEvents(true);
		}
	}
	
	
	/**
	 * Returns the template to render the data type Combo Box. It displays
	 * an icon to represent the category of each data type (NOTIFICATION or TIME_SERIES)
	 * and the name of the data type.
	 * @return the HTML template.
	 */
	private native String getTypeComboTemplate() /*-{  
    return  [  
    '<tpl for=".">',  
    '<div class="prl-combo-list-item"><img width="16px" height="12px" src="images/shared/{[values.imagename]}.png"> {[values.type]}</div>',  
    '</tpl>'  
    ].join("");  
  }-*/; 
}
