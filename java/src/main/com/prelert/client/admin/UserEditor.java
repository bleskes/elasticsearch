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

import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.CardLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.UserEditorEvent;
import com.prelert.data.gxt.UserModel;
import com.prelert.service.UserQueryService;
import com.prelert.service.UserQueryServiceAsync;


/**
 * Ext GWT (GXT) widget holding the fields for viewing and editing the details
 * of a new or existing user. The container holds a number of form fields,
 * with buttons for saving any changes to the user's details. The panel has
 * two editing modes:
 * <ul>
 * <li>CREATE: for entering details of a new user</li>
 * <li>EDIT: for editing the details of an existing user</li>
 * </ul>
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>AfterEdit</b> : UserEditorEvent(userEditor, startData, userData)<br>
 * <div>Fires after a user is edited.</div>
 * <ul>
 * <li>userEditor : this</li>
 * <li>startData : the user data before the edit</li>
 * <li>userData : the user data after the edit</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>CancelEdit</b> : UserEditorEvent(userEditor, startData, userData)<br>
 * <div>Fires after a user is edited.</div>
 * <ul>
 * <li>userEditor : this</li>
 * <li>startData : the user data before the aborted edit</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class UserEditor extends LayoutContainer
{	
	public enum EditingMode
	{
		CREATE, EDIT
	}
	
	private UserQueryServiceAsync		m_QueryService;
	
	private ListStore<BaseModelData>	m_RoleList;
	
	private CardLayout 				m_CardLayout;
	private ContentPanel 			m_EditForm;
	private ContentPanel 			m_AddForm;
	
	private TextField<String>		m_EditUsername;
	private TextField<String>		m_EditFirstName;
	private TextField<String>		m_EditLastName;
	private ComboBox<BaseModelData> m_EditRoleCombo;
	private TextField<String>		m_EditPassword;
	private TextField<String>		m_EditConfirmPassword;
	
	private TextField<String>		m_AddUsername;
	private TextField<String>		m_AddFirstName;
	private TextField<String>		m_AddLastName;
	private ComboBox<BaseModelData> m_AddRoleCombo;
	private TextField<String>		m_AddPassword;
	private TextField<String>		m_AddConfirmPassword;
	
	private UserModel	m_User;
	protected final static String	ROLE_NAME_PROPERTY = "rolename";
	
	
	/**
	 * Creates a new panel for viewing and editing the details of a Prelert user.
	 */
	public UserEditor()
	{
		m_QueryService = AdminServiceLocator.getInstance().getUserQueryService();
		
		m_RoleList = new ListStore<BaseModelData>();
		
		// Run the query for the to populate the list of role names.
		ApplicationResponseHandler<List<String>> callback = 
			new ApplicationResponseHandler<List<String>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading list of user roles", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						AdminModule.ADMIN_MESSAGES.errorLoadingUserData(), null);
			}


			@Override
            public void uponSuccess(List<String> roleNames)
			{
				BaseModelData roleData;
				for (String roleName : roleNames)
				{
					roleData = new BaseModelData();
					roleData.set(ROLE_NAME_PROPERTY, roleName);
					m_RoleList.add(roleData);
				}
			}
		};
		m_QueryService.getRoles(callback);
		
		m_CardLayout = new CardLayout();
		setLayout(m_CardLayout);
		
		m_EditForm = createEditForm();
		add(m_EditForm);
		
		m_CardLayout.setActiveItem(m_EditForm);
	}
	
	
	/**
	 * Creates the components for the EDIT mode.
	 * @return content panel containing the edit fields.
	 */
	protected ContentPanel createEditForm()
	{
		AdminMessages messages = AdminModule.ADMIN_MESSAGES;
		
		ContentPanel editPanel = new ContentPanel();
		editPanel.setHeading(messages.userDetailsHeading(""));
		editPanel.setBodyStyleName("x-border-layout-ct");
		
		VBoxLayout panelLayout = new VBoxLayout();    
        panelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.CENTER);   
        panelLayout.setPadding(new Padding(40));
        editPanel.setLayout(panelLayout); 
        
        int fieldSetWidth = 390;
        int formWidth = 380;
        int labelWidth = 140;
        FormData formData = new FormData();   
		formData.setWidth(200);
        
		
		// Add the 'User Details' fields for editing the 
		// username, first name, last name and role.
        FieldSet detailsFieldSet = new FieldSet();   
		detailsFieldSet.setHeading(messages.userDetails());
		detailsFieldSet.setWidth(fieldSetWidth);
		
		FormPanel detailsForm = new FormPanel();
		detailsForm.setHeaderVisible(false);
		detailsForm.setBodyBorder(false);
		detailsForm.setWidth(formWidth);
		detailsForm.setPadding(5);
		
		FormLayout detailsLayout = new FormLayout();   
		detailsLayout.setLabelWidth(labelWidth);  
		detailsForm.setLayout(detailsLayout); 
	    
	    detailsFieldSet.add(detailsForm);
		
	    m_EditUsername = new TextField<String>();   
	    m_EditUsername.setFieldLabel(messages.username());   
	    m_EditUsername.setAllowBlank(false);   
	    detailsForm.add(m_EditUsername, formData); 
	    
	    m_EditFirstName = new TextField<String>();   
	    m_EditFirstName.setFieldLabel(messages.firstName());   
	    m_EditFirstName.setAllowBlank(false);   
	    detailsForm.add(m_EditFirstName, formData); 
	    
	    m_EditLastName = new TextField<String>();   
	    m_EditLastName.setFieldLabel(messages.lastName());   
	    m_EditLastName.setAllowBlank(false);   
	    detailsForm.add(m_EditLastName, formData); 
	    
	    m_EditRoleCombo = new ComboBox<BaseModelData>();
	    m_EditRoleCombo.setFieldLabel(messages.role());
	    m_EditRoleCombo.setStore(m_RoleList);
	    m_EditRoleCombo.setEditable(false);
	    m_EditRoleCombo.setDisplayField(ROLE_NAME_PROPERTY);
	    m_EditRoleCombo.setTriggerAction(TriggerAction.ALL); 
	    detailsForm.add(m_EditRoleCombo, formData);
	    
	    // Add save and cancel buttons.
	    Button saveButton = new Button(ClientUtil.CLIENT_CONSTANTS.save());
	    saveButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				saveEdit();
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
	    
	    detailsForm.setButtonAlign(HorizontalAlignment.CENTER);   
	    detailsForm.addButton(saveButton);   
	    detailsForm.addButton(cancelButton);   

	    // Add a FormButtonBinding to disable the Save button if fields are blank.
	    FormButtonBinding binding = new FormButtonBinding(detailsForm);   
	    binding.addButton(saveButton); 

		
	    // Add the 'Change Password' fields for editing user's password.
	    FieldSet passwordFieldSet = new FieldSet();   
		passwordFieldSet.setHeading(ClientUtil.CLIENT_CONSTANTS.changePassword());
		passwordFieldSet.setCollapsible(true);
		passwordFieldSet.setWidth(fieldSetWidth);
		
		FormPanel passwordForm = new FormPanel();
		passwordForm.setHeaderVisible(false);
		passwordForm.setBodyBorder(false);
		passwordForm.setWidth(formWidth);
		passwordForm.setPadding(5);
		
		FormLayout passwordLayout = new FormLayout();   
		passwordLayout.setLabelWidth(labelWidth);  
		passwordForm.setLayout(passwordLayout); 
		
		passwordFieldSet.add(passwordForm);

		
		m_EditPassword = new TextField<String>();   
		m_EditPassword.setPassword(true);
		m_EditPassword.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordNew());    
		m_EditPassword.setAllowBlank(false);
		passwordForm.add(m_EditPassword, formData); 
	    
		m_EditConfirmPassword = new TextField<String>();   
		m_EditConfirmPassword.setPassword(true);
		m_EditConfirmPassword.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordNewConfirm());
		m_EditConfirmPassword.setAllowBlank(false);
	    passwordForm.add(m_EditConfirmPassword, formData); 
	    
	    // Add a save button.
	    Button pwdSaveButton = new Button(ClientUtil.CLIENT_CONSTANTS.save());   
	    passwordForm.setButtonAlign(HorizontalAlignment.CENTER);   
	    passwordForm.addButton(pwdSaveButton);   
	    pwdSaveButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				savePasswordEdit();
            }
		});
	    
	    // Add a FormButtonBinding to disable the Save button if fields are blank.
	    FormButtonBinding pwdBinding = new FormButtonBinding(passwordForm);   
	    pwdBinding.addButton(pwdSaveButton); 
	    
        editPanel.add(detailsFieldSet);   
        editPanel.add(passwordFieldSet, new VBoxLayoutData(new Margins(30, 0, 0, 0)));     
		
		return editPanel;
	}
	
	
	/**
	 * Creates the components for the CREATE mode.
	 * @return content panel containing the fields for adding a new user.
	 */
	protected ContentPanel createAddForm()
	{
		AdminMessages messages = AdminModule.ADMIN_MESSAGES;
		
		ContentPanel addPanel = new ContentPanel();
		addPanel.setHeading(messages.addNewUserHeading());
		addPanel.setBodyStyleName("x-border-layout-ct");
		
		VBoxLayout panelLayout = new VBoxLayout();    
        panelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.CENTER);   
        panelLayout.setPadding(new Padding(40));
        addPanel.setLayout(panelLayout); 
        
        int fieldSetWidth = 390;
        int formWidth = 380;
        int labelWidth = 140;
        FormData formData = new FormData();   
		formData.setWidth(200);
        
		
		// Add the 'User Details' fields for entering the 
		// username, first name, last name, role and password.
        FieldSet detailsFieldSet = new FieldSet();   
		detailsFieldSet.setHeading(messages.userDetails());
		detailsFieldSet.setWidth(fieldSetWidth);
		
		FormPanel detailsForm = new FormPanel();
		detailsForm.setHeaderVisible(false);
		detailsForm.setBodyBorder(false);
		detailsForm.setWidth(formWidth);
		detailsForm.setPadding(5);
		
		FormLayout detailsLayout = new FormLayout();   
		detailsLayout.setLabelWidth(labelWidth);  
		detailsForm.setLayout(detailsLayout); 
	    
	    detailsFieldSet.add(detailsForm);
		
	    m_AddUsername = new TextField<String>();   
	    m_AddUsername.setFieldLabel(messages.username());   
	    m_AddUsername.setAllowBlank(false);   
	    detailsForm.add(m_AddUsername, formData); 
	    
	    m_AddFirstName = new TextField<String>();   
	    m_AddFirstName.setFieldLabel(messages.firstName());   
	    m_AddFirstName.setAllowBlank(false);   
	    detailsForm.add(m_AddFirstName, formData); 
	    
	    m_AddLastName = new TextField<String>();   
	    m_AddLastName.setFieldLabel(messages.lastName());   
	    m_AddLastName.setAllowBlank(false);   
	    detailsForm.add(m_AddLastName, formData); 
	    
	    m_AddRoleCombo = new ComboBox<BaseModelData>();
	    m_AddRoleCombo.setFieldLabel(messages.role());
	    m_AddRoleCombo.setStore(m_RoleList);
	    m_AddRoleCombo.setEditable(false);
	    m_AddRoleCombo.setAllowBlank(false);
	    m_AddRoleCombo.setDisplayField(ROLE_NAME_PROPERTY);
	    m_AddRoleCombo.setTriggerAction(TriggerAction.ALL); 
	    detailsForm.add(m_AddRoleCombo, formData);
	    
		m_AddPassword = new TextField<String>();   
		m_AddPassword.setPassword(true);
		m_AddPassword.setFieldLabel(ClientUtil.CLIENT_CONSTANTS.passwordNew());   
		m_AddPassword.setAllowBlank(false);
		detailsForm.add(m_AddPassword, formData); 
	    
	    m_AddConfirmPassword = new TextField<String>();   
	    m_AddConfirmPassword.setPassword(true);
	    m_AddConfirmPassword.setFieldLabel(
	    		ClientUtil.CLIENT_CONSTANTS.passwordNewConfirm());  
	    m_AddConfirmPassword.setAllowBlank(false);
	    detailsForm.add(m_AddConfirmPassword, formData); 
	    
	    
	    // Add save and cancel buttons.
	    Button saveButton = new Button(ClientUtil.CLIENT_CONSTANTS.save());
	    saveButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				saveNew();
            }
		});
	    
	    // Add a FormButtonBinding to disable the Save button if fields are blank.
	    FormButtonBinding binding = new FormButtonBinding(detailsForm);   
	    binding.addButton(saveButton);
	    
	    Button cancelButton = new Button(ClientUtil.CLIENT_CONSTANTS.cancel());
	    cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				cancelEdit();
            }
		});
	    
	    
	    detailsForm.setButtonAlign(HorizontalAlignment.CENTER);   
	    detailsForm.addButton(saveButton);   
	    detailsForm.addButton(cancelButton);   
	    
        addPanel.add(detailsFieldSet);   
		
		return addPanel;
	}
	
	
	/**
	 * Sets the user for editing in the panel.
	 * @param user the user to edit.
	 * @param mode the editing mode - {@link EditingMode#CREATE} for a new user,
	 * 	or {@link EditingMode#EDIT} for an existing user.
	 */
	public void setUser(UserModel user, EditingMode mode)
	{
		m_User = user;
		
		setEditingMode(mode);
		
		switch (mode)
		{
			case EDIT:
				m_EditForm.setHeading(AdminModule.ADMIN_MESSAGES.userDetailsHeading(
						m_User.getUsername()));
				m_EditUsername.setValue(m_User.getUsername());
				m_EditFirstName.setValue(m_User.getFirstName());
				m_EditLastName.setValue(m_User.getLastName());
				
				BaseModelData role = m_RoleList.findModel(ROLE_NAME_PROPERTY, m_User.getRoleName());
				if (role != null)
				{
					m_EditRoleCombo.setValue(role);
				}
				else
				{
					m_EditRoleCombo.setValue(m_RoleList.getAt(0));
				}
				
				// Prevent user editing their own username or role.
				String loggedInUsername = ClientUtil.getLoggedInUsername();
				boolean editingSelf = m_User.getUsername().equals(loggedInUsername);
				m_EditUsername.setEnabled(!editingSelf);
				m_EditRoleCombo.setEnabled(!editingSelf);
				
				// Clear any existing entries in the Change Password fields.
				m_EditPassword.clear();
				m_EditConfirmPassword.clear();
				break;
			
			case CREATE:
				m_AddUsername.clear();
				m_AddFirstName.clear();
				m_AddLastName.clear();
				m_AddRoleCombo.clearSelections();
				m_AddPassword.clear();
				m_AddConfirmPassword.clear();
				break;
		}
	}
	
	
	/**
	 * Saves changes to an existing user to the server.
	 */
	protected void saveEdit()
	{
		UserModel edit = new UserModel();
		edit.setUsername(m_EditUsername.getValue());
		edit.setFirstName(m_EditFirstName.getValue());
		edit.setLastName(m_EditLastName.getValue());
		
		BaseModelData roleData = m_EditRoleCombo.getValue();
		String rolename = roleData.get(ROLE_NAME_PROPERTY);
		edit.setRoleName(rolename);
		
		m_QueryService.editUser(m_User.getUsername(), edit, new UserEditCallback(edit));
	}
	
	
	/**
	 * Saves an edit to an existing user's password.
	 */
	protected void savePasswordEdit()
	{
		if (validatePassword(m_EditPassword, m_EditConfirmPassword) == true)
		{
			m_QueryService.setPassword(m_User.getUsername(), m_EditPassword.getValue(), 
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
					if (result == UserQueryService.STATUS_SUCCESS)
					{
						MessageBox.info(ClientUtil.CLIENT_CONSTANTS.changePassword(), 
								ClientUtil.CLIENT_CONSTANTS.passwordChangedSuccess(m_User.getUsername()), null);
					}
					else
					{
						GWT.log("Error changing user password, status code=" + result);
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
								ClientUtil.CLIENT_CONSTANTS.errorSavingPassword(), null);
					}
                }
				
			});
		}
	}
	
	
	/**
	 * Saves a new user to the server.
	 */
	protected void saveNew()
	{
		if (validatePassword(m_AddPassword, m_AddConfirmPassword) == true)
		{
			UserModel newUser = new UserModel();
			newUser.setUsername(m_AddUsername.getValue());
			newUser.setFirstName(m_AddFirstName.getValue());
			newUser.setLastName(m_AddLastName.getValue());
			newUser.setPassword(m_AddPassword.getValue());
			
			BaseModelData roleData = m_AddRoleCombo.getValue();
			String rolename = roleData.get(ROLE_NAME_PROPERTY);
			newUser.setRoleName(rolename);
			
			m_QueryService.addUser(newUser, new UserEditCallback(newUser));
		}
	}
	
	
	/**
	 * Cancels the current edit, and reverts the data in the form to the previous
	 * saved values.
	 */
	protected void cancelEdit()
	{
		setUser(m_User, getEditingMode());
		
		UserEditorEvent event = new UserEditorEvent(UserEditor.this);
		event.setStartData(m_User);
		fireEvent(Events.CancelEdit, event);
	}
	
	
	/**
	 * Validates the entry in the password fields prior to a save operation.
	 * @param passwordField password field whose value is being validated.
	 * @param confirmField password confirmation field.
	 * @return <code>true</code> if the password entry is valid, 
	 * <code>false</code> otherwise.
	 */
	protected boolean validatePassword(
			final TextField<String> passwordField, final TextField<String> confirmField)
	{
		// Just check that the values match.
		// Check that the password fields match.
		String password = passwordField.getValue();
		boolean valid = password.equals(confirmField.getValue());
		if (valid == false)
		{
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.changePassword(), 
					ClientUtil.CLIENT_CONSTANTS.passwordsDoNotMatch(), 
					new Listener<MessageBoxEvent>(){

						@Override
                        public void handleEvent(MessageBoxEvent be)
                        {
							passwordField.focus();
                        }
				
			});
			
			passwordField.clear();
			confirmField.clear();
		}
		
		return valid;
	}
	
	
	/**
	 * Sets the editing mode of the panel, switching to the 'edit' or 'add' form.
	 * @param the editing mode - {@link EditingMode#CREATE} for a new user,
	 * 	or {@link EditingMode#EDIT} for an existing user.
	 */
	protected void setEditingMode(EditingMode mode)
	{
		switch (mode)
		{
			case EDIT:
				m_CardLayout.setActiveItem(m_EditForm);
				break;
				
			case CREATE:
				if (m_AddForm == null)
				{
					m_AddForm = createAddForm();
					add(m_AddForm);
				}
				m_CardLayout.setActiveItem(m_AddForm);
				break;
		}
	}
	
	
	/**
	 * Returns the current editing mode of the user editor.
	 * @return the editing mode - {@link EditingMode#CREATE} if a new user is be
	 * 	created, or {@link EditingMode#EDIT} for editing an existing user.
	 */
	protected EditingMode getEditingMode()
	{
		if (m_CardLayout.getActiveItem() == m_EditForm)
		{
			return EditingMode.EDIT;
		}
		else
		{
			return EditingMode.CREATE;
		}
	}
	
	
	/**
	 * Response handler to check for the success or failure of user edits.
	 */
	class UserEditCallback extends ApplicationResponseHandler<Integer>
	{
		private UserModel m_Edit;
		
		public UserEditCallback(UserModel edit)
		{
			m_Edit = edit;
		}
		

		@Override
        public void uponFailure(Throwable caught)
        {
			GWT.log("Error saving user data.", caught);
			
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					AdminModule.ADMIN_MESSAGES.errorSavingUserData(), null);
        }
		

		@Override
        public void uponSuccess(Integer result)
        {
			GWT.log("UserEditCallback - uponSuccess(" + result + ")");
			
			if (result == UserQueryService.STATUS_SUCCESS)
			{
				UserEditorEvent event = new UserEditorEvent(UserEditor.this);
				event.setStartData(m_User);
				event.setUserData(m_Edit);
				fireEvent(Events.AfterEdit, event);
				
				// Update the reference to the current user details.
				m_User = m_Edit;	
			}
			else
			{
				GWT.log("Error saving user data, status code=" + result);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						AdminModule.ADMIN_MESSAGES.errorSavingUserData(), null);
				
				EditingMode mode = getEditingMode();
				if (mode == EditingMode.EDIT)
				{
					// Put original data back in form.
					setUser(m_User, getEditingMode());
				}
			}
        }
		
	}
	
}
