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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.odata4j.core.OProperty;

import com.prelert.data.Attribute;
import com.prelert.data.XmlStringEscaper;


public class MetricFeed 
{	
	/**
	 * The Json data must be encoded in this charater set.
	 */
	public static final String JSON_CHARACTER_SET = "UTF-8";
	
	public static final Logger s_Logger = Logger.getLogger(MetricFeed.class);
	/**
	 * Attribute Names
	 */
	public static final String AGENT_ATTRIBUTE = "Agent";
	public static final String PROCESS_ATTRIBUTE = "Process";
	public static final String RESOURCE_PATH_ATTRIBUTE = "ResourcePath";
	public static final String PATH_SEPARATOR = "|";
	public static final String METRIC_SEPARATOR = ":";
	
	
	/**
	 * XML tags
	 */
	public static final String TAGGED_POINTS_OPEN_TAG = "<tagged_points>";
	public static final String TAGGED_POINTS_CLOSE_TAG = "</tagged_points>";
	
	
	/**
	 * In the Json object notation metric paths are keyed by this string.
	 */
	final static public String METRIC = "m"; 
	
	/**
	 * In the Json object notation metric values are keyed by this string.
	 */
	final static public String VALUE = "d"; 
	
	private int	m_Id;
	private String m_Source;
	private DateTime m_CollectionTime;
	private int m_Count	= 0;
	private Compression	m_Compression = Compression.PLAIN;
	private List<MetricData> m_MetricData;
	private String m_XmlInvariants;

	public MetricFeed() 
	{
		m_MetricData = new ArrayList<MetricData>();
	}

	public String getCompression() 
	{
		return m_Compression.toString();
	}

	public void setCompression(String compression)
	{
		m_Compression = Compression.valueOf(compression.toUpperCase());
	}

	
	@EntityKey
	public int getId() 
	{
		return m_Id;
	}

	public void setId(int id) 
	{
		m_Id = id;
	}
	
	public String getSource() 
	{
		return m_Source;
	}

	public void setSource(String source) 
	{
		m_Source = source;
	}

	public DateTime getCollectionTime() 
	{
		return m_CollectionTime;
	}

