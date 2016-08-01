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

package com.prelert.master.rs.resources;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.prelert.rs.provider.AcknowledgementWriter;
import com.prelert.rs.provider.DataUploadExceptionMapper;
import com.prelert.rs.provider.JobConfigurationMessageBodyReader;
import com.prelert.rs.provider.JobExceptionMapper;

public class MasterApiWebApp extends Application
{
    private Set<Class<?>> m_ResourceClasses;

    public MasterApiWebApp()
    {
        m_ResourceClasses = new HashSet<>();
        addEndPoints();
        addMessageReaders();
        addExceptionMappers();
        addMessageWriters();
    }

    private void addEndPoints()
    {
        m_ResourceClasses.add(Jobs.class);
        m_ResourceClasses.add(Data.class);
        m_ResourceClasses.add(Status.class);
    }

    private void addMessageReaders()
    {
        m_ResourceClasses.add(JobConfigurationMessageBodyReader.class);
    }

    private void addExceptionMappers()
    {
        m_ResourceClasses.add(JobExceptionMapper.class);
        m_ResourceClasses.add(DataUploadExceptionMapper.class);
    }

    private void addMessageWriters()
    {
        m_ResourceClasses.add(AcknowledgementWriter.class);
    }


    @Override
    public Set<Class<?>> getClasses()
    {
        return m_ResourceClasses;
    }
}
