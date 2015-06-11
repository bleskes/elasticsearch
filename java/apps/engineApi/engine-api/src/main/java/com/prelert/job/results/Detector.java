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
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.utils.json.AutoDetectParseException;
import com.prelert.utils.json.FieldNameParser;

/**
 * Represents the anomaly detector.
 * Only the detector name is serialised anomaly records aren't.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties({"records"})
public class Detector
{
    public static final String TYPE = "detector";
    public static final String NAME = "name";
    public static final String RECORDS = "records";

    private static final Logger LOGGER = Logger.getLogger(Detector.class);

    private String m_Name;
    private List<AnomalyRecord> m_Records;


    public Detector()
    {
        m_Records = new ArrayList<>();
    }

    public Detector(String name)
    {
        this();
        m_Name = name.intern();
    }

    public String getName()
    {
        return m_Name;
    }

    private void setName(String name)
    {
        m_Name = name.intern();
    }

    public void addRecord(AnomalyRecord record)
    {
        m_Records.add(record);
    }

    public List<AnomalyRecord> getRecords()
    {
        return m_Records;
    }


    /**
     * Create a new <code>Detector</code> and populate it from the JSON parser.
     * The parser must be pointing at the start of the object then all the object's
     * fields are read and if they match the property names the appropriate
     * members are set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     *
     * @param parser The JSON Parser should be pointing to the start of the object,
     * when the function returns it will be pointing to the end.
     * @return The new Detector
     * @throws JsonParseException
     * @throws IOException
     * @throws AutoDetectParseException
     */
    public static Detector parseJson(JsonParser parser)
    throws JsonParseException, IOException, AutoDetectParseException
    {
        Detector detector = new Detector();
        DetectorJsonParser detectorJsonParser = new DetectorJsonParser(parser, LOGGER);
        detectorJsonParser.parse(detector);
        return detector;
    }

    private static class DetectorJsonParser extends FieldNameParser<Detector>
    {
        public DetectorJsonParser(JsonParser jsonParser, Logger logger)
        {
            super("Detector", jsonParser, logger);
        }

        @Override
        protected void handleFieldName(String fieldName, Detector detector)
                throws AutoDetectParseException, JsonParseException, IOException
        {
            JsonToken token = m_Parser.nextToken();
            switch (fieldName)
            {
            case NAME:
                detector.setName(parseAsStringOrNull(token, fieldName));
                break;
            case RECORDS:
                if (token == JsonToken.START_ARRAY)
                {
                    token = m_Parser.nextToken();
                    while (token != JsonToken.END_ARRAY)
                    {
                        AnomalyRecord record = AnomalyRecord.parseJson(m_Parser);
                        detector.addRecord(record);

                        token = m_Parser.nextToken();
                    }
                }
                else
                {
                    LOGGER.warn("Expected the start of an array for field '"
                                + fieldName + "' not " + m_Parser.getText());
                }
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in detector %s:%s",
                        fieldName, token.asString()));
                break;
            }
        }
    }
}
