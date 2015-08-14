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

package com.prelert.rs.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.alert.Alert;
import com.prelert.job.results.Bucket;

public class AlertMessageBodyWriterTest
{
    private final AlertMessageBodyWriter m_Writer = new AlertMessageBodyWriter();

    @Test
    public void testGetSize()
    {
        assertEquals(0, m_Writer.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable()
    {
        assertTrue(m_Writer.isWriteable(Alert.class, null, null, null));
        assertFalse(m_Writer.isWriteable(Bucket.class, null, null, null));
    }

    @Test
    public void testWriteTo() throws WebApplicationException, IOException
    {
        Alert alert = new Alert();
        alert.setAnomalyScore(90.0);
        alert.setJobId("foo");
        alert.setTimeout(true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        m_Writer.writeTo(alert, Alert.class, null, null, MediaType.APPLICATION_JSON_TYPE, null,
                outputStream);

        String output = outputStream.toString("UTF-8");
        ObjectReader reader = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).reader()
                .forType(Alert.class);
        Alert readValue = reader.readValue(output);

        assertEquals(90.0, readValue.getAnomalyScore(), 0.0000001);
        assertEquals("foo", readValue.getJobId());
        assertTrue(readValue.isTimeout());
    }
}
