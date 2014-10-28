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

import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;


/**
 * Extension of the GXT <code>CheckBoxSelectionModel</code> with customisations
 * for the Prelert UI:
 * <ul>
 * <li>Flag to control whether to add a 'Select all' checkbox to the column header.</li>
 * <li>Selection or deselection of a row only occurs by clicking in the checkbox
 * 		itself (as opposed to anywhere in the row).</li>
 * </ul>
 * @param <M> the model data type
 * 
 * @author Pete Harverson
 */
public class GridCheckBoxSelectionModel<M extends ModelData> extends CheckBoxSelectionModel<M>
{
	/**
	 * Creates a new GridCheckBoxSelectionModel.
	 * @param showHeaderSelectAll <code>true</code> to add a 'Select all' checkbox
	 * to the header column, <code>false</code> to not show a 'Select all' control.
	 */
	public GridCheckBoxSelectionModel(boolean showHeaderSelectAll)
	{
		setSelectionMode(SelectionMode.SIMPLE);
		
		if (showHeaderSelectAll == false)
		{
			// Set the column id to customize the header row style so as to 
			// remove the 'select all' checkbox.
			getColumn().setId("prl-groupChecker");
		}
	}
	
	
	@Override
    protected void onHeaderClick(GridEvent<M> e)
    {
		El hd = e.getTargetEl().getParent();
		boolean isSelectAllShown = !(hd.hasStyleName("x-grid3-td-prl-groupChecker"));
		if (isSelectAllShown == true)
		{
			// Do nothing - no select/deselect all functionality.
			super.onHeaderClick(e);
		}
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
