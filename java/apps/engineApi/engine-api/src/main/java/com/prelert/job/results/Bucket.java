/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.job.results;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Bucket Result POJO
 */
@JsonIgnoreProperties({"epoch", "detectors"})
@JsonInclude(Include.NON_NULL)
public class Bucket
{
    /*
     * Field Names
     */
    public static final String ID = "id";
    public static final String TIMESTAMP = "timestamp";
    public static final String RAW_ANOMALY_SCORE = "rawAnomalyScore";
    public static final String ANOMALY_SCORE = "anomalyScore";
    public static final String MAX_NORMALIZED_PROBABILITY = "maxNormalizedProbability";
    public static final String IS_INTERIM = "isInterim";
    public static final String RECORD_COUNT = "recordCount";
    public static final String EVENT_COUNT = "eventCount";
    public static final String DETECTORS = "detectors";
    public static final String RECORDS = "records";
    public static final String INFLUENCES = "influences";


    /**
     * Elasticsearch type
     */
    public static final String TYPE = "bucket";

    private static final Logger LOGGER = Logger.getLogger(Bucket.class);

    private Date m_Timestamp;
    private double m_RawAnomalyScore;
    private double m_AnomalyScore;
    private double m_MaxNormalizedProbability;
    private int m_RecordCount;
    private List<Detector> m_Detectors;
    private List<AnomalyRecord> m_Records;
    private long m_EventCount;
    private Boolean m_IsInterim;
    private boolean m_HadBigNormalisedUpdate;
    private List<Influence> m_Influences;

    public Bucket()
    {
        m_Detectors = new ArrayList<>();
        m_Records = Collections.emptyList();
    }

    /**
     * The bucket Id is the bucket's timestamp in seconds
     * from the epoch. As the id is derived from the timestamp
     * field it doesn't need to be serialised, however, in the
     * past it was serialised accidentally, so it still is.
     *
     * @return The bucket id
     */
    public String getId()
    {
        return Long.toString(getEpoch()).intern();
    }

    /**
     * Set the ID and derive the timestamp from it.  It MUST be
     * a number that corresponds to the bucket's timestamp in seconds
     * from the epoch.
     */
    public void setId(String id)
    {
        try
        {
            long epoch = Long.parseLong(id);
            m_Timestamp = new Date(epoch * 1000);
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.error("Could not parse ID " + id + " as a long");
        }
    }

