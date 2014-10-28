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

package demo.app.data;

import java.io.Serializable;

/**
 * Class encapsulating the data behind an edge, or connector, in an episode model.
 * It holds the ids of the pair of evidence descriptions, and the probability
 * of the link between them.
 * @author Pete Harverson
 */
public class EpisodeGraphEdge implements Serializable
{
	private int m_Id;
	private int m_FirstId;
	private int m_SecondId;
	private float m_Probability;
	private String m_Attributes;
	private boolean m_Directional;

	
	/**
	 * Creates a new, empty edge in an episode graph.
	 */
	public EpisodeGraphEdge()
	{
		
	}
	
	
	/**
	 * Creates a new edge in an episode graph linking the pair of
	 * evidence descriptions with the specified ids.
	 * @param id the identifier of the edge.
	 * @param firstId evidence description id of the first endpoint of the edge.
	 * @param secondId evidence description id of the second endpoint of the edge.
	 * @param probability the edge probability, between 0 and 100.
	 * @param attributes comma-separated list of attributes.
	 */
	public EpisodeGraphEdge(int id, int firstId, int secondId, 
			float probability, String attributes)
	{
		m_Id = id;
		m_FirstId = firstId;
		m_SecondId = secondId;
		m_Probability = probability;
		m_Attributes = attributes;
	}
	

	/**
	 * Returns the identifier of the graph edge.
     * @return the id of the graph edge.
     */
    public int getId()
    {
    	return m_Id;
    }


	/**
	 * Sets the identifier of the graph edge.
     * @param id the id of the graph edge.
     */
    public void setId(int id)
    {
    	m_Id = id;
    }


	/**
	 * Returns the evidence description id of the first endpoint of the edge.
	 * @return the evidence description id of the first endpoint.
	 */
	public int getFirstId()
	{
		return m_FirstId;
	}


	/**
	 * Sets the evidence description id of the first endpoint of the edge.
	 * @param firstId the evidence description id of the first endpoint.
	 */
	public void setFirstId(int firstId)
	{
		m_FirstId = firstId;
	}


	/**
	 * Returns the evidence description id of the second endpoint of the edge.
	 * @return the evidence description id of the second endpoint.
	 */
	public int getSecondId()
	{
		return m_SecondId;
	}


	/**
	 * Sets the evidence description id of the second endpoint of the edge.
	 * @param secondId the evidence description id of the second endpoint.
	 */
	public void setSecondId(int secondId)
	{
		m_SecondId = secondId;
	}


	/**
	 * Returns the probability of the edge in the episode model.
	 * @return the edge probability, between 0 and 100.
	 */
	public float getProbability()
	{
		return m_Probability;
	}


	/**
	 * Sets the probability of the edge as held in the Prelert model.
	 * @param probability the edge probability, between 0 and 100.
	 */
	public void setProbability(float probability)
	{
		m_Probability = probability;
	}


	/**
	 * Returns a comma-separated list of attributes that the edge has been
	 * correlated on by the Prelert engine.
     * @return a comma-separated list of attributes.
     */
    public String getAttributes()
    {
    	return m_Attributes;
    }


	/**
	 * Sets the attributes that the edge has been correlated on by the Prelert engine
	 * as a comma-separated list of attributes.
     * @param attributes comma-separated list of attributes.
     */
    public void setAttributes(String attributes)
    {
    	m_Attributes = attributes;
    }
    

	/**
     * @return the Directional
     */
    public boolean isDirectional()
    {
    	return m_Directional;
    }


	/**
     * @param directional the m_Directional to set
     */
    public void setDirectional(boolean directional)
    {
    	m_Directional = directional;
    }


	/**
	 * Returns a summary of this episode graph edge.
	 * 
	 * @return String representation of the graph edge, showing all fields and values.
	 */
    public String toString()
    {
	    return "id=" + m_Id;
    }
	
}
