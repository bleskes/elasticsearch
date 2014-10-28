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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Class encapsulating a list of marketing messages and the expiry
 * time of the product license.
 * @author Pete Harverson
 */
public class MarketingMessages implements Serializable
{
    private static final long serialVersionUID = -4803679384928740421L;
    
	private List<String>	m_Messages;
	private Date			m_ExpiryTime;
	
	
	/**
	 * Returns the list of marketing messages.
	 * @return the list of messages.
	 */
	public List<String> getMessages()
	{
		return m_Messages;
	}
	
	
	/**
	 * Sets the list of marketing messages.
	 * @param messages list of messages.
	 */
	public void setMessages(List<String> messages)
	{
		m_Messages = messages;
	}
	
	
	/**
	 * Returns the license expiry date/time.
	 * @return the expiry time.
	 */
	public Date getExpiryDate()
	{
		return m_ExpiryTime;
	}
	
	
	/**
	 * Sets the license expiry date/time.
	 * @param time the expiry time.
	 */
	public void setExpiryDate(Date time)
	{
		m_ExpiryTime = time;
	}
}
