/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.plugin.introscope.clworkstation;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesData;
import com.wily.introscope.clws.CLWClientService;
import com.wily.introscope.spec.server.beans.clw.CommandException;
import com.wily.introscope.spec.server.beans.clw.CommandNotification;
import com.wily.introscope.spec.server.beans.session.IllegalSessionException;
import com.wily.isengard.messageprimitives.service.IllegalServiceAccessException;

public class CLWorkstation8Cmd extends IntroscopeWorkstationConnection  
{
	private static Logger s_Logger = Logger.getLogger(CLWorkstation8Cmd.class);

	@Override
	public Collection<TimeSeriesData> getMetricData(String agentRegex,
					String metricRegex, Date start, Date stop, int intervalSecs) 
	throws ConnectionException
	{
		synchronized(m_ConnectionMutex)
		{
			// Don't allow queries for times in the future.
			// Querying for events in the future causes the CLW 
			// to hang in version 8.x.
			Date now = new Date(new Date().getTime());
			if (start.after(now))
			{
				return Collections.emptyList();
			}
			
			if (stop.after(now))
			{
				stop = now;
			}
			
			
			String cmd = String.format(HISTORICAL_DATA_CMD, agentRegex, metricRegex, 
					start, stop, intervalSecs);
			s_Logger.debug("getMetricData cmd = " + cmd);
			
			resetDisconnectTimer();
			reconnectIfDisconnected();
			
			
			Date time1 = new Date();
			CLWClientService clwClientService = new CLWClientService(m_EnterpriseManager.getPostOffice());

			MetricDataCallbackNotification callback = new MetricDataCallbackNotification(getMetricDataType());
			CommandNotification commandNotification = new CommandNotification(m_EnterpriseManager.getPostOffice(),
					callback, clwClientService);  

			try 
			{
				m_ClwService.executeAsyncCommand(m_EnterpriseManager.getSessionToken(), 
						new String[]{cmd}, 
						commandNotification);

				callback.waitForFinish();

				Date time2 = new Date();

				clwClientService.close();
				commandNotification.close();    

				Collection<TimeSeriesData> results = callback.getQueryResults();

				Date time3 = new Date();

				long time3_1 = time3.getTime() - time1.getTime();
				long time2_1 = time2.getTime() - time1.getTime();
				long time3_2 = time3.getTime() - time2.getTime();
				
				String timings = "getMetricData = %s metrics queried in %s ms, processed in %s ms. %s ms in total.";
				s_Logger.debug(String.format(timings, results.size(), time2_1, time3_2, time3_1));
				            
				return results;
			}
			catch (com.wily.isengard.messageprimitives.ConnectionException e) 
			{
				s_Logger.error("getMetricData ConnectionException: " + e);

				disconnect();

				throw new com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException(e.getMessage());
			}
			catch (CommandException e) 
			{
				s_Logger.error("getMetricData CommandException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (IllegalServiceAccessException e) 
			{
				s_Logger.error("getMetricData IllegalServiceAccessException: " + e);

				disconnect();

				return Collections.emptyList();
			} 
			catch (IllegalSessionException e) 
			{
				s_Logger.error("getMetricData IllegalSessionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			finally
			{
				resetDisconnectTimer();
			}
		}
	}
	

	@Override
	public Collection<TimeSeriesData> getMetricDataForLastNMinutes(String agentRegex,
					String metricRegex, int lastNMinutes, int intervalSecs) 
	throws ConnectionException
	{
		synchronized(m_ConnectionMutex)
		{
			String cmd = String.format(RECENT_DATA_CMD, agentRegex, metricRegex, 
					lastNMinutes, intervalSecs);
			s_Logger.debug("getMetricDataForLastNMinutes cmd = " + cmd);
			
			resetDisconnectTimer();
			reconnectIfDisconnected();
			
			
			Date time1 = new Date();
		
			CLWClientService clwClientService = new CLWClientService(m_EnterpriseManager.getPostOffice());

			MetricDataCallbackNotification callback = new MetricDataCallbackNotification(getMetricDataType());
			CommandNotification commandNotification = new CommandNotification(m_EnterpriseManager.getPostOffice(),
					callback, clwClientService);  

			try 
			{
				m_ClwService.executeAsyncCommand(m_EnterpriseManager.getSessionToken(), 
						new String[]{cmd}, 
						commandNotification);

				callback.waitForFinish();

				Date time2 = new Date();

				clwClientService.close();
				commandNotification.close();    

				Collection<TimeSeriesData> results = callback.getQueryResults();

				Date time3 = new Date();

				long time3_1 = time3.getTime() - time1.getTime();
				long time2_1 = time2.getTime() - time1.getTime();
				long time3_2 = time3.getTime() - time2.getTime();
				
				String timings = "getMetricDataForLastNMinutes = %s metrics queried in %s ms, processed in %s ms. %s ms in total.";
				s_Logger.debug(String.format(timings, results.size(), time2_1, time3_2, time3_1));
				            
				return results;
			}
			catch (com.wily.isengard.messageprimitives.ConnectionException e) 
			{
				s_Logger.error("getMetricDataForLastNMinutes ConnectionException: " + e);

				disconnect();

				throw new com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException(e.getMessage());
			}
			catch (CommandException e) 
			{
				s_Logger.error("getMetricDataForLastNMinutes CommandException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (IllegalServiceAccessException e) 
			{
				s_Logger.error("getMetricDataForLastNMinutes IllegalServiceAccessException: " + e);

				disconnect();

				return Collections.emptyList();
			} 
			catch (IllegalSessionException e) 
			{
				s_Logger.error("getMetricDataForLastNMinutes IllegalSessionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			finally
			{
				resetDisconnectTimer();
			}
		}
	}
	
	
	@Override
	public List<String> listAgents(String agentRegex) 
	{
		synchronized(m_ConnectionMutex)
		{
			String cmd = String.format(LIST_AGENTS_CMD, agentRegex);
			
			resetDisconnectTimer();
			reconnectIfDisconnected();

			CLWClientService clwClientService = new CLWClientService(m_EnterpriseManager.getPostOffice());

			ListAgentsCallbackNotification listAgentCallbackNotification = new ListAgentsCallbackNotification();
			CommandNotification commandNotification = new CommandNotification(m_EnterpriseManager.getPostOffice(),
					listAgentCallbackNotification, clwClientService);  

			try 
			{
				m_ClwService.executeAsyncCommand(m_EnterpriseManager.getSessionToken(), 
						new String[]{cmd}, 
						commandNotification);

				listAgentCallbackNotification.waitForFinish();

				clwClientService.close();
				commandNotification.close();	

				return listAgentCallbackNotification.getQueryResults();

			}
			catch (com.wily.isengard.messageprimitives.ConnectionException e) 
			{
				s_Logger.error("ListAgents ConnectionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (CommandException e) 
			{
				s_Logger.error("ListAgents CommandException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (IllegalServiceAccessException e) 
			{
				s_Logger.error("ListAgents IllegalServiceAccessException: " + e);

				disconnect();

				return Collections.emptyList();
			} 
			catch (IllegalSessionException e) 
			{
				s_Logger.error("ListAgents IllegalSessionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			finally
			{
				resetDisconnectTimer();
			}
		}
	}
	
	
	@Override
	public List<String> listManagementModules(String moduleRegex) 
	{
		synchronized(m_ConnectionMutex)
		{
			resetDisconnectTimer();
			reconnectIfDisconnected();
			
			CLWClientService clwClientService = new CLWClientService(m_EnterpriseManager.getPostOffice());

			ListAgentsCallbackNotification listAgentCallbackNotification = new ListAgentsCallbackNotification();
			CommandNotification commandNotification = new CommandNotification(m_EnterpriseManager.getPostOffice(),
					listAgentCallbackNotification, clwClientService);  
			
			if (moduleRegex == null || moduleRegex.isEmpty())
			{
				moduleRegex = ".*";
			}
			String cmd = String.format(LIST_MAN_MODULES_CMD, moduleRegex);

			try 
			{
				m_ClwService.executeAsyncCommand(m_EnterpriseManager.getSessionToken(), 
						new String[]{cmd}, 
						commandNotification);

				listAgentCallbackNotification.waitForFinish();

				clwClientService.close();
				commandNotification.close();	

				return listAgentCallbackNotification.getQueryResults();
			}
			catch (com.wily.isengard.messageprimitives.ConnectionException e) 
			{
				s_Logger.error("listManagementModules ConnectionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (CommandException e) 
			{
				s_Logger.error("listManagementModules CommandException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			catch (IllegalServiceAccessException e) 
			{
				s_Logger.error("listManagementModules IllegalServiceAccessException: " + e);

				disconnect();

				return Collections.emptyList();
			} 
			catch (IllegalSessionException e) 
			{
				s_Logger.error("listManagementModules IllegalSessionException: " + e);

				disconnect();

				return Collections.emptyList();
			}
			finally
			{
				resetDisconnectTimer();
			}
		}
	}
}

