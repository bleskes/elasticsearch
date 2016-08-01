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
package com.prelert.master.streaming;


import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class HttpDataStreamerTest
{
//    @Test
    public void test() throws IOException
    {
        try (HttpDataStreamer streamer = new HttpDataStreamer("http://localhost:7888"))
        {
            OutputStream out1 = streamer.openStream("Blah");
            OutputStream out2 = streamer.openStream("gah");

            out1.write("time,airline,responsetime,sourcetype\n".getBytes());
            out2.write("time,airline,responsetime,sourcetype\n".getBytes());

            for (int i=0; i<100; i++)
            {
                out1.write("blah2014-06-23 00:00:00Z,AAL,132.2046,farequote\n".getBytes());
                out2.write("gah2014-06-23 00:00:00Z,AAL,132.2046,farequote\n".getBytes());
            }

            out1.close();
            out2.close();
        }
    }

//    @Test
    public void testWaitForLatch() throws IOException
    {
        try (HttpDataStreamer streamer = new HttpDataStreamer("http://localhost:7888"))
        {
            OutputStream out1 = streamer.openStream("Blah");
            OutputStream out2 = streamer.openStream("gah");

            ExecutorService e = Executors.newFixedThreadPool(2);
            e.execute(() -> writeData(out1));
            e.execute(() -> writeData(out2));

            streamer.waitForUploadsToComplete();

            out1.close();
            out2.close();

            streamer.waitForUploadsToComplete();
        }
    }

    private void writeData(OutputStream out)
    {
        try
        {
            for (int i=0; i<100; i++)
            {
                out.write("gah2014-06-23 00:00:00Z,AAL,132.2046,farequote\n".getBytes());
            }
        }
        catch (IOException e)
        {

        }
    }
}
