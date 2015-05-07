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
import java.util.Collections;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

class HttpGetRequester<T>
{
    private static final Logger LOGGER = Logger.getLogger(HttpGetRequester.class);

    private final EngineApiClient m_Client;

    public HttpGetRequester(EngineApiClient client)
    {
        m_Client = client;
    }

    protected Pagination<T> getPage(String fullUrl, TypeReference<Pagination<T>> typeRef)
            throws IOException
    {
        LOGGER.debug("GET " + fullUrl);

        Pagination<T> page = m_Client.get(fullUrl, typeRef);

        // else return empty page
        if (page == null)
        {
            page = new Pagination<>();
            page.setDocuments(Collections.emptyList());
        }

        return page;
    }

    protected SingleDocument<T> getSingleDocument(String fullUrl,
            TypeReference<SingleDocument<T>> typeRef) throws IOException
    {
        LOGGER.debug("GET " + fullUrl);

        SingleDocument<T> doc = m_Client.get(fullUrl, typeRef);

        // else return empty doc
        if (doc == null)
        {
            doc = new SingleDocument<>();
        }
        return doc;
    }
}
