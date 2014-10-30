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

import static org.junit.Assert.*;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.ExpressionVisitor;

import com.prelert.api.data.Activity;
import com.prelert.api.data.BeanToOProperties;
import com.prelert.api.data.EntityKey;
import com.prelert.api.data.IgnoreProperty;
import com.prelert.api.data.MetricConfig;
import com.prelert.api.data.MetricFeed;
import com.prelert.api.data.RelatedMetric;

/**
 * Test that the bean to OProperties methods return the
 * same fields as those defined in the bean. Use introspection 
 * to get the field names. 
 */
public class BeanOPropertiesTest 
{
	@Test
	public void testActivityToOProperties() throws IntrospectionException
	{
		Activity act = new Activity();
		List<OProperty<?>> oprops = BeanToOProperties.activityOProperties(act);
		PropertyDescriptor[] pds = Introspector.getBeanInfo(Activity.class, Object.class).getPropertyDescriptors();
		
		compareProperties(oprops, pds, "Id");
	}
	
	@Test
	public void testRelatedMetricToOProperties() throws IntrospectionException
	{
		RelatedMetric rm = new RelatedMetric();
		List<OProperty<?>> oprops = BeanToOProperties.relatedMetricOProperties(rm);
		PropertyDescriptor[] pds = Introspector.getBeanInfo(RelatedMetric.class, Object.class).getPropertyDescriptors();
		
		compareProperties(oprops, pds, "EvidenceId");
	}
	
	@Test
	public void testMetricFeedToOProperties() throws IntrospectionException
	{
		MetricFeed feed = new MetricFeed();
		List<OProperty<?>> oprops = BeanToOProperties.metricFeedOProperties(feed);
		PropertyDescriptor[] pds = Introspector.getBeanInfo(MetricFeed.class, Object.class).getPropertyDescriptors();
		
		compareProperties(oprops, pds, "Id");
	}
	
	@Test
	public void testMetricConfigToOProperties() throws IntrospectionException
	{
		List<String> metricPaths = new ArrayList<String>();
		metricPaths.add("path1");
		metricPaths.add("path2");
		metricPaths.add("etc");
		
		List<OProperty<?>> oprops = BeanToOProperties.metricConfigOProperties(metricPaths);
		PropertyDescriptor[] pds = Introspector.getBeanInfo(MetricConfig.class, Object.class).getPropertyDescriptors();
		
		compareProperties(oprops, pds, "Id");
	}
	
	
	private void compareProperties(List<OProperty<?>> oprops, PropertyDescriptor[] pds, String keyName)
	{
		// get the properties that should be ignored
		int ignorePropCount = 0;
		for (PropertyDescriptor pd : pds)
		{
			IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
			if (ignoreProp != null)
			{
				ignorePropCount++;
			}
		}
		assertTrue(oprops.size() == pds.length - ignorePropCount);
		
		
		// put the oprops in a map so they are easier to manage.
		Map<String, OProperty<?>> opropByName = new HashMap<String, OProperty<?>>();
		for (OProperty<?> oprop : oprops)
		{
			opropByName.put(oprop.getName(), oprop);
		}
		
		int entityKeyCount = 0;
		String entityKey = null;
		for (PropertyDescriptor pd : pds)
		{
			IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
			if (ignoreProp != null)
			{
				continue;
			}
			
			// name is the same
			StringBuilder propName = new StringBuilder().append(Character.toUpperCase(pd.getName().charAt(0))).append(
	                  pd.getName().substring(1));
			assertTrue(opropByName.containsKey(propName.toString()));
			
			OProperty<?> oprop = opropByName.get(propName.toString());
			
			// return type is the same
			EdmSimpleType<?> edmType = EdmSimpleType.forJavaType(pd.getReadMethod().getReturnType());
			// If the property is a date use the datetimeoffset class not 
			// the default datetime which has no timezone. 
			if (pd.getReadMethod().getReturnType().equals(Date.class))
			{
				edmType = EdmSimpleType.DATETIMEOFFSET;
			}
			assertTrue(oprop.getType().equals(edmType));
			
			EntityKey key = pd.getReadMethod().getAnnotation(EntityKey.class);
			if (key != null)
			{
				entityKey = propName.toString();
				entityKeyCount++;
			}
			
			// OK so remove from set
			opropByName.remove(propName.toString());
		}
		
		assertNotNull(entityKey);
		assertTrue(entityKey.equals(keyName));
		assertEquals(entityKeyCount, 1);
		
		assertTrue(opropByName.isEmpty());
	}
	
	
	@Test
	public void testActivitySelects() throws IntrospectionException
	{
		Activity act = new Activity();
		
		List<OProperty<?>> oprops = BeanToOProperties.activityOProperties(act);
		for (final OProperty<?> oprop : oprops)
		{
			List<EntitySimpleProperty> selects = new ArrayList<EntitySimpleProperty>();
			
			selects.add(new EntitySimpleProperty() {
				public String getPropertyName() {return oprop.getName();}
				public void visit(ExpressionVisitor visitor) {;}
			});
			
			
			List<OProperty<?>> selectedProps = BeanToOProperties.activityOProperties(act, selects);
			assertTrue(selectedProps.size() == 1);
			assertTrue(selectedProps.get(0).getName().equals(oprop.getName()));
		}
		
		List<EntitySimpleProperty> selects = new ArrayList<EntitySimpleProperty>();
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "anomalyscore";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "Id";}
			public void visit(ExpressionVisitor visitor) {;}
		});

		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "relatedmetriccount";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "non_existent_prop";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		
		List<OProperty<?>> selectedProps = BeanToOProperties.activityOProperties(act, selects);
		assertTrue(selectedProps.size() == 3);
		assertTrue(selectedProps.get(0).getName().equals("AnomalyScore"));
		assertTrue(selectedProps.get(1).getName().equals("Id"));
		assertTrue(selectedProps.get(2).getName().equals("RelatedMetricCount"));
	}
	
	
	@Test
	public void testRelatedMetricSelects() throws IntrospectionException
	{
		RelatedMetric rm = new RelatedMetric();
		
		List<OProperty<?>> oprops = BeanToOProperties.relatedMetricOProperties(rm);
		for (final OProperty<?> oprop : oprops)
		{
			List<EntitySimpleProperty> selects = new ArrayList<EntitySimpleProperty>();
			selects.add(new EntitySimpleProperty() {
				public String getPropertyName() {return oprop.getName();}
				public void visit(ExpressionVisitor visitor) {;}
			});
			
			
			List<OProperty<?>> selectedProps = BeanToOProperties.relatedMetricOProperties(rm, selects);
			assertTrue(selectedProps.size() == 1);
			assertTrue(selectedProps.get(0).getName().equals(oprop.getName()));
		}
		
		
		List<EntitySimpleProperty> selects = new ArrayList<EntitySimpleProperty>();
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "evidenceId";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "DateTime";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		selects.add(new EntitySimpleProperty() {
			public String getPropertyName() {return "non_existent_prop";}
			public void visit(ExpressionVisitor visitor) {;}
		});
		
		
		List<OProperty<?>> selectedProps = BeanToOProperties.relatedMetricOProperties(rm, selects);
		assertTrue(selectedProps.size() == 2);
		assertTrue(selectedProps.get(0).getName().equals("EvidenceId"));
		assertTrue(selectedProps.get(1).getName().equals("DateTime"));
	}

}
