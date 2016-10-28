
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;

public interface UsagePersisterFactory
{
    /**
     * Get an instance of a {@linkplain UsagePersister}
     */
    UsagePersister getInstance(Logger logger);
}
