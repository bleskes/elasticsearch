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

package com.prelert.client.gxt;

import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.RowExpander;
import com.google.gwt.user.client.Element;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * An extension of the standard GXT RowExpander Grid component plugin that uses
 * a custom GridCellRenderer for rendering the expanded row. The plugin also 
 * offers extended control over expanding and contracting the rows.
 *
 * The custom renderer for the expanded part comes from the Ext GWT forum posting:
 * http://www.extjs.com/forum/showthread.php?p=424228#post424228
 *
 * @author Pete Harverson
 */
public class ExtendedRowExpander <M extends ModelData> extends RowExpander
{
	private GridCellRenderer<M> m_ExpandRenderer;
	java.util.Map<El, Component> m_Layouts = new java.util.HashMap<El, Component>();


	/**
	 * Creates a new extended row expander.
	 * @param expandRenderer the renderer to be used to render the expanded row.
	 */
    public ExtendedRowExpander(GridCellRenderer<M> expandRenderer)
	{
		super();
		m_ExpandRenderer = expandRenderer;
		

		setRenderer(new GridCellRenderer<ModelData>() {
		      public String render(ModelData model, String property, ColumnData d, int rowIndex, int colIndex,
		          ListStore<ModelData> store, Grid<ModelData> grid) {
		    	  
		    	  ProbableCauseModelCollection probCauseColl = (ProbableCauseModelCollection)model;
		    	  if (probCauseColl.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
		    	  {
		    		  d.cellAttr = "rowspan='2'";
		    		  return "<div class='x-grid3-row-expander'>&#160;</div>";
		    	  }
		    	  else
		    	  {
		    		  d.cellAttr = "rowspan='2'";
		    		  return "";
		    	  }
		      }
		    });


	}


	/**
	 * Expand all the rows in the grid.
	 */
	public void expandAllRows()
	{
		for (int rowIndex = 0; rowIndex < grid.getStore().getCount(); rowIndex++)
		{
			expandRow(rowIndex);
		}
	}


	/**
	 * Collapses all rows in the grid.
	 */
	public void collapseAllRows()
	{
		for (int rowIndex = 0; rowIndex < grid.getStore().getCount(); rowIndex++)
		{
			collapseRow(rowIndex);
		}
	}


	/**
	 * Toggles all the rows in the grid.
	 */
	public void toggleAllRows()
	{
		for (int rowIndex = 0; rowIndex < grid.getStore().getCount(); rowIndex++)
		{
			toggleRow(rowIndex);
		}
	}


	/**
	 * Expands the specified row.
	 * @param rowIndex index of the row to expand.
	 */
	public void expandRow(int rowIndex)
	{
		expandRow(getRowAsEl(rowIndex));
	}


	/**
	 * Collapses the specified row.
	 * @param rowIndex index of the row to collapse.
	 */
	public void collapseRow(int rowIndex)
	{
		collapseRow(getRowAsEl(rowIndex));
	}


	/**
	 * Toggles the specified row.
	 * 
	 * @param rowIndex index of the row
	 */
	public void toggleRow(int rowIndex)
	{
		toggleRow(getRowAsEl(rowIndex));
	}


	/**
	 * Returns the row as El.
	 * @param rowIndex index of row to return.
	 * @return El row
	 */
	protected El getRowAsEl(int rowIndex)
	{
		return El.fly(grid.getView().getRow(rowIndex));
	}


	/**
	 * Returns the RowExpander's cell renderer.
	 * @return the renderer being used to render the expanded row.
	 */
	public GridCellRenderer<M> getExpandRenderer()
	{
		return m_ExpandRenderer;
	}


	/**
	 * Sets the RowExpander's cell renderer (pre-render).
	 * @param expandRenderer the renderer to be used to render the expanded row.
	 */
	public void setExpandRenderer(GridCellRenderer<M> expandRenderer)
	{
		m_ExpandRenderer = expandRenderer;
	}


	@Override
	protected boolean beforeExpand(ModelData model, Element body, El row,
	        int rowIndex)
	{
		if (m_ExpandRenderer == null)
		{
			return super.beforeExpand(model, body, row, rowIndex);
		}
		else
		{
			if (fireEvent(Events.BeforeExpand))
			{
				return makePanel(model, body, row, rowIndex);
			}
			return false;
		}
	}


	@Override
	protected void collapseRow(El row)
	{
		if (fireEvent(Events.BeforeCollapse))
		{
			row.replaceStyleName("x-grid3-row-expanded", "x-grid3-row-collapsed");
			// Detach so we don't bleed memory
			if (m_Layouts.get(row) != null)
			{
				ComponentHelper.doDetach(m_Layouts.get(row));
			}
			m_Layouts.remove(row);
			fireEvent(Events.Collapse);
		}
	}


	/**
	 * Creates the HTML or Widget which displays the contents of the expanded row.
	 * @return <code>true</code> if the expanded row's contents were rendered.
	 */
	protected boolean makePanel(ModelData model, Element body, El row,
	        int rowIndex)
	{
		// TODO : Fix warnings
		Object detail = m_ExpandRenderer.render((M)model, null, null, rowIndex, 0,
		       (ListStore<M>)getGrid().getStore(), (Grid<M>)getGrid());

		if (detail instanceof Component)
		{
			m_Layouts.put(row, (Component) detail);
			body.setInnerHTML("");
			m_Layouts.get(row).render(body);
			ComponentHelper.doAttach(m_Layouts.get(row));
		}
		else if (detail != null)
		{
			m_Layouts.put(row, null);
			body.setInnerHTML(detail.toString());
		}
		else
		{
			m_Layouts.put(row, null);
			body.setInnerHTML("");
		}
		return true;
	}


	/**
	 * Dynamically updates the row for the specified model for use when a property in the
	 * model has been changed since the row was rendered.
	 * @param model
	 */
	public void updateGridRowOnClick(ModelData model)
	{
		// Get the element for this row.
		com.google.gwt.dom.client.Element row = grid.getView().getRow(model);

		El rowEl = El.fly(row);
		com.google.gwt.user.client.Element body = rowEl.childElement("div.x-grid3-row-body");

		// Update the HTML
		// NB. pass dummy row index - not used.
		if (body != null)
		{
			body.setInnerHTML(getBodyContent(model, 0));
		}
	}

}
