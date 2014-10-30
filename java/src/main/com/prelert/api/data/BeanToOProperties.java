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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.expression.EntitySimpleProperty;

/**
 * Utility class creates the list of OProperties for the 
 * Prelert API bean classes. 
 * The property names are hardcoded rather than using introspection
 * for run-time performance so if the bean definition changes these
 * methods have to be updated.
 * See the related unit test.
 */
public class BeanToOProperties 
{
	
	/**
	 * Create a list of OProperties from the Activity class
	 * 
	 * @param act if <code>null</code> then a empty list is returned.
	 * @return List of OProperty
	 */
	static public List<OProperty<?>> activityOProperties(Activity act)
	{
		if (act == null)
		{
			return Collections.emptyList();
		}
		
		 List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		 OProperty<?> prop = OProperties.datetimeOffset("PeakEvidenceTime", act.getPeakEvidenceTime());		
		 properties.add(prop);
		 prop = OProperties.datetimeOffset("FirstEvidenceTime", act.getFirstEvidenceTime());		
		 properties.add(prop);
		 prop = OProperties.datetimeOffset("LastEvidenceTime", act.getLastEvidenceTime());		
		 properties.add(prop);
		 prop = OProperties.datetimeOffset("UpdateTime", act.getUpdateTime());		
		 properties.add(prop);
		 prop = OProperties.int32("AnomalyScore", act.getAnomalyScore());		
		 properties.add(prop);
		 prop = OProperties.string("Description", act.getDescription());		
		 properties.add(prop);
		 prop = OProperties.int32("Id", act.getId());		
		 properties.add(prop);
		 prop = OProperties.string("SourceType", act.getSourceType());		
		 properties.add(prop);
		 prop = OProperties.int32("RelatedMetricCount", act.getRelatedMetricCount());		
		 properties.add(prop);
		 prop = OProperties.int32("HostCount", act.getHostCount());		
		 properties.add(prop);
		 prop = OProperties.string("SharedMetricPath", act.getSharedMetricPath());		
		 properties.add(prop);
		 
		 return properties;
	}
	
