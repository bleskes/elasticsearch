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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.exceptions.JobConfigurationException;

public class DataDescriptionTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testDataFormatForString()
    {
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("delineated"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("DELINEATED"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("delimited"));
        assertEquals(DataFormat.DELIMITED, DataFormat.forString("DELIMITED"));

        assertEquals(DataFormat.JSON, DataFormat.forString("json"));
        assertEquals(DataFormat.JSON, DataFormat.forString("JSON"));

        assertEquals(DataFormat.SINGLE_LINE, DataFormat.forString("single_line"));
        assertEquals(DataFormat.SINGLE_LINE, DataFormat.forString("SINGLE_LINE"));
    }

    @Test
    public void testVerify_GivenNullTimeFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat(null);

        assertTrue(description.verify());
    }

    @Test
    public void testVerify_GivenEmptyTimeFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("");

        assertTrue(description.verify());
    }

    @Test
    public void testVerify_GivenTimeFormatIsEpoch() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch");

        assertTrue(description.verify());
    }

    @Test
    public void testVerify_GivenTimeFormatIsEpochMs() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch_ms");

        assertTrue(description.verify());
    }

    @Test
    public void testVerify_GivenTimeFormatIsValidDateFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("yyyy-MM-dd");

        assertTrue(description.verify());
    }

    @Test
    public void testVerify_GivenTimeFormatIsInvalidDateFormat() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid Time format string 'invalid'");

        DataDescription description = new DataDescription();
        description.setTimeFormat("invalid");

        description.verify();
    }

    @Test
    public void testVerify_GivenTimeFormatIsInvalidDateParseFormat()
    throws JobConfigurationException
    {
        String badFormat = "YYY-mm-UU hh:mm:ssY";
        DataDescription dd = new DataDescription();

        dd.setTimeFormat(badFormat);
        try
        {
            dd.verify();
            // shouldn't get here
            Assert.assertTrue("Invalid format should throw", false);
        }
        catch (JobConfigurationException e)
        {
        }

        String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
        dd.setTimeFormat(goodFormat);
        Assert.assertTrue("Good time format", dd.verify());
    }

    @Test
    public void testEquals_GivenEqual()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertTrue(description1.equals(description1));
        assertTrue(description1.equals(description2));
        assertTrue(description2.equals(description1));
    }

    @Test
    public void testEquals_GivenDifferentDateFormat()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.DELIMITED);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }

    @Test
    public void testEquals_GivenDifferentQuoteCharacter()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('\'');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }

    @Test
    public void testEquals_GivenDifferentTimeField()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("time");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }

    @Test
    public void testEquals_GivenDifferentTimeFormat()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch_ms");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }

    @Test
    public void testEquals_GivenDifferentFieldDelimiter()
    {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(';');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        DataDescription dataDescription1 = new DataDescription();
        dataDescription1.setFormat(DataFormat.JSON);
        dataDescription1.setTimeField("timestamp");
        dataDescription1.setQuoteCharacter('\'');
        dataDescription1.setTimeFormat("timeFormat");
        dataDescription1.setFieldDelimiter(',');

        DataDescription dataDescription2 = new DataDescription();
        dataDescription2.setFormat(DataFormat.JSON);
        dataDescription2.setTimeField("timestamp");
        dataDescription2.setQuoteCharacter('\'');
        dataDescription2.setTimeFormat("timeFormat");
        dataDescription2.setFieldDelimiter(',');

        assertEquals(dataDescription1.hashCode(), dataDescription2.hashCode());
    }
}
