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

package com.prelert.client.list;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;


/**
 * Custom GridCellRenderer for the probability column to display the value
 * graphically (e.g. with a progress bar) as a percentage.
 * @author Pete Harverson
 */
public class ProbabilityCellRenderer implements GridCellRenderer<BaseModelData>
{
	private static ProbabilityCellRenderer s_Instance;
	
	
	/**
	 * Returns the HTML to be used in a grid cell displaying probability.
	 * @return  the HTML to be used in a grid cell. 
	 */
	public String render(BaseModelData model, String property, ColumnData config,
			int rowIndex, int colIndex, ListStore<BaseModelData> store)
	{
		// For discussion on rendering a progress bar in a grid cell see:
		// http://extjs.com/forum/showthread.php?t=52955
		
		if (model.get(property) != null)
		{
			// probability value is percentage as an integer between 0 and 100.
			Integer probVal = (Integer)(model.get(property));
			
			if (probVal >= 0)
			{
				String probPercent = probVal.toString() + "%";
				
				String htmlTxt = "<div class=\"prob-progress-wrap\">";
				htmlTxt += "<div class=\"prob-progress-inner\" style=\"width:100%;\">";
	
				// Add the progress bar as a scaled image:
				htmlTxt += "<img ";
				
				if (probVal < 100)
				{
					htmlTxt += "style=\"border-right:1px solid #7FA9E4;\" ";
				}
				
				htmlTxt += "src=\"prelertdesktop/desktop/images/progress-bg.gif\" width=\"";
				htmlTxt += probPercent;
				htmlTxt += "\" height=\"100%\" />";
				
				// Add the textual % value.
				//htmlTxt += "<span class=\"prob-progress-text\" style=\"width:100%;\">";
				//htmlTxt += probPercent;
				//htmlTxt += "</span>";
				
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				
				return htmlTxt;
				
				/*
				// This does not center the text but does it all with styles:
				String htmlTxt = "<div class=\"prob-progress-wrap\">";
				htmlTxt += "<div class=\"prob-progress-inner\">";
				htmlTxt += "<div class=\"prob-progress-bar\" style=\"width:";
				htmlTxt += probPercent;
				//htmlTxt += "5%";
				htmlTxt += ";\">";
				htmlTxt += "<div class=\"prob-progress-text prob-progress-text-back\" ";
				htmlTxt += "style=\"width:100%;\">";
				htmlTxt += probPercent;
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				htmlTxt += "</div>";	
				*/
			}
			else
			{
				// Negative value.
				return "";
			}
		}
		else
		{
			return "";
		}
	}
	
	
	/**
	 * Returns an instance of the ProbabilityCellRenderer.
	 * @return a ProbabilityCellRenderer.
	 */
	public static ProbabilityCellRenderer getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new ProbabilityCellRenderer();
		}
		return s_Instance;
	}
}

