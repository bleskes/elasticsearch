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
package com.prelert.job.process.output.parsing;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.utils.json.FieldNameParser;

final class ModelSnapshotParser extends FieldNameParser<ModelSnapshot>
{
    private static final Logger LOGGER = Logger.getLogger(ModelSnapshotParser.class);

    public ModelSnapshotParser(JsonParser jsonParser)
    {
        super("ModelSnapshot", jsonParser, LOGGER);
    }

    @Override
    protected ModelSnapshot supply()
    {
        return new ModelSnapshot();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelSnapshot modelSnapshot) throws IOException
    {
        JsonToken token = m_Parser.nextToken();
        switch (fieldName)
        {
        case ModelSnapshot.TIMESTAMP:
            modelSnapshot.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case ModelSnapshot.DESCRIPTION:
            modelSnapshot.setDescription(parseAsStringOrNull(fieldName));
            break;
        case ModelSnapshot.RESTORE_PRIORITY:
            modelSnapshot.setRestorePriority(parseAsLongOrZero(fieldName));
            break;
        case ModelSnapshot.SNAPSHOT_ID:
            modelSnapshot.setSnapshotId(parseAsStringOrNull(fieldName));
            break;
        case ModelSnapshot.SNAPSHOT_DOC_COUNT:
            modelSnapshot.setSnapshotDocCount(parseAsIntOrZero(fieldName));
            break;
        case ModelSizeStats.TYPE:
            modelSnapshot.setModelSizeStats(new ModelSizeStatsParser(m_Parser).parseJson());
            break;
        case ModelSnapshot.LATEST_RECORD_TIME:
            modelSnapshot.setLatestRecordTimeStamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case ModelSnapshot.LATEST_RESULT_TIME:
            modelSnapshot.setLatestResultTimeStamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        default:
            LOGGER.warn(String.format("Parse error unknown field in ModelSnapshot %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
