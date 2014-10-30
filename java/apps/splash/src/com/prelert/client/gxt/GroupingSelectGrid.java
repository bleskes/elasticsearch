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

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridGroupRenderer;
import com.extjs.gxt.ui.client.widget.grid.GroupColumnData;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.google.gwt.dom.client.NodeList;    
import com.google.gwt.user.client.Element; 

import com.prelert.client.ClientUtil;


/**
 * An extension of the GXT Grid widget which displays the data in groups.
 * A checkbox in the grouping row allows all the items in a group to be selected
 * or deselected at once. The 'select all' checkbox in the grid header is removed
 * to prevent all rows in the grid being selected at once.
 * @author Pete Harverson
 * 
 * @param <M> the model type
 */
public class GroupingSelectGrid<M extends ModelData> extends Grid<M>
{
	private String m_CheckedStyle = "x-grid3-group-check";   
	private String m_UncheckedStyle = "x-grid3-group-uncheck"; 
	
	private GroupingView m_GroupingView;
	
	
	/**
	 * Creates a new GroupingSelectGrid.
	 * @param store the grouping store.
	 * @param columns the columns to display in the grid. A checkbox selection
	 * 		model will be added automatically as the first column in the grid.
	 */
	public GroupingSelectGrid(GroupingStore<M> store, List<ColumnConfig> columns)
	{
		this.store = store;
		
		// Create the selection model.
		GroupingGridSelectionModel selectionModel = new GroupingGridSelectionModel();
		
		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
		config.add(selectionModel.getColumn());
		config.addAll(columns);
		this.cm = new ColumnModel(config);

		disabledStyle = null;
		baseStyle = "x-grid-panel";
		disableTextSelection(true);

		m_GroupingView = new GroupingView()
		{

			@Override
			protected void onMouseDown(GridEvent<ModelData> ge)
			{
				// Overrides onMouseDown to enable the select / deselect group functionality.
				El hd = ge.getTarget(".x-grid-group-hd", 10);
				El target = ge.getTargetEl();
				if (hd != null && target.hasStyleName(m_UncheckedStyle) || 
						target.hasStyleName(m_CheckedStyle))
				{
					boolean checked = !ge.getTargetEl().hasStyleName(m_UncheckedStyle);
					checked = !checked;
					if (checked)
					{
						ge.getTargetEl().replaceStyleName(m_UncheckedStyle, m_CheckedStyle);
					}
					else
					{
						ge.getTargetEl().replaceStyleName(m_CheckedStyle, m_UncheckedStyle);
					}

					Element group = (Element) findGroup(ge.getTarget());
					if (group != null)
					{
						NodeList<Element> rows = El.fly(group).select(".x-grid3-row");
						List<ModelData> temp = new ArrayList<ModelData>();
						for (int i = 0; i < rows.getLength(); i++)
						{
							Element r = rows.getItem(i);
							int idx = findRowIndex(r);
							ModelData m = grid.getStore().getAt(idx);
							temp.add(m);
						}
						if (checked)
						{
							grid.getSelectionModel().select(temp, true);
						}
						else
						{
							grid.getSelectionModel().deselect(temp);
						}
					}
					return;
				}
				super.onMouseDown(ge);

			}

		};

		m_GroupingView.setShowGroupedColumn(false);
		m_GroupingView.setForceFit(true);

		// Set the renderer for the grouping row to be of the form:
		// type: data_log (3 items)
		m_GroupingView.setGroupRenderer(new GridGroupRenderer()
		{
			public String render(GroupColumnData data)
			{
				String f = GroupingSelectGrid.this.cm.getColumnById(data.field).getHeader();
				String itemCount = ClientUtil.CLIENT_CONSTANTS.itemCount(data.models.size());
				return "<div class='x-grid3-group-checker'><div class='" + m_UncheckedStyle +
						"'> </div></div>&nbsp;" + f + ": " + data.group + " (" + itemCount + ")";
			}
		});
		setView(m_GroupingView);

		setSelectionModel(selectionModel);
		addPlugin(selectionModel);
	}
	
	
	private void setGroupChecked(Element group, boolean checked)
	{
		El checkEl = null;
		
		El groupChecker = El.fly(group).selectNode(".x-grid3-group-checker");
		if (groupChecker != null)
		{
			checkEl = groupChecker.firstChild();
			checkEl.replaceStyleName(
			        checked ? m_UncheckedStyle : m_CheckedStyle,
			        checked ? m_CheckedStyle : m_UncheckedStyle);
		}
	}


	/**
	 * Extension of the GXT CheckBoxSelectionModel which removes the 'select all'
	 * functionality via the column header and which manages the state of the 
	 * group selection checkboxes. It also changes the selection so that a row
	 * is only selected / deselected if the user clicks on the checkbox.
	 */
	class GroupingGridSelectionModel extends CheckBoxSelectionModel<M>
	{
		protected GroupingGridSelectionModel()
		{
			setSelectionMode(SelectionMode.SIMPLE);
			getColumn().setId("prl-groupChecker");
		}


		@Override
		protected void doDeselect(List<M> models, boolean supressEvent)
		{
			super.doDeselect(models, supressEvent);
			
			NodeList<com.google.gwt.dom.client.Element> groups = m_GroupingView.getGroups();
			search: for (int i = 0; i < groups.getLength(); i++)
			{
				com.google.gwt.dom.client.Element group = groups.getItem(i);
				NodeList<Element> rows = El.fly(group).select(".x-grid3-row");
				for (int j = 0, len = rows.getLength(); j < len; j++)
				{
					Element r = rows.getItem(j);
					int idx = grid.getView().findRowIndex(r);
					M m = grid.getStore().getAt(idx);
					if (!isSelected(m))
					{
						setGroupChecked((Element) group, false);
						continue search;
					}
				}
			}

		}


		@Override
		protected void doSelect(List<M> models, boolean keepExisting,
		        boolean supressEvent)
		{
			super.doSelect(models, keepExisting, supressEvent);
			
			NodeList<com.google.gwt.dom.client.Element> groups = m_GroupingView.getGroups();	
			search: for (int i = 0; i < groups.getLength(); i++)
			{
				com.google.gwt.dom.client.Element group = groups.getItem(i);
				NodeList<Element> rows = El.fly(group).select(".x-grid3-row");
				for (int j = 0, len = rows.getLength(); j < len; j++)
				{
					Element r = rows.getItem(j);
					int idx = grid.getView().findRowIndex(r);
					M m = grid.getStore().getAt(idx);
					if (!isSelected(m))
					{
						continue search;
					}
				}
				setGroupChecked((Element) group, true);

			}
		}

		
        @Override
        protected void onHeaderClick(GridEvent<M> e)
        {
	        // Do nothing - no select/deselect all functionality.
        }
		
        
		@Override
		protected void handleMouseDown(GridEvent<M> e)
		{
			// Only select / deselect via the checkbox.
			if (e.getTarget().getClassName().equals("x-grid3-row-checker"))
			{
				super.handleMouseDown(e);	
			}
		}


		@Override
		protected void handleMouseClick(GridEvent<M> e)
		{
			// Only select / deselect via the checkbox.
			if (e.getTarget().getClassName().equals("x-grid3-row-checker"))
			{
				super.handleMouseClick(e);
			}
		}
	}
}
