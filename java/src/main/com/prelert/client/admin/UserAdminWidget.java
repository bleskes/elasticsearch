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

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.admin.UserEditor.EditingMode;
import com.prelert.client.event.UserEditorEvent;
import com.prelert.data.gxt.UserModel;
import com.prelert.service.UserQueryService;
import com.prelert.service.UserQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for the administration of Prelert users. The widget is
 * divided into two sections, with the left hand side displaying a list of the
 * users added to Prelert, and the right hand side a form for viewing and
 * editing the details of the selected user.
 *
 * @author Pete Harverson
 */
public class UserAdminWidget extends LayoutContainer
{
	private UserQueryServiceAsync	m_QueryService;
	
	private String					m_LoggedInUsername;
	
	private UserPagingLoader		m_ListLoader;
	private ListStore<UserModel> 	m_ListStore;
	private Grid<UserModel> 		m_UserGrid;
	private Button					m_DeleteBtn;
	private int						m_PageSize = 20;
	
	private UserModel				m_LastSelected;
	
	private UserEditor				m_UserEditor;
	
	
	/**
	 * Creates a new widget for the administration of Prelert users. The widget
	 * consists of a list of the users, and a form for viewing and editing user
	 * details.
	 */
	public UserAdminWidget()
	{
		m_QueryService = AdminServiceLocator.getInstance().getUserQueryService();
		
		m_LoggedInUsername = ClientUtil.getLoggedInUsername();
		
		initComponents();
		
		loadUsers();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	    
	    // Create the User List.
	    ContentPanel listPanel = createListPanel();
	    
	    // Create the form for editing users.
	    // List for events firing after an edit.
	    m_UserEditor = new UserEditor();
	    m_UserEditor.addListener(Events.AfterEdit, new Listener<UserEditorEvent>(){

			@Override
            public void handleEvent(UserEditorEvent be)
            {
	            // Page the edited user into view.
				// - may be a new user, or username may have been edited.
				m_LastSelected = be.getUserData();
				if (m_LastSelected != null)
				{
					m_ListLoader.loadPageWithUser(m_LastSelected.getUsername());
				}
				else
				{
					loadUsers();
				}
            }
	    });
	    
	    m_UserEditor.addListener(Events.CancelEdit, new Listener<UserEditorEvent>(){

			@Override
            public void handleEvent(UserEditorEvent be)
            {
	            if (m_LastSelected != null)
	            {
	            	m_UserGrid.getSelectionModel().select(m_LastSelected, false);
	            }
            }
	    });
	    
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    BorderLayoutData eastData = new BorderLayoutData(LayoutRegion.EAST, 0.4f, 300, 800); 
	    eastData.setSplit(true);   
	    eastData.setFloatable(false);   
	    eastData.setMargins(new Margins(0, 0, 0, 5)); 
	    
	    add(listPanel, centerData);   
        add(m_UserEditor, eastData); 
	}
	
	
	/**
	 * Creates the components for the User List.
	 * @return the ContentPanel holding the user list.
	 */
	protected ContentPanel createListPanel()
	{
		final AdminMessages messages = AdminModule.ADMIN_MESSAGES;
		
		ContentPanel listPanel = new ContentPanel();
		listPanel.setLayout(new FitLayout());
	    listPanel.setHeading(messages.userListHeading());
	    
	    // Create the RpcProxy and PagingLoader to populate the list.
	    UserPagingRpcProxy proxy = new UserPagingRpcProxy();
		m_ListLoader = new UserPagingLoader(proxy);
	    m_ListLoader.setRemoteSort(true);
	    m_ListLoader.addLoadListener(new LoadListener(){

            @Override
            public void loaderLoadException(LoadEvent le)
            {
            	GWT.log("UserAdminWidget list loaderLoadException: " + le.exception);
				
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						messages.errorLoadingUserData(), null);
            }
	    	
	    });
	  
	    m_ListStore = new ListStore<UserModel>(m_ListLoader);  
	    m_ListStore.addStoreListener(new StoreListener<UserModel>(){

            @Override
            public void storeDataChanged(StoreEvent<UserModel> se)
            {
            	String selectUsername = null;
	            if (m_LastSelected != null)
	            {
	            	selectUsername = m_LastSelected.getUsername();
	            }
	            
	            selectInList(selectUsername);
            }
	    	
	    });
	  
	    // Create the paging toolbar, and add buttons for adding and deleting users.
	    PagingToolBar toolbar = new PagingToolBar(m_PageSize){

            @Override
            public void setEnabled(boolean enabled)
            {
	            // Override setEnabled() to ensure the delete button is disabled
            	// if the logged in user is selected after the grid reloads.
	            super.setEnabled(enabled);
	            
	            if (enabled == true)
	            {
		            boolean enableDelete = false;
		            UserModel selectedUser = m_UserGrid.getSelectionModel().getSelectedItem();
		            if (selectedUser != null)
		            {
		            	enableDelete = !(selectedUser.getUsername().equals(m_LoggedInUsername));
		            }
		        	m_DeleteBtn.setEnabled(enableDelete);
	            }
            }
	    	
	    };   
	    
	    int itemCount = toolbar.getItemCount();
	    toolbar.insert(new SeparatorToolItem(), itemCount-2);	// Before fill and displayText.
	    