    /**
     * Timestamp expressed in seconds since the epoch (rather than Java's
     * convention of milliseconds).
     */
    public long getEpoch()
    {
        return m_Timestamp.getTime() / 1000;
    }

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }


    public double getRawAnomalyScore()
    {
        return m_RawAnomalyScore;
    }

    public void setRawAnomalyScore(double rawAnomalyScore)
    {
        m_RawAnomalyScore = rawAnomalyScore;
    }

    public double getAnomalyScore()
    {
        return m_AnomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        m_AnomalyScore = anomalyScore;
    }

    public double getMaxNormalizedProbability()
    {
        return m_MaxNormalizedProbability;
    }

    public void setMaxNormalizedProbability(double maxNormalizedProbability)
    {
        m_MaxNormalizedProbability = maxNormalizedProbability;
    }

    public int getRecordCount()
    {
        return m_RecordCount;
    }

    public void setRecordCount(int recordCount)
    {
        m_RecordCount = recordCount;
    }


    /**
     * Get the list of detectors that produced output in this bucket
     *
     * @return A list of detector
     */
    public List<Detector> getDetectors()
    {
        return m_Detectors;
    }

    public void setDetectors(List<Detector> detectors)
    {
        m_Detectors = detectors;
    }


    /**
     * Add a detector that produced output in this bucket
     *
     */
    private void addDetector(Detector detector)
    {
        m_Detectors.add(detector);
    }


    /**
     * Get all the anomaly records associated with this bucket
     * @return All the anomaly records
     */
    public List<AnomalyRecord> getRecords()
    {
        return m_Records;
    }

    public void setRecords(List<AnomalyRecord> records)
    {
        m_Records = records;
    }

    /**
     * The number of records (events) actually processed
     * in this bucket.
     * @return
     */
    public long getEventCount()
    {
        return m_EventCount;
    }

    public void setEventCount(long value)
    {
        m_EventCount = value;
    }

    @JsonProperty("isInterim")
    public Boolean isInterim()
    {
        return m_IsInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(Boolean isInterim)
    {
        m_IsInterim = isInterim;
    }


    /**
     * Create a new <code>Bucket</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new bucket
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Bucket parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Bucket bucket = new Bucket();
        BucketJsonParser bucketJsonParser = new BucketJsonParser(parser, LOGGER);
        bucketJsonParser.parse(bucket);
        setRecordCountToSumOfDetectorsRecords(bucket);
        return bucket;
    }

    /** Set the record count to what was actually read */
    private static void setRecordCountToSumOfDetectorsRecords(Bucket bucket)
    {
        bucket.m_RecordCount = 0;
        for (Detector d : bucket.getDetectors())
        {
            bucket.m_RecordCount += d.getRecords().size();
        }
    }

    public List<Influence> getInfluences()
    {
        return m_Influences;
    }

    public void setInfluences(List<Influence> influences)
    {
        this.m_Influences = influences;
    }

    /**
     * Create a new <code>Bucket</code> and populate it from the JSON parser.
     * The parser must be pointing at the first token inside the object.  It
     * is assumed that prior code has validated that the previous token was
     * the start of an object.  Then all the object's fields are read and if
     * they match the property names the appropriate members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new bucket
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Bucket parseJsonAfterStartObject(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Bucket bucket = new Bucket();
        BucketJsonParser bucketJsonParser = new BucketJsonParser(parser, LOGGER);
        bucketJsonParser.parseAfterStartObject(bucket);
        setRecordCountToSumOfDetectorsRecords(bucket);
        return bucket;
    }

    private static class BucketJsonParser extends FieldNameParser<Bucket>
    {
        public BucketJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Bucket", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, Bucket bucket)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case TIMESTAMP:
                parseTimestamp(bucket, token);
                break;
            case RAW_ANOMALY_SCORE:
                bucket.setRawAnomalyScore(parseAsDoubleOrZero(token, fieldName));
                break;
            case ANOMALY_SCORE:
                bucket.setAnomalyScore(parseAsDoubleOrZero(token, fieldName));
                break;
            case MAX_NORMALIZED_PROBABILITY:
                bucket.setMaxNormalizedProbability(parseAsDoubleOrZero(token, fieldName));
                break;
            case RECORD_COUNT:
                bucket.setRecordCount(parseAsIntOrZero(token, fieldName));
                break;
            case EVENT_COUNT:
                bucket.setEventCount(parseAsLongOrZero(token, fieldName));
                break;
            case IS_INTERIM:
                bucket.setInterim(parseAsBooleanOrNull(token, fieldName));
                break;
            case DETECTORS:
                parseDetectors(token, bucket);
                break;
            case INFLUENCES:
                bucket.setInfluences(Influences.parseJson(m_Parser));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in Bucket %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }

        private void parseTimestamp(Bucket bucket, JsonToken token) throws IOException
        {
            if (token == JsonToken.VALUE_NUMBER_INT)
            {
                // convert seconds to ms
                long val = m_Parser.getLongValue() * 1000;
                bucket.setTimestamp(new Date(val));
            }
            else
            {
                LOGGER.warn("Cannot parse " + TIMESTAMP + " : " + m_Parser.getText()
                                + " as a long");
            }
        }

        private void parseDetectors(JsonToken token, Bucket bucket)
                throws AutoDetectParseException, IOException, JsonParseException
        {
            if (token != JsonToken.START_ARRAY)
            {
                String msg = "Invalid value Expecting an array of detectors";
                LOGGER.warn(msg);
                throw new AutoDetectParseException(msg);
            }

            token = m_Parser.nextToken();
            while (token != JsonToken.END_ARRAY)
            {
                Detector detector = Detector.parseJson(m_Parser);
                bucket.addDetector(detector);

                token = m_Parser.nextToken();
            }
        }
    }

    @Override
    public int hashCode()
    {
        // m_HadBigNormalisedUpdate is deliberately excluded from the hash
        return Objects.hash(m_Timestamp, m_EventCount, m_RawAnomalyScore, m_AnomalyScore,
                m_MaxNormalizedProbability, m_RecordCount, m_Records, m_IsInterim, m_Influences);
    }

    /**
     * Compare all the fields and embedded anomaly records
     * (if any), does not compare detectors as they are not
     * serialized anyway.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Bucket == false)
        {
            return false;
        }

        Bucket that = (Bucket)other;

        // m_HadBigNormalisedUpdate is deliberately excluded from the test
        // don't bother testing detectors
        return Objects.equals(this.m_Timestamp, that.m_Timestamp)
                && (this.m_EventCount == that.m_EventCount)
                && (this.m_RawAnomalyScore == that.m_RawAnomalyScore)
                && (this.m_AnomalyScore == that.m_AnomalyScore)
                && (this.m_MaxNormalizedProbability == that.m_MaxNormalizedProbability)
                && (this.m_RecordCount == that.m_RecordCount)
                && Objects.equals(this.m_Records, that.m_Records)
                && Objects.equals(this.m_IsInterim, that.m_IsInterim)
                && Objects.equals(this.m_Influences, that.m_Influences);
    }


    public boolean hadBigNormalisedUpdate()
    {
        return m_HadBigNormalisedUpdate;
    }


    public void resetBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = true;
    }
}
