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

package com.prelert.proxy.inputmanager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querydatetime.CurrentQueryDateTimeProducer;
import com.prelert.proxy.inputmanager.querydatetime.HistoricalQueryDateTimeProducer;
import com.prelert.proxy.inputmanager.querydatetime.QueryDateTimeProducer;
import com.prelert.proxy.runpolicy.RunPolicy;

public class InputManagerFactory
{
	private static Logger s_Logger = Logger.getLogger(InputManagerFactory.class);
	
	
	/**
	 * Create and configure a new InputManager from the array of string values.
	 *
	 * <code>properties</code> can contain the following values in 
	 * this order.
	 * <dl>
	 * <dt>host</dt><dd>The host machine the Prelert Processes are running on.</dd>
	 * <dt>port</dt><dd>The port to connect to the Prelert Processes on.</dd>
	 * <dt>update interval secs</dt><dd>Period to wait before pulling more data. 
	 * For historical inputmanagers 0 is a suitable value.</dd>
	 * <dt>query length secs</dt><dd>Length of the queries in seconds. For real time 
	 * inputmanagers this should be equal to update interval.</dd>
	 * <dt>start date</dt><dd>If specified create a historical input manager. If end date
	 *  is not specified then the inputmanager will collect data from the start date 
	 *  until now then create a realtime inputmanager.</dd>
	 * <dt>end date</dt><dd>The end date for a a historical input manager.</dd>
	 * 
	 * @param properties
	 * @param inputManagerDAO
	 * @return
	 */
	static public InputManager newInputManager(String[] properties, InputManagerDAO inputManagerDAO)
	{
		if (properties.length < 3) 
		{
			String msg = "Cannot create the InputManager. " + 
						"The type, host, and port properties should be defined " +
						"at the minimum. Invalid arg = " + properties;
			s_Logger.error(msg);
			
			throw new IllegalArgumentException(msg);
		}
		
		
		InputManagerType inputManagerType = InputManagerType.enumValue(properties[0].trim());
		InputManager inputManager = createInputManager(inputManagerType, inputManagerDAO);

		
		/*
		 *  Set host and port.
		 */
		String host = properties[1];
		int port;
		try
		{
			port = Integer.parseInt(properties[2].trim());
		}
		catch (NumberFormatException ne)
		{
			String msg = "Could not parse port number '" + properties[2].trim() +
								"' in argument = " +  properties;
			s_Logger.error(msg);
			
			throw new IllegalArgumentException(msg);
		}
		
		
		/*
		 * update interval
		 */
		int updateIntervalSecs = -1;
		if (properties.length >= 4) 
		{
			String intervalStr = properties[3].trim();
			try
			{
				updateIntervalSecs = Integer.parseInt(intervalStr);
			}
			catch (NumberFormatException nfe)
			{
				String msg = "Could not parse update interval'" + properties[3].trim() +
							"' in argument = " +  properties;
				s_Logger.error(msg);

				throw new IllegalArgumentException(msg);		
			}
		}
		
		
		/*
		 * Historical or RealTime inputmanager?
		 */
		boolean isHistoricalInputManager = properties.length >= 6;
		if (isHistoricalInputManager) 
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			Date start;
			Date end;

			try
			{
				long queryLength = Integer.parseInt(properties[4].trim());
				start = dateFormat.parse(properties[5].trim());

				end = null;
				if (properties.length >= 7) // has end date.
				{
					end = dateFormat.parse(properties[6].trim());
				}

				s_Logger.info(String.format("Creating historical input manager with start = %s and end =%s", 
								start, end));

				
				if (updateIntervalSecs < 0)
				{
					updateIntervalSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS;
				}
				
				QueryDateTimeProducer queryDateProducer = 
					new HistoricalQueryDateTimeProducer(start, end, queryLength, updateIntervalSecs);

				inputManager.setQueryDateTimeProducer(queryDateProducer);
			}
			catch (ParseException pe)
			{
				s_Logger.error("Could not parse dates for historical Inputmanager data. " + 
						"Start = " + properties[6] + ", End = " + properties[7]);

				QueryDateTimeProducer queryDateProducer = new CurrentQueryDateTimeProducer();
				inputManager.setQueryDateTimeProducer(queryDateProducer);
			}
		}
		else if (RunPolicy.isDownloadProduct()) // CAV will be configured later.
		{
			// This bit of code set the update interval then there was a bit 
			// of hackery later on to preserve it when the CAV start/end dates
			// were set.

			long queryLength = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_QUERY_LENGTH_SECS;
			if (properties.length == 5)
			{
				try
				{
					queryLength = Integer.parseInt(properties[4].trim());
				}
				catch (NumberFormatException pe)
				{
					s_Logger.error("The QueryLengthSecs value for is not a number. " +
									"Value = " + properties[4].trim());
				}
				
			}

			if (updateIntervalSecs < 0)
			{
				updateIntervalSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS;
			}
			QueryDateTimeProducer queryDateProducer = 
					new HistoricalQueryDateTimeProducer(null, null, queryLength, updateIntervalSecs);
			
			inputManager.setQueryDateTimeProducer(queryDateProducer);
		}
		else // use the default of a real time query manager.
		{
			if (updateIntervalSecs < 0)
			{
				updateIntervalSecs = CurrentQueryDateTimeProducer.DEFAULT_UPDATE_INTERVAL_SECS;
			}
			
			QueryDateTimeProducer queryDateProducer = new CurrentQueryDateTimeProducer(updateIntervalSecs);
			inputManager.setQueryDateTimeProducer(queryDateProducer);
		}
		
