package com.prelert.dao;

import java.util.ArrayList;
import java.util.List;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.server.ViewDirectory;

/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information on list views that have been configured to display
 * information stored in the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface ListViewDAO
{		
	public List<String> getAllColumns(ListView view);
	
	public int getTotalRowCount(ListView view);
	
	public List<EventRecord> getRecords(ListView view, int offset, int limit, List<SortInformation> orderBy);
	
	public List<GridRowInfo> getRowInfo(ListView view, int id);
	
	public int getRowNumber(ListView view, 
			String selectRowFilterAttribute, String selectRowFilterValue, 
			List<SortInformation> orderBy);
	
	
}
