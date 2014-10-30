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

package com.prelert.data.gxt;

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.prelert.data.Severity;


/**
 * Extension of the GXT BaseModelData class for incident data.
 * @author Pete Harverson
 */
public class IncidentModel extends BaseModelData
{
	private static final long serialVersionUID = 653968770653941610L;
	
	@SuppressWarnings("unused")
	private Severity		m_Severity;		// DO NOT DELETE - custom GWT field serializer.
	
	
	/**
	 * Returns the time of the incident.
     * @return the time of the incident.
     */
    public Date getTime()
    {
    	Date time = get("time");
    	return time;
    }


	/**
	 * Sets the time of this incident.
     * @param time	the time of the incident.
     */
    public void setTime(Date time)
    {
    	set("time", time);
    }


	/**
	 * Returns the description of the incident.
     * @return the m_Description
     */
    public String getDescription()
    {
    	return get("description");
    }


	/**
	 * Sets the description of this incident.
     * @param description description of the incident.
     */
    public void setDescription(String description)
    {
    	set("description", description);
    }


	/**
	 * Returns the anomaly score of the incident, a value between 1 and 100.
	 * This represents the probability of the incident occurring within the time
	 * frame that is being considered.
     * @return the anomaly score, between 1 and 100.
     */
    public int getAnomalyScore()
    {
    	int score = get("score", new Integer(1));
    	return score;
    }


	/**
	 * Sets the anomaly score. This represents the probability of the incident  
	 * occurring within the time frame that is being considered.
     * @param anomalyScore the anomaly score, between 1 and 100.
     */
    public void setAnomalyScore(int anomalyScore)
    {
    	set("score", anomalyScore);
    }


	/**
	 * Returns the ID of the 'headline' item of evidence in the incident.
     * @return the ID of the headline notification or time series feature
     *  in the incident, or -1 if no headline evidence has been set.
     */
    public int getEvidenceId()
    {
    	int evidenceId = get("evidenceId", new Integer(-1));
    	return evidenceId;
    }


	/**
	 * Sets the ID of the 'headline' item of evidence in the incident.
     * @param evidenceId the ID of the headline notification or time series feature
     *  in the incident.
     */
    public void setEvidenceId(int evidenceId)
    {
    	set("evidenceId", evidenceId);
    }
	
	
	/**
	 * Returns a summary of this incident.
	 * @return String representation of this incident.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
