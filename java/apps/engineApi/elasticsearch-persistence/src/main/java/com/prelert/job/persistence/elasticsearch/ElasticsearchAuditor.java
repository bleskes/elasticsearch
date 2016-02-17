/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.job.persistence.elasticsearch;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.audit.AuditMessage;
import com.prelert.job.audit.Auditor;

public class ElasticsearchAuditor implements Auditor
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchAuditor.class);

    private final Client m_Client;
    private final String m_Index;
    private final String m_JobId;
    private final ObjectMapper m_ObjectMapper;

    public ElasticsearchAuditor(Client client, String index, String jobId)
    {
        m_Client = Objects.requireNonNull(client);
        m_Index = index;
        m_JobId = jobId;
        m_ObjectMapper = new ObjectMapper();
    }

    @Override
    public void info(String message)
    {
        persistAuditMessage(AuditMessage.newInfo(m_JobId, message));
    }

    @Override
    public void warning(String message)
    {
        persistAuditMessage(AuditMessage.newWarning(m_JobId, message));
    }

    @Override
    public void error(String message)
    {
        persistAuditMessage(AuditMessage.newError(m_JobId, message));
    }

    private void persistAuditMessage(AuditMessage message)
    {
        try
        {
            String json = m_ObjectMapper.writeValueAsString(message);
            m_Client.prepareIndex(m_Index, AuditMessage.TYPE)
                    .setSource(json)
                    .execute().actionGet();
        }
        catch (JsonProcessingException | IndexNotFoundException e)
        {
            LOGGER.error("Error writing auditMessage", e);
            return;
        }
    }
}
