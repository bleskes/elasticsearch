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
package com.prelert.job.persistence;

import java.util.Optional;

import com.prelert.job.ListDocument;

/**
 * The persistence interface for managing lists.
 */
public interface ListProvider
{
    /**
     * Save the new list to the datastore.
     * @param list the list to be persisted
     * @return <tt>true</tt> if the list gets created successfully
     */
    boolean createList(ListDocument list);

    /**
     * Retrieves the list with the given {@code listId} from the datastore.
     * @param listId the id of the requested list
     * @return the matching list if it exists
     */
    Optional<ListDocument> getList(String listId);

    /**
     * Retrieves all lists.
     * @return
     */
    QueryPage<ListDocument> getLists();
}
