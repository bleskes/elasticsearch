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

package com.prelert.api;


import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.apache.log4j.Logger;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OLink;
import org.odata4j.core.OLinks;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.core.ORelatedEntitiesLinkInline;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.exceptions.NotImplementedException;
import org.odata4j.exceptions.ODataProducerException;
import org.odata4j.exceptions.ServerErrorException;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ErrorResponseExtension;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.edm.MetadataProducer;

import com.prelert.api.dao.DataStore;
import com.prelert.api.data.Activity;
import com.prelert.api.data.BeanToOProperties;
import com.prelert.api.data.EntityKey;
import com.prelert.api.data.IgnoreProperty;
import com.prelert.api.data.MetricConfig;
import com.prelert.api.data.MetricFeed;
import com.prelert.api.data.OdataExpressionVisitor;
import com.prelert.api.data.OrderByExpressionVisitor;
import com.prelert.api.data.RelatedMetric;


/**
 * ODATA producer implementation, entry point for all ODATA calls.
 * 
 * The datastore and metric manager beans needs to be injected for this class to function
 */
public class QueryAPI implements ODataProducer, ErrorResponseExtension
{
	private final static Logger s_Logger = Logger.getLogger(QueryAPI.class);
	
	/**
	 * ODATA constants
	 */
	public final static String NAMESPACE = "Prelert";

	public final static String ACTIVITY_ENTITY = "Activity";
	public final static String RELATEDMETRICS_ENTITY = "RelatedMetric";
	
	public final static String ACTIVITIES_SET = "Activities";
	public final static String RELATEDMETRICS_SET = "RelatedMetrics";
	public final static String METRIC_FEEDS_SET = "MetricFeeds";
	public final static String METRIC_CONFIG_SET = "MetricConfigs";
	
	public final static String RELATEDMETRICS_NAV = "ActivityMetrics"; 
	public final static String METRIC_ACTIVITY_NAV = "MetricActivity";
	
	public final static String EARLIEST_ACTIVITY_FUNC = "EarliestActivityTime"; 
	public final static String LATEST_ACTIVITY_FUNC = "LatestActivityTime"; 

	
	private DataStore m_DataStore;
	private MetricManager m_MetricManager;
	
	/**
	 * Static shared metadata object.
	 */
	private final static EdmDataServices s_MetaData;
	
	/**
	 * Configuration manager object.
	 */
	private ConfigurationManager m_ConfigurationManager;

	/**
	 * Static initialisation of the shared metadata object.
	 */
	static 
	{
		s_MetaData = QueryAPI.createMetaData();
	}

	
	