	    Button addBtn = new Button(messages.add());
	    addBtn.setIcon(AbstractImagePrototype.create(
	    		ClientUtil.CLIENT_IMAGES.toolbar_user_add()));
	    addBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				m_UserEditor.setUser(new UserModel(), EditingMode.CREATE);
				GWT.log("addBtn deselectAll()");
				m_UserGrid.getSelectionModel().deselectAll();
            }
		});
	    toolbar.insert(addBtn, itemCount-1);
	    
	    m_DeleteBtn = new Button(messages.delete());
	    m_DeleteBtn.setIcon(AbstractImagePrototype.create(
	    		ClientUtil.CLIENT_IMAGES.toolbar_user_delete()));
	    toolbar.insert(m_DeleteBtn, itemCount);
	    m_DeleteBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				UserModel selectedUser = m_UserGrid.getSelectionModel().getSelectedItem();
				if (selectedUser != null)
				{	
					if (selectedUser.getUsername().equals(m_LoggedInUsername) == false)
					{
						confirmDeleteUser(selectedUser.getUsername());
					}
					else
					{
						// Delete button should be disabled if own user is selected.
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
								messages.userCannotDeleteSelf(), null);
					}
				}
            }
		});
	    
	    toolbar.bind(m_ListLoader);  
	    listPanel.setTopComponent(toolbar);
	  
	    
	    // Set up the columns for the grid.
	    List<ColumnConfig> columnConfig = new ArrayList<ColumnConfig>();   
	  
	    ColumnConfig userNameColumn = new ColumnConfig("username", 
	    		messages.username(), 150);
	    ColumnConfig firstNameColumn = new ColumnConfig("firstName", 
	    		messages.firstName(), 150);
	    ColumnConfig lastNameColumn = new ColumnConfig("lastName", 
	    		messages.lastName(), 150);
	    ColumnConfig roleNameColumn = new ColumnConfig("roleName", 
	    		messages.role(), 150);

	    columnConfig.add(userNameColumn);
	    columnConfig.add(firstNameColumn);
	    columnConfig.add(lastNameColumn);
	    columnConfig.add(roleNameColumn);
	  
	    ColumnModel columnModel = new ColumnModel(columnConfig);   
	  
	    m_UserGrid = new Grid<UserModel>(m_ListStore, columnModel);   
	    m_UserGrid.setAutoExpandColumn("username");   
	    m_UserGrid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
	    m_UserGrid.getSelectionModel().addSelectionChangedListener(
	    		new SelectionChangedListener<UserModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<UserModel> se)
            {
				UserModel selectedUser = se.getSelectedItem();
				boolean enableDelete = false;
				if (selectedUser != null)
				{
					m_UserEditor.setUser(selectedUser, EditingMode.EDIT);
					m_LastSelected = selectedUser;
					enableDelete = !(selectedUser.getUsername().equals(m_LoggedInUsername));
				}
				
				m_DeleteBtn.setEnabled(enableDelete);
            }
	    });
	    
	    listPanel.add(m_UserGrid);   
	    
	    return listPanel;
	}
	
	
	/**
	 * Loads the list of users.
	 */
	public void loadUsers()
	{
		m_ListLoader.load();
	}
	
	
	/**
	 * Selects a user in the list.
	 * @param username username of the user to select.
	 */
	public void selectInList(String username)
	{
		UserModel user = null;
		if (username != null)
		{
			user = m_ListStore.findModel("username", username);
		}
		
		if (user != null)
		{
			m_UserGrid.getSelectionModel().select(user, false);
		}
		else
		{
			m_UserGrid.getSelectionModel().select(0, false);
		}
	}
	
	
	/**
	 * Pops up a confirmation dialog to the user, asking for confirmation that the
	 * specified should be deleted, and if so, then proceeds to delete the user.
	 * @param username username of user to be deleted.
	 */
	protected void confirmDeleteUser(String username)
	{
		final String finalUsername = username;
		
		MessageBox.confirm(AdminModule.ADMIN_MESSAGES.deleteConfirm(), 
				AdminModule.ADMIN_MESSAGES.userDeleteConfirm(username), 
				new Listener<MessageBoxEvent>(){

				@Override
                public void handleEvent(MessageBoxEvent be)
                {
					if (be.getButtonClicked().getItemId() == Dialog.YES)
					{
						deleteUser(finalUsername);
					}
                }
		});
	}
	
	
	/**
	 * Fires off a request to the server to delete the specified user.
	 * @param username username username of user to be deleted.
	 */
	protected void deleteUser(String username)
	{	
		m_QueryService.deleteUser(username, new ApplicationResponseHandler<Integer>(){
			
			AdminMessages messages = AdminModule.ADMIN_MESSAGES;

			@Override
            public void uponFailure(Throwable caught)
            {
				GWT.log("Error deleting user.", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						messages.errorDeletingUser(), null);
            }

			@Override
            public void uponSuccess(Integer result)
            {
				if (result == UserQueryService.STATUS_SUCCESS)
				{
					loadUsers();
				}
				else
				{
					GWT.log("Error deleting user, status code=" + result);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
							messages.errorDeletingUser(), null);
				}
            }
			
		});
	}
	
}
