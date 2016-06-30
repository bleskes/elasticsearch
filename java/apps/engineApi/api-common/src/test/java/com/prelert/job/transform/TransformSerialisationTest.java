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

package com.prelert.job.transform;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class TransformSerialisationTest
{
    @Test
    public void testDeserialise_singleFieldAsArray() throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":\"dns\", \"transform\":\"highest_registered_domain\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1,  tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());
        assertEquals(0, tr.getOutputs().size());


        json = "{\"inputs\":\"dns\", \"transform\":\"highest_registered_domain\", \"outputs\":\"catted\"}";
        tr = reader.readValue(json);

        assertEquals(1,  tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());
        assertEquals(1, tr.getOutputs().size());
        assertEquals("catted", tr.getOutputs().get(0));
    }

    @Test
    public void testDeserialise_fieldsArray() throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(TransformConfig.class)
                            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "{\"inputs\":[\"dns\"], \"transform\":\"highest_registered_domain\"}";
        TransformConfig tr = reader.readValue(json);

        assertEquals(1,  tr.getInputs().size());
        assertEquals("dns", tr.getInputs().get(0));
        assertEquals("highest_registered_domain", tr.getTransform());

        json = "{\"inputs\":[\"a\", \"b\", \"c\"], \"transform\":\"concat\", \"outputs\":[\"catted\"]}";
        tr = reader.readValue(json);

        assertEquals(3,  tr.getInputs().size());
        assertEquals("a", tr.getInputs().get(0));
        assertEquals("b", tr.getInputs().get(1));
        assertEquals("c", tr.getInputs().get(2));
        assertEquals("concat", tr.getTransform());
        assertEquals(1, tr.getOutputs().size());
        assertEquals("catted", tr.getOutputs().get(0));
    }
}
