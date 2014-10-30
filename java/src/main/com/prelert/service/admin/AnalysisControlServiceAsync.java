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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.CavStatus;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface to
 * the service used for controlling the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
public interface AnalysisControlServiceAsync
{
	/**
	 * Returns the status of the analysis.
	 * @param callback object to receive the status of the analysis process from the
	 * 	remote procedure call.
	 */
	public void getAnalysisStatus(AsyncCallback<CavStatus> callback);
	
	
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
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void startAnalysis(DataTypeConfigModel dataTypeConfig, 
			AnalysisConfigDataModel analysisConfig, AsyncCallback<Integer> callback);
	
	
	/**
	 * Cancels the analysis process on the back-end.
	 * @param callback callback object to receive the flag indicating whether the
	 * 		analysis was cancelled successfully from the remote procedure call.
	 */
	public void cancelAnalysis(AsyncCallback<Boolean> callback);
	
}
