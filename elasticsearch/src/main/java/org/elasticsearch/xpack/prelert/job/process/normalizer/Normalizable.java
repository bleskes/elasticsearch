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
     */
    boolean isContainerOnly();

    Level getLevel();

    String getPartitionFieldName();

    String getPartitionFieldValue();

    String getPersonFieldName();

    String getFunctionName();

    String getValueFieldName();

    double getProbability();

    double getNormalizedScore();

    void setNormalizedScore(double normalizedScore);

    List<Integer> getChildrenTypes();

    List<Normalizable> getChildren();

    List<Normalizable> getChildren(int type);

    /**
     * Set the aggregate normalized score for a type of children
     *
     * @param childrenType the integer that corresponds to a children type
     * @param maxScore     the aggregate normalized score of the children
     * @return true if the score has changed or false otherwise
     */
    boolean setMaxChildrenScore(int childrenType, double maxScore);

    /**
     * If this {@code Normalizable} holds the score of its parent,
     * set the parent score
     *
     * @param parentScore the score of the parent {@code Normalizable}
     */
    void setParentScore(double parentScore);

    void resetBigChangeFlag();

    void raiseBigChangeFlag();
}
