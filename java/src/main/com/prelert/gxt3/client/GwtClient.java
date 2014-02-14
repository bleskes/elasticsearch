package com.prelert.gxt3.client;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor.Path;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.ui.RootPanel;

import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.client.loader.HttpProxy;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;
import com.sencha.gxt.data.shared.TreeStore;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.treegrid.TreeGrid;
/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class GwtClient implements EntryPoint 
{
	  public interface DataProperties extends PropertyAccess<TreeData>
	  {
		    @Path("name")
		    ModelKeyProvider<TreeData> key();
		    ValueProvider<TreeData, String> name();
		    ValueProvider<TreeData, String> isEnabled();
	  }
	  
	  public class TreeData
	  {
		  public String type = "";
		  public boolean isLeaf = false;
		  public boolean isEnabled = false;
		  public String stanza = "";
		  
		  public List<TreeData> children;
		  
		  public TreeData(String type, boolean isEnabled)
		  {
			  this.type = type;
			  this.isEnabled = isEnabled;
		  }
		  
		  public String getType()
		  {
			  return type;
		  }
		  
		  public void setType(String type)
		  {
			  this.type =  type;
		  }
		  
		  public boolean isEnabled()
		  {
			  return isEnabled;
		  }
		  
		  public void setIsEnabled(boolean isEnabled)
		  {
			  this.isEnabled = isEnabled;
		  }
	  }
	  
	  
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() 
	{
		// Build a http proxy
		//String url = GWT.getModuleBaseURL() + "read";
		String url = "http://localhost:8000/en-GB/custom/prelert/prelertsetup/read";

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
		HttpProxy<TreeData> proxy = new HttpProxy<TreeData>(builder);
		
		
		
	    // Setup the tree
	    DataProperties dp = GWT.create(DataProperties.class);
	    TreeStore<TreeData> store = new TreeStore<TreeData>(dp.key());
//	    TreeData r1 = new TreeData("One", "true");
//	    store.add(r1);
//	    store.add(r1, new TreeData("One A", "true"));
//	    store.add(r1, new TreeData("One B", "false"));
	    
	    // Create the configurations for each column in the tree grid
	    List<ColumnConfig<TreeData, ?>> ccs = new LinkedList<ColumnConfig<TreeData, ?>>();
	    ccs.add(new ColumnConfig<TreeData, String>(dp.name(), 200, "Name"));
	    ccs.add(new ColumnConfig<TreeData, String>(dp.isEnabled(), 200, "IsEnabled"));
	    ColumnModel<TreeData> cm = new ColumnModel<TreeData>(ccs);

	    TreeGrid<TreeData> treegrid = new TreeGrid<TreeData>(store, cm, ccs.get(0));

	    RootPanel.get("setup-tree").add(treegrid);
	}
}
