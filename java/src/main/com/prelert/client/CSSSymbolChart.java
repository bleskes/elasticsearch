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

package com.prelert.client;

import java.util.EnumMap;

import com.prelert.data.Severity;


/**
 * Class which maintains a 'chart' of symbols to use when plotting data in charts 
 * e.g. notifications in a Causality chart. It contains methods for creating symbols
 * with colours mapped to notification severity, or colours to match HTML colour names.
 * @author Pete Harverson
 */
public class CSSSymbolChart
{
	
	/**
	 * Enumeration of symbol shapes in the chart.
	 */
	public enum Shape
	{
		SQUARE, DIAMOND, ARROW_UP, ARROW_DOWN, 
		DIABOLO_HORIZONTAL, DIABOLO_VERTICAL,
		CIRCLE, CROSS, CROSS_DIAGONAL, STAR
	}
	
	private static CSSSymbolChart 	s_Instance;
	
	private EnumMap<Shape, String>	m_SymbolMap;
	private Shape[] 				m_SymbolArray;
	
	
	private CSSSymbolChart()
	{
		// Create the map of shapes against identifiers used in CSS classes.
		m_SymbolMap = new EnumMap<Shape, String>(Shape.class);	
		
		m_SymbolMap.put(Shape.SQUARE, "sq");
		m_SymbolMap.put(Shape.DIAMOND, "diam");
		m_SymbolMap.put(Shape.ARROW_UP, "up");
		m_SymbolMap.put(Shape.ARROW_DOWN, "down");
		m_SymbolMap.put(Shape.DIABOLO_HORIZONTAL, "dbh");
		m_SymbolMap.put(Shape.DIABOLO_VERTICAL, "dbv");
		m_SymbolMap.put(Shape.CIRCLE, "cir");
		m_SymbolMap.put(Shape.CROSS, "crs");
		m_SymbolMap.put(Shape.CROSS_DIAGONAL, "x");
		m_SymbolMap.put(Shape.STAR, "star");
		
		m_SymbolArray = m_SymbolMap.keySet().toArray(new Shape[0]);
	}
	
	
	/**
	 * Returns the symbol shape at the specified index in the chart.
	 * @param index
	 * @return symbol Shape.
	 */
	public Shape getSymbolShape(int index)
	{	
		return m_SymbolArray[index % m_SymbolArray.length];
	}
	
	
	/**
	 * Returns the HTML image tag &lt;img&gt; for displaying a chart symbol with
	 * the specified shape and colour.
	 * @param symbolShape the symbol shape. Currently only SQUARE and DIAMOND supported.
	 * 	If <code>null</code> a line symbol will be rendered.
	 * @param htmlColorName standard HTML colour name, such as 'navy' or 'darkgreen'. 
	 * @return the HTML image tag <code>&lt;img&gt;</code> for the symbol. 
	 */
	public String getImageTag(Shape symbolShape, String htmlColorName)
	{
		String cssIdentifier = "line";
		if (symbolShape != null)
		{
			// TODO - so far only square and diamond images and symbols are available.
			cssIdentifier = m_SymbolMap.get(symbolShape);
		}
		
    	return "<img class=\"prl-symbol-" + cssIdentifier + 
    		" prl-symbol-" + htmlColorName + 
    		"\" src=\"images/shared/chart_lines_transp.png\" />";
	}

	
	/**
	 * Returns the HTML image tag &lt;img&gt; for displaying a transparent chart symbol 
	 * with the specified shape.
	 * @param symbolShape the symbol shape. Currently only SQUARE and DIAMOND supported.
	 * 	If <code>null</code> a line symbol will be rendered. This is used to set the
	 * 	width of the image rendered.
	 * @return the HTML image tag <code>&lt;img&gt;</code> for a transparent symbol. 
	 */
	public String getTransparentImageTag(Shape symbolShape)
	{
		if (symbolShape == null)
		{
			return "<img src=\"images/shared/chart_lines_transp.png\" />";
		}
		else
		{
			// TODO - so far only square and diamond images and symbols are available, both 10px width.
			return "<img src=\"images/shared/chart_boxes_transp.png\" />";
		}
		
	}
	
	/**
	 * Returns the HTML image tag &lt;img&gt; for displaying the symbol at the 
	 * specified index in the chart.
	 * @param index index of symbol to return. If this index is greater than the
	 * number of symbols in the chart, then the value wraps to restart at the
	 * beginning of the list.
	 * @param severity severity of data for which the symbol is being used.
	 * @return the HTML image tag &lt;img&gt; for the symbol. 
	 */
	public String getImageTag(int index, Severity severity)
	{
		return "<img class=\"prl-symbol " + getImageClass(index, severity) +  
			"\" src=\"images/shared/chart_boxes_transp.png\" />";
	}
	
	
	/**
	 * Returns the CSS class name to use for displaying the symbol at the specified
	 * specified index in the chart.
	 * @param index index of symbol. If this index is greater than the
	 * number of symbols in the chart, then the value wraps to restart at the
	 * beginning of the list.
	 * @param severity severity of data for which the symbol is being used.
	 * @return the CSS class name. 
	 */
	protected String getImageClass(int index, Severity severity)
	{
		Shape shape = getSymbolShape(index);
		String cssIdentifier = m_SymbolMap.get(shape);
		return new String("prl-symbol-" + cssIdentifier + 
				"-" + severity.toString().toLowerCase());
	}
	
	
	/**
	 * Returns the number of different symbols in the chart.
	 * @return the number of symbols.
	 */
	public int getNumberOfSymbols()
	{
		return m_SymbolMap.size();
	}
	
	
	/**
	 * Returns an instance of the CSSSymbolChart.
	 * @return the CSSSymbolChart instance.
	 */
	public static CSSSymbolChart getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new CSSSymbolChart();
		}
		return s_Instance;
	}
}
