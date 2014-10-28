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
 ************************************************************/

package com.prelert.data;

import java.util.List;

import com.prelert.data.Attribute;


/**
 * Class encapsulating all the data of a time series i.e. its configuration
 * properties together with the data points.
 * @author Pete Harverson
 */
public class TimeSeriesData
{
	private TimeSeriesConfig			m_Config;
	private List<TimeSeriesDataPoint>	m_DataPoints;
	
	
	
	/**
	 * Creates a new object for time series data with the specified configuration
	 * and data points.
	 * @param config TimeSeriesConfig holding the configuration properties of the
	 * 			time series e.g. data type, metric, source.
	 * @param dataPoints the data points.
	 */
	public TimeSeriesData(TimeSeriesConfig config, List<TimeSeriesDataPoint> dataPoints)
	{
		m_Config = config;
		m_DataPoints = dataPoints;
	}


	/**
	 * Returns the configuration of this time series.
     * @return TimeSeriesConfig object holding the configuration properties of
	 * 			the time series e.g. data type, metric, source.
     */
    public TimeSeriesConfig getConfig()
    {
    	return m_Config;
    }


	/**
	 * Sets the configuration of this time series.
     * @param config TimeSeriesConfig object holding the configuration
	 * 			properties of  the time series e.g. data type, metric, source.
     */
    public void setConfig(TimeSeriesConfig config)
    {
    	m_Config = config;
    }


	/**
	 * Returns the list of data points in this time series.
     * @return the list of data points (time/value pairs).
     */
    public List<TimeSeriesDataPoint> getDataPoints()
    {
    	return m_DataPoints;
    }


	/**
	 * Sets the list of data points in this time series.
	 * @param dataPoints the list of data points (time/value pairs).
	 */
	public void setDataPoints(List<TimeSeriesDataPoint> dataPoints)
	{
		m_DataPoints = dataPoints;
	}
	

	/**
	 * Appends <code>dataPoints</dataPoints> to current list of points.
	 * @param dataPoints
	 */
	public void addDataPoints(List<TimeSeriesDataPoint> dataPoints)
	{
		m_DataPoints.addAll(dataPoints);
	}
	
	/**
	 * Returns an xml representation in the Prelert tagged points format of the
	 * time series data.   Tagged points carry the time series type ID and time
	 * series ID in addition to the underlying point data.
	 * @param seriesId Unique Time Series ID (from the database).
	 * @param typeId Unique Time Series Type ID (from the database).
	 * @return The XML string representation of this object.
	 */
	public String toXmlStringExternal(int seriesId, int typeId)
	{
		StringBuilder invariants = new StringBuilder("<tagged_point>");
		invariants.append("<type>").append(XmlStringEscaper.escapeXmlString(m_Config.getDataType())).append("</type>");
		invariants.append("<source>").append(XmlStringEscaper.escapeXmlString(m_Config.getSource())).append("</source>");
		invariants.append("<metric>").append(XmlStringEscaper.escapeXmlString(m_Config.getMetric())).append("</metric>");
		invariants.append("<time_series_type_id>").append(typeId).append("</time_series_type_id>");
		invariants.append("<time_series_id>").append(seriesId).append("</time_series_id>");

		List<Attribute> attributeList = m_Config.getAttributes();
		if (attributeList != null)
		{
			for (Attribute attribute : attributeList)
			{
				invariants.append(attribute.toXmlTagExternal());
			}
		}

		StringBuilder builder = new StringBuilder("<tagged_points>");

		for (TimeSeriesDataPoint point : m_DataPoints)
		{
			builder.append(invariants);

			builder.append("<time>").append(point.getTime() / 1000l).append("</time>"); // seconds since epoch
			builder.append("<value>").append(point.getValue()).append("</value>");

			builder.append("</tagged_point>");
		}

		builder.append("</tagged_points>");

		return builder.toString();
	}


	/**
	 * Returns an xml representation in the Prelert points format of the
	 * time series data.
	 * This representation allows Prelert to store the points internally
	 * in the database and has optional xml attributes to determine the
	 * ordering the attributes on the metric path.
	 *
	 * @param addOuterPointsTag If true then Xml string will be surrounded
	 *                          with <points></points>
	 * @return The XML string representation of this object.
	 */
	public String toXmlStringInternal(boolean addOuterPointsTag)
	{
		StringBuilder invariants = new StringBuilder("<point>");
		invariants.append("<type>").append(XmlStringEscaper.escapeXmlString(m_Config.getDataType())).append("</type>");
		
		invariants.append("<source");
		if (m_Config.getSourcePrefix() != null)			
		{
			invariants.append(" prefix='").append(m_Config.getSourcePrefix()).append("'");
		}
		if (m_Config.getSourcePosition() >= 0)			
		{
			invariants.append(" position='").append(m_Config.getSourcePosition()).append("'");
		}		
		invariants.append('>').append(XmlStringEscaper.escapeXmlString(m_Config.getSource())).append("</source>");
		
		
		invariants.append("<metric");
		if (m_Config.getMetricPrefix() != null)
		{
			invariants.append(" prefix='").append(m_Config.getMetricPrefix()).append("'");
		}		
		if (m_Config.getMetricPosition() >= 0)			
		{
			invariants.append(" position='").append(m_Config.getMetricPosition()).append("'");
		}
		invariants.append('>').append(XmlStringEscaper.escapeXmlString(m_Config.getMetric())).append("</metric>");

		List<Attribute> attributeList = m_Config.getAttributes();
		if (attributeList != null)
		{
			for (Attribute attribute : attributeList)
			{
				invariants.append(attribute.toXmlTagInternal());
			}
		}

		StringBuilder builder = new StringBuilder();
		if (addOuterPointsTag)
		{
			builder.append("<points>");
		}

		for (TimeSeriesDataPoint point : m_DataPoints)
		{
			builder.append(invariants);

			builder.append("<time>").append(point.getTime() / 1000l).append("</time>"); // seconds since epoch
			builder.append("<value>").append(point.getValue()).append("</value>");

			builder.append("</point>");
		}

		if (addOuterPointsTag)
		{
			builder.append("</points>");
		}

		return builder.toString();
	}


	/**
	 * Calls toXmlStringInternal(true).
	 *
	 * @return The XML string representation of this object within
	 * 			<points></points> tags.
	 */
	public String toXmlStringInternal()
	{
		return toXmlStringInternal(true);
	}
	
}
