package demo.app.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.CausalityEpisode;
import demo.app.data.CausalityEpisodeLayoutData;
import demo.app.data.gxt.GridRowInfo;
import demo.app.data.gxt.ProbableCauseModel;
import demo.app.data.gxt.ProbableCauseModelCollection;

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
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getProbableCauses(int evidenceId, int timeSpanSecs, 
			AsyncCallback<List<ProbableCauseModel>> callback);
	

	/**
	 * Returns a list of probable causes which have been aggregated across shared data
	 * source type, time, and description values. For example:<br>
	 *  - system_udp, Mon Mar 15 2010, 'feature in packets_sent metric'<br>
	 *  which would contain a list probable cause objects for a range of servers.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the aggregated probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getAggregatedProbableCauses(int evidenceId, int timeSpanSecs, 
			AsyncCallback<List<ProbableCauseModelCollection>> callback);
	
	
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
