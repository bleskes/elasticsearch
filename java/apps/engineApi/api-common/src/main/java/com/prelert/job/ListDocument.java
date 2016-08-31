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
package com.prelert.job;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class ListDocument
{
    public static final String TYPE = "list";
    public static final String ID = "id";
    public static final String ITEMS = "items";

    private final String m_Id;
    private final List<String> m_Items;

    @JsonCreator
    public ListDocument(@JsonProperty("id") String id, @JsonProperty("items") List<String> items)
    {
        m_Id = Objects.requireNonNull(id);
        m_Items = items;
    }

    public String getId()
    {
        return m_Id;
    }

    public void addItem(String item)
    {
        if (item != null)
        {
            m_Items.add(item);
        }
    }

    public List<String> getItems()
    {
        return ImmutableList.copyOf(m_Items);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof ListDocument))
        {
            return false;
        }

        ListDocument other = (ListDocument) obj;
        return m_Id.equals(other.m_Id) && m_Items.equals(other.m_Items);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_Id, m_Items);
    }
}
