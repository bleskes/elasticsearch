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
 * Class encapsulating the data behind a node in an episode model.
 * It holds the id of the item in the model, along with its description and severity.
 * @author Pete Harverson
 */
public class EpisodeGraphNode implements Serializable
{
	private int m_Id;
	private String m_Description;
	private int m_Severity;
	
	
	/**
	 * Creates a new, empty node in an episode graph.
	 */
	public EpisodeGraphNode()
	{
		
	}
	
	
	/**
	 * Creates a new node in the episode model for an item with the specified
	 * id, description and severity.
	 * @param id identifier for the node in the graph.
	 * @param description description of the node.
	 * @param severity severity of the item represented by the node.
	 */
	public EpisodeGraphNode(int id, String description, int severity)
	{
		m_Id = id;
		m_Description = description;
		m_Severity = severity;
	}


	/**
	 * Returns the identifier of the graph node.
     * @return the id of the item represented by this node in the episode graph.
     */
    public int getId()
    {
    	return m_Id;
    }


	/**
	 * Sets the identifier of the graph node.
     * @param id the id of the item represented by this node in the episode graph.
     */
    public void setId(int id)
    {
    	m_Id = id;
    }


	/**
	 * Returns the description of this graph node.
     * @return the description of the item represented by this node in the graph.
     */
    public String getDescription()
    {
    	return m_Description;
    }


	/**
	 * Sets the description of this graph node.
     * @param description the description of the item represented by 
     * 		this node in the graph.
     */
    public void setDescription(String description)
    {
    	m_Description = description;
    }


	/**
	 * Returns the severity of the item represented by the graph node.
     * @return the severity of the graph node.
     */
    public int getSeverity()
    {
    	return m_Severity;
    }


	/**
	 * Sets the severity of this graph node.
     * @param severity the severity of the item represented by the graph node.
     */
    public void setSeverity(int severity)
    {
    	m_Severity = severity;
    }


    /**
	 * Returns a summary of this episode graph node.
	 * @return String representation of the graph node.
	 */
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder("id=");
    	strRep.append(m_Id);
    	strRep.append(" (");
    	strRep.append(m_Description);
    	strRep.append(')');
    	
    	
    	return strRep.toString();
    }

}
