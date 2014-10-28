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

/**
 * Class encapsulating the configuration data for an alerter, such as a script to run
 * or the sending of an email.
 * @author Pete Harverson
 */
public class Alerter implements Serializable
{
	private static final long serialVersionUID = -8727929632505729287L;

	private String 		m_Type;
	private boolean		m_IsEnabled;
	private int 		m_Threshold = THRESHOLD_DISABLED;
	private String 		m_ScriptName;
	
	public static final String TYPE_SCRIPT = "script";
	public static final int THRESHOLD_DISABLED = 101;


	/**
	 * Returns the type of the alerter, such as 'script' or 'email'.
	 * @return the type of the alerter.
	 */
	public String getType()
	{
		return m_Type;
	}


	/**
	 * Sets the type of the alerter, such as 'script' or 'email'.
	 * @param type the type of the alerter.
	 */
	public void setType(String type)
	{
		m_Type = type;
	}


	/**
	 * Returns the value of the activity anomaly score at which alerts will be generated.
	 * @return the activity anomaly score threshold. Alerts will be generated for
	 * 	activities whose anomaly score is greater than or equal to this value.
	 */
	public int getThreshold()
	{
		return m_Threshold;
	}


	/**
	 * Sets the value of the activity anomaly score at which alerts will be generated.
	 * @param threshold the activity anomaly score threshold. Alerts will be generated for
	 * 	activities whose anomaly score is greater than or equal to this value.
	 */
	public void setThreshold(int threshold)
	{
		m_Threshold = threshold;
	}


	/**
	 * For <code>script</code> type alerts, returns the name of the script that will be run
	 * when an alert is triggered.
	 * @return the script file name. The file must be stored in the 
	 * 	<code>$PRELERT_HOME/config/alertscripts</code> directory on the Prelert server.
	 */
	public String getScriptName()
	{
		return m_ScriptName;
	}


	/**
	 * For <code>script</code> type alerts, sets the name of the script that will be run
	 * when an alert is triggered.
	 * @param scriptName the script file name. The file must be stored in the 
	 * 	<code>$PRELERT_HOME/config/alertscripts</code> directory on the Prelert server.
	 */
	public void setScriptName(String scriptName)
	{
		m_ScriptName = scriptName;
	}
	
	
	/**
	 * Returns whether the Alerter is enabled.
	 * @return <code>true</code> if the Alerter is enabled and alerts will be fired
	 * 	according to the configured threshold, <code>false</code> otherwise (the default).
	 */
	public boolean isEnabled()
	{
		return m_IsEnabled;
	}
	
	
	/**
	 * Sets whether the Alerter is enabled.
	 * @param enabled <code>true</code> to enabled the Alerter and fire alerts
	 * 	according to the configured threshold, <code>false</code> to disable.
	 */
	public void setEnabled(boolean enabled)
	{
		m_IsEnabled = enabled;
	}
	
	
	/**
	 * Returns a String representation of this alerter.
	 * @return String representation of the alerter.
	 */
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append("{type=");
		strRep.append(m_Type);
		strRep.append(", enabled=");
		strRep.append(m_IsEnabled);
		strRep.append(", threshold=");
		strRep.append(m_Threshold);
		if (m_ScriptName != null)
		{
			strRep.append(", script=");
			strRep.append(m_ScriptName);
		}
		strRep.append('}');
		
		return strRep.toString();
	}

}
