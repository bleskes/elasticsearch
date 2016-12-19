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
        instance.dataCounts(randomBoolean());
        instance.modelSizeStats(randomBoolean());
        instance.schedulerStatus(randomBoolean());
        instance.status(randomBoolean());
        if (randomBoolean()) {
            int from = randomInt(PageParams.MAX_FROM_SIZE_SUM);
            int maxSize = PageParams.MAX_FROM_SIZE_SUM - from;
            int size = randomInt(maxSize);
            instance.setPageParams(new PageParams(from, size));
        } else {
            instance.setJobId(randomAsciiOfLengthBetween(1, 20));
        }
        return instance;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }

}
