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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * The duplicated streams must be read in separate
 * threads for the duplicator to work
 */
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

    private class ReadTask implements Runnable
    {
        String text = "";
        BufferedReader reader;

        ReadTask(BufferedReader reader)
        {
            this.reader = reader;
        }

        @Override
        public void run()
        {
            try
            {
                String line = reader.readLine();
                while (line != null)
                {
                    text += line;
                    line = reader.readLine();
                }
            }
            catch (IOException e)
            {

            }
        }
    }


    @Test
    public void testDuplicate()
    throws InterruptedException, IOException
    {
        ByteArrayInputStream input = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        InputStreamDuplicator duplicator = new InputStreamDuplicator(input);


        List<ReadTask> tasks = new ArrayList<>();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i=0; i<3; i++)
        {
           BufferedReader reader = new BufferedReader(
                                new InputStreamReader(duplicator.createDuplicateStream(),
                                        StandardCharsets.UTF_8));

           ReadTask task = new ReadTask(reader);
           tasks.add(task);
           threads.add(new Thread(task));
        }

        new Thread(() -> duplicator.duplicate()).start();

        for (Thread th : threads)
        {
            th.start();
        }

        for (Thread th : threads)
        {
            th.join();
        }

        for (ReadTask task : tasks)
        {
            assertEquals(TEXT, task.text);
        }
    }

    @Test
    public void testDuplicate_StreamThrowsIOException()
    throws IOException, InterruptedException
    {
        ByteArrayInputStream input = new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8));
        InputStreamDuplicator duplicator = new InputStreamDuplicator(input);


        List<ReadTask> tasks = new ArrayList<>();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i=0; i<3; i++)
        {
           BufferedReader reader = new BufferedReader(
                                new InputStreamReader(duplicator.createDuplicateStream(),
                                        StandardCharsets.UTF_8));

           ReadTask task = new ReadTask(reader);
           tasks.add(task);
           threads.add(new Thread(task));
        }


        BufferedReader badReader = new BufferedReader(
                new InputStreamReader(duplicator.createDuplicateStream(),
                        StandardCharsets.UTF_8));

        // Closing this reader stream will cause an IOException
        // when the other end of the pipe is written too
        badReader.close();
        ReadTask badTask = new ReadTask(badReader);
        Thread badThread = new Thread(badTask);
        badThread.start();

        new Thread(() -> duplicator.duplicate()).start();

        for (Thread th : threads)
        {
            th.start();
        }


        for (Thread th : threads)
        {
            th.join();
        }

        badThread.join();

        for (ReadTask task : tasks)
        {
            assertEquals(TEXT, task.text);
        }

        assertEquals("", badTask.text);
    }

}
