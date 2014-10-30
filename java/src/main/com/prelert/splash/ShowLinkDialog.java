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

package com.prelert.splash;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;

import com.prelert.client.ClientUtil;


/**
 * An extension of the Ext GWT (GXT) dialog for displaying a link for the user 
 * to share data from the UI, such as a link to an activity in the Analysis module.
 * @author Pete Harverson
 */
public class ShowLinkDialog extends Dialog
{
	private static ShowLinkDialog s_Instance;
	
	private Text 	m_Label;
	private Text 	m_Link;
	
	
	/**
	 * Returns the application-wide instance of the dialog used for displaying
	 * a link to a view in the UI.
	 * @return application-wide instance of the Show Link dialog.
	 */
	public static ShowLinkDialog getInstance()
	{
		if (s_Instance == null)
		{
			s_Instance = new ShowLinkDialog();
		}
		
		return s_Instance;
	}
	
	
	/**
	 * Creates a new dialog which displays the link for the user to share data 
	 * from the UI, such as a link to an activity in the Analysis module.
	 */
	private ShowLinkDialog()
	{
		setHeading(ClientUtil.CLIENT_CONSTANTS.urlLink()); 
		setSize(430, 160);
		setHideOnButtonClick(true);  

		// Add an instruction label and a Text field containing the link.
	    setLayout(new RowLayout(Orientation.VERTICAL));
	  
	    m_Label = new Text();   
	    m_Label.addStyleName("x-form-label");   
	  
	    m_Link = new Text();  
	    m_Link.addStyleName("x-form-label");
	    m_Link.addStyleName("x-form-field");
	    m_Link.setStyleAttribute("padding-left", "3px");  
	    m_Link.setStyleAttribute("backgroundColor", "white"); 
	    m_Link.setBorders(true);
	  
	    add(m_Label, new RowData(1, -1, new Margins(10)));   
	    add(m_Link, new RowData(1, -1, new Margins(0, 10, 0, 10)));   
	}
	
	
	/**
	 * Sets the link displayed in the dialog, refreshing the dialog heading and
	 * information label with the supplied text.
	 * @param heading the heading text.
	 * @param linkDescription text to use in the descriptive label for the link.
	 * @param linkToURL the complete URL link.
	 */
	public void setLink(String heading, String linkDescription, String linkToURL)
	{
		setHeading(heading);
		m_Label.setText(linkDescription);
		m_Link.setText(linkToURL);
	}
}
