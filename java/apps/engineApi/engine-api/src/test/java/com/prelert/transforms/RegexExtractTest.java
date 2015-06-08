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

package com.prelert.transforms;

import static com.prelert.transforms.TransformTestUtils.createIndexArray;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class RegexExtractTest {

    @Test
    public void testTransform() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0),
                new TransformIndex(2, 1), new TransformIndex(2, 2));

        String regex = "Tag=\"Windfarm ([0-9]+)\\.Turbine ([0-9]+)\\.(.*)\"";

        RegexExtract transform = new RegexExtract(regex, readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"Tag=\"Windfarm 04.Turbine 06.Temperature\""};
        String [] scratch = {};
        String [] output = new String [3];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
        assertEquals("04", output[0]);
        assertEquals("06", output[1]);
        assertEquals("Temperature", output[2]);
    }
}
