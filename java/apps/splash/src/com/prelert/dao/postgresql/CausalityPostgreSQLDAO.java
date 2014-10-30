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

package com.prelert.dao.postgresql;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceModelListRowMapper;
import com.prelert.dao.EvidenceModelRowMapper;
import com.prelert.dao.ProbableCauseRowMapper;
import com.prelert.dao.SpringJdbcTemplateDAO;
import com.prelert.data.ProbableCause;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the CausalityDAO interface which 
 * uses calls to functions to obtain information for Causality views.
 * @author Pete Harverson
 */
public class CausalityPostgreSQLDAO extends SpringJdbcTemplateDAO 
	implements CausalityDAO
{
	static Logger logger = Logger.getLogger(CausalityPostgreSQLDAO.class);
	
	private int m_PageSize = 20;
	

	@Override
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs)
	{
		String query = "select * from probable_cause_list(?, ?)";
		
		String debugQuery = "select * from probable_cause_list({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs);
		logger.debug("getProbableCauses() query: " + debugQuery);

		return m_SimpleJdbcTemplate.query(query, 
					new ProbableCauseRowMapper(), evidenceId, timeSpanSecs);
	}
	
	
	@Transactional(readOnly = true)
	@Override
	public List<EvidenceModel> getFirstPage(int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
		// Query calls a function of the form:
		// cause_list_notifications_first_page(evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_first_page(?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, 
				filterAttrsArg, filterValsArg, m_PageSize);
		logger.debug("getFirstPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				evidenceId, filterAttrsArg, filterValsArg, m_PageSize);
	}


	@Transactional(readOnly = true)
	@Override
	public List<EvidenceModel> getLastPage(int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
    	// Query calls a function of the form:
		// cause_list_notifications_last_page(evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_last_page(?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, 
				filterAttrsArg, filterValsArg, m_PageSize);
		logger.debug("getLastPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				evidenceId, filterAttrsArg, filterValsArg, m_PageSize);	
	}
	
	
	@Transactional(readOnly = true)
	@Override
	public List<EvidenceModel> getNextPage(int bottomRowId, Date bottomRowTime,
	        List<String> filterAttributes, List<String> filterValues)
	{
    	// Query calls a function of the form:
		// cause_list_notifications_next_page(bottomRowId, bottomRowTime, attributesIn, valuesIn, pageSize)
		String query = "select * from cause_list_notifications_next_page(?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_next_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, bottomRowId, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, m_PageSize);
		logger.debug("getNextPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				bottomRowId, bottomRowTime, 
				filterAttrsArg, filterValsArg, m_PageSize);
	}


	@Transactional(readOnly = true)
	@Override
	public List<EvidenceModel> getPreviousPage(int topRowId, Date topRowTime,
	        List<String> filterAttributes, List<String> filterValues)
	{
		// Query calls a function of the form:
		// cause_list_notifications_previous_page(topRowId, topRowTime, attributesIn, valuesIn, pageSize)
		String query = "select * from cause_list_notifications_previous_page(?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_previous_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, topRowId, 
				ServerUtil.formatTimeField(topRowTime, TimeFrame.SECOND),
				filterAttrsArg, filterValsArg, m_PageSize);
		logger.debug("getPreviousPage() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(),
				topRowId, topRowTime, 
				filterAttrsArg, filterValsArg, m_PageSize);
	}
	
	
	@Transactional(readOnly = true)
	@Override
	public List<EvidenceModel> getAtTime(int evidenceId, Date time,
	        List<String> filterAttributes, List<String> filterValues)
	{
		// Query calls a function of the form:
		// cause_list_notifications_at_time(evidenceId, time, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_at_time(?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_at_time({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, m_PageSize);
		logger.debug("getAtTime() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.queryForObject(query, new EvidenceModelListRowMapper(), 
				evidenceId, time, filterAttrsArg, filterValsArg, m_PageSize);
	}


	@Override
	public EvidenceModel getEarliestEvidence(int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from cause_list_notifications_min_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), evidenceId, filterAttribute, filterValue);
	}


	@Override
	public EvidenceModel getLatestEvidence(int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
    	String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from cause_list_notifications_max_time(?, ?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				new EvidenceModelRowMapper(), evidenceId, filterAttribute, filterValue);
	}

}
