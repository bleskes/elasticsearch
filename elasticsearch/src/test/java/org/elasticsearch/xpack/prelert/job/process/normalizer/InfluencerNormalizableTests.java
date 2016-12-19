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
    @Before
    public void setUpInfluencer() {
        influencer = new Influencer("foo", "airline", "AAL", new Date(), 600, 1);
        influencer.setAnomalyScore(1.0);
        influencer.setInitialAnomalyScore(2.0);
        influencer.setProbability(0.05);
    }

    public void testIsContainerOnly() {
        assertFalse(new InfluencerNormalizable(influencer).isContainerOnly());
    }

    public void testGetLevel() {
        assertEquals(Level.INFLUENCER, new InfluencerNormalizable(influencer).getLevel());
    }

    public void testGetPartitionFieldName() {
        assertNull(new InfluencerNormalizable(influencer).getPartitionFieldName());
    }

    public void testGetPartitionFieldValue() {
        assertNull(new InfluencerNormalizable(influencer).getPartitionFieldValue());
    }

    public void testGetPersonFieldName() {
        assertEquals("airline", new InfluencerNormalizable(influencer).getPersonFieldName());
    }

    public void testGetFunctionName() {
        assertNull(new InfluencerNormalizable(influencer).getFunctionName());
    }

    public void testGetValueFieldName() {
        assertNull(new InfluencerNormalizable(influencer).getValueFieldName());
    }

    public void testGetProbability() {
        assertEquals(0.05, new InfluencerNormalizable(influencer).getProbability(), EPSILON);
    }

    public void testGetNormalizedScore() {
        assertEquals(1.0, new InfluencerNormalizable(influencer).getNormalizedScore(), EPSILON);
    }

    public void testSetNormalizedScore() {
        InfluencerNormalizable normalizable = new InfluencerNormalizable(influencer);

        normalizable.setNormalizedScore(99.0);

        assertEquals(99.0, normalizable.getNormalizedScore(), EPSILON);
        assertEquals(99.0, influencer.getAnomalyScore(), EPSILON);
    }

    public void testGetChildrenTypes() {
        assertTrue(new InfluencerNormalizable(influencer).getChildrenTypes().isEmpty());
    }

    public void testGetChildren_ByType() {
        expectThrows(IllegalStateException.class, () -> new InfluencerNormalizable(influencer).getChildren(0));
    }

    public void testGetChildren() {
        assertTrue(new InfluencerNormalizable(influencer).getChildren().isEmpty());
    }

    public void testSetMaxChildrenScore() {
        expectThrows(IllegalStateException.class, () -> new InfluencerNormalizable(influencer).setMaxChildrenScore(0, 42.0));
    }

    public void testSetParentScore() {
        expectThrows(IllegalStateException.class, () -> new InfluencerNormalizable(influencer).setParentScore(42.0));
    }

    public void testResetBigChangeFlag() {
        InfluencerNormalizable normalizable = new InfluencerNormalizable(influencer);
        normalizable.raiseBigChangeFlag();

        normalizable.resetBigChangeFlag();

        assertFalse(influencer.hadBigNormalizedUpdate());
    }

    public void testRaiseBigChangeFlag() {
        InfluencerNormalizable normalizable = new InfluencerNormalizable(influencer);
        normalizable.resetBigChangeFlag();

        normalizable.raiseBigChangeFlag();

        assertTrue(influencer.hadBigNormalizedUpdate());
    }
}
