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

package com.prelert.client.diagnostics;

import com.extjs.gxt.ui.client.Style.ButtonScale;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.data.BuildInfo;
import com.prelert.splash.IncidentsModule;
import com.prelert.splash.SyncServiceLocator;


/**
 * Welcome module for the Prelert Diagnostics for CA APM (Introscope) UI.
 * It displays introductory text, and links to the Configuration and Activity
 * modules.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>ShowModuleClick</b> : ShowModuleEvent(component, moduleID)<br>
 * <div>Fires when the user clicks on a link to a UI module.</div>
 * <ul>
 * <li>component : this</li>
 * <li>moduleID : the ID of the module whose link has been clicked on</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class DiagnosticsWelcomeModule extends LayoutContainer implements ModuleComponent
{
	public static final String MODULE_ID = "diagnostics_welcome";
	
	private Html		m_HeaderElement;
	private LabelField	m_IntroLabel;
	
	private Button	m_RunAnalysisBtn;
	private Button 	m_ExploreResultsBtn;
	private Button	m_ViewExampleBtn;
	
	
	/**
	 * Creates the Welcome module for the Prelert Diagnostics UI.
	 */
	public DiagnosticsWelcomeModule()
	{
		// Create the header component, used for displaying the application title.
		LayoutContainer header = new LayoutContainer();  
        header.addStyleName("prl_header");
        
        m_HeaderElement = new Html();
        m_HeaderElement.addStyleName("prl_header_resize");
        header.add(m_HeaderElement);
        
        
        // Create the panel displaying introductory text and options.
        LayoutContainer optionsPanel = createOptionsPanel();
        
        // Create the panel displaying links to support and the product video.
        LayoutContainer resourcePanel = createResourcePanel();
        
        // Add the components to the main body.
        LayoutContainer bodyPanel = new LayoutContainer();  
        bodyPanel.setLayout(new RowLayout(Orientation.HORIZONTAL));
        bodyPanel.addStyleName("prl_body");
        bodyPanel.add(optionsPanel, new RowData(1, 1));
        bodyPanel.add(resourcePanel, new RowData(-1, -1, new Margins(5, 0, 5, 50))); 
        
        
        // Create the footer component displaying the version number, copyright notice
        // and link to the Contact page on the Prelert website.
        LayoutContainer footer = new LayoutContainer();   
        footer.setLayout(new RowLayout(Orientation.HORIZONTAL)); 
        footer.addStyleName("prl_footer");
        
        LabelField versionLabel = new LabelField(ClientUtil.CLIENT_CONSTANTS.versionBuild(
        		BuildInfo.VERSION_NUMBER, BuildInfo.BUILD_NUMBER));
        versionLabel.addStyleName("prl_footer_text");
        versionLabel.addStyleName("prl_footer_text_left");
        footer.add(versionLabel, new RowData(-1, 1));
        
        LabelField copyrightLabel = new LabelField(
        		ClientUtil.CLIENT_CONSTANTS.copyrightNotice(BuildInfo.BUILD_YEAR));
        copyrightLabel.addStyleName("prl_footer_text");
        copyrightLabel.addStyleName("prl_footer_text_right1");
        footer.add(copyrightLabel, new RowData(1, 1));
        
        LabelField contactLabel = new LabelField(
        		ClientUtil.CLIENT_CONSTANTS.contactPrelert());
        contactLabel.addListener(Events.OnClick, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent be)
            {
				SyncServiceLocator.openPrelertContactPage();
            }
        	
        });
        
        contactLabel.addStyleName("prl_footer_text");
        contactLabel.addStyleName("prl_footer_text_right2");
        contactLabel.addStyleName("prl-label-link");
        footer.add(contactLabel, new RowData(-1, 1));
        
        add(header);
        add(bodyPanel);
        add(footer);
        
        setScrollMode(Scroll.AUTOY);

	}
	
	
	/**
	 * Creates the panel which displays introductory text and suggested options to
	 * lead the user through the UI.
	 * @return the panel displaying options for the user.
	 */
	protected LayoutContainer createOptionsPanel()
	{
		DiagnosticsMessages messages = DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES;
		
		LayoutContainer optionsPanel = new LayoutContainer();  
        optionsPanel.setLayout(new RowLayout(Orientation.VERTICAL));
        
        LabelField titleLabel = new LabelField(messages.welcomeTitle());
        titleLabel.addStyleName("prl_body_title");
        optionsPanel.add(titleLabel);
        
        m_IntroLabel = new LabelField(messages.welcomeIntro());
        m_IntroLabel.addStyleName("prl_body_text");
        optionsPanel.add(m_IntroLabel, new MarginData(5, 0, 0, 0));
        
        
        // Add buttons for links to the demo and the Config and Activity modules.
        m_RunAnalysisBtn = new Button();
        m_RunAnalysisBtn.addStyleName("prl-icon-btn");
        m_RunAnalysisBtn.addStyleName("prl-option-btn");
        m_RunAnalysisBtn.setText(messages.runAnalysis());
        m_RunAnalysisBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_play_large()));
        m_RunAnalysisBtn.setScale(ButtonScale.LARGE);
        m_RunAnalysisBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				fireEvent(GXTEvents.ShowModuleClick, new ShowModuleEvent(
						DiagnosticsWelcomeModule.this, DiagnosticsConfigModuleComponent.MODULE_ID));
            }
		});
        
        m_ExploreResultsBtn = new Button();
        m_ExploreResultsBtn.addStyleName("prl-icon-btn");
        m_ExploreResultsBtn.addStyleName("prl-option-btn");
        m_ExploreResultsBtn.setText(messages.exploreResults());
        m_ExploreResultsBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_search_large()));
        m_ExploreResultsBtn.setScale(ButtonScale.LARGE);
        m_ExploreResultsBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				fireEvent(GXTEvents.ShowModuleClick, new ShowModuleEvent(
						DiagnosticsWelcomeModule.this, IncidentsModule.MODULE_ID));
            }
		});
        
        m_ViewExampleBtn = new Button();
        m_ViewExampleBtn.addStyleName("prl-icon-btn");
        m_ViewExampleBtn.addStyleName("prl-option-btn");
        m_ViewExampleBtn.setText(messages.seeExample());
        m_ViewExampleBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_table_info_large()));
        m_ViewExampleBtn.setScale(ButtonScale.LARGE);
        m_ViewExampleBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				SyncServiceLocator.openDemoUI();
            }
		});
		
		optionsPanel.add(m_RunAnalysisBtn, new MarginData(20, 0, 25, 50));
		optionsPanel.add(m_ExploreResultsBtn, new MarginData(0, 0, 25, 50));
		optionsPanel.add(m_ViewExampleBtn, new MarginData(0, 0, 0, 50));
		
        return optionsPanel;
	}
	
	
	/**
	 * Creates the panel which displays links to recommended resources, such as
	 * how to contact Prelert Support and a link to a demo video.
	 * @return the panel displaying links to recommended resources.
	 */
	protected LayoutContainer createResourcePanel()
	{
		DiagnosticsMessages messages = DiagnosticsUIBuilder.DIAGNOSTIC_MESSAGES;
		
		LayoutContainer resourcePanel = new LayoutContainer(
        		new RowLayout(Orientation.VERTICAL));
        resourcePanel.setBorders(true);
        resourcePanel.addStyleName("prl_resource_panel");
        
        LabelField titleLabel = new LabelField(messages.welcomeResourcesTitle());
        titleLabel.addStyleName("prl_body_subtitle");
        resourcePanel.add(titleLabel);
        
        // Create a link for contacting support.
        Button contactBtn = new Button();
        contactBtn.addStyleName("prl-icon-btn");
        contactBtn.addStyleName("prl-resource-btn");
        contactBtn.setText(messages.resourceEngineerTitle());
        contactBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_user_large()));
        contactBtn.setScale(ButtonScale.LARGE);
        contactBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				SyncServiceLocator.openSupportDetailsPopup();
            }
		});
        
        // Create a link to the demo video.
        Button videoBtn = new Button();
        videoBtn.addStyleName("prl-icon-btn");
        videoBtn.addStyleName("prl-resource-btn");
        videoBtn.setText(messages.resourceVideoTitle());
        videoBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_video_large()));
        videoBtn.setScale(ButtonScale.LARGE);
        videoBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				Window.open("http://www.prelert.com/download/support/prelert_walkthru_video.html", 
						"prlDiagnosticsVideo", "");
            }
		});
        
        // Create a link to the FAQs.
        Button faqBtn = new Button();
        faqBtn.addStyleName("prl-icon-btn");
        faqBtn.addStyleName("prl-resource-btn");
        faqBtn.setText(messages.resourceFAQsTitle());
        faqBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_info_large()));
        faqBtn.setScale(ButtonScale.LARGE);
        faqBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				Window.open("http://www.prelert.com/download/support/FAQ.html", 
						"prlDiagnosticsFAQ", "");
            }
		});
        
        resourcePanel.add(contactBtn, new MarginData(20, 0, 0, 0));
        resourcePanel.add(videoBtn, new MarginData(30, 0, 0, 0));
        resourcePanel.add(faqBtn, new MarginData(30, 0, 0, 0));
        
        return resourcePanel;
	}
	
	
	/**
	 * Sets the text to use in the Welcome page's header.
	 * @param text the header text.
	 */
	public void setHeaderText(String text)
	{
		// Wrap inside an <h2> tag.
		m_HeaderElement.setHtml("<h2>" + text + "</h2>");
	}
	
	
	/**
	 * Sets the text for the introduction label.
	 * @param text the introduction text.
	 */
	public void setIntroText(String text)
	{
		m_IntroLabel.setText(text);
	}
	

	@Override
    public Component getComponent()
    {
	    return this;
    }

	
	@Override
    public String getModuleId()
    {
	    return MODULE_ID;
    }
	
	
	/**
	 * Shows or hides the link to the Activity module. 
	 * @param show <code>true</code> to show the link to the Activity module, 
	 * 	<code>false</code> to hide.
	 */
	public void showActivityModuleLink(boolean show)
	{
		m_ExploreResultsBtn.setVisible(show);
	}
	
	
	/**
	 * Sets the text on the button which opens up the 'Configure/Run Analysis' page.
	 * @param text text for the button.
	 */
	public void setRunAnalysisButtonText(String text)
	{
		m_RunAnalysisBtn.setText(text);
	}

}
