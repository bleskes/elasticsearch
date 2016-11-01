/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.audit;

import java.util.Date;

public class AuditMessage
{
    public static final String TYPE = "auditMessage";

    public static final String JOB_ID = "jobId";
    public static final String MESSAGE = "message";
    public static final String LEVEL = "level";

    private String jobId;
    private String message;
    private Level level;
    private Date timestamp;

    public AuditMessage()
    {
        // Default constructor
    }

    private AuditMessage(String jobId, String message, Level severity)
    {
        this.jobId = jobId;
        this.message = message;
        level = severity;
        timestamp = new Date();
    }

    public String getJobId()
    {
        return jobId;
    }

    public void setJobId(String jobId)
    {
        this.jobId = jobId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public static AuditMessage newInfo(String jobId, String message)
    {
        return new AuditMessage(jobId, message, Level.INFO);
    }

    public static AuditMessage newWarning(String jobId, String message)
    {
        return new AuditMessage(jobId, message, Level.WARNING);
    }

    public static AuditMessage newActivity(String jobId, String message)
    {
        return new AuditMessage(jobId, message, Level.ACTIVITY);
    }

    public static AuditMessage newError(String jobId, String message)
    {
        return new AuditMessage(jobId, message, Level.ERROR);
    }
}
