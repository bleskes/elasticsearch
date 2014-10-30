/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.client.event;

import com.extjs.gxt.ui.client.event.BoxComponentEvent;
import com.google.gwt.user.client.Event;

import com.prelert.client.widget.SliderComponent;

/**
 * Slider event type.
 * 
 * @see Slider
 */
public class SliderComponentEvent extends BoxComponentEvent
{

	private int m_NewValue = -1;
	private int m_OldValue = -1;
	private SliderComponent m_Slider;


	public SliderComponentEvent(SliderComponent slider)
	{
		super(slider);
		m_Slider = slider;
	}


	public SliderComponentEvent(SliderComponent slider, Event event)
	{
		super(slider, event);
		m_Slider = slider;
	}


	/**
	 * Returns the new value.
	 * 
	 * @return the new value
	 */
	public int getNewValue()
	{
		return m_NewValue;
	}


	/**
	 * Returns the old value.
	 * 
	 * @return the old value
	 */
	public int getOldValue()
	{
		return m_OldValue;
	}


	/**
	 * Returns the source slider.
	 * 
	 * @return the slider
	 */
	public SliderComponent getSlider()
	{
		return m_Slider;
	}


	/**
	 * Sets the new value.
	 * 
	 * @param newValue  the new value
	 */
	public void setNewValue(int newValue)
	{
		m_NewValue = newValue;
	}


	/**
	 * Sets the old value.
	 * 
	 * @param oldValue the old value
	 */
	public void setOldValue(int oldValue)
	{
		m_OldValue = oldValue;
	}


	/**
	 * Sets the source slider.
	 * 
	 * @param slider the slider
	 */
	public void setSlider(SliderComponent slider)
	{
		m_Slider = slider;
	}

}
