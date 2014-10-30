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

package com.prelert.data.gxt.introscope;

import java.util.ArrayList;
import java.util.List;

import com.prelert.data.gxt.AnalysisConfigDataModel;


/**
 * Extension of the AnalysisConfigDataModel class, adding properties specific to
 * the analysis of data from CA APM (Introscope).
 * @author Pete Harverson
 */
public class IntroscopeDataAnalysisModel extends AnalysisConfigDataModel
{
    private static final long serialVersionUID = 6472552702653448816L;
    

 	/**
	 * Returns the list of Introscope agents which are being analysed by Prelert.
	 * @return the list of agents being analysed. If no agents are currently selected
	 * for analysis an empty list is returned.
	 */
	public List<String> getAgents()
	{
		return get("agents", new ArrayList<String>());
	}
	
	
	/**
	 * Sets the list of Introscope agents to be analysed by Prelert.
	 * @param agents the list of agents to analyse.
	 */
	public void setAgents(List<String> agents)
	{
		set("agents", agents);
	}
}
