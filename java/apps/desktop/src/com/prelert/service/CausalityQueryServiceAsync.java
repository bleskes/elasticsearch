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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.CausalityEpisode;
import com.prelert.data.CausalityEpisodeLayoutData;
import com.prelert.data.gxt.GridRowInfo;

import com.prelert.data.gxt.ProbableCauseModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Causality query service.
 * @author Pete Harverson
 */
public interface CausalityQueryServiceAsync
{
	/**
	 * Requests the list of probable cause episodes for the item of evidence with
	 * the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 */
	public void getEpisodes(int evidenceId, AsyncCallback<List<CausalityEpisode>> callback);
	
	
	/**
	 * Returns the list of probable cause episodes for the item of evidence with
	 * the specified id, encapsulated into layout data for display in a graphical
	 * chart with the specified parameters.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 * @param chartWidth width of plotting area, in pixels, for Probable Cause chart.
	 * @param chartHeight height of plotting area, in pixels, for Probable Cause chart.
	 * @param symbolSize width and height of symbol used to denote an item of evidence.
	 * @param minXSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @param minYSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @return list of probable cause episodes encapsulated with layout data for
	 *  placement in a chart with the specified parameters.
	 */
	public void getEpisodeLayoutData(int evidenceId, int chartWidth, int chartHeight, 
				int symbolSize, int minXSpacing, int minYSpacing,
				AsyncCallback<List<CausalityEpisodeLayoutData>> callback);
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getProbableCauses(
			int evidenceId, AsyncCallback<List<ProbableCauseModel>> callback);
	
	
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
	public void getEvidenceInfo(int episodeId, int evidenceId,
			AsyncCallback<List<GridRowInfo>> callback);
}