	/**
	 * Uses introspection on the data objects to create the ODATA
	 * metadata.
	 * 
	 * @return
	 */
	private static EdmDataServices createMetaData() 
	{	

		List<EdmEntityType.Builder> entityTypes = new ArrayList<EdmEntityType.Builder>();		
		List<EdmEntitySet.Builder> entitySets = new ArrayList<EdmEntitySet.Builder>();

			
		// RelatedMetrics			
		List<EdmProperty.Builder> relatedMetricProps = new ArrayList<EdmProperty.Builder>();
		EdmEntityType.Builder relatedMetricType = null;
		try
		{
			PropertyDescriptor[] pds = Introspector.getBeanInfo(RelatedMetric.class, Object.class).getPropertyDescriptors();
			
			String entityKey = null;
			for (PropertyDescriptor pd : pds)
			{
				IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
				if (ignoreProp != null)
				{
					continue;
				}
				
				
				String propName = new StringBuilder().append(Character.toUpperCase(pd.getName().charAt(0))).append(
		                  pd.getName().substring(1)).toString();
				
				EntityKey key = pd.getReadMethod().getAnnotation(EntityKey.class);
				if (key != null)
				{
					entityKey = propName;
				}

				EdmSimpleType<?> edmType = EdmSimpleType.forJavaType(pd.getReadMethod().getReturnType());
				// If the property is a date use the datetimeoffset class not 
				// the default datetime which has no timezone. 
				if (pd.getReadMethod().getReturnType().equals(Date.class))
				{
					edmType = EdmSimpleType.DATETIMEOFFSET;
				}
				
				if (edmType != null)
				{
					relatedMetricProps.add(EdmProperty.newBuilder(propName.toString()).setType(edmType));
				}
			}
			
			relatedMetricType = EdmEntityType.newBuilder().setNamespace(NAMESPACE)
												.setName(RelatedMetric.class.getSimpleName())
												.addKeys(entityKey)
												.addProperties(relatedMetricProps);
			entityTypes.add(relatedMetricType);
			
			EdmEntitySet.Builder relatedMetricsEntitySet = EdmEntitySet.newBuilder().setName(RELATEDMETRICS_SET).setEntityType(relatedMetricType);
			entitySets.add(relatedMetricsEntitySet);
		}
		catch (IntrospectionException ie)
		{
			s_Logger.error("Error introspecting RelatedMetrics", ie);
		}
			
			
		// Activities
		List<EdmProperty.Builder> activityProps = new ArrayList<EdmProperty.Builder>();
		EdmEntityType.Builder activityType = null;
		try
		{
			PropertyDescriptor[] pds = Introspector.getBeanInfo(Activity.class, Object.class).getPropertyDescriptors();
			
			String entityKey = null;
			for (PropertyDescriptor pd : pds)
			{
				IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
				if (ignoreProp != null)
				{
					continue;
				}
				
				String propName = new StringBuilder().append(Character.toUpperCase(pd.getName().charAt(0))).append(
						pd.getName().substring(1)).toString();

				EntityKey key = pd.getReadMethod().getAnnotation(EntityKey.class);
				if (key != null)
				{
					entityKey = propName;
				}
				EdmSimpleType<?> edmType = EdmSimpleType.forJavaType(pd.getReadMethod().getReturnType());
				// If the property is a date use the datetimeoffset class not 
				// the default datetime which has no timezone. 
				if (pd.getReadMethod().getReturnType().equals(Date.class))
				{
					edmType = EdmSimpleType.DATETIMEOFFSET;
				}
				
				if (edmType != null)
				{
					activityProps.add(EdmProperty.newBuilder(propName.toString()).setType(edmType));
				}
			}
			
			activityType = EdmEntityType.newBuilder().setNamespace(NAMESPACE)
												.setName(Activity.class.getSimpleName())
												.addKeys(entityKey)
												.addProperties(activityProps);
			entityTypes.add(activityType);
			
			EdmEntitySet.Builder activitiesEntitySet = EdmEntitySet.newBuilder().setName(ACTIVITIES_SET).setEntityType(activityType);			
			entitySets.add(activitiesEntitySet);
		}
		catch (IntrospectionException ie)
		{
			s_Logger.error("Error introspecting Activity", ie);
		}
	
			// Metric Feed
			List<EdmProperty.Builder> metricFeedProps = new ArrayList<EdmProperty.Builder>();
		try
		{
			PropertyDescriptor[] pds = Introspector.getBeanInfo(MetricFeed.class, Object.class).getPropertyDescriptors();
			
			String entityKey = null;
			for (PropertyDescriptor pd : pds)
			{
				IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
				if (ignoreProp != null)
				{
					continue;
				}
				
				String propName = new StringBuilder().append(Character.toUpperCase(pd.getName().charAt(0))).append(
						pd.getName().substring(1)).toString();

				EntityKey key = pd.getReadMethod().getAnnotation(EntityKey.class);
				if (key != null)
				{
					entityKey = propName;
				}
				
				EdmSimpleType<?> edmType = EdmSimpleType.forJavaType(pd.getReadMethod().getReturnType());
				// If the property is a date use the datetimeoffset class not 
				// the default datetime which has no timezone. 
				if (pd.getReadMethod().getReturnType().equals(Date.class))
				{
					edmType = EdmSimpleType.DATETIMEOFFSET;
				}
				
				if (edmType != null)
				{
					metricFeedProps.add(EdmProperty.newBuilder(propName.toString()).setType(edmType));
				}
			}
			
			EdmEntityType.Builder metricFeedType = EdmEntityType.newBuilder().setNamespace(NAMESPACE)
												.setName(MetricFeed.class.getSimpleName())
												.addKeys(entityKey)
												.addProperties(metricFeedProps);
			entityTypes.add(metricFeedType);	
			
			EdmEntitySet.Builder metricFeedEntitySet = EdmEntitySet.newBuilder().setName(METRIC_FEEDS_SET).setEntityType(metricFeedType);
			entitySets.add(metricFeedEntitySet);
		}
		catch (IntrospectionException ie)
		{
			s_Logger.error("Error introspecting MetricFeed", ie);
		}			
			
			// Metric Config
			List<EdmProperty.Builder> metricConfigProps = new ArrayList<EdmProperty.Builder>();
		try
		{
			PropertyDescriptor[] pds = Introspector.getBeanInfo(MetricConfig.class, Object.class).getPropertyDescriptors();
			
			String entityKey = null;
			for (PropertyDescriptor pd : pds)
			{
				IgnoreProperty ignoreProp = pd.getReadMethod().getAnnotation(IgnoreProperty.class);
				if (ignoreProp != null)
				{
					continue;
				}
				
				String propName = new StringBuilder().append(Character.toUpperCase(pd.getName().charAt(0))).append(
						pd.getName().substring(1)).toString();

				EntityKey key = pd.getReadMethod().getAnnotation(EntityKey.class);
				if (key != null)
				{
					entityKey = propName;
				}
				
				EdmSimpleType<?> edmType = EdmSimpleType.forJavaType(pd.getReadMethod().getReturnType());
				// If the property is a date use the datetimeoffset class not 
				// the default datetime which has no timezone. 
				if (pd.getReadMethod().getReturnType().equals(Date.class))
				{
					edmType = EdmSimpleType.DATETIMEOFFSET;
				}
				
				if (edmType != null)
				{
					metricConfigProps.add(EdmProperty.newBuilder(propName.toString()).setType(edmType));
				}
			}
			
			EdmEntityType.Builder metricConfigType = EdmEntityType.newBuilder().setNamespace(NAMESPACE)
												.setName(MetricConfig.class.getSimpleName())
												.addKeys(entityKey)
												.addProperties(metricConfigProps);
			entityTypes.add(metricConfigType);	
			
			EdmEntitySet.Builder metricConfigEntitySet = EdmEntitySet.newBuilder().setName(METRIC_CONFIG_SET).setEntityType(metricConfigType);
			entitySets.add(metricConfigEntitySet);
		}
		catch (IntrospectionException ie)
		{
			s_Logger.error("Error introspecting MetricConfig", ie);
		}			
			
			// Activity to Metrics Navigation
			EdmAssociationEnd.Builder from = EdmAssociationEnd.newBuilder()
			  .setMultiplicity(EdmMultiplicity.ONE)
			  .setType(activityType)
			  .setTypeName(ACTIVITY_ENTITY)
			  .setRole(ACTIVITY_ENTITY);
			
			EdmAssociationEnd.Builder to = EdmAssociationEnd.newBuilder()
			  .setMultiplicity(EdmMultiplicity.MANY)
			  .setType(relatedMetricType)
			  .setTypeName(RELATEDMETRICS_ENTITY)
			  .setRole(RELATEDMETRICS_ENTITY);
			
			EdmAssociation.Builder activityMetricsAssoc = EdmAssociation.newBuilder()
															.setNamespace(NAMESPACE)
															.setName("ActivityToMetrics")
															.setEnds(from, to);
			
			EdmNavigationProperty.Builder navBuilder = EdmNavigationProperty.newBuilder(RELATEDMETRICS_NAV)
																		.setFromTo(from, to)																					
																		.setRelationship(activityMetricsAssoc);
			
			// Metric to Activity Navigation
			EdmAssociationEnd.Builder from2 = EdmAssociationEnd.newBuilder()
						.setMultiplicity(EdmMultiplicity.ONE)
						.setType(relatedMetricType)
						.setTypeName(RELATEDMETRICS_ENTITY)
						.setRole(RELATEDMETRICS_ENTITY);
			
			EdmAssociationEnd.Builder to2 = EdmAssociationEnd.newBuilder()
						.setMultiplicity(EdmMultiplicity.ONE)
						.setType(activityType)
						.setTypeName(ACTIVITY_ENTITY)
						.setRole(ACTIVITY_ENTITY);


			EdmAssociation.Builder metricActivityAssoc = EdmAssociation.newBuilder()
															.setNamespace(NAMESPACE)
															.setName("MetricToActivity")
															.setEnds(from2, to2);
			
			EdmNavigationProperty.Builder navBuilder2 = EdmNavigationProperty.newBuilder(METRIC_ACTIVITY_NAV)
																		.setFromTo(from2, to2)																					
																		.setRelationship(metricActivityAssoc);
			
			activityType.addNavigationProperties(navBuilder);
			relatedMetricType.addNavigationProperties(navBuilder2);
			
			
			
			EdmSchema.Builder modelSchema = EdmSchema.newBuilder().setNamespace(NAMESPACE)
					.addEntityTypes(entityTypes);
			
			modelSchema.addAssociations(new ArrayList<EdmAssociation.Builder>(Arrays.asList(
					new EdmAssociation.Builder [] {activityMetricsAssoc, metricActivityAssoc})));

			EdmEntityContainer.Builder container = EdmEntityContainer.newBuilder().setName(NAMESPACE)
															.setIsDefault(true).addEntitySets(entitySets);
			
			// Add functions
			container.addFunctionImports(EdmFunctionImport.newBuilder()
					.setName(EARLIEST_ACTIVITY_FUNC)
					.setReturnType(EdmSimpleType.DATETIMEOFFSET)
					.setIsCollection(false)
					.setEntitySetName(null)
					.setHttpMethod("GET"));
			
			container.addFunctionImports(EdmFunctionImport.newBuilder()
					.setName(LATEST_ACTIVITY_FUNC)
					.setReturnType(EdmSimpleType.DATETIMEOFFSET)
					.setIsCollection(false)
					.setEntitySetName(null)
					.setHttpMethod("GET"));
			

			modelSchema.addEntityContainers(container);
			EdmDataServices metadata = EdmDataServices.newBuilder().addSchemas(modelSchema).build();
			
			return metadata;
	}
	
	
	
