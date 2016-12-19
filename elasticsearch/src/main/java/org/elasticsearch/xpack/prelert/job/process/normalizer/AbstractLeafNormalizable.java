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
    public final List<Integer> getChildrenTypes() {
        return Collections.emptyList();
    }

    @Override
    public final List<Normalizable> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public final List<Normalizable> getChildren(int type) {
        throw new IllegalStateException(getClass().getSimpleName() + " has no children");
    }

    @Override
    public final boolean setMaxChildrenScore(int childrenType, double maxScore) {
        throw new IllegalStateException(getClass().getSimpleName() + " has no children");
    }
}
