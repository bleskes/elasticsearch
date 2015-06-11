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

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

/**
 * Test the valid detector/functions/field combinations i.e. the
 * logic encoded in the table below
 *
<table cellpadding="4px" border="1" width="90%"><colgroup><col class="col_1" /><col class="col_2" /><col class="col_3" /><col class="col_4" /><col class="col_5" /></colgroup>
    <thead>
        <tr><th align="left" valign="top">name</th><th align="left" valign="top">description</th><th align="left" valign="top">fieldName</th><th align="left" valign="top">byFieldName</th><th align="left" valign="top">overFieldName</th></tr></thead><tbody>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-count.html" title="count">count</a></p></td><td align="left" valign="top"><p>individual count</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-high_count.html" title="high_count">high_count</a></p></td><td align="left" valign="top"><p>individual count (high only)</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-low_count.html" title="low_count">low_count</a></p></td><td align="left" valign="top"><p>individual count (low only)</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-non_zero_count.html" title="non_zero_count or nzc">non_zero_count or nzc</a></p></td><td align="left" valign="top"><p>"count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-low_non_zero_count.html" title="low_non_zero_count or low_nzc">low_non_zero_count or low_nzc</a></p></td><td align="left" valign="top"><p>"low count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-high_non_zero_count.html" title="high_non_zero_count or high_nzc">high_non_zero_count or high_nzc</a></p></td><td align="left" valign="top"><p>"high count, but zeros are null, not zero"</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>N/A</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-distinct_count.html" title="distinct_count or dc">distinct_count or dc</a></p></td><td align="left" valign="top"><p>distinct count</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-rare.html" title="rare">rare</a></p></td><td align="left" valign="top"><p>rare items</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-freq_rare.html" title="freq_rare">freq_rare</a></p></td><td align="left" valign="top"><p>frequently rare items</p></td><td align="left" valign="top"><p>N/A</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>required</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-metric.html" title="metric">metric</a></p></td><td align="left" valign="top"><p>all of mean, min, max and sum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-mean.html" title="mean or avg">mean or avg</a></p></td><td align="left" valign="top"><p>arithmetic mean</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-min.html" title="min">min</a></p></td><td align="left" valign="top"><p>arithmetic minimum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-max.html" title="max">max</a></p></td><td align="left" valign="top"><p>arithmetic maximum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
        <tr><td align="left" valign="top"><p><a class="link" href="functions-sum.html" title="sum">sum</a></p></td><td align="left" valign="top"><p>arithmetic sum</p></td><td align="left" valign="top"><p>required</p></td><td align="left" valign="top"><p>optional</p></td><td align="left" valign="top"><p>optional</p></td></tr>
    </tbody>
</table>
 *
 */
public class DetectorTest
{
    /**
     * Test the good/bad detector configurations
     * @throws JobConfigurationException
     */
    @Test
    public void testVerify() throws JobConfigurationException
    {
        // if nothing else is set the count functions (excluding distinct count)
        // are the only allowable functions
        Detector d = new Detector();
        d.setFunction(Detector.COUNT);
        d.verify(false);
        d.verify(true);

        Set<String> difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.COUNT);
        difference.remove(Detector.HIGH_COUNT);
        difference.remove(Detector.LOW_COUNT);
        difference.remove(Detector.NON_ZERO_COUNT);
        difference.remove(Detector.NZC);
        difference.remove(Detector.LOW_NON_ZERO_COUNT);
        difference.remove(Detector.LOW_NZC);
        difference.remove(Detector.HIGH_NON_ZERO_COUNT);
        difference.remove(Detector.HIGH_NZC);
        for (String f : difference)
        {
            try
            {
                d.setFunction(f);
                d.verify(false);
                System.out.println(f);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.setFunction(f);
                d.verify(true);
                System.out.println(f);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }

        // a byField on its own is invalid
        d = new Detector();
        d.setByFieldName("by");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }

