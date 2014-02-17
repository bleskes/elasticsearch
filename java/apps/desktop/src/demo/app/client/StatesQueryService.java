package demo.app.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;

import demo.app.data.EventRecord;
import demo.app.data.GridRowInfo;

public interface StatesQueryService extends RemoteService
{	
	public PagingLoadResult<EventRecord> getEvidenceData(PagingLoadConfig config);
	
	public PagingLoadResult<EventRecord> getProbableCauseData(PagingLoadConfig config);
	
	public List<GridRowInfo> getRowInfo(int rowId);
	
	public List<EventRecord> getWeeklyAggregate();
	
	public List<String> getTableColumns(String tableName);

}
