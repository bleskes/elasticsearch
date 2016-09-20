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

package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Objects;

@JsonIgnoreProperties({"enabled"})
@JsonInclude(Include.NON_NULL)
public class ModelDebugConfig
{
    /**
     * Enum of the acceptable output destinations.
     */
    public enum DebugDestination
    {
        FILE, DATA_STORE;

        /**
         * Case-insensitive from string method.
         * Works with FILE, File, file, etc.
         *
         * @param value String representation
         * @return The output destination
         */
        @JsonCreator
        public static DebugDestination forString(String value)
        {
            String valueUpperCase = value.toUpperCase();
            return DebugDestination.valueOf(valueUpperCase);
        }
    }

    public static final String TYPE = "modelDebugConfig";
    public static final String WRITE_TO = "writeTo";
    public static final String BOUNDS_PERCENTILE = "boundsPercentile";
    public static final String TERMS = "terms";

    private DebugDestination writeTo;
    private Double boundsPercentile;
    private String terms;

    public ModelDebugConfig()
    {
        // NB: this.writeTo defaults to null in this case, otherwise an update to
        // the bounds percentile could switch where the debug is written to
    }

    public ModelDebugConfig(Double boundsPercentile, String terms)
    {
        this.writeTo = DebugDestination.FILE;
        this.boundsPercentile = boundsPercentile;
        this.terms = terms;
    }

    public ModelDebugConfig(DebugDestination writeTo, Double boundsPercentile, String terms)
    {
        this.writeTo = writeTo;
        this.boundsPercentile = boundsPercentile;
        this.terms = terms;
    }

    public DebugDestination getWriteTo()
    {
        return this.writeTo;
    }

    public void setWriteTo(DebugDestination writeTo)
    {
        this.writeTo = writeTo;
    }

    public boolean isEnabled()
    {
        return this.boundsPercentile != null;
    }

    public Double getBoundsPercentile()
    {
        return this.boundsPercentile;
    }

    public void setBoundsPercentile(Double boundsPercentile)
    {
        this.boundsPercentile = boundsPercentile;
    }

    public String getTerms()
    {
        return this.terms;
    }

    public void setTerms(String terms)
    {
        this.terms = terms;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof ModelDebugConfig == false)
        {
            return false;
        }

        ModelDebugConfig that = (ModelDebugConfig) other;
        return Objects.equals(this.writeTo, that.writeTo)
                && Objects.equals(this.boundsPercentile, that.boundsPercentile)
                && Objects.equals(this.terms, that.terms);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.writeTo, boundsPercentile, terms);
    }
}
