
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;

public interface UsagePersisterFactory
{
    /**
     * Get an instance of a {@linkplain UsagePersister}
     * @return
     */
    UsagePersister getInstance(Logger logger);
}
