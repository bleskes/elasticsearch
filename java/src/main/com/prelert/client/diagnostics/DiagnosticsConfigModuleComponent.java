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

package com.prelert.client.diagnostics;

import com.prelert.client.gxt.ModuleComponent;
import com.prelert.data.CavStatus;


/**
 * 
 * @author Pete Harverson
 */
public interface DiagnosticsConfigModuleComponent extends ModuleComponent
{
	/** Module ID to use for the configuration module in a diagnostics UI. */
	public static final String MODULE_ID = "diagnostics_config";
	
	/**
	 * Returns the current status of the analysis.
	 * @return the current status of the analysis.
	 */
	public CavStatus getAnalysisStatus();
	
	
	/**
	 * Sets the current status of the analysis, enabling or disabling the various
	 * form controls in the module according to the run state.
	 * @param status the current status of the analysis.
	 */
	public void setAnalysisStatus(CavStatus status);
	
	
	/**
	 * Resets the various form controls in the module in readiness for a new
	 * analysis to be configured.
	 */
	public void resetForNewAnalysis();
	
	
	/**
	 * Cancels the analysis as configured in the module.
	 */
	public void cancelAnalysis();
}
