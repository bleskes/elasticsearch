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
 ************************************************************/

package com.prelert.api.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;

import com.prelert.data.Attribute;
import com.prelert.data.Incident;

/**
 * This class encapsulates a shallow description of an Activity 
 */
public class Activity 
{
	private DateTime m_PeakDate;
	private DateTime m_FirstDate;
	private DateTime m_LastDate;
	private DateTime m_UpdateDate;
	private int m_AnomalyScore;
	private String m_Description;
	private int m_Id;
	private String m_SourceType;
	private int m_RelatedMetricCount;
	private int m_HostCount;
	private String m_SharedMetricPath;
	private List<RelatedMetric> m_RelatedMetrics;
	
	
	/**
	 * The time of Activity. 
	 * This is the time of the most significant piece of evidence.
	 * @return
	 */
	public DateTime getPeakEvidenceTime() 
	{
		return m_PeakDate;
	}

	public void setPeakEvidenceTime(DateTime epochDate) 
	{
		this.m_PeakDate = epochDate;
	}
	
	
	/**
	 * The time of the earliest piece of evidence in the Activity.
	 * Together with {@link #getEndTime()} this is the time span
	 * of the activity. 
	 * @return
	 */
	public DateTime getFirstEvidenceTime() 
	{
		return m_FirstDate;
	}

	public void setFirstEvidenceTime(DateTime first) 
	{
		this.m_FirstDate = first;
	}
	
	
	/**
	 * The time of the last piece of evidence in the Activity.
	 * Together with {@link #getFirstEvidenceTime()} this is the time span
	 * of the activity. 
	 * @return
	 */
	public DateTime getLastEvidenceTime() 
	{
		return m_LastDate;
	}

	public void setLastEvidenceTime(DateTime last) 
	{
		this.m_LastDate = last;
	}
	
	
	/**
	 * The wall clock time of the last update to the
	 * activity. An activity is updated when new evidence
	 * is added or it is merged.  
	 * @return
	 */
	public DateTime getUpdateTime() 
	{
		return m_UpdateDate;
	}

	public void setUpdateTime(DateTime updateTime) 
	{
		this.m_UpdateDate = updateTime;
	}
	

	/**
	 * The anomaly score of the Activity. 
	 * This is a value between 1 and 100.  
	 * @return
	 */
	public int getAnomalyScore() 
	{
		return m_AnomalyScore;
	}

	public void setAnomalyScore(int anomalyScore) 
	{
		this.m_AnomalyScore = anomalyScore;
	}

	/**
	 * Brief description of the Activity
	 * @return
	 */
	public String getDescription() 
	{
		return m_Description;
	}

	public void setDescription(String description) 
	{
		this.m_Description = description;
	}

	/**
	 * The ID of the 'headline' item of evidence in the Activity
	 * @return
	 */
	@EntityKey
	public int getId() 
	{
		return m_Id;
	}

	public void setId(int Id) 
	{
		this.m_Id = Id;
	}

	/**
	 * The source type of the headline feature. 
	 * @return
	 */
	public String getSourceType() 
	{
		return m_SourceType;
	}

	public void setSourceType(String sourceType) 
	{
		this.m_SourceType = sourceType;
	}

	/**
	 * The total number of time series in the Activity
	 * This the same as <code>getRelatedMetrics().size()</code>. 
	 * @return
	 */
	public int getRelatedMetricCount() 
	{
		return m_RelatedMetricCount;
	}

	public void setRelatedMetricCount(int timeSeriesCount) 
	{
		this.m_RelatedMetricCount = timeSeriesCount;
	}

	/**
	 * The number of hosts involved in the Activity
	 * @return
	 */
	public int getHostCount() 
	{
		return m_HostCount;
	}

	public void setHostCount(int sourceCount) 
	{
		this.m_HostCount = sourceCount;
	}

	/**
	 * The partial metric path that is common to every time 
	 * series in the Activity
	 * @return
	 */
	public String getSharedMetricPath() 
	{
		return m_SharedMetricPath;
	}

	public void setSharedMetricPath(String sharedMetricPath) 
	{
		this.m_SharedMetricPath = sharedMetricPath;
	}
	
	/**
	 * The list of related metrics.
	 * If no related metrics have been loaded then <code>null</code> is returned
	 * else the metrics. 
	 * 
	 * IgnoreProperty annotation is used so this will not be part of the ODATA metadata.
	 * 
	 * @return may be <code>null</code>
	 */
	@IgnoreProperty
	public List<RelatedMetric> getRelatedMetrics()
	{
		return m_RelatedMetrics;
	}
	
