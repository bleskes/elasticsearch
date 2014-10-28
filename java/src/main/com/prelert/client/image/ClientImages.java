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

package com.prelert.client.image;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;


/**
 * ClientBundle containing images and icons for use in client applications.
 * @author Pete Harverson
 */
public interface ClientImages extends ClientBundle
{
	@Source("folder_closed14.png") 
	ImageResource icon_folder_closed(); 
	
	@Source("folder_up.png") 
	ImageResource icon_folder_up(); 
	
	@Source("folder_up_disabled.png") 
	ImageResource icon_folder_up_disabled(); 
	
	@Source("information.png") 
	ImageResource icon_info_large(); 
	
	@Source("icon_notification.png") 
	ImageResource icon_notification(); 
	
	@Source("play_32.png") 
	ImageResource icon_play_large(); 
	
	@Source("search_32.png") 
	ImageResource icon_search_large(); 
	
	@Source("icon_timeseries.png") 
	ImageResource icon_time_series(); 
	
	@Source("icon_table14.png") 
	ImageResource icon_table(); 
	
	@Source("table_info_32.png") 
	ImageResource icon_table_info_large(); 
	
	@Source("user-blue.png") 
	ImageResource icon_user_large(); 
	
	@Source("video.png") 
	ImageResource icon_video_large(); 
	
	@Source("logo_black2012.png") 
	ImageResource logo();
	
	@Source("table-delete.png") 
	ImageResource toolbar_chart_clear(); 
	
	@Source("table-go.png") 
	ImageResource toolbar_explore(); 
	
	@Source("export.png") 
	ImageResource toolbar_export(); 
	
	@Source("arrow-left1.png") 
	ImageResource toolbar_pan_left();
	
	@Source("arrow-right1.png") 
	ImageResource toolbar_pan_right(); 
	
	@Source("pause.png") 
	ImageResource toolbar_pause(); 
	
	@Source("play.png") 
	ImageResource toolbar_play();
	
	@Source("scale_to_fit.png") 
	ImageResource toolbar_scale_to_fit();
	
	@Source("search.png") 
	ImageResource toolbar_search();
	
	@Source("user1-add.png") 
	ImageResource toolbar_user_add(); 
	
	@Source("user1-delete.png") 
	ImageResource toolbar_user_delete();
	
	@Source("zoom-in.png") 
	ImageResource toolbar_zoom_in(); 
	
	@Source("zoom-out.png") 
	ImageResource toolbar_zoom_out(); 
}
