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

package com.prelert.api.test.producer;

import static org.odata4j.examples.JaxRsImplementation.JERSEY;

import org.apache.log4j.Logger;
import org.odata4j.examples.ODataServerFactory;
import org.odata4j.producer.resources.DefaultODataProducerProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.api.ConfigurationManager;
import com.prelert.api.MetricManager;
import com.prelert.api.QueryAPI;
import com.prelert.api.dao.DataStore;


/**
 * Test ODATA server. 
 */
public class ProducerExample 
{
	private static Logger s_Logger = Logger.getLogger(ProducerExample.class);

	public static final String endpointUri = "http://localhost:8080/prelertApi/prelert.svc";
	


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		ProducerExample example = new ProducerExample();
	    example.run(args);
	}
	
	private void run(String[] args) throws Exception
	{
		System.out.println("Please direct your browser to " + endpointUri + "Customers");
		
		QueryAPI queryApi = new QueryAPI();
		queryApi.setDataStore(loadDataStoreBean());
		queryApi.setConfigurationManager(loadConfigManagerBean());
		
		MetricManager manager = loadMetricManagerBean();
		manager.start();
		queryApi.setMetricManager(manager);
		
		// register the producer as the static instance, then launch the http server
		DefaultODataProducerProvider.setInstance(queryApi);
		new ODataServerFactory(JERSEY).hostODataServer(endpointUri);
	}

	
	/**
	 * Load the datastore bean from applicationContext.xml
	 */
	private DataStore loadDataStoreBean() throws Exception
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
	private MetricManager loadMetricManagerBean() throws Exception 
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
