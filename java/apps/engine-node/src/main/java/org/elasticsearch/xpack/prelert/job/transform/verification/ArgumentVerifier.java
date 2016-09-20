
package org.elasticsearch.xpack.prelert.job.transform.verification;


import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

@FunctionalInterface
public interface ArgumentVerifier
{
    void verify(String argument, TransformConfig tc) throws JobConfigurationException;
}
