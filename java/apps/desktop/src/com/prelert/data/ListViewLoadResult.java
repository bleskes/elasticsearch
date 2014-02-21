package com.prelert.data;

import java.io.Serializable;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;

/**
 * An extension of BasePagingLoadResult which allows for the setting of a row
 * in the result set to be marked as selected.
 * @author Pete Harverson
 */
public class ListViewLoadResult<BaseModel> extends BasePagingLoadResult<BaseModel> implements Serializable
{
	private	int	m_SelectedRowIndex = -1;
	
	/**
	 * Creates a new ListViewLoadResult.
	 * 
	 * @param data list of the model data contained within the load result.
	 */
	public ListViewLoadResult(List<BaseModel> data)
	{
		super(data);
	}
	
	
	/**
	 * Creates a new ListViewLoadResult.
	 * 
	 * @param data the data.
	 * @param offset the offset of the result into the complete list.
	 * @param totalLength the total length of data in the List.
	 * @param selectedRowIndex the index of the selected row. A value of -1 indicates
	 * that no row is selected.
	 */
	public ListViewLoadResult(List<BaseModel> data, int offset, int totalLength, int selectedRowIndex)
	{
		super(data, offset, totalLength);
		m_SelectedRowIndex = selectedRowIndex;
	}
	
	
	ListViewLoadResult()
	{
		this(null);
	}

	
	/**
	 * Returns the index of the selected row.
	 * @return index of the selected row, or -1 if no row is selected.
	 */
	public int getSelectedRowIndex()
    {
    	return m_SelectedRowIndex;
    }

	
	/**
	 * Sets the index of the selected row in the List View paging load result.
	 * @param selectedRowIndex the index of the selected row, or -1 if no row
	 * is selected.
	 */
	public void setSelectedRowIndex(int selectedRowIndex)
    {
    	m_SelectedRowIndex = selectedRowIndex;
    }
}