	/**
	 * Create a list of OProperties from the Activity class only 
	 * returning the properties specified in <code>selectProps</code>.
	 * 
	 * @param act
	 * @param selectProps Properties to select. If null or empty then 
	 * {@link #activityOProperties(Activity)} is called.
	 * @return
	 */
	static public List<OProperty<?>> activityOProperties(Activity act, List<EntitySimpleProperty> selectProps)
	{
		if (selectProps == null || selectProps.size() == 0)
		{
			return activityOProperties(act);
		}

		if (act == null)
		{
			return Collections.emptyList();
		}
		
		Set<String> selects = new HashSet<String>();
		for (EntitySimpleProperty prop : selectProps)
		{
			selects.add(prop.getPropertyName().toLowerCase());
		}


		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		if (selects.contains("PeakEvidenceTime".toLowerCase()))
		{
			OProperty<?> prop = OProperties.datetimeOffset("PeakEvidenceTime", act.getPeakEvidenceTime());		
			properties.add(prop);
		}
		if (selects.contains("FirstEvidenceTime".toLowerCase()))
		{
			OProperty<?> prop = OProperties.datetimeOffset("FirstEvidenceTime", act.getFirstEvidenceTime());		
			properties.add(prop);
		}
		if (selects.contains("LastEvidenceTime".toLowerCase()))
		{
			OProperty<?> prop = OProperties.datetimeOffset("LastEvidenceTime", act.getLastEvidenceTime());		
			properties.add(prop);
		}
		if (selects.contains("UpdateTime".toLowerCase()))
		{
			OProperty<?> prop = OProperties.datetimeOffset("UpdateTime", act.getUpdateTime());		
			properties.add(prop);
		}
		if (selects.contains("AnomalyScore".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("AnomalyScore", act.getAnomalyScore());		
			properties.add(prop);
		}
		if (selects.contains("Description".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("Description", act.getDescription());		
			properties.add(prop);
		}
		if (selects.contains("Id".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("Id", act.getId());		
			properties.add(prop);
		}
		if (selects.contains("SourceType".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("SourceType", act.getSourceType());		
			properties.add(prop);
		}
		if (selects.contains("RelatedMetricCount".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("RelatedMetricCount", act.getRelatedMetricCount());		
			properties.add(prop);
		}
		if (selects.contains("HostCount".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("HostCount", act.getHostCount());		
			properties.add(prop);
		}
		if (selects.contains("SharedMetricPath".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("SharedMetricPath", act.getSharedMetricPath());		
			properties.add(prop);
		}
		
		return properties;
	}
	
	/**
	 * Create a list of OProperties from the RelatedMetric class
	 * 
	 * @param rm if <code>null</code> then a empty list is returned.
	 * @return List of OProperty
	 */
	static public List<OProperty<?>> relatedMetricOProperties(RelatedMetric rm)
	{
		if (rm == null)
		{
			return Collections.emptyList();
		}

		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

		OProperty<?> prop = OProperties.int32("EvidenceId", rm.getEvidenceId());		
		properties.add(prop);
		prop = OProperties.string("SourceType", rm.getSourceType());
		properties.add(prop);
		prop = OProperties.datetimeOffset("DateTime", rm.getDateTime());
		properties.add(prop);
		prop = OProperties.string("Description", rm.getDescription());
		properties.add(prop);
		prop = OProperties.int32("Count", rm.getCount());
		properties.add(prop);
		prop = OProperties.string("MetricPath", rm.getMetricPath());
		properties.add(prop);
		prop = OProperties.int32("Significance", rm.getSignificance());
		properties.add(prop);
		prop = OProperties.double_("Magnitude", rm.getMagnitude());
		properties.add(prop);

		return properties;
	}
	
	
	/**
	 * Create a list of OProperties from the RelatedMetric class only 
	 * returning the properties specified in <code>selectProps</code>.
	 * 
	 * @param rm
	 * @param selectProps Properties to select. If null or empty then 
	 * {@link #relatedMetricOProperties(RelatedMetric)} is called.
	 * @return
	 */
	static public List<OProperty<?>> relatedMetricOProperties(RelatedMetric rm, 
			List<EntitySimpleProperty> selectProps)
	{
		if (selectProps == null || selectProps.size() == 0)
		{
			return relatedMetricOProperties(rm);
		}

		if (rm == null)
		{
			return Collections.emptyList();
		}
		
		Set<String> selects = new HashSet<String>();
		for (EntitySimpleProperty prop : selectProps)
		{
			selects.add(prop.getPropertyName().toLowerCase());
		}

		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

		if (selects.contains("EvidenceId".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("EvidenceId", rm.getEvidenceId());		
			properties.add(prop);
		}
		if (selects.contains("SourceType".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("SourceType", rm.getSourceType());
			properties.add(prop);
		}
		if (selects.contains("DateTime".toLowerCase()))
		{
			OProperty<?> prop = OProperties.datetimeOffset("DateTime", rm.getDateTime());
			properties.add(prop);
		}
		if (selects.contains("Description".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("Description", rm.getDescription());
			properties.add(prop);
		}
		if (selects.contains("Count".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("Count", rm.getCount());
			properties.add(prop);
		}
		if (selects.contains("MetricPath".toLowerCase()))
		{
			OProperty<?> prop = OProperties.string("MetricPath", rm.getMetricPath());
			properties.add(prop);
		}
		if (selects.contains("Significance".toLowerCase()))
		{
			OProperty<?> prop = OProperties.int32("Significance", rm.getSignificance());
			properties.add(prop);
		}
		if (selects.contains("Magnitude".toLowerCase()))
		{
			OProperty<?> prop = OProperties.double_("Magnitude", rm.getMagnitude());
			properties.add(prop);
		}

		return properties;
	}
	
	
	/**
	 * Create a list of MetricConfig OProperties from a list of metric paths.
	 * Compression is always set to 'gzip' and Id = 1.
	 * 
	 * @param metricPaths
	 * @return List of OProperty
	 */
	static public List<OProperty<?>> metricConfigOProperties(List<String> metricPaths)
	{
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

		OProperty<?> prop = OProperties.int32("Id", 1);		
		properties.add(prop);
		prop = OProperties.int32("Count", metricPaths.size());
		properties.add(prop);
		prop = OProperties.string("Compression", "gzip");
		properties.add(prop);

		byte [] data = GzipByteArrayUtil.compress(MetricConfig.jsonEncodeMetricPaths(metricPaths));
		prop = OProperties.binary("MetricNames", data);
		properties.add(prop);

		return properties;
	}

	
	/**
	 * Create a list of OProperties from the MetricFeed class
	 * 
	 * @param metricFeed if <code>null</code> then a empty list is returned.
	 * @return List of OProperty
	 */
	static public List<OProperty<?>> metricFeedOProperties(MetricFeed metricFeed)
	{
		if (metricFeed == null)
		{
			return Collections.emptyList();
		}
		
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

		OProperty<?> prop = OProperties.int32("Id", metricFeed.getId());		
		properties.add(prop);
		prop = OProperties.string("Source", metricFeed.getSource());
		properties.add(prop);
		prop = OProperties.datetimeOffset("CollectionTime", new DateTime(metricFeed.getCollectionTime()));
		properties.add(prop);
		prop = OProperties.int32("Count", metricFeed.getCount());
		properties.add(prop);
		prop = OProperties.string("Compression", metricFeed.getCompression());
		properties.add(prop);
		prop = OProperties.binary("Data", metricFeed.getData());
		properties.add(prop);

		return properties;
	}
}
