/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2011     *
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
	@Source("icon_table.png") 
	ImageResource icon_table(); 
	
	@Source("logo_transp.png") 
	ImageResource logo(); 
	
	@Source("pause.png") 
	ImageResource toolbar_pause(); 
	
	@Source("play.png") 
	ImageResource toolbar_play();
	
}
