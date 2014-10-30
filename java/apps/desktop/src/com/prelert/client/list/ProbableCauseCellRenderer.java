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

package com.prelert.client.list;

import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;


/**
 * Custom GridCellRenderer used for a probable cause column in an evidence
 * table to provide a link to open up the appropriate Probable Cause View.
 * <p>
 * Example usage:
 * <pre>
 *	ColumnConfig probCauseColumn = new ColumnConfig();
 *	probCauseColumn.setId("prob_cause");
 *	probCauseColumn.setHeader("Probable Cause");
 *	probCauseColumn.setWidth(100);
 *	probCauseColumn.setRenderer(ProbableCauseCellRenderer.getInstance());
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
public class ProbableCauseCellRenderer implements GridCellRenderer<BaseModel>
{
	private static ProbableCauseCellRenderer s_Instance;
	
	
	/**
	 * Returns the HTML to be used in a grid cell displaying probable cause.
	 * @return  the HTML to be used in a grid cell. 
	 */
	public String render(BaseModel model, String property, ColumnData config,
			int rowIndex, int colIndex, ListStore<BaseModel> store)
	{
		// MySQL has no boolean value, so check is:
		// value==0 - no episode
		// value==1 - episode.
		boolean hasEpisode = false;
		if (model.get(property) != null)
		{
			Long longVal = model.get(property);
			if (longVal == 1)
			{
				hasEpisode = true;
			}
		}
		
		if (hasEpisode == true)
		{
			return "<img class=\"probcause\" src=\"prelertdesktop/desktop/images/arrow_right.gif\" " +
				"alt=\"Show Probable Cause\" title=\"Show Probable Cause\" />";
		}
		else
		{
			return "";
		}
	}
	
	
	/**
	 * Returns an instance of the ProbableCauseCellRenderer.
	 * @return a ProbableCauseCellRenderer.
	 */
	public static ProbableCauseCellRenderer getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new ProbableCauseCellRenderer();
		}
		return s_Instance;
	}

}
