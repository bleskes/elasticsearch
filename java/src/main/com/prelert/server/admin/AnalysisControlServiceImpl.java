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

import java.util.Arrays;
import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.proxy.ProxyDataAccessException;
import com.prelert.dao.proxy.configuration.ConfigurationProxyDAO;
import com.prelert.dao.proxy.control.ProxyControlDAO;
import com.prelert.data.CavStatus;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.service.admin.AnalysisControlService;


/**
 * Server-side implementation of service used for controlling
 * the analysis of data by the Prelert engine.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class AnalysisControlServiceImpl extends RemoteServiceServlet 
	implements AnalysisControlService
{
	static Logger s_Logger = Logger.getLogger(AnalysisControlServiceImpl.class);
	
	private ConfigurationProxyDAO	m_ConfigDAO;
	private ProxyControlDAO			m_ControlDAO;
	
	
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
	
	
	/**
	 * Sets the data access object to be used for controlling the analysis.
	 * @param controlDAO the data access object for controlling the analysis.
	 */
	public void setControlDAO(ProxyControlDAO controlDAO)
	{
		m_ControlDAO = controlDAO;
	}
	
	
	/**
	 * Returns the data access object being used for controlling the analysis.
	 * @return the data access object controlling the analysis.
	 */
	public ProxyControlDAO getControlDAO()
	{
		return m_ControlDAO;
	}
	
	
	@Override
    public CavStatus getAnalysisStatus()
    {
	    CavStatus statusData = m_ConfigDAO.getCavStatus();
	    s_Logger.debug("getAnalysisStatus() returning " + statusData);
	    
	    return statusData;
    }


    @Override
    public int startAnalysis(DataTypeConfigModel dataTypeConfigModel,
            AnalysisConfigDataModel analysisConfig)
    {
    	int status = AnalysisControlService.STATUS_SUCCESS;

		// Get the template for this data type.
    	String dataType = dataTypeConfigModel.getDataType();
    	DataTypeConfig dataTypeConfig = m_ConfigDAO.getTemplateDataType(dataType);
    	
    	// Set the connection configuration.
    	DataTypeConnectionModel connection = dataTypeConfigModel.getConnectionConfig();
    	SourceConnectionConfig connectionConfig = new SourceConnectionConfig(connection.getHost(),
    			connection.getPort(), connection.getUsername(), connection.getPassword());
    	dataTypeConfig.setSourceConnectionConfig(connectionConfig);
    	
    	// Set InputManager properties (query length).
    	InputManagerConfig imConfig = dataTypeConfig.getInputManagerConfig();
    	imConfig.setQueryLengthSecs(analysisConfig.getQueryLength());
    	
    	// Set plugin properties (Database name, data point interval).
    	dataTypeConfig.addPluginProperty("DataBaseName", dataTypeConfigModel.getDatabaseName());
    	dataTypeConfig.addPluginProperty("Interval", "" + analysisConfig.getDataPointInterval());
    	
    	// Save the configuration.
    	boolean success = m_ConfigDAO.addDataType(dataTypeConfig);
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
    	
	    return status;
    }


	@Override
	public boolean cancelAnalysis()
	{
		boolean cancelled = true;
		
		try
		{
			cancelled = m_ControlDAO.stopCav();
			s_Logger.debug("cancelAnalysis() - cancelled:" + cancelled);
		}
		catch (ProxyDataAccessException e)
		{
			s_Logger.error("cancelAnalysis() error cancelling analysis", e);
			cancelled = false;
		}
	    
	    return cancelled;
	}
}
