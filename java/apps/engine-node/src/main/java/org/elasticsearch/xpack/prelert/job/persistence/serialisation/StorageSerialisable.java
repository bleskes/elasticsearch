
package org.elasticsearch.xpack.prelert.job.persistence.serialisation;

import java.io.IOException;

public interface StorageSerialisable
{
    void serialise(StorageSerialiser serialiser) throws IOException;
}
