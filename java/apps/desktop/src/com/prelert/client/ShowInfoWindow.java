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

package com.prelert.client;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.prelert.data.gxt.GridRowInfo;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * details of a particular record e.g. an evidence record.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>CellClick</b> : GridEvent(grid, event)<br>
 * <div>Fires after a cell displaying an active attribute is clicked.</div>
 * <ul>
 * <li>grid : this</li>
 * <li>event : the dom event</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class ShowInfoWindow extends Window
{
	private Desktop						m_Desktop;
	
	private List<String>				m_ActiveAttributes;
	private ListStore<GridRowInfo> 		m_Store;
	private Grid<GridRowInfo> 			m_Grid;
	
	
	/**
	 * Constructs a new empty ShowInfoWindow.
	 * @param desktop the parent Prelert desktop.
	 */
	public ShowInfoWindow(Desktop desktop)
	{
		m_Desktop = desktop;
		
		setCloseAction(CloseAction.CLOSE);
		setIconStyle("icon-grid");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Row Info");
		setSize(400, 500);
		setLayout(new FitLayout());
		setResizable(true);
		
		m_Store = new ListStore<GridRowInfo>();
		
	    ColumnConfig propColumn = new ColumnConfig("columnName", "Property", 150);
	    ColumnConfig valueColumn = new ColumnConfig("columnValue", "Value", 250);
	    valueColumn.setRenderer(new ShowInfoValueCellRenderer());

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(propColumn);
	    config.add(valueColumn);

	    ColumnModel columnModel = new ColumnModel(config);
		
	    m_Grid = new Grid<GridRowInfo>(m_Store, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    
	    // Listen for clicks in cells which can be linked to filtered evidence views.
		m_Grid.addListener(Events.CellClick, new Listener<GridEvent>(){
	    	public void handleEvent(GridEvent event) {
	    		if(event.getTarget(".showInfoLink", 1) != null)
	    		{
	    			// Fire an event to listeners for CellClick events from the window.
	    			ShowInfoWindow.this.fireEvent(Events.CellClick, event);	
	    		}
	    	}
	    });

	    add(m_Grid);
	}
	
	
	/**
	 * Sets the list of active attributes. Active attributes are ones from
	 * which new filtered views can be launched e.g. by the user clicking 
	 * on the attribute value.
	 * @param attributeNames list of active attribute names.
	 */
	public void setActiveAttributes(List<String> attributeNames)
	{
		m_ActiveAttributes = attributeNames;
	}
	
	
	/**
	 * Returns the list of active attributes. Active attributes are ones from
	 * which new filtered views can be launched e.g. by the user clicking 
	 * on the attribute value.
	 * @return the list of active attribute names, or <code>null</code> if the
	 * active attributes have not been set.
	 */
	public List<String> getActiveAttributes()
	{
		return m_ActiveAttributes;
	}


	/**
	 * Sets the data for display in the window.
	 * @param data list of GridRowInfo objects for display in the window's grid.
	 */
	public void setModelData(List<GridRowInfo> data)
	{
		m_Store.removeAll();
		m_Store.add(data);
	}
	
	
	/**
	 * Custom GridCellRenderer to style 'linkable' value cells from which filtered
	 * Views can be opened, and also to show tooltips on the value column cells.
	 */
	class ShowInfoValueCellRenderer implements GridCellRenderer<GridRowInfo>
	{
		
		public String render(GridRowInfo rowInfo, String property, ColumnData config,
				int rowIndex, int colIndex, ListStore<GridRowInfo> store)
		{
			String text = "";
			
			if (rowInfo.get(property) != null)
			{
				String columnVal = rowInfo.getColumnValue();
				String tooltip = rowInfo.getColumnValue();
				
				text += "<span ";
				
				if ( (m_ActiveAttributes != null) && (m_ActiveAttributes.contains(rowInfo.getColumnName())) )
				{
					// Add tooltip of the form:
					// Show all evidence for username = pete
					// and add a CSS class to show a hyperlink effect.
					tooltip = "Show all evidence for ";
					tooltip += rowInfo.getColumnName();
					tooltip += " = ";
					tooltip += rowInfo.getColumnValue();
					
					text += "class=\"showInfoLink cursor-pointer\" ";
				}
				
				text += "alt=\"";
				text += tooltip;
				text += "\" title=\"";
				text += tooltip;
				text += "\">";
				text += columnVal;
				text += "</span>";
			}
			
			return text;
		}
		
	}

}
