package demo.app.client;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.messages.MyMessages;
import com.extjs.gxt.ui.client.widget.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.ChangeListener;

/**
 * A concise version of the standard PagingToolBar, displaying only the
 * First, Previous, Next, Last and Refresh buttons, plus the display message
 * (defaulting to "Displaying {0} - {1} of {2}") sandwiched in between the 
 * Previous and Next buttons.
 * 
 * @author Pete Harverson
 */
public class ConcisePagingToolBar extends PagingToolBar
{
	private LoadEvent<PagingLoadConfig, PagingLoadResult> m_RenderEvent;
	
	public ConcisePagingToolBar(int pageSize)
	{
		super(pageSize);
	}
	
	
	protected void onLoad(LoadEvent<PagingLoadConfig, PagingLoadResult> event)
	{
		m_RenderEvent = event;
		super.onLoad(event);
	}


	@Override
	protected void onRender(Element target, int index)
	{
		MyMessages msg = GXT.MESSAGES;

		msgs.setRefreshText(msgs.getRefreshText() == null ? msg
		        .pagingToolBar_refreshText() : msgs.getRefreshText());
		msgs.setNextText(msgs.getNextText() == null ? msg
		        .pagingToolBar_nextText() : msgs.getNextText());
		msgs.setPrevText(msgs.getPrevText() == null ? msg
		        .pagingToolBar_prevText() : msgs.getPrevText());
		msgs.setFirstText(msgs.getFirstText() == null ? msg
		        .pagingToolBar_firstText() : msgs.getFirstText());
		msgs.setLastText(msgs.getLastText() == null ? msg
		        .pagingToolBar_lastText() : msgs.getLastText());
		msgs.setBeforePageText(msgs.getBeforePageText() == null ? msg
		        .pagingToolBar_beforePageText() : msgs.getBeforePageText());
		msgs.setEmptyMsg(msgs.getEmptyMsg() == null ? msg
		        .pagingToolBar_emptyMsg() : msgs.getEmptyMsg());

		toolBar = new ToolBar();

		first = new TextToolItem();
		first.setIconStyle("x-tbar-page-first");
		if (showToolTips)
			first.setToolTip(msgs.getFirstText());
		first.addSelectionListener(new SelectionListener<ComponentEvent>()
		{
			public void componentSelected(ComponentEvent ce)
			{
				first();
			}
		});

		prev = new TextToolItem();
		prev.setIconStyle("x-tbar-page-prev");
		if (showToolTips)
			prev.setToolTip(msgs.getPrevText());
		prev.addSelectionListener(new SelectionListener<ComponentEvent>()
		{
			public void componentSelected(ComponentEvent ce)
			{
				previous();
			}
		});

		next = new TextToolItem();
		next.setIconStyle("x-tbar-page-next");
		if (showToolTips)
			next.setToolTip(msgs.getNextText());
		next.addSelectionListener(new SelectionListener<ComponentEvent>()
		{
			public void componentSelected(ComponentEvent ce)
			{
				next();
			}
		});

		last = new TextToolItem();
		last.setIconStyle("x-tbar-page-last");
		if (showToolTips)
			last.setToolTip(msgs.getLastText());
		last.addSelectionListener(new SelectionListener<ComponentEvent>()
		{
			public void componentSelected(ComponentEvent ce)
			{
				last();
			}
		});

		refresh = new TextToolItem();
		refresh.setIconStyle("x-tbar-loading");
		if (showToolTips)
			refresh.setToolTip(msgs.getRefreshText());
		refresh.addSelectionListener(new SelectionListener<ComponentEvent>()
		{
			public void componentSelected(ComponentEvent ce)
			{
				refresh();
			}
		});

		Label beforePage = new Label(msgs.getBeforePageText());
		beforePage.setStyleName("my-paging-text");
		afterText = new Label();
		afterText.setStyleName("my-paging-text");
		pageText = new TextBox();
		if (!GXT.isGecko && !GXT.isSafari)
		{
			pageText.addKeyboardListener(new KeyboardListenerAdapter()
			{
				public void onKeyDown(Widget sender, char keyCode, int modifiers)
				{
					if (keyCode == KeyboardListener.KEY_ENTER)
					{
						onPageChange();
					}
				}
			});
		}
		pageText.setWidth("30px");

		pageText.addChangeListener(new ChangeListener()
		{
			public void onChange(Widget sender)
			{
				onPageChange();
			}
		});

		displayText = new Label();
		displayText.setStyleName("my-paging-display");

		toolBar.add(first);
		toolBar.add(prev);
		toolBar.add(new SeparatorToolItem());
		toolBar.add(new AdapterToolItem(displayText));
		//toolBar.add(new AdapterToolItem(beforePage));
		//toolBar.add(new AdapterToolItem(pageText));
		//toolBar.add(new AdapterToolItem(afterText));
		toolBar.add(new SeparatorToolItem());
		toolBar.add(next);
		toolBar.add(last);
		toolBar.add(new SeparatorToolItem());
		toolBar.add(refresh);

		// PH - 03-09: No custom ToolItems
		//for (ToolItem item : items)
		//{
		//	toolBar.add(item);
		//}

		//toolBar.add(new FillToolItem());
		//toolBar.add(new AdapterToolItem(displayText));

		toolBar.render(target, index);
		setElement(toolBar.getElement());

		if (m_RenderEvent != null)
		{
			onLoad(m_RenderEvent);
			m_RenderEvent = null;
		}
	}

}
