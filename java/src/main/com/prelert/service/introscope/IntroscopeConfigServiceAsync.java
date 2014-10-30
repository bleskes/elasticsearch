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

package com.prelert.service.introscope;

import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.client.introscope.AgentPagingLoadConfig;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.data.gxt.introscope.IntroscopeDataAnalysisModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the configuration service for the CA APM (Introscope) product.
 * @author Pete Harverson
 */
public interface IntroscopeConfigServiceAsync
{
	/**
	 * Returns the Introscope template data type configuration.
	 * @param callback callback object to receive the data type from the remote procedure call. 
	 * 	The callback returns <code>null</code> if no Introscope template file is 
	 * 	found on the server.
	 */
	public void getTemplateDataType(AsyncCallback<DataTypeConfigModel> callback);
	
	
	/**
	 * Returns the Introscope data type configuration. If this has yet to be
	 * configured and saved to the server, the template data type will be returned.
	 * @param callback callback object to receive the data type from the remote procedure call. 
	 * 	The callback returns <code>null</code> if a type has yet to be configured, 
	 * 	and no Introscope template file is found on the server.
	 */
	public void getConfiguredDataType(AsyncCallback<DataTypeConfigModel> callback);
	
	
	/**
	 * Tests the validity of the configuration parameters for the connection to
	 * Introscope.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
	 * @param callback callback object to receive the response from the remote procedure call.
	 */
	public void testConnectionConfig(DataTypeConnectionModel connectionModel,
			AsyncCallback<ConnectionStatus> callback);
	
	
	/**
	 * Returns a page from the list of all agents that are available for analysis
	 * within Introscope.
	 * @param pagingConfig paging load configuration, specifying the page number, size,
	 * 	and an optional String to search for within the agent name so that only agents 
	 * 	whose name contains the supplied String will be returned (case-insensitive match).
	 * @param callback callback object to receive the page of agents from the
	 * 	remote procedure call.
	 */
	public void listAgents(AgentPagingLoadConfig pagingConfig, 
			AsyncCallback<PagingLoadResult<AttributeModel>> callback);
	
	
	/**
	 * Returns the settings (date range, analysis time, agents) that are configured
	 * for the data analysis.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
	 * @param callback callback object to receive the data analysis settings from
	 * 	the remote procedure call.
	 */
	public void getDataAnalysisSettings(DataTypeConnectionModel connectionModel,
			AsyncCallback<IntroscopeDataAnalysisModel> callback);
	
	
	/**
	 * Estimates the length of time it will take to run an analysis using the 
	 * specified settings.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
	 * @param settings <code>IntroscopeDataAnalysisModel</code> encapsulating the 
	 * 	settings that should be used for the estimate. Only the analysis time and 
	 * 	list agents should be set.
	 * @param callback callback object to receive the length of time (in milliseconds)
	 * 	from the remote procedure call.
	 */
	public void estimateAnalysisDuration(DataTypeConnectionModel connectionModel,
			IntroscopeDataAnalysisModel settings, AsyncCallback<AnalysisDuration> callback);
	
	
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

}
