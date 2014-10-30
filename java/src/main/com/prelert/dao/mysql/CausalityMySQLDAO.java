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

package com.prelert.dao.mysql;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.CausalityDAO; 
import com.prelert.data.CausalityData;
import com.prelert.data.ProbableCause;
import com.prelert.data.TimeFrame;
import com.prelert.data.Evidence;
import com.prelert.dao.spring.CausalityDataRowMapper;
import com.prelert.dao.spring.EvidenceRowMapper;
import com.prelert.dao.spring.ProbableCauseRowMapper;
import com.prelert.dao.spring.ProbableCauseExportRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the CausalityDAO interface which 
 * uses calls to stored procedures to obtain information for Causality views.
 * @author Pete Harverson
 */
public class CausalityMySQLDAO extends SimpleJdbcDaoSupport implements CausalityDAO
{
	static Logger s_Logger = Logger.getLogger(CausalityMySQLDAO.class);
	

	@Override
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs,
												boolean maxOneFeaturePerSeries)
	{
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (maxOneFeaturePerSeries ? 1 : 0);

		String query = "CALL probable_cause_list(?, ?, ?)";
		
		String debugQuery = "CALL probable_cause_list({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs,
											tinyInt);
		s_Logger.debug("getProbableCauses() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseRowMapper(), evidenceId,
					timeSpanSecs, tinyInt);
	}
	

	@Override
	public List<ProbableCause> getProbableCausesForExport(int evidenceId,
							int timeSpanSecs, boolean maxOneFeaturePerSeries)
	{
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (maxOneFeaturePerSeries ? 1 : 0);

		String query = "CALL probable_cause_list_export(?, ?, ?)";
		
		String debugQuery = "CALL probable_cause_list_export({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs,
											tinyInt);
		s_Logger.debug("getProbableCausesForExport() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseExportRowMapper(), evidenceId,
					timeSpanSecs, tinyInt);
	}


	@Override
	public List<ProbableCause> getProbableCausesInBulk(List<Integer> evidenceIds,
												boolean maxOneFeaturePerSeries)
	{
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (maxOneFeaturePerSeries ? 1 : 0);

		String evidenceIdStr = StringUtils.join(evidenceIds, ',');

		String query = "CALL probable_cause_list_api(?, ?)";
		
		String debugQuery = "CALL probable_cause_list_api({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, evidenceIdStr,
											tinyInt);
		s_Logger.debug("getProbableCausesInBulk() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query,
					new ProbableCauseExportRowMapper(), evidenceIdStr,
					tinyInt);
	}


    @Override
    public List<CausalityData> getCausalityData(int evidenceId, 
			List<String> returnAttributes, List<String> primaryFilterNamesNull,
			List<String> primaryFilterNamesNotNull, List<String> primaryFilterValues,
			String secondaryFilterName, String secondaryFilterValue)
    {
		// Query calls a stored procedure in the form:
		// incident_summary_grid(evidenceId, attributesIn,
		//                       primaryFilterNamesNull,
		//                       primaryFilterNamesNotNull, primaryFilterValues,
		//                       secondaryFilterName, secondaryFilterValue)
		String query = "CALL incident_summary_grid(?, ?, ?, ?, ?, ?, ?)";

		String returnAttributesArg = ServerUtil.prepareFilterArgument(returnAttributes);
		String primaryFilterNamesNullArg = ServerUtil.prepareFilterArgument(primaryFilterNamesNull);
		String primaryFilterNamesNotNullArg = ServerUtil.prepareFilterArgument(primaryFilterNamesNotNull);
		String primaryFilterValuesArg = ServerUtil.prepareFilterArgument(primaryFilterValues);

		String debugQuery = "CALL incident_summary_grid({0}, {1}, {2}, {3}, {4}, {5}, {6})";
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


	@Override
    public List<Evidence> getFirstPage(boolean singleDescription, int evidenceId,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	// Query calls a stored procedure in the form:
		// cause_list_notifications_first_page(singleDescription, evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "CALL cause_list_notifications_first_page(?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "CALL cause_list_notifications_first_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, tinyInt, evidenceId, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getFirstPage() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				tinyInt, evidenceId, filterAttrsArg, filterValsArg, pageSize);
    }
    

    @Override
    public List<Evidence> getLastPage(boolean singleDescription, int evidenceId,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	// Query calls a stored procedure in the form:
		// cause_list_notifications_last_page(singleDescription, evidenceId, attributesIn, valuesIn, pageSize)
    	String query = "CALL cause_list_notifications_last_page(?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "CALL cause_list_notifications_last_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, tinyInt, evidenceId, 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getLastPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				tinyInt, evidenceId, filterAttrsArg, filterValsArg, pageSize);	
    }
    

    @Override
    public List<Evidence> getNextPage(boolean singleDescription, int bottomRowId, Date bottomRowTime,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	// Query calls a stored procedure in the form:
		// cause_list_notifications_next_page(singleDescription, bottomRowId, bottomRowTime, attributesIn, valuesIn, pageSize)
		String query = "CALL cause_list_notifications_next_page(?, ?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "CALL cause_list_notifications_next_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, bottomRowId, 
				ServerUtil.formatTimeField(bottomRowTime, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getNextPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				tinyInt, bottomRowId, bottomRowTime, 
				filterAttrsArg, filterValsArg, pageSize);
    }


    @Override
    public List<Evidence> getPreviousPage(boolean singleDescription, int topRowId, Date topRowTime,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	// Query calls a stored procedure in the form:
		// cause_list_notifications_previous_page(singleDescription, topRowId, topRowTime, attributesIn, valuesIn, pageSize)
		String query = "CALL cause_list_notifications_previous_page(?, ?, ?, ?, ?, ?)";
		
		String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "CALL cause_list_notifications_previous_page({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, tinyInt, topRowId, 
				ServerUtil.formatTimeField(topRowTime, TimeFrame.SECOND),
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getPreviousPage() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
				tinyInt, topRowId, topRowTime, 
				filterAttrsArg, filterValsArg, pageSize);
    }


	@Override
    public List<Evidence> getAtTime(boolean singleDescription, int evidenceId, Date time,
            List<String> filterAttributes, List<String> filterValues, int pageSize)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

		// Query calls a stored procedure in the form:
		// cause_list_notifications_at_time(singleDescription, evidenceId, time, attributesIn, valuesIn, pageSize)
    	String query = "CALL cause_list_notifications_at_time(?, ?, ?, ?, ?, ?)";
    	
    	String filterAttrsArg = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValsArg = ServerUtil.prepareFilterArgument(filterValues);
		
		String debugQuery = "CALL cause_list_notifications_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, tinyInt, evidenceId, 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), 
				filterAttrsArg, filterValsArg, pageSize);
		s_Logger.debug("getAtTime() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
				tinyInt, evidenceId, time, filterAttrsArg, filterValsArg, pageSize);
    }


	@Override
    public Evidence getEarliestEvidence(boolean singleDescription, int evidenceId,
            List<String> filterAttributes, List<String> filterValues)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL cause_list_notifications_min_time(?, ?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), tinyInt, evidenceId, filterAttribute, filterValue);	
    }


    @Override
    public Evidence getLatestEvidence(boolean singleDescription, int evidenceId,
            List<String> filterAttributes, List<String> filterValues)
    {
		// MySQL's BOOLEANs are really TINYINTs
		int tinyInt = (singleDescription ? 1 : 0);

    	String filterAttribute = ServerUtil.prepareFilterArgument(filterAttributes);
		String filterValue = ServerUtil.prepareFilterArgument(filterValues);
		
		String query = "CALL cause_list_notifications_max_time(?, ?, ?, ?)";
		return getSimpleJdbcTemplate().queryForObject(query.toString(), 
				new EvidenceRowMapper(), tinyInt, evidenceId, filterAttribute, filterValue);
    } 

}
