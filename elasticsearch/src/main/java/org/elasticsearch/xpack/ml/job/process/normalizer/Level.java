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
package org.elasticsearch.xpack.ml.job.process.normalizer;


/**
 * An enumeration of the different normalization levels.
 * The string value of each level has to match the equivalent
 * level names in the normalizer C++ process.
 */
enum Level {
    ROOT("root"),
    LEAF("leaf"),
    BUCKET_INFLUENCER("inflb"),
    INFLUENCER("infl"),
    PARTITION("part");

    private final String m_Key;

    Level(String key) {
        m_Key = key;
    }

    public String asString() {
        return m_Key;
    }
}
