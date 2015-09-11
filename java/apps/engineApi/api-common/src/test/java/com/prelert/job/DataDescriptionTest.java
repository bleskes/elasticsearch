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

import org.junit.Test;

import com.prelert.job.DataDescription.DataFormat;

public class DataDescriptionTest
{
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
    public void testTransform_GivenJson()
    {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        assertTrue(dd.transform());
    }

    @Test
    public void testTransform_GivenDelimitedAndEpoch()
    {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setTimeFormat("epoch");
        assertFalse(dd.transform());
    }

    @Test
    public void testTransform_GivenDelimitedAndEpochMs()
    {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setTimeFormat("epoch_ms");
        assertTrue(dd.transform());
    }

    @Test
    public void testIsTransformTime_GivenTimeFormatIsNull()
    {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat(null);
        assertFalse(dd.isTransformTime());
    }

    @Test
    public void testIsTransformTime_GivenTimeFormatIsEpoch()
    {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("epoch");
        assertFalse(dd.isTransformTime());
    }

    @Test
    public void testIsTransformTime_GivenTimeFormatIsEpochMs()
    {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("epoch_ms");
        assertTrue(dd.isTransformTime());
    }

    @Test
    public void testIsTransformTime_GivenTimeFormatPattern()
    {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        assertTrue(dd.isTransformTime());
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
