
package org.elasticsearch.xpack.prelert.job.persistence;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;

import org.elasticsearch.xpack.prelert.job.audit.AuditActivity;
import org.elasticsearch.xpack.prelert.job.audit.AuditMessage;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;

public class ElasticsearchAuditor implements Auditor
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchAuditor.class);

    private final Client client;
    private final String index;
    private final String jobId;

    public ElasticsearchAuditor(Client client, String index, String jobId)
    {
        this.client = Objects.requireNonNull(client);
        this.index = index;
        this.jobId = jobId;
    }

    @Override
    public void info(String message)
    {
        persistAuditMessage(AuditMessage.newInfo(jobId, message));
    }

    @Override
    public void warning(String message)
    {
        persistAuditMessage(AuditMessage.newWarning(jobId, message));
    }

    @Override
    public void error(String message)
    {
        persistAuditMessage(AuditMessage.newError(jobId, message));
    }

    @Override
    public void activity(String message)
    {
        persistAuditMessage(AuditMessage.newActivity(jobId, message));
    }

    @Override
    public void activity(int totalJobs, int totalDetectors, int runningJobs, int runningDetectors)
    {
        persistAuditActivity(AuditActivity.newActivity(totalJobs, totalDetectors, runningJobs, runningDetectors));
    }

    private void persistAuditMessage(AuditMessage message)
    {
        try
        {
            client.prepareIndex(index, AuditMessage.TYPE)
                    .setSource(serialiseMessage(message))
                    .execute().actionGet();
        }
        catch (IOException | IndexNotFoundException e)
        {
            LOGGER.error("Error writing auditMessage", e);
        }
    }

    private void persistAuditActivity(AuditActivity activity)
    {
        try
        {
            client.prepareIndex(index, AuditActivity.TYPE)
                    .setSource(serialiseActivity(activity))
                    .execute().actionGet();
        }
        catch (IOException | IndexNotFoundException e)
        {
            LOGGER.error("Error writing auditActivity", e);
        }
    }

    private XContentBuilder serialiseMessage(AuditMessage message) throws IOException
    {
        return jsonBuilder().startObject()
                .field(ElasticsearchMappings.ES_TIMESTAMP, message.getTimestamp())
                .field(AuditMessage.JOB_ID, message.getJobId())
                .field(AuditMessage.LEVEL, message.getLevel())
                .field(AuditMessage.MESSAGE, message.getMessage())
                .endObject();
    }

    private XContentBuilder serialiseActivity(AuditActivity activity) throws IOException
    {
        return jsonBuilder().startObject()
                .field(ElasticsearchMappings.ES_TIMESTAMP, activity.getTimestamp())
                .field(AuditActivity.TOTAL_JOBS, activity.getTotalJobs())
                .field(AuditActivity.TOTAL_DETECTORS, activity.getTotalDetectors())
                .field(AuditActivity.RUNNING_JOBS, activity.getRunningJobs())
                .field(AuditActivity.RUNNING_DETECTORS, activity.getRunningDetectors())
                .endObject();
    }
}
