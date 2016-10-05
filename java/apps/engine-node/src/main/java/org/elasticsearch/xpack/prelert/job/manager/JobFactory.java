package org.elasticsearch.xpack.prelert.job.manager;

import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.config.DefaultDetectorDescription;
import org.elasticsearch.xpack.prelert.job.config.verification.JobConfigurationVerifier;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A factory that creates new jobs.
 * If the hostname constructor is used the hostname is converted to
 * lowercase to make it compatible with Elasticsearch index names
 */
class JobFactory
{
    private static final int MIN_SEQUENCE_LENGTH = 5;
    private static final String HOSTNAME_ID_TEMPLATE = "%s-%s-%05d";
    private static final String NO_HOSTNAME_ID_TEMPLATE = "%s-%05d";
    private static final int HOSTNAME_ID_SEPARATORS_LENGTH = 2;

    private final AtomicLong idSequence;
    private final DateTimeFormatter jobIdDateFormat;
    private final String hostname;

    public JobFactory()
    {
        this(null);
    }

    public JobFactory(String hostname)
    {
        this.hostname = hostname == null ? null : hostname.toLowerCase();
        idSequence = new AtomicLong();
        jobIdDateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    }

    /**
     * Creates a new job according to the given configuration.
     * If no {@code id} was specified, a unique id is generated.
     * The method also checks that license related limits are not violated
     * by creating this job.
     *
     * @param jobConfig the job configuration
     * @return the created {@Code JobDetails} object
     * @throws JobConfigurationException
     */
    public JobDetails create(JobConfiguration jobConfig)
            throws JobConfigurationException
    {
        String jobId = jobConfig.getId();
        if (jobId == null || jobId.isEmpty())
        {
            jobId = generateJobId();
        }
        JobDetails jobDetails = new JobDetails(jobId, jobConfig);
        fillDefaults(jobDetails);
        return jobDetails;
    }

    /**
     * If hostname is null the job Id is a concatenation of the date in
     * 'yyyyMMddHHmmss' format and a sequence number that is a minimum of
     * 5 digits wide left padded with zeros<br>
     * If hostname is not null the Id is the concatenation of the date in
     * 'yyyyMMddHHmmss' format the hostname and a sequence number that is a
     * minimum of 5 digits wide left padded with zeros. If hostname is long
     * and it is truncated so the job Id does not exceed the maximum length<br>
     *
     * e.g. the first Id created 23rd November 2013 at 11am
     *     '20131125110000-serverA-00001'
     *
     * @return The new unique job Id
     */
    String generateJobId()
    {
        String dateStr = jobIdDateFormat.format(LocalDateTime.now());
        long sequence = idSequence.incrementAndGet();
        if (hostname != null) {
            int formattedSequenceLen = Math.max(String.valueOf(sequence).length(), MIN_SEQUENCE_LENGTH);
            int hostnameMaxLen = JobConfigurationVerifier.MAX_JOB_ID_LENGTH - dateStr.length()
                    - formattedSequenceLen - HOSTNAME_ID_SEPARATORS_LENGTH;
            String trimmedHostName = hostname.substring(0,
                    Math.min(hostname.length(), hostnameMaxLen));
            return String.format(Locale.ROOT, HOSTNAME_ID_TEMPLATE, dateStr, trimmedHostName, sequence);
        }
        else
        {
            return String.format(Locale.ROOT, NO_HOSTNAME_ID_TEMPLATE, dateStr, sequence);
        }
    }

    private void fillDefaults(JobDetails jobDetails)
    {
        for (Detector detector : jobDetails.getAnalysisConfig().getDetectors())
        {
            if (detector.getDetectorDescription() == null ||
                    detector.getDetectorDescription().isEmpty())
            {
                detector.setDetectorDescription(DefaultDetectorDescription.of(detector));
            }
        }

        // Disable auto-close for scheduled jobs
        if (jobDetails.getSchedulerConfig() != null)
        {
            jobDetails.setTimeout(0);
        }
    }
}
