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

/**
 * CavStatus: indicates whether a CAV is currently running/stopped/finished and 
 * the progress if it is running/stopped/finished. 
 */
public class CavStatus implements Serializable 
{
	public enum CavRunState 
	{
		CAV_NOT_STARTED, 
		CAV_RUNNING, 
		CAV_PAUSED, 
		CAV_STOPPED, 
		CAV_FINISHED,
		CAV_ERROR
	};
	
	private static final long serialVersionUID = 3747789540507548566L;
	
	private Date m_DateCavStarted;
	private Date m_FirstCavQueryDate;
	private Date m_CurrentCavQueryDate;
	private Date m_LastCavQueryDate;
	private Date m_TimeOfIncident;
	private Date m_TimeStamp;
	private CavRunState m_CavRunState;
	private float m_ProgressPercent;
	private String m_FatalErrorMessage;
	
	
	public CavStatus()
	{
		m_FatalErrorMessage =  "";
		m_ProgressPercent = 0.0f;
		
		m_CavRunState = CavRunState.CAV_NOT_STARTED;
		
		m_TimeStamp = new Date();
	}
	
	
	/**
	 * Create a new CavStatus object exactly the same as <code>other</code>
	 * but with a new time stamp of now.
	 * @param other
	 */
	public CavStatus(CavStatus other)
	{
		m_DateCavStarted = other.m_DateCavStarted;
		m_FirstCavQueryDate = other.m_FirstCavQueryDate;
		m_CurrentCavQueryDate = other.m_CurrentCavQueryDate;
		m_LastCavQueryDate = other.m_LastCavQueryDate;
		m_TimeOfIncident = other.m_TimeOfIncident;
		m_CavRunState = other.m_CavRunState;
		m_ProgressPercent = other.m_ProgressPercent;
		m_FatalErrorMessage = other.m_FatalErrorMessage;
		
		m_TimeStamp = new Date();
	}
	
	
	/**
	 * Returns the Date the CAV was actually started.
	 * @return the time the CAV was started.
	 */
	public Date getDateCavStarted() 
	{
		return m_DateCavStarted;
	}
	
	
	/**
	 * Sets the date/time the CAV was actually started.
	 * @param dateCavStarted the time the CAV was started.
	 */
	public void setDateCavStarted(Date dateCavStarted) 
	{
		m_DateCavStarted = dateCavStarted;
	}

	/**
	 * Returns the date of the first data collected during the CAV.
	 * @return the date/time of the the first data collected.
	 */
	public Date getFirstCavQueryDate() 
	{
		return m_FirstCavQueryDate;
	}
	
	
	/**
	 * Sets the date/time of the first data queried during the CAV.
	 * @param firstCavQueryDate the date/time of the the first data queried.
	 */
	public void setFirstCavQueryDate(Date firstCavQueryDate) 
	{
		m_FirstCavQueryDate = firstCavQueryDate;
	}
	
	/**
	 * Returns the date of the current query in the CAV. 
	 * This is an indication of the progress of the CAV.
	 * @return the date/time of the data that is currently being queried.
	 */
	public Date getCurrentCavQueryDate() 
	{
		return m_CurrentCavQueryDate;
	}
	
	
	/**
	 * Sets the date of the current query in the CAV.
	 * @param currentCavQueryDate the date/time of the data that is currently 
	 * 	being queried.
	 */
	public void setCurrentCavQueryDate(Date currentCavQueryDate) 
	{
		m_CurrentCavQueryDate = currentCavQueryDate;
	}
	
	
	/**
	 * The date of the final query in the CAV.
	 * The CAV is finished once <code>CurrentCavQueryDate == LastCavQueryDate<code>.
	 * @return the date/time of the the last data queried.
	 */
	public Date getLastCavQueryDate() 
	{
		return m_LastCavQueryDate;
	}
	
	
	/**
	 * Returns the date of the final query in the the CAV.
	 * The CAV is finished once <code>CurrentCavQueryDate == LastCavQueryDate<code>.
	 * @param lastCavQueryDate the date/time of the the last data queried.
	 */
	public void setLastCavQueryDate(Date lastCavQueryDate) 
	{
		m_LastCavQueryDate = lastCavQueryDate;
	}
	
	/**
	 * Returns the time of the actual incident which the
	 * CAV is to analyse. 
	 * The result may be <code>null</code> if a Date hasn't beeen set.
	 * 
	 * @return The Date or <code>null</code>
	 */
	public Date getTimeOfIncident()
	{
		return m_TimeOfIncident;
	}
	
	/**
	 * Sets the time of the CAV incident.
	 * @param timeOfIncident
	 */
	public void setTimeOfIncident(Date timeOfIncident)
	{
		m_TimeOfIncident = timeOfIncident;
	}
	
	/**
	 * Returns the run state of the CAV.
	 * @return <code>CavRunState</code> representing the run state of the CAV.
	 */
	public CavRunState getRunState() 
	{
		return m_CavRunState;
	}

	
	/**
	 * Sets the run state of the CAV.
	 * @param runState <code>CavRunState</code> representing the run state of the CAV.
	 */
	public void setRunState(CavRunState runState) 
	{
		m_CavRunState = runState;
	}

	
	/**
	 * Progress of the CAV as a percentage.
	 * If the CavRunState is not equal to CAV_RUNNING then this value
	 * will not be set.
	 * 
	 * @return the percentage progress of the CAV.
	 */
	public float getProgressPercent()
	{
		return m_ProgressPercent;
	}
	
	
	/**
	 * Sets the progress of the CAV as a percentage.
	 * @param progress the percentage progress of the CAV.
	 */
	public void setProgressPercent(float progress)
	{
		m_ProgressPercent = progress;
	}


	/**
	 * If one of the Prelert processes have had a fatal error this 
	 * returns the associated error message.
	 * 
	 * @return
	 */
	public String getFatalErrorMessage()
	{
		return m_FatalErrorMessage;
	}

	public void setFatalErrorMessage(String errorMessage)
	{
		m_FatalErrorMessage = errorMessage;
	}
	
	
	/**
	 * Return the time this object was created 
	 * i.e. the timestamp of this status record.
	 * 
	 * @return
	 */
	public Date getTimeStamp()
	{
		return m_TimeStamp;
	}
	
	
	
	/**
	 * Returns a String representation of this <code>CavStatus</code>.
	 * @return String showing the properties of this <code>CavStatus</code>.
     */
    @Override
    public String toString()
    {    	
    	StringBuilder strRep = new StringBuilder();
		
		strRep.append("{state=");
		strRep.append(m_CavRunState);
		
		strRep.append(", cavStartedTime=");
		strRep.append(m_DateCavStarted);

		strRep.append(", firstQueryTime=");
		strRep.append(m_FirstCavQueryDate);
		
		strRep.append(", currentQueryTime=");
		strRep.append(m_CurrentCavQueryDate);
		
		strRep.append(", lastQueryTime=");
		strRep.append(m_LastCavQueryDate);
		
		strRep.append(", timeOfIncident=");
		strRep.append(m_TimeOfIncident);
		
		strRep.append(", %progress=");
		strRep.append(m_ProgressPercent);
		
		strRep.append(", FatalErrorMessage=");
		strRep.append(m_FatalErrorMessage);
		
		strRep.append(", TimeStamp=");
		strRep.append(m_TimeStamp);
		
		strRep.append('}');
		
		return strRep.toString();
    }
    	
}
