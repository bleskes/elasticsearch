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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.rs.data.Acknowledgement;

public class AcknowledgementWriterTest
{

    @Test
    public void testIsWritable()
    {
        AcknowledgementWriter writer = new AcknowledgementWriter();

        assertFalse(writer.isWriteable(String.class, mock(Type.class), null, null));
        assertTrue(writer.isWriteable(
                Acknowledgement.class, mock(ParameterizedType.class), null, null));
    }

    @Test
    public void testSerialise() throws WebApplicationException, IOException
    {
        Acknowledgement ack = new Acknowledgement();
        AcknowledgementWriter writer = new AcknowledgementWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.writeTo(ack, Acknowledgement.class, mock(Type.class),
                new Annotation[] {}, MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(),
                output);

        String content = new String(output.toByteArray());

        ObjectMapper jsonMapper = new ObjectMapper();
        ack = jsonMapper.readValue(content, new TypeReference<Acknowledgement>() {} );

        assertTrue(ack.getAcknowledgement());
    }

}
