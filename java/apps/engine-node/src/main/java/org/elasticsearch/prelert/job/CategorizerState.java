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
package org.elasticsearch.prelert.job;


/**
 * The categorizer state does not need to be loaded on the Java side.
 * However, the Java process DOES set up a mapping on the Elasticsearch
 * index to tell Elasticsearch not to analyse the categorizer state documents
 * in any way.
 */
public class CategorizerState {
    /**
     * The type of this class used when persisting the data
     */
    public static final String TYPE = "categorizerState";

    private CategorizerState() {
    }
}

