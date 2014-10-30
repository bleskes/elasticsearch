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
 ***********************************************************/

package com.prelert.proxy.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.dao.TimeSeriesDAO;

/**
 * Test helper class.
 * 
 * Sets up a connection to a MySQL or PostgreSQL database and creates the DAOs
 * to access data from that database.
 */
public class LocalDatabaseTestUtil 
{
	private CausalityDAO m_CausalityDAO;
	private DataSourceDAO	m_DataSourceDAO;
	private EvidenceDAO m_EvidenceDAO;
	private IncidentDAO m_IncidentDAO;
	private TimeSeriesDAO	m_TimeSeriesDAO;
	
	public LocalDatabaseTestUtil() throws Exception
	{			
		createDAOsFromSpring();
	}
	
	private void createDAOsFromSpring()
	{
		ApplicationContext applicationContext =  
			new ClassPathXmlApplicationContext("referenceDAOs.xml");
		
		m_CausalityDAO = applicationContext.getBean("referenceCausalityDAO", CausalityDAO.class);
		m_DataSourceDAO = applicationContext.getBean("referenceDataSourceDAO", DataSourceDAO.class);
		m_EvidenceDAO = applicationContext.getBean("referenceEvidenceDAO", EvidenceDAO.class);
		m_IncidentDAO = applicationContext.getBean("referenceIncidentDAO", IncidentDAO.class);
		m_TimeSeriesDAO = applicationContext.getBean("referenceTimeSeriesDAO", TimeSeriesDAO.class);
	}	
	
	public CausalityDAO getCausalityDAO() 
	{
		return m_CausalityDAO;
	}
	
	public void setCausalityDAO(CausalityDAO dao)
	{
		m_CausalityDAO = dao;
	}

	public DataSourceDAO getDataSourceDAO() 
	{
		return m_DataSourceDAO;
	}
	
	public void getDataSourceDAO(DataSourceDAO dao) 
	{
		m_DataSourceDAO = dao;
	}

	public EvidenceDAO getEvidenceDAO() 
	{
		return m_EvidenceDAO;
	}

	public void setEvidenceDAO(EvidenceDAO dao) 
	{
		m_EvidenceDAO = dao;
	}

	public IncidentDAO getIncidentDAO() 
	{
		return m_IncidentDAO;
	}
	
	public void getIncidentDAO(IncidentDAO dao) 
	{
		m_IncidentDAO = dao;
	}

	public TimeSeriesDAO getTimeSeriesDAO() 
	{
		return m_TimeSeriesDAO;
	}
	
	public void setTimeSeriesDAO(TimeSeriesDAO dao) 
	{
		m_TimeSeriesDAO = dao;
	}
}
