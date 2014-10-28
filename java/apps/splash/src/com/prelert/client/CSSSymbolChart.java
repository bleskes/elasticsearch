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

package com.prelert.client;

import java.util.ArrayList;

import com.prelert.data.Severity;


/**
 * Class which maintains a 'chart' of symbols to use when plotting severity based
 * data in charts e.g. notifications in a Causality chart. It contains methods to
 * return the CSS class name or HTML image tag to use when plotting data of a 
 * particular severity.
 * @author Pete Harverson
 */
public class CSSSymbolChart
{
	private static CSSSymbolChart 	s_Instance;
	
	private ArrayList<String>		s_SymbolIds;
	
	private CSSSymbolChart()
	{
		s_SymbolIds = new ArrayList<String>(10);
		
		s_SymbolIds.add("sq");		// Square
		s_SymbolIds.add("diam");	// Diamond
		s_SymbolIds.add("up");		// Up triangle
		s_SymbolIds.add("down");	// Down triangle
		s_SymbolIds.add("dbh");		// Horizontal diabolo
		s_SymbolIds.add("dbv");		// Vertical diabolo
		s_SymbolIds.add("cir");		// Circle
		s_SymbolIds.add("crs");		// Cross
		s_SymbolIds.add("x");		// X
		s_SymbolIds.add("star");	// Star
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
			"\" src=\"images/chart_boxes_transp.png\" />";
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
		int listIndex = index % s_SymbolIds.size();
		return new String("prl-symbol-" + s_SymbolIds.get(listIndex) + 
				"-" + severity.toString().toLowerCase());
	}
	
	
	/**
	 * Returns the number of different symbols in the chart.
	 * @return the number of symbols.
	 */
	public int getNumberOfSymbols()
	{
		return s_SymbolIds.size();
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
