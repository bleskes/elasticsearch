/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.client.admin;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldSetEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Slider;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.AdapterField;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;

import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.Alerter;
import com.prelert.service.AlertingConfigService;
import com.prelert.service.AlertingConfigServiceAsync;


/**
 * GXT widget for the administration of alerting functionality. It contains
 * a field set with checkbox for enabling or disabling alerting, with a threshold 
 * slider, and a text field for entering the name of the back-end script to be run.
 * @author Pete Harverson
 */
public class AlertingAdminWidget extends LayoutContainer
{
	private AlertingConfigServiceAsync	m_ConfigService;
	
	private Alerter				m_Alerter;
	
	private FieldSet 			m_ConfigFieldSet;
	private Slider		 		m_ThresholdSlider;
	private Label				m_ScriptDirLabel;
	private TextField<String>	m_ScriptName;
	
	/** Directory location on the Prelert server holding the alerting scripts. */
	public static final String	ALERTING_SCRIPT_DIRECTORY = "$PRELERT_HOME/config/alertscripts/";
	
	
	/**
	 * Creates a new widget for administering alerting functionality. It contains
	 * a checkbox for enabling or disabling alerting, a threshold slider, and a
	 * text field for entering the name of the back-end script to be run.
	 */
	public AlertingAdminWidget()
	{
		m_ConfigService = AdminServiceLocator.getInstance().getAlertingConfigService();
		
		initComponents();
		
		loadAlerter();
	}


	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		addStyleName("x-border-layout-ct");
		setStyleAttribute("padding", "15px 30px");
		
		AdminMessages messages = AdminModule.ADMIN_MESSAGES;
		
		// Add a label with a summary of the alerting functionality.
		Label instructions = new Label(messages.alertingInstructions(ALERTING_SCRIPT_DIRECTORY));
        add(instructions);
        
        
    	// Create a FieldSet with checkbox toggle, threshold slider and script name field.
        m_ConfigFieldSet = new FieldSet();
        m_ConfigFieldSet.setHeading("&nbsp;&nbsp;" + messages.enableAlerts());
        m_ConfigFieldSet.setCheckboxToggle(true);
        m_ConfigFieldSet.setWidth(570);
        
        
		FormLayout formLayout = new FormLayout();   
		formLayout.setLabelWidth(120);  
		m_ConfigFieldSet.setLayout(formLayout); 
		
		FormPanel configForm = new FormPanel();
		configForm.setWidth(580);
		configForm.setHeaderVisible(false);
		configForm.setBodyBorder(false);

		
		// Add a slider for setting the activity threshold at which alerts are triggered.
		m_ThresholdSlider = new Slider();
		m_ThresholdSlider.setWidth(210);
		m_ThresholdSlider.setIncrement(1);
		m_ThresholdSlider.setMinValue(1);
		m_ThresholdSlider.setMaxValue(100);
		m_ThresholdSlider.setUseTip(true);

		LayoutContainer sliderCont = new LayoutContainer();   
		sliderCont.setWidth(210);
		sliderCont.addStyleName("prl-anomaly-slider");
		sliderCont.add(m_ThresholdSlider);
		
		final AdapterField sliderField = new AdapterField(sliderCont);
		sliderField.setFieldLabel(messages.anomalyThreshold());

		
		// Add a text field for entering the script file name.
		m_ScriptDirLabel = new Label(ALERTING_SCRIPT_DIRECTORY);
		m_ScriptName = new TextField<String>();  
		m_ScriptName.setEmptyText(messages.scriptFilename());
		m_ScriptName.setWidth(170);
		
		LayoutContainer scriptCont = new LayoutContainer();   
		scriptCont.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
		scriptCont.setSize(390, 22);
		scriptCont.add(m_ScriptDirLabel, new RowData(-1, 1, new Margins(4, 0, 0, 0)));
		scriptCont.add(m_ScriptName, new RowData(160, 1)); 
		
		final AdapterField scriptField = new AdapterField(scriptCont);
		scriptField.setFieldLabel(messages.scriptFile());
		
