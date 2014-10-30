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
 ***********************************************************/

package com.prelert.dao.postgresql;

import java.text.MessageFormat;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.CausalityDAO;

import com.prelert.data.CausalityData;
import com.prelert.data.ProbableCause;
import com.prelert.data.TimeFrame;
import com.prelert.data.Evidence;
import com.prelert.dao.spring.CausalityDataRowMapper;
import com.prelert.dao.spring.EvidenceRowMapper;
import com.prelert.dao.spring.EvidenceListRowMapper;
import com.prelert.dao.spring.ProbableCauseExportRowMapper;
import com.prelert.dao.spring.ProbableCauseRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the CausalityDAO interface which 
 * uses calls to functions to obtain information for Causality views.
 * @author Pete Harverson
 */
public class CausalityPostgreSQLDAO extends SimpleJdbcDaoSupport 
	implements CausalityDAO
{
	static Logger s_Logger = Logger.getLogger(CausalityPostgreSQLDAO.class);
	

	@Override
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs,
												boolean maxOneFeaturePerSeries)
	{
		String query = "select * from probable_cause_list(?, ?, ?)";
		
		String debugQuery = "select * from probable_cause_list({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs,
                                        	maxOneFeaturePerSeries);
		s_Logger.debug("getProbableCauses() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseRowMapper(), evidenceId, timeSpanSecs,
					maxOneFeaturePerSeries);
	}
	
	
	@Override
	public List<ProbableCause> getProbableCausesForExport(int evidenceId, int timeSpanSecs,
												boolean maxOneFeaturePerSeries)
	{
		String query = "select * from probable_cause_list_export(?, ?, ?)";
		
		String debugQuery = "select * from probable_cause_list_export({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs,
                                        	maxOneFeaturePerSeries);
		s_Logger.debug("getProbableCausesForExport() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseExportRowMapper(), evidenceId, timeSpanSecs,
					maxOneFeaturePerSeries);
	}


	@Override
	public List<ProbableCause> getProbableCausesInBulk(List<Integer> evidenceIds,
												boolean maxOneFeaturePerSeries)
	{
		String evidenceIdStr = StringUtils.join(evidenceIds, ',');

		String query = "select * from probable_cause_list_api(?, ?)";
		
		String debugQuery = "select * from probable_cause_list_api({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, evidenceIdStr,
                                        	maxOneFeaturePerSeries);
		s_Logger.debug("getProbableCausesInBulk() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseRowMapper(), evidenceIdStr,
					maxOneFeaturePerSeries);
	}


    @Override
    public List<CausalityData> getCausalityData(int evidenceId, 
			List<String> returnAttributes, List<String> primaryFilterNamesNull,
			List<String> primaryFilterNamesNotNull, List<String> primaryFilterValues,
			String secondaryFilterName, String secondaryFilterValue)
	{
		// Query calls a stored function of the form:
		// incident_summary_grid(evidenceId, attributesIn,
		//                       primaryFilterNamesNull,
		//                       primaryFilterNamesNotNull, primaryFilterValues,
		//                       secondaryFilterName, secondaryFilterValue)
		String query = "select * from incident_summary_grid(?, ?, ?, ?, ?, ?, ?)";

		String returnAttributesArg = ServerUtil.prepareFilterArgument(returnAttributes);
		String primaryFilterNamesNullArg = ServerUtil.prepareFilterArgument(primaryFilterNamesNull);
		String primaryFilterNamesNotNullArg = ServerUtil.prepareFilterArgument(primaryFilterNamesNotNull);
		String primaryFilterValuesArg = ServerUtil.prepareFilterArgument(primaryFilterValues);

		String debugQuery = "select * from incident_summary_grid({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, returnAttributesArg,
				primaryFilterNamesNullArg,
				primaryFilterNamesNotNullArg, primaryFilterValuesArg,
				secondaryFilterName, secondaryFilterValue);
		s_Logger.debug("getCausalityData() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new CausalityDataRowMapper(),
				evidenceId, returnAttributesArg,
				primaryFilterNamesNullArg,
				primaryFilterNamesNotNullArg, primaryFilterValuesArg,
				secondaryFilterName, secondaryFilterValue);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getFirstPage(boolean singleDescription, int evidenceId,
	        List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		// Query calls a function of the form:
		// cause_list_notifications_first_page(singleDescription, evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_first_page(?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_first_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, singleDescription, evidenceId, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getFirstPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				singleDescription, evidenceId, filterAttrsArg, filterValsArg, pageSize);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getLastPage(boolean singleDescription, int evidenceId,
	        List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
    	// Query calls a function of the form:
		// cause_list_notifications_last_page(singleDescription, evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_last_page(?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_last_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, singleDescription, evidenceId, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getLastPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				singleDescription, evidenceId, filterAttrsArg, filterValsArg, pageSize);	
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getNextPage(boolean singleDescription, int bottomRowId, Date bottomRowTime,
	        List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
    	// Query calls a function of the form:
		// cause_list_notifications_next_page(singleDescription, bottomRowId, bottomRowTime, attributesIn, valuesIn, pageSize)
		String query = "select * from cause_list_notifications_next_page(?, ?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, singleDescription, bottomRowId, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getNextPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				singleDescription, bottomRowId, bottomRowTime, 
				filterAttrsArg, filterValsArg, pageSize);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getPreviousPage(boolean singleDescription, int topRowId, Date topRowTime,
	        List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		// Query calls a function of the form:
		// cause_list_notifications_previous_page(singleDescription, topRowId, topRowTime, attributesIn, valuesIn, pageSize)
		String query = "select * from cause_list_notifications_previous_page(?, ?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, singleDescription, topRowId, 
				ServerUtil.formatTimeField(topRowTime, TimeFrame.SECOND),
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getPreviousPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
				singleDescription, topRowId, topRowTime, 
				filterAttrsArg, filterValsArg, pageSize);
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getAtTime(boolean singleDescription, int evidenceId, Date time,
	        List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		// Query calls a function of the form:
		// cause_list_notifications_at_time(singleDescription, evidenceId, time, attributesIn, valuesIn, pageSize)
    	String query = "select * from cause_list_notifications_at_time(?, ?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "select * from cause_list_notifications_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, singleDescription, evidenceId, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getAtTime() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
				singleDescription, evidenceId, time, filterAttrsArg, filterValsArg, pageSize);
	}


	@Override
	public Evidence getEarliestEvidence(boolean singleDescription, int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
		String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from cause_list_notifications_min_time(?, ?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), singleDescription, evidenceId, filterAttribute, filterValue);
	}


	@Override
	public Evidence getLatestEvidence(boolean singleDescription, int evidenceId,
	        List<String> filterAttributes, List<String> filterValues)
	{
    	String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "select * from cause_list_notifications_max_time(?, ?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), singleDescription, evidenceId, filterAttribute, filterValue);
	}

}
