/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.proxy.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;
import com.prelert.proxy.dao.RemoteCausalityDAO;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.TimeSeriesPlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;


public class CausalityServerRMI extends UnicastRemoteObject implements RemoteCausalityDAO 
{
	private static final long serialVersionUID = -1278512267353163015L;

	private static Logger s_Logger = Logger.getLogger(CausalityServerRMI.class);

	private String m_ServerName;
	private CausalityDAO m_CausalityDAO;	
	private TimeSeriesDAO m_TimeSeriesDAO;
	private PluginLocator m_PluginLocator;
	
	public CausalityServerRMI() throws RemoteException
	{
		super();
	}
	
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}

	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}
	
	public CausalityDAO getCausalityDAO()
	{
		return m_CausalityDAO;
	}
	
	public void setCausalityDAO(CausalityDAO dao)
	{
		m_CausalityDAO = dao;
	}
	
	public TimeSeriesDAO getTimeSeriesDAO()
	{
		return m_TimeSeriesDAO;
	}
	
	public void setTimeSeriesDAO(TimeSeriesDAO dao)
	{
		m_TimeSeriesDAO = dao;
	}
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}


	@Override
	public List<ProbableCause> getProbableCauses(int evidenceId,
												int timeSpanSecs,
												boolean maxOneFeaturePerSeries) 
	{
		// This will store the epoch time of the probable cause corresponding to
		// the input evidence ID
		long utcTime = -1;

		List<ProbableCause> causes = m_CausalityDAO.getProbableCauses(evidenceId,
																	timeSpanSecs,
																	maxOneFeaturePerSeries);

		Map<ExternalPointsPlugin, List<String>> externalKeysByPlugin = new HashMap<ExternalPointsPlugin, List<String>>();
		
		for (ProbableCause cause : causes)
		{
			// If this is the probable cause corresponding to the input evidence
			// ID, record the time, because this sets the mid-point of the GUI's
			// causality view
			if (cause.getEvidenceId() == evidenceId)
			{
				utcTime = cause.getTime().getTime();
			}

			String externalKey = cause.getExternalKey();
			if (externalKey != null && !externalKey.isEmpty())
			{
				if (cause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
				{
					DataSourceType datatype = cause.getDataSourceType();
					
					// Add keys to get the peak value of the points later.
					if (m_PluginLocator.isExternalPoints(datatype))
					{
						ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(datatype);
						List<String> keys = externalKeysByPlugin.get(plugin);
						if (keys == null)
						{
							keys = new ArrayList<String>();
							externalKeysByPlugin.put(plugin, keys);
						}
						keys.add(externalKey);
					}
					

					if (m_PluginLocator.isExternal(datatype))
					{
						ExternalPlugin plugin = m_PluginLocator.getExternalPluginForDataType(datatype);
						if (plugin != null)
						{
							List<Attribute> attributes = plugin.getAttributesForKey(externalKey);
							cause.setAttributes(attributes);
						}
					}

					if (m_PluginLocator.isExternalPoints(datatype) == false &&
							 m_PluginLocator.isExternal(datatype) == false)
					{
						s_Logger.warn("Inconsistency: probable cause for " +
										datatype + " / " + cause.getMetric() +
										" time series ID " + cause.getTimeSeriesId() +
										" has external key " + externalKey +
										" but no plugin is registered for this type.");
					}
				}
			}
		}

		if (causes.isEmpty())
		{
			return causes;
		}

		// It's possible that the probable cause list didn't contain a cause for
		// the input evidence ID - this can happen when displaying a causality
		// view for a time series anomaly when a higher anomaly for the same
		// time series is also part of the incident.  In this case we have to
		// make a database procedure call to find the time of the feature that
		// the input evidence ID relates to, but this is costly, hence we only
		// want to do it if necessary.
		if (utcTime == -1)
		{
			s_Logger.debug("probable_cause_list proc did not return a row for evidence ID " +
							evidenceId +
							" - calling evidence_single_complete to get time");

			Evidence feature = m_TimeSeriesDAO.getFeature(evidenceId);
			if (feature != null)
			{
				Date featureDate = feature.getTime();
				if (featureDate != null)
				{
					s_Logger.debug("Time for evidence ID " + evidenceId +
									" found as " + featureDate);
					utcTime = featureDate.getTime();
				}
			}

			// This is a last resort
			if (utcTime == -1)
			{
				s_Logger.error("Still could not find feature time for evidence ID " +
								evidenceId);
				utcTime = causes.get(causes.size() / 2).getTime().getTime();
			}
		}

		long timeSpanMS = timeSpanSecs * 1000;
		Date minTime = new Date(utcTime - (timeSpanMS / 2));
		Date maxTime = new Date(utcTime + (timeSpanMS / 2));


		// Now get the data points for all the external keys
		Set<ExternalPointsPlugin> keys = externalKeysByPlugin.keySet();

		for (ExternalPointsPlugin plugin : keys)
		{
			int interval = plugin.getUsualPointIntervalSecs();
			
			List<TimeSeriesPlugin.ExternalKeyPeakValuePair> peakValues =
							plugin.getPeakValueForTimeSpan(
											externalKeysByPlugin.get(plugin),
											minTime, maxTime, interval);

			Collections.sort(peakValues);

			for (ProbableCause pc : causes)
			{
				// A key feature of the
				// TimeSeriesPlugin.ExternalKeyPeakValuePair class is that
				// its compareTo() method ONLY considers the external key,
				// and NOT the peak value, hence we can just set it to 0
				// here
				TimeSeriesPlugin.ExternalKeyPeakValuePair keyPair =
					new TimeSeriesPlugin.ExternalKeyPeakValuePair(pc.getExternalKey(), 0.0);

				int pos = Collections.binarySearch(peakValues, keyPair);
				if (pos >= 0)
				{
					pc.setPeakValue(peakValues.get(pos).getPeakValue());
				}
			}
		}
		
		return causes;
	}


	@Override
	public List<Evidence> getAtTime(boolean singleDescription, int evidenceId, Date time,
								List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		List<Evidence> evidence = m_CausalityDAO.getAtTime(singleDescription, evidenceId, time, 
													filterAttributes, filterValues, pageSize);
		
		return evidence;	
	}


	@Override
	public List<ProbableCause> getProbableCausesForExport(int evidenceId,
														int timeSpanSecs,
														boolean maxOneFeaturePerSeries) throws RemoteException
	{
		List<ProbableCause> causes = m_CausalityDAO.getProbableCausesForExport(evidenceId,
																			timeSpanSecs,
																			maxOneFeaturePerSeries);

		// Get the attributes for external time series probable causes
		for (ProbableCause cause : causes)
		{
			String externalKey = cause.getExternalKey();
			if (externalKey != null && !externalKey.isEmpty())
			{
				if (cause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
				{
					DataSourceType datatype = cause.getDataSourceType();

					if (m_PluginLocator.isExternal(datatype))
					{
						ExternalPlugin plugin = m_PluginLocator.getExternalPluginForDataType(datatype);
						if (plugin != null)
						{
							List<Attribute> attributes = plugin.getAttributesForKey(externalKey);
							cause.setAttributes(attributes);
						}
					}

					if (m_PluginLocator.isExternal(datatype) == false)
					{
						s_Logger.warn("Inconsistency: probable cause for " +
								datatype + " / " + cause.getMetric() +
								" time series ID " + cause.getTimeSeriesId() +
								" has external key " + externalKey +
						" but no plugin is registered for this type.");
					}
				}
			}
		}

		return causes;
	}


	@Override
	public List<ProbableCause> getProbableCausesInBulk(List<Integer> evidenceIds,
													boolean maxOneFeaturePerSeries) throws RemoteException
	{
		List<ProbableCause> causes = m_CausalityDAO.getProbableCausesInBulk(evidenceIds,
																			maxOneFeaturePerSeries);

		// TODO - it would be possible to fill in other fields, e.g. time series
		// attributes and peak values using the same code as is in
		// getProbableCauses(), but this is not required at the moment

		return causes;
	}


    @Override
    public List<CausalityData> getCausalityData(int evidenceId, 
			List<String> returnAttributes, List<String> primaryFilterNamesNull,
			List<String> primaryFilterNamesNotNull, List<String> primaryFilterValues,
			String secondaryFilterName, String secondaryFilterValue) throws RemoteException
    {
	    return m_CausalityDAO.getCausalityData(evidenceId, returnAttributes, 
	    		primaryFilterNamesNull, primaryFilterNamesNotNull, primaryFilterValues, 
	    		secondaryFilterName, secondaryFilterValue);
    }

    
	@Override
	public List<Evidence> getFirstPage(boolean singleDescription, int evidenceId, List<String> filterAttributes,
										List<String> filterValues, int pageSize) 
	{
		List<Evidence> evidence = m_CausalityDAO.getFirstPage(singleDescription, evidenceId, 
				filterAttributes, filterValues, pageSize);
		
		return evidence;	
	}

	
	@Override
	public List<Evidence> getLastPage(boolean singleDescription,
			int evidenceId, List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{
		List<Evidence> evidence = m_CausalityDAO.getLastPage(singleDescription, evidenceId, 
				filterAttributes, filterValues, pageSize);

		return evidence;
	}

	
	@Override
	public List<Evidence> getNextPage(boolean singleDescription, int bottomRowId, Date bottomRowTime,
								List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{
		List<Evidence> evidence = m_CausalityDAO.getNextPage(singleDescription, bottomRowId, bottomRowTime, 
				filterAttributes, filterValues, pageSize);
		
		return evidence;	
	}

	
	@Override
	public List<Evidence> getPreviousPage(boolean singleDescription, int topRowId, Date topRowTime,
								List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{
		List<Evidence> evidence = m_CausalityDAO.getPreviousPage(singleDescription, topRowId, topRowTime, 
				filterAttributes, filterValues, pageSize);
		
		return evidence;	
	}

	
	@Override
	public Evidence getEarliestEvidence(boolean singleDescription, int evidenceId,
			List<String> filterAttributes, List<String> filterValues) 
	{
		Evidence evidence = m_CausalityDAO.getEarliestEvidence(singleDescription, evidenceId, filterAttributes, filterValues);
		
		return evidence;	
	}

	
	@Override
	public Evidence getLatestEvidence(boolean singleDescription, int evidenceId, List<String> filterAttributes,
													List<String> filterValues) 
	{
		Evidence evidence = m_CausalityDAO.getLatestEvidence(singleDescription, evidenceId, filterAttributes, filterValues);
		
		return evidence;	
	}	
}