	public void setCollectionTime(DateTime collectionTime) 
	{
		m_CollectionTime = collectionTime;
	}
	
	
	/**
	 * Encode Metric data as Json and return as an array of bytes.
	 * If compression is set to GZIP then compress the returned data.
	 * @return
	 */
	public byte[] getData()
	{
		JSONArray jsonArray = metricDataToJson();
		if (m_Compression == Compression.GZIP)
		{
			return GzipByteArrayUtil.compress(jsonArray.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
		}
		else 
		{
			return jsonArray.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET));
		}
	}

	
	/**
	 * If compression is set to <code>GZIP</code> then uncompress
	 * the raw data before parsing it as JSON and coverting it 
	 * to Metric data.
	 * @param data
	 */
	public void setData(byte[] data) 
	{
		byte[] buffer = data;
		if (m_Compression == Compression.GZIP && data.length != 0)
		{
			buffer = GzipByteArrayUtil.uncompress(data);
		}

		if (buffer != null)
		{
			parseJsonData(buffer);
		}
	}
	

	/**
	 * @return the metricCount
	 */
	public int getCount() 
	{
		return m_Count;
	}
	
	/**
	 * @param metricCount
	 *            the metricCount to set
	 */
	public void setCount(int count) 
	{
		m_Count = count;
	}
	
	
	@IgnoreProperty
	public List<MetricData> getMetricData()
	{
		return m_MetricData;
	}
	
	
	
	/**
	 * Overwrites any previously set data.
	 * 
	 * @param data
	 * @return
	 */
	// TODO horrible unchecked API look at other Json libraries 
	//@SuppressWarnings("unchecked")
	private boolean parseJsonData(byte [] data)
	{
		if (data.length == 0)
		{
			return false;
		}
		
		m_MetricData = new ArrayList<MetricData>();
		
		JSONArray jsonArrayIn = (JSONArray)JSONValue.parse(new String(data, Charset.forName(JSON_CHARACTER_SET)));
		for (int i=0; i<jsonArrayIn.size(); i++)
		{
			JSONObject jsonObj = (JSONObject)jsonArrayIn.get(i);
			
			String path = (String)jsonObj.get(METRIC);
			Object unknown = jsonObj.get(VALUE);
			if (unknown instanceof Double)
			{
				double value = (Double)jsonObj.get(VALUE);
				m_MetricData.add(new MetricData(path, value));
			}
			else if (unknown instanceof Long)
			{
				Long value = (Long)jsonObj.get(VALUE);
				m_MetricData.add(new MetricData(path, value.doubleValue()));
			}
		}
		
		return m_MetricData.size() > 0;
	}

	
	/**
	 * Convert the metric data to an array of JSON objects.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private JSONArray metricDataToJson()
	{
		JSONArray jsonArray = new JSONArray();
		
		for (MetricData md : m_MetricData)
		{
			jsonArray.add(md.toJson());
		}
		
		return jsonArray;
	}
	

	@Override
	public String toString() 
	{
		return String.format("MetricFeed[Id=%d,Time=%s,Source=%s, "
		        + "count: %d, compression: %s, MetricCount: %d]",
		        m_Id, m_CollectionTime, m_Source,
		        m_Count, m_Compression, m_MetricData.size());
	}
	
	/**
	 * Returns the Xml that is common to all points in the metric feed object.
	 * In this case it is only the <code>type</code> and <code>time</code> that
	 * are common. This function is an optimisation.
	 * @return
	 */
	@IgnoreProperty
	private String getXmlInvariants()
	{
		if (m_XmlInvariants == null || m_XmlInvariants.isEmpty())
		{
			StringBuilder invariants = new StringBuilder();
			invariants.append("<type>").append(XmlStringEscaper.escapeXmlString(getSource())).append("</type>");

			String timeStamp = new Long(getCollectionTime().getMillis() / 1000L).toString(); // seconds from epoch
			invariants.append("<time>").append(timeStamp).append("</time>"); // seconds since epoch
			
			m_XmlInvariants = invariants.toString();
		}
		
		return m_XmlInvariants;
	}
	
	
	/**
	 * Returns the Xml representation in the Prelert tagged points format of the
	 * metric data. Tagged points carry the time series type ID and time
	 * series ID in addition to the underlying point data.
	 * 
	 * @param seriesId Unique Time Series ID (from the database).
	 * @param typeId Unique Time Series Type ID (from the database).
	 * @param surroundWithTaggedPoints If true the time series points will be
	 * surrounded by <code>&lttagged_points&gt...&lt/tagged_point&gt</code>
	 * 
	 * @return The XML string representation of this object.
	 */
	public String toXmlStringExternal(MetricData dataPoint, int seriesId, int typeId, 
				boolean surroundWithTaggedPoints)
	{
		StringBuilder builder = new StringBuilder();
		if (surroundWithTaggedPoints)
		{
			builder.append(TAGGED_POINTS_OPEN_TAG);
		}
		builder.append("<tagged_point>");
		
		builder.append(getXmlInvariants());
		builder.append("<time_series_type_id>").append(typeId).append("</time_series_type_id>");
		builder.append("<time_series_id>").append(seriesId).append("</time_series_id>");

		MetricData.PathAttributes pathAttrs = dataPoint.getAttributes();
		if (pathAttrs == null)
		{
			s_Logger.error("Skipping unparsable metric path: " + dataPoint.getMetricPath());
			return "";
		}

		builder.append("<source>").append(XmlStringEscaper.escapeXmlString(pathAttrs.getHost())).append("</source>");
		builder.append("<metric>").append(XmlStringEscaper.escapeXmlString(pathAttrs.getMetric())).append("</metric>");

		for (Attribute attr : pathAttrs.m_Attributes)
		{
			builder.append(attr.toXmlTagExternal());
		}

		builder.append("<value>").append(dataPoint.getValue()).append("</value>");

		builder.append("</tagged_point>");
		if (surroundWithTaggedPoints)
		{
			builder.append(TAGGED_POINTS_CLOSE_TAG);
		}
		
		return builder.toString();
	}
	
	
	public static MetricFeed fromOProperties(List<OProperty<?>> props)
	{
		MetricFeed mf = new MetricFeed();
		
		// compression type must be set first before data
		for (OProperty<?> prop : props)
		{		
			if ("Compression".equals(prop.getName()))
			{
				mf.setCompression((String)prop.getValue());
				break;
			}
		}
		
		for (OProperty<?> prop : props)
		{
			if ("Id".equals(prop.getName()))
			{
				mf.setId((Integer)prop.getValue());
			}
			else if ("Source".equals(prop.getName()))
			{
				mf.setSource((String)prop.getValue());
			}
			else if ("CollectionTime".equals(prop.getName()))
			{
				mf.setCollectionTime((DateTime)prop.getValue());
			}
			else if ("Count".equals(prop.getName()))
			{
				mf.setCount((Integer)prop.getValue());
			}
			else if ("Data".equals(prop.getName()))
			{
				mf.setData((byte[])prop.getValue());
			}
		}	
		
		return mf;
	}
	
	
	/**
	 * The actual metric data. 
	 * Metric path is the full path including the metric
	 */
	public class MetricData
	{
		private String m_MetricPath;
		private double m_Value;
		
		public MetricData(String path, double value)
		{
			m_MetricPath = path;
			m_Value = value;
		}
		
		public String getMetricPath()
		{
			return m_MetricPath;
		}
		
		public double getValue()
		{
			return m_Value;
		}
		
		/**
		 * Class to group attributes, metric and source.
		 */
		public class PathAttributes
		{
			private String m_Host;
			private String m_Metric;
			private List<Attribute> m_Attributes;
			
			public PathAttributes(String source, String metric, 
					List<Attribute> attrs)
			{
				m_Host = source;
				m_Metric = metric;		
				m_Attributes = attrs;
			}
			
			public String getHost()
			{
				return m_Host;
			}
			
			public String getMetric()
			{
				return m_Metric;
			}
			
			public List<Attribute> getAttributes()
			{
				return m_Attributes;
			}
		}
		
		/**
		 * Return the metric from the metric path.
		 * @return
		 */
		public String getMetric()
		{
			String [] pathAndMetric = getMetricPath().split(METRIC_SEPARATOR);
			if (pathAndMetric.length < 2)
			{
				s_Logger.error("Invalid metric path has no metric" + getMetricPath());
				return null;
			}
			
			return pathAndMetric[1];
		}
		
		/**
		 * Get the attributes for this metric path and the source & 
		 * metric values
		 * @return
		 */
		public PathAttributes getAttributes()
		{
			List<Attribute> attrs = new ArrayList<Attribute>();
			
			String [] pathAndMetric = getMetricPath().split(METRIC_SEPARATOR);
			if (pathAndMetric.length != 2)
			{
				s_Logger.error("Invalid metric path is not metricpath:metric" + getMetricPath());
				return null;
			}
			String metric = pathAndMetric[1];
			
			String [] paths = pathAndMetric[0].split("\\" + PATH_SEPARATOR);
			if (paths.length < 3)
			{
				s_Logger.error("Cannot parse metric path " + getMetricPath());
				return null;
			}
			String source = paths[0];
			String process = paths[1];
			String agent = paths[2];
			
			attrs.add(new Attribute(AGENT_ATTRIBUTE, agent));
			attrs.add(new Attribute(PROCESS_ATTRIBUTE, process));
			
			
			for (int i=3; i<paths.length; i++)
			{
				String attrName = String.format("%s%d", RESOURCE_PATH_ATTRIBUTE, i - 2);
				attrs.add(new Attribute(attrName, paths[i]));
			}
			
			return new PathAttributes(source, metric, attrs);
		}

		
		/**
		 * Convert the metric data to a JSONObject
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public JSONObject toJson()
		{
			JSONObject obj = new JSONObject();
			obj.put(METRIC, this.m_MetricPath);
			obj.put(VALUE, this.m_Value);
			return obj;
		}
	}
	
}
