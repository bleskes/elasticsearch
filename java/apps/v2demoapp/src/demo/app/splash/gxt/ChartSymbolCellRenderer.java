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

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;

import demo.app.client.CSSColorChart;
import demo.app.data.DataSourceCategory;
import demo.app.data.Severity;
import demo.app.data.gxt.ProbableCauseModelCollection;


/**
 * Custom GridCellRenderer used for the symbol column in the Causality View aggregated
 * probable cause list. It acts as a key for the causality chart, rendering the symbol
 * used to display that probable cause.
 * @author Pete Harverson
 */
public class ChartSymbolCellRenderer implements GridCellRenderer<ProbableCauseModelCollection>
{

	private static ChartSymbolCellRenderer s_Instance;
	
	
	/**
	 * Returns the HTML to be used in a grid cell displaying the symbol for a
	 * probable cause in chart.
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
			
			String html = "";
			
			switch (category)
			{
				case NOTIFICATION:
					Severity severity = model.getSeverity();
					html = "<img class=\"prl-chartSymbolBox-" + severity.toString().toLowerCase() + 
						"\" src=\"images/chart_boxes_transp.png\" />";
					break;
					
				case TIME_SERIES:
					// IDs for time series ProbableCauseModelCollection are generated
					// sequentially to map to a unique color chart index.
					int index = model.getId();
					String colorName = CSSColorChart.getInstance().getColorName(index);
					
					html = "<img class=\"prl-chartSymbolLine-" + colorName + 
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
