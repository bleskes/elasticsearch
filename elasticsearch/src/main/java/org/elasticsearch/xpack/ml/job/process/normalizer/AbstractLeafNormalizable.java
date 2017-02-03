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

import java.util.Collections;
import java.util.List;

abstract class AbstractLeafNormalizable extends Normalizable {

    AbstractLeafNormalizable(String indexName) {
        super(indexName);
    }

    @Override
    public final boolean isContainerOnly() {
        return false;
    }

    @Override
    public final List<ChildType> getChildrenTypes() {
        return Collections.emptyList();
    }

    @Override
    public final List<Normalizable> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public final List<Normalizable> getChildren(ChildType type) {
        throw new IllegalStateException(getClass().getSimpleName() + " has no children");
    }

    @Override
    public final boolean setMaxChildrenScore(ChildType childrenType, double maxScore) {
        throw new IllegalStateException(getClass().getSimpleName() + " has no children");
    }
}
