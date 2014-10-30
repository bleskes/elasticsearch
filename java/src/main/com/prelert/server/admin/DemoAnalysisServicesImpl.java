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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.CavStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.ConnectionStatus.Status;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.service.admin.AnalysisConfigService;
import com.prelert.service.admin.AnalysisControlService;


/**
 * Implementation of the <code>AnalysisConfigService</code> and 
 * <code>AnalysisControlService</code> for use by the demo UI. It returns a set
 * of hardcoded values for a connection to a dummy SQL data source. The control and
 * status methods all return hardcoded values indicating success or valid configurations.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class DemoAnalysisServicesImpl extends RemoteServiceServlet
	implements AnalysisConfigService, AnalysisControlService
{
	static Logger s_Logger = Logger.getLogger(DemoAnalysisServicesImpl.class);
	
	private String 	m_DataType = "SQL Database";
	private String 	m_DatabaseName = "prelert";
	private String 	m_Host = "db-server-1";
	private int 	m_Port = 5432;
	private String	m_Username = "admin";
	private Date	m_DateToAnalyze;
	

	@Override
	public List<DataTypeConfigModel> getTemplateDataTypes()
	{
		// Return a single hardcoded dummy data type.
		DataTypeConfigModel dummyType = getConfiguredDataType();
		return Collections.singletonList(dummyType);
	}
	

	@Override
	public DataTypeConfigModel getConfiguredDataType()
	{
		// Return dummy data type using values optionally overridden in client.properties.
		DataTypeConfigModel dummyConfig = new DataTypeConfigModel();
		dummyConfig.setDataType(m_DataType);
		dummyConfig.setDatabaseName(m_DatabaseName);
		
		DataTypeConnectionModel connectionConfig = new DataTypeConnectionModel();
		connectionConfig.setHost(m_Host);
		connectionConfig.setPort(m_Port);
		connectionConfig.setUsername(m_Username);
		connectionConfig.setValid(true);
		dummyConfig.setConnectionConfig(connectionConfig);
		
		return dummyConfig;
	}

	
	@Override
	public ConnectionStatus testConnectionConfig(
			DataTypeConfigModel dataTypeConfig)
	{
		return new ConnectionStatus(Status.CONNECTION_OK);
	}

	
	@Override
	public AnalysisConfigDataModel getDataAnalysisSettings()
	{
		AnalysisConfigDataModel settings = new AnalysisConfigDataModel();
		
		// TODO Set to date of SAP demo analysis.
		Locale requestLocale = getThreadLocalRequest().getLocale();
		GregorianCalendar activityCal = new GregorianCalendar(requestLocale);
		if (m_DateToAnalyze != null)
		{
			activityCal.setTime(m_DateToAnalyze);
		}
		else
		{
			activityCal.set(2012, 1, 29);
		}
		
		settings.setTimeToAnalyze(activityCal.getTime());
		
		// Set valid analysis time span 1 week either side.
		activityCal.add(Calendar.DAY_OF_YEAR, -7);
		settings.setValidDataStartTime(activityCal.getTime());
		activityCal.add(Calendar.DAY_OF_YEAR, 14);
		settings.setValidDataEndTime(activityCal.getTime());
		
		return settings;
	}

	
	@Override
	public AnalysisDuration estimateAnalysisDuration(
			DataTypeConfigModel dataTypeConfig, Date timeOfIncident)
	{
		// Return hardcoded duration of 1 hour.
		return new AnalysisDuration(3600000, 1);
	}


	@Override
	public CavStatus getAnalysisStatus()
	{
		// Set to finished status.
		CavStatus status = new CavStatus();
		status.setRunState(CavRunState.CAV_FINISHED);
		status.setProgressPercent(100f);
		
		return status;
	}


	@Override
	public int startAnalysis(DataTypeConfigModel dataTypeConfig,
			AnalysisConfigDataModel analysisConfig)
	{
		return AnalysisControlService.STATUS_SUCCESS;
	}


	@Override
	public boolean cancelAnalysis()
	{
		return true;
	}
	
	
	/**
	 * Sets the name of the data type to display in the Run configuration page.
	 * @param dataType the demo data type, used for display only.
	 */
	public void setDataType(String dataType)
	{
		m_DataType = dataType;
	}
	
	
	/**
	 * Sets the name of database to display in the Run configuration page.
	 * @param databaseName the demo database name, used for display only.
	 */
	public void setDatabaseName(String databaseName)
	{
		m_DatabaseName = databaseName;
	}
	
	
	/**
	 * Sets the host to display in the Run configuration page.
	 * @param host the demo host, used for display only.
	 */
	public void setHost(String host)
	{
		m_Host = host;
	}
	
	
	/**
	 * Sets the port number to display in the Run configuration page.
	 * @param port the demo port number, used for display only.
	 */
	public void setPort(int port)
	{
		m_Port = port;
	}
	
	
	/**
	 * Sets the username to display in the Run configuration page.
	 * @param username the demo username, used for display only.
	 */
	public void setUsername(String username)
	{
		m_Username = username;
	}

	
	/**
	 * Sets the analysis date to display in the Run configuration page.
	 * @param dateToAnalyze the demo analysis date, which <b>must</b> be in the
	 * 	format <i>yy-MM-dd</i>, used for display only.
	 */
	public void setDateToAnalyze(String dateToAnalyze)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yy-MM-dd");
		try
		{
			m_DateToAnalyze = dateFormatter.parse(dateToAnalyze);
		}
		catch (ParseException pe)
		{
			s_Logger.error("Error parsing supplied dateToAnalyze: " + dateToAnalyze, pe);
		}
	}
}
