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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.persistence.JobProvider;

public class BackendInfoTest
{
    @Mock private JobProvider m_JobProvider;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLicenseViolations()
    {
        String json = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                BackendInfo.JOBS_LICENSE_CONSTRAINT, 5,
                BackendInfo.DETECTORS_LICENSE_CONSTRAINT, 10,
                BackendInfo.PARTITIONS_LICENSE_CONSTRAINT, -1);

        BackendInfo bInfo = create(json);

        assertEquals(bInfo.getLicenseJobLimit(), 5);
        assertEquals(bInfo.getMaxRunningDetectors(), 10);

        assertTrue(bInfo.isLicenseJobLimitViolated(6));
        assertTrue(bInfo.isLicenseDetectorLimitViolated(8, 3));

        assertFalse(bInfo.isLicenseJobLimitViolated(4));
        assertFalse(bInfo.isLicenseDetectorLimitViolated(8, 2));
    }

    @Test
    public void testNoRestrictions()
    {
        BackendInfo bInfo = create("{}");

        assertEquals(bInfo.getLicenseJobLimit(), -1);
        assertEquals(bInfo.getMaxRunningDetectors(), -1);

        assertFalse(bInfo.isLicenseJobLimitViolated(Integer.MAX_VALUE));
        assertFalse(bInfo.isLicenseDetectorLimitViolated(Integer.MAX_VALUE -1, 1));
        assertTrue(bInfo.arePartitionsAllowed());
    }

    @Test
    public void testPartitionsLicence()
    {
        String json = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                BackendInfo.JOBS_LICENSE_CONSTRAINT, 5,
                BackendInfo.DETECTORS_LICENSE_CONSTRAINT, 10,
                BackendInfo.PARTITIONS_LICENSE_CONSTRAINT, -1);

        BackendInfo bInfo = create(json);

        assertTrue(bInfo.arePartitionsAllowed());

        json = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                BackendInfo.JOBS_LICENSE_CONSTRAINT, 5,
                BackendInfo.DETECTORS_LICENSE_CONSTRAINT, 10,
                BackendInfo.PARTITIONS_LICENSE_CONSTRAINT, 1);

        bInfo = create(json);

        assertFalse(bInfo.arePartitionsAllowed());
    }

    private BackendInfo create(String json)
    {
        return BackendInfo.fromJson(json, m_JobProvider, "2.0.0");
    }

}
