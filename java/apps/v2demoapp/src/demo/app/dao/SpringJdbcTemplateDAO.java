/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package demo.app.dao;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


/**
 * Abstract base class for data access objects (DAOs) that use the SimpleJdbcTemplate
 * from the Spring Framework to manage core JDBC workflow.
 * @author Pete Harverson
 */
public abstract class SpringJdbcTemplateDAO
{
	/**
	 * The Java-5-based JDBC template used for managing core workflow.
	 */
	protected SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
}
