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
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.prelert.client.introscope.AgentPagingLoadConfig;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.data.gxt.introscope.IntroscopeDataAnalysisModel;


/**
 * Defines the methods for the interface to the configuration service for the 
 * CA APM (Introscope) product.
 * @author Pete Harverson
 */
@RemoteServiceRelativePath("services/introscopeConfigService")
public interface IntroscopeConfigService extends RemoteService
{	
	/**
	 * Returns the Introscope template data type configuration.
	 * @return the template data type, or <code>null</code> if no Introscope 
	 * 	template file is found on the server.
	 */
	public DataTypeConfigModel getTemplateDataType();
	
	/**
	 * Returns the Introscope data type configuration. If this has yet to be
	 * configured and saved to the server, the template data type will be returned.
	 * @return the configured data type including the Introscope connection parameters,
	 * 	or the template data type if a connection has yet to be configured. Returns
	 * 	<code>null</code> if a type has yet to be configured, and no Introscope 
	 * 	template file is found on the server.
	 */
	public DataTypeConfigModel getConfiguredDataType();
	
	
	/**
	 * Tests the validity of the configuration parameters for the connection to
	 * Introscope.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating
	 * 	the	configuration properties of the connection to Introscope.
	 * @return CONNECTION_FAILED if no connection can be made, MISSING_HEALTH_METRICS 
	 * 	if the Enterprise Managers health metrics cannot be read else CONNECTION_OK.
	 */
	public ConnectionStatus testConnectionConfig(DataTypeConnectionModel connectionModel);
	
	
	/**
	 * Returns a page from the list of all agents that are available for analysis
	 * within Introscope.
	 * @param pagingConfig paging load configuration, specifying the page number, size,
	 * 	and an optional String to search for within the agent name so that only agents 
	 * 	whose name contains the supplied String will be returned (case-insensitive match).
	 * @return a page of agents matching the specified load criteria.
	 */
	public PagingLoadResult<AttributeModel> listAgents(AgentPagingLoadConfig pagingConfig);
	
	
	/**
	 * Returns the settings (date range, analysis time, agents) that are configured
	 * for the data analysis.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
	 * @return <code>IntroscopeDataAnalysisModel</code> encapsulating the settings of
	 * 	the data for analysis.
	 */
	public IntroscopeDataAnalysisModel getDataAnalysisSettings(DataTypeConnectionModel connectionModel);
	
	
	/**
	 * Estimates the length of time it will take to run an analysis using the 
	 * specified settings.
	 * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
	 * @param settings <code>IntroscopeDataAnalysisModel</code> encapsulating the 
	 * 	settings that should be used for the estimate. Only the analysis time and 
	 * 	list agents should be set.
	 * @return an object encapsulating the estimated time and the optimal query
	 *  options.
	 */
	public AnalysisDuration estimateAnalysisDuration(DataTypeConnectionModel connectionModel,
								IntroscopeDataAnalysisModel DataTypeConnectionModel);
	
	
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
	 * 		<li>agents</li>
	 *  </ul>
	 * @return a status code, one of the <code>STATUS_XXX</code> values from 
	 * 	{@link com.prelert.service.admin.AnalysisControlService},
	 * 	where zero indicates the analysis was started successfully, and non-zero 
	 * 	indicates the operation failed.
	 */
	public int startAnalysis(DataTypeConfigModel dataTypeConfig, 
			AnalysisConfigDataModel analysisConfig);
}
