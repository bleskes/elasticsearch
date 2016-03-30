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

package com.prelert.data.extractor.elasticsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

class HttpResponse
{
    private static final String NEW_LINE = "\n";

    private final InputStream m_Stream;
    private final int m_ResponseCode;

    public HttpResponse(InputStream responseStream, int responseCode)
    {
        m_Stream = responseStream;
        m_ResponseCode = responseCode;
    }

    public int getResponseCode()
    {
        return m_ResponseCode;
    }

    public InputStream getStream()
    {
        return m_Stream;
    }

    public String getResponseAsString() throws IOException
    {
        return getStreamAsString(m_Stream);
    }

    public static String getStreamAsString(InputStream stream) throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining(NEW_LINE));
        }
    }
}