	@Override
	public EdmDataServices getMetadata() 
	{
		s_Logger.debug("getMetadata()");
		
		return s_MetaData;
	}

	@Override
	public MetadataProducer getMetadataProducer() 
	{
		return null;
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public <TExtension extends OExtension<ODataProducer>> TExtension 
	findExtension(Class<TExtension> clazz) 
	{
		if (clazz.equals(ErrorResponseExtension.class))
		{
			return (TExtension)this;
		}
		else
		{
			return null;
		}
	}
	

	/**
	 * Just log the error messages
	 */
	@Override
	public boolean returnInnerError(HttpHeaders httpHeaders, UriInfo uriInfo,
			ODataProducerException exception) 
	{
		s_Logger.error("Exception URI: " + uriInfo.getAbsolutePath() + 
				"\n" + exception);
		
		return true;
	}
	
	@Override
	public EntitiesResponse getEntities(String entitySetName, QueryInfo queryInfo)
	{
		 s_Logger.debug("getEntities(), entitySet = " + entitySetName +
				 " queryInfo = " + queryInfo);
		 
		 if (ACTIVITIES_SET.equals(entitySetName))
		 {		
			 EdmEntitySet entitySet = getMetadata().getEdmEntitySet(entitySetName);

			 List<Activity> activities = Collections.emptyList();
			 try
			 {
				 activities = queryActivities(queryInfo);
			 }
			 catch (Exception e)
			 {
				 String msg = "queryActivities Error " + e;
				 s_Logger.error(msg);

				 throw new ServerErrorException(msg);
			 }
			 
			 List<OEntity> entities = new ArrayList<OEntity>();
			 EdmEntitySet rmEntitySet = getMetadata().getEdmEntitySet(RELATEDMETRICS_SET);
			 
			 for (Activity act : activities)
			 {
				 List<OProperty<?>> properties;
				 if (queryInfo.select != null && queryInfo.select.isEmpty() == false)
				 {
					 properties = BeanToOProperties.activityOProperties(act, queryInfo.select);
				 }
				 else
				 {
					 properties = BeanToOProperties.activityOProperties(act);
				 }

				 List<OLink> olinks = null;
				 if (act.getRelatedMetrics() != null)
				 {
					 List<OEntity> relatedEnts = new ArrayList<OEntity>();
					 for (RelatedMetric rm : act.getRelatedMetrics())
					 {
						 List<OProperty<?>> rmProperties = BeanToOProperties.relatedMetricOProperties(rm);
						 relatedEnts.add(OEntities.create(rmEntitySet, OEntityKey.create(rm.getEvidenceId()), rmProperties, null));
					 }

					 ORelatedEntitiesLinkInline inlineEntities = OLinks.relatedEntitiesInline(RELATEDMETRICS_NAV, RELATEDMETRICS_NAV, null, relatedEnts);
					 olinks = new ArrayList<OLink>(Arrays.asList(new OLink [] {inlineEntities}));
				 }
					
				 entities.add(OEntities.create(entitySet, OEntityKey.create(act.getId()), properties, olinks));
			 }

			 return Responses.entities(entities, entitySet, entities.size(), null);
		 }
		 else
		 {
			 String msg = "Get entities from entity set '" + entitySetName + "' not implemented";
			 s_Logger.warn(msg);
			 throw new NotImplementedException(msg);
		 }
	}
	
	
	/**
	 * This is an inefficient implementation that first gets all 
	 * the activities then returns the number of them.
	 */
	@Override
	public CountResponse getEntitiesCount(String entitySetName, QueryInfo queryInfo) 
	{
		 EdmEntitySet entitySet = getMetadata().getEdmEntitySet(entitySetName);

		 s_Logger.debug("getEntitiesCount(), entitySet = " + entitySet);
		 
		 if (ACTIVITIES_SET.equals(entitySetName))
		 {		 
			 List<Activity> activities = queryActivities(queryInfo);

			 return Responses.count(activities.size());
		 }
		 else
		 {
			 String msg = "Get entities count from entity set '" + entitySetName + "' not implemented";
			 s_Logger.warn(msg);
			 throw new NotImplementedException(msg);
		 }
		 
	}
	
	
	@Override
	public EntityResponse getEntity(String entitySetName, OEntityKey key,
			EntityQueryInfo queryInfo) 
	{	
		s_Logger.debug(String.format("getEntity %s, %s, %s", entitySetName,
				key, queryInfo));

		EdmEntitySet entitySet = getMetadata().getEdmEntitySet(entitySetName);


		if (ACTIVITIES_SET.equals(entitySetName))
		{	
			int activityId = 0;
			try
			{
				activityId = Integer.parseInt(key.toKeyStringWithoutParentheses());
				Activity act = m_DataStore.getActivity(activityId); 
				
				if (act == null)
				{
					String msg = String.format("No Activity with id = %d", activityId);
					s_Logger.warn(msg);
					throw new NotFoundException(msg);
				}

				List<OProperty<?>> properties;
				if (queryInfo.select != null && queryInfo.select.isEmpty() == false)
				{
					properties = BeanToOProperties.activityOProperties(act, queryInfo.select);
				}
				else
				{
					properties = BeanToOProperties.activityOProperties(act);
				}

				List<OLink> olinks = null;
				if ((queryInfo.expand.size() > 0) && 
						RELATEDMETRICS_NAV.equals(queryInfo.expand.get(0).getPropertyName()))
				{
					List<RelatedMetric> metrics = m_DataStore.getRelatedMetrics(activityId);

					EdmEntitySet rmEntitySet = getMetadata().getEdmEntitySet(RELATEDMETRICS_SET);
					List<OEntity> relatedEnts = new ArrayList<OEntity>();
					for (RelatedMetric rm : metrics)
					{
						List<OProperty<?>> rmProperties = BeanToOProperties.relatedMetricOProperties(rm);
						relatedEnts.add(OEntities.create(rmEntitySet, OEntityKey.create(rm.getEvidenceId()), rmProperties, null));
					}

					ORelatedEntitiesLinkInline inlineEntities = OLinks.relatedEntitiesInline(RELATEDMETRICS_NAV, RELATEDMETRICS_NAV, null, relatedEnts);
					olinks = new ArrayList<OLink>(Arrays.asList(new OLink [] {inlineEntities}));
				}

				OEntity entity = OEntities.create(entitySet, key, properties, olinks);
				return Responses.entity(entity);

			}
			catch (NumberFormatException e)
			{
				String msg = String.format("Cannot parse the entity key '%s' as an Integer",
						key.toKeyStringWithoutParentheses());
				s_Logger.warn(msg);
				
				throw new BadRequestException(msg);
			}
		}
		else if (RELATEDMETRICS_SET.equals(entitySetName))
		{
			int evidenceId = -1;
			try
			{
				evidenceId = Integer.parseInt(key.toKeyStringWithoutParentheses());

				RelatedMetric rm = m_DataStore.getRelatedMetric(evidenceId);
				if (rm == null)
				{
					String msg = String.format("No RelatedMetric with evidence id = %d", evidenceId);
					s_Logger.warn(msg);
					throw new NotFoundException(msg);
				}
				
				List<OProperty<?>> properties = BeanToOProperties.relatedMetricOProperties(rm, queryInfo.select);

				OEntity entity = OEntities.create(entitySet, key, properties, null);
				return Responses.entity(entity);
			}
			catch (NumberFormatException e)
			{
				String msg = String.format("Cannot parse the entity key '%s' as an Integer",
						key.toKeyStringWithoutParentheses());
				s_Logger.warn(msg);
				
				throw new BadRequestException(msg);

			}
		}
		else if (METRIC_CONFIG_SET.equals(entitySetName))
		{
			synchronized(m_ConfigurationManager)
			{
				List<OProperty<?>> properties = BeanToOProperties.metricConfigOProperties(
						m_ConfigurationManager.getTrackedMetrics());

				OEntity entity = OEntities.create(entitySet, OEntityKey.create(1), properties, null);
				return Responses.entity(entity);
			}
		}
		else
		{
			String msg = "Get entity from entity set '" + entitySetName + "' not implemented";
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}

	
	@Override
	public BaseResponse callFunction(EdmFunctionImport funcImport,
			Map<String, OFunctionParameter> params, QueryInfo queryInfo) 
	{
		s_Logger.debug("call function " + funcImport.getName());
		
		if (EARLIEST_ACTIVITY_FUNC.equals(funcImport.getName()))
		{
			Date date = m_DataStore.getEarliestActivityTime();
			if (date == null)
			{
				return Responses.simple(EdmSimpleType.DATETIMEOFFSET, EARLIEST_ACTIVITY_FUNC, new DateTime(0));
			}
			return Responses.simple(EdmSimpleType.DATETIMEOFFSET, EARLIEST_ACTIVITY_FUNC, new DateTime(date));
		}
		else if (LATEST_ACTIVITY_FUNC.equals(funcImport.getName()))
		{
			Date date = m_DataStore.getLatestActivityTime();
			if (date == null)
			{
				return Responses.simple(EdmSimpleType.DATETIMEOFFSET, LATEST_ACTIVITY_FUNC, new DateTime(0));
			}
			return Responses.simple(EdmSimpleType.DATETIMEOFFSET, LATEST_ACTIVITY_FUNC, new DateTime(date));
		}
		else 
		{
			String msg = "Unknown function " + funcImport.getName();
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}
	
	
	@Override
	public BaseResponse getNavProperty(String entitySetName, OEntityKey key,
			String nav, QueryInfo queryInfo) 
	{
		s_Logger.debug(String.format("getNavProperty %s, %s, %s, %s", 
				entitySetName, key, nav, queryInfo));
	
		if (ACTIVITIES_SET.equals(entitySetName) && RELATEDMETRICS_NAV.equalsIgnoreCase(nav))
		{	
			int activityId = 0;
			try
			{
				activityId = Integer.parseInt(key.toKeyStringWithoutParentheses());			
				List<RelatedMetric> metrics = queryRelatedMetrics(activityId, queryInfo);
 
				
				EdmEntitySet rmEntitySet = getMetadata().getEdmEntitySet(RELATEDMETRICS_SET);
				List<OEntity> relatedEnts = new ArrayList<OEntity>();
				for (RelatedMetric rm : metrics)
				{
					List<OProperty<?>> rmProperties = BeanToOProperties.relatedMetricOProperties(rm, queryInfo.select);
					relatedEnts.add(OEntities.create(rmEntitySet, OEntityKey.create(rm.getEvidenceId()), rmProperties, null));
				}
			
				return Responses.entities(relatedEnts, rmEntitySet, relatedEnts.size(), null);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error(String.format("Cannot parse the entity key '%s' as an Integer",
						key.toKeyStringWithoutParentheses()));

				return null;
			}
		}
		else if (RELATEDMETRICS_SET.equals(entitySetName) && METRIC_ACTIVITY_NAV.equalsIgnoreCase(nav))
		{
			EntityQueryInfo entQueryInfo = new EntityQueryInfo(queryInfo.filter, 
					queryInfo.customOptions, queryInfo.expand, queryInfo.select);
			
			return getEntity(ACTIVITIES_SET, key, entQueryInfo);
		}
		else 
		{
			String msg = String.format("Unknown navigation '%s' in entity set '%s", entitySetName, nav);
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}
	

	@Override
	public void close() 
	{
		s_Logger.info("Closing QueryAPI");
		
		// TODO Stop the metric manager??
	}

	@Override
	public EntityResponse createEntity(String entitySetName, OEntity entity) 
	{
		s_Logger.debug(String.format("createEntity %s", entitySetName));

		if (METRIC_FEEDS_SET.equals(entitySetName))
		{
			MetricFeed metricFeed = MetricFeed.fromOProperties(entity.getProperties());

			s_Logger.debug("Sending " + metricFeed.getCount() + " metrics");

			// Sync access to the metric manager.
			// This is a single point of access that will block
			// on concurrent uploads.
			synchronized (m_MetricManager) 
			{
				m_MetricManager.addMetricFeed(metricFeed);
			}
			
			
			s_Logger.debug("Sent " + metricFeed.getCount() + " metrics");
			
 			// Return a dummy entity.
			List<OProperty<?>> props = new ArrayList<OProperty<?>>();
			props.add(OProperties.int32("Id", 1));
			return Responses.entity(OEntities.create(entity.getEntitySet(), OEntityKey.create(1), props, null));
		}
		else if (METRIC_CONFIG_SET.equals(entitySetName))
		{
			MetricConfig metricConfig = MetricConfig.fromOProperties(entity.getProperties());
			List<String> metricPaths = metricConfig.getMetricPaths();
			
			s_Logger.debug("Configuring " + metricPaths.size() + " metric paths");
			
			synchronized(m_ConfigurationManager)
			{
				m_ConfigurationManager.setTrackedMetrics(metricPaths);
			}
			
			// Return a dummy entity.
			List<OProperty<?>> props = new ArrayList<OProperty<?>>();
			props.add(OProperties.int32("Id", 1));
			return Responses.entity(OEntities.create(entity.getEntitySet(), OEntityKey.create(1), props, null));
		}
		else
		{
			String msg = String.format("Cannot create entity in set '%s", entitySetName);
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}
	

	@Override
	public void updateEntity(String entitySetName, OEntity entity) 
	{
		s_Logger.info(String.format("updateEntity %s, %s", entitySetName, entity));

		if (METRIC_CONFIG_SET.equals(entitySetName))
		{
			MetricConfig metricConfig = MetricConfig.fromOProperties(entity.getProperties());
			List<String> metricPaths = metricConfig.getMetricPaths();
			
			s_Logger.debug("Configuring " + metricPaths.size() + " metric paths");
			
			synchronized(m_ConfigurationManager)
			{
				m_ConfigurationManager.setTrackedMetrics(metricPaths);
			}
		}
		else
		{
			String msg = String.format("Cannot update entity in set '%s", entitySetName);
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}
	

	@Override
	public EntityResponse createEntity(String arg0, OEntityKey arg1,
						String arg2, OEntity arg3) 
	{
		s_Logger.info(String.format("createEntity %s, %s, %s, %s", arg0, arg1, arg2, arg3));
		
		throw new NotImplementedException("createEntity");
	}

	@Override
	public void createLink(OEntityId arg0, String arg1, OEntityId arg2) 
	{
		throw new NotImplementedException("createLink");
	}

	@Override
	public void deleteEntity(String arg0, OEntityKey arg1) 
	{
		throw new NotImplementedException("deleteEntity");
	}

	@Override
	public void deleteLink(OEntityId arg0, String arg1, OEntityKey arg2) 
	{
		throw new NotImplementedException("deleteLink");
	}

	@Override
	public EntityIdResponse getLinks(OEntityId arg0, String arg1) 
	{
		throw new NotImplementedException("getLinks");
	}

	@Override
	public CountResponse getNavPropertyCount(String entitySetName, OEntityKey key,
			String nav, QueryInfo queryInfo) 
	{
		s_Logger.debug(String.format("getNavPropertyCount %s, %s, %s, %s", 
				entitySetName, key, nav, queryInfo));
	
		if (ACTIVITIES_SET.equals(entitySetName) && RELATEDMETRICS_NAV.equalsIgnoreCase(nav))
		{	
			try
			{
				int activityId = Integer.parseInt(key.toKeyStringWithoutParentheses());			
				List<RelatedMetric> metrics = queryRelatedMetrics(activityId, queryInfo);
			
				return Responses.count(metrics.size());
			}
			catch (NumberFormatException e)
			{
				s_Logger.error(String.format("Cannot parse the entity key '%s' as an Integer",
						key.toKeyStringWithoutParentheses()));

				return null;
			}
		}
		else if (RELATEDMETRICS_SET.equals(entitySetName) && METRIC_ACTIVITY_NAV.equalsIgnoreCase(nav))
		{
			try
			{
				int evidenceId = Integer.parseInt(key.toKeyStringWithoutParentheses());
				Activity act = m_DataStore.getActivity(evidenceId); 
				
				if (act != null)
				{
					return Responses.count(1);
				}
				else
				{
					return Responses.count(0);
				}
				
			}
			catch (NumberFormatException e)
			{
				s_Logger.error(String.format("Cannot parse the entity key '%s' as an Integer",
						key.toKeyStringWithoutParentheses()));

				return null;
			}
		}
		else 
		{
			String msg = String.format("Cannot get nav count for navigation '%s' in set '%s", 
					nav, entitySetName);
			s_Logger.warn(msg);
			throw new NotImplementedException(msg);
		}
	}

	@Override
	public void mergeEntity(String arg0, OEntity arg1) 
	{
		throw new NotImplementedException("mergeEntity");
	}

	@Override
	public void updateLink(OEntityId arg0, String arg1, OEntityKey arg2,
			OEntityId arg3) 
	{
		throw new NotImplementedException("updateLink");
	}

	
	/**
	 * Get the related metrics for the activity id and apply the query options.
	 * 
	 * The only query option supported is <code>$orderby</code> and sorting can 
	 * only these be done on these fields:<br/>
	 * <ul>
	 * <li>significance</li>
	 * <li>peakEvidenceTime</li>
	 * </ul>
	 * 
	 * Secondary sorting is by the other sort field e.g. if sorting by <code>significance</code>
	 * then the secondary sort order is <code>PeakEvidenceTime</code>.
	 * 
	 * @param activityId
	 * @param queryInfo
	 * @return
	 */
	private List<RelatedMetric> queryRelatedMetrics(int activityId, final QueryInfo queryInfo)
	{
		List<RelatedMetric> rms = m_DataStore.getRelatedMetrics(activityId);
		return orderRelatedMetrics(rms, queryInfo);
	}
	
	
	/**
	 * Sort the list of related metrics according to the orderby query option.
	 * 
	 * @param metrics
	 * @param queryInfo
	 * @return Sorting is done inline so the returned object is the same
	 * as the metrics parameter.
	 */
	private List<RelatedMetric> orderRelatedMetrics(List<RelatedMetric> metrics, final QueryInfo queryInfo)
	{
		if (queryInfo.orderBy != null && queryInfo.orderBy.size() > 0)
		{
			OrderByExpressionVisitor visitor = new OrderByExpressionVisitor();
			queryInfo.orderBy.get(0).getExpression().visit(visitor);
			
			if ("significance".equalsIgnoreCase(visitor.getPropertyName()))
			{
				Comparator<RelatedMetric> comp = new Comparator<RelatedMetric>() {
					
					@Override
					public int compare(RelatedMetric arg0, RelatedMetric arg1) {
						int result;
						if (queryInfo.orderBy.get(0).getDirection() == Direction.ASCENDING)
						{
							result = arg0.getSignificance() - arg1.getSignificance();
						}
						else 
						{
							result = arg1.getSignificance() - arg0.getSignificance();
						}
						
						if (result == 0)
						{
							// use date time as secondary order.
							result = arg0.getDateTime().compareTo(arg1.getDateTime());
						}
						
						return result;
					}
				};
				
				Collections.sort(metrics, comp);
			}
			else if ("datetime".equalsIgnoreCase(visitor.getPropertyName()))
			{
				Comparator<RelatedMetric> comp = new Comparator<RelatedMetric>() {
					
					@Override
					public int compare(RelatedMetric arg0, RelatedMetric arg1) {
						int result;
						if (queryInfo.orderBy.get(0).getDirection() == Direction.ASCENDING)
						{
							result = arg0.getDateTime().compareTo(arg1.getDateTime());
						}
						else 
						{
							result = arg1.getDateTime().compareTo(arg0.getDateTime());
						}
						
						if (result == 0)
						{
							// secondary order is by significance
							result = arg0.getSignificance() - arg1.getSignificance();
						}
						
						return result;
					}
				};
				
				Collections.sort(metrics, comp);
			}
		}
		
		return metrics;
	}

	public List<Activity> runQuery(OdataExpressionVisitor filterVisitor)
	{
		List<String> metricPaths = Collections.emptyList();
		List<String> likeMetricPaths = Collections.emptyList();
		String escapeChar = "\\"; // default escape
		int anomalyThreshold = 1;

		Date minTime = null;
		boolean minTimeIsOpen = false;
		Date maxTime = null;
		boolean maxTimeIsOpen = false;
		Date minFirstTime = null;
		boolean minFirstTimeIsOpen = false;
		Date maxFirstTime = null;
		boolean maxFirstTimeIsOpen = false;
		Date minLastTime = null;
		boolean minLastTimeIsOpen = false;
		Date maxLastTime = null;
		boolean maxLastTimeIsOpen = false;
		Date minUpdateTime = null;
		boolean minUpdateTimeIsOpen = false;
		Date maxUpdateTime = null;
		boolean maxUpdateTimeIsOpen = false;


		if (filterVisitor.getMinPeakEvidenceTime() != null)
		{
			minTime = filterVisitor.getMinPeakEvidenceTime().getDateTime();
			minTimeIsOpen = filterVisitor.getMinPeakEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMaxPeakEvidenceTime() != null)
		{
			maxTime = filterVisitor.getMaxPeakEvidenceTime().getDateTime();
			maxTimeIsOpen = filterVisitor.getMaxPeakEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMinFirstEvidenceTime() != null)
		{
			minFirstTime = filterVisitor.getMinFirstEvidenceTime().getDateTime();
			minFirstTimeIsOpen = filterVisitor.getMinFirstEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMaxFirstEvidenceTime() != null)
		{
			maxFirstTime = filterVisitor.getMaxFirstEvidenceTime().getDateTime();
			maxFirstTimeIsOpen = filterVisitor.getMaxFirstEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMinLastEvidenceTime() != null)
		{
			minLastTime = filterVisitor.getMinLastEvidenceTime().getDateTime();
			minLastTimeIsOpen = filterVisitor.getMinLastEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMaxLastEvidenceTime() != null)
		{
			maxLastTime = filterVisitor.getMaxLastEvidenceTime().getDateTime();
			maxLastTimeIsOpen = filterVisitor.getMaxLastEvidenceTime().getOperator().isOpen();
		}

		if (filterVisitor.getMinUpdateTime()!= null)
		{
			minUpdateTime = filterVisitor.getMinUpdateTime().getDateTime();
			minUpdateTimeIsOpen = filterVisitor.getMinUpdateTime().getOperator().isOpen();
		}

		if (filterVisitor.getMaxUpdateTime()!= null)
		{
			maxUpdateTime = filterVisitor.getMaxUpdateTime().getDateTime();
			maxUpdateTimeIsOpen = filterVisitor.getMaxUpdateTime().getOperator().isOpen();
		}

		if (filterVisitor.isThresholdFilter())
		{
			anomalyThreshold = filterVisitor.getThresholdValue();
		}

		if (filterVisitor.getEscapeCharValue() != null)
		{
			escapeChar = filterVisitor.getEscapeCharValue();
		}

		metricPaths = filterVisitor.getMetricPathAnds();
		likeMetricPaths = filterVisitor.getMetricPathLikeAnds();


		boolean noPathQueries = likeMetricPaths.isEmpty() && metricPaths.isEmpty();

		List<Activity> acts = new ArrayList<Activity>();
		if (noPathQueries && filterVisitor.getOredExpressions().isEmpty())
		{
			// simplest query, no or expressions and no metric path queries
			acts = m_DataStore.getActivitiesRange(minTime, minTimeIsOpen, maxTime, maxTimeIsOpen, 
					minFirstTime, minFirstTimeIsOpen, maxFirstTime, maxFirstTimeIsOpen, 
					minLastTime, minLastTimeIsOpen, maxLastTime, maxLastTimeIsOpen, 
					minUpdateTime, minUpdateTimeIsOpen, maxUpdateTime, maxUpdateTimeIsOpen,
					anomalyThreshold,  
					null, null, escapeChar);
		}
		else if (filterVisitor.getOredExpressions().isEmpty())
		{
			// more compliated query, no or expression but a list of (like) metric path
			// expressions that have to be anded together. 
			List<Activity> queryResults = new ArrayList<Activity>();
			String likePath = null;
			if (likeMetricPaths.size() > 0)
			{
				likePath = likeMetricPaths.get(0);
			}
			String metricPath = null;
			if (metricPaths.size() > 0)
			{
				metricPath = metricPaths.get(0);
			}

			queryResults = m_DataStore.getActivitiesRange(minTime, minTimeIsOpen, maxTime, maxTimeIsOpen, 
					minFirstTime, minFirstTimeIsOpen, maxFirstTime, maxFirstTimeIsOpen, 
					minLastTime, minLastTimeIsOpen, maxLastTime, maxLastTimeIsOpen, 
					minUpdateTime, minUpdateTimeIsOpen, maxUpdateTime, maxUpdateTimeIsOpen,
					anomalyThreshold,  
					metricPath, likePath, escapeChar);

			if (metricPaths.size() <= 1 && likeMetricPaths.size() <= 1)
			{
				acts.addAll(queryResults);
			}
			
			if (likeMetricPaths.size() > 1)
			{
				// check all paths present
				List<Integer> ids = new ArrayList<Integer>();
				for (Activity act : queryResults)
				{
					ids.add(act.getId());
				}

				Map<Integer, List<RelatedMetric>> rmById = m_DataStore.getRelatedMetricsBulk(ids);


				for (Activity act : queryResults)
				{
					List<RelatedMetric> rms = rmById.get(act.getId());

					boolean foundAllPaths = true;

					for (int i=1; i<likeMetricPaths.size(); ++i) // skip the first path as used in the query
					{
						String regex = sqlLikeToRegEx(likeMetricPaths.get(i), escapeChar.charAt(0)); 

						boolean found = false;
						for (RelatedMetric rm : rms)
						{
							if (rm.getMetricPath().matches(regex))
							{
								found = true;
								break;
							}
						}

						if (found == false)
						{
							foundAllPaths = false;
							break;
						}
					}

					if (foundAllPaths)
					{
						acts.add(act);
					}
				}
			}

			if (metricPaths.size() > 1)
			{
				// check all paths present
				List<Integer> ids = new ArrayList<Integer>();
				for (Activity act : queryResults)
				{
					ids.add(act.getId());
				}

				Map<Integer, List<RelatedMetric>> rmById = m_DataStore.getRelatedMetricsBulk(ids);


				for (Activity act : queryResults)
				{
					List<RelatedMetric> rms = rmById.get(act.getId());

					boolean foundAllPaths = true;

					for (int i=1; i<metricPaths.size(); ++i) // skip the first path as used in the query
					{
						boolean found = false;
						for (RelatedMetric rm : rms)
						{
							if (rm.getMetricPath().equals(metricPaths.get(i)))
							{
								found = true;
								break;
							}
						}

						if (found == false)
						{
							foundAllPaths = false;
							break;
						}
					}

					if (foundAllPaths)
					{
						acts.add(act);
					}
				}
			}

		}
		else if (filterVisitor.getOredExpressions().size() > 0)
		{
			// this query contains or expressions which mean multiple calls
			// have to be made.
			for (OdataExpressionVisitor orVisitor : filterVisitor.getOredExpressions())
			{
				OdataExpressionVisitor mergedVisitor = filterVisitor.mergeExpression(orVisitor);
				acts.addAll(runQuery(mergedVisitor));
			}
		}

		return acts;
	}
	
	
	/**
	 * Convert an SQL like query into a regular expression.
	 * '%' is replaced by '.*' and '_' by '.' unless they are protected 
	 * by <code>escapeChar</code>
	 * 
	 * @param likeQuery
	 * @param escapeChar
	 * @return
	 */
	static public String sqlLikeToRegEx(String likeQuery, char escapeChar)
	{
		// If the escape char is  a special regex character it will be escaped here
		String replaced = com.prelert.proxy.regex.RegExUtilities.escapeRegex(likeQuery); 

		if (com.prelert.proxy.regex.RegExUtilities.s_SpecialCharsSet.contains(escapeChar))
		{
			// replace all occurrences of % and _ with unless they are escaped. 
			replaced = replaced.replaceAll("(?<!\\" + escapeChar + ")%", ".*");
			replaced = replaced.replaceAll("(?<!\\" + escapeChar + ")_", ".");
			// remove the escape char
			replaced = replaced.replace("\\" + escapeChar + "_", "_");
			replaced = replaced.replace("\\" + escapeChar + "%", "%");
		}
		else
		{
			// replace all occurrences of % and _ with unless they are escaped. 
			replaced = replaced.replaceAll("(?<!" + escapeChar + ")%", ".*");
			replaced = replaced.replaceAll("(?<!" + escapeChar + ")_", ".");
			// remove the escape char
			replaced = replaced.replace(escapeChar + "_", "_");
			replaced = replaced.replace(escapeChar + "%", "%");
		}

		return replaced;
	}
	
	/**
	 * Return activities according to the query options
	 * 
	 * @param queryInfo
	 * @return
	 */
	public List<Activity> queryActivities(QueryInfo queryInfo)
	{	
		List<Activity> acts = new ArrayList<Activity>();
		
		if (queryInfo.filter != null)
		{
			OdataExpressionVisitor filterVisitor = new OdataExpressionVisitor();
			queryInfo.filter.visit(filterVisitor);
			
			if ((filterVisitor.getExpressionType() == OdataExpressionVisitor.ExpressionType.OR) )
				//|| (filterVisitor.getExpressionType() == OdataExpressionVisitor.ExpressionType.BOOLPAREN))
			{
				for (OdataExpressionVisitor visitor : filterVisitor.getOredExpressions())
				{
					List<Activity> qResults = runQuery(visitor);
					acts.addAll(qResults);
				}
				
			}
			else
			{
				acts = runQuery(filterVisitor);
			}

			
			// de-duplicate
			Map<Integer, Activity> activitiesById = new HashMap<Integer, Activity>();
			for (Activity act : acts)
			{
				activitiesById.put(act.getId(), act);
			}
			acts = new ArrayList<Activity>(activitiesById.values());
		}
		else
		{
			// no filter get everything
			Date minTime = null;
			boolean minTimeIsOpen = false;
			Date maxTime = null;
			boolean maxTimeIsOpen = false;
			Date minFirstTime = null;
			boolean minFirstTimeIsOpen = false;
			Date maxFirstTime = null;
			boolean maxFirstTimeIsOpen = false;
			Date minLastTime = null;
			boolean minLastTimeIsOpen = false;
			Date maxLastTime = null;
			boolean maxLastTimeIsOpen = false;
			Date minUpdateTime = null;
			boolean minUpdateTimeIsOpen = false;
			Date maxUpdateTime = null;
			boolean maxUpdateTimeIsOpen = false;
			
			acts = m_DataStore.getActivitiesRange(minTime, minTimeIsOpen, maxTime, maxTimeIsOpen, 
					minFirstTime, minFirstTimeIsOpen, maxFirstTime, maxFirstTimeIsOpen, 
					minLastTime, minLastTimeIsOpen, maxLastTime, maxLastTimeIsOpen, 
					minUpdateTime, minUpdateTimeIsOpen, maxUpdateTime, maxUpdateTimeIsOpen,
					1,  
					null, null, null);
		}
		
		
		// apply ordering
		if (queryInfo.orderBy != null)
		{
			OrderByExpressionVisitor orderByVisitor = new OrderByExpressionVisitor();
			queryInfo.orderBy.get(0).getExpression().visit(orderByVisitor);

			if (orderByVisitor.getPropertyName() != null)
			{
				orderActivities(acts, orderByVisitor.getPropertyName(), 
						queryInfo.orderBy.get(0).getDirection() == Direction.ASCENDING);
			}
		}
		else
		{
			// default ordering
			orderActivities(acts, "PeakEvidenceTime", true);
		}
		
		
		// paging 
		Integer skip = queryInfo.skip;
		Integer top = queryInfo.top;

		int from = 0;
		if (skip != null)
		{
			from = Math.min(skip, acts.size());
		}

		int to = acts.size();
		if (top != null)
		{
			to = Math.min(from + top, acts.size());
		}
		
		acts = acts.subList(from, to);
		
		
		// expand related metrics
		if ((queryInfo.expand.size() > 0) && 
				RELATEDMETRICS_NAV.equals(queryInfo.expand.get(0).getPropertyName()))
		{
			// Batch up the activity ids for more efficient query.
			List<Integer> evidenceIds = new ArrayList<Integer>();
			for (Activity act : acts)
			{
				evidenceIds.add(act.getId());
			}
			
			
			Map<Integer, List<RelatedMetric>> relatedMetricByActivityId = m_DataStore.getRelatedMetricsBulk(evidenceIds);
			for (Activity act : acts)
			{
				List<RelatedMetric> rm = relatedMetricByActivityId.get(act.getId());
				if (rm != null)
				{
					act.setRelatedMetrics(rm);
				}
			}
		}

		return acts;
	}

	
	/**
	 * Sort the list of Activities by one of the sortable fields.
	 * 
	 * Sorting can only be done on these fields:<br/>
	 * <ul>
	 * <li>anomalyScore</li>
	 * <li>PeakEvidenceTime</li>
	 * </ul>
	 * 
	 * Secondary sorting is by the other sort field e.g. if sorting by 
	 * <code>anomalyScore</code> then the secondary sort order is 
	 * <code>PeakEvidenceTime</code> and vice versa. 
	 * 
	 * @param acts
	 * @param property property to sort by
	 * @param ascending sort ascending if true else descending
	 * @return The sorted activities. This container is the same one as
	 * the <code>acts</code> parameter
	 */
	private List<Activity> orderActivities(List<Activity> acts, String property, final boolean ascending)
	{
		if ("anomalyScore".equalsIgnoreCase(property))
		{
			Collections.sort(acts, new Activity.ScoreComparator(ascending));
		}
		else if ("PeakEvidenceTime".equalsIgnoreCase(property))
		{
			Collections.sort(acts, new Activity.PeakTimeComparator(ascending));
		}
		else if ("FirstEvidenceTime".equalsIgnoreCase(property))
		{
			Collections.sort(acts, new Activity.FirstTimeComparator(ascending));
		}
		else if ("LastEvidenceTime".equalsIgnoreCase(property))
		{
			Collections.sort(acts, new Activity.LastTimeComparator(ascending));
		}
		else if ("UpdateTime".equalsIgnoreCase(property))
		{
			Collections.sort(acts, new Activity.UpdateTimeComparator(ascending));
		}
		
		return acts;
	}
	
	
	/**
	 * DataStore accessor 
	 * @return
	 */
	public DataStore getDataStore()
	{
		return m_DataStore;
	}
	
	/**
	 * The datastore bean needs to be injected for this 
	 * class to function
	 * 
	 * @param dataStore
	 */
	public void setDataStore(DataStore dataStore)
	{
		m_DataStore = dataStore;
	}
	
	
	/**
	 * MetricManager accessor 
	 * @return
	 */
	public MetricManager getMetricManager()
	{
		return m_MetricManager;
	}
	
	/**
	 * The datastore bean needs to be injected for this 
	 * class to function
	 * 
	 * @param metricManager
	 */
	public void setMetricManager(MetricManager metricManager)
	{
		m_MetricManager = metricManager;
	}

	
	/**
	 * ConfigurationManager  
	 * @return
	 */ 
	public ConfigurationManager getConfigurationManager()
	{
		return m_ConfigurationManager;
	}
	
	/**
	 * Sets the configuration manager and reads the active metrics
	 * from the database.
	 * @param configManager
	 */
	public void setConfigurationManager(ConfigurationManager configManager)
	{
		m_ConfigurationManager = configManager;
		m_ConfigurationManager.loadActiveMetrics();
	}

}
