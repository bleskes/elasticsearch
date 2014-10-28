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

package demo.app.client;

import java.util.ArrayList;


/**
 * Class which maintains a colour 'chart' i.e. a list of HTML colours with their
 * hex values and colour names as supported by most major browsers. This should be
 * used by components which need access to a range of different colours e.g. a
 * time series chart where different lines need to be plotted in different colours.
 * @author Pete Harverson
 */
public class CSSColorChart
{
	private static CSSColorChart 					s_Instance;
	
	private ArrayList<CSSColorChartEntry>			s_ColorList;
	
	
	
	private CSSColorChart()
	{
		s_ColorList = new ArrayList<CSSColorChartEntry>(20);
		
		s_ColorList.add(new CSSColorChartEntry("navy", "#000080"));
		s_ColorList.add(new CSSColorChartEntry("red", "#ff0000"));
		s_ColorList.add(new CSSColorChartEntry("orange", "#ffa500"));
		s_ColorList.add(new CSSColorChartEntry("darkgreen", "#006400"));
		s_ColorList.add(new CSSColorChartEntry("purple", "#800080"));
		s_ColorList.add(new CSSColorChartEntry("dodgerblue", "#1e90ff"));
		s_ColorList.add(new CSSColorChartEntry("maroon", "#800000"));
		s_ColorList.add(new CSSColorChartEntry("goldenrod", "#daa520"));
		s_ColorList.add(new CSSColorChartEntry("lime", "#00ff00"));
		s_ColorList.add(new CSSColorChartEntry("magenta", "#ff00ff"));
		s_ColorList.add(new CSSColorChartEntry("cyan", "#00ffff"));
		s_ColorList.add(new CSSColorChartEntry("indianred", "#cd5c5c"));
		s_ColorList.add(new CSSColorChartEntry("orangered", "#ff4500"));
		s_ColorList.add(new CSSColorChartEntry("greenyellow", "#adff2f"));
		s_ColorList.add(new CSSColorChartEntry("indigo", "#4B0082"));
		s_ColorList.add(new CSSColorChartEntry("blue", "#0000ff"));
		s_ColorList.add(new CSSColorChartEntry("darkred", "#8b0000"));
		s_ColorList.add(new CSSColorChartEntry("gold", "#ffd700"));
		s_ColorList.add(new CSSColorChartEntry("green", "#008000"));
		s_ColorList.add(new CSSColorChartEntry("violet", "#ee82ee"));
	}
	
	
	/**
	 * Returns the CSS hex colour notation for the colour at the specified index
	 * in the chart.
	 * @param index index of colour to return. If this index is greater than the
	 * number of colours in the chart, then the value wraps to restart at the
	 * beginning of the list.
	 * @return the CSS hex colour notation for the given severity e.g. '#ff0000'. 
	 */
	public String getColor(int index)
	{
		CSSColorChartEntry colorEntry = s_ColorList.get(index % s_ColorList.size());
		return colorEntry.getHex();
	}
	
	
	/**
	 * Returns the HTML colour name, as supported by all the major browsers, for 
	 * the colour at the specified index in the chart
	 * @param index index of colour to return. If this index is greater than the
	 * number of colours in the chart, then the value wraps to restart at the
	 * beginning of the list.
	 * @return the HTML colour name e.g. 'navy' or 'darkgreen'. 
	 */
	public String getColorName(int index)
	{
		CSSColorChartEntry colorEntry = s_ColorList.get(index % s_ColorList.size());
		return colorEntry.getName();
	}
	

	
	/**
	 * Returns an instance of the CSSColorChart.
	 * @return the CSSColorChart instance.
	 */
	public static CSSColorChart getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new CSSColorChart();
		}
		return s_Instance;
	}
	
	
	/**
	 * Class encapsualating an entry in the chart, holding its name and hex value.
	 */
	protected class CSSColorChartEntry
	{
		private String m_ColorName;
		private String m_Hex;
		
		protected CSSColorChartEntry(String colorName, String hex)
		{
			m_ColorName = colorName;
			m_Hex = hex;
		}
	
		
		protected String getName()
		{
			return m_ColorName;
		}
		
		
		protected String getHex()
		{
			return m_Hex;
		}
		
	}
}
