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
package com.prelert.job.manager;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

import com.prelert.job.config.verification.JobConfigurationVerifier;

public class JobFactoryTest {

    @Test
    public void testGenerateJobId_doesnotIncludeHost()
    {
        Pattern pattern = Pattern.compile("[0-9]{14}-[0-9]{5}");

        JobFactory factory = new JobFactory();
        String id = factory.generateJobId();

        assertTrue(pattern.matcher(id).matches());
    }

    @Test
    public void testGenerateJobId_IncludesHost()
    {
        Pattern pattern = Pattern.compile("[0-9]{14}-server-1-[0-9]{5}");

        JobFactory factory = new JobFactory("server-1");
        String id = factory.generateJobId();

        assertTrue(pattern.matcher(id).matches());
    }

    @Test
    public void testGenerateJobId_isShorterThanMaxHJobLength()
    {
        JobFactory factory = new JobFactory();
        assertTrue(factory.generateJobId().length() < JobConfigurationVerifier.MAX_JOB_ID_LENGTH);
    }

    @Test
    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname()
    {
        JobFactory factory = new JobFactory("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, factory.generateJobId().length());
    }

}
