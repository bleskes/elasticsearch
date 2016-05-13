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

package com.prelert.rs.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.exception.InvalidParametersException;

public class CategoryDefinitionsTest extends ServiceTest
{
    private static final String JOB_ID = "foo";

    private CategoryDefinitions m_CategoryDefinitions;

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Before
    public void setUp() throws UnknownJobException
    {
        m_CategoryDefinitions = new CategoryDefinitions();
        configureService(m_CategoryDefinitions);
    }

    @Test
    public void testCategoryDefinitions_GivenNegativeSkip() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_CategoryDefinitions.categoryDefinitions(JOB_ID, -1, 100);
    }

    @Test
    public void testCategoryDefinitions_GivenNegativeTake() throws UnknownJobException, NativeProcessRunException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_CategoryDefinitions.categoryDefinitions(JOB_ID, 0, -1);
    }

    @Test
    public void testCategoryDefinitions_GivenAllResultsInOnePage() throws UnknownJobException
    {
        CategoryDefinition category1 = new CategoryDefinition();
        category1.setCategoryId(1);
        CategoryDefinition category2 = new CategoryDefinition();
        category2.setCategoryId(2);
        CategoryDefinition category3 = new CategoryDefinition();
        category3.setCategoryId(3);

        QueryPage<CategoryDefinition> page = new QueryPage<>(Arrays.asList(category1, category2, category3), 3);

        when(jobReader().categoryDefinitions(JOB_ID, 0, 100)).thenReturn(page);

        Pagination<CategoryDefinition> result =
                                    m_CategoryDefinitions.categoryDefinitions(JOB_ID, 0, 100);

        assertNull(result.getNextPage());
        assertNull(result.getPreviousPage());
    }

    @Test
    public void testCategoryDefinition_GivenExistingCategoryId() throws UnknownJobException
    {
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(7);
        Optional<CategoryDefinition> result = Optional.of(category);

        when(jobReader().categoryDefinition(JOB_ID, "7")).thenReturn(result);

        Response response = m_CategoryDefinitions.categoryDefinition(JOB_ID, "7");

        @SuppressWarnings("unchecked")
        SingleDocument<CategoryDefinition> doc = (SingleDocument<CategoryDefinition>)response.getEntity();

        assertEquals(category, doc.getDocument());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCategoryDefinition_GivenNonExistingCategoryId() throws UnknownJobException
    {
        Optional<CategoryDefinition> result = Optional.empty();

        when(jobReader().categoryDefinition(JOB_ID, "7")).thenReturn(result);

        Response response = m_CategoryDefinitions.categoryDefinition(JOB_ID, "7");
        @SuppressWarnings("unchecked")
        SingleDocument<CategoryDefinition> doc = (SingleDocument<CategoryDefinition>)response.getEntity();

        assertEquals(CategoryDefinition.TYPE, doc.getType());
        assertEquals(false, doc.isExists());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
