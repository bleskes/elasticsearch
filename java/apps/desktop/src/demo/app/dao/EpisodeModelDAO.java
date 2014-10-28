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
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import demo.app.data.EpisodeGraphEdge;
import demo.app.data.EpisodeGraphNode;

public class EpisodeModelDAO
{
	static Logger logger = Logger.getLogger(EpisodeModelDAO.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	private String 	m_NodesQuery;
	private String	m_EdgesQuery;
	
	
	/**
	 * Creates a new data access object to the episode model.
	 */
	public EpisodeModelDAO()
	{
		// Define default queries.
		m_NodesQuery = "select id, description, severity_id from evidence_description";
		
		StringBuilder edgesQryBuff = new StringBuilder();
		edgesQryBuff.append("select id, first, second, attribute, ");
		edgesQryBuff.append("MAX(probability) as probability from behaviour_episodes ");
		edgesQryBuff.append("group by first, second order by first");
		
		m_EdgesQuery = edgesQryBuff.toString();
	}
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	
	/**
	 * Returns the list of nodes in the episode model, which correspond to the 
	 * evidence descriptions, their ids and severities.
	 * @return list of nodes.
	 */
	public List<EpisodeGraphNode> getNodes()
	{
		logger.debug("getNodes() query: " + m_NodesQuery);
		
		ParameterizedRowMapper<EpisodeGraphNode> mapper = new ParameterizedRowMapper<EpisodeGraphNode>(){

            public EpisodeGraphNode mapRow(ResultSet rs, int rowNum) throws SQLException
            {
            	int id = rs.getInt("id");
            	String description = rs.getString("description");
            	int severity = rs.getInt("severity_id");
            	
            	return new EpisodeGraphNode(id, description, severity);
            }
		};
		
		return m_SimpleJdbcTemplate.query(m_NodesQuery, mapper);
	}
	
	
	/**
	 * Returns the list of all edges (returning both A->B and B->A if a link is
	 * found in both directions) in the episode model. Only the edge corresponding 
	 * to the maximum probability is returned.
	 * @return list of edges.
	 */
	public List<EpisodeGraphEdge> getEdges()
	{
		StringBuilder edgesQryBuff = new StringBuilder();
		edgesQryBuff.append("select id, first, second, attribute, ");
		edgesQryBuff.append("MAX(probability) as probability from behaviour_episodes ");
		edgesQryBuff.append("group by first, second order by first");
		
		String edgesQry = edgesQryBuff.toString();
		logger.debug("getEdges() query: " + edgesQry);
		
		ParameterizedRowMapper<EpisodeGraphEdge> mapper = new ParameterizedRowMapper<EpisodeGraphEdge>(){

            public EpisodeGraphEdge mapRow(ResultSet rs, int rowNum) throws SQLException
            {
            	int id = rs.getInt("id");
            	int first = rs.getInt("first");
            	int second = rs.getInt("second");
            	float prob = rs.getFloat("probability");
            	String attributes = rs.getString("attribute");
            	
            	return new EpisodeGraphEdge(id, first, second, prob, attributes);
            }
		};
		
		return m_SimpleJdbcTemplate.query(edgesQry, mapper);
	}


	/**
	 * Returns the query being used to obtain the list of nodes for the episode 
	 * model from the database.
     * @return the query to obtain the list of nodes.
     */
    public String getNodesQuery()
    {
    	return m_NodesQuery;
    }


	/**
	 * Sets the query to be used to obtain the list of nodes for the episode 
	 * model from the database.
     * @param nodesQuery the query to obtain the list of nodes.
     */
    public void setNodesQuery(String nodesQuery)
    {
    	m_NodesQuery = nodesQuery;
    }


    /**
	 * Returns the query being used to obtain the list of edges for the episode 
	 * model from the database.
     * @return the query to obtain the list of edges.
     */
    public String getEdgesQuery()
    {
    	return m_EdgesQuery;
    }


	/**
	 * Sets the query to be used to obtain the list of edges for the episode 
	 * model from the database.
     * @param edgesQuery the m_EdgesQuery to set
     */
    public void setEdgesQuery(String edgesQuery)
    {
    	m_EdgesQuery = edgesQuery;
    }
}
