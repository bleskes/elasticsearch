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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SingleDocumentTest
{
    @Test
    public void testSetDocument_GivenNonNull()
    {
        SingleDocument<String> doc = new SingleDocument<String>();

        doc.setDocument("a document");

        assertTrue(doc.isExists());
        assertEquals("a document", doc.getDocument());
    }

    @Test
    public void testSetDocument_GivenNull()
    {
        SingleDocument<String> doc = new SingleDocument<String>();

        doc.setDocument(null);

        assertFalse(doc.isExists());
        assertNull(doc.getDocument());
    }

    @Test
    public void testSetExists()
    {
        SingleDocument<String> doc = new SingleDocument<String>();
        doc.setDocument("a document");
        doc.setExists(false);

        assertFalse(doc.isExists());
    }
}
