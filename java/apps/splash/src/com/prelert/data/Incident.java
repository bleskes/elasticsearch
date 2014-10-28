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
import java.util.Date;

/**
 * Class encapsulating an incident.
 * @author Pete Harverson
 */
public class Incident implements Serializable
{
	private Date 		m_Time;
	private String 		m_Description;
	private Severity	m_Severity;
	private int			m_AnomalyScore;
	private int			m_EvidenceId;
	
	
	/**
	 * Creates a new, blank incident.
	 */
	public Incident()
	{
		m_Severity = Severity.NONE;
	}


	/**
	 * Returns the time of the incident.
     * @return the time of the incident.
     */
    public Date getTime()
    {
    	return m_Time;
    }


	/**
	 * Sets the time of this incident.
     * @param time	the time of the incident.
     */
    public void setTime(Date time)
    {
    	m_Time = time;
    }


	/**
	 * Returns the description of the incident.
     * @return the m_Description
     */
    public String getDescription()
    {
    	return m_Description;
    }


	/**
	 * Sets the description of this incident.
     * @param description description of the incident.
     */
    public void setDescription(String description)
    {
    	m_Description = description;
    }


	/**
	 * Returns the severity of the incident, which equates to the highest severity
	 * notification or time series feature in the incident.
     * @return the severity of the incident, such as 'minor', 'major' or 'critical'.
     */
    public Severity getSeverity()
    {
    	return m_Severity;
    }


    /**
	 * Sets the severity of the incident, which should equate to the highest severity
	 * notification or time series feature in the incident.
	 * @param severity the incident severity such as 'minor', 'major' or 'critical'.
	 */
    public void setSeverity(Severity severity)
    {
    	m_Severity = severity;
    }


	/**
	 * Returns the anomaly score of the incident, a value between 1 and 100.
	 * This represents the probability of the incident occurring within the time
	 * frame that is being considered.
     * @return the anomaly score, between 1 and 100.
     */
    public int getAnomalyScore()
    {
    	return m_AnomalyScore;
    }


	/**
	 * Sets the anomaly score. This represents the probability of the incident  
	 * occurring within the time frame that is being considered.
     * @param anomalyScore the anomaly score, between 1 and 100.
     */
    public void setAnomalyScore(int anomalyScore)
    {
    	m_AnomalyScore = anomalyScore;
    }


	/**
	 * Returns the ID of an item of evidence in the incident.
     * @return the ID of an item of evidence or time series feature in the incident.
     */
    public int getEvidenceId()
    {
    	return m_EvidenceId;
    }


	/**
	 * Sets the ID of an item of evidence in the incident.
     * @param evidenceId the ID of an item of evidence or time series feature
     *  in the incident.
     */
    public void setEvidenceId(int evidenceId)
    {
    	m_EvidenceId = evidenceId;
    }
	
	
    /**
	 * Returns a summary of this incident.
	 * @return String representation of this incident.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		
		strRep.append("time=");
		strRep.append(m_Time);
		
		strRep.append(", description=");
		strRep.append(m_Description);
		
		strRep.append(", severity=");
		strRep.append(m_Severity);

		strRep.append(", anomaly_score=");
		strRep.append(m_AnomalyScore);
		
		strRep.append(", evidenceId=");
		strRep.append(m_EvidenceId);
		
		strRep.append('}');
		
		return strRep.toString();
	}
}
