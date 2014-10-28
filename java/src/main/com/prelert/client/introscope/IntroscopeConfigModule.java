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

package com.prelert.client.introscope;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.DatePickerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.ProgressBar;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.AdapterField;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.admin.AdminServiceLocator;
import com.prelert.client.diagnostics.DiagnosticsConfigModuleComponent;
import com.prelert.client.diagnostics.DiagnosticsMessages;
import com.prelert.client.diagnostics.DiagnosticsUIBuilder;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.introscope.AgentListField;
import com.prelert.client.introscope.IntroscopeDiagnosticsUIBuilder;
import com.prelert.client.introscope.IntroscopeMessages;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.data.CavStatus;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.data.gxt.introscope.IntroscopeDataAnalysisModel;
import com.prelert.service.admin.AnalysisControlService;
import com.prelert.service.admin.AnalysisControlServiceAsync;
import com.prelert.service.introscope.IntroscopeConfigServiceAsync;
import com.prelert.splash.IncidentsModule;


/**
 * Configuration module for the Prelert Diagnostics for CA APM (Introscope) UI.
 * It consists of a set of form fields which allow the user to configure the 
 * necessary properties to run an analysis of Introscope data.
 * @author Pete Harverson
 */
public class IntroscopeConfigModule extends LayoutContainer 
	implements DiagnosticsConfigModuleComponent
{
	private ClientMessages 		m_Messages = ClientUtil.CLIENT_CONSTANTS;
	private DiagnosticsMessages m_DiagnosticMessages = DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES;
	private IntroscopeMessages m_CAMessages = IntroscopeDiagnosticsUIBuilder.INTROSCOPE_MESSAGES;
	
	private AnalysisControlServiceAsync		m_AnalysisControlService;
	private IntroscopeConfigServiceAsync	m_ConfigService;
	
	private CavRunState						m_RunState = CavRunState.CAV_NOT_STARTED;
	private CavStatus						m_CavStatus;
	private DataTypeConfigModel 			m_DataTypeConfig;
	private boolean							m_ValidEMConfig;
	private boolean							m_IsWaitingForResponse;
	private boolean							m_ConfiguringNewAnalysis;
	private int 						    m_OptimalQueryLength;
	private int 							m_DataPointInterval;
	
	private Label				m_EMFormLabel;
	private Label				m_DataToAnalyzeLabel;
	private Label				m_ProgressLabel;
	
	private FormData 			m_FormData;  
	
	private FormPanel			m_EMForm;
	private FieldSet			m_EMFieldSet;
	private TextField<String>	m_Host;
	private NumberField			m_Port;
	private TextField<String>	m_Username;
	private TextField<String>	m_Password;
	private Button				m_EMFormButton;
	private FormButtonBinding	m_EMFormBinding;
	private LabelField			m_EMSavingLabel;
	
	private FieldSet			m_DataFieldSet;
	private DateField			m_Time;
	private AgentListField		m_AgentField;
	private LabelField			m_EstimateTime;
	private Button				m_EstimateButton;
	private FormButtonBinding 	m_DataFormBinding;
	
	private FieldSet			m_ProgressFieldSet;
	private ProgressBar			m_ProgressBar;
	private LabelField			m_StatusLabel1;
	private LabelField			m_StatusLabel2;
	private Button				m_RunButton;
	
	private Label				m_FinishSectionNumber;
	private Label				m_FinishLabel;
	private Button				m_ExploreButton;
	
	private static final int FIELD_SET_WIDTH = 800;
	private static final int FORM_WIDTH = 850;
	private static final int LABEL_WIDTH = 120;
    private static final int INPUT_FIELD_WIDTH = 320;
    
    /** Maximum number of Introscope agents that may be selected for analysis. */
    private static final int MAX_SELECT_AGENTS = 8;
	
	
	/**
	 * Creates the configuration module for the Introscope Diagnostics UI.
	 */
	public IntroscopeConfigModule()
	{
		m_AnalysisControlService = AdminServiceLocator.getInstance().getAnalysisControlService();
		m_ConfigService = IntroscopeServiceLocator.getInstance().getConfigService();
		m_DataTypeConfig = new DataTypeConfigModel(); 
		
		LayoutContainer formsContainer = new LayoutContainer();
		m_FormData = new FormData();   
		m_FormData.setWidth(INPUT_FIELD_WIDTH);
		
		// Add the EM settings form.
		m_EMFormLabel = new Label(m_CAMessages.wizardEMSettingsInstructions());
        formsContainer.add(m_EMFormLabel);
        formsContainer.add(createEMDetailsForm(), new MarginData(0, 0, 10, 0));
        
        // Add the time and agents form.
        m_DataToAnalyzeLabel = new Label(m_CAMessages.wizardDataSettingsInstructions(MAX_SELECT_AGENTS));
        formsContainer.add(m_DataToAnalyzeLabel);
        formsContainer.add(createDataToAnalyzeForm(), new MarginData(0, 0, 10, 0));
        
        // Add the progress bar and run button form.
        m_ProgressLabel = new Label(m_DiagnosticMessages.wizardProgressInstructions(3));
        formsContainer.add(m_ProgressLabel);
        formsContainer.add(createProgressForm(), new MarginData(0, 0, 10, 0));
        
        // Add the button directing the user to the Activity page on completion.
        formsContainer.add(createFinishForm());
        
        add(formsContainer, new MarginData(15, 30, 10, 30));
        setScrollMode(Scroll.AUTO);
        
        setEMDetailsFormEnabled(false, true);
        setDataToAnalyzeFormEnabled(false, false);
        setProgressFormEnabled(false, false);
        setFinishFormEnabled(false);
        
        // Load the stored connection configuration.
        loadConnectionConfiguration();
	}


	/**
	 * Creates the form for entering the details of the CA Enterprise Manager.
	 * @return <code>FormPanel</code> with fields for specifying the EM.
	 */
	protected Component createEMDetailsForm()
	{
		// Add fields for host, port, username and password.
		m_EMFieldSet = new FieldSet();  
		m_EMFieldSet.setHeading(m_CAMessages.wizardEMSettingsLabel());
		m_EMFieldSet.setWidth(FIELD_SET_WIDTH);
		m_EMFieldSet.addStyleName("prl-configFieldSet");
        
        FormLayout emLayout = new FormLayout();   
		emLayout.setLabelWidth(LABEL_WIDTH); 
		m_EMFieldSet.setLayout(emLayout); 
		
		m_EMForm = new FormPanel();
		m_EMForm.setWidth(FORM_WIDTH);   
		m_EMForm.setHeaderVisible(false);
		m_EMForm.setBodyBorder(false);
		m_EMForm.add(m_EMFieldSet);   
		
	  	m_Host = new TextField<String>();   
	  	m_Host.setFieldLabel(m_DiagnosticMessages.host());   
	  	m_Host.setAllowBlank(false);   
	  	m_EMFieldSet.add(m_Host, m_FormData); 
	    
	    // Add a NumberField with an Integer Property Editor for the EM port.
	    m_Port = new NumberField();   
	    m_Port.setPropertyEditorType(Integer.class);
	    m_Port.setAllowDecimals(false);
	    m_Port.setAllowNegative(false);
	    m_Port.setFieldLabel(m_DiagnosticMessages.port());   
	    m_Port.setAllowBlank(false);   
	    m_EMFieldSet.add(m_Port, m_FormData); 
	    
	    m_Username = new TextField<String>();   
	    m_Username.setFieldLabel(m_DiagnosticMessages.username());   
	    m_Username.setAllowBlank(false);   
	    m_EMFieldSet.add(m_Username, m_FormData); 
	    
	    m_Password = new TextField<String>();   
	    m_Password.setPassword(true);
	    m_Password.setFieldLabel(m_DiagnosticMessages.password());   
	    m_EMFieldSet.add(m_Password, m_FormData); 

	    // Add a Test/Edit button.
	 	m_EMFormButton = new Button(m_DiagnosticMessages.test());
	    m_EMFormButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				if (m_ValidEMConfig == false)
				{
					testConnectionConfiguration();
				}
				else
				{	
					// Button is in Edit mode.
					m_ValidEMConfig = false;
					
					// Clear time and list of selected agents.
					m_Time.clear();
					m_AgentField.setSelectedAgents(new ArrayList<String>());
					m_EstimateTime.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimateTime(0, 0));
					
					updateEMFormEditButtonState(m_RunState);
					updateFormsEnabledState(m_RunState);
				}
            }
		});
	    m_EMFormButton.setWidth(m_EMForm.getMinButtonWidth()); 
	    
	    m_EMSavingLabel = new LabelField();
	    m_EMSavingLabel.addStyleName("prl-configLabelStress");
	    
	    LayoutContainer applyPanel = new LayoutContainer();
	    applyPanel.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
	    applyPanel.setSize(LABEL_WIDTH + INPUT_FIELD_WIDTH, 22);
	    applyPanel.add(m_EMFormButton, new RowData(-1, 1, 
	    		new Margins(0, (LABEL_WIDTH + m_EMForm.getPadding() ) - m_EMForm.getMinButtonWidth(), 0, 0)));
	    applyPanel.add(m_EMSavingLabel, new RowData(1, 1));
	    
	    AdapterField applyField = new AdapterField(applyPanel);
	    applyField.setHideLabel(true);
	    
	    FormData applyFormData = new FormData();
	    applyFormData.setWidth(LABEL_WIDTH + INPUT_FIELD_WIDTH);
	    applyFormData.setMargins(new Margins(10, 0, 0, 0));
	    m_EMFieldSet.add(applyField, applyFormData); 

	    // Add a FormButtonBinding to disable the Apply button if fields are blank.
	    m_EMFormBinding = new FormButtonBinding(m_EMForm);   
	    m_EMFormBinding.addButton(m_EMFormButton); 
	    
	    return m_EMForm;
	}
	
	
	/**
	 * Creates the form for entering the incident time and the list of Introscope agents.
	 * @return <code>FormPanel</code> with fields for specifying the data to analyze.
	 */
	protected Component createDataToAnalyzeForm()
    {	
		// Add fields for the date, agents, and a label for the estimate time.
		m_DataFieldSet = new FieldSet();   
        m_DataFieldSet.setHeading(m_DiagnosticMessages.wizardDataToAnalyzeLabel());
        m_DataFieldSet.setWidth(FIELD_SET_WIDTH);
        m_DataFieldSet.addStyleName("prl-configFieldSet");
        
        FormLayout layout = new FormLayout();   
		layout.setLabelWidth(LABEL_WIDTH);  
		m_DataFieldSet.setLayout(layout); 
		
		FormPanel dataForm = new FormPanel();
		dataForm.setHeaderVisible(false);
		dataForm.setBodyBorder(false);
		dataForm.setWidth(FORM_WIDTH);
		dataForm.add(m_DataFieldSet);
		
	  	m_Time = new DateField();   
	  	m_Time.setFieldLabel(m_DiagnosticMessages.problemTime());   
	  	m_Time.setAllowBlank(false);
	  	m_Time.getDatePicker().addListener(Events.Select, new Listener<DatePickerEvent>(){

	  		@Override
            public void handleEvent(DatePickerEvent be) 
	  		{
	            // Force a new estimate to be obtained.
	  			m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
	  			updateFormsEnabledState(m_RunState);
            }
            
        });
	  	
	  	m_Time.addListener(Events.Change, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent fe)
            {
	            // Force a new estimate to be obtained - note this only fires when field loses focus.
	            m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
	            updateFormsEnabledState(m_RunState);
            }
	  		
	  	});
	  	m_DataFieldSet.add(m_Time, m_FormData); 
	    
	    m_AgentField = new AgentListField(MAX_SELECT_AGENTS); 
	    m_AgentField.setFieldLabel(m_CAMessages.agents());   
	    m_AgentField.setAllowBlank(false);   
	    m_DataFieldSet.add(m_AgentField, m_FormData); 
	    m_AgentField.getAgentSelector().addListener(Events.AfterEdit, new Listener<ComponentEvent>(){

			@Override
            public void handleEvent(ComponentEvent be)
            {
				// Selected agents have changed. Force a new estimate to be obtained.
	            m_EstimateTime.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimateTime(0, 0));
	            updateFormsEnabledState(m_RunState);
            }
	    });
	    
	    
	    LabelField estimateText = new LabelField(m_DiagnosticMessages.estimateText());
	    m_EstimateTime = new LabelField(m_DiagnosticMessages.estimateTime(0, 0));
	    m_EstimateTime.addStyleName("prl-configLabelStress");
	    m_EstimateButton = new Button(m_DiagnosticMessages.estimate());
	    m_EstimateButton.setWidth(dataForm.getMinButtonWidth());
	    m_EstimateButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				estimateAnalysisDuration();
            }
		});
	    
	    
	    LayoutContainer estimatePanel = new LayoutContainer();
	    estimatePanel.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
	    estimatePanel.setSize(FIELD_SET_WIDTH - 50, 22);
	    estimatePanel.add(m_EstimateButton);
	    estimatePanel.add(estimateText, new RowData(-1, 1, 
	    		new Margins(0, 4, 0, LABEL_WIDTH - dataForm.getMinButtonWidth() + layout.getLabelPad())));
	    estimatePanel.add(m_EstimateTime, new RowData(1, 1));
	    
	    AdapterField estimateField = new AdapterField(estimatePanel);
	    estimateField.setHideLabel(true);
	    
	    FormData estimateFormData = new FormData();
	    estimateFormData.setWidth(FIELD_SET_WIDTH - 50);
	    estimateFormData.setMargins(new Margins(15, 0, 0, 0));
	    m_DataFieldSet.add(estimateField, estimateFormData); 


	    // Add a FormButtonBinding to disable the Update button if fields are blank.
	    m_DataFormBinding = new FormButtonBinding(dataForm);   
	    m_DataFormBinding.addButton(m_EstimateButton); 
	    
	    return dataForm;
    }
	
	
	/**
	 * Creates the form which displays the progress of the analysis, with buttons
	 * for starting and stopping the analysis.
	 * @return <code>FormPanel</code> with fields for monitoring the progress.
	 */
	protected Component createProgressForm()
    {
        m_ProgressFieldSet = new FieldSet();   
        m_ProgressFieldSet.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
    	m_ProgressFieldSet.setHeading(m_DiagnosticMessages.wizardProgressLabel());
        m_ProgressFieldSet.setSize(FIELD_SET_WIDTH, 85);
        m_ProgressFieldSet.addStyleName("prl-configFieldSet");
		
		FormPanel progressForm = new FormPanel();
		progressForm.setHeaderVisible(false);
		progressForm.setBodyBorder(false);
		progressForm.setWidth(FORM_WIDTH);
		progressForm.add(m_ProgressFieldSet);
		
		
		// Add a Start/Cancel button.
	    m_RunButton = new Button(m_Messages.start());
	    m_RunButton.setWidth(progressForm.getMinButtonWidth());
	    m_RunButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				processRunButtonSelection();
            }
		});
	    
	    // Add a label to show the current stage of the analysis.
	    m_StatusLabel1 = new LabelField(m_DiagnosticMessages.stageNotYetStarted());
		m_StatusLabel1.setHideLabel(true);
		
		LayoutContainer progressLeftPanel = new LayoutContainer();
		progressLeftPanel.setLayout(new RowLayout(Orientation.VERTICAL)); 
		progressLeftPanel.add(m_RunButton);
		progressLeftPanel.add(m_StatusLabel1, new RowData(-1, -1, new Margins(10, 0, 0, 0)));
	    
	    m_ProgressFieldSet.add(progressLeftPanel, new RowData(265, 55)); 
	    
		
		// Add the Progress bar and label.
	    LabelField progressBarLabel = new LabelField(m_DiagnosticMessages.progressFieldLabel());
		m_ProgressBar = new ProgressBar(){

            @Override
            public ProgressBar updateProgress(double value, String text)
            {
	            super.updateProgress(value, text);
	            
	            if (rendered)
	            {
	            	// Fix GXT bug by hiding white text element for low percentages.
	            	double barWidth = Math.floor(value * el().firstChild().getWidth());
	            	
	            	El inner = el().firstChild();
	                El progressBarEl = inner.firstChild();
	                El textTopEl = progressBarEl.firstChild();

	                // Adjust width for border and padding as in El.setWidth().
					barWidth -= textTopEl.getFrameWidth("lr");	
					if (barWidth <= 0)
					{
						textTopEl.addStyleName("x-hidden");
					}
	            }
	            
	            return this;
            }
			
		};   
		
		LayoutContainer progressBarPanel = new LayoutContainer();
		progressBarPanel.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
		progressBarPanel.setSize(380, 22);
		progressBarPanel.add(progressBarLabel);
		progressBarPanel.add(m_ProgressBar, new RowData(1, -1, new Margins(0, 0, 0, 5)));
	
		
		// Add a label below the progress bar to display the time to completion.
		m_StatusLabel2 = new LabelField(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
		m_StatusLabel2.setHideLabel(true);
		
		
		LayoutContainer progressRightPanel = new LayoutContainer();
		progressRightPanel.setLayout(new RowLayout(Orientation.VERTICAL)); 
		progressRightPanel.add(progressBarPanel);
		progressRightPanel.add(m_StatusLabel2, new RowData(-1, -1, new Margins(10, 0, 0, 0)));
	    
	    m_ProgressFieldSet.add(progressRightPanel, new RowData(400, 55)); 
	    
	    return progressForm;
    }
	
	
	/**
	 * Creates the panel which displays a link to the Activity page when the 
	 * analysis is complete.
	 * @return <code>FormPanel</code> with fields for monitoring the progress.
	 */
	protected Component createFinishForm()
    {
		LayoutContainer finishPanel = new LayoutContainer();
		finishPanel.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
		finishPanel.setHeight(22);
		finishPanel.addStyleName("prl-transp-icon");	// Transparent image filter for IE.
		
		// Add a button to show the Activity page.
		m_ExploreButton = new Button(m_DiagnosticMessages.exploreResults(),
				AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_search()));
		m_ExploreButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
			public void componentSelected(ButtonEvent ce)
			{
				fireEvent(GXTEvents.ShowModuleClick, new ShowModuleEvent(
						IntroscopeConfigModule.this, IncidentsModule.MODULE_ID));
			}
		});
		
		m_FinishSectionNumber = new Label(m_DiagnosticMessages.wizardCompletedSectionLabel(4));
		m_FinishLabel = new Label(m_DiagnosticMessages.wizardCompletedInstructions());
		finishPanel.add(m_FinishSectionNumber, new RowData(-1, -1, new Margins(3, 10, 0, 0)));
		finishPanel.add(m_ExploreButton, new RowData(-1, -1));
		finishPanel.add(m_FinishLabel, new RowData(1, -1, new Margins(3, 0, 0, 15)));
	    
	    return finishPanel;
    }
	

	@Override
	public String getModuleId()
	{
		return MODULE_ID;
	}


	@Override
	public Component getComponent()
	{
		return this;
	}
	
	
	@Override
	public void setAnalysisStatus(CavStatus status)
    {
		m_CavStatus = status;
		if (m_ConfiguringNewAnalysis == true)
		{
			m_RunState = CavRunState.CAV_NOT_STARTED;
		}
		else
		{
			m_RunState = m_CavStatus.getRunState();
		}	
		
		if (m_IsWaitingForResponse == false)
		{
			updateFormsEnabledState(m_RunState);
			updateEMFormEditButtonState(m_RunState);
			updateProgress(status);
		}
    }
	
	
	@Override
	public CavStatus getAnalysisStatus()
	{
		return m_CavStatus;
	}
	
	
	/**
	 * Loads the Introscope connection configuration stored on the server.
	 */
	protected void loadConnectionConfiguration()
	{
		// Loads the stored Introscope connection configuration, and populates
		// the fields in the EM settings form.
		ApplicationResponseHandler<DataTypeConfigModel> callback = 
			new ApplicationResponseHandler<DataTypeConfigModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading Introscope connection configuration", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_CAMessages.errorLoadingConfigurationData(), null);
				m_EMSavingLabel.clear();
			}


			@Override
            public void uponSuccess(DataTypeConfigModel dataTypeConfig)
			{
				m_EMSavingLabel.clear();
				
				// Check for null DataTypeConfigModel - this indicates
				// Introscope template has not been found. Show error.
				if (dataTypeConfig == null)
				{
					GWT.log("Introscope template data not found.");
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							m_CAMessages.errorMissingDataTypeConfiguration(), null);
				}
				else
				{
					// Check the response hasn't been received after the user has
					// clicked on 'Run New Analysis'.
					m_DataTypeConfig = dataTypeConfig;
					setEMDetailsFormSettings(m_DataTypeConfig.getConnectionConfig());
					
					if (m_ConfiguringNewAnalysis == false)
					{
						if (m_ValidEMConfig == true)
						{
							m_AgentField.getAgentSelector().setConnectionConfig(m_DataTypeConfig.getConnectionConfig());
							loadAnalysisConfiguration();
						}
						else
						{
							updateAnalysisStatus();
						}
					}
				}
			}
		};
		
		m_EMSavingLabel.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.loadingWait());
		
		if (m_ConfiguringNewAnalysis == true)
		{
			// Force the template data to be loaded.
			m_ConfigService.getTemplateDataType(callback);
		}
		else
		{
			m_ConfigService.getConfiguredDataType(callback);
		}
		
	}
	
	
	/**
	 * Returns the EM settings defined by the user in the EM details form. 
	 * Note this only gets saved to the server when an analysis is started.
	 */
	protected DataTypeConnectionModel getEMDetailsFormSettings()
	{
		DataTypeConnectionModel configModel = new DataTypeConnectionModel();
		configModel.setHost(m_Host.getValue());
		Number portVal = m_Port.getValue();
		configModel.setPort(portVal.intValue());
		configModel.setUsername(m_Username.getValue());
		configModel.setPassword(m_Password.getValue());
		
		return configModel;
	}
	
	
	/**
	 * Sets the values in the EM settings form from the properties in the supplied model.
	 * @param connectionConfig <code>DataTypeConnectionModel</code> encapsulating the
	 * 	properties of a connection to CA APM (Introscope).
	 */
	protected void setEMDetailsFormSettings(DataTypeConnectionModel connectionConfig)
	{
		// Check for empty host/username to prevent validation on null values.
		String host = connectionConfig.getHost();
		if (host != null && host.length() > 0)
		{
			m_Host.setValue(host);
		}
		else
		{
			m_Host.clear();
		}
		m_Port.setValue(connectionConfig.getPort());
		
		String username = connectionConfig.getUsername();
		if (username != null && username.length() > 0)
		{
			m_Username.setValue(username);
		}
		else
		{
			m_Username.clear();
		}
		m_Password.setValue(connectionConfig.getPassword());
		m_ValidEMConfig = connectionConfig.isValid();
		
		if (m_ValidEMConfig == true)
		{	
			m_EMFormButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
		}
		else
		{
			m_EMFormButton.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.test());
		}
	}
	
	
	/**
	 * Tests the validity of the entered Introscope connection configuration.
	 */
	protected void testConnectionConfiguration()
	{
		m_IsWaitingForResponse = true;
		
		final DataTypeConnectionModel configModel = getEMDetailsFormSettings();
		
		ApplicationResponseHandler<ConnectionStatus> callback = 
			new ApplicationResponseHandler<ConnectionStatus>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error testing Introscope connection configuration", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_CAMessages.errorTestingConfigurationData(), null);
				m_EMSavingLabel.clear();
				m_ValidEMConfig = false;
				m_IsWaitingForResponse = false;
				setEMDetailsFormEnabled(true, true);
			}


			@Override
            public void uponSuccess(ConnectionStatus connectionStatus)
			{
				m_IsWaitingForResponse = false;
				m_ValidEMConfig = (connectionStatus.getStatus() == ConnectionStatus.Status.CONNECTION_OK);
				m_EMSavingLabel.clear();
				
				if (connectionStatus.getStatus() == ConnectionStatus.Status.CONNECTION_FAILED)
				{
					String message = m_CAMessages.warningInvalidConfigurationData();
					if (connectionStatus.getErrorMessage() != null &&
							connectionStatus.getErrorMessage().isEmpty() == false)
					{
						message = m_DiagnosticMessages.warningConnectionError(
													connectionStatus.getErrorMessage());
					}
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.warningTitle(),
									message, null);
					
					setAnalysisStatus(m_CavStatus);
				}
				else
				{
					if (connectionStatus.getStatus() == ConnectionStatus.Status.MISSING_HEALTH_METRICS)
					{
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.warningTitle(),
								m_CAMessages.warningMissingHealthMetrics(), null);
					}

					m_AgentField.getAgentSelector().setConnectionConfig(configModel);
					loadAnalysisConfiguration();
				}
			}
		};
		m_ConfigService.testConnectionConfig(configModel, callback);
		
		setEMDetailsFormEnabled(false, true);
		m_EMFormButton.setEnabled(false);
		m_EMSavingLabel.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.testingWait());
	}

	
	/**
	 * Loads the data analysis configuration (time and agents) from the server.
	 */
	protected void loadAnalysisConfiguration()
	{
		// Loads the stored settings, and populates the fields in the Data Analysis form.
		ApplicationResponseHandler<IntroscopeDataAnalysisModel> callback = 
			new ApplicationResponseHandler<IntroscopeDataAnalysisModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading Introscope data analysis settings", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_CAMessages.errorLoadingAnalysisSettings(), null);
			}


			@Override
            public void uponSuccess(IntroscopeDataAnalysisModel settings)
			{
				GWT.log("Loaded data analysis settings: " + settings);
				
				m_Time.setMinValue(settings.getValidDataStartTime());
				m_Time.setMaxValue(settings.getValidDataEndTime());
				
				if (m_ConfiguringNewAnalysis == false)
				{
					// Force user to select time and agents for new analysis.
					Date analysisTime = settings.getTimeToAnalyze();
					if (analysisTime != null)
					{
						m_Time.setValue(settings.getTimeToAnalyze());
					}
					
					List<String> agents = settings.getAgents();
					m_AgentField.setSelectedAgents(agents);
				}
				
				// Request status to enable forms as appropriate.
				updateAnalysisStatus();
			}
		};
		
		DataTypeConnectionModel configModel = getEMDetailsFormSettings();
		
		m_ConfigService.getDataAnalysisSettings(configModel, callback);
	}
	
	
	/**
	 * Obtains an estimate for the length of time the analysis of the data will take,
	 * using the list of Introscope agents selected by the user.
	 */
	protected void estimateAnalysisDuration()
	{
		if (m_RunState == CavRunState.CAV_RUNNING && m_CavStatus.getProgressPercent() > 0f)
		{
			Date now = new Date();
			double toDateMs = (now.getTime() - m_CavStatus.getDateCavStarted().getTime());
			long durationMs =  (long)(((100d/m_CavStatus.getProgressPercent()) * toDateMs));
			int hours = ClientUtil.getNumberOfHours(durationMs);
			int mins = ClientUtil.getModMinutes(durationMs);
			m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(hours, mins));
		}
		else
		{
			m_IsWaitingForResponse = true;
			
			ApplicationResponseHandler<AnalysisDuration> callback = new ApplicationResponseHandler<AnalysisDuration>()
			{
				@Override
	            public void uponFailure(Throwable caught)
				{
					GWT.log("Error estimating the time it will take to analyse Introscope data", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							m_DiagnosticMessages.errorEstimatedAnalysisDuration(), null);
					m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
					m_IsWaitingForResponse = false;
					setDataToAnalyzeFormEnabled(true, true);
				}
	
	
				@Override
	            public void uponSuccess(AnalysisDuration estimate)
				{
					m_IsWaitingForResponse = false;
					
					long durationMs = estimate.getEstimatedAnalysisDurationMs();
					if (estimate.getErrorState() == ErrorState.NO_ERROR &&
							durationMs > 0l)
					{
						m_OptimalQueryLength = estimate.getOptimalQueryLengthSecs();
						m_DataPointInterval = estimate.getActualDataPointIntervalSecs();
						
						int hours = ClientUtil.getNumberOfHours(durationMs);
						int mins = ClientUtil.getModMinutes(durationMs);
						m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(hours, mins));
					}
					else
					{
						if (estimate.getErrorState() == ErrorState.CONNECTION_FAILURE)
						{
							MessageBox.alert(m_Messages.warningTitle(),
									m_CAMessages.warningNoEstimateConnectionFailure(), null);
						}
						else if (estimate.getErrorState() == ErrorState.NO_DATA)
						{
							String message = m_DiagnosticMessages.warningNoEstimateData();
							if (estimate.getErrorMessage() != null && estimate.getErrorMessage().isEmpty() == false)
							{
								message = m_DiagnosticMessages.warningEstimateDurationError(estimate.getErrorMessage());
							}
							MessageBox.alert(m_Messages.warningTitle(), message, null);
						}
						else if (estimate.getErrorState() == ErrorState.DATA_AT_TOO_LARGE_INTERVAL)
						{
							MessageBox.alert(m_Messages.warningTitle(),
									m_DiagnosticMessages.warningEstimateDataPointIntervalTooLarge(
											estimate.getRequiredDataPointIntervalSecs(),
											estimate.getActualDataPointIntervalSecs()), null);
						}
							
						m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
					}
					
					// Update the state of the UI to enable the controls in the progress section.
					setAnalysisStatus(m_CavStatus);
				}
			};
			
			DataTypeConnectionModel emConfig = getEMDetailsFormSettings();
			
			IntroscopeDataAnalysisModel settings = new IntroscopeDataAnalysisModel();
			
			// Set the incident time to the end of the day.
			DateWrapper wrapper = new DateWrapper(m_Time.getValue()).clearTime();
			Date timeOfIncident = wrapper.addHours(23).addMinutes(59).addSeconds(59).asDate();
			settings.setTimeToAnalyze(timeOfIncident);
			
			settings.setAgents(m_AgentField.getSelectedAgents());
			
			m_ConfigService.estimateAnalysisDuration(emConfig, settings, callback);
			
			m_EstimateTime.setText(m_DiagnosticMessages.estimatingWait());
			setDataToAnalyzeFormEnabled(false, true);
		}
	}
	
	
	/**
	 * Queries the status of the analysis from the back-end in order to update
	 * the state of the form controls in response to user action.
	 */
	protected void updateAnalysisStatus()
	{
		ApplicationResponseHandler<CavStatus> callback = 
			new ApplicationResponseHandler<CavStatus>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error obtaining status of analysis", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_DiagnosticMessages.errorLoadingAnalysisStatus(), null);
			}


			@Override
            public void uponSuccess(CavStatus status)
			{
				GWT.log("DiagnosticConfigModule CavStatus: " + status);
				setAnalysisStatus(status);
			}
		};
		
		m_AnalysisControlService.getAnalysisStatus(callback);
	}
	
	
	/**
	 * Enables or disables the controls in the CA Enterprise Manager (EM) form.
	 * @param enabled <code>true</code> to enable the controls, <code>false</code>
	 * 	to disable.
	 * @param labelsEnabled <code>true</code> to enable the labels in the form, 
	 * 	<code>false</code> to disable the labels.
	 */
	public void setEMDetailsFormEnabled(boolean enabled, boolean labelsEnabled)
	{
		if ((enabled == false) && (labelsEnabled == false) )
		{
			m_EMFormLabel.setEnabled(false);
			m_EMFieldSet.setEnabled(false);
			m_EMFormBinding.stopMonitoring();
		}
		else if ((enabled == false) && (labelsEnabled == true) )
		{
			m_EMFormLabel.setEnabled(true);
			m_EMFieldSet.setEnabled(true);
			m_Host.setEnabled(false);
			m_Port.setEnabled(false);
			m_Username.setEnabled(false);
			m_Password.setEnabled(false);
			m_EMFormButton.setEnabled(true);
			m_EMFormBinding.stopMonitoring();
		}
		else
		{
			m_EMFormLabel.setEnabled(true);
			m_EMFieldSet.setEnabled(true);
			m_EMFormBinding.startMonitoring();
		}
		
	}
	
	
	/**
	 * Enables or disables the controls in the form for specifying the details of the
	 * data to be analyzed.
	 * @param enabled <code>true</code> to enable the controls, <code>false</code>
	 * 	to disable.
	 * @param labelsEnabled <code>true</code> to enable the labels in the form, 
	 * 	<code>false</code> to disable the labels.
	 */
	public void setDataToAnalyzeFormEnabled(boolean enabled, boolean labelsEnabled)
	{
		if ((enabled == false) && (labelsEnabled == false) )
		{
			m_DataToAnalyzeLabel.setEnabled(false);
			m_DataFieldSet.setEnabled(false);
			m_EstimateButton.setEnabled(false);
			m_DataFormBinding.stopMonitoring();
		}
		else if ((enabled == false) && (labelsEnabled == true) )
		{
			m_DataToAnalyzeLabel.setEnabled(true);
			m_Time.setEnabled(false);
			m_AgentField.setEnabled(false);
			m_EstimateButton.setEnabled(false);
			m_DataFormBinding.stopMonitoring();
		}
		else
		{
			m_DataToAnalyzeLabel.setEnabled(true);
			m_DataFieldSet.setEnabled(true);
			m_EstimateButton.setEnabled(true);
			m_DataFormBinding.startMonitoring();
		}
	}
	
	
	/**
	 * Enables or disables the controls in the form used for following the progress
	 * of the analysis.
	 * @param enabled <code>true</code> to enable the controls, <code>false</code>
	 * 	to disable.
	 * @param labelsEnabled <code>true</code> to enable the labels in the form, 
	 * 	<code>false</code> to disable the labels.
	 */
	public void setProgressFormEnabled(boolean enabled, boolean labelsEnabled)
	{
		m_ProgressLabel.setEnabled(labelsEnabled);
		m_ProgressFieldSet.setEnabled(enabled);
		m_RunButton.setEnabled(enabled);
	}
	
	
	/**
	 * Enables or disables the controls in the form with links at the end of the analysis.
	 * @param enabled <code>true</code> to enable the controls, <code>false</code>
	 * 	to disable.
	 */
	public void setFinishFormEnabled(boolean enabled)
	{
		m_FinishSectionNumber.setEnabled(enabled);
		m_FinishLabel.setEnabled(enabled);
		m_ExploreButton.setEnabled(enabled);
	}
	
	
	@Override
	public void resetForNewAnalysis()
	{
		m_ConfiguringNewAnalysis = true;
		
		m_EMSavingLabel.clear();
		setEMDetailsFormSettings(new DataTypeConnectionModel());
		
		m_Time.clear();
		m_AgentField.setSelectedAgents(new ArrayList<String>());
		m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
		
		// Update the state of the UI to enable the controls in the progress section.
		setAnalysisStatus(m_CavStatus);
		
		// Load the template connection data.
		loadConnectionConfiguration();
	}
	
	
	/**
	 * Updates the enabled state of the forms depending on the 
	 * specified <code>CavRunState</code>.
	 * @param runState run state.
	 */
	protected void updateFormsEnabledState(CavRunState runState)
	{
		// Enable/disable forms and controls depending on the run state.
		switch (runState)
		{			
			case CAV_NOT_STARTED:
				if (m_ValidEMConfig == true)
				{	
					setEMDetailsFormEnabled(false, true);
				}
				else
				{
					setEMDetailsFormEnabled(true, true);
				}
				setDataToAnalyzeFormEnabled(m_ValidEMConfig, m_ValidEMConfig);
				
				if ( (m_Time.getValue() == null) || (m_AgentField.getSelectedAgents().size() == 0) ||
						(m_EstimateTime.getText() .equals(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimateTime(0, 0)) ) ||
						(m_EstimateTime.getText().equals(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimatingWait())) )
				{
					setProgressFormEnabled(false, false);
				}
				else
				{
					setProgressFormEnabled(true, true);
				}
				
				setFinishFormEnabled(false);
				break;
				
			case CAV_RUNNING:
				setEMDetailsFormEnabled(false, false);
				setDataToAnalyzeFormEnabled(false, false);
				setProgressFormEnabled(true, true);
				setFinishFormEnabled(false);
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
			case CAV_ERROR:
				if (m_ValidEMConfig == true)
				{	
					setEMDetailsFormEnabled(false, true);
				}
				else
				{
					setEMDetailsFormEnabled(true, true);
				}
				setDataToAnalyzeFormEnabled(m_ValidEMConfig, m_ValidEMConfig);
				
				if ( (m_ValidEMConfig == false) || 
						(m_Time.getValue() == null) || (m_AgentField.getSelectedAgents().size() == 0) || 
						(m_EstimateTime.getText() == DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimateTime(0, 0)) ||
						(m_EstimateTime.getText().equals(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.estimatingWait())) )
				{
					setProgressFormEnabled(false, false);
					
				}
				else
				{
					setProgressFormEnabled(true, true);
				}
				setFinishFormEnabled(false);
				break;
				
			case CAV_FINISHED:
				setEMDetailsFormEnabled(false, false);
				setDataToAnalyzeFormEnabled(false, false);
				setProgressFormEnabled(false, false);
				setFinishFormEnabled(true);
				break;
		}
	}
	
	
	/**
	 * Updates the text and enabled state of the Apply/Edit button in the
	 * EM settings form depending on the specified <code>CavRunState</code>.
	 * @param runState run state.
	 */
	protected void updateEMFormEditButtonState(CavRunState runState)
	{
		switch (runState)
		{
			case CAV_NOT_STARTED:
			case CAV_STOPPED:
			case CAV_FINISHED:
			case CAV_ERROR:
				m_EMFormButton.setEnabled(true);
				if (m_ValidEMConfig == true)
				{	
					m_EMFormButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
				}
				else
				{
					m_EMFormButton.setText(DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES.test());
				}
				break;

				
			case CAV_RUNNING:
			case CAV_PAUSED:
				m_EMFormButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
				m_EMFormButton.setEnabled(false);
				break;
		}
	}
	
	
	/**
	 * Updates the progress bar and labels to show the current state of the analysis.
	 * @param status the current status of the analysis.
	 */
	protected void updateProgress(CavStatus status)
	{
		DiagnosticsMessages caMessages = DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES;
		
		// Convert to 0-100 scale used by GXT progress bar.
		Float percentProg = status.getProgressPercent();
		double progressVal = percentProg.doubleValue()/100d;
		progressVal = Math.min(progressVal, 1d);	// Prevent rounding anomaly.
		
		// Enable/disable forms and controls depending on the run state.
		switch (m_RunState)
		{			
			case CAV_NOT_STARTED:
				m_ProgressBar.reset();
				m_StatusLabel1.setText(caMessages.stageNotYetStarted());
				m_StatusLabel2.setText(caMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
				
			case CAV_RUNNING:
				m_ProgressBar.updateProgress(progressVal, 
						caMessages.percentComplete(Math.round(percentProg)));

				// Update the progress labels.
				Date qryTime = status.getCurrentCavQueryDate();
				String qryTimeText = ClientUtil.formatTimeField(qryTime, TimeFrame.MINUTE);
				m_StatusLabel1.setText(caMessages.queryingDataForTime(qryTimeText));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.cancel());
				
				if (percentProg > 0)
				{
					double toDateMs = (status.getTimeStamp().getTime()
										- status.getDateCavStarted().getTime());
					if (toDateMs < 0)
					{
						// This should never be < 0 unless some error has occurred. 
						// Possibly a NTP adjustment could cause this.
						toDateMs = 1;
					}
					long timeToFinish =  (long)(((100d/percentProg) * toDateMs) - toDateMs);
					
					int hours = ClientUtil.getNumberOfHours(timeToFinish);
					int mins = ClientUtil.getModMinutes(timeToFinish);
					m_StatusLabel2.setText(caMessages.estimatedTimeLeft(hours, mins));
				}
				else
				{
					m_StatusLabel2.setText(caMessages.estimatedTimeLeft(0, 0));
				}
				
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
				m_ProgressBar.updateProgress(progressVal, 
						caMessages.percentComplete(Math.round(percentProg)));
				m_StatusLabel1.setText(caMessages.stageStopped());
				m_StatusLabel2.setText(caMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
			
			case CAV_FINISHED:
				m_ProgressBar.updateProgress(progressVal, 
						caMessages.percentComplete(Math.round(percentProg)));
				
				m_StatusLabel1.setText(caMessages.stageCompleted());
				m_StatusLabel2.setText(caMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
				
			case CAV_ERROR:
				m_StatusLabel1.setText(caMessages.stageError());
				m_StatusLabel2.setText(caMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
		}
	}

	
	/**
	 * Processes the selection event on the 'Run' button according to the current
	 * state of the analysis.
	 */
	protected void processRunButtonSelection()
	{	
		switch (m_RunState)
		{			
			case CAV_NOT_STARTED:
				if (m_ConfiguringNewAnalysis == true)
				{
					Listener<MessageBoxEvent> cb = new Listener<MessageBoxEvent>() 
					{   
						@Override
	                    public void handleEvent(MessageBoxEvent be)
						{   
							if (be.getButtonClicked().getItemId() == Dialog.YES)
							{
								saveAndStartAnalysis();
							}
						}   
					}; 
					
					MessageBox.confirm(m_Messages.warningTitle(), 
							m_DiagnosticMessages.warningStartAnalysis(), cb);
				}
				else
				{
					saveAndStartAnalysis();
				}
				break;
				
			case CAV_RUNNING:
				// Ask for confirmation.
				Listener<MessageBoxEvent> callback = new Listener<MessageBoxEvent>() 
				{   
					@Override
                    public void handleEvent(MessageBoxEvent be)
					{   
						if (be.getButtonClicked().getItemId() == Dialog.YES)
						{
							cancelAnalysis();
						}
					}   
				}; 
				
				MessageBox.confirm(m_Messages.warningTitle(), 
						m_DiagnosticMessages.warningCancelAnalysis(), callback);
				
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
				saveAndStartAnalysis();
				break;
			
			case CAV_FINISHED:
				// Run button should not be enabled in this state, but just in case..
				Listener<MessageBoxEvent> cb = new Listener<MessageBoxEvent>() 
				{   
					@Override
                    public void handleEvent(MessageBoxEvent be)
					{   
						if (be.getButtonClicked().getItemId() == Dialog.YES)
						{
							saveAndStartAnalysis();
						}
					}   
				}; 
				
				MessageBox.confirm(m_Messages.warningTitle(), 
						m_DiagnosticMessages.warningStartAnalysis(), cb);
				break;
		}
	}
	
	
	/**
	 * Saves the Introscope connection configuration to the server and then starts
	 * the analysis using the entered list of agents and incident time.
	 */
	public void saveAndStartAnalysis()
	{
		m_ConfiguringNewAnalysis = false;
		m_IsWaitingForResponse = true;
		
		// Set the connection properties in the data type config model.
		DataTypeConnectionModel connectionConfig = getEMDetailsFormSettings();
		m_DataTypeConfig.setConnectionConfig(connectionConfig);
		
		// Set the incident time to the end of the day.
		DateWrapper wrapper = new DateWrapper(m_Time.getValue()).clearTime();
		Date timeOfIncident = wrapper.addHours(23).addMinutes(59).addSeconds(59).asDate();
		
		// Build the analysis model.
		IntroscopeDataAnalysisModel analysisConfig = new IntroscopeDataAnalysisModel();
		analysisConfig.setTimeToAnalyze(timeOfIncident);		
		analysisConfig.setQueryLength(m_OptimalQueryLength);
		analysisConfig.setDataPointInterval(m_DataPointInterval);
		analysisConfig.setAgents(m_AgentField.getSelectedAgents());
		
		ApplicationResponseHandler<Integer> callback = 
			new ApplicationResponseHandler<Integer>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				m_EMSavingLabel.clear();
				setEMDetailsFormEnabled(false, true);
				m_RunButton.setText(m_Messages.start());
				m_RunButton.setEnabled(true);
				m_IsWaitingForResponse = false;
			}


			@Override
            public void uponSuccess(Integer statusCode)
			{
				m_IsWaitingForResponse = false;
				m_EMSavingLabel.clear();
				
				if (statusCode == AnalysisControlService.STATUS_SUCCESS)
				{
					GWT.log("saveAnalysis() started up successfully");
				}
				else
				{
					m_RunButton.setText(m_Messages.start());
					m_RunButton.setEnabled(true);
					setEMDetailsFormEnabled(false, true);	
					
					String errorMessage = m_DiagnosticMessages.errorStartingAnalysis();
					switch (statusCode.intValue())
					{
						case AnalysisControlService.STATUS_FAILURE_SAVING_CONFIGURATION:
							errorMessage = m_CAMessages.errorSavingConfigurationData();
							break;
							
						case AnalysisControlService.STATUS_FAILURE_STARTING_ANALYSIS:
						case AnalysisControlService.STATUS_FAILURE_UNKNOWN:
						default:
							errorMessage = m_DiagnosticMessages.errorStartingAnalysis();
							break;
					}
					
					GWT.log("Error saving configuration and starting analysis. Error code=" + statusCode);
					MessageBox.alert(m_Messages.errorTitle(), errorMessage, null);
				}
				
				updateAnalysisStatus();
			}
		};
		
		m_ConfigService.startAnalysis(m_DataTypeConfig, analysisConfig, callback);
		m_RunButton.setText(m_Messages.cancel());
		
		// Disable form controls and display loading/progress messages.
		setEMDetailsFormEnabled(false, false);
		setDataToAnalyzeFormEnabled(false, false);
		
		m_EMSavingLabel.setText(m_DiagnosticMessages.savingWait());
		m_ProgressBar.updateProgress(0d, m_DiagnosticMessages.percentComplete(Math.round(0)));
		m_StatusLabel1.setText(m_DiagnosticMessages.stageStarting());
		m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
		m_RunButton.setText(m_Messages.cancel());
		m_RunButton.setEnabled(false);
	}
	
	
	@Override
	public void cancelAnalysis()
	{
		m_IsWaitingForResponse = true;
		
		ApplicationResponseHandler<Boolean> callback = 
			new ApplicationResponseHandler<Boolean>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				m_IsWaitingForResponse = false;
				m_RunButton.setEnabled(true);
				GWT.log(m_CAMessages.errorCancellingAnalysis(), caught);
				MessageBox.alert(m_Messages.errorTitle(),
						m_CAMessages.errorCancellingAnalysis(), null);
			}


			@Override
            public void uponSuccess(Boolean cancelled)
			{
				m_IsWaitingForResponse = false;
				updateAnalysisStatus();
				
				if (cancelled == false)
				{
					m_RunButton.setText(m_Messages.cancel());
					GWT.log(m_CAMessages.errorCancellingAnalysis());
					MessageBox.alert(m_Messages.errorTitle(),
							m_CAMessages.errorCancellingAnalysis(), null);
				}
			}
		};
		
		m_AnalysisControlService.cancelAnalysis(callback);
		m_RunButton.setText(m_Messages.start());
		m_RunButton.setEnabled(false);
		m_StatusLabel1.setText(m_DiagnosticMessages.stageCancelling());
	}
}
