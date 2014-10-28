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

package com.prelert.server.introscope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.client.introscope.AgentPagingLoadConfig;
import com.prelert.dao.proxy.ProxyDataAccessException;
import com.prelert.dao.proxy.configuration.ConfigurationProxyDAO;
import com.prelert.dao.proxy.configuration.IntroscopeConfigDAO;
import com.prelert.dao.proxy.control.ProxyControlDAO;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.data.gxt.introscope.IntroscopeDataAnalysisModel;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.server.GXTModelConverter;
import com.prelert.server.ServerUtil;
import com.prelert.service.admin.AnalysisControlService;
import com.prelert.service.introscope.IntroscopeConfigService;


/**
 * Server-side implementation of the configuration service for the 
 * CA APM (Introscope) product.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class IntroscopeConfigServiceImpl extends RemoteServiceServlet 
	implements IntroscopeConfigService
{
	static Logger s_Logger = Logger.getLogger(IntroscopeConfigServiceImpl.class);
	
	private ConfigurationProxyDAO	m_ConfigDAO;
	private IntroscopeConfigDAO		m_IntroscopeConfigDAO;
	private ProxyControlDAO			m_ControlDAO;
	
	public static final String INTROSCOPE_DATA_TYPE_NAME = "Introscope";


	/**
	 * Sets the data access object to used for configuring the Introscope plugin.
	 * @param configDAO data access object for configuring the Introscope connection.
	 */
	public void setConfigurationDAO(IntroscopeConfigDAO configDAO)
	{
		m_IntroscopeConfigDAO = configDAO;
	}
	
	
	/**
	 * Returns the data access object being used for configuring the Introscope plugin.
	 * @return the data access object for configuring the Introscope connection.
	 */
	public IntroscopeConfigDAO getConfigurationDAO()
	{
		return m_IntroscopeConfigDAO;
	}
	
	
	/**
	 * Sets the data access object to be used for configuring the Introscope CAV.
	 * @param cavControlDAO the data access object for configuring the CAV.
	 */
	public void setCavConfigurationDAO(ConfigurationProxyDAO cavControlDAO)
	{
		m_ConfigDAO = cavControlDAO;
	}
	
	
	/**
	 * Returns the data access object being used for configuring the Introscope CAV.
	 * @return the data access object configuring the CAV.
	 */
	public ConfigurationProxyDAO getCavConfigurationDAO()
	{
		return m_ConfigDAO;
	}
	
	
	/**
	 * Sets the data access object to be used for controlling the Introscope CAV.
	 * @param controlDAO the data access object for controlling the CAV.
	 */
	public void setCavControlDAO(ProxyControlDAO controlDAO)
	{
		m_ControlDAO = controlDAO;
	}
	
	
	/**
	 * Returns the data access object being used for controlling the Introscope CAV.
	 * @return the data access object controlling the CAV.
	 */
	public ProxyControlDAO getCavControlDAO()
	{
		return m_ControlDAO;
	}
	
	
    @Override
    public DataTypeConfigModel getTemplateDataType()
    {
    	DataTypeConfigModel dataTypeConfigModel = null;
		
		DataTypeConfig dataTypeConfig = 
			m_ConfigDAO.getTemplateDataType(INTROSCOPE_DATA_TYPE_NAME);
		
		if (dataTypeConfig != null)
		{
			dataTypeConfigModel = GXTModelConverter.createDataTypeConfigModel(dataTypeConfig);
			s_Logger.debug("getTemplateDataType() returning: " + dataTypeConfigModel);
		}
		
		return dataTypeConfigModel;
    }


	@Override
	public DataTypeConfigModel getConfiguredDataType()
	{
		DataTypeConfigModel dataTypeConfigModel = null;
		
		// Look to see if an Introscope config has already been added.
		DataTypeConfig dataTypeConfig = 
			m_ConfigDAO.getConfiguredDataType(INTROSCOPE_DATA_TYPE_NAME);
		if (dataTypeConfig == null)
		{
			// Obtain the template.
			dataTypeConfig = m_ConfigDAO.getTemplateDataType(INTROSCOPE_DATA_TYPE_NAME);
		}
		
		if (dataTypeConfig != null)
		{
			dataTypeConfigModel = GXTModelConverter.createDataTypeConfigModel(dataTypeConfig);
			
			SourceConnectionConfig connection = dataTypeConfig.getSourceConnectionConfig();
			if (connection != null)
			{
				String host = connection.getHost();
				String username = connection.getUsername();
				
				// If host and username are set, validate connection configuration before returning.
				if ( (host != null) && (host.isEmpty() == false) && 
						(username != null) && (username.isEmpty() == false) )
				{
					boolean valid = m_IntroscopeConfigDAO.testConnection(connection).getStatus() == 
						ConnectionStatus.Status.CONNECTION_OK;
					s_Logger.debug("getConfiguredDataType() testConnection valid=" + valid);
					dataTypeConfigModel.getConnectionConfig().setValid(valid);
				}
			}
			
			s_Logger.debug("getConfiguredDataType() returning: " + dataTypeConfigModel);
		}
		
		return dataTypeConfigModel;
	}
	

    @Override
    public ConnectionStatus testConnectionConfig(DataTypeConnectionModel connectionModel)
    {
    	SourceConnectionConfig config = GXTModelConverter.getConnectionConfig(connectionModel);
    	
    	// Config will be validated by the plugin.
    	ConnectionStatus status = m_IntroscopeConfigDAO.testConnection(config);
    	
    	s_Logger.debug("testConnectionConfig() for " + config + 
    			", status=" + status);
    	
    	return status;
    }


	@Override
    public PagingLoadResult<AttributeModel> listAgents(AgentPagingLoadConfig pagingConfig)
    {
		DataTypeConnectionModel connectionModel = pagingConfig.getConnectionConfig();
    	SourceConnectionConfig config = GXTModelConverter.getConnectionConfig(connectionModel);
    	
    	List<String> allAgents = m_IntroscopeConfigDAO.listAgents(null, config);
	    
	    // If a Search String is specified, filter the list.
	    ArrayList<String> filteredList = new ArrayList<String>();
		if (allAgents != null)
		{
			String containsText = pagingConfig.getContainsText();
			if (containsText != null && containsText.isEmpty() == false)
			{
				for (String agentName : allAgents)
				{
					if (StringUtils.containsIgnoreCase(agentName, containsText))
					{
						filteredList.add(agentName);
					}
				}
			}
			else
			{
				filteredList.addAll(allAgents);
			}
		}
    	
    	// Sort by agent name.
		Collections.sort(filteredList, new Comparator<String>(){

			@Override
            public int compare(String agentName1, String agentName2)
            {
	            return agentName1.compareToIgnoreCase(agentName2);
            }
			
		});
		
		// Return specified page of agents.
		int offset = pagingConfig.getOffset();
		int pageSize = pagingConfig.getLimit();
		
		List<AttributeModel> sublist = new ArrayList<AttributeModel>();
		int limit = filteredList.size();
		if (pageSize > 0)
		{
			limit = Math.min(offset + pageSize, limit);
		}
		for (int i = offset; i < limit; i++)
		{
			sublist.add(new AttributeModel("agentName", filteredList.get(i)));
		}
    	
	    return new BasePagingLoadResult<AttributeModel>(sublist,  offset, filteredList.size());
    }

    
    @Override
    public IntroscopeDataAnalysisModel getDataAnalysisSettings(DataTypeConnectionModel connectionModel)
    {
    	IntroscopeDataAnalysisModel settings = new IntroscopeDataAnalysisModel();
    	
    	SourceConnectionConfig config = GXTModelConverter.getConnectionConfig(connectionModel);
    	
    	// Obtain the date range, analysis time, and agents from the plugin.
    	CavAvailableDateRange dataDateRange = m_IntroscopeConfigDAO.getCavDateRange();
    	Date analysisTime = m_ConfigDAO.getCavTimeOfIncident();
    	
    	// Note that if the connection params don't match the current configured
    	// Introscope datatype then the proxy should return an empty list.
    	List<String> agents = m_IntroscopeConfigDAO.getAgents(config);
    	
    	settings.setValidDataStartTime(dataDateRange.getStart());
    	settings.setValidDataEndTime(dataDateRange.getEnd());
    	settings.setTimeToAnalyze(analysisTime);
    	settings.setAgents(agents);
    	
    	s_Logger.debug("getDataAnalysisSettings() returning " + settings);

	    return settings;
    }
    

    @Override
    public AnalysisDuration estimateAnalysisDuration(DataTypeConnectionModel connectionModel,
    		IntroscopeDataAnalysisModel settings)
    {
    	s_Logger.debug("estimateAnalysisDuration() for config: " + connectionModel +
    			", settings: " + settings);
    	
    	SourceConnectionConfig config = GXTModelConverter.getConnectionConfig(connectionModel);
    	
    	List<String> agents = settings.getAgents();
    	AnalysisDuration estimate = m_IntroscopeConfigDAO.estimateCavDuration(
    			agents, settings.getTimeToAnalyze(), config);
    	
    	s_Logger.debug("estimateAnalysisDuration() for " + agents.size() + 
    			" agents, estimate=" + (estimate.getEstimatedAnalysisDurationMs() / 1000l) + " seconds");
    	
	    return estimate;
    }
    

    @Override
    public int startAnalysis(DataTypeConfigModel dataTypeConfigModel,
    		AnalysisConfigDataModel analysisConfig)
    {
    	// Call resetConfiguration() on IntroscopeConfigDAO as it contains some
    	// specific calls for the canned demo.
    	int status = AnalysisControlService.STATUS_SUCCESS;
    	boolean success = m_IntroscopeConfigDAO.resetConfiguration();
    	s_Logger.debug("startAnalysis() reset configuration:" + success);
    	
    	if (success == true)
    	{
    		// Get the template for this data type.
        	String dataType = dataTypeConfigModel.getDataType();
        	DataTypeConfig dataTypeConfig = m_ConfigDAO.getTemplateDataType(dataType);
        	
        	// Get the connection configuration.
        	DataTypeConnectionModel connectionModel = dataTypeConfigModel.getConnectionConfig();
        	SourceConnectionConfig connectionConfig = GXTModelConverter.getConnectionConfig(connectionModel);
        	dataTypeConfig.setSourceConnectionConfig(connectionConfig);
        	
        	// Set InputManager properties (query length).
        	InputManagerConfig imConfig = dataTypeConfig.getInputManagerConfig();
        	imConfig.setQueryLengthSecs(analysisConfig.getQueryLength());
        	
        	// Set Introscope plugin properties (data point interval, agents).
        	dataTypeConfig.addPluginProperty("Interval", "" + analysisConfig.getDataPointInterval());
        	
        	IntroscopeDataAnalysisModel introscopeAnalysisConfig = 
        		(IntroscopeDataAnalysisModel)analysisConfig;
        	String separator = ServerUtil.REGEX_SAFE_DELIMITER;
        	List<String> agents = introscopeAnalysisConfig.getAgents();
        	String agentsStr = StringUtils.join(agents, separator);
        	dataTypeConfig.addPluginProperty("Agents", agentsStr);
        	dataTypeConfig.addPluginProperty("AgentSeparator", separator);
        	
        	// Save the configuration.
        	success = m_ConfigDAO.addDataType(dataTypeConfig);
        	s_Logger.debug("startAnalysis() data type added:" + success + 
        			", for config: " + dataTypeConfig);
        	if (success == false)
    		{
    			status = AnalysisControlService.STATUS_FAILURE_SAVING_CONFIGURATION;
    		}
        	
        	// Start the analysis.
        	if (success == true)
        	{
        		try
        		{
	        		success = m_ControlDAO.startCav(analysisConfig.getTimeToAnalyze(), 
	        				Arrays.asList(dataTypeConfig));
	        		s_Logger.debug("startAnalysis() analysis started:" + success);
	        		if (success == false)
	        		{
	        			status = AnalysisControlService.STATUS_FAILURE_STARTING_ANALYSIS;
	        		}
        		}
        		catch (ProxyDataAccessException e)
        		{
        			s_Logger.error("startAnalysis() error starting analysis", e);
        			status = AnalysisControlService.STATUS_FAILURE_STARTING_ANALYSIS;
        		}
        	}
    	}
    	else
    	{
    		status = AnalysisControlService.STATUS_FAILURE_UNKNOWN;
    	}
    	
	    return status;
    }
}
