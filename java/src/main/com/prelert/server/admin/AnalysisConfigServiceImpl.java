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

package com.prelert.server.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.proxy.configuration.ConfigurationProxyDAO;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.ConnectionStatus.Status;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.server.GXTModelConverter;
import com.prelert.service.admin.AnalysisConfigService;


/**
 * Server-side implementation of service used for configuring
 * the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class AnalysisConfigServiceImpl extends RemoteServiceServlet 
	implements AnalysisConfigService
{
	static Logger s_Logger = Logger.getLogger(AnalysisConfigServiceImpl.class);
	
	private ConfigurationProxyDAO	m_ConfigDAO;
	
	
	/**
	 * Sets the data access object to be used for configuring the analysis.
	 * @param configDAO the data access object for configuring the analysis.
	 */
	public void setConfigurationDAO(ConfigurationProxyDAO configDAO)
	{
		m_ConfigDAO = configDAO;
	}
	
	
	/**
	 * Returns the data access object being used for configuring the analysis.
	 * @return the data access object configuring the analysis.
	 */
	public ConfigurationProxyDAO getConfigurationDAO()
	{
		return m_ConfigDAO;
	}
	

    @Override
    public List<DataTypeConfigModel> getTemplateDataTypes()
    {
    	List<DataTypeConfig> dataTypeConfigs = m_ConfigDAO.getTemplateDataTypes();
    	
    	// Convert to a list of GWT DataTypeConfigModels.
    	ArrayList<DataTypeConfigModel> configModels = new ArrayList<DataTypeConfigModel>();
    	
    	DataTypeConfigModel model;
    	for (DataTypeConfig config : dataTypeConfigs)
    	{
    		model = GXTModelConverter.createDataTypeConfigModel(config);
    		configModels.add(model);
    	}
    	
    	s_Logger.debug("getTemplateDataTypes() returning:" + configModels);
    	return configModels;
    }


    @Override
    public DataTypeConfigModel getConfiguredDataType()
    {
    	DataTypeConfigModel configuredType = null;
    	
    	List<DataTypeConfig> dataTypeConfigs = m_ConfigDAO.getConfiguredDataTypes();
    	
    	// May 2012: Just return the first item in the list as the UI 
    	// is currently only set up to allow one data type to be configured.
    	if (dataTypeConfigs != null && dataTypeConfigs.size() > 0)
    	{
    		configuredType = GXTModelConverter.createDataTypeConfigModel(dataTypeConfigs.get(0));
    		
    		// If host and username are set in the connection config, 
        	// validate connection configuration before returning.
    		DataTypeConnectionModel connection = configuredType.getConnectionConfig();
    		String host = connection.getHost();
    		String username = connection.getUsername();
    		if ( (host != null) && (host.isEmpty() == false) && 
    				(username != null) && (username.isEmpty() == false) )
    		{
    			ConnectionStatus connStatus = testConnectionConfig(configuredType);
    			boolean valid = (connStatus.getStatus() == Status.CONNECTION_OK);
    			connection.setValid(valid);
    		}
    	}
    	else
    	{
    		// Return empty model.
    		configuredType = new DataTypeConfigModel();
    	}
    	
    	s_Logger.debug("getConfiguredDataType() returning:" + configuredType);
    	return configuredType;
    }
    

    @Override
    public ConnectionStatus testConnectionConfig(DataTypeConfigModel dataTypeConfig)
    {
    	DataTypeConfig typeConfig = m_ConfigDAO.getTemplateDataType(dataTypeConfig.getDataType());
    	
    	DataTypeConnectionModel connection = dataTypeConfig.getConnectionConfig();	
    	SourceConnectionConfig connectionConfig = new SourceConnectionConfig(connection.getHost(),
    			connection.getPort(), connection.getUsername(), connection.getPassword());
    	typeConfig.setSourceConnectionConfig(connectionConfig);
    	
    	typeConfig.addPluginProperty("DataBaseName", dataTypeConfig.getDatabaseName());
    	
    	// Config will be validated by the plugin.
    	ConnectionStatus status = m_ConfigDAO.testConnection(typeConfig);
    	
    	s_Logger.debug("testConnectionConfig() for " + dataTypeConfig + 
    			", status=" + status);
    	
    	return status;
    }

    
    @Override
    public AnalysisConfigDataModel getDataAnalysisSettings()
    {
    	AnalysisConfigDataModel settings = new AnalysisConfigDataModel();
    	
    	// Obtain the date range and analysis time.
    	CavAvailableDateRange dataDateRange = m_ConfigDAO.getValidDateRange();
    	Date analysisTime = m_ConfigDAO.getCavTimeOfIncident();
    	
    	settings.setValidDataStartTime(dataDateRange.getStart());
    	settings.setValidDataEndTime(dataDateRange.getEnd());
    	settings.setTimeToAnalyze(analysisTime);
    	
    	s_Logger.debug("getDataAnalysisSettings() returning " + settings);

	    return settings;
    }
    

    @Override
    public AnalysisDuration estimateAnalysisDuration(
            DataTypeConfigModel dataTypeConfig, Date timeOfIncident)
    {
    	DataTypeConfig typeConfig = m_ConfigDAO.getTemplateDataType(dataTypeConfig.getDataType());
    	
    	DataTypeConnectionModel connection = dataTypeConfig.getConnectionConfig();	
    	SourceConnectionConfig connectionConfig = new SourceConnectionConfig(connection.getHost(),
    			connection.getPort(), connection.getUsername(), connection.getPassword());
    	typeConfig.setSourceConnectionConfig(connectionConfig);
    	
    	typeConfig.addPluginProperty("DataBaseName", dataTypeConfig.getDatabaseName());
    	
    	AnalysisDuration estimate = m_ConfigDAO.estimateCompletionTime(typeConfig, timeOfIncident);
    	
    	s_Logger.debug("estimateAnalysisDuration() for " + dataTypeConfig + 
    			" at " + timeOfIncident + ", estimate=" + 
    			(estimate.getEstimatedAnalysisDurationMs() / 1000l) + " seconds");
    	
	    return estimate;
    }
}
