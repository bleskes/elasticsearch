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

package com.prelert.dao;

import java.util.List;

import com.prelert.data.CausalityEpisode;
import com.prelert.data.EventRecord;
import com.prelert.data.ProbableCause;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Causality views from information stored in 
 * the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface CausalityViewDAO
{
	
	/**
	 * Returns the list of probable cause episodes for the item of evidence with
	 * the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 * @return list of probable cause episodes.
	 */
	public List<CausalityEpisode> getEpisodes(int evidenceId);
	
	
	/**
	 * Returns the evidence making up the causality episode with the specified
	 * episode id.
	 * @param episodeId id of the episode for which to return the evidence. A value
	 * of 0 indicates that there is no episode associated with this item of evidence.
	 * @param evidenceId the id of the item of evidence for which episode
	 * information is being requested.
	 * @return the evidence for the episode. If there is no causality episode, then
	 * this call will return a subset of fields of the original evidence item.
	 */
	public List<EventRecord> getEpisodeEvidence(int episodeId, int evidenceId);
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 */
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs);
	
	
	/**
	 * Returns the details on the item of evidence with the given id from the
	 * specified probable cause episode.
	 * @param episodeId id of the episode for which to return the evidence..
	 * @param evidenceId the id of the item of evidence within the specified episode
	 * 		for which episode information is being requested.
	 * @return List of GridRowInfo objects for the item of evidence from the 
	 * 		specified episode.
	 */
	public List<GridRowInfo> getEvidenceInfo(int episodeId, int evidenceId);
}
