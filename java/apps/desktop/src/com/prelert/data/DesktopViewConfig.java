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

package com.prelert.data;

import java.io.Serializable;
import java.util.List;

/**
 * Desktop view configuration which encapsulates details on the
 * views that will be created on the Prelert desktop.
 * @author Pete Harverson
 */
public class DesktopViewConfig implements Serializable
{
	private List<EvidenceView>		m_EvidenceViews;	// The Desktop evidence view.
	private ExceptionView			m_ExceptionView;
	private List<UsageView>			m_TimeSeriesViews;	// The Desktop time series views.
	private CausalityView			m_CausalityView;
	private HistoryView				m_HistoryView;
	
	private List<View>				m_UserViews;		// User-defined views.
	
	
    /**
	 * Returns the list of desktop Evidence Views that have been configured 
	 * in the view configuration file.
     * @return the list of Evidence views.
     */
    public List<EvidenceView> getEvidenceViews()
    {
    	return m_EvidenceViews;
    }


	/**
	 * Sets the list of desktop Evidence Views that have been configured 
	 * in the view configuration file.
     * @param evidenceViews the list of desktop Evidence views.
     */
    public void setEvidenceViews(List<EvidenceView> evidenceViews)
    {
    	m_EvidenceViews = evidenceViews;
    } 
	
    
	/**
	 * Returns the Exception View that has been configured in the view 
	 * configuration file.
	 * @return the Exception View.
     */
    public ExceptionView getExceptionView()
    {
    	return m_ExceptionView;
    }


	/**
     * Sets the reference to the Exception View that has been configured 
	 * in the view configuration file.
	 * @param exceptionView the configured Exception View.
     */
    public void setExceptionView(ExceptionView exceptionView)
    {
    	m_ExceptionView = exceptionView;
    }


	/**
	 * Returns the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @return the list of time series views.
     */
    public List<UsageView> getTimeSeriesViews()
    {
    	return m_TimeSeriesViews;
    }


	/**
	 * Sets the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @param timeSeriesViews the list of desktop time series views.
     */
    public void setTimeSeriesViews(List<UsageView> timeSeriesViews)
    {
    	m_TimeSeriesViews = timeSeriesViews;
    }
    
    
	/**
	 * Returns the Causality View that has been configured in the view 
	 * configuration file.
	 * @return the Causality View or <code>null</code> if no Causality View has 
	 * 			been configured.
	 */
	public CausalityView getCausalityView()
	{
		return m_CausalityView;
	}
    
	
	/**
	 * Sets the reference to the Causality View that has been configured 
	 * in the view configuration file.
	 * @param historyView the configured History View.
	 */
	public void setCausalityView(CausalityView causalityView)
	{
		m_CausalityView = causalityView;
	}


	/**
	 * Returns the Evidence History View that has been configured in the view 
	 * configuration file.
	 * @return the History View or <code>null</code> if no History View has been
	 * 			configured.
	 */
	public HistoryView getHistoryView()
	{
		return m_HistoryView;
	}
    
	
	/**
	 * Sets the reference to the Evidence History View that has been configured 
	 * in the view configuration file.
	 * @param historyView the configured History View.
	 */
	public void setHistoryView(HistoryView historyView)
	{
		m_HistoryView = historyView;
	}
	

	/**
	 * Returns the list of user-defined desktop views.
	 * @return list of user-defined views which should be added to the desktop
	 * in addition to the Prelert 'system' views.
	 */
	public List<View> getUserDefinedViews()
	{
		return m_UserViews;
	}
	
	
	/**
	 * Sets the list of user-defined desktop views.
	 * @param userViews list of user-defined views which should be added to the desktop
	 * in addition to the Prelert 'system' views.
	 */
	public void setUserDefinedViews(List<View> userViews)
	{
		m_UserViews = userViews;
	}
}
