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

package com.prelert.rs.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class PaginationTest
{
    @Test
    public void testGetDocumentCount_GivenDocumentsIsNull()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setDocuments(null);

        assertEquals(0, pagination.getDocumentCount());
    }

    @Test
    public void testGetDocumentCount_GivenDocumentsIsEmpty()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setDocuments(new ArrayList<>());

        assertEquals(0, pagination.getDocumentCount());
    }

    @Test
    public void testGetDocumentCount_GivenDocumentsIsNonEmpty()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setDocuments(Arrays.asList("foo", "bar"));

        assertEquals(2, pagination.getDocumentCount());
    }

    @Test
    public void testIsAllResults_GivenDocumentsIsNull()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setHitCount(3);
        pagination.setDocuments(null);

        assertFalse(pagination.isAllResults());
    }

    @Test
    public void testIsAllResults_GivenDocumentsSizeIsNotEqualToHitCount()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setHitCount(3);
        pagination.setDocuments(new ArrayList<>());

        assertFalse(pagination.isAllResults());
    }

    @Test
    public void testIsAllResults_GivenDocumentsSizeIsEqualToHitCount()
    {
        Pagination<String> pagination = new Pagination<>();
        pagination.setHitCount(3);
        pagination.setDocuments(Arrays.asList("foo", "bar", "foobar"));

        assertTrue(pagination.isAllResults());
    }
}
