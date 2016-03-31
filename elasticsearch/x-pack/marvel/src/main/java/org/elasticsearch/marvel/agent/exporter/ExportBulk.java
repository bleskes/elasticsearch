/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.marvel.agent.exporter;

import java.util.Collection;

/**
 *
 */
public abstract class ExportBulk {

    protected final String name;

    public ExportBulk(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract ExportBulk add(Collection<MonitoringDoc> docs) throws ExportException;

    public abstract void flush() throws ExportException;

    public final void close(boolean flush) throws ExportException {
        ExportException exception = null;
        if (flush) {
            flush();
        }

        // now closing
        try {
            onClose();
        } catch (Exception e) {
            if (exception != null) {
                exception.addSuppressed(e);
            } else {
                exception = new ExportException("Exception when closing export bulk", e);
            }
        }

        // rethrow exception
        if (exception != null) {
            throw exception;
        }
    }

    protected void onClose() throws Exception {
    }

    public static class Compound extends ExportBulk {

        private final Collection<ExportBulk> bulks;

        public Compound(Collection<ExportBulk> bulks) {
            super("all");
            this.bulks = bulks;
        }

        @Override
        public ExportBulk add(Collection<MonitoringDoc> docs) throws ExportException {
            ExportException exception = null;
            for (ExportBulk bulk : bulks) {
                try {
                    bulk.add(docs);
                } catch (ExportException e) {
                    if (exception == null) {
                        exception = new ExportException("failed to add documents to export bulks");
                    }
                    exception.addExportException(e);
                }
            }
            if (exception != null) {
                throw exception;
            }
            return this;
        }

        @Override
        public void flush() throws ExportException {
            ExportException exception = null;
            for (ExportBulk bulk : bulks) {
                try {
                    bulk.flush();
                } catch (ExportException e) {
                    if (exception == null) {
                        exception = new ExportException("failed to flush export bulks");
                    }
                    exception.addExportException(e);
                }
            }
            if (exception != null) {
                throw exception;
            }
        }

        @Override
        protected void onClose() throws Exception {
            ExportException exception = null;
            for (ExportBulk bulk : bulks) {
                try {
                    bulk.onClose();
                } catch (ExportException e) {
                    if (exception == null) {
                        exception = new ExportException("failed to close export bulks");
                    }
                    exception.addExportException(e);
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }
}
