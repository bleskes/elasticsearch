/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 ************************************************************/

package com.prelert.rs.client;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.SingleDocument;

public class CategoryDefinitionRequestBuilder extends BaseJobRequestBuilder<CategoryDefinition>
{
    private final String m_CategoryId;

    /**
     * @param client The engine API client
     * @param jobId The Job's unique Id
     */
    public CategoryDefinitionRequestBuilder(EngineApiClient client, String jobId, String categoryId)
    {
        super(client, jobId);
        m_CategoryId = categoryId;
    }

    /**
     * Returns a single document with the category definition that was requested
     *
     * @return A {@link SingleDocument} object containing the requested {@link CategoryDefinition}
     * object
     * @throws IOException
     */
    public SingleDocument<CategoryDefinition> get() throws IOException
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl()).append("/results/").append(jobId()).append("/categorydefinitions/")
                .append(m_CategoryId);
        return createHttpGetRequester().getSingleDocument(url.toString(),
                new TypeReference<SingleDocument<CategoryDefinition>>() {});
    }
}
