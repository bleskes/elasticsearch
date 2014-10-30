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

package com.prelert.service.admin;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.prelert.data.CavStatus;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;


/**
 * Defines the methods for the interface to the service used for controlling
 * the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
@RemoteServiceRelativePath("services/analysisControlService")
public interface AnalysisControlService extends RemoteService
{
	/** Status code indicating operation on user service succeeded. */
	public static final int STATUS_SUCCESS = 0;
	
	/** Status code indicating failure for unknown reason. */
	public static final int STATUS_FAILURE_UNKNOWN = 101;
	
	/** Status code indicating failure in saving configuration. */
	public static final int STATUS_FAILURE_SAVING_CONFIGURATION = 102;
	
	/** Status code indicating failure in starting analysis. */
	public static final int STATUS_FAILURE_STARTING_ANALYSIS = 103;
	
	
	/**
	 * Returns the status of the analysis.
	 * @return <code>CavStatus</code> encapsulating the status of the
	 * 	analysis process.
	 */
	public CavStatus getAnalysisStatus();
	
	
	/**
	 * Starts the analysis process on the back-end using the specified data type
	 * configuration and analysis settings.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> encapsulating the
	 * 	configuration properties for the connection to the data type being analysed.
	 * @param analysisConfig <code>AnalysisConfigDataModel</code> encapsulating the 
	 * 	settings that should be used for the analysis. The following properties
	 * 	must be set in the model:
	 * 	<ul>
	 * 		<li>analysisTime</li>
	 * 		<li>queryLength</li>
	 * 		<li>dataPointInterval</li>
	 *  </ul>
	 * @return a status code, one of the <code>STATUS_XXX</code> values from 
	 * 	{@link com.prelert.service.admin.AnalysisControlService},
	 * 	where zero indicates the analysis was started successfully, and non-zero 
	 * 	indicates the operation failed.
	 */
	public int startAnalysis(DataTypeConfigModel dataTypeConfig, 
			AnalysisConfigDataModel analysisConfig);
	
	
	/**
	 * Cancels the analysis process on the back-end.
	 * @return <code>true</code> if the analysis was cancelled successfully,
	 * 	<code>false</code> if a problem occurred.
	 */
	public boolean cancelAnalysis();

}
