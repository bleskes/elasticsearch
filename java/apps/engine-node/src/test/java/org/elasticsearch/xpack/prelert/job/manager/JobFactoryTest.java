
package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JobFactoryTest extends ESTestCase {


    public void testGenerateJobId_doesnotIncludeHost()
    {
        Pattern pattern = Pattern.compile("[0-9]{14}-[0-9]{5}");

        JobFactory factory = new JobFactory();
        String id = factory.generateJobId();

        assertTrue(pattern.matcher(id).matches());
    }


    public void testGenerateJobId_IncludesHost()
    {
        Pattern pattern = Pattern.compile("[0-9]{14}-server-1-[0-9]{5}");

        JobFactory factory = new JobFactory("server-1");
        String id = factory.generateJobId();

        assertTrue(pattern.matcher(id).matches());
    }


    public void testGenerateJobId_isShorterThanMaxHJobLength()
    {
        JobFactory factory = new JobFactory();
        assertTrue(factory.generateJobId().length() < JobConfigurationVerifier.MAX_JOB_ID_LENGTH);
    }


    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname()
    {
        JobFactory factory = new JobFactory("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        String id = factory.generateJobId();
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
        assertTrue(id.endsWith("-00001"));
    }


    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname_andSixDigitSequence()
    {
        JobFactory factory = new JobFactory("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        String id = null;
        for (int i = 0; i < 100000; i++)
        {
            id = factory.generateJobId();
        }
        assertTrue(id.endsWith("-100000"));
        assertEquals(JobConfigurationVerifier.MAX_JOB_ID_LENGTH, id.length());
    }
}
