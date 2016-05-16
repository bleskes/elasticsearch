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

package com.prelert.job.persistence.elasticsearch;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.persistence.serialisation.DotNotationReverser;
import com.prelert.job.persistence.serialisation.StorageSerialisable;
import com.prelert.job.persistence.serialisation.StorageSerialiser;

class ElasticsearchStorageSerialiser implements StorageSerialiser
{
    private final XContentBuilder m_Builder;

    public ElasticsearchStorageSerialiser(XContentBuilder builder)
    {
        m_Builder = Objects.requireNonNull(builder);
    }

    @Override
    public StorageSerialiser startObject() throws IOException
    {
        m_Builder.startObject();
        return this;
    }

    @Override
    public StorageSerialiser startObject(String fieldName) throws IOException
    {
        m_Builder.startObject(fieldName);
        return this;
    }

    @Override
    public StorageSerialiser endObject() throws IOException
    {
        m_Builder.endObject();
        return this;
    }

    @Override
    public StorageSerialiser startList(String name) throws IOException
    {
        m_Builder.startArray(name);
        return this;
    }

    @Override
    public StorageSerialiser endList() throws IOException
    {
        m_Builder.endArray();
        return this;
    }

    @Override
    public StorageSerialiser addTimestamp(Date value) throws IOException
    {
        m_Builder.field(ElasticsearchMappings.ES_TIMESTAMP, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Object value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, int value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, long value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, boolean value) throws IOException
    {
        m_Builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String... values) throws IOException
    {
        m_Builder.field(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double... values) throws IOException
    {
        m_Builder.field(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Map<String, Object> map) throws IOException
    {
        m_Builder.field(name, map);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Collection<? extends StorageSerialisable> values)
            throws IOException
    {
        m_Builder.startArray(name);
        for (StorageSerialisable value : values)
        {
            m_Builder.startObject();
            value.serialise(this);
            m_Builder.endObject();
        }
        m_Builder.endArray();
        return this;
    }

    @Override
    public StorageSerialiser serialise(StorageSerialisable value) throws IOException
    {
        value.serialise(this);
        return this;
    }

    @Override
    public DotNotationReverser newDotNotationReverser()
    {
        return new ElasticsearchDotNotationReverser();
    }

}
