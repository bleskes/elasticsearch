package demo.app.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;

import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;

public interface StatesQueryService extends RemoteService
{	
	public PagingLoadResult<EvidenceModel> getEvidenceData(PagingLoadConfig config);
	
	public PagingLoadResult<EvidenceModel> getProbableCauseData(PagingLoadConfig config);
	
	public List<GridRowInfo> getRowInfo(int rowId);
	
	public List<EvidenceModel> getWeeklyAggregate();
	
	public List<String> getTableColumns(String tableName);

}
