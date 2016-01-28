/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package com.prelert.job.quantiles;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Strings;

/**
 * Quantiles Result POJO
 */
@JsonInclude(Include.NON_NULL)
public class Quantiles
{
    public static final String QUANTILES_ID = "hierarchical";
    public static final String CURRENT_VERSION = "1";

    /**
     * Field Names
     */
    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String TIMESTAMP = "timestamp";
    public static final String QUANTILE_STATE = "quantileState";

    /**
     * Elasticsearch type
     */
    public static final String TYPE = "quantiles";

    private Date m_Timestamp;
    private String m_State;

    public String getId()
    {
        return QUANTILES_ID;
    }

    public String getVersion()
    {
        return CURRENT_VERSION;
    }

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }

    public String getState()
    {
        return Strings.nullToEmpty(m_State);
    }

    public void setState(String state)
    {
        m_State = state;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(m_State);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Quantiles == false)
        {
            return false;
        }

        Quantiles that = (Quantiles) other;

        return Objects.equals(this.m_State, that.m_State);
    }
}