        // an overField on its own is invalid
        d = new Detector();
        d.setOverFieldName("over");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }

        // certain fields aren't allowed with certain functions
        // first do the over field
        d = new Detector();
        d.setOverFieldName("over");
        for (String f : new String[] { Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.LOW_NON_ZERO_COUNT, Detector.LOW_NZC, Detector.HIGH_NON_ZERO_COUNT,
                Detector.HIGH_NZC })
        {
            d.setFunction(f);
            try
            {
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }

        // these functions cannot have just an over field
        difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.COUNT);
        difference.remove(Detector.HIGH_COUNT);
        difference.remove(Detector.LOW_COUNT);
        for (String f : difference)
        {
            d.setFunction(f);
            try
            {
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }

        // these functions can have just an over field
        for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }

        d.setByFieldName("by");
        for (String f : new String [] {Detector.RARE, Detector.FREQ_RARE})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }
        d.setByFieldName(null);


        // some functions require a fieldname
        d.setFieldName("f");
        for (String f : new String[] { Detector.DISTINCT_COUNT, Detector.DC,
                Detector.HIGH_DISTINCT_COUNT, Detector.HDC, Detector.LOW_DISTINCT_COUNT, Detector.LDC,
                Detector.INFO_CONTENT, Detector.LOW_INFO_CONTENT, Detector.HIGH_INFO_CONTENT,
                Detector.METRIC, Detector.MEAN, Detector.HIGH_MEAN, Detector.LOW_MEAN, Detector.AVG,
                Detector.HIGH_AVG, Detector.LOW_AVG, Detector.MAX, Detector.MIN, Detector.SUM,
                Detector.LOW_SUM, Detector.HIGH_SUM, Detector.NON_NULL_SUM,
                Detector.LOW_NON_NULL_SUM, Detector.HIGH_NON_NULL_SUM })
        {
            d.setFunction(f);
            d.verify(false);
            try
            {
                d.verify(true);
                Assert.assertFalse(Detector.METRIC.equals(f));
            }
            catch (JobConfigurationException e)
            {
                // "metric" is not allowed as the function for pre-summarised input
                Assert.assertEquals(Detector.METRIC, f);
            }
        }

        // these functions cannot have a field name
        difference = new HashSet<String>(Detector.ANALYSIS_FUNCTIONS);
        difference.remove(Detector.METRIC);
        difference.remove(Detector.MEAN);
        difference.remove(Detector.LOW_MEAN);
        difference.remove(Detector.HIGH_MEAN);
        difference.remove(Detector.AVG);
        difference.remove(Detector.LOW_AVG);
        difference.remove(Detector.HIGH_AVG);
        difference.remove(Detector.MIN);
        difference.remove(Detector.MAX);
        difference.remove(Detector.SUM);
        difference.remove(Detector.LOW_SUM);
        difference.remove(Detector.HIGH_SUM);
        difference.remove(Detector.NON_NULL_SUM);
        difference.remove(Detector.LOW_NON_NULL_SUM);
        difference.remove(Detector.HIGH_NON_NULL_SUM);
        difference.remove(Detector.DISTINCT_COUNT);
        difference.remove(Detector.HIGH_DISTINCT_COUNT);
        difference.remove(Detector.LOW_DISTINCT_COUNT);
        difference.remove(Detector.DC);
        difference.remove(Detector.LDC);
        difference.remove(Detector.HDC);
        difference.remove(Detector.INFO_CONTENT);
        difference.remove(Detector.LOW_INFO_CONTENT);
        difference.remove(Detector.HIGH_INFO_CONTENT);
        for (String f : difference)
        {
            d.setFunction(f);
            try
            {
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }

        d.setFieldName(null);
        d.setByFieldName("b");
        d.setOverFieldName(null);

        // these can have a by field
        for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.RARE,
                Detector.NON_ZERO_COUNT, Detector.NZC})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }

        d.setOverFieldName("over");
        d.setFunction(Detector.FREQ_RARE);
        d.verify(false);
        d.verify(true);
        d.setOverFieldName(null);

        // some functions require a fieldname
        d.setFieldName("f");
        for (String f : new String[] { Detector.METRIC, Detector.MEAN, Detector.HIGH_MEAN,
                Detector.LOW_MEAN, Detector.AVG, Detector.HIGH_AVG, Detector.LOW_AVG, Detector.MAX,
                Detector.MIN, Detector.SUM, Detector.LOW_SUM, Detector.HIGH_SUM, Detector.NON_NULL_SUM,
                Detector.LOW_NON_NULL_SUM, Detector.HIGH_NON_NULL_SUM, Detector.DISTINCT_COUNT, Detector.DC,
                Detector.HIGH_DISTINCT_COUNT, Detector.HDC, Detector.LOW_DISTINCT_COUNT, Detector.LDC,
                Detector.INFO_CONTENT, Detector.LOW_INFO_CONTENT, Detector.HIGH_INFO_CONTENT })
        {
            d.setFunction(f);
            d.verify(false);
            try
            {
                d.verify(true);
                Assert.assertFalse(Detector.METRIC.equals(f));
            }
            catch (JobConfigurationException e)
            {
                // "metric" is not allowed as the function for pre-summarised input
                Assert.assertEquals(Detector.METRIC, f);
            }
        }


        // test field name
        d = new Detector();
        d.setFieldName("field");

        // these functions don't work with fieldname
        for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.RARE, Detector.FREQ_RARE})
        {
            try
            {
                d.setFunction(f);
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.setFunction(f);
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }


        // these functions cant have a field
        d.setOverFieldName(null);
        for (String f : new String [] {Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC,
                Detector.RARE, Detector.FREQ_RARE})
        {
            try
            {
                d.setFunction(f);
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.setFunction(f);
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }

        d.setOverFieldName("over");
        d.setFunction(Detector.FREQ_RARE);
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
        }


        d = new Detector();
        d.setByFieldName("by");
        for (String f : new String [] {Detector.HIGH_COUNT,
                Detector.LOW_COUNT, Detector.NON_ZERO_COUNT, Detector.NZC})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }

        d = new Detector();
        d.setOverFieldName("over");
        for (String f : new String [] {Detector.COUNT, Detector.HIGH_COUNT,
                Detector.LOW_COUNT})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }

        d = new Detector();
        d.setByFieldName("by");
        d.setOverFieldName("over");
        for (String f : new String [] {Detector.HIGH_COUNT,
                Detector.LOW_COUNT})
        {
            d.setFunction(f);
            d.verify(false);
            d.verify(true);
        }

        d = new Detector();
        d.setByFieldName("by");
        d.setOverFieldName("over");
        for (String f : new String [] {Detector.NON_ZERO_COUNT, Detector.NZC})
        {
            try
            {
                d.setFunction(f);
                d.verify(false);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
            try
            {
                d.setFunction(f);
                d.verify(true);
                Assert.fail("JobConfigurationException not thrown when expected");
            }
            catch (JobConfigurationException e)
            {
            }
        }
    }

    @Test
    public void testVerify_GivenInvalidFieldNames()
    {
        Detector d = new Detector();
        d.setFieldName("(invalid\"chars");
        d.setByFieldName("by");
        d.setOverFieldName("over");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("f");
        d.setByFieldName("(invalid-chars");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("f");
        d.setOverFieldName("bad=name");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d.setOverFieldName("over");
        d.setPartitionFieldName("bad\\name");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("over(");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("over)");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("f");
        d.setByFieldName("b[");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }

        d = new Detector();
        d.setFieldName("f");
        d.setByFieldName("b]");
        try
        {
            d.verify(false);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            Assert.assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, e.getErrorCode());
        }
        try
        {
            d.verify(true);
            Assert.fail("JobConfigurationException not thrown when expected");
        }
        catch (JobConfigurationException e)
        {
            // "metric" function for pre-summarised data trumps invalid characters
            Assert.assertEquals(ErrorCodes.INVALID_FUNCTION, e.getErrorCode());
        }
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        Detector detector1 = new Detector();
        detector1.setFunction("mean");
        detector1.setFieldName("field");
        detector1.setByFieldName("by");
        detector1.setOverFieldName("over");
        detector1.setPartitionFieldName("partition");
        detector1.setUseNull(false);

        Detector detector2 = new Detector();
        detector2.setFunction("mean");
        detector2.setFieldName("field");
        detector2.setByFieldName("by");
        detector2.setOverFieldName("over");
        detector2.setPartitionFieldName("partition");
        detector2.setUseNull(false);

        assertEquals(detector1.hashCode(), detector2.hashCode());
    }
}
