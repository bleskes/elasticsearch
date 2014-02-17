
package demo.app.client;

import com.extjs.gxt.ui.client.event.BoxComponentEvent;
import com.google.gwt.user.client.Event;

/**
 * Slider event type.
 * 
 * @see Slider
 */
public class SliderComponentEvent extends BoxComponentEvent {

  private int newValue = -1;
  private int oldValue = -1;
  private SliderComponent slider;

  public SliderComponentEvent(SliderComponent slider) {
    super(slider);
    this.slider = slider;
  }
  
  public SliderComponentEvent(SliderComponent slider, Event event) {
    super(slider, event);
    this.slider = slider;
  }

  /**
   * Returns the new value.
   * 
   * @return the new value
   */
  public int getNewValue() {
    return newValue;
  }

  /**
   * Returns the old value.
   * 
   * @return the old value
   */
  public int getOldValue() {
    return oldValue;
  }

  /**
   * Returns the source slider.
   * 
   * @return the slider
   */
  public SliderComponent getSlider() {
    return slider;
  }

  /**
   * Sets the new value.
   * 
   * @param newValue the new value
   */
  public void setNewValue(int newValue) {
    this.newValue = newValue;
  }

  /**
   * Sets the old value.
   * 
   * @param oldValue the old value
   */
  public void setOldValue(int oldValue) {
    this.oldValue = oldValue;
  }

  /**
   * Sets the source slider.
   * 
   * @param slider the slider
   */
  public void setSlider(SliderComponent slider) {
    this.slider = slider;
  }

}
