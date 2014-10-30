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

package com.prelert.client.chart;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;

import com.prelert.client.CSSSymbolChart;
import com.prelert.client.CSSSymbolChart.Shape;


/**
 * Custom GridCellRenderer used for rendering chart symbols in GXT Grid cells.
 * The colour and shape of the symbol rendered is dependent on the value of 
 * the symbolColor and symbolShape properties of the <code>ModelData</code> 
 * object being rendered.
 * 
 * @param <M> the model type
 * 
 * @author Pete Harverson
 */
public class ChartSymbolCellRenderer implements GridCellRenderer<ModelData>
{

	public static final String DEFAULT_COLOR_PROPERTY = "symbolColor";
	public static final String DEFAULT_SHAPE_PROPERTY = "symbolShape";
	
	private String	m_SymbolColorProperty;
	private String	m_SymbolShapeProperty;
	
	
	/**
	 * Creates a new GridCellRenderer to render chart symbols in grid cells, using
	 * the default property names of the symbol colour (symbolColor) and symbol
	 * shape (symbolShape).
	 */
	public ChartSymbolCellRenderer()
	{
		m_SymbolColorProperty = DEFAULT_COLOR_PROPERTY;
		m_SymbolShapeProperty = DEFAULT_SHAPE_PROPERTY;
	}
	
	
	/**
	 * Returns the HTML to be used in a grid cell to display a chart symbol for
	 * the specified item of model data.
	 * <p>
	 * The colour of the symbol rendered is determined by the value of the
	 * <code>symbolColor</code> property, which should be the HTML colour name
	 * of a colour in the CSSColor chart.
	 * <p>
	 * The shape of the symbol rendered is determined by the value of the 
	 * <code>symbolShape</code> property, which must be of type {@link Shape}. If 
	 * <code>null</code>, a line will be rendered.
	 * @param model the ModelData being rendered in the grid. By default, the 
	 * renderer will look for the values of the <code>symbolColor</code> and 
	 * <code>symbolShape</code> properties to determine the symbol to render.
	 * @return  the HTML to be used in a grid cell. 
	 */
    @Override
    public Object render(ModelData model, String property, ColumnData config, 
    		int rowIndex, int colIndex, ListStore<ModelData> store, Grid<ModelData> grid)
    {
	    String symbolColor = model.get(m_SymbolColorProperty);
	    Shape symbolShape = model.get(m_SymbolShapeProperty);
	    
	    if (symbolColor != null)
	    {
	    	// Return the HTML to render a symbol for the specified shape.
	    	return CSSSymbolChart.getInstance().getImageTag(symbolShape, symbolColor);
	    }
	    
	    return "";
    }
}
