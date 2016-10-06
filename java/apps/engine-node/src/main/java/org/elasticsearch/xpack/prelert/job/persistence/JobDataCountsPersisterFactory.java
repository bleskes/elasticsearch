
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;

public interface JobDataCountsPersisterFactory
{
    /**
     * Get an instance of a {@linkplain JobDataCountsPersister}
     * @return
     */
    JobDataCountsPersister getInstance(Logger logger);
}
