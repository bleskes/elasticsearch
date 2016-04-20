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

package com.prelert.job;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ElasticsearchDataSourceCompatibilityTest
{
    @Test
    public void testFrom_GivenValidOptions()
    {
        assertEquals(ElasticsearchDataSourceCompatibility.V_1_7_X,
                ElasticsearchDataSourceCompatibility.from("1.7.x"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_1_7_X,
                ElasticsearchDataSourceCompatibility.from("1.7.X"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_2_X_X,
                ElasticsearchDataSourceCompatibility.from("2.x.x"));
        assertEquals(ElasticsearchDataSourceCompatibility.V_2_X_X,
                ElasticsearchDataSourceCompatibility.from("2.X.X"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFrom_GivenNull()
    {
        ElasticsearchDataSourceCompatibility.from(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFrom_GivenInvalidOption()
    {
        ElasticsearchDataSourceCompatibility.from("invalid");
    }
}
