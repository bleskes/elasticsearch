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

import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.GridViewConfig;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridView;
import com.google.gwt.dom.client.Element;


/**
 * An extension of the GWT TreeGridView class for the data sources tree
 * to style the cells in the grid on row selection and mouse over.
 * @author Pete Harverson
 */
public class DataSourceTreeGridView extends TreeGridView
{
	/**
	 * Constructs a new grid view for use by the data sources to
	 * style the cells in the grid on row selection and mouse over.
	 */
	public DataSourceTreeGridView()
	{
		setViewConfig(new GridViewConfig()
		{
			public String getRowStyle(ModelData model, int rowIndex, ListStore ds)
			{
				return "prl-dataSourceTree-row";
			}
		});
	}
	
	
	protected void onRowSelect(int rowIndex)
	{
		Element row = getRow(rowIndex);
		if (row != null)
		{
			onRowOut(row);
			//addRowStyle(row, "prl-dataSourceTree-row-selected");
		}
		
		// Add the selected style to each cell in the row.
		Element labelCell = getCell(rowIndex, 0);
		if (labelCell != null)
		{
			El nodeEl = fly(labelCell).selectNode(".prl-dataSourceTree-source-text");
			
			if (nodeEl == null)
			{
				nodeEl = fly(labelCell).selectNode(".prl-dataSourceTree-sourceType-text");
			}
			
			if (nodeEl != null)
			{
				nodeEl.addStyleName("prl-dataSourceTree-row-selected");
			}
		}
		
		/*
		Element countCell = getCell(rowIndex, 1);
		if (countCell != null)
		{
			El nodeEl = fly(countCell).selectNode(".prl-dataSourceTree-text");
			if (nodeEl != null)
			{
				nodeEl.addStyleName("prl-dataSourceTree-row-selected");
			}
		}
		*/
		
		tree.setExpanded(treeStore.getParent(tree.getStore().getAt(rowIndex)), true);
	}


	@Override
    protected void onRowDeselect(int rowIndex)
	{
		Element row = getRow(rowIndex);
		if (row != null)
		{
			//removeRowStyle(row, "prl-dataSourceTree-row-selected");
		}
		
		// Remove the selected style to each cell in the row.
		Element labelCell = getCell(rowIndex, 0);
		if (labelCell != null)
		{
			El nodeEl = fly(labelCell).selectNode(".prl-dataSourceTree-source-text");			
			if (nodeEl == null)
			{
				nodeEl = fly(labelCell).selectNode(".prl-dataSourceTree-sourceType-text");
			}
			
			if (nodeEl != null)
			{
				nodeEl.removeStyleName("prl-dataSourceTree-row-selected");
			}
		}
		
		/*
		Element countCell = getCell(rowIndex, 1);
		if (countCell != null)
		{
			El nodeEl = fly(countCell).selectNode(".prl-dataSourceTree-text");
			if (nodeEl != null)
			{
				nodeEl.removeStyleName("prl-dataSourceTree-row-selected");
			}
		}
		*/
	}
	
	
	@Override
    protected void onRowOver(Element row)
	{
		if (grid.isTrackMouseOver())
		{
			// Add the row over style to the source name cell.
			El nodeEl = fly(row).selectNode(".prl-dataSourceTree-source-text");		
			if (nodeEl == null)
			{
				nodeEl = fly(row).selectNode(".prl-dataSourceTree-sourceType-text");
			}
			
			if (nodeEl != null)
			{
				nodeEl.addStyleName("prl-dataSourceTree-row-over");
			}
			
			overRow = row;
		}
	}
	
	
	@Override
    protected void onRowOut(Element row)
	{
		if (grid.isTrackMouseOver())
		{
			// Remove the row over style from the source name cell.
			El nodeEl = fly(row).selectNode(".prl-dataSourceTree-source-text");		
			if (nodeEl == null)
			{
				nodeEl = fly(row).selectNode(".prl-dataSourceTree-sourceType-text");
			}
			
			if (nodeEl != null)
			{
				nodeEl.removeStyleName("prl-dataSourceTree-row-over");
			}
			
			overRow = null;
		}
	}
}
