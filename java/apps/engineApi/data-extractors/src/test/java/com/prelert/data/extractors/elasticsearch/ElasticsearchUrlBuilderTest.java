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

package com.prelert.data.extractors.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ElasticsearchUrlBuilderTest
{
    private static final String BASE_URL = "http://localhost:9200";
    private static final List<String> EMPTY_TYPES = Collections.emptyList();
    private static final List<String> TWO_TYPES = Arrays.asList("type_1", "type_2");

    @Test
    public void testBuildIndexSettingsUrl()
    {
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildIndexSettingsUrl("foo");
        assertEquals("http://localhost:9200/foo/_settings", url);
    }

    @Test
    public void testBuildInitScrollUrl_GivenMultipleIndicesAndTypes()
    {
        List<String> indices = Arrays.asList("index_1", "index_2");
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildInitScrollUrl(indices, 5000);
        assertEquals("http://localhost:9200/index_1,index_2/type_1,type_2/_search?scroll=60m&size=5000", url);
    }

    @Test
    public void testBuildContinueScrollUrl()
    {
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildContinueScrollUrl();
        assertEquals("http://localhost:9200/_search/scroll?scroll=60m", url);
    }

    @Test
    public void testBuildClearScrollUrl()
    {
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildClearScrollUrl();
        assertEquals("http://localhost:9200/_search/scroll", url);
    }

    @Test
    public void testBuildSearchSizeOneUrl_GivenMultipleIndicesAndTypes()
    {
        List<String> indices = Arrays.asList("index_1", "index_2");;
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildSearchSizeOneUrl(indices);
        assertEquals("http://localhost:9200/index_1,index_2/type_1,type_2/_search?size=1", url);
    }

    @Test
    public void testBuildSearchSizeOneUrl_GivenMultipleIndicesAndEmptyTypes()
    {
        List<String> indices = Arrays.asList("index_1", "index_2");;
        String url = ElasticsearchUrlBuilder.create(BASE_URL, EMPTY_TYPES).buildSearchSizeOneUrl(indices);
        assertEquals("http://localhost:9200/index_1,index_2/_search?size=1", url);
    }

    @Test
    public void testBuildFieldStatsUrl()
    {
        List<String> indices = Arrays.asList("myPattern*");
        String url = ElasticsearchUrlBuilder.create(BASE_URL, TWO_TYPES).buildFieldStatsUrl(indices);
        assertEquals("http://localhost:9200/myPattern*/_field_stats?level=indices", url);
    }

    @Test
    public void testGetBaseUrl_GivenNoEndingSlash()
    {
        String url = ElasticsearchUrlBuilder.create("http://localhost:9200", TWO_TYPES).getBaseUrl();
        assertEquals("http://localhost:9200/", url);
    }

    @Test
    public void testGetBaseUrl_GivenEndingSlash()
    {
        String url = ElasticsearchUrlBuilder.create("http://localhost:9200/", TWO_TYPES).getBaseUrl();
        assertEquals("http://localhost:9200/", url);
    }
}
