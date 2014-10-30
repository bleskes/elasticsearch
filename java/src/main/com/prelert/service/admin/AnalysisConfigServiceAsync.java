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

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface to
 * the service used for configuring the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
public interface AnalysisConfigServiceAsync
{	
	/**
	 * Returns the list of template data types that are available.
	 * @param callback object to receive the list of <code>DataTypeConfigModel</code> 
	 * 	objects encapsulating the properties of the template data types.
	 */
	public void getTemplateDataTypes(AsyncCallback<List<DataTypeConfigModel>> callback);
	
	
	/**
	 * Returns the data type that has been configured for analysis by the UI.
	 * @param callback object to receive the configured data type, or an empty
	 * 	model if no data type has been configured and saved to the server for analysis.
	 */
	public void getConfiguredDataType(AsyncCallback<DataTypeConfigModel> callback);
	
	
	/**
	 * Tests the validity of the connection parameters in the supplied data type
	 * configuration.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> whose connection
	 * 	properties are to be verified.
	 * @param callback object to receive the ConnectionStatus.
	 */
	public void testConnectionConfig(DataTypeConfigModel dataTypeConfig,
			AsyncCallback<ConnectionStatus> callback);
	
	
	/**
	 * Returns the settings (date range and analysis time) that are configured
	 * for the data analysis.
	 * @param callback object to receive the <code>AnalysisConfigDataModel</code> settings.
	 */
	public void getDataAnalysisSettings(AsyncCallback<AnalysisConfigDataModel> callback);
	
	
	/**
	 * Estimates the length of time it will take to run an analysis using the 
	 * specified settings.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> encapsulating the 
	 * 	configuration properties of the data type to be analysed.
	 * @param timeOfIncident time of incident to be analysed.
	 * @param callback callback to receive the analysis duration.
	 */
	public void estimateAnalysisDuration(DataTypeConfigModel dataTypeConfig,
			Date timeOfIncident, AsyncCallback<AnalysisDuration> callback);
}
