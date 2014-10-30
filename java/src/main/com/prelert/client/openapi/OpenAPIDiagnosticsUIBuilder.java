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

import com.prelert.client.diagnostics.DiagnosticsConfigModuleComponent;
import com.prelert.client.diagnostics.DiagnosticsUIBuilder;


/**
 * GWT entry point for the Prelert Diagnostics for OpenAPI UI. It creates 
 * a Run configuration page specific to the OpenAPI product.
 * @author Pete Harverson
 */
public class OpenAPIDiagnosticsUIBuilder extends DiagnosticsUIBuilder
{

	@Override
    protected DiagnosticsConfigModuleComponent addConfigModule()
    {
		OpenAPIConfigModule configModule = new OpenAPIConfigModule();
		
		m_ModulePanel.add(configModule.getComponent());
		m_Modules.put(configModule.getModuleId(), configModule);
		m_NavigationBar.insertModuleLink(configModule.getModuleId(), 
				DIAGNOSTIC_MESSAGES.configModuleLabel(), 1);
		
		return configModule;
    }

}
