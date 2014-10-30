/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package demo.app.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import demo.app.data.CausalityEpisode;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.data.ProbableCause;



/**
 * Implementation for a MySQL database of the CausalityViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Causality views.
 * @author Pete Harverson
 */
public class CausalityViewDAO extends SpringJdbcTemplateDAO
{
	static Logger logger = Logger.getLogger(CausalityViewDAO.class);
	
	
	/**
	 * Returns the list of probable cause episodes for the item of evidence with
	 * the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 * @return list of probable cause episodes.
	 */
	public List<CausalityEpisode> getEpisodes(int evidenceId)
	{
		String episodesQry = "CALL episodes(?)";
		logger.debug("getEpisodes() query: " + episodesQry);
		
		List<CausalityEpisode> episodes = 
			m_SimpleJdbcTemplate.query(episodesQry, new EpisodeRowMapper(), evidenceId);

		List<EvidenceModel> evidenceList;
		for (CausalityEpisode episode : episodes)
		{
			if (episode.getId() > 0)
			{
				evidenceList = getEpisodeEvidence(episode.getId(), 0);
			}
			else
			{
				// No episode - just obtain details on this item of evidence.
				evidenceList = getEpisodeEvidence(episode.getId(), evidenceId);
			}
			episode.setEvidenceList(evidenceList);
		}
		
		return episodes;
	}
	
	
	/**
	 * Returns details of the evidence item(s) within the specified causality episode.
	 * 
	 * @param episodeId id of the episode for which to return the evidence. A value
	 * of 0 indicates that there is no episode associated with this item of evidence.
	 * @param evidenceId the id of the item of evidence within the specified episode
	 * for which episode information is being requested. If a value of 0 is supplied, 
	 * all evidence within the specified episode will be returned.
	 * @return the evidence for the episode. If there is no causality episode, then
	 * this call will return the record of the evidence with the specified id.
	 */
	public List<EvidenceModel> getEpisodeEvidence(int episodeId, int evidenceId)
	{
		List<EvidenceModel> evidenceList;
		
		String evidenceQry = "CALL episode_evidence(?, ?)";
		logger.debug("getEpisodeEvidence() query: " + evidenceQry);
		
		if (episodeId > 0)
		{
			evidenceList = m_SimpleJdbcTemplate.query(evidenceQry, 
					new EventRecordRowMapper(), episodeId, evidenceId);
		}
		else
		{
			evidenceList = m_SimpleJdbcTemplate.query(evidenceQry, 
					new EventRecordRowMapper(), null, evidenceId);
		}
		return evidenceList;
	}
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the
	 * specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @return list of probable causes. If the item of evidence has no probable
	 * 		causes, the returned list should contain a ProbableCause object
	 * 		referring to the item itself.
	 */
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs)
	{
		String query = "CALL probable_cause_list(?, ?)";
		
		String debugQuery = "CALL probable_cause_list({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId, timeSpanSecs);
		logger.debug("getProbableCauses() query: " + debugQuery);

		return m_SimpleJdbcTemplate.query(query, 
					new ProbableCauseRowMapper(), evidenceId, timeSpanSecs);

	}
	
	
	/**
	 * Returns the details on the item of evidence with the given id from the
	 * specified probable cause episode.
	 * @param episodeId id of the episode for which to return the evidence. A value
	 * 		of 0 indicates that there is no episode associated with this item of evidence.
	 * @param evidenceId the id of the item of evidence within the specified episode
	 * 		for which episode information is being requested. This must be greater than 0.
	 * @return List of GridRowInfo objects for the item of evidence from the 
	 * 		specified episode.
	 */
	public List<GridRowInfo> getEvidenceInfo(int episodeId, int evidenceId)
	{
		if (evidenceId == 0)
		{
			throw new IllegalArgumentException("Evidence id must be greater than 0");
		}
		
		String rowQry = "CALL episode_evidence(?, ?)";
		logger.debug("getEvidenceInfo() query: " + rowQry);	

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
					columnName = metaData.getColumnLabel(i);
					
					if (metaData.getColumnType(i) == Types.BIT || 
							columnName.equals(CausalityEpisode.CONTIGUOUS_EVIDENCE_COLUMN))
					{
						// Looking for 'contiguous' field which is a TINYINT in MySQL
						// but comes across as Types.BIT.
						columnValue = (new Boolean(rs.getBoolean(i))).toString();
					}
					else
					{
						columnValue = rs.getString(i);
					}
					rowData.add(new GridRowInfo(columnName, columnValue));
				}

	            return rowData;
            }
		};
		
		return m_SimpleJdbcTemplate.queryForObject(rowQry, mapper, episodeId, evidenceId);
	}
	
	
	/**
	 * Converts the value of the category column returned by the database
	 * to the appropriate DataSourceCategory enum.
	 * @param dbCategory	the value returned by the database e.g. 'time series'.
	 * @return DataSourceCategory enum, or <code>null</code> if the value could
	 * 			was not successfully converted to a DataSourceCategory.
	 * @throws IllegalArgumentException if there is no DataSourceCategory matching
	 * 			the database value.
     * @throws NullPointerException if <tt>dbCategory</tt> is null.
	 */
	protected DataSourceCategory toDataSourceCategory(String dbCategory)
	{
		String categoryStr = dbCategory.replace(' ', '_').toUpperCase();
		return Enum.valueOf(DataSourceCategory.class, categoryStr);
	}
	
	
    /**
	 * ParameterizedRowMapper class for causality query result sets to
	 * CausalityEpisode objects.
	 */
    class EpisodeRowMapper implements ParameterizedRowMapper<CausalityEpisode>
	{
		@Override
        public CausalityEpisode mapRow(ResultSet rs, int rowNum) throws SQLException
        {		
			int id = rs.getInt("episode_id");
			int probability = rs.getInt("probability");
			
			CausalityEpisode episode = new CausalityEpisode();
			episode.setId(id);
			episode.setProbability(probability);
			
            return episode;
        }
	}
    
    
    /**
	 * ParameterizedRowMapper class for mapping probable cause query result 
	 * sets to ProbableCause objects.
	 */
    class ProbableCauseRowMapper implements ParameterizedRowMapper<ProbableCause>
	{
		@Override
        public ProbableCause mapRow(ResultSet rs, int rowNum) throws SQLException
        {	
			ProbableCause cause = new ProbableCause();
			
			String categoryStr = rs.getString("category");
			String typeName = rs.getString("type");
			
			DataSourceCategory category = null;
			try
			{	
				category = toDataSourceCategory(categoryStr);
				DataSourceType dataSourceType = new DataSourceType(typeName, category);
				
				cause.setDataSourceType(dataSourceType);
			}
			catch (Exception e)
			{
				logger.error("Error extracting data source type " +
						"from result set for type " + typeName, e);
			}
			
			java.sql.Timestamp time = rs.getTimestamp("time");
			String description = rs.getString("description");
			String source = rs.getString("source");
			int significance = rs.getInt("significance");
			double magnitude = rs.getDouble("magnitude");
			
			cause.setTime(time);
			cause.setDescription(description);
			cause.setSource(source);
			cause.setSignificance(significance);
			cause.setMagnitude(magnitude);
			
			if (category == DataSourceCategory.TIME_SERIES)
			{
				// Set metric and stats for time series probable causes.
				int typeId = rs.getInt("time_series_type_id");
				double scalingFactor = rs.getDouble("scaling_factor");
				int peakValue = rs.getInt("peak_value");
				String metric = rs.getString("metric");
				
				cause.setTimeSeriesTypeId(typeId);
				cause.setMetric(metric);
				cause.setScalingFactor(scalingFactor);
				cause.setPeakValue(peakValue);
			}
			
			String attributeName = rs.getString("attribute_name");
			if (attributeName != null)
			{
				String attributeValue = rs.getString("attribute_value");
				
				cause.setAttributeName(attributeName);
				cause.setAttributeValue(attributeValue);	
			}
			
			// attribute_label will be non-null for time series features 
			// even if there are no attributes - it has the time series feature id.
			String attributeLabel = rs.getString("attribute_label");
			cause.setAttributeLabel(attributeLabel);
			
            return cause;
        }
	}
    

}
