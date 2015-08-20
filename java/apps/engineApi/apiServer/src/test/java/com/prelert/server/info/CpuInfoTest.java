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
package com.prelert.server.info;

import static org.junit.Assert.*;

import org.junit.Test;

public class CpuInfoTest
{
    @Test
    public void testToString()
    {
        CpuInfo info = new CpuInfo();
        assertEquals("", info.toString());

        info.setVendor("intel");
        info.setModel("pentium 2");
        assertEquals("intel pentium 2", info.toString().trim());

        info.setFrequencyMHz(800);
        assertEquals("800MHz intel pentium 2", info.toString().trim());

        info.setCores(1);
        assertEquals("800MHz intel pentium 2 (1 cores)", info.toString().trim());
    }

}
