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

package com.prelert.job.transform;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.job.transform.TransformType;

public class TransformTypeTest
{
    @Test
    public void testFromString() throws TransformConfigurationException
    {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all)
        {
            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    @Test(expected=TransformConfigurationException.class)
    public void testFromString_UnknownType()
    throws TransformConfigurationException
    {
        @SuppressWarnings("unused")
        TransformType created = TransformType.fromString("random_type");
    }

    @Test
    public void testVerify() throws TransformConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        assertTrue(TransformType.CONCAT.verify(conf));

        conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList("dns"));
        assertTrue(TransformType.DOMAIN_SPLIT.verify(conf));
    }

    @Test
    public void testVerify_wrongNumberInputArgs()
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        tryVerify(TransformType.CONCAT, conf);

        conf.setInputs(Arrays.asList());
        tryVerify(TransformType.CONCAT, conf);

        conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList());
        tryVerify(TransformType.DOMAIN_SPLIT, conf);

    }

    @Test
    public void testVerify_OptionalArguments()
    throws TransformConfigurationException
    {
        // Concat can take an optional delimiter
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        assertTrue(conf.verify());

        conf.setArguments(Arrays.asList("delimiter"));
        assertTrue(conf.verify());

        conf.setArguments(Arrays.asList("delimiter", "invalidarg"));
        tryVerify(TransformType.CONCAT, conf);
    }

    private void tryVerify(TransformType type, TransformConfig conf)
    {
        try
        {
            type.verify(conf);
            fail("verify should throw");
        }
        catch (TransformConfigurationException e)
        {

        }
    }

}
