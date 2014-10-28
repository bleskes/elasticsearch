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

package com.prelert.splash;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.admin.AdminServiceLocator;
import com.prelert.service.UserQueryService;
import com.prelert.service.UserQueryServiceAsync;


/**
 * Ext GWT (GXT) dialog allowing the user to change their Prelert password.
 * The dialog contains text fields for entering the current and new password,
 * and Save and Cancel buttons.
 * @author Pete Harverson
 */
public class ChangePasswordDialog extends Dialog
{
	
	private TextField<String> m_CurrentPwdField;
	private TextField<String> m_NewPwdField;
	private TextField<String> m_ConfirmPwdField;
	
	
	/**
	 * Creates a new dialog for a user to change their Prelert password.
	 */
	public ChangePasswordDialog()
	{
		setHeading(ClientUtil.CLIENT_CONSTANTS.changePassword()); 
		setSize(430, 230);
		setResizable(false);
		
		// Add 'Save' and 'Cancel' buttons.
		setButtons(Dialog.OK + Dialog.CLOSE);    
		Button saveBtn = getButtonById(Dialog.OK);
		saveBtn.setText(ClientUtil.CLIENT_CONSTANTS.save());
		saveBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				saveNewPassword();
            }
		});
		
		Button cancelBtn = getButtonById(Dialog.CLOSE);
		cancelBtn.setText(cancelText);
		
		
		VBoxLayout dialogLayout = new VBoxLayout();    
		dialogLayout.setVBoxLayoutAlign(VBoxLayoutAlign.CENTER);   
		dialogLayout.setPadding(new Padding(25, 15, 25, 25));
		setLayout(dialogLayout); 
		
		FormData formData = new FormData();   
		formData.setWidth(200);
		formData.setMargins(new Margins(5, 0, 5, 0));
		
		FormPanel passwordForm = new FormPanel();
		passwordForm.setHeaderVisible(false);
		passwordForm.setBodyBorder(false);
		passwordForm.setWidth(380);
		
		FormLayout formLayout = new FormLayout();   
		formLayout.setLabelWidth(140); 
		passwordForm.setLayout(formLayout); 
		add(passwordForm);
		
		m_CurrentPwdField = new TextField<String>();   
		m_CurrentPwdField.setPassword(true);
		m_CurrentPwdField.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordCurrent());    
		m_CurrentPwdField.setAllowBlank(false);
		
		m_NewPwdField = new TextField<String>();   
		m_NewPwdField.setPassword(true);
		m_NewPwdField.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordNew());    
		m_NewPwdField.setAllowBlank(false);
		
		m_ConfirmPwdField = new TextField<String>();   
		m_ConfirmPwdField.setPassword(true);
		m_ConfirmPwdField.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordNewConfirm());    
		m_ConfirmPwdField.setAllowBlank(false);
		
		// Add a FormButtonBinding to disable the Save button if fields are blank.
	    FormButtonBinding binding = new FormButtonBinding(passwordForm);   
	    binding.addButton(saveBtn); 
		
		passwordForm.add(m_CurrentPwdField, formData);
		passwordForm.add(m_NewPwdField, formData);
		passwordForm.add(m_ConfirmPwdField, formData);
	}
	

    @Override
    public void show()
    {
    	if (hidden == true)
    	{
    		// If re-opening the dialog, clear existing entries.
    		m_CurrentPwdField.clear();
    		m_NewPwdField.clear();
    		m_ConfirmPwdField.clear();
    	}
    	
	    super.show();
    }


	/**
	 * Saves an edit to an existing user's password.
	 */
	protected void saveNewPassword()
	{
		String newPassword = m_NewPwdField.getValue();
		String confirmPassword = m_ConfirmPwdField.getValue();
		
		if (newPassword.equals(confirmPassword))
		{
			String currentPassword = m_CurrentPwdField.getValue();
			
			UserQueryServiceAsync userQueryService = 
	        	AdminServiceLocator.getInstance().getUserQueryService();
			
			final String loggedInUsername = ClientUtil.getLoggedInUsername();
			userQueryService.setPassword(loggedInUsername, currentPassword, newPassword,
					new ApplicationResponseHandler<Integer>(){

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error changing user password.", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
							ClientUtil.CLIENT_CONSTANTS.errorSavingPassword(), null);
                }

				@Override
                public void uponSuccess(Integer result)
                {
					switch (result)
					{
						case UserQueryService.STATUS_SUCCESS:
							MessageBox.info(ClientUtil.CLIENT_CONSTANTS.changePassword(), 
									ClientUtil.CLIENT_CONSTANTS.passwordChangedSuccess(loggedInUsername), 
									new Listener<MessageBoxEvent>(){

								@Override
		                        public void handleEvent(MessageBoxEvent be)
		                        {
									hide();
		                        }
								
							});
							break;
						
						case UserQueryService.STATUS_INCORRECT_PASSWORD:
							MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
									ClientUtil.CLIENT_CONSTANTS.passwordIncorrect(), null);
							m_CurrentPwdField.clear();
							m_CurrentPwdField.focus();
							break;
							
						default:
							GWT.log("Error changing user password, status code=" + result);
							MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
									ClientUtil.CLIENT_CONSTANTS.errorSavingPassword(), null);
							break;
					}
                }
				
			});
		}
		else
		{
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.changePassword(), 
					ClientUtil.CLIENT_CONSTANTS.passwordsDoNotMatch(), 
					new Listener<MessageBoxEvent>(){

						@Override
                        public void handleEvent(MessageBoxEvent be)
                        {
							m_NewPwdField.focus();
                        }
						
			});
			
			m_NewPwdField.clear();
			m_ConfirmPwdField.clear();
			
		}
	}
	
}
