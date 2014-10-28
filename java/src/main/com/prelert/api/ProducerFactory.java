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

import java.util.Properties;

import org.apache.log4j.Logger;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.ODataProducerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.api.dao.DataStore;


/** 
 * ODATA producer factory. 
 * This is the entry point for the Tomcat servlet.  
 */
public class ProducerFactory implements ODataProducerFactory 
{
	static final public Logger s_Logger = Logger.getLogger(ProducerFactory.class);
	
	static int count = 0;
	
	static final private QueryAPI s_QueryAPI;
	static 
	{
		s_QueryAPI = ProducerFactory.createQueryApi();
	}
	
	@Override
	public ODataProducer create(Properties properties) 
	{
		return s_QueryAPI;
	}
	
	static private QueryAPI createQueryApi()
	{
		try 
		{
			QueryAPI api = new QueryAPI();
			api.setDataStore(loadDataStoreBean());
			api.setConfigurationManager(loadConfigManagerBean());
			
			MetricManager manager = loadMetricManagerBean();
			manager.start();
			api.setMetricManager(manager);
			
			count++;
			s_Logger.info("ODATAProducer Count = " + count);

			return api;
		}		
		catch (Exception e) 
		{
			s_Logger.error("Failed to create QueryAPI");
			s_Logger.error(e);
		}
		
		
		return null;
	}
	
	
	
	/**
	 * Load the datastore bean from applicationContext.xml
	 */
	static private DataStore loadDataStoreBean() throws Exception
	{
		s_Logger.info("Loading datastore bean");
		
		ApplicationContext applicationContext;
		DataStore datastore = null;
		try
		{
			applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");	
			datastore = applicationContext.getBean("dataStore", DataStore.class);
		}
		catch (BeansException be)
		{
			s_Logger.fatal("Failed to load applicationContext.xml");
			throw be;
		}
		catch (Exception e)
		{
			s_Logger.fatal("Fatal error initialising DAOs", e);
			throw e;
		}
		
		return datastore;
	}
	
	
	/**
	 * Load the metric manager bean
	 * @return
	 */
	static private MetricManager loadMetricManagerBean() throws Exception 
	{
		s_Logger.info("Loading MetricManager bean");
		
		ApplicationContext applicationContext;
		MetricManager manager = null;
		try
		{
			applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");	
			manager = applicationContext.getBean("metricManager", MetricManager.class);
		}
		catch (BeansException be)
		{
			s_Logger.fatal("Failed to load applicationContext.xml");
			throw be;
		}
		catch (Exception e)
		{
			s_Logger.fatal("Fatal error initialising DAOs", e);
			throw e;
		}
		
		return manager;
	}
	
	
	/**
	 * Load the config manager bean
	 * @return
	 */
	static private ConfigurationManager loadConfigManagerBean() throws Exception 
	{
		s_Logger.info("Loading ConfigurationManager bean");
		
		ApplicationContext applicationContext;
		ConfigurationManager manager = null;
		try
		{
			applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");	
			manager = applicationContext.getBean("configManager", ConfigurationManager.class);
		}
		catch (BeansException be)
		{
			s_Logger.fatal("Failed to load applicationContext.xml");
			throw be;
		}
		catch (Exception e)
		{
			s_Logger.fatal("Fatal error initialising DAOs", e);
			throw e;
		}
		
		return manager;
	}

}
