
package org.elasticsearch.xpack.prelert.job.persistence;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.common.xcontent.XContentBuilder;

import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

class ElasticsearchStorageSerialiser implements StorageSerialiser
{
    private final XContentBuilder builder;

    public ElasticsearchStorageSerialiser(XContentBuilder builder)
    {
        this.builder = Objects.requireNonNull(builder);
    }

    @Override
    public StorageSerialiser startObject() throws IOException
    {
        builder.startObject();
        return this;
    }

    @Override
    public StorageSerialiser startObject(String fieldName) throws IOException
    {
        builder.startObject(fieldName);
        return this;
    }

    @Override
    public StorageSerialiser endObject() throws IOException
    {
        builder.endObject();
        return this;
    }

    @Override
    public StorageSerialiser startList(String name) throws IOException
    {
        builder.startArray(name);
        return this;
    }

    @Override
    public StorageSerialiser endList() throws IOException
    {
        builder.endArray();
        return this;
    }

    @Override
    public StorageSerialiser addTimestamp(Date value) throws IOException
    {
        builder.field(ElasticsearchMappings.ES_TIMESTAMP, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Object value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, int value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, long value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, boolean value) throws IOException
    {
        builder.field(name, value);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, String... values) throws IOException
    {
        builder.field(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, double... values) throws IOException
    {
        builder.field(name, values);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Map<String, Object> map) throws IOException
    {
        builder.field(name, map);
        return this;
    }

    @Override
    public StorageSerialiser add(String name, Collection<? extends StorageSerialisable> values)
            throws IOException
    {
        builder.startArray(name);
        for (StorageSerialisable value : values)
        {
            builder.startObject();
            value.serialise(this);
            builder.endObject();
        }
        builder.endArray();
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

    @Override
    public StorageSerialiser addReverserResults(DotNotationReverser reverser) throws IOException
    {
        for (Map.Entry<String, Object> entry : reverser.getResultsMap().entrySet())
        {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

}
