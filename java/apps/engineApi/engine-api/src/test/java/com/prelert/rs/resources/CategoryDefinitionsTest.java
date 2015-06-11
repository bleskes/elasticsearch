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

package com.prelert.rs.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class CategoryDefinitionsTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    private CategoryDefinitions m_CategoryDefinitions;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_CategoryDefinitions = new CategoryDefinitions();
        configureService(m_CategoryDefinitions);
    }

    @Test
    public void testCategoryDefinitions_GivenAllResultsInOnePage() throws UnknownJobException
    {
        Pagination<CategoryDefinition> results = new Pagination<>();
        results.setHitCount(3);
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(1);
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(2);
        CategoryDefinition category3 = new CategoryDefinition();
        category3.setCategoryId(3);
        results.setDocuments(Arrays.asList(category1, category2, category3));

        when(jobManager().categoryDefinitions(JOB_ID, 0, 100)).thenReturn(results);

        assertEquals(results, m_CategoryDefinitions.categoryDefinitions(JOB_ID, 0, 100));

        assertNull(results.getNextPage());
        assertNull(results.getPreviousPage());
    }

    @Test
    public void testCategoryDefinition_GivenExistingCategoryId() throws UnknownJobException
    {
        SingleDocument<CategoryDefinition> result = new SingleDocument<>();
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(7);
        result.setDocument(category);
        when(jobManager().categoryDefinition(JOB_ID, "7")).thenReturn(result);

        Response response = m_CategoryDefinitions.categoryDefinition(JOB_ID, "7");

        assertEquals(result, response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCategoryDefinition_GivenNonExistingCategoryId() throws UnknownJobException
    {
        SingleDocument<CategoryDefinition> result = new SingleDocument<>();
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(7);
        when(jobManager().categoryDefinition(JOB_ID, "7")).thenReturn(result);

        Response response = m_CategoryDefinitions.categoryDefinition(JOB_ID, "7");

        assertEquals(result, response.getEntity());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
