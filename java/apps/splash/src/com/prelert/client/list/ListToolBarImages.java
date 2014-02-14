package com.prelert.client.list;

import com.extjs.gxt.ui.client.GXT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class ListToolBarImages
{
	private AbstractImagePrototype m_FirstImage = GXT.IMAGES.paging_toolbar_first();
	private AbstractImagePrototype m_PrevImage = GXT.IMAGES.paging_toolbar_prev();
	private AbstractImagePrototype m_NextImage = GXT.IMAGES.paging_toolbar_next();
	private AbstractImagePrototype m_LastImage = GXT.IMAGES.paging_toolbar_last();
	private AbstractImagePrototype m_RefreshImage = GXT.IMAGES.paging_toolbar_refresh();

	private AbstractImagePrototype m_FirstDisabledImage = GXT.IMAGES.paging_toolbar_first_disabled();
	private AbstractImagePrototype m_PrevDisabledImage = GXT.IMAGES.paging_toolbar_prev_disabled();
	private AbstractImagePrototype m_NextDisabledImage = GXT.IMAGES.paging_toolbar_next_disabled();
	private AbstractImagePrototype m_LastDisabledImage = GXT.IMAGES.paging_toolbar_last_disabled();


	public void setFirst(AbstractImagePrototype first)
	{
		m_FirstImage = first;
	}


	public AbstractImagePrototype getFirst()
	{
		return m_FirstImage;
	}


	public void setPrev(AbstractImagePrototype prev)
	{
		m_PrevImage = prev;
	}


	public AbstractImagePrototype getPrev()
	{
		return m_PrevImage;
	}


	public void setNext(AbstractImagePrototype next)
	{
		m_NextImage = next;
	}


	public AbstractImagePrototype getNext()
	{
		return m_NextImage;
	}


	public void setLast(AbstractImagePrototype last)
	{
		m_LastImage = last;
	}


	public AbstractImagePrototype getLast()
	{
		return m_LastImage;
	}


	public void setRefresh(AbstractImagePrototype refresh)
	{
		m_RefreshImage = refresh;
	}


	public AbstractImagePrototype getRefresh()
	{
		return m_RefreshImage;
	}


	public void setFirstDisabled(AbstractImagePrototype firstDisabled)
	{
		m_FirstDisabledImage = firstDisabled;
	}


	public AbstractImagePrototype getFirstDisabled()
	{
		return m_FirstDisabledImage;
	}


	public void setPrevDisabled(AbstractImagePrototype prevDisabled)
	{
		m_PrevDisabledImage = prevDisabled;
	}


	public AbstractImagePrototype getPrevDisabled()
	{
		return m_PrevDisabledImage;
	}


	public void setNextDisabled(AbstractImagePrototype nextDisabled)
	{
		m_NextDisabledImage = nextDisabled;
	}


	public AbstractImagePrototype getNextDisabled()
	{
		return m_NextDisabledImage;
	}


	public void setLastDisabled(AbstractImagePrototype lastDisabled)
	{
		m_LastDisabledImage = lastDisabled;
	}


	public AbstractImagePrototype getLastDisabled()
	{
		return m_LastDisabledImage;
	}
}
