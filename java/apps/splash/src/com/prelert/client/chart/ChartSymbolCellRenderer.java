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

package com.prelert.client.chart;

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.prelert.client.CSSColorChart;
import com.prelert.client.CSSSymbolChart;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.Severity;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Custom GridCellRenderer used for the symbol column in the Causality View aggregated
 * probable cause list. It acts as a key for the causality chart, rendering the symbol
 * used to display that probable cause. The renderer uses the id of the 
 * ProbableCauseModelCollection as the index of the symbol to return.
 * @author Pete Harverson
 */
public class ChartSymbolCellRenderer implements GridCellRenderer<ProbableCauseModelCollection>
{

	private static ChartSymbolCellRenderer s_Instance;
	
	
	/**
	 * Returns the HTML to be used in a grid cell displaying the symbol for a
	 * probable cause in chart.
	 * @param model the ProbableCauseModelCollection being rendered in the grid.
	 * 	The chart uses the id of the collection as the chart index, so IDs should
	 * 	be assigned sequentially to maximise the number of different symbols used.
	 * @return  the HTML to be used in a grid cell. 
	 */
	@Override
    public Object render(ProbableCauseModelCollection model, String property,
            ColumnData config, int rowIndex, int colIndex,
            ListStore<ProbableCauseModelCollection> store,
            Grid<ProbableCauseModelCollection> grid)
    {
		if ( (model != null) && model.getDataSourceCategory() != null)
		{
			DataSourceCategory category = model.getDataSourceCategory();
			int chartIndex = model.getId();
			
			String html = "";
			
			switch (category)
			{
				case NOTIFICATION:
					Severity severity = model.getSeverity();
					html = CSSSymbolChart.getInstance().getImageTag(chartIndex, severity);
					break;
					
				case TIME_SERIES:
					String colorName = CSSColorChart.getInstance().getColorName(chartIndex);
					html = "<img class=\"prl-symbolLine prl-symbolLine-" + colorName + 
							"\" src=\"images/chart_lines_transp.png\" />";

					break;
			}
			
			return html;
		}
		else
		{
			return "";
		}
    }
	
	
	/**
	 * Returns an instance of the ChartSymbolCellRenderer.
	 * @return a ChartSymbolCellRenderer.
	 */
	public static ChartSymbolCellRenderer getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new ChartSymbolCellRenderer();
		}
		return s_Instance;
	}

}
