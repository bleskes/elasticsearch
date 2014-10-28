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

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;


/**
 * Defines the methods for the interface to the service used for configuring
 * the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
@RemoteServiceRelativePath("services/analysisConfigService")
public interface AnalysisConfigService extends RemoteService
{
	/**
	 * Returns the list of template data types that are available for analysis.
	 * @return list of <code>DataTypeConfigModel</code> objects encapsulating
	 * 	the properties of the template data types.
	 */
	public List<DataTypeConfigModel> getTemplateDataTypes();
	
	
	/**
	 * Returns the data type that has been configured for analysis by the UI.
	 * @return the configured data type, or an empty model if no data type
	 * 	has been configured and saved to the server for analysis.
	 */
	public DataTypeConfigModel getConfiguredDataType();
	
	
	/**
	 * Tests the validity of the connection parameters in the supplied data type
	 * configuration.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> whose connection
	 * 	properties are to be verified.
	 * @return CONNECTION_OK if the properties represent a valid connection, or
	 * 	CONNECTION_FAILED if no connection can be made.
	 */
	public ConnectionStatus testConnectionConfig(DataTypeConfigModel dataTypeConfig);
	
	
	/**
	 * Returns the settings (date range and analysis time) that are configured
	 * for the data analysis.
	 * @return <code>AnalysisConfigDataModel</code> encapsulating the settings of
	 * 	the data for analysis.
	 */
	public AnalysisConfigDataModel getDataAnalysisSettings();
	
	
	/**
	 * Estimates the length of time it will take to run an analysis using the 
	 * specified settings.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> encapsulating the 
	 * 	configuration properties of the data type to be analysed.
	 * @param timeOfIncident time of incident to be analysed.
	 * @return an object encapsulating the estimated time and the optimal query
	 *  options.
	 */
	public AnalysisDuration estimateAnalysisDuration(DataTypeConfigModel dataTypeConfig,
								Date timeOfIncident);
}
