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
    }

    @Override
    protected PageParams createTestInstance() {
        int from = randomInt(PageParams.MAX_FROM_SIZE_SUM);
        int maxSize = PageParams.MAX_FROM_SIZE_SUM - from;
        int size = randomInt(maxSize);
        return new PageParams(from, size);
    }

    @Override
    protected Reader<PageParams> instanceReader() {
        return PageParams::new;
    }

    public void testValidate_GivenFromIsMinusOne() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(-1, 100));
        assertEquals("Parameter [from] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenFromIsMinusTen() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(-10, 100));
        assertEquals("Parameter [from] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenSizeIsMinusOne() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, -1));
        assertEquals("Parameter [size] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenSizeIsMinusHundred() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, -100));
        assertEquals("Parameter [size] cannot be < 0", e.getMessage());
    }

    public void testValidate_GivenFromAndSizeSumIsMoreThan10000() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new PageParams(0, 10001));
        assertEquals("The sum of parameters [from] and [size] cannot be higher than 10000.", e.getMessage());
    }
}
