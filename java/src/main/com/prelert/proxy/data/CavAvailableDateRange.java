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

package com.prelert.proxy.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Simple class which holds the valid start and end dates for a CAV.
 * i.e. data can be collected within these dates.
 * 
 */
public class CavAvailableDateRange implements Serializable
{
	private static final long serialVersionUID = -1907534482900603627L;

	private Date m_Start;
	private Date m_End;
	
	/*
	 * For GWT serialisation
	 */
	public CavAvailableDateRange()
	{
		
	}
	
	public CavAvailableDateRange(Date start, Date end)
	{
		m_Start = start;
		m_End = end;
	}
	
	public Date getStart()
	{
		return m_Start;
	}
	
	public Date getEnd()
	{
		return m_End;
	}

}
