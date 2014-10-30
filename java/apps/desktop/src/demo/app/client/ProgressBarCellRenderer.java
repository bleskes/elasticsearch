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

package demo.app.client;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;



/**
 * Custom GridCellRenderer which uses a progress bar to indicate the value.
 * @author Pete Harverson
 */
public class ProgressBarCellRenderer implements GridCellRenderer<BaseModelData>
{
	private static ProgressBarCellRenderer s_Instance;
	
	public String render(BaseModelData model, String property, ColumnData config,
			int rowIndex, int colIndex, ListStore<BaseModelData> store, Grid<BaseModelData> grid)
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
				
				/*
				String htmlTxt = "<div class=\"prob-progress-wrap\">";
				htmlTxt += "<div class=\"prob-progress-inner\">";
				htmlTxt += "<div class=\"prob-progress-bar\" style=\"width:";
				//htmlTxt += probPercent;
				htmlTxt += "59%";
				htmlTxt += ";\"></div>";
				htmlTxt += "<div class=\"prob-progress-text prob-progress-text-back\" ";
				htmlTxt += "style=\"width:100%;\">";
				htmlTxt += probPercent;
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				*/
				
				/*
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
			
				
				String htmlTxt = "<div class=\"prob-progress-wrap\">";
				htmlTxt += "<div class=\"prob-progress-inner\" style=\"width:100%;\">";
	
				// Add the progress bar as a scaled image:
				htmlTxt += "<img ";
				
				if (probVal < 100)
				{
					htmlTxt += "style=\"border-right:1px solid #7FA9E4;\" ";
				}
				
				htmlTxt += "src=\"images/progress-bg.gif\" width=\"";
				htmlTxt += probPercent;
				htmlTxt += "\" height=\"100%\" />";
				
				// Add the textual % value.
				//htmlTxt += "<span class=\"prob-progress-text\" style=\"width:100%;\">";
				//htmlTxt += probPercent;
				//htmlTxt += "</span>";
				
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				
				
				// style,v,text_front,text_back
				/*
				String htmlTxt = "<div class=\"prob-progress-wrap\">";
				htmlTxt += "<div class=\"x-progress-inner\">";
				htmlTxt += "<div class=\"x-progress-bar\" style=\"width:";
				htmlTxt += probPercent;
				htmlTxt += ";\">";
				htmlTxt += "<div class=\"x-progress-text\" style=\"width:100%;\"> " + probPercent;
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				htmlTxt += "<div class=\"x-progress-text x-progress-text-back\" style=\"width:100%;\">" + probPercent;
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				htmlTxt += "</div>";
				*/
				
				return htmlTxt;
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
	
	
	public static ProgressBarCellRenderer getInstance()
	{
		if(s_Instance == null) 
		{
			s_Instance = new ProgressBarCellRenderer();
		}
		return s_Instance;
	}


}
