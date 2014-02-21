package demo.app.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;

import demo.app.data.EventRecord;
import demo.app.data.GridRowInfo;

public interface StatesQueryServiceAsync
{	
	public void getEvidenceData(PagingLoadConfig config, AsyncCallback<PagingLoadResult<EventRecord>> callback);
	
	public void getProbableCauseData(PagingLoadConfig config, AsyncCallback<PagingLoadResult<EventRecord>> callback);
	
	public void getRowInfo(int rowId, AsyncCallback<List<GridRowInfo>> callback);
	
	public void getWeeklyAggregate(AsyncCallback<List<EventRecord>> callback);
	
	public void getTableColumns(String tableName, AsyncCallback<List<String>> callback);

}