		inputManager.createBackEndClient(host, port);
		

		return inputManager;
	}
	
	
	
	/**
	 * Create and configure a new InputManager from the <code>config</code>
	 * object.
	 * 
	 * @param config - The configuration for the new InputManager.
	 * @param dao - The InputManager DAO.
	 * @return
	 */
	static public InputManager newInputManager(InputManagerConfig config, InputManagerDAO dao)
	{
		InputManager inputManager = createInputManager(config.getInputManagerType(), dao);
		
		
		Date start = config.getStartDate();
		Date end = config.getEndDate();
		
		int updateIntervalSecs = (config.getUpdateIntervalSecs() == null) ? -1 : config.getUpdateIntervalSecs();
		int queryLengthSecs = (config.getQueryLengthSecs() == null) ? -1 : config.getQueryLengthSecs();
		int delaySecs = (config.getDelaySecs() == null) ? 0 : config.getDelaySecs();
		
		if (start != null)
		{
			s_Logger.info(String.format("Creating historical input manager with start = %s and end =%s", 
					start, end));
			
	
			if (updateIntervalSecs < 0)
			{
				updateIntervalSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS;
			}
			
			if (queryLengthSecs < 0)
			{
				queryLengthSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_QUERY_LENGTH_SECS;
			}

			QueryDateTimeProducer queryDateProducer = 
				new HistoricalQueryDateTimeProducer(start, end, queryLengthSecs, updateIntervalSecs);

			inputManager.setQueryDateTimeProducer(queryDateProducer);
		}
		else if (RunPolicy.isDownloadProduct()) // CAV will be configured later.
		{
			// This bit of code set the update interval then there was a bit 
			// of hackery later on to preserve it when the CAV start/end dates
			// were set.
			if (queryLengthSecs < 0)
			{
				queryLengthSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_QUERY_LENGTH_SECS;				
			}

			if (updateIntervalSecs < 0)
			{
				updateIntervalSecs = HistoricalQueryDateTimeProducer.DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS;
			}
			
			QueryDateTimeProducer queryDateProducer = 
					new HistoricalQueryDateTimeProducer(null, null, queryLengthSecs, updateIntervalSecs);
			
			inputManager.setQueryDateTimeProducer(queryDateProducer);
		}
		else // use the default of a real time query manager.
		{
			if (updateIntervalSecs < 0)
			{
				updateIntervalSecs = CurrentQueryDateTimeProducer.DEFAULT_UPDATE_INTERVAL_SECS;
			}
			
			CurrentQueryDateTimeProducer queryDateProducer = new CurrentQueryDateTimeProducer(updateIntervalSecs);
			queryDateProducer.setDelaySeconds(delaySecs);
			inputManager.setQueryDateTimeProducer(queryDateProducer);
		}
		
		if (config.getHost() != null || config.getPort() != null)
		{
			inputManager.createBackEndClient(config.getHost(), config.getPort());
		}

		return inputManager;
	}
	
	
	/**
	 * Creates and returns a new InputManager depending on the type parameter.
	 * 
	 * @param type
	 * @param inputManagerDAO
	 * @return The InputManager or <code>null</code> if type is unknown.
	 */
	static private InputManager createInputManager(InputManagerType type,
									InputManagerDAO inputManagerDAO)
	{
		InputManager inputManager = null;

		
		if (type == InputManagerType.EXTERNAL)
		{
			inputManager = new InputManagerExternalTimeSeries(inputManagerDAO);
		}
		else if (type == InputManagerType.EXTERNALPOINTS)
		{
			inputManager = new InputManagerExternalPoints(inputManagerDAO);
		}
		else if(type == InputManagerType.INTERNAL)
		{
			inputManager = new InputManagerInternalTimeSeries(inputManagerDAO);
		}
		else if (type == InputManagerType.NOTIFICATION)
		{
			inputManager = new InputManagerNotification(inputManagerDAO);
		}
		else if (type == InputManagerType.GZIP)
		{
			inputManager = new InputManagerGZipTimeSeries(inputManagerDAO);
		}
		else
		{
			String msg = String.format("Unknown InputManager Type = %s. " +
								"Should be one of: %s, %s, %s, %s.",
								type, InputManagerType.EXTERNAL,
								InputManagerType.EXTERNALPOINTS, InputManagerType.INTERNAL,
								InputManagerType.NOTIFICATION);
			
			s_Logger.error(msg);
		}
		
		return inputManager;
	}
	
	
	static public void setInputManagerWindowSize(int windowSizeSecs)
	{
		InputManager.s_InputManagerSync.setMaxWindowSize(windowSizeSecs);
	}
	
}
