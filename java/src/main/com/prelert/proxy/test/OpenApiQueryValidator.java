package com.prelert.proxy.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.proxy.plugin.openapi.QueryValidator;

/**
 * Test of the OpenAPI query validator.
 * Valid queries must have :StartTime & :EndTime tokens, 
 * DateTime and source fields and matching metrics, attributes
 * and keys. 
 */
public class OpenApiQueryValidator 
{
	@Test
	public void testValidate()
	{
		String allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		String timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";	
		assertTrue(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));
		
		// No source
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";	
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));
		
		// no datetime	
		allTimeSeriesQuery = "SELECT source AS Source, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT from V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));

		// no metric
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime FROM V_QOS_URL_DNSRESOLVE_TIME WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));

		// no start & end time
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) ";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));
		
		// EndTime instead of :EndTime
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, target AS Attribute_Target, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));

		// no Attribute_Target in all query.		
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, origin AS Attribute_Origin, robot AS Attribute_Robot, probe AS Attribute_Probe, compressed AS Attribute_Compressed, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND origin = :Attribute_Origin AND robot = :Attribute_Robot AND compressed = :Attribute_Compressed AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));
		
		// no Key_Target in all query.
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, origin AS Attribute_Origin samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Key_Target AND origin = :Attribute_Origin AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));
		
		// different metric 
		allTimeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, source AS Source, target AS Attribute_Target, samplevalue AS Metric_WEBLOGIC_EXECUTETHREADTOTALCOUNT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE DateDiff(Second, '19700101', sampletime) BETWEEN StartTime AND :EndTime";
		timeSeriesQuery = "SELECT DateDiff(Second, '19700101', sampletime) AS DateTime, samplevalue AS Metric_DIFFERENT FROM V_QOS_WEBLOGIC_EXECUTETHREADTOTALCOUNT WHERE source = :Source AND target = :Attribute_Target AND DateDiff(Second, '19700101', sampletime) BETWEEN :StartTime AND :EndTime";
		assertFalse(QueryValidator.validateOpenApiQueryPair(allTimeSeriesQuery, timeSeriesQuery));		
	}
}
