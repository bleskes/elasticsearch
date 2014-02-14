package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.CausalityEpisode;
import com.prelert.data.CausalityEpisodeLayoutData;
import com.prelert.data.gxt.GridRowInfo;

import com.prelert.data.gxt.ProbableCauseModel;


/**
 * Defines the methods for the interface to the Causality Query service.
 * @author Pete Harverson
 */
public interface CausalityQueryService extends RemoteService
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
	public List<CausalityEpisodeLayoutData> getEpisodeLayoutData(int evidenceId,
			int chartWidth, int chartHeight, int symbolSize, int minXSpacing, int minYSpacing);
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 */
	public List<ProbableCauseModel> getProbableCauses(int evidenceId);
	
	
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
	public List<GridRowInfo> getEvidenceInfo(int episodeId, int evidenceId);
}
