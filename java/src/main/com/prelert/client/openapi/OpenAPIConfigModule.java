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

package com.prelert.client.openapi;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.DatePickerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
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
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
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
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.CavStatus;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.TimeFrame;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.gxt.AnalysisConfigDataModel;
import com.prelert.data.gxt.DataTypeConfigModel;
import com.prelert.data.gxt.DataTypeConnectionModel;
import com.prelert.service.admin.AnalysisConfigServiceAsync;
import com.prelert.service.admin.AnalysisControlService;
import com.prelert.service.admin.AnalysisControlServiceAsync;
import com.prelert.splash.IncidentsModule;


/**
 * Configuration module for the Prelert Diagnostics for OpenAPI UI.
 * It consists of a set of form fields which allow the user to configure the 
 * necessary properties to run an analysis of data via the Prelert OpenAPI.
 * @author Pete Harverson
 */
public class OpenAPIConfigModule extends LayoutContainer 
	implements DiagnosticsConfigModuleComponent
{
	private ClientMessages 		m_Messages = ClientUtil.CLIENT_CONSTANTS;
	private DiagnosticsMessages m_DiagnosticMessages = GWT.create(DiagnosticsMessages.class);
	
	private AnalysisConfigServiceAsync		m_AnalysisConfigService;
	private AnalysisControlServiceAsync		m_AnalysisControlService;
	
	private CavRunState			m_RunState = CavRunState.CAV_NOT_STARTED;
	private CavStatus			m_CavStatus;
	private List<DataTypeConfigModel> m_DataTypeTemplates;
	private boolean				m_ValidDataConfig;
	private boolean				m_IsWaitingForResponse;
	private boolean				m_ConfiguringNewAnalysis;
	private int 				m_OptimalQueryLength;
	private int 				m_DataPointInterval;
	
	private Label				m_DataSourceLabel;
	private Label				m_DataToAnalyzeLabel;
	private Label				m_ProgressLabel;
	
	private FormData 			m_FormData; 
	
	private FormPanel			m_DataForm;
	private FieldSet			m_DataSourceFieldSet;
	private SimpleComboBox<String>	m_DataTypeCombo;
	private TextField<String>	m_Host;
	private NumberField			m_Port;
	private TextField<String>	m_Username;
	private TextField<String>	m_Password;
	private TextField<String>	m_DatabaseName;
	private Button				m_DataSourceButton;
	private FormButtonBinding	m_DataSourceFormBinding;
	private LabelField			m_DataSourceSavingLabel;
	
	private FieldSet			m_TimeFieldSet;
	private DateField			m_Time;
	private LabelField			m_EstimateTime;
	private Button				m_EstimateButton;
	private FormButtonBinding 	m_TimeFormBinding;
	
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
	
	/**
	 * Creates the configuration module for the OpenAPI Diagnostics UI.
	 */
	public OpenAPIConfigModule()
	{
		m_AnalysisConfigService = AdminServiceLocator.getInstance().getAnalysisConfigService();
		m_AnalysisControlService = AdminServiceLocator.getInstance().getAnalysisControlService();
		
		LayoutContainer formsContainer = new LayoutContainer();
		m_FormData = new FormData();   
		m_FormData.setWidth(INPUT_FIELD_WIDTH);
		
		// Add the Data source settings form.
		m_DataSourceLabel = new Label(m_DiagnosticMessages.wizardDataSourceInstructions(1));
        formsContainer.add(m_DataSourceLabel);
        formsContainer.add(createDataSourceForm(), new MarginData(0, 0, 9, 0));
        
        // Add the form for entering the date to analyse and estimate button..
        m_DataToAnalyzeLabel = new Label(m_DiagnosticMessages.wizardDateSettingsInstructions(2));
        formsContainer.add(m_DataToAnalyzeLabel);
        formsContainer.add(createDataToAnalyzeForm(), new MarginData(0, 0, 9, 0));

        // Add the progress bar and run button form.
        m_ProgressLabel = new Label(m_DiagnosticMessages.wizardProgressInstructions(3));
        formsContainer.add(m_ProgressLabel);
        formsContainer.add(createProgressForm(), new MarginData(0, 0, 9, 0));
        
        // Add the button directing the user to the Activity page on completion.
        formsContainer.add(createFinishForm());
        
        add(formsContainer, new MarginData(15, 30, 7, 30));
        setScrollMode(Scroll.AUTO);
        
        setDataSourceFormEnabled(false, true);
        setDataToAnalyzeFormEnabled(false, false);
        setProgressFormEnabled(false, false);
        setFinishFormEnabled(false);
        
        // Load the list of templates, which in turn then loads the stored configuration.
        loadTemplateDataTypes();
        
        // Load the stored data source configuration.
        loadDataSourceConfiguration();
	}
	
	
	/**
	 * Creates the form for entering the details of the type of data to be analysed
	 * and how to connect to the deta source.
	 * @return <code>FormPanel</code> with fields for specifying the data source.
	 */
	protected Component createDataSourceForm()
	{
		// Add fields for data type, host, port, username, password and database.
		m_DataSourceFieldSet = new FieldSet();  
		m_DataSourceFieldSet.setHeading(m_DiagnosticMessages.wizardDataSourceLabel());
		m_DataSourceFieldSet.setWidth(FIELD_SET_WIDTH);
		m_DataSourceFieldSet.addStyleName("prl-configFieldSet");
        
        FormLayout formLayout = new FormLayout();   
        formLayout.setLabelWidth(LABEL_WIDTH); 
		m_DataSourceFieldSet.setLayout(formLayout); 
		
		m_DataForm = new FormPanel();
		m_DataForm.setWidth(FORM_WIDTH);   
		m_DataForm.setHeaderVisible(false);
		m_DataForm.setBodyBorder(false);
		m_DataForm.add(m_DataSourceFieldSet); 
		
		m_DataTypeCombo = new SimpleComboBox<String>();
		m_DataTypeCombo.setFieldLabel(m_DiagnosticMessages.dataType());    
		m_DataTypeCombo.setEditable(false);
		m_DataTypeCombo.setEmptyText(ClientUtil.CLIENT_CONSTANTS.optionSelect());
		m_DataTypeCombo.setTriggerAction(TriggerAction.ALL); 
		m_DataTypeCombo.setAllowBlank(false);  
		m_DataTypeCombo.addListener(Events.SelectionChange, 
				new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	String selectedType = m_DataTypeCombo.getSimpleValue();
	        	
	        	if (m_DataTypeTemplates != null)
	        	{
	        		for (DataTypeConfigModel template : m_DataTypeTemplates)
	        		{
	        			if (selectedType.equals(template.getDataType()))
	        			{
	        				setDataSourceFormSettings(template);
	        				setDataSourceFormEnabled(true, true);
	        				break;
	        			}
	        		}
	        	}
	        }
        });
	  	m_DataSourceFieldSet.add(m_DataTypeCombo, m_FormData); 
		
	  	m_Host = new TextField<String>();   
	  	m_Host.setFieldLabel(m_DiagnosticMessages.host());   
	  	m_Host.setAllowBlank(false);   
	  	m_DataSourceFieldSet.add(m_Host, m_FormData); 
	    
	    // Add a NumberField with an Integer Property Editor for the EM port.
	    m_Port = new NumberField();   
	    m_Port.setPropertyEditorType(Integer.class);
	    m_Port.setAllowDecimals(false);
	    m_Port.setAllowNegative(false);
	    m_Port.setFieldLabel(m_DiagnosticMessages.port());   
	    m_Port.setAllowBlank(false);   
	    m_DataSourceFieldSet.add(m_Port, m_FormData); 
	    
	    m_Username = new TextField<String>();   
	    m_Username.setFieldLabel(m_DiagnosticMessages.username());   
	    m_Username.setAllowBlank(false);   
	    m_DataSourceFieldSet.add(m_Username, m_FormData); 
	    
	    m_Password = new TextField<String>();   
	    m_Password.setPassword(true);
	    m_Password.setFieldLabel(m_DiagnosticMessages.password());   
	    m_DataSourceFieldSet.add(m_Password, m_FormData); 
	    
	    m_DatabaseName = new TextField<String>();   
	    m_DatabaseName.setFieldLabel(m_DiagnosticMessages.databaseName());   
	    m_DatabaseName.setAllowBlank(false);   
	    m_DataSourceFieldSet.add(m_DatabaseName, m_FormData); 

	    // Add a Test/Edit button.
	    m_DataSourceButton = new Button(m_DiagnosticMessages.test());
	    m_DataSourceButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				if (m_ValidDataConfig == false)
				{
					testConnectionConfiguration();
				}
				else
				{	
					// Button is in Edit mode.
					m_ValidDataConfig = false;
					
					// Clear time and list of selected agents.
					m_Time.clear();
					m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
					
					updateDataSourceEditButtonState(m_RunState);
					updateFormsEnabledState(m_RunState);
				}
            }
		});
	    m_DataSourceButton.setWidth(m_DataForm.getMinButtonWidth()); 
	    
	    m_DataSourceSavingLabel = new LabelField();
	    m_DataSourceSavingLabel.addStyleName("prl-configLabelStress");
	    
	    LayoutContainer applyPanel = new LayoutContainer();
	    applyPanel.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
	    applyPanel.setSize(LABEL_WIDTH + INPUT_FIELD_WIDTH, 22);
	    applyPanel.add(m_DataSourceButton, new RowData(-1, 1, 
	    		new Margins(0, (LABEL_WIDTH + m_DataForm.getPadding() ) - 
	    				m_DataForm.getMinButtonWidth(), 0, 0)));
	    applyPanel.add(m_DataSourceSavingLabel, new RowData(1, 1));
	    
	    AdapterField applyField = new AdapterField(applyPanel);
	    applyField.setHideLabel(true);
	    
	    FormData applyFormData = new FormData();
	    applyFormData.setWidth(LABEL_WIDTH + INPUT_FIELD_WIDTH);
	    applyFormData.setMargins(new Margins(10, 0, 0, 0));
	    m_DataSourceFieldSet.add(applyField, applyFormData); 

	    // Add a FormButtonBinding to disable the Apply button if fields are blank.
	    m_DataSourceFormBinding = new FormButtonBinding(m_DataForm);   
	    m_DataSourceFormBinding.addButton(m_DataSourceButton); 
	    
	    return m_DataForm;
	}
	
	
	/**
	 * Creates the form for entering the incident time, with a button for estimating
	 * the time to analyse the data.
	 * @return <code>FormPanel</code> with fields for specifying the data to analyse.
	 */
	protected Component createDataToAnalyzeForm()
    {	
		// Add a field for the date and estimate button.
		m_TimeFieldSet = new FieldSet();   
		m_TimeFieldSet.setHeading(m_DiagnosticMessages.wizardDataToAnalyzeLabel());
		m_TimeFieldSet.setWidth(FIELD_SET_WIDTH);
		m_TimeFieldSet.addStyleName("prl-configFieldSet");
        
        FormLayout layout = new FormLayout();   
		layout.setLabelWidth(LABEL_WIDTH);  
		m_TimeFieldSet.setLayout(layout); 
		
		FormPanel timeForm = new FormPanel();
		timeForm.setHeaderVisible(false);
		timeForm.setBodyBorder(false);
		timeForm.setWidth(FORM_WIDTH);
		timeForm.add(m_TimeFieldSet);
		
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
	  	m_TimeFieldSet.add(m_Time, m_FormData); 

	    
	    LabelField estimateText = new LabelField(m_DiagnosticMessages.estimateText());
	    m_EstimateTime = new LabelField(m_DiagnosticMessages.estimateTime(0, 0));
	    m_EstimateTime.addStyleName("prl-configLabelStress");
	    m_EstimateButton = new Button(m_DiagnosticMessages.estimate());
	    m_EstimateButton.setWidth(timeForm.getMinButtonWidth());
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
	    		new Margins(0, 4, 0, LABEL_WIDTH - timeForm.getMinButtonWidth() + layout.getLabelPad())));
	    estimatePanel.add(m_EstimateTime, new RowData(1, 1));
	    
	    AdapterField estimateField = new AdapterField(estimatePanel);
	    estimateField.setHideLabel(true);
	    
	    FormData estimateFormData = new FormData();
	    estimateFormData.setWidth(FIELD_SET_WIDTH - 50);
	    estimateFormData.setMargins(new Margins(15, 0, 0, 0));
	    m_TimeFieldSet.add(estimateField, estimateFormData); 


	    // Add a FormButtonBinding to disable the Estimate button if fields are blank.
	    m_TimeFormBinding = new FormButtonBinding(timeForm);   
	    m_TimeFormBinding.addButton(m_EstimateButton); 
	    
	    return timeForm;
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
						OpenAPIConfigModule.this, IncidentsModule.MODULE_ID));
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
	
	
	/**
	 * Tests the validity of the entered Introscope connection configuration.
	 */
	protected void testConnectionConfiguration()
	{
		m_IsWaitingForResponse = true;
		
		DataTypeConfigModel configModel = getDataSourceFormSettings();
		
		ApplicationResponseHandler<ConnectionStatus> callback = 
			new ApplicationResponseHandler<ConnectionStatus>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error testing connection configuration", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_DiagnosticMessages.errorTestingConfigurationData(), null);
				m_DataSourceSavingLabel.clear();
				m_ValidDataConfig = false;
				m_IsWaitingForResponse = false;
				setDataSourceFormEnabled(true, true);
			}


			@Override
            public void uponSuccess(ConnectionStatus connectionStatus)
			{
				m_IsWaitingForResponse = false;
				m_ValidDataConfig = (connectionStatus.getStatus() == ConnectionStatus.Status.CONNECTION_OK);
				m_DataSourceSavingLabel.clear();
				
				if (connectionStatus.getStatus() == ConnectionStatus.Status.CONNECTION_OK)
				{
					loadAnalysisConfiguration();
				}
				else
				{
					String message = m_DiagnosticMessages.warningInvalidConfigurationData();
					if (connectionStatus.getErrorMessage() != null && 
							connectionStatus.getErrorMessage().isEmpty() == false)
					{
						message = m_DiagnosticMessages.warningConnectionError(connectionStatus.getErrorMessage());
					}
					ClientUtil.showErrorMessage(message);
					
					setAnalysisStatus(m_CavStatus);
				}
			}
		};

		m_AnalysisConfigService.testConnectionConfig(configModel, callback);
		
		setDataSourceFormEnabled(false, true);
		m_DataSourceButton.setEnabled(false);
		m_DataSourceSavingLabel.setText(m_DiagnosticMessages.testingWait());
	}
	
	
	/**
	 * Obtains an estimate for the length of time the analysis for the source of data
	 * and time entered by the user.
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
					GWT.log("Error estimating the time it will take to analyse data", caught);
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
							String message = m_DiagnosticMessages.warningNoEstimateConnectionFailure();
							if (estimate.getErrorMessage() != null && 
									estimate.getErrorMessage().isEmpty() == false)
							{
								message = m_DiagnosticMessages.warningEstimateDurationError(estimate.getErrorMessage());
							}
							
							ClientUtil.showErrorMessage(message);
						}
						else if (estimate.getErrorState() == ErrorState.NO_DATA)
						{
							String message = m_DiagnosticMessages.warningNoEstimateData();
							if (estimate.getErrorMessage() != null && 
									estimate.getErrorMessage().isEmpty() == false)
							{
								message = m_DiagnosticMessages.warningEstimateDurationError(estimate.getErrorMessage());
							}
							ClientUtil.showErrorMessage(message);
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
			
			DataTypeConfigModel dataTypeConfig = getDataSourceFormSettings();
			
			// Set the incident time to the end of the day.
			DateWrapper wrapper = new DateWrapper(m_Time.getValue()).clearTime();
			Date timeOfIncident = wrapper.addHours(23).addMinutes(59).addSeconds(59).asDate();
			
			m_AnalysisConfigService.estimateAnalysisDuration(
					dataTypeConfig, timeOfIncident, callback);
			
			m_EstimateTime.setText(m_DiagnosticMessages.estimatingWait());
			setDataToAnalyzeFormEnabled(false, true);
		}
	}


	@Override
	public CavStatus getAnalysisStatus()
	{
		return m_CavStatus;
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
			updateDataSourceEditButtonState(m_RunState);
			updateProgress(status);
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
				GWT.log("OpenAPIConfigModule CavStatus: " + status);
				setAnalysisStatus(status);
			}
		};

		m_AnalysisControlService.getAnalysisStatus(callback);
	}
	
	
	/**
	 * Loads the list of available template data types which can be selected
	 * for configuration in the module.
	 */
	protected void loadTemplateDataTypes()
	{
		ApplicationResponseHandler<List<DataTypeConfigModel>> callback = 
			new ApplicationResponseHandler<List<DataTypeConfigModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error obtaining data type templates", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_DiagnosticMessages.errorLoadingConfigurationData(), null);
			}


			@Override
            public void uponSuccess(List<DataTypeConfigModel> templates)
			{
				// Populate the data type combo and store the templates.
				m_DataTypeTemplates = templates;
				
				// Restore value for case when this call returns after the 
				// call to obtain the stored configuration.
				String comboValue = m_DataTypeCombo.getSimpleValue();

				m_DataTypeCombo.enableEvents(false);
				m_DataTypeCombo.clearSelections();
				m_DataTypeCombo.removeAll();
				for (DataTypeConfigModel template : templates)
				{
					m_DataTypeCombo.add(template.getDataType());
				}
				
				if (comboValue != null)
				{
					m_DataTypeCombo.setSimpleValue(comboValue);
				}
				
				m_DataTypeCombo.enableEvents(true);
			}
		};

		m_AnalysisConfigService.getTemplateDataTypes(callback);
	}
	
	
	/**
	 * Loads the current data source configuration stored on the server.
	 */
	protected void loadDataSourceConfiguration()
	{
		// Loads the stored configuration, and populates
		// the fields in the data source settings form.
		ApplicationResponseHandler<DataTypeConfigModel> callback = 
			new ApplicationResponseHandler<DataTypeConfigModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading data source connection configuration", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_DiagnosticMessages.errorLoadingConfigurationData(), null);
				m_DataSourceSavingLabel.clear();
			}


			@Override
            public void uponSuccess(DataTypeConfigModel config)
			{
				// Check the response hasn't been received after the user has
				// clicked on 'Run New Analysis'.
				if (m_ConfiguringNewAnalysis == false)
				{
					m_DataSourceSavingLabel.clear();	
					setDataSourceFormSettings(config);
					
					if (m_ValidDataConfig == true)
					{
						loadAnalysisConfiguration();
					}
					else
					{
						updateAnalysisStatus();
					}
				}
			}
		};
		
		m_AnalysisConfigService.getConfiguredDataType(callback);
		m_DataSourceSavingLabel.setText(m_DiagnosticMessages.loadingWait());
	}
	
	
	/**
	 * Returns the connection settings defined by the user in the data source form.
	 * Note this only gets saved to the server when an analysis is started.
	 */
	protected DataTypeConfigModel getDataSourceFormSettings()
	{
		DataTypeConfigModel configModel = new DataTypeConfigModel();
		configModel.setDataType(m_DataTypeCombo.getSimpleValue());
		configModel.setDatabaseName(m_DatabaseName.getValue());
		
		DataTypeConnectionModel connection = new DataTypeConnectionModel();
		connection.setHost(m_Host.getValue());
		Number portVal = m_Port.getValue();
		connection.setPort(portVal.intValue());
		connection.setUsername(m_Username.getValue());
		connection.setPassword(m_Password.getValue());
		configModel.setConnectionConfig(connection);
		
		return configModel;
	}
	
	
	/**
	 * Sets the values in the data source form from the properties in the supplied model.
	 * @param dataTypeConfig <code>DataTypeConfigModel</code> encapsulating the
	 * 	configuration data for a data type.
	 */
	protected void setDataSourceFormSettings(DataTypeConfigModel dataTypeConfig)
	{
		String dataType = dataTypeConfig.getDataType();
		if (dataType != null)
		{
			if (m_DataTypeCombo.findModel(dataType) == null)
			{
				// Add this type for case when this is called before the 
				// call to obtain the stored template types.
				m_DataTypeCombo.add(dataType);
			}
			
			m_DataTypeCombo.enableEvents(false);
			m_DataTypeCombo.setSimpleValue(dataType);
			m_DataTypeCombo.enableEvents(true);
		}
		else
		{
			m_DataTypeCombo.clearSelections();
		}

		DataTypeConnectionModel connectionConfig = dataTypeConfig.getConnectionConfig();
		if (connectionConfig != null)
		{
			String host = connectionConfig.getHost();
			if (host != null && host.length() > 0)
			{
				m_Host.setValue(host);
			}
			else
			{
				m_Host.clear();
			}
			
			int port = connectionConfig.getPort();
			if (port > 0)
			{
				m_Port.setValue(connectionConfig.getPort());
			}
			else
			{
				m_Port.clear();
			}
			
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
			
			String databaseName = dataTypeConfig.getDatabaseName();
			if (databaseName != null && databaseName.length() > 0)
			{
				m_DatabaseName.setValue(databaseName);
			}
			else
			{
				m_DatabaseName.clear();
			}
			
			m_ValidDataConfig = connectionConfig.isValid();
			
			if (m_ValidDataConfig == true)
			{	
				m_DataSourceButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
			}
			else
			{
				m_DataSourceButton.setText(m_DiagnosticMessages.test());
			}
		}
		
	}
	
	
	/**
	 * Loads the data analysis configuration (time settings) from the server.
	 */
	protected void loadAnalysisConfiguration()
	{
		// Loads the stored settings, and populates the fields in the Data Analysis form.
		ApplicationResponseHandler<AnalysisConfigDataModel> callback = 
			new ApplicationResponseHandler<AnalysisConfigDataModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading data analysis settings", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						m_DiagnosticMessages.errorLoadingConfigurationData(), null);
			}


			@Override
            public void uponSuccess(AnalysisConfigDataModel settings)
			{
				GWT.log("Loaded data analysis settings: " + settings);
				
				m_Time.setMinValue(settings.getValidDataStartTime());
				m_Time.setMaxValue(settings.getValidDataEndTime());
				
				if (m_ConfiguringNewAnalysis == false)
				{
					// Force user to select time for new analysis.
					Date analysisTime = settings.getTimeToAnalyze();
					if (analysisTime != null)
					{
						m_Time.setValue(settings.getTimeToAnalyze());
					}
				}
				
				// Request status to enable forms as appropriate.
				updateAnalysisStatus();
			}
		};
		
		m_AnalysisConfigService.getDataAnalysisSettings(callback);
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
	 * Saves the data source configuration to the server and then starts
	 * the analysis for the entered incident time.
	 */
	public void saveAndStartAnalysis()
	{
		m_ConfiguringNewAnalysis = false;
		m_IsWaitingForResponse = true;
		
		// Get the configured data type properties.
		DataTypeConfigModel dataTypeConfig = getDataSourceFormSettings();
		
		// Set the incident time to the end of the day.
		DateWrapper wrapper = new DateWrapper(m_Time.getValue()).clearTime();
		Date timeOfIncident = wrapper.addHours(23).addMinutes(59).addSeconds(59).asDate();
		
		// Build the analysis model.
		AnalysisConfigDataModel analysisConfig = new AnalysisConfigDataModel();
		analysisConfig.setTimeToAnalyze(timeOfIncident);		
		analysisConfig.setQueryLength(m_OptimalQueryLength);
		analysisConfig.setDataPointInterval(m_DataPointInterval);
		
		ApplicationResponseHandler<Integer> callback = 
			new ApplicationResponseHandler<Integer>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				m_DataSourceSavingLabel.clear();
				setDataSourceFormEnabled(false, true);
				m_RunButton.setText(m_Messages.start());
				m_RunButton.setEnabled(true);
				m_IsWaitingForResponse = false;
			}


			@Override
            public void uponSuccess(Integer statusCode)
			{
				m_IsWaitingForResponse = false;
				m_DataSourceSavingLabel.clear();
				
				if (statusCode == AnalysisControlService.STATUS_SUCCESS)
				{
					GWT.log("saveAnalysis() started up successfully");
				}
				else
				{
					m_RunButton.setText(m_Messages.start());
					m_RunButton.setEnabled(true);
					setDataSourceFormEnabled(false, true);	
					
					String errorMessage = m_DiagnosticMessages.errorStartingAnalysis();
					switch (statusCode.intValue())
					{
						case AnalysisControlService.STATUS_FAILURE_SAVING_CONFIGURATION:
							errorMessage = m_DiagnosticMessages.errorSavingConfigurationData();
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
		
		m_AnalysisControlService.startAnalysis(dataTypeConfig, analysisConfig, callback);
		m_RunButton.setText(m_Messages.cancel());
		
		// Disable form controls and display loading/progress messages.
		setDataSourceFormEnabled(false, false);
		setDataToAnalyzeFormEnabled(false, false);
		
		m_DataSourceSavingLabel.setText(m_DiagnosticMessages.savingWait());
		m_ProgressBar.updateProgress(0d, m_DiagnosticMessages.percentComplete(Math.round(0)));
		m_StatusLabel1.setText(m_DiagnosticMessages.stageStarting());
		m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
		m_RunButton.setText(m_Messages.cancel());
		m_RunButton.setEnabled(false);
	}
	
	
	/**
	 * Enables or disables the controls in the Data source form.
	 * @param enabled <code>true</code> to enable the controls, <code>false</code>
	 * 	to disable.
	 * @param labelsEnabled <code>true</code> to enable the labels in the form, 
	 * 	<code>false</code> to disable the labels.
	 */
	public void setDataSourceFormEnabled(boolean enabled, boolean labelsEnabled)
	{
		m_DataSourceLabel.setEnabled(labelsEnabled);
		
		if ((enabled == false) && (labelsEnabled == false) )
		{
			m_DataSourceFieldSet.setEnabled(false);
			m_DataSourceFormBinding.stopMonitoring();
		}
		else if ((enabled == false) && (labelsEnabled == true) )
		{
			m_DataSourceFieldSet.setEnabled(true);
			m_DataTypeCombo.setEnabled(false);
			m_Host.setEnabled(false);
			m_Port.setEnabled(false);
			m_Username.setEnabled(false);
			m_Password.setEnabled(false);
			m_DatabaseName.setEnabled(false);
			m_DataSourceButton.setEnabled(true);
			m_DataSourceFormBinding.stopMonitoring();
		}
		else
		{
			m_DataSourceFieldSet.setEnabled(true);
	
			// Disable other fields if data type combo not set.
			String dataType = m_DataTypeCombo.getSimpleValue();	
			if (dataType == null || dataType.length() == 0)
			{
				m_DataTypeCombo.setEnabled(true);
				m_Host.setEnabled(false);
				m_Port.setEnabled(false);
				m_Username.setEnabled(false);
				m_Password.setEnabled(false);
				m_DatabaseName.setEnabled(false);
				m_DataSourceButton.setEnabled(false);
			}
			m_DataSourceFormBinding.startMonitoring();
		}
		
	}
	
	
	/**
	 * Enables or disables the controls in the form for specifying the details of the
	 * data to be analysed.
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
			m_TimeFieldSet.setEnabled(false);
			m_EstimateButton.setEnabled(false);
			m_TimeFormBinding.stopMonitoring();
		}
		else if ((enabled == false) && (labelsEnabled == true) )
		{
			m_DataToAnalyzeLabel.setEnabled(true);
			m_Time.setEnabled(false);
			m_EstimateButton.setEnabled(false);
			m_TimeFormBinding.stopMonitoring();
		}
		else
		{
			m_DataToAnalyzeLabel.setEnabled(true);
			m_TimeFieldSet.setEnabled(true);
			m_EstimateButton.setEnabled(true);
			m_TimeFormBinding.startMonitoring();
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
		
		m_DataSourceSavingLabel.clear();
		setDataSourceFormSettings(new DataTypeConfigModel());
		
		m_Time.clear();
		m_EstimateTime.setText(m_DiagnosticMessages.estimateTime(0, 0));
		
		// Update the state of the UI to enable the controls in the progress section.
		setAnalysisStatus(m_CavStatus);
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
				GWT.log(m_DiagnosticMessages.errorCancellingAnalysis(), caught);
				MessageBox.alert(m_Messages.errorTitle(),
						m_DiagnosticMessages.errorCancellingAnalysis(), null);
			}


			@Override
            public void uponSuccess(Boolean cancelled)
			{
				m_IsWaitingForResponse = false;
				updateAnalysisStatus();
				
				if (cancelled == false)
				{
					m_RunButton.setText(m_Messages.cancel());
					GWT.log(m_DiagnosticMessages.errorCancellingAnalysis());
					MessageBox.alert(m_Messages.errorTitle(),
							m_DiagnosticMessages.errorCancellingAnalysis(), null);
				}
			}
		};
		
		m_AnalysisControlService.cancelAnalysis(callback);
		m_RunButton.setText(m_Messages.start());
		m_RunButton.setEnabled(false);
		m_StatusLabel1.setText(m_DiagnosticMessages.stageCancelling());
	}
	
	
	/**
	 * Updates the enabled state of the forms depending on the 
	 * specified <code>CavRunState</code>.
	 * @param runState run state.
	 */
	protected void updateFormsEnabledState(CavRunState runState)
	{
		switch (runState)
		{			
			case CAV_NOT_STARTED:
				if (m_ValidDataConfig == true)
				{	
					setDataSourceFormEnabled(false, true);
				}
				else
				{
					setDataSourceFormEnabled(true, true);
				}
				setDataToAnalyzeFormEnabled(m_ValidDataConfig, m_ValidDataConfig);
				
				if ( (m_Time.getValue() == null) ||
						(m_EstimateTime.getText() .equals(m_DiagnosticMessages.estimateTime(0, 0)) ) ||
						(m_EstimateTime.getText().equals(m_DiagnosticMessages.estimatingWait())) )
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
				setDataSourceFormEnabled(false, false);
				setDataToAnalyzeFormEnabled(false, false);
				setProgressFormEnabled(true, true);
				setFinishFormEnabled(false);
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
			case CAV_ERROR:
				if (m_ValidDataConfig == true)
				{	
					setDataSourceFormEnabled(false, true);
				}
				else
				{
					setDataSourceFormEnabled(true, true);
				}
				setDataToAnalyzeFormEnabled(m_ValidDataConfig, m_ValidDataConfig);
				
				if ( (m_ValidDataConfig == false) || (m_Time.getValue() == null) || 
						(m_EstimateTime.getText() == m_DiagnosticMessages.estimateTime(0, 0)) ||
						(m_EstimateTime.getText().equals(m_DiagnosticMessages.estimatingWait())) )
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
				setDataSourceFormEnabled(false, false);
				setDataToAnalyzeFormEnabled(false, false);
				setProgressFormEnabled(false, false);
				setFinishFormEnabled(true);
				break;
		}
	}
	
	
	/**
	 * Updates the text and enabled state of the Apply/Edit button in the
	 * Data source form depending on the specified <code>CavRunState</code>.
	 * @param runState run state.
	 */
	protected void updateDataSourceEditButtonState(CavRunState runState)
	{
		switch (runState)
		{
			case CAV_NOT_STARTED:
			case CAV_STOPPED:
			case CAV_FINISHED:
			case CAV_ERROR:
				m_DataSourceButton.setEnabled(m_DataForm.isValid(true));
				if (m_ValidDataConfig == true)
				{	
					m_DataSourceButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
				}
				else
				{
					m_DataSourceButton.setText(m_DiagnosticMessages.test());
				}
				break;

				
			case CAV_RUNNING:
			case CAV_PAUSED:
				m_DataSourceButton.setText(ClientUtil.CLIENT_CONSTANTS.edit());
				m_DataSourceButton.setEnabled(false);
				break;
		}
	}
	
	
	/**
	 * Updates the progress bar and labels to show the current state of the analysis.
	 * @param status the current status of the analysis.
	 */
	protected void updateProgress(CavStatus status)
	{
		// Convert to 0-100 scale used by GXT progress bar.
		Float percentProg = status.getProgressPercent();
		double progressVal = percentProg.doubleValue()/100d;
		progressVal = Math.min(progressVal, 1d);	// Prevent rounding anomaly.
		
		// Enable/disable forms and controls depending on the run state.
		switch (m_RunState)
		{			
			case CAV_NOT_STARTED:
				m_ProgressBar.reset();
				m_StatusLabel1.setText(m_DiagnosticMessages.stageNotYetStarted());
				m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
				
			case CAV_RUNNING:
				m_ProgressBar.updateProgress(progressVal, 
						m_DiagnosticMessages.percentComplete(Math.round(percentProg)));

				// Update the progress labels.
				Date qryTime = status.getCurrentCavQueryDate();
				String qryTimeText = ClientUtil.formatTimeField(qryTime, TimeFrame.MINUTE);
				m_StatusLabel1.setText(m_DiagnosticMessages.queryingDataForTime(qryTimeText));
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
					m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(hours, mins));
				}
				else
				{
					m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
				}
				
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
				m_ProgressBar.updateProgress(progressVal, 
						m_DiagnosticMessages.percentComplete(Math.round(percentProg)));
				m_StatusLabel1.setText(m_DiagnosticMessages.stageStopped());
				m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
			
			case CAV_FINISHED:
				m_ProgressBar.updateProgress(progressVal, 
						m_DiagnosticMessages.percentComplete(Math.round(percentProg)));
				
				m_StatusLabel1.setText(m_DiagnosticMessages.stageCompleted());
				m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
				
			case CAV_ERROR:
				m_StatusLabel1.setText(m_DiagnosticMessages.stageError());
				m_StatusLabel2.setText(m_DiagnosticMessages.estimatedTimeLeft(0, 0));
				m_RunButton.setText(ClientUtil.CLIENT_CONSTANTS.start());
				break;
		}
	}

}
