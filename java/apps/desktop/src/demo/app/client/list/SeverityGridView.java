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

package demo.app.client.list;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.GridView;
import com.extjs.gxt.ui.client.widget.grid.GridViewConfig;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;


/**
 * An extension of the GWT GridView class to colour the rows in the grid according
 * to the value of the 'severity' field. The class provides a 'reverse-video' 
 * effect on row selection, swapping over the text and background colours.
 * @author Pete Harverson
 */
public class SeverityGridView extends GridView
{
	private ListStore	m_Store;
	
	
	/**
	 * Constructs a new SeverityGridView object to colour the rows in the grid 
	 * according to the value of the 'severity' field.
	 * @param store the data model of the corresponding Grid.
	 */
	public SeverityGridView(ListStore store)
	{
		m_Store = store;
		
		setViewConfig(new GridViewConfig()
		{
			public String getRowStyle(ModelData model, int rowIndex, ListStore ds)
			{
				return "severity_" + model.get("severity");
			}
		});
	}


	/**
	 * Produces a reverse-video effect on the selected row,
	 * swapping round the text and background colours.
	 */
	protected void onRowSelect(int rowIndex)
	{
		ModelData model = m_Store.getAt(rowIndex);
		
		String styleName = "severity_none_selected";
		if ( (model != null) && (model.get("severity") != null) )
		{
			styleName = "severity_" + model.get("severity") + "_selected";
		}
		
		Element row = getRow(rowIndex);
		if (row != null)
		{
			onRowOut(row); // Removes the 'x-grid3-row-over' style.
			
			// Add the selected style to each cell in the row.
			NodeList<Element> cells = row.getElementsByTagName("td");
			for (int i = 0; i < cells.getLength(); i++) 
			{
				fly(cells.getItem(i)).addStyleName(styleName);
			}
		}
	}
	
	
	/**
	 * Removes the reverse-video effect on row deselection.
	 */
	protected void onRowDeselect(int rowIndex)
	{
		ModelData model = m_Store.getAt(rowIndex);
		
		String styleName = "severity_none_selected";
		if ( (model != null) && (model.get("severity") != null) )
		{
			styleName = "severity_" + model.get("severity") + "_selected";
		}
		
		Element row = getRow(rowIndex);
		if (row != null)
		{
			// Remove the selected style from each cell in the grid.
			NodeList<Element> cells = row.getElementsByTagName("td");
			for (int i = 0; i < cells.getLength(); i++) 
			{
				fly(cells.getItem(i)).removeStyleName(styleName);
			}
		}
	}
	
	
}
