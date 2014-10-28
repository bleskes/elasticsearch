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

import com.extjs.gxt.ui.client.GXT;

public class ListToolBarMessages
{
	private String m_EmptyMsg = GXT.MESSAGES.pagingToolBar_emptyMsg();
	private String m_FirstText = GXT.MESSAGES.pagingToolBar_firstText();
	private String m_LastText = GXT.MESSAGES.pagingToolBar_lastText();
	private String m_NextText = GXT.MESSAGES.pagingToolBar_nextText();
	private String m_PrevText = GXT.MESSAGES.pagingToolBar_prevText();
	private String m_RefreshText = GXT.MESSAGES.pagingToolBar_refreshText();



	/**
	 * Returns the empty message.
	 * 
	 * @return the empty message
	 */
	public String getEmptyMsg()
	{
		return m_EmptyMsg;
	}


	public String getFirstText()
	{
		return m_FirstText;
	}


	/**
	 * Returns the last text.
	 * 
	 * @return the last text
	 */
	public String getLastText()
	{
		return m_LastText;
	}


	/**
	 * Returns the next text.
	 * 
	 * @return the next ext
	 */
	public String getNextText()
	{
		return m_NextText;
	}


	/**
	 * Returns the previous text.
	 * 
	 * @return the previous text
	 */
	public String getPrevText()
	{
		return m_PrevText;
	}


	/**
	 * Returns the refresh text.
	 * 
	 * @return the refresh text
	 */
	public String getRefreshText()
	{
		return m_RefreshText;
	}


	/**
	 * The message to display when no records are found (defaults to "No
	 * data to display").
	 * 
	 * @param emptyMsg the empty message
	 */
	public void setEmptyMsg(String emptyMsg)
	{
		m_EmptyMsg = emptyMsg;
	}


	/**
	 * Customizable piece of the default paging text (defaults to
	 * "First Page").
	 * 
	 * @param firstText  the first text
	 */
	public void setFirstText(String firstText)
	{
		m_FirstText = firstText;
	}


	/**
	 * Customizable piece of the default paging text (defaults to
	 * "Last Page").
	 * 
	 * @param lastText the last text
	 */
	public void setLastText(String lastText)
	{
		m_LastText = lastText;
	}


	/**
	 * Customizable piece of the default paging text (defaults to
	 * "Next Page").
	 * 
	 * @param nextText the next text
	 */
	public void setNextText(String nextText)
	{
		m_NextText = nextText;
	}


	/**
	 * Customizable piece of the default paging text (defaults to "Previous
	 * Page").
	 * 
	 * @param prevText the prev text
	 */
	public void setPrevText(String prevText)
	{
		m_PrevText = prevText;
	}


	/**
	 * Customizable piece of the default paging text (defaults to
	 * "Refresh").
	 * 
	 * @param refreshText the refresh text
	 */
	public void setRefreshText(String refreshText)
	{
		m_RefreshText = refreshText;
	}
}
