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

package com.prelert.server;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.data.Alerter;
import com.prelert.service.AlertingConfigService;


/**
 * Server-side implementation of the service for configuring Alerting functionality.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class AlertingConfigServiceImpl extends RemoteServiceServlet 
	implements AlertingConfigService
{
	static Logger s_Logger = Logger.getLogger(AlertingConfigServiceImpl.class);
	
	/** Filename of the UI alerters XML configuration file */
	public static final String GUI_ALERTERS_FILENAME = "gui_alerters.xml";

	
	@Override
	public Alerter getAlerter()
	{
		Alerter alerter = new Alerter();
		
		try
		{
			AlertingFileConfiguration fileConfig = new AlertingFileConfiguration();
			alerter = fileConfig.load();
		}
		catch (IOException ioe)
		{
			s_Logger.error("getAlerter() error loading configuration from file: " + 
					ioe.getMessage(), ioe);
			
			// Set script type by default.
			alerter.setType(Alerter.TYPE_SCRIPT);
		}
		
		return alerter;
	}


	@Override
	public int setAlerter(Alerter alerter)
	{
		try
		{
			AlertingFileConfiguration fileConfig = new AlertingFileConfiguration();
			fileConfig.save(alerter);
			return AlertingConfigService.STATUS_SUCCESS;
		}
		catch (FileNotFoundException fnfe)
		{
			s_Logger.error("setAlerter() alerter configuration file " +
					"does not exist and cannot be created: " + fnfe.getMessage(), fnfe);
			return AlertingConfigService.STATUS_CANNOT_CREATE_FILE;
		}
		catch (IOException ioe)
		{
			s_Logger.error("setAlerter() error saving configuration to file: " + 
					ioe.getMessage(), ioe);
			return AlertingConfigService.STATUS_FAILURE_UNKNOWN;
		}
	}

}