		// Initially contract the field set, and add the fields on initial expand.
		configForm.add(m_ConfigFieldSet);  	
		m_ConfigFieldSet.setExpanded(false);
		m_ConfigFieldSet.addListener(Events.Expand, new Listener<FieldSetEvent>(){

			@Override
            public void handleEvent(FieldSetEvent be)
            {
				// Add fields on expand to ensure they are correctly laid out.
				FormData formData = new FormData();   
				formData.setWidth(420);
				m_ConfigFieldSet.add(sliderField, formData);
				m_ConfigFieldSet.add(scriptField, formData);
				m_ConfigFieldSet.layout(true);
				m_ConfigFieldSet.removeListener(Events.Expand, this);
            }
	
		});
 
		
		 // Add save and cancel buttons.
	    Button saveButton = new Button(ClientUtil.CLIENT_CONSTANTS.save());
	    saveButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				saveAlerter();
            }
		});
	    Button cancelButton = new Button(ClientUtil.CLIENT_CONSTANTS.cancel());
	    cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				cancelEdit();
            }
		});
	    
	    configForm.setButtonAlign(HorizontalAlignment.LEFT);   
	    configForm.addButton(saveButton);   
	    configForm.addButton(cancelButton);   
		
		add(configForm, new MarginData(10, 0, 0, 20));
	}
	
	
	/**
	 * Loads the Alerter configuration from the server, and populates the form
	 * fields with the loaded settings.
	 */
	protected void loadAlerter()
	{
		// Run the query to load the Alerter config from the config file under $PRELERT_HOME/config.
		ApplicationResponseHandler<Alerter> callback = 
			new ApplicationResponseHandler<Alerter>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading alerter", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						AdminModule.ADMIN_MESSAGES.errorLoadingAlertingData(), null);
			}


			@Override
            public void uponSuccess(Alerter alerter)
			{
				setAlerter(alerter);
			}
		};
		m_ConfigService.getAlerter(callback);
	}
	
	
	/**
	 * Sets the alerting configuration for display in the widget, populating the form
	 * fields with the supplied settings.
	 * @param alerter <code>Alerter</code> to display.
	 */
	public void setAlerter(Alerter alerter)
	{
		m_Alerter = alerter;
		
		if (alerter.getType().equals(Alerter.TYPE_SCRIPT))
		{
			m_ScriptName.setValue(alerter.getScriptName());
			m_ThresholdSlider.setValue(alerter.getThreshold());
			
			// Set expanded state of field set.
			// Do after setting slider as otherwise slider value stays at 0!
			m_ConfigFieldSet.setExpanded(alerter.isEnabled());	
		}
	}
	
	
	/**
	 * Saves changes to the alerting configuration to the server.
	 */
	protected void saveAlerter()
	{
		// Check that a script name has been entered if alerting is enabled.
		String scriptName = m_ScriptName.getValue();
		if (m_ConfigFieldSet.isExpanded() &&
				(scriptName == null || scriptName.length() < 1 || scriptName.equals("")))
		{
			m_ScriptName.markInvalid(m_ScriptName.getMessages().getBlankText());
			
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.warningTitle(),
					AdminModule.ADMIN_MESSAGES.warningNoScriptName(), null);
			
			return;
		}
		
		Alerter alerter = new Alerter();
		alerter.setType(Alerter.TYPE_SCRIPT);
		alerter.setEnabled(m_ConfigFieldSet.isExpanded());
		alerter.setThreshold(m_ThresholdSlider.getValue());
		alerter.setScriptName(m_ScriptName.getValue());
		
		// Run the query to load the Alerter config from the config file under $PRELERT_HOME/config.
		ApplicationResponseHandler<Integer> callback = 
			new ApplicationResponseHandler<Integer>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error saving alerter", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						AdminModule.ADMIN_MESSAGES.errorSavingAlertingData(), null);
			}


			@Override
            public void uponSuccess(Integer success)
			{
				if (success == AlertingConfigService.STATUS_SUCCESS)
				{
					MessageBox.info(ClientUtil.CLIENT_CONSTANTS.infoTitle(),
							AdminModule.ADMIN_MESSAGES.alertingDataSavedSuccess(), null);
				}
				else
				{
					GWT.log("Error code returned from saving alerter: " + success);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							AdminModule.ADMIN_MESSAGES.errorSavingAlertingData(), null);
				}
			}
		};
		m_ConfigService.setAlerter(alerter, callback);
	}
	
	
	/**
	 * Cancels the current edit, and reverts the data in the form to the previous
	 * saved values.
	 */
	protected void cancelEdit()
	{
		setAlerter(m_Alerter);
	}
}
