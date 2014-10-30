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

import demo.app.data.CausalityView;
import demo.app.data.View;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.CausalityQueryServiceAsync;
import demo.app.service.DatabaseServiceLocator;


/**
 * Abstract base class for a Prelert Causality View displayed in an
 * Ext GWT (GXT) Window.
 * 
 * @author Pete Harverson
 */
public abstract class CausalityViewWindow extends ViewWindow
{
	protected CausalityView					m_CausalityView;
	
	protected CausalityQueryServiceAsync 	m_CausalityQueryService;
	
	private EvidenceModel						m_Evidence;
	
	
	/**
	 * Creates a new causality window for displaying the specified view.
	 */
	public CausalityViewWindow(CausalityView causalityView)
	{
		m_CausalityView = causalityView;
		m_CausalityQueryService = DatabaseServiceLocator.getInstance().getCausalityQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setResizable(true);
		
		setHeading(m_CausalityView.getName());
		
		if (m_CausalityView.getStyleId() != null)
	    {
			setIconStyle(m_CausalityView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("prob-cause-win-icon");
	    }
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the causality view displayed in the Window.
	 */
	public View getView()
	{
		return m_CausalityView;
	}
	
	
    /**
     * Returns the item of evidence whose probable causes are being 
     * displayed in the window.
     * @return the evidence whose probable causes are being displayed.
     */
    public EvidenceModel getEvidence()
    {
    	return m_Evidence;
    }


	/**
	 * Sets the item of evidence whose probable causes are to be 
	 * displayed in the window, and then reloads the view.
     * @param evidence the id of the evidence whose probable causes are
     * to be displayed.
     */
    public void setEvidence(EvidenceModel evidence)
    {
    	m_Evidence = evidence;
    	load();
    }
    
}
