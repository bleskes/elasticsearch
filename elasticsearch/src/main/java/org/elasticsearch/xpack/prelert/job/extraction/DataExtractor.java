/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.extraction;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface DataExtractor {
    /**
     * Set-up the extractor for a new search
     *
     * @param start start time
     * @param end end time
     * @param logger logger
     */
    void newSearch(long start, long end, Logger logger) throws IOException;

    /**
     * Cleans up after a search.
     */
    void clear();

    /**
     * @return {@code true} if the search has not finished yet, or {@code false} otherwise
     */
    boolean hasNext();

    /**
     * Returns the next available extracted data. Note that it is possible for the
     * extracted data to be empty the last time this method can be called.
     * @return an optional input stream with the next available extracted data
     * @throws IOException if an error occurs while extracting the data
     */
    Optional<InputStream> next() throws IOException;

    /**
     * Cancel the current search.
     */
    void cancel();
}
