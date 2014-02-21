package demo.app.client;

import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.menu.Item;
import com.google.gwt.user.client.Element;

/**
 * A MenuItem that displays a DatePicker.
 */
public class DateTimeMenuItem extends Item {

  protected DateTimePicker picker;

  /**
   * Creates a new menu item.
   */
  public DateTimeMenuItem() {
    hideOnClick = true;
    picker = new DateTimePicker();
    picker.addListener(Events.Select, new Listener<ComponentEvent>() {
      public void handleEvent(ComponentEvent ce) {
        parentMenu.fireEvent(Events.Select, ce);
        parentMenu.hide();
      }
    });
  }

  @Override
  protected void onRender(Element target, int index) {
    super.onRender(target, index);
    picker.render(target, index);
    setElement(picker.getElement());
  }

  @Override
  protected void handleClick(ComponentEvent be) {
    picker.onComponentEvent((ComponentEvent) be);
  }

}
