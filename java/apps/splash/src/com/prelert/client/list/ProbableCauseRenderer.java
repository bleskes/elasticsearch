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

package com.prelert.client.list;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;

import com.prelert.client.ClientUtil;


/**
 * Custom GridCellRenderer used for a probable cause column in a grid to
 * provide a link to open up the appropriate Causality View. The renderer will 
 * only work for columns that display booleans, with a probable cause indicator
 * being rendered if the value of the column is <code>true</code>.
 * <p>
 * Example usage:
 * <pre>
 *	ColumnConfig probCauseColumn = new ColumnConfig();
 *	probCauseColumn.setId("prob_cause");
 *	probCauseColumn.setHeader("Probable Cause");
 *	probCauseColumn.setWidth(100);
 *	probCauseColumn.setRenderer(ProbableCauseRenderer.getInstance());
 *
 *	m_Grid.addListener(Events.CellClick, new Listener<GridEvent>(){
 *		public void handleEvent(GridEvent event) 
 *		{
 *			if(event.getTarget(".probcause", 1) != null)
 * 			{
 * 				ListStore<EventRecord> gridStore = m_Grid.getStore();
 * 				EventRecord selectedRec = gridStore.getAt(event.rowIndex);
 * 				m_Desktop.openProbableCauseView(selectedRec);
 * 			}
 * 		} 
 * 	});
 *
 * </pre>
 * @author Pete Harverson
 */
public class ProbableCauseRenderer implements GridCellRenderer<BaseModelData>
{
	private static ProbableCauseRenderer s_Instance;
	
	
	/**
	 * Returns the HTML to be used in a grid cell displaying probable cause.
	 * A probable cause indicator will be rendered if the value of the column
	 * is <code>true</code>.
	 * @return  the HTML to be used in a grid cell. 
	 */
	public String render(BaseModelData model, String property, ColumnData config,
			int rowIndex, int colIndex, ListStore<BaseModelData> store, Grid<BaseModelData> grid)
	{
		boolean hasProbableCause = model.get(property, false);		
		if (hasProbableCause == true)
		{
			String tooltip = ClientUtil.CLIENT_CONSTANTS.showProbableCause();
			return "<img class=\"probcause\" src=\"images/arrow_right.gif\" " +
				"alt=\"" + tooltip + "\" title=\"" + tooltip + "\" />";
		}
		else
		{
			return "";
		}
		
	}
	
	
	/**
	 * Returns an instance of the ProbableCauseRenderer.
	 * @return a ProbableCauseRenderer.
	 */
	public static ProbableCauseRenderer getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new ProbableCauseRenderer();
		}
		return s_Instance;
	}


}
