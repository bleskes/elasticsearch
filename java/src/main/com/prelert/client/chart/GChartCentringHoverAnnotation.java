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

import com.extjs.gxt.ui.client.widget.Html;
import com.googlecode.gchart.client.HoverUpdateable;
import com.googlecode.gchart.client.GChart.AnnotationLocation;
import com.googlecode.gchart.client.GChart.Axis;
import com.googlecode.gchart.client.GChart.Curve;
import com.googlecode.gchart.client.GChart.Symbol;


/**
 * Widget that provides tooltips for points on a GChart so that the tooltip is
 * located in a direction from the hovered over point towards the centre of the chart.
 * This should ensure that the tooltip is always fully visible in the chart area.
 * @author Pete Harverson
 */
public class GChartCentringHoverAnnotation extends Html implements HoverUpdateable
{
	@Override
	public void hoverUpdate(Curve.Point hoveredOver)
	{
		Axis xAxis = hoveredOver.getParent().getParent().getXAxis();
		double xAxisMidPoint = (xAxis.getAxisMin() + xAxis.getAxisMax())/2;
		
		Axis yAxis = hoveredOver.getParent().getParent().getYAxis();
		double yAxisTwoThirds = 2*(yAxis.getAxisMin() + yAxis.getAxisMax())/3;
		
		Symbol symbol = hoveredOver.getParent().getSymbol();
		
		if (xAxis.getMouseCoordinate() <= xAxisMidPoint)
		{
			if (hoveredOver.getY() <= yAxisTwoThirds)
			{
				symbol.setHoverLocation(AnnotationLocation.NORTHEAST);
			}
			else
			{
				symbol.setHoverLocation(AnnotationLocation.SOUTHEAST);
			}
		}
		else
		{
			if (yAxis.getMouseCoordinate() <= yAxisTwoThirds)
			{
				symbol.setHoverLocation(AnnotationLocation.NORTHWEST);
			}
			else
			{
				symbol.setHoverLocation(AnnotationLocation.SOUTHWEST);
			}
		}
		
		setHtml(hoveredOver.getHovertext());
	}

	
	@Override
	public void hoverCleanup(Curve.Point hoveredAwayFrom)
	{
	}
}
