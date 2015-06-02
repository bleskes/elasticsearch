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

package com.prelert.job.data;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;

public class InputStreamDuplicatorTest
{
    private static final String TEXT = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife. " +
            "However little known the feelings or views of such a man may be on his first entering a neighbourhood, " +
            "this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful " +
            "property of some one or other of their daughters. My dear Mr. Bennet, said his lady to him one day, \"" +
            "have you heard that Netherfield Park is let at last?\" Mr. Bennet replied that he had not. \"But it is,\" " +
            " returned she; \"for Mrs. Long has just been here, and she told me all about it.\" Mr. Bennet made no answer. " +
            "\"Do you not want to know who has taken it?\" cried his wife impatiently. \"You want to tell me, and I have no objection to hearing it.\" " +
            "This was invitation enough. \"Why, my dear, you must know, Mrs. Long says that Netherfield is taken by a young man of large fortune " +
            "from the north of England; that he came down on Monday in a chaise and four to see the place, and was so much delighted with it, " +
            "that he agreed with Mr. Morris immediately; that he is to take possession before Michaelmas, and some of his servants are to be in" +
            " the house by the end of next week.";
    @Test
    public void testDuplicate()
    throws UnsupportedEncodingException
    {
        ByteArrayInputStream input = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        ByteArrayOutputStream out3 = new ByteArrayOutputStream();

        InputStreamDuplicator duplicator = new InputStreamDuplicator(input);
        duplicator.addOutput(out1);
        duplicator.addOutput(out2);
        duplicator.addOutput(out3);

        duplicator.run();

        assertEquals(TEXT, out1.toString(StandardCharsets.UTF_8.name()));
        assertEquals(TEXT, out2.toString(StandardCharsets.UTF_8.name()));
        assertEquals(TEXT, out3.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void testDuplicate_StreamThrowsIOException()
    throws IOException
    {
        ByteArrayInputStream input = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        // mock object throws IOException when write is called
        OutputStream mockOut = mock(OutputStream.class);
        Mockito.doThrow(new IOException()).when(mockOut).write(Mockito.any(byte[].class),
                                    Mockito.anyInt(), Mockito.anyInt());

        InputStreamDuplicator duplicator = new InputStreamDuplicator(input);
        duplicator.addOutput(out1);
        duplicator.addOutput(out2);
        duplicator.addOutput(mockOut);

        duplicator.run();

        assertEquals(TEXT, out1.toString(StandardCharsets.UTF_8.name()));
        assertEquals(TEXT, out2.toString(StandardCharsets.UTF_8.name()));
    }

}
