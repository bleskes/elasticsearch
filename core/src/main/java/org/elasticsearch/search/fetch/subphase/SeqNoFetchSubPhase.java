/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.fetch.subphase;

import org.apache.lucene.index.NumericDocValues;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.mapper.SeqNoFieldMapper;
import org.elasticsearch.index.seqno.SequenceNumbersService;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;

public final class SeqNoFetchSubPhase implements FetchSubPhase {

    @Override
    public void hitExecute(SearchContext context, HitContext hitContext) {
        // TODO: give it's flag
        if (context.version() == false ||
            (context.storedFieldsContext() != null && context.storedFieldsContext().fetchFields() == false)) {
            return;
        }
        long seqNo = SequenceNumbersService.UNASSIGNED_SEQ_NO;
        long term  = -1;
        try {
            NumericDocValues docValues = hitContext.reader().getNumericDocValues(SeqNoFieldMapper.NAME);
            if (docValues != null) {
                seqNo = docValues.get(hitContext.docId());
            }
            docValues = hitContext.reader().getNumericDocValues(SeqNoFieldMapper.PRIMARY_TERM_NAME);
            if (docValues != null) {
                term = docValues.get(hitContext.docId());
            }
        } catch (IOException e) {
            throw new ElasticsearchException("Could not retrieve sequence numbers", e);
        }
        hitContext.hit().setSeqNoAndTerm(seqNo, term);
    }
}
