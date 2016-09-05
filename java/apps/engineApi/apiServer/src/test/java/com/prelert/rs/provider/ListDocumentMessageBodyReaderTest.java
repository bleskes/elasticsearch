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

package com.prelert.rs.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.prelert.job.JobDetails;
import com.prelert.job.ListDocument;
import com.prelert.job.errorcodes.ErrorCodes;

public class ListDocumentMessageBodyReaderTest
{
    @Test
    public void testIsReadable()
    {
        ListDocumentMessageBodyReader reader = new ListDocumentMessageBodyReader();

        assertTrue(reader.isReadable(ListDocument.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));

        assertFalse(reader.isReadable(JobDetails.class,
                                    mock(Type.class),
                                    new Annotation [] {},
                                    mock(MediaType.class)));
    }

    @Test(expected=WebApplicationException.class)
    public void testInvalidMediaType() throws IOException
    {
        ListDocumentMessageBodyReader reader = new ListDocumentMessageBodyReader();

        reader.readFrom(ListDocument.class, mock(Type.class), new Annotation [] {},
                        MediaType.APPLICATION_ATOM_XML_TYPE, new MultivaluedHashMap<String, String>(),
                        mock(InputStream.class));
    }

    @Test
    public void testReadListDocument() throws IOException
    {
        final String LIST = "{\"id\":\"a_test_list\","
                + "\"items\":[\"Bach\", \"Biber\", \"Buxtehude\", \"Boismortier\", \"Boccherini\"]}";

        ListDocumentMessageBodyReader reader = new ListDocumentMessageBodyReader();

        ListDocument list = reader.readFrom(ListDocument.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(LIST.getBytes("UTF-8")));

        ListDocument doc = new ListDocument("a_test_list", Arrays.asList("Bach", "Biber", "Buxtehude", "Boismortier", "Boccherini"));

        assertEquals(doc.getId(), list.getId());
        assertEquals(doc.getItems(), list.getItems());
    }

    @Test
    public void testReadListDocument_ParseException() throws IOException
    {
        final String LIST = "{\"id\":\"a_test_list\","
                + "\"\"\":[\"Bach\", \"Biber\", \"Buxtehude\", \"Boismortier\", \"Boccherini\"]}";

        ListDocumentMessageBodyReader reader = new ListDocumentMessageBodyReader();

        try
        {
            reader.readFrom(ListDocument.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(LIST.getBytes("UTF-8")));
            assertTrue(false);
        }
        catch (JobConfigurationParseException e)
        {
            assertEquals("JSON parse error reading the list", e.getMessage());
            assertEquals(ErrorCodes.LIST_PARSE_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void testReadListDocument_MappingException() throws IOException
    {
        final String LIST = "{\"id\":\"a_test_list\","
                + "\"an_illegal_string\":[\"Bach\", \"Biber\", \"Buxtehude\", \"Boismortier\", \"Boccherini\"]}";

        ListDocumentMessageBodyReader reader = new ListDocumentMessageBodyReader();

        try
        {
            reader.readFrom(ListDocument.class, mock(Type.class),
                                                new Annotation [] {},
                                                MediaType.APPLICATION_JSON_TYPE,
                                                new MultivaluedHashMap<String, String>(),
                                        new ByteArrayInputStream(LIST.getBytes("UTF-8")));
            assertTrue(false);
        }
        catch (JobConfigurationParseException e)
        {
            assertEquals("JSON mapping error reading the list", e.getMessage());
            assertEquals(ErrorCodes.LIST_UNKNOWN_FIELD_ERROR, e.getErrorCode());
        }
    }
}
