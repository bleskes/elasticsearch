package demo.app.client;

import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.SplitButton;
import com.extjs.gxt.ui.client.widget.button.ToggleButton;
import com.extjs.gxt.ui.client.widget.custom.ThemeSelector;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.menu.CheckMenuItem;
import com.extjs.gxt.ui.client.widget.menu.DateMenu;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;

public class ToolBarTestWindow extends Window
{
	public ToolBarTestWindow()
	{
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Exception List");
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);

		setIconStyle("list-sec-win-icon");

		ToolBar toolBar = new ToolBar();

		Button item1 = new Button("Button w/ Menu");

		Menu menu = new Menu();
		CheckMenuItem menuItem = new CheckMenuItem("I Like Cats");
		menuItem.setChecked(true);
		menu.add(menuItem);

		menuItem = new CheckMenuItem("I Like Dogs");
		menu.add(menuItem);
		item1.setMenu(menu);

		menu.add(new SeparatorMenuItem());

		MenuItem radios = new MenuItem("Radio Options");
		menu.add(radios);

		Menu radioMenu = new Menu();
		CheckMenuItem r = new CheckMenuItem("Blue Theme");
		r.setGroup("radios");
		r.setChecked(true);
		radioMenu.add(r);
		r = new CheckMenuItem("Gray Theme");
		r.setGroup("radios");
		radioMenu.add(r);
		radios.setSubMenu(radioMenu);

		MenuItem date = new MenuItem("Choose a Date");
		menu.add(date);

		date.setSubMenu(new DateTimeMenu());

		toolBar.add(item1);

		toolBar.add(new SeparatorToolItem());

		SplitButton splitItem = new SplitButton("Split Button");

		menu = new Menu();
		menu.add(new MenuItem("<b>Bold</b>"));
		menu.add(new MenuItem("<i>Italic</i>"));
		menu.add(new MenuItem("<u>Underline</u>"));
		splitItem.setMenu(menu);

		toolBar.add(splitItem);

		toolBar.add(new SeparatorToolItem());

		ToggleButton toggle = new ToggleButton("Toggle");
		toggle.toggle(true);
		toolBar.add(toggle);

		toolBar.add(new SeparatorToolItem());

		Button scrollerButton = new Button("Scrolling Menu");

		Menu scrollMenu = new Menu();
		scrollMenu.setMaxHeight(200);
		for (int i = 0; i < 40; i++)
		{
			scrollMenu.add(new MenuItem("Item " + i));
		}

		scrollerButton.setMenu(scrollMenu);

		toolBar.add(scrollerButton);

		toolBar.add(new SeparatorToolItem());

		toolBar.add(new FillToolItem());
		ThemeSelector selector = new ThemeSelector();
		toolBar.add(selector);

		setTopComponent(toolBar);

	}

}
