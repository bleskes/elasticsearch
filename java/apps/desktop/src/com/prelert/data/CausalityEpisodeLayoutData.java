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

package com.prelert.data;

import java.io.Serializable;
import java.util.HashMap;


/**
 * Class encapsulating a causality episode and its layout data for display in
 * a Causality Episode chart. Each item of evidence in the episode will have its
 * x- and y-coordinates set according to the layout used in the window e.g. minimum
 * gap between evidence and episodes.
 * @author Pete Harverson
 */
public class CausalityEpisodeLayoutData implements Serializable
{
	private CausalityEpisode			m_Episode;
	private HashMap<Integer, Double>	m_XCoords;
	private double						m_YCoord;
	private double						m_MaxX;
	private double						m_MinX;
	
	
	/**
	 * Creates a new, empty causality episode layout data object.
	 */
	public CausalityEpisodeLayoutData()
	{
		
	}
	
	
	/**
	 * Sets the causality episode.
	 * @param episode the causality episode - see {@link #CausalityEpisode}.
	 */
	public void setEpisode(CausalityEpisode episode)
	{
		m_Episode = episode;
	}
	
	
	/**
	 * Returns the causality episode.
	 * @return the causality episode - see {@link #CausalityEpisode}.
	 */
	public CausalityEpisode getEpisode()
	{
		return m_Episode;
	}
	
	
	/**
	 * Sets the map  of x-coordinates for the items of evidence in the episode,
	 * hashed on their evidence IDs.
	 * @param xCoords HashMap of x-coordinates against the evidence IDs.
	 */
	public void setXCoords(HashMap<Integer, Double> xCoords)
	{
		m_XCoords = xCoords;
		
		m_MinX = Double.MAX_VALUE;
		m_MaxX = 0;
		for (Double x : xCoords.values())
		{
			m_MinX = Math.min(m_MinX, x);
			m_MaxX = Math.max(m_MaxX, x);
		}
	}
	
	
	/**
	 * Returns the x-coordinate of the item of evidence in the episode with the
	 * specified ID.
	 * @param evidenceId ID of the evidence for which to return the x-coordinate.
	 * @return x-coordinate for evidence item in the causality chart.
	 */
	public double getEvidenceX(int evidenceId)
	{
		double x = -1;
		Double xCoord = m_XCoords.get(evidenceId);
		if (xCoord != null)
		{
			x = xCoord.doubleValue();
		}
		
		return x;
	}
	
	
	/**
	 * Sets the y-coordinate to use for the episode in a causality chart. This will be
	 * set in proportion to the probability of the episode. 
	 * @param yCoord the y-coordinate for the episode.
	 * @see CausalityEpisode#getProbability()
	 */
	public void setY(double yCoord)
	{
		m_YCoord = yCoord;
	}
	
	
	/**
	 * Returns the y-coordinate to use for the episode in a causality chart. This will be
	 * set in proportion to the probability of the episode. 
	 * @return the y-coordinate for the episode.
	 * @see CausalityEpisode#getProbability()
	 */
	public double getY()
	{
		return m_YCoord;
	}
	
	
	/**
	 * Returns the minimum x-coordinate for the episode, which will correspond to 
	 * the first (earliest) item of evidence in the episode.
	 * @return the minimum x-coordinate.
	 */
	public double getMinX()
	{
		return m_MinX;
	}
	
	
	/**
	 * Returns the maximum x-coordinate for the episode, which will correspond to 
	 * the last (latest) item of evidence in the episode.
	 * @return the maximum x-coordinate.
	 */
	public double getMaxX()
	{
		return m_MaxX;
	}
}

