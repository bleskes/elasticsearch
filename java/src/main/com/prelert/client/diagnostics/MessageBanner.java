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

package com.prelert.client.diagnostics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.google.gwt.user.client.Timer;

import com.prelert.data.MarketingMessages;


/**
 * GXT container which displays a message banner. The banner cycles through a 
 * set of marketing messages and a license expiry warning provided to it 
 * via the {@link #setMessages(MarketingMessages)} method.
 * @author Pete Harverson
 */
public class MessageBanner extends LayoutContainer
{
	/** Frequency at which to change the message displayed, in seconds. */
	public static final int BANNER_REFRESH_FREQUENCY_SECS = 60;
	
	private LabelField			m_DisplayMessage;
	private List<String>		m_MarketingMessages;
	private Date				m_ExpiryTime;
	
	private Timer			m_UpdateTimer;
	private int				m_Index;
	private int				m_NumMessages;	// Number of marketing messages plus expiry message.
	
	
	/**
	 * Creates a new banner component for cycling through a set of marketing messages.
	 */
	public MessageBanner()
	{
		addStyleName("prl-messageBanner");
		
		// Set a default message.
		m_DisplayMessage = new LabelField(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.technicalSupport());
		add(m_DisplayMessage);
		
		// Create the timer to cycle through the messages.
		m_UpdateTimer = new Timer()
        {	
			@Override
            public void run()
            {
				if (m_ExpiryTime != null)
				{
					if (m_Index == 0)
					{
						Date now = new Date();
						long msLeft = m_ExpiryTime.getTime() - now.getTime();
						if (msLeft > 0)
						{
							long days = msLeft/86400000l;
							long hours = Math.round((msLeft % 86400000l)/3600000d);
							m_DisplayMessage.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.licenseTimeLeft(
									(int)days, (int)hours));
						}
						else
						{
							m_DisplayMessage.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.licenseExpired());
						}
					}
					else
					{
						m_DisplayMessage.setText(m_MarketingMessages.get(m_Index-1));
					}
				}
				else
				{
					m_DisplayMessage.setText(m_MarketingMessages.get(m_Index));
				}
				
				m_Index++;
				m_Index = (m_Index) % m_NumMessages;
            }
        };
        
	}
	
	
	/**
	 * Sets the marketing messages for display in the banner.
	 * @param marketingMessages <code>MarketingMessages</code> object
	 * 	encapsulating the marketing messages and license expiry time.
	 */
	public void setMessages(MarketingMessages marketingMessages)
	{
		m_UpdateTimer.cancel();
		m_MarketingMessages = new ArrayList<String>();
		m_ExpiryTime = marketingMessages.getExpiryDate();
		
		List<String> messages = marketingMessages.getMessages();
		if (messages != null && messages.size() > 0)
		{
			m_MarketingMessages = messages;
			m_NumMessages = messages.size();
		}
		else
		{
			if (m_ExpiryTime == null)
			{
				// No messages, and no license expiry, set a default message.
				m_MarketingMessages.add(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.technicalSupport());
				m_NumMessages = 1;
			}
		}
		
		if (m_ExpiryTime != null)
		{
			m_NumMessages++;
		}
		
		m_Index = 0;
		
		if (m_NumMessages > 1 || m_ExpiryTime != null)
		{
			m_UpdateTimer.scheduleRepeating(BANNER_REFRESH_FREQUENCY_SECS * 1000);
		}
		
		m_UpdateTimer.run();
	}
}
