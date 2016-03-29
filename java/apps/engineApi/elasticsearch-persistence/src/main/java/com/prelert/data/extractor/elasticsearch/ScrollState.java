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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

class ScrollState
{
    static final Pattern SCROLL_ID_PATTERN = Pattern.compile("\"_scroll_id\":\"(.*?)\"");
    static final Pattern EMPTY_HITS_PATTERN = Pattern.compile("\"hits\":\\[\\]");
    static final Pattern EMPTY_AGGREGATIONS_PATTERN = Pattern.compile("\"aggregations\":.*?\"buckets\":\\[\\]");

    private final int m_BufferSize;
    private final Pattern m_ScrollCompletePattern;
    private volatile String m_ScrollId;
    private volatile boolean m_IsComplete;

    private ScrollState(int bufferSize, Pattern scrollCompletePattern)
    {
        m_BufferSize = bufferSize;
        m_ScrollCompletePattern = Objects.requireNonNull(scrollCompletePattern);
    }

    public static ScrollState createDefault(int bufferSize)
    {
        return new ScrollState(bufferSize, EMPTY_HITS_PATTERN);
    }

    public static ScrollState createAggregated(int bufferSize)
    {
        return new ScrollState(bufferSize, EMPTY_AGGREGATIONS_PATTERN);
    }

    public final void reset()
    {
        m_ScrollId = null;
        m_IsComplete = false;
    }

    public String getScrollId()
    {
        return m_ScrollId;
    }

    public boolean isComplete()
    {
        return m_IsComplete;
    }

    public void forceComplete()
    {
        m_ScrollId = null;
        m_IsComplete = true;
    }

    public InputStream updateFromStream(InputStream stream) throws IOException
    {
        if (stream == null)
        {
            m_ScrollId = null;
            m_IsComplete = true;
            return null;
        }

        PushbackInputStream pushbackStream = new PushbackInputStream(stream, m_BufferSize);
        updateScrollId(pushbackStream);
        updateIsScrollComplete(pushbackStream);
        return pushbackStream;
    }

    private void updateScrollId(PushbackInputStream stream) throws IOException
    {
        if (stream == null)
        {
            return;
        }

        Matcher matcher = peekAndMatchInStream(stream, SCROLL_ID_PATTERN);
        if (!matcher.find())
        {
            throw new IOException("Field '_scroll_id' was expected but not found in response:\n"
                    + HttpGetResponse.getStreamAsString(stream));
        }
        m_ScrollId = matcher.group(1);
    }

    private void updateIsScrollComplete(PushbackInputStream stream) throws IOException
    {
        m_IsComplete = (stream == null) || peekAndMatchInStream(stream, m_ScrollCompletePattern).find();
    }

    @VisibleForTesting
    Matcher peekAndMatchInStream(PushbackInputStream stream, Pattern pattern) throws IOException
    {
        byte[] peek = new byte[m_BufferSize];
        int bytesRead = readUntilEndOrLimit(stream, peek);

        // We make the assumption here that invalid byte sequences will be read as invalid char
        // rather than throwing an exception
        String peekString = new String(peek, 0, bytesRead, StandardCharsets.UTF_8);

        Matcher matcher = pattern.matcher(peekString);
        stream.unread(peek, 0, bytesRead);
        return matcher;
    }

    /**
     * Reads from a stream until it has read at least {@link #PUSHBACK_BUFFER_BYTES}
     * or the stream ends.
     *
     * @param stream the stream
     * @param buffer the buffer where the stream is read into
     * @return the number of total bytes read
     * @throws IOException
     */
    private int readUntilEndOrLimit(PushbackInputStream stream, byte[] buffer) throws IOException
    {
        int totalBytesRead = 0;
        int bytesRead = 0;

        while (bytesRead >= 0 && totalBytesRead < m_BufferSize)
        {
            bytesRead = stream.read(buffer, totalBytesRead, m_BufferSize - totalBytesRead);
            if (bytesRead > 0)
            {
                totalBytesRead += bytesRead;
            }
        }

        return totalBytesRead;
    }
}
