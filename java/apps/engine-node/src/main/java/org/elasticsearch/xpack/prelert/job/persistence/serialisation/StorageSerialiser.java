
package org.elasticsearch.xpack.prelert.job.persistence.serialisation;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public interface StorageSerialiser
{
    StorageSerialiser startObject() throws IOException;
    StorageSerialiser startObject(String fieldName) throws IOException;
    StorageSerialiser endObject() throws IOException;
    StorageSerialiser startList(String name) throws IOException;
    StorageSerialiser endList() throws IOException;
    StorageSerialiser addTimestamp(Date value) throws IOException;
    StorageSerialiser add(String name, Object value) throws IOException;
    StorageSerialiser add(String name, String value) throws IOException;
    StorageSerialiser add(String name, int value) throws IOException;
    StorageSerialiser add(String name, long value) throws IOException;
    StorageSerialiser add(String name, double value) throws IOException;
    StorageSerialiser add(String name, boolean value) throws IOException;
    StorageSerialiser add(String name, String... values) throws IOException;
    StorageSerialiser add(String name, double... values) throws IOException;
    StorageSerialiser add(String name, Map<String, Object> map) throws IOException;
    StorageSerialiser add(String name, Collection<? extends StorageSerialisable> values) throws IOException;

    StorageSerialiser serialise(StorageSerialisable value) throws IOException;
    DotNotationReverser newDotNotationReverser();
    StorageSerialiser addReverserResults(DotNotationReverser reverser) throws IOException;
}