	public void setRelatedMetrics(List<RelatedMetric> value)
	{
		m_RelatedMetrics = value;
	}
	
	
	/**
	 * Factory method to create an Activity from an Incident.
	 * @param incident
	 * @return
	 */
	static public Activity createFromIncident(Incident incident)
	{
		Activity act = new Activity();
		act.setPeakEvidenceTime(new DateTime(incident.getTime()));
		act.setFirstEvidenceTime(new DateTime(incident.getFirstTime()));
		act.setLastEvidenceTime(new DateTime(incident.getLastTime()));
		act.setUpdateTime(new DateTime(incident.getUpdateTime())); 
		act.setAnomalyScore(incident.getAnomalyScore());
		act.setDescription(incident.getDescription());
		act.setId(incident.getTopEvidenceId());
		act.setSourceType(incident.getTopDataSourceType().getName());
		act.setRelatedMetricCount(incident.getTimeSeriesCount());
		act.setHostCount(incident.getSourceCount());

		
		// Sort attributes (by attribute name) so any named 
		// ResourcePath1, ResourcePath2, etc are in the right order.
		Collections.sort(incident.getSharedAttributes());
		
		// Build the metric path 
		String host = null;
		String agent = null;
		String process = null;
		String resourcePath = "";
		String metric = null;
		
		for (Attribute attr : incident.getSharedAttributes())
		{
			if ("source".equals(attr.getAttributeName()))
			{
				host = attr.getAttributeValue();
			}
			else if (RelatedMetric.PROCESS_ATTRIBUTE.equals(attr.getAttributeName()))
			{
				process = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().equals(RelatedMetric.AGENT_ATTRIBUTE))
			{
				agent = attr.getAttributeValue();
			}
			else if (attr.getAttributeName().startsWith(RelatedMetric.RESOURCE_PATH_ATTRIBUTE))
			{
				resourcePath = resourcePath + RelatedMetric.PATH_SEPARATOR + attr.getAttributeValue();
			}
			else if ("metric".equals(attr.getAttributeName()))
			{
				metric = attr.getAttributeValue();
			}
		}
		
		
		StringBuilder sb = new StringBuilder();
		if (host != null)
		{
			sb.append(host);
			if (process != null) 
			{
				sb.append(RelatedMetric.PATH_SEPARATOR).append(process);
				
				if (agent != null) 
				{
					sb.append(RelatedMetric.PATH_SEPARATOR).append(agent);
					
					if (resourcePath.isEmpty() == false) 
					{
						sb.append(resourcePath);
						
						if (metric != null)
						{
							sb.append(RelatedMetric.METRIC_SEPARATOR).append(metric);
						}
					}
				}
			}
		}

		act.setSharedMetricPath(sb.toString());
		
		return act;
	}
	
	
	/**
	 * Sort by anomaly score comparator. 
	 */
	static public class ScoreComparator implements Comparator<Activity>
	{
		private boolean m_Ascending;
		
		public ScoreComparator(boolean ascendingSortDir)
		{
			m_Ascending = ascendingSortDir;	
		}
		
		@Override
		public int compare(Activity arg0, Activity arg1) {
			int result;
			if (m_Ascending)
			{
				result = arg0.getAnomalyScore() - arg1.getAnomalyScore();
			}
			else 
			{
				result = arg1.getAnomalyScore() - arg0.getAnomalyScore();
			}

			if (result == 0)
			{
				// use date time as secondary order.
				result = arg0.getPeakEvidenceTime().compareTo(arg1.getPeakEvidenceTime());
			}

			return result;
		}
	};
	
	
	/**
	 * Sort by peak evidence time comparator. 
	 */
	static public class PeakTimeComparator implements Comparator<Activity>
	{
		private boolean m_Ascending;
		
		public PeakTimeComparator(boolean ascendingSortDir)
		{
			m_Ascending = ascendingSortDir;	
		}
		
		@Override
		public int compare(Activity arg0, Activity arg1) {
			int result;
			if (m_Ascending)
			{
				result = arg0.getPeakEvidenceTime().compareTo(arg1.getPeakEvidenceTime());
			}
			else 
			{
				result = arg1.getPeakEvidenceTime().compareTo(arg0.getPeakEvidenceTime());
			}

			if (result == 0)
			{
				// secondary order is by anomaly score
				result = arg0.getAnomalyScore() - arg1.getAnomalyScore();
			}

			return result;
		}
	};
	
	
	/**
	 * Sort by first evidence time comparator. 
	 */
	static public class FirstTimeComparator implements Comparator<Activity>
	{
		private boolean m_Ascending;
		
		public FirstTimeComparator(boolean ascendingSortDir)
		{
			m_Ascending = ascendingSortDir;	
		}
		
		@Override
		public int compare(Activity arg0, Activity arg1) {
			int result;
			if (m_Ascending)
			{
				result = arg0.getFirstEvidenceTime().compareTo(arg1.getFirstEvidenceTime());
			}
			else 
			{
				result = arg1.getFirstEvidenceTime().compareTo(arg0.getFirstEvidenceTime());
			}

			if (result == 0)
			{
				// secondary order is by anomaly score
				result = arg0.getAnomalyScore() - arg1.getAnomalyScore();
			}

			return result;
		}
	};
	
	
	/**
	 * Sort by last evidence time comparator. 
	 */
	static public class LastTimeComparator implements Comparator<Activity>
	{
		private boolean m_Ascending;
		
		public LastTimeComparator(boolean ascendingSortDir)
		{
			m_Ascending = ascendingSortDir;	
		}
		
		@Override
		public int compare(Activity arg0, Activity arg1) {
			int result;
			if (m_Ascending)
			{
				result = arg0.getLastEvidenceTime().compareTo(arg1.getLastEvidenceTime());
			}
			else 
			{
				result = arg1.getLastEvidenceTime().compareTo(arg0.getLastEvidenceTime());
			}

			if (result == 0)
			{
				// secondary order is by anomaly score
				result = arg0.getAnomalyScore() - arg1.getAnomalyScore();
			}

			return result;
		}
	};
	
	
	/**
	 * Sort by update time comparator. 
	 */
	static public class UpdateTimeComparator implements Comparator<Activity>
	{
		private boolean m_Ascending;
		
		public UpdateTimeComparator(boolean ascendingSortDir)
		{
			m_Ascending = ascendingSortDir;	
		}
		
		@Override
		public int compare(Activity arg0, Activity arg1) {
			int result;
			if (m_Ascending)
			{
				result = arg0.getUpdateTime().compareTo(arg1.getUpdateTime());
			}
			else 
			{
				result = arg1.getUpdateTime().compareTo(arg0.getUpdateTime());
			}

			if (result == 0)
			{
				// secondary order is by anomaly score
				result = arg0.getAnomalyScore() - arg1.getAnomalyScore();
			}

			return result;
		}
	};
	
	
}
	
