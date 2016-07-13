/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.manager;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prelert.job.persistence.JobDetailsProvider;
import com.prelert.settings.PrelertSettings;

class BackendInfo
{
    private static final Logger LOGGER = Logger.getLogger(BackendInfo.class);

    /**
     * constraints in the license key.
     */
    public static final String JOBS_LICENSE_CONSTRAINT = "jobs";
    public static final String DETECTORS_LICENSE_CONSTRAINT = "detectors";
    public static final String PARTITIONS_LICENSE_CONSTRAINT = "partitions";

    private static final String MAX_JOBS_FACTOR_NAME = "max.jobs.factor";
    private static final double DEFAULT_MAX_JOBS_FACTOR = 6.0;

    /**
     * Field name in which to store the API version in the usage info
     */
    private static final String APP_VER_FIELDNAME = "appVer";

    /**
     * These default to unlimited (indicated by negative limits), but may be
     * overridden by constraints in the license key.
     */
    private final int m_LicenceJobLimit;
    private final int m_MaxRunningDetectors;

    private final int m_MaxJobsAllowedCpuLimited;

    /**
     * The constraint on whether partition fields are allowed.
     * See https://anomaly.atlassian.net/wiki/display/EN/Electronic+license+keys
     * and bug 1034 in Bugzilla for background.
     */
    private final boolean m_ArePartitionsAllowed;

    private BackendInfo(int licenseJobLimit, int licenseDetectorLimit, boolean arePartitionsAllowed)
    {
        m_LicenceJobLimit = licenseJobLimit;
        m_MaxRunningDetectors = licenseDetectorLimit;
        m_ArePartitionsAllowed = arePartitionsAllowed;

        m_MaxJobsAllowedCpuLimited = calculateMaxJobsAllowedCpuLimited();
    }

    /**
     * Reads usage and license info from a JSON string. It also adds the API version
     * and stores that info into the storage.
     *
     * @param json The JSON string as returned by the back-end
     * @param jobProvider A job provider capable of saving the info into the storage
     * @param apiVersion The API version
     * @return The {@code BackendInfo}
     */
    public static BackendInfo fromJson(String json, JobDetailsProvider jobProvider, String apiVersion)
    {
        int licenseJobLimit = -1;
        int licenseDetectorLimit = -1;
        boolean arePartitionsAllowed = true;

        // Try to parse the string returned from the C++ process and extract
        // any license constraints
        ObjectNode doc = parseJson(json);
        if (doc == null)
        {
            return new BackendInfo(licenseJobLimit, licenseDetectorLimit, arePartitionsAllowed);
        }

        // Negative numbers indicate no constraint, i.e. unlimited maximums
        JsonNode constraint = doc.get(JOBS_LICENSE_CONSTRAINT);
        if (constraint != null)
        {
            licenseJobLimit = constraint.asInt(-1);
        }
        LOGGER.info("License job limit = " + licenseJobLimit);

        constraint = doc.get(DETECTORS_LICENSE_CONSTRAINT);
        if (constraint != null)
        {
            licenseDetectorLimit = constraint.asInt(-1);
        }
        LOGGER.info("License detector limit = " + licenseDetectorLimit);

        constraint = doc.get(PARTITIONS_LICENSE_CONSTRAINT);
        if (constraint != null)
        {
            int val = constraint.asInt(-1);
            // See https://anomaly.atlassian.net/wiki/display/EN/Electronic+license+keys
            // and bug 1034 in Bugzilla for the reason behind this
            // seemingly weird condition.
            arePartitionsAllowed = val < 0;
        }
        LOGGER.info("Are partitions allowed = " + arePartitionsAllowed);

        // Try to add extra fields (just appVer for now)
        doc.put(APP_VER_FIELDNAME, apiVersion);

        // Try to persist the modified document
        try
        {
            jobProvider.savePrelertInfo(doc.toString());
            LOGGER.debug("Persisted Prelert info: " + doc.toString());
        }
        catch (Exception e)
        {
            LOGGER.warn("Error persisting Prelert info", e);
        }

        return new BackendInfo(licenseJobLimit, licenseDetectorLimit, arePartitionsAllowed);
    }

    private static ObjectNode parseJson(String input)
    {
        try
        {
            return (ObjectNode) new ObjectMapper().readTree(input);
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to parse JSON document " + input, e);
        }
        catch (ClassCastException e)
        {
            LOGGER.warn("Parsed non-object JSON document " + input, e);
        }
        return null;
    }

    private int calculateMaxJobsAllowedCpuLimited()
    {
        int cores = Runtime.getRuntime().availableProcessors();
        double factor = PrelertSettings.getSettingOrDefault(MAX_JOBS_FACTOR_NAME,
                DEFAULT_MAX_JOBS_FACTOR);
        return (int) Math.ceil(cores * factor);
    }

    /**
     * @return The job limit or -1 when unlimited
     */
    public int getLicenseJobLimit()
    {
        return m_LicenceJobLimit;
    }

    /**
     * @return the max number of running detectors for all jobs
     */
    public int getMaxRunningDetectors()
    {
        return m_MaxRunningDetectors;
    }

    /**
     * The maximum number of jobs allowed as function of the number of CPU cores.
     * This is different to the licence limit, the limit is based on the machine's
     * hardware
     * @return
     */
    public int getMaxJobsAllowedCpuLimited()
    {
        return m_MaxJobsAllowedCpuLimited;
    }

    public boolean arePartitionsAllowed()
    {
        return m_ArePartitionsAllowed;
    }

    public boolean isLicenseDetectorLimitViolated(int numberOfRunningDetectors, int newDetectors)
    {
        return m_MaxRunningDetectors >= 0 &&
                    (numberOfRunningDetectors + newDetectors) > m_MaxRunningDetectors;
    }

    public boolean isLicenseJobLimitViolated(int numberOfRunningJobs)
    {
        return m_LicenceJobLimit >= 0 && numberOfRunningJobs >= m_LicenceJobLimit;
    }

    public boolean isCpuLimitViolated(int numberOfRunningJobs)
    {
        return numberOfRunningJobs >= m_MaxJobsAllowedCpuLimited;
    }


}
