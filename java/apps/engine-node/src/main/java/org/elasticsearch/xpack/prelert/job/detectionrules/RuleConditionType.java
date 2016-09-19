/****************************************************************************
 *                                                                          *
 * Copyright 2016-2016 Prelert Ltd                                          *
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

package org.elasticsearch.xpack.prelert.job.detectionrules;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RuleConditionType
{
    CATEGORICAL, NUMERICAL_ACTUAL, NUMERICAL_TYPICAL, NUMERICAL_DIFF_ABS;

    /**
     * Case-insensitive from string method.
     *
     * @param value String representation
     * @return The condition type
     */
    @JsonCreator
    public static RuleConditionType forString(String value)
    {
        return RuleConditionType.valueOf(value.toUpperCase());
    }
}
