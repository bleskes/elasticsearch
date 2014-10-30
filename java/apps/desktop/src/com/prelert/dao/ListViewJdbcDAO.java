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

package com.prelert.dao;

import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.server.ViewDirectory;

public class ListViewJdbcDAO extends SpringJdbcTemplateDAO implements ListViewDAO
{

	static Logger logger = Logger.getLogger(ListViewJdbcDAO.class);


	@Override
	public List<String> getAllColumns(ListView view)
	{
		String descQry = "desc " + view.getDatabaseView();
		logger.debug("getAllColumns() query: " + descQry);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("Field");
            }
		};
		
		return m_SimpleJdbcTemplate.query(descQry, mapper);
	}
	
	
	@Override
	public int getTotalRowCount(ListView view)
	{
		// Build the query.
		StringBuilder qryBlder = new StringBuilder("SELECT count(*) from ");
		qryBlder.append(view.getDatabaseView());
		qryBlder.append(' ');
		
		// Add in a WHERE clause if necessary.
		String filterAttribute = view.getFilterAttribute();
		if ( (filterAttribute != null) && (filterAttribute.length() > 0) )
		{
			qryBlder.append("WHERE ");
			qryBlder.append(filterAttribute);
			qryBlder.append(" = ?");
		}
		
		String countQry = qryBlder.toString();
		logger.debug("getTotalRowCount() query: " + countQry);
		
		int numRows = 0;
		
		if ( (filterAttribute != null) && (filterAttribute.length() > 0) )
		{
			numRows = m_SimpleJdbcTemplate.queryForInt(countQry, view.getFilterValue());
		}
		else
		{
			numRows = m_SimpleJdbcTemplate.queryForInt(countQry);
		}
		
		return numRows;
	}


	@Override
	public List<EventRecord> getRecords(ListView view, int offset, int limit,
			List<SortInformation> orderBy)
	{
		// Build the query.
		// e.g. SELECT description, entity, severity, cond_prob FROM evidence LIMIT offset, limit;
		StringBuilder qryBlder = new StringBuilder("SELECT * FROM ");
		qryBlder.append(view.getDatabaseView());
		qryBlder.append(' ');

		// Add in a WHERE clause if necessary.
		String filterAttribute = view.getFilterAttribute();
		if ( (filterAttribute != null) && (filterAttribute.length() > 0) )
		{
			qryBlder.append("WHERE ");
			qryBlder.append(filterAttribute);
			qryBlder.append(" = ?");
		}
		
		// Add in an ORDER BY clause if necessary.
		qryBlder.append(getOrderByClause(orderBy));
		
		qryBlder.append(" LIMIT ");
		qryBlder.append(limit);
		qryBlder.append(" OFFSET ");
		qryBlder.append(offset);
		
		String recordsQry = qryBlder.toString();
		logger.debug("getRecords() query: " + recordsQry);	

		ParameterizedRowMapper<EventRecord> mapper = new ParameterizedRowMapper<EventRecord>(){

			@Override
            public EventRecord mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				EventRecord record = new EventRecord();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				Object columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getObject(i);
					
					record.set(columnName, columnValue);
				}

	            return record;
            }
		};
		
		if ( (filterAttribute != null) && (filterAttribute.length() > 0) )
		{
			return m_SimpleJdbcTemplate.query(recordsQry, mapper, view.getFilterValue());
		}
		else
		{
			return m_SimpleJdbcTemplate.query(recordsQry, mapper);
		}
	}


	/**
	 * Returns the details on the row from the specified view with the
	 * given id. Note that the view must contain an id column.
	 * @param view the View to query.
	 * @param id the value of the id column for the row to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(ListView view, int id)
	{
		StringBuilder qryBlder = new StringBuilder("SELECT * FROM ");
		qryBlder.append(view.getDatabaseView());
		qryBlder.append(" WHERE id=");
		qryBlder.append(id);
		String rowQry = qryBlder.toString();
		
		logger.debug("getRowInfo() query: " + rowQry);		

		ParameterizedRowMapper<List<GridRowInfo>> mapper = new ParameterizedRowMapper<List<GridRowInfo>>(){

			@Override
            public List<GridRowInfo> mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				ArrayList<GridRowInfo> rowData = new ArrayList<GridRowInfo>();
				
				ResultSetMetaData metaData = rs.getMetaData();

				String columnName;
				String columnValue;
				for (int i = 1; i <= metaData.getColumnCount(); i++)
				{
					columnName = metaData.getColumnName(i);
					columnValue = rs.getString(i);
					rowData.add(new GridRowInfo(columnName, columnValue));
				}

	            return rowData;
            }
		};
		
		return m_SimpleJdbcTemplate.queryForObject(rowQry, mapper);
	}
	
	
	/**
	 * Returns the number of the first row in the specified View matching the 
	 * criteria defined by the specified filter.
	 * @param view the list View containing the row to be identified.
	 * @param selectRowFilter the filter providing the match for the row, with
	 * 		placeholder characters ('?') to be substituted by the supplied filter parameters.
	 * @param selectRowFilterParams parameters for substituting into the supplied filter.
	 * @return the index of the matching row, where a value of zero indicates the
	 * 		first row in the View. Returns -1 if there is no row matching the
	 * 		specified filter.
	 */
	public int getRowNumber(ListView view, 
			String selectRowFilterAttribute, String selectRowFilterValue, 
			List<SortInformation> orderBy)
	{
		// Get the first record matching the specified filter by creating a 
		// copy of the view and setting its filter to the selectRowFilter.
		ListView copyView = new ListView();
		copyView.setDatabaseView(view.getDatabaseView());
		copyView.setFilterAttribute(selectRowFilterAttribute);
		copyView.setFilterValue(selectRowFilterValue);
		List<EventRecord> matchingRecs = getRecords(copyView, 0, 1, orderBy);
		if (matchingRecs.size() == 0)
		{
			return -1;
		}
		
		EventRecord record = matchingRecs.get(0);
		
		
		// Get the row index of the selected row in the View via SQL in the form:
		// SELECT count(*) FROM evidence_second WHERE (time > '2009-04-16 15:33:10') 
		// OR (time = '2009-04-16 15:33:10' AND id >= 888024)
		StringBuilder qryBlder = new StringBuilder("SELECT count(*) FROM ");
		qryBlder.append(view.getDatabaseView());
		qryBlder.append(" WHERE ");
		
		SortInformation sortInfo;
		ArrayList<Object> qryParams = new ArrayList<Object>();
		for (int i = 0; i < orderBy.size(); i++)
		{
			if (i > 0)
			{
				qryBlder.append(" OR ");
			}
			
			qryBlder.append('(');
			
			for (int j = 0; j < i; j++)
			{
				sortInfo = orderBy.get(j);	
				qryBlder.append(sortInfo.getColumnName());
				qryBlder.append(" = ?");
				qryParams.add(record.get(sortInfo.getColumnName()));
			}
			
			if (i > 0)
			{
				qryBlder.append(" AND ");
			}
			
			sortInfo = orderBy.get(i);	
			qryBlder.append(sortInfo.getColumnName());
			if (sortInfo.getSortDirection() == SortInformation.SortDirection.DESC)
			{
				qryBlder.append(" > ?");
			}
			else
			{
				qryBlder.append(" < ?");
			}
			
			qryBlder.append(')');
			
			qryParams.add(record.get(sortInfo.getColumnName()));
			
		}
		
		String rowNumberQry = qryBlder.toString();
		logger.debug("getRowNumber() query: " + rowNumberQry);
		logger.debug("getRowNumber() qryParams: " + qryParams);
		
		return m_SimpleJdbcTemplate.queryForInt(rowNumberQry, qryParams.toArray());
	}
	
	
	/**
	 * Builds an ORDER BY clause for the supplied list of SortInformation objects.
	 * @param orderBy the list of SortInformation objects.
	 * @return ORDER BY clause.
	 */
	private String getOrderByClause(List<SortInformation> orderBy)
	{
		StringBuilder clause = new StringBuilder();
		
		if (orderBy != null)
		{
			for (SortInformation sortInfo : orderBy)
			{
				if (sortInfo.getSortDirection() != SortInformation.SortDirection.NONE)
				{
					clause.append(' ');
					clause.append(sortInfo.getColumnName());
					clause.append(' ');
					clause.append(sortInfo.getSortDirection());
					clause.append(',');
				}
			}
			if (clause.length() > 0)
			{
				clause.deleteCharAt(clause.length()-1);
				clause.insert(0, "ORDER BY");
			}
		}

		return clause.toString();
	}

}
