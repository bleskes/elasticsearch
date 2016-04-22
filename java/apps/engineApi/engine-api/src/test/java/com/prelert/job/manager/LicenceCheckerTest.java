package com.prelert.job.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.JobProvider;

public class LicenceCheckerTest
{
    @Mock private JobProvider m_JobProvider;
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCheckLicenceViolationsOnCreate_givenValidConditions()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);
        JobConfiguration config = createConfig(2);

        assertTrue(checker.checkLicenceViolationsOnCreate(config.getAnalysisConfig(), 4));
    }

    @Test
    public void testCheckLicenceViolationsOnCreate_givenTooManyJobs()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);
        JobConfiguration config = createConfig(2);

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_JOBS, 5));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));


        checker.checkLicenceViolationsOnCreate(config.getAnalysisConfig(), 5);
    }

    @Test
    public void testCheckLicenceViolationsOnCreate_givenTooManyDetectors()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);
        JobConfiguration config = createConfig(11);

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS, 10, 11));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));


        checker.checkLicenceViolationsOnCreate(config.getAnalysisConfig(), 1);
    }

    @Test
    public void testCheckLicenceViolationsOnCreate_givenUnlicensedPartitions()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, 1);
        JobConfiguration config = createConfig(4);

        // add a partition
        config.getAnalysisConfig().getDetectors().get(0).setPartitionFieldName("partition");

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_PARTITIONS));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));


        checker.checkLicenceViolationsOnCreate(config.getAnalysisConfig(), 1);
    }


    @Test
    public void testCheckLicenceViolationsOnReactivate_givenValidConditions()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);
        assertTrue(checker.checkLicenceViolationsOnReactivate("foo", 4, 2, 8));
    }

    @Test
    public void testCheckLicenceViolationsOnReactivate_givenTooManyJobs()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_JOBS_REACTIVATE, "foo", 5));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        checker.checkLicenceViolationsOnReactivate("foo", 5, 3, 7);
    }

    @Test
    public void testCheckLicenceViolationsOnReactivate_givenTooManyDetectors()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5, 10, -1);

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS_REACTIVATE, "foo", 10));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        checker.checkLicenceViolationsOnReactivate("foo", 4, 4, 7);
    }

    @Test
    public void testCheckLicenceViolationsOnReactivate_givenHardwareLimit()
    throws TooManyJobsException, LicenseViolationException
    {
        LicenceChecker checker = create(5000, 10, -1);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.CPU_LIMIT_JOB, "foo"));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY));

        checker.checkLicenceViolationsOnReactivate("foo", 1000, 3, 7);
    }

    private LicenceChecker create(int maxJobs, int maxDetectors, int partitions)
    {
        String json = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                BackendInfo.JOBS_LICENSE_CONSTRAINT, maxJobs,
                BackendInfo.DETECTORS_LICENSE_CONSTRAINT, maxDetectors,
                BackendInfo.PARTITIONS_LICENSE_CONSTRAINT, partitions);

        return new LicenceChecker(BackendInfo.fromJson(json, m_JobProvider, "2.0.0"));
    }

    private JobConfiguration createConfig(int numDetectors)
    {
        AnalysisConfig ac = new AnalysisConfig();
        List<Detector> detectors = new ArrayList<>();

        for (int i=0; i<numDetectors; i++)
        {
            detectors.add(new Detector());
        }

        ac.setDetectors(detectors);
        return new JobConfiguration(ac);
    }

}
