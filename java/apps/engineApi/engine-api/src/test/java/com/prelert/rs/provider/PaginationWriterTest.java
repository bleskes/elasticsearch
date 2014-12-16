/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.rs.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.Test;

import com.prelert.rs.data.Pagination;

public class PaginationWriterTest
{

    private final PaginationWriter<String> m_PaginationWriter = new PaginationWriter<>();

    @Test
    public void testIsWritable()
    {
        assertFalse(m_PaginationWriter.isWriteable(
                String.class, mock(Type.class), null, null));
        assertFalse(m_PaginationWriter.isWriteable(
                String.class, mock(ParameterizedType.class), null, null));
        assertFalse(m_PaginationWriter.isWriteable(
                Pagination.class, mock(Type.class), null, null));
        assertTrue(m_PaginationWriter.isWriteable(
                Pagination.class, mock(ParameterizedType.class), null, null));
    }

    @Test
    public void testGetSize()
    {
        assertEquals(0, m_PaginationWriter.getSize(null, null, null, null, null));
    }
}
