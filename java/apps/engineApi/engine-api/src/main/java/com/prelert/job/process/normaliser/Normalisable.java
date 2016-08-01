/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.process.normaliser;

import java.util.List;

interface Normalisable
{
    /**
     * A {@code Normalisable} may be the owner of scores or just a
     * container of other {@code Normalisable} objects. A container only
     * {@code Normalisable} does not have any scores to be normalised.
     * It contains scores that are aggregates of its children.
     *
     * @return true if this {@code Normalisable} is only a container
     *
     */
    boolean isContainerOnly();

    Level getLevel();
    String getPartitionFieldName();
    String getPartitionFieldValue();
    String getPersonFieldName();
    String getFunctionName();
    String getValueFieldName();
    double getProbability();
    double getNormalisedScore();
    void setNormalisedScore(double normalisedScore);
    List<Integer> getChildrenTypes();
    List<Normalisable> getChildren();
    List<Normalisable> getChildren(int type);

    /**
     * Set the aggregate normalised score for a type of children
     *
     * @param childrenType the integer that corresponds to a children type
     * @param maxScore the aggregate normalised score of the children
     * @return true if the score has changed or false otherwise
     */
    boolean setMaxChildrenScore(int childrenType, double maxScore);

    /**
     * If this {@code Normalisable} holds the score of its parent,
     * set the parent score
     *
     * @param parentScore the score of the parent {@code Normalisable}
     */
    void setParentScore(double parentScore);

    void resetBigChangeFlag();
    void raiseBigChangeFlag();
}
