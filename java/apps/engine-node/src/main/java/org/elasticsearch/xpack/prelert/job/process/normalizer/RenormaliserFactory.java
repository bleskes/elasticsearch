
package org.elasticsearch.xpack.prelert.job.process.normalizer;


public interface RenormaliserFactory {
    Renormaliser create(String jobId);
}
