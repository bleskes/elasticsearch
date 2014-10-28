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

package com.prelert.api.test;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.ExpressionParser;

import com.prelert.api.QueryAPI;
import com.prelert.api.data.OdataExpressionVisitor;
import com.prelert.api.data.LogicalOperator;
import com.prelert.api.data.OdataExpressionVisitor.ExpressionType;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Test of the ODATA expression parser.
 * 
 *  Only equality expressions involving 'anomalyScore', 'metricpath',
 *  'earliestDate' and 'latestDate' are processed. 
 *
 *  The expressions themselves are generated using the ODATA 
 *  ExpressionParser utility.
 */
public class ExpressionParserTest 
{
	/**
	 * Test the anomaly threshold filter options.
	 */
	@Test
	public void parseThresholdFilter()
	{
		String [] filterValues = {"20", "20.0d", "20.0f"};
		for (String filterValue : filterValues)
		{
			String query = "anomalyScore ge " + filterValue;

			CommonExpression exp = ExpressionParser.parse(query);

			OdataExpressionVisitor parser = new OdataExpressionVisitor();
			exp.visit(parser);

			assertTrue(parser.isThresholdFilter());
			assertTrue(parser.getThresholdValue() == 20);
			assertTrue(parser.isMetricPathFilter() == false);

			assertTrue(parser.getMinFirstEvidenceTime() == null);
			assertTrue(parser.getMaxFirstEvidenceTime() == null);
			assertTrue(parser.getMinLastEvidenceTime() == null);
			assertTrue(parser.getMaxLastEvidenceTime() == null);
			assertTrue(parser.getMinPeakEvidenceTime() == null);
			assertTrue(parser.getMaxPeakEvidenceTime() == null);
			assertTrue(parser.getMaxUpdateTime() == null);
			assertTrue(parser.getMinUpdateTime() == null);		
			assertTrue(parser.getMetricPathLikeAnds().size() == 0);		
		}
		
		
		String [] operators = {"ne", "le", "lt"};
		for (String op : operators)
		{
			String query = String.format("anomalyScore %s 20", op);
			try
			{
				OdataExpressionVisitor parser = new OdataExpressionVisitor();
				CommonExpression exp = ExpressionParser.parse(query);
				exp.visit(parser);
				assertTrue("Parser should throw UnsupportedOperationException for filter " 
						+ query, false);
			}
			catch (UnsupportedOperationException e)
			{

			}
		}
	}
	
	
	/**
	 * Test the metric path filter options.
	 * @throws ParseException 
	 */
	@Test
	public void parseMetricPathFilter() throws ParseException
	{
		String query = "metricpath eq 'A|B|C'";
		CommonExpression exp = ExpressionParser.parse(query);
		
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.isThresholdFilter() == false);
		
		
		// Test invalid operators
		String [] operators = new String[] {"ne", "le", "lt", "ge", "gt"};
		for (String op : operators)
		{
			query = String.format("metricPath %s 'A|B|C'", op);
			try
			{
				parser = new OdataExpressionVisitor();
				exp = ExpressionParser.parse(query);
				exp.visit(parser);
				assertTrue("Parser should throw UnsupportedOperationException for filter " 
						+ query, false);
			}
			catch (UnsupportedOperationException e)
			{

			}
		}
		
		
		// Test anded metric paths
		query = "metricpath eq 'A|B|C' and metricPath eq 'a|b|D' and metricpath eq 'E|F|G'";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 3);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getMetricPathAnds().get(1).equals("a|b|D"));
		assertTrue(parser.getMetricPathAnds().get(2).equals("E|F|G"));
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.isThresholdFilter() == false);
		
		// Test anded metric paths in brackets
		query = "(metricpath eq 'A|B|C') and (metricPath eq 'a|b|D')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 2);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getMetricPathAnds().get(1).equals("a|b|D"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.isThresholdFilter() == false);
		
		
		// Fuller query
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		query = "(metricpath eq 'A|B|C') and (metricPath eq 'a|b|D') and (metricpath eq 'E|F|G')" + 
		" and PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50)";
		
		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 3);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getMetricPathAnds().get(1).equals("a|b|D"));
		assertTrue(parser.getMetricPathAnds().get(2).equals("E|F|G"));
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		
		
		// Or metric paths
		// Test anded metric paths in brackets
		query = "(metricpath eq 'A|B|C') or (metricPath eq 'a|b|D')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.getMetricPathAnds().size() == 0);
		assertTrue(parser.getOredExpressions().size() == 2);
		assertTrue(parser.getOredExpressions().get(0).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(0).getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().get(1).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(1).getMetricPathAnds().get(0).equals("a|b|D"));
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.isThresholdFilter() == false);
		
		//no brackets
		query = "metricpath eq 'A|B|C' or metricPath eq 'a|b|D' or metricPath eq 'E|F|G'";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.getMetricPathAnds().size() == 0);
		assertTrue(parser.getOredExpressions().size() == 3);
		assertTrue(parser.getOredExpressions().get(0).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(0).getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().get(1).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(1).getMetricPathAnds().get(0).equals("a|b|D"));
		assertTrue(parser.getOredExpressions().get(2).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(2).getMetricPathAnds().get(0).equals("E|F|G"));
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.isThresholdFilter() == false);
		
		
		// more compliated or query
		query = "PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50) and " +
		"((metricpath eq 'A|B|C') or (metricPath eq 'a|b|D') or (metricPath eq 'E|F|G'))";

		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isEscapeCharFilter() == false);
		
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.getMetricPathAnds().size() == 0);
		assertTrue(parser.getOredExpressions().size() == 3);
		assertTrue(parser.getOredExpressions().get(0).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(0).getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().get(1).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(1).getMetricPathAnds().get(0).equals("a|b|D"));
		assertTrue(parser.getOredExpressions().get(2).isMetricPathFilter());
		assertTrue(parser.getOredExpressions().get(2).getMetricPathAnds().get(0).equals("E|F|G"));


		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		
	}
	
	
	/**
	 * Test parsing brackets in filter strings.
	 */
	@Test
	public void parseBrackets()
	{
		// test bracketing
		String [] queries = {"(metricpath eq 'A|B|C') and (anomalyScore ge 20)", "metricpath eq 'A|B|C' and anomalyScore ge 20"};
		for (String q : queries)
		{
			CommonExpression exp = ExpressionParser.parse(q);

			OdataExpressionVisitor parser = new OdataExpressionVisitor();
			exp.visit(parser);

			assertTrue(parser.isThresholdFilter() == true);
			assertTrue(parser.getThresholdValue() == 20);
			assertTrue(parser.isMetricPathFilter() == true);
			assertTrue(parser.getMetricPathAnds().size() == 1);
			assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));

			assertTrue(parser.getMinFirstEvidenceTime() == null);
			assertTrue(parser.getMaxFirstEvidenceTime() == null);
			assertTrue(parser.getMinLastEvidenceTime() == null);
			assertTrue(parser.getMaxLastEvidenceTime() == null);
			assertTrue(parser.getMinPeakEvidenceTime() == null);
			assertTrue(parser.getMaxPeakEvidenceTime() == null);
			assertTrue(parser.getMaxUpdateTime() == null);
			assertTrue(parser.getMinUpdateTime() == null);		
			assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		}
	}
	
	
	/**
	 * Test the peakevidencetime filter options with a timezone.
	 * Run the tests in different timezones.
	 */
	@Test
	public void parseDateTimeOffset() throws ParseException 
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z");
		
		// specify peakevidencetime in a different timezone
		String query = "peakevidencetime gt datetimeoffset'2012-02-29T12:18:45+05:00'";
		CommonExpression exp = ExpressionParser.parse(query);

		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45 +0000")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GT);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		
		// same again but the comparison time is in a different time zone
		query = "peakevidencetime gt datetimeoffset'2012-02-29T12:18:45+05:00'";
		exp = ExpressionParser.parse(query);

		parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45 +0000")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GT);
		
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		
		String [] timeZones = {"CET", "EST", "UTC"};
		
		for (String zone : timeZones)
		{
			TimeZone tmz = TimeZone.getTimeZone(zone);
			dateFormat.setTimeZone(tmz);

			// test greater than again but without the timezone offset
			query = "peakevidencetime gt datetimeoffset'2012-02-29T07:18:45+05:00'";
			exp = ExpressionParser.parse(query);

			parser = new OdataExpressionVisitor();
			exp.visit(parser);

			assertTrue(parser.isThresholdFilter() == false);
			assertTrue(parser.isMetricPathFilter() == false);
			
			assertTrue(parser.getMinFirstEvidenceTime() == null);
			assertTrue(parser.getMaxFirstEvidenceTime() == null);
			assertTrue(parser.getMinLastEvidenceTime() == null);
			assertTrue(parser.getMaxLastEvidenceTime() == null);
			assertTrue(parser.getMaxPeakEvidenceTime() == null);
			
			assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45 +0500")));
			assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GT);
			
			assertTrue(parser.getMaxUpdateTime() == null);
			assertTrue(parser.getMinUpdateTime() == null);	
			
			assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
			


			query = "peakevidencetime lt datetimeoffset'2012-02-29T07:18:45-11:00'";
			exp = ExpressionParser.parse(query);

			parser = new OdataExpressionVisitor();
			exp.visit(parser);

			assertTrue(parser.isThresholdFilter() == false);
			assertTrue(parser.isMetricPathFilter() == false);
			
			
			assertTrue(parser.getMinFirstEvidenceTime() == null);
			assertTrue(parser.getMaxFirstEvidenceTime() == null);
			assertTrue(parser.getMinLastEvidenceTime() == null);
			assertTrue(parser.getMaxLastEvidenceTime() == null);
			assertTrue(parser.getMinPeakEvidenceTime() == null);
			
			assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45 -1100")));
			assertTrue(parser.getMaxPeakEvidenceTime().getOperator() == LogicalOperator.LT);
			
			assertTrue(parser.getMaxUpdateTime() == null);
			assertTrue(parser.getMinUpdateTime() == null);	
			
			assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
			
			
					
			
			query = "peakevidencetime gt datetimeoffset'2012-02-29T07:18:45+00:00'";
			exp = ExpressionParser.parse(query);

			parser = new OdataExpressionVisitor();
			exp.visit(parser);

			assertTrue(parser.isThresholdFilter() == false);
			assertTrue(parser.isMetricPathFilter() == false);
			
			assertTrue(parser.getMinFirstEvidenceTime() == null);
			assertTrue(parser.getMaxFirstEvidenceTime() == null);
			assertTrue(parser.getMinLastEvidenceTime() == null);
			assertTrue(parser.getMaxLastEvidenceTime() == null);
			
			assertTrue(parser.getMaxPeakEvidenceTime() == null);
			assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45 +0000")));
			assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GT);
			
			assertTrue(parser.getMaxUpdateTime() == null);
			assertTrue(parser.getMinUpdateTime() == null);		
			
			assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		}
		
	}
	

	/**
	 * Test the evidence time filter options.
	 * 
	 * @throws ParseException 
	 */
	@Test
	public void parsePeakEvidenceTime() throws ParseException 
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		// test greater than
		String query = "peakevidencetime ge datetimeoffset'2012-02-29T07:18:45Z'";
		CommonExpression exp = ExpressionParser.parse(query);
		
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GE);
		
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
			
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		// test fuller query
		query = "(metricpath eq 'A|B|C:d') and (anomalyScore ge 20) and (peakevidencetime ge datetimeoffset'2012-02-29T07:18:45Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C:d"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GE);
		
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		

		// between 2 dates
		query = "(metricpath eq 'A|B|C') and (anomalyScore ge 20) and (peakevidencetime le datetimeoffset'2012-02-29T19:30:00Z') " +
				"and (peakevidencetime ge datetimeoffset'2012-02-29T07:18:45Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 19:30:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getOperator() == LogicalOperator.LE);
		
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GE);

		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		

		// test less than
		query = "(metricpath eq 'A|B|C') and (anomalyScore ge 20) and (peakevidencetime gt datetimeoffset'2012-02-29T07:18:45Z') " +
				"and (peakEvidenceTime lt datetimeoffset'2012-02-29T19:30:00Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 19:30:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getOperator() == LogicalOperator.LT);
		
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinPeakEvidenceTime().getOperator() == LogicalOperator.GT);

		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
	
		
		// ne expression should throw
		try
		{
			query = "peakevidencetime ne datetimeoffset'2012-02-29T07:18:45Z'";
			parser = new OdataExpressionVisitor();
			exp = ExpressionParser.parse(query);
			exp.visit(parser);
			assertTrue("Parser should throw UnsupportedOperationException for filter " 
					+ query, false);
		}
		catch (UnsupportedOperationException e)
		{

		}
	}

	
	
	/**
	 * Test firstEvidenceTime & lastEvidenceTime queries
	 * 
	 * @throws ParseException 
	 */
	@Test
	public void parseFirst_LastEvidenceTime() throws ParseException 
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		// test greater than
		String query = "firstEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z'";
		CommonExpression exp = ExpressionParser.parse(query);
		
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinFirstEvidenceTime().getOperator() == LogicalOperator.GE);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
			
		
		// test fuller query
		query = "(metricpath eq 'A|B|C:d') and (anomalyScore ge 20) and (firstEvidenceTime le datetimeoffset'2012-02-29T07:18:45Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C:d"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LE);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		
		// between 2 dates
		query = "(metricpath eq 'A|B|C') and (anomalyScore ge 20) and (FirstEvidenceTime le datetimeoffset'2012-02-29T19:30:00Z') " +
				"and (lastEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 19:30:00")));
		assertTrue(parser.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LE);
		
		assertTrue(parser.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinLastEvidenceTime().getOperator() == LogicalOperator.GE);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		

		// test less than
		query = "(metricpath eq 'A|B|C') and (anomalyScore ge 20) and (lastEvidenceTime gt datetimeoffset'2012-02-29T07:18:45Z') " +
				"and (firstevidencetime lt datetimeoffset'2012-02-29T19:30:00Z')";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 20);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 19:30:00")));
		assertTrue(parser.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LT);
		
		assertTrue(parser.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinLastEvidenceTime().getOperator() == LogicalOperator.GT);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
	}
	
	
	
	/**
	 * Test update time queries
	 * 
	 */
	@Test
	public void parseUpdateTime() throws ParseException 
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

		// test greater than
		String query = "updatetime ge datetimeoffset'2012-02-29T07:18:45Z'";
		CommonExpression exp = ExpressionParser.parse(query);
		
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);

		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinUpdateTime().getOperator() == LogicalOperator.GE);
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
			
		
		// test fuller query
		query = "(metricpath eq 'A|B|C') and updatetime ge datetimeoffset'2012-02-29T07:18:45Z' and updatetime le datetimeoffset'2012-02-29T06:00:45Z'";
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		
		assertTrue(parser.getMinUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinUpdateTime().getOperator() == LogicalOperator.GE);
		assertTrue(parser.getMaxUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 06:00:45")));
		assertTrue(parser.getMaxUpdateTime().getOperator() == LogicalOperator.LE);
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		// test query with 2 'updatetime ge' statements - selects the second one but this isn't entirely predictable.
		query = "(metricpath eq 'A|B|C') and (updatetime ge datetimeoffset'2012-02-29T07:18:45Z') and (updatetime le datetimeoffset'2012-02-29T06:00:45Z')" +
			" and (updatetime gt datetimeoffset'2012-02-29T08:16:45Z')";
		
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		
		assertTrue(parser.getMinUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 08:16:45")));
		assertTrue(parser.getMinUpdateTime().getOperator() == LogicalOperator.GT);
		assertTrue(parser.getMaxUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 06:00:45")));
		assertTrue(parser.getMaxUpdateTime().getOperator() == LogicalOperator.LE);
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		
		// overly complex query
		query = "(metricpath eq 'A|B|C') and (updatetime ge datetimeoffset'2012-02-29T07:18:45Z') and (firstEvidenceTime le datetimeoffset'2012-02-29T13:00:45Z')" +
			" and (lastEvidenceTime gt datetimeoffset'2012-02-29T08:16:45Z')";
		
		exp = ExpressionParser.parse(query);
		
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == true);
		assertTrue(parser.getMetricPathAnds().size() == 1);
		assertTrue(parser.getMetricPathAnds().get(0).equals("A|B|C"));
		assertTrue(parser.getOredExpressions().size() == 0);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 13:00:45")));
		assertTrue(parser.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LE);
		
		assertTrue(parser.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 08:16:45")));
		assertTrue(parser.getMinLastEvidenceTime().getOperator() == LogicalOperator.GT);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		
		assertTrue(parser.getMinUpdateTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinUpdateTime().getOperator() == LogicalOperator.GE);
		assertTrue(parser.getMaxUpdateTime() == null);
		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
	}
	
	
	/**
	 * Test metric path like filter and escapechar
	 */
	@Test
	public void parseMetricPathLikeFilter() throws ParseException 
	{
		String query = "escapeChar eq '\\' and MPQuery eq '%:\\% CPU'";
		CommonExpression exp = ExpressionParser.parse(query);

		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isEscapeCharFilter());
		assertTrue(parser.getEscapeCharValue().equals("\\"));
		assertTrue(parser.isMetricPathLikeFilter());
		assertTrue(parser.getMetricPathLikeAnds().size() == 1);
		assertTrue(parser.getMetricPathLikeAnds().get(0).equals("%:\\% CPU"));
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	

		// no escape char
		query = "mpquery eq '%:% CPU'";
		exp = ExpressionParser.parse(query);

		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.isMetricPathLikeFilter());
		assertTrue(parser.getMetricPathLikeAnds().size() == 1);
		assertTrue(parser.getMetricPathLikeAnds().get(0).equals("%:% CPU"));
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		
		query = "((MPQuery eq '%WebSphere_MQ%') or (MPQuery eq '%|Backends|%'))";
		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.getExpressionType() == ExpressionType.OR);
		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		List<OdataExpressionVisitor> ors = parser.getOredExpressions();
		assertTrue(ors.size() == 2);
		
		assertTrue(ors.get(0).isMetricPathLikeFilter());
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%WebSphere_MQ%"));
		assertTrue(ors.get(1).isMetricPathLikeFilter());
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|Backends|%"));

		

		
		
		query = "(AnomalyScore ge 50) and ((mpquery eq '%:% CPU') or (mpquery eq '%|%|%|%|%|%|Trade Service|%|Login:%'))";
		exp = ExpressionParser.parse(query);

		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isEscapeCharFilter() == false);
		
		ors = parser.getOredExpressions();
		assertTrue(ors.size() == 2);
		
		assertTrue(ors.get(0).isMetricPathLikeFilter());
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%:% CPU"));
		assertTrue(ors.get(1).isMetricPathLikeFilter());
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	

		
		query = "(AnomalyScore ge 50) and escapeChar eq '<' and ((mpquery eq '%:% CPU') or (mpquery eq '%|%|%|%|%|%|Trade Service|%|Login:%')) and (firstEvidenceTime le datetimeoffset'2012-02-29T13:00:45Z')";
		exp = ExpressionParser.parse(query);

		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		assertTrue(parser.isEscapeCharFilter() == true);
		assertTrue(parser.getEscapeCharValue().equals("<"));
		
		ors = parser.getOredExpressions();
		assertTrue(ors.size() == 2);
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%:% CPU"));		
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		

		
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 13:00:45")));
		assertTrue(parser.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LE);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);	
		
		
		query = "PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
				" and (AnomalyScore ge 50) and " +
				" ((MPQuery eq '%|%|%|%|Trade Service|%|Login:%') or (MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%')" +
				" or (MPQuery eq '%|%|%|%|Trade  Service|Login|%') or (MPQuery eq '%|%|%|%|%|%|Trade Service|Login%'))";
		
		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.isEscapeCharFilter() == false);
		
		ors = parser.getOredExpressions();
		assertTrue(ors.size() == 4);
		assertTrue(ors.get(0).isMetricPathLikeFilter());
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ors.get(1).isMetricPathLikeFilter());
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ors.get(2).isMetricPathLikeFilter());
		assertTrue(ors.get(2).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(2).getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ors.get(3).isMetricPathLikeFilter());
		assertTrue(ors.get(3).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(3).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|Login%"));		
				
		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);
		
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		
		
		// As above but or's aren't bracketed 
		query = "PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50) and " +
		"(MPQuery eq '%|%|%|%|Trade Service|%|Login:%' or MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'" +
		" or MPQuery eq '%|%|%|%|Trade  Service|Login|%' or MPQuery eq '%|%|%|%|%|%|Trade Service|Login%')";

		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isEscapeCharFilter() == false);

		ors = parser.getOredExpressions();
		assertTrue(ors.size() == 4);
		assertTrue(ors.get(0).isMetricPathLikeFilter());
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ors.get(1).isMetricPathLikeFilter());
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ors.get(2).isMetricPathLikeFilter());
		assertTrue(ors.get(2).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(2).getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ors.get(3).isMetricPathLikeFilter());
		assertTrue(ors.get(3).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(3).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|Login%"));	

		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		
		
		// And tests

		query = "MPQuery eq '%|%|%|%|Trade Service|%|Login:%' and MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'" +
			" and MPQuery eq '%|%|%|%|Trade  Service|Login|%' and MPQuery eq '%|%|%|%|%|%|Trade Service|Login%'";
		
		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.isMetricPathLikeFilter());
		assertTrue(parser.getOredExpressions().size() == 0);
		
		List<String> ands = parser.getMetricPathLikeAnds();
		assertTrue(ands.size() == 4);
		assertTrue(ands.get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(1).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(2).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ands.get(3).equals("%|%|%|%|%|%|Trade Service|Login%"));		

		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
		
		
		// more complicated and
		query = "PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50) and (escapeChar eq '<') and " +
		"MPQuery eq '%|%|%|%|Trade Service|%|Login:%' and MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'" +
		" and MPQuery eq '%|%|%|%|Trade  Service|Login|%' and MPQuery eq '%|%|%|%|%|%|Trade Service|Login%'";

		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);

		assertTrue(parser.isEscapeCharFilter() == true);
		assertTrue(parser.getEscapeCharValue().equals("<"));

		assertTrue(parser.getOredExpressions().size() == 0);
		
		ands = parser.getMetricPathLikeAnds();
		assertTrue(ands.size() == 4);
		assertTrue(ands.get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(1).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(2).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ands.get(3).equals("%|%|%|%|%|%|Trade Service|Login%"));		

		assertTrue(parser.isThresholdFilter() == true);
		assertTrue(parser.getThresholdValue() == 50);
		assertTrue(parser.isMetricPathFilter() == false);

		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(parser.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);
	}
		
	
	/**
	 * Test complex queries or'd together
	 * @throws ParseException 
	 */
	@Test
	public void parseLargeOrQuery() throws ParseException
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		String query = "(PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50) and (escapeChar eq '<') and " +
		"MPQuery eq '%|%|%|%|Trade Service|%|Login:%' and MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'" +
		" and MPQuery eq '%|%|%|%|Trade  Service|Login|%' and MPQuery eq '%|%|%|%|%|%|Trade Service|Login%') " +
		" or " +
		"(AnomalyScore ge 61)";
		
		
		CommonExpression exp = ExpressionParser.parse(query);
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.getExpressionType() == ExpressionType.OR);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.isMetricPathLikeFilter() == false);
		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.getOredExpressions().size() == 2);
		
		OdataExpressionVisitor firstQuery = parser.getOredExpressions().get(0);
		
		List<String>ands = firstQuery.getMetricPathLikeAnds();
		assertTrue(ands.size() == 4);
		assertTrue(ands.get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(1).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(2).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ands.get(3).equals("%|%|%|%|%|%|Trade Service|Login%"));		

		assertTrue(firstQuery.isThresholdFilter() == true);
		assertTrue(firstQuery.getThresholdValue() == 50);
		assertTrue(firstQuery.isMetricPathFilter() == false);
		assertTrue(firstQuery.isMetricPathLikeFilter());

		assertTrue(firstQuery.getMinFirstEvidenceTime() == null);
		assertTrue(firstQuery.getMaxFirstEvidenceTime() == null);
		assertTrue(firstQuery.getMinLastEvidenceTime() == null);
		assertTrue(firstQuery.getMaxLastEvidenceTime() == null);
		assertTrue(firstQuery.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(firstQuery.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(firstQuery.getMaxUpdateTime() == null);
		assertTrue(firstQuery.getMinUpdateTime() == null);
		
		
		OdataExpressionVisitor secondQuery = parser.getOredExpressions().get(1);
		
		assertTrue(secondQuery.isThresholdFilter() == true);
		assertTrue(secondQuery.getThresholdValue() == 61);
		assertTrue(secondQuery.isMetricPathFilter() == false);
		assertTrue(secondQuery.isMetricPathLikeFilter() == false);
		assertTrue(secondQuery.isEscapeCharFilter() == false);
		assertTrue(secondQuery.getMinFirstEvidenceTime() == null);
		assertTrue(secondQuery.getMaxFirstEvidenceTime() == null);
		assertTrue(secondQuery.getMinLastEvidenceTime() == null);
		assertTrue(secondQuery.getMaxLastEvidenceTime() == null);
		assertTrue(secondQuery.getMinPeakEvidenceTime() == null);
		assertTrue(secondQuery.getMaxPeakEvidenceTime() == null);
		assertTrue(secondQuery.getMaxUpdateTime() == null);
		assertTrue(secondQuery.getMinUpdateTime() == null);		
		assertTrue(secondQuery.getMetricPathLikeAnds().size() == 0);	
		assertTrue(secondQuery.getMetricPathLikeAnds().size() == 0);
		
	
		
		
		
		// 2 big queries or'd together
		query = "(PeakEvidenceTime ge datetimeoffset'2013-01-25T00:00:00Z' and PeakEvidenceTime le datetimeoffset'2013-01-30T00:00:00Z'" +
		" and (AnomalyScore ge 50) and (escapeChar eq '<') and " +
		"MPQuery eq '%|%|%|%|Trade Service|%|Login:%' and MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'" +
		" and MPQuery eq '%|%|%|%|Trade  Service|Login|%' and MPQuery eq '%|%|%|%|%|%|Trade Service|Login%') " +
		" or " +
		"(firstEvidenceTime le datetimeoffset'2012-02-29T07:18:45Z' and AnomalyScore ge 61 and " +
		"lastEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z' and " + 
		"(MPQuery eq '%|%|%|%|Trade Service|%|Login:%' or MPQuery eq '%|%|%|%|%|%|Trade Service|%|Login:%'))";
		
		
		exp = ExpressionParser.parse(query);
		parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		assertTrue(parser.getExpressionType() == ExpressionType.OR);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.isMetricPathLikeFilter() == false);
		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime() == null);
		assertTrue(parser.getMinLastEvidenceTime() == null);
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathAnds().size() == 0);	
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		
		assertTrue(parser.getOredExpressions().size() == 2);
		
		firstQuery = parser.getOredExpressions().get(0);
		
		ands = firstQuery.getMetricPathLikeAnds();
		assertTrue(ands.size() == 4);
		assertTrue(ands.get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(1).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ands.get(2).equals("%|%|%|%|Trade  Service|Login|%"));
		assertTrue(ands.get(3).equals("%|%|%|%|%|%|Trade Service|Login%"));		

		assertTrue(firstQuery.isThresholdFilter() == true);
		assertTrue(firstQuery.getThresholdValue() == 50);
		assertTrue(firstQuery.isMetricPathFilter() == false);
		assertTrue(firstQuery.isMetricPathLikeFilter());
		assertTrue(firstQuery.getMinFirstEvidenceTime() == null);
		assertTrue(firstQuery.getMaxFirstEvidenceTime() == null);
		assertTrue(firstQuery.getMinLastEvidenceTime() == null);
		assertTrue(firstQuery.getMaxLastEvidenceTime() == null);
		assertTrue(firstQuery.getMinPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-25 00:00:00")));
		assertTrue(firstQuery.getMaxPeakEvidenceTime().getDateTime().equals(dateFormat.parse("2013-01-30 00:00:00")));
		assertTrue(firstQuery.getMaxUpdateTime() == null);
		assertTrue(firstQuery.getMinUpdateTime() == null);
		assertTrue(parser.getMetricPathAnds().size() == 0);	
		
		
		secondQuery = parser.getOredExpressions().get(1);
		
		List<OdataExpressionVisitor> ors = secondQuery.getOredExpressions();
		assertTrue(ors.size() == 2);
		assertTrue(ors.get(0).isMetricPathLikeFilter());
		assertTrue(ors.get(0).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(0).getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(ors.get(1).isMetricPathLikeFilter());
		assertTrue(ors.get(1).getMetricPathLikeAnds().size() == 1);
		assertTrue(ors.get(1).getMetricPathLikeAnds().get(0).equals("%|%|%|%|%|%|Trade Service|%|Login:%"));
	
		
		assertTrue(secondQuery.isThresholdFilter() == true);
		assertTrue(secondQuery.getThresholdValue() == 61);
		assertTrue(secondQuery.isMetricPathFilter() == false);
		assertTrue(secondQuery.isMetricPathLikeFilter() == false);
		assertTrue(secondQuery.isEscapeCharFilter() == false);
		assertTrue(secondQuery.getMinFirstEvidenceTime() == null);
		assertTrue(secondQuery.getMaxFirstEvidenceTime().getOperator() == LogicalOperator.LE);
		assertTrue(secondQuery.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(secondQuery.getMinLastEvidenceTime().getOperator() == LogicalOperator.GE);
		assertTrue(secondQuery.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(secondQuery.getMaxLastEvidenceTime() == null);
		assertTrue(secondQuery.getMinPeakEvidenceTime() == null);
		assertTrue(secondQuery.getMaxPeakEvidenceTime() == null);
		assertTrue(secondQuery.getMaxUpdateTime() == null);
		assertTrue(secondQuery.getMinUpdateTime() == null);		
		assertTrue(secondQuery.getMetricPathAnds().size() == 0);	
		assertTrue(secondQuery.getMetricPathLikeAnds().size() == 0);	
	}
	
	
	/**
	 * 
	 * @throws ParseException 
	 */
	@Test
	public void parseAndMergeComplexQueriesTest() throws ParseException
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		
		// This will be parsed as 2 separate queries.
		String query = "(firstEvidenceTime le datetimeoffset'2012-02-29T07:18:45Z' and lastEvidenceTime ge datetimeoffset'2012-02-29T07:18:45Z') and " + 
		"(MPQuery eq '%|%|%|%|Trade Service|%|Login:%' or AnomalyScore ge 70)";
		
		CommonExpression exp = ExpressionParser.parse(query);
		OdataExpressionVisitor parser = new OdataExpressionVisitor();
		exp.visit(parser);
		
		
		assertTrue(parser.getExpressionType() == ExpressionType.AND);
		
		assertTrue(parser.isThresholdFilter() == false);
		assertTrue(parser.isMetricPathFilter() == false);
		assertTrue(parser.isMetricPathLikeFilter() == false);
		assertTrue(parser.isEscapeCharFilter() == false);
		assertTrue(parser.getMinFirstEvidenceTime() == null);
		assertTrue(parser.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(parser.getMaxLastEvidenceTime() == null);
		assertTrue(parser.getMinPeakEvidenceTime() == null);
		assertTrue(parser.getMaxPeakEvidenceTime() == null);
		assertTrue(parser.getMaxUpdateTime() == null);
		assertTrue(parser.getMinUpdateTime() == null);		
		assertTrue(parser.getMetricPathAnds().size() == 0);	
		assertTrue(parser.getMetricPathLikeAnds().size() == 0);	
		assertTrue(parser.getOredExpressions().size() == 2);
		
		OdataExpressionVisitor firstOrQuery = parser.mergeExpression(parser.getOredExpressions().get(0));
		
		assertTrue(firstOrQuery.isThresholdFilter() == false);
		assertTrue(firstOrQuery.isMetricPathFilter() == false);
		assertTrue(firstOrQuery.isMetricPathLikeFilter() == true);
		assertTrue(firstOrQuery.isEscapeCharFilter() == false);
		assertTrue(firstOrQuery.getMinFirstEvidenceTime() == null);
		assertTrue(firstOrQuery.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(firstOrQuery.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(firstOrQuery.getMaxLastEvidenceTime() == null);
		assertTrue(firstOrQuery.getMinPeakEvidenceTime() == null);
		assertTrue(firstOrQuery.getMaxPeakEvidenceTime() == null);
		assertTrue(firstOrQuery.getMaxUpdateTime() == null);
		assertTrue(firstOrQuery.getMinUpdateTime() == null);		
		assertTrue(firstOrQuery.getMetricPathAnds().size() == 0);	
		
		assertTrue(firstOrQuery.getMetricPathLikeAnds().size() == 1);	
		assertTrue(firstOrQuery.getMetricPathLikeAnds().get(0).equals("%|%|%|%|Trade Service|%|Login:%"));
		assertTrue(firstOrQuery.getOredExpressions().size() == 0);
		
		
		OdataExpressionVisitor secondOrQuery = parser.mergeExpression(parser.getOredExpressions().get(1));
		
		assertTrue(secondOrQuery.isThresholdFilter() == true);
		assertTrue(secondOrQuery.getThresholdValue() == 70);
		assertTrue(secondOrQuery.isMetricPathFilter() == false);
		assertTrue(secondOrQuery.isMetricPathLikeFilter() == false);
		assertTrue(secondOrQuery.isEscapeCharFilter() == false);
		assertTrue(secondOrQuery.getMinFirstEvidenceTime() == null);
		assertTrue(secondOrQuery.getMaxFirstEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(secondOrQuery.getMinLastEvidenceTime().getDateTime().equals(dateFormat.parse("2012-02-29 07:18:45")));
		assertTrue(secondOrQuery.getMaxLastEvidenceTime() == null);
		assertTrue(secondOrQuery.getMinPeakEvidenceTime() == null);
		assertTrue(secondOrQuery.getMaxPeakEvidenceTime() == null);
		assertTrue(secondOrQuery.getMaxUpdateTime() == null);
		assertTrue(secondOrQuery.getMinUpdateTime() == null);		
		assertTrue(secondOrQuery.getMetricPathAnds().size() == 0);	
		assertTrue(secondOrQuery.getMetricPathLikeAnds().size() == 0);	
		assertTrue(secondOrQuery.getOredExpressions().size() == 0);
	
		
		// TODO want a test like
		query = "(firstEvidenceTime le datetimeoffset'2012-02-29T07:18:45Z' or AnomalyScore ge 70) and " + 
		"(MPQuery eq '%|%|%|%|Trade Service|%|Login:%' or LastEvidenceTime ge datetimeoffset'2012-02-29T08:00:00Z')";

		
		
	}
	
	
	/**
	 * Test converting the SQL like queries into regexs
	 */
	@Test 
	public void likeQueryToRegExTest()
	{
		char escapeChar = '\\';
		
		String likeQuery = "%|%|%|%|Trade Service|%|Login:%";
		String replaced = QueryAPI.sqlLikeToRegEx(likeQuery, escapeChar);
		
		assertTrue(replaced.equals(".*\\|.*\\|.*\\|.*\\|Trade Service\\|.*\\|Login:.*"));
		
		
		likeQuery = "%|%|%|%|Trade Service|%|Login:\\% CPU";
		replaced = QueryAPI.sqlLikeToRegEx(likeQuery, escapeChar);
		
		assertTrue(replaced.equals(".*\\|.*\\|.*\\|.*\\|Trade Service\\|.*\\|Login:% CPU"));

		
		likeQuery = "_Trade Service|%|Login:\\% CPU";
		replaced = QueryAPI.sqlLikeToRegEx(likeQuery, escapeChar);

		assertTrue(replaced.equals(".Trade Service\\|.*\\|Login:% CPU"));

		
		likeQuery = "_Trade\\_Service|%|Login:% CPU";
		replaced = QueryAPI.sqlLikeToRegEx(likeQuery, escapeChar);

		assertTrue(replaced.equals(".Trade_Service\\|.*\\|Login:.* CPU"));
		
		
		// different escape char
		escapeChar = '^';
		likeQuery = "_Trade^_Service|%|Login:^% CPU";
		replaced = QueryAPI.sqlLikeToRegEx(likeQuery, escapeChar);

		assertTrue(replaced.equals(".Trade_Service\\|.*\\|Login:% CPU"));
	}
		
	
	/**
	 * Use a nonsense unknown property in the query string. 
	 * 
	 * @throws ParseException
	 */
	@Test
	public void unknownPropertyTime() throws ParseException 
	{
		try
		{
			// 'some_property' expression should throw
			String query = "some_property ne datetimeoffset'2012-02-29T07:18:45Z'";
			OdataExpressionVisitor parser = new OdataExpressionVisitor();
			CommonExpression exp = ExpressionParser.parse(query);
			exp.visit(parser);
			assertTrue("Parser should throw UnsupportedOperationException for filter " +
					"with unknown property: " 
					+ query, false);
		}
		catch (UnsupportedOperationException e)
		{

		}
	}
}
