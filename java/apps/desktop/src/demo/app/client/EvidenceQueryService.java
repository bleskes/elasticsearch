package demo.app.client;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.*;


/**
 * Defines the methods for the interface to the Evidence View query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryService extends RemoteService
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getFirstPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getLastPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getNextPage(
			EvidencePagingLoadConfig config, String bottomRowId);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getPreviousPage(
			EvidencePagingLoadConfig config, String topRowId);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description, and whose time corresponds to the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getForDescription(
			EvidencePagingLoadConfig config, String description);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getAtTime(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the details on the row of evidence with the given id.
	 * @param id the value of the id column for the row to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int rowId);
	
	
	/**
	 * Returns a list of all of the columns in an Evidence View with the specified
	 * time frame.
	 * @param view time frame for which to return the list of columns.
	 * @return list of all of the columns for an Evidence View with the specified
	 * time frame.
	 */
	public List<String> getAllColumns(TimeFrame timeFrame);
	
	
	/**
	 * Returns the list of values in the evidence table for the specified
	 * column.
	 * @param columnName name of the column for which to return the values.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String columnName);

}
