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
package org.elasticsearch.xpack.ml.job.transform.verification;


import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.job.transform.TransformConfig;

import java.util.List;
import java.util.regex.Pattern;

public class RegexExtractVerifier implements ArgumentVerifier {
    @Override
    public void verify(String arg, TransformConfig tc) {
        new RegexPatternVerifier().verify(arg, tc);

        Pattern pattern = Pattern.compile(arg);
        int groupCount = pattern.matcher("").groupCount();
        List<String> outputs = tc.getOutputs();
        int outputCount = outputs == null ? 0 : outputs.size();
        if (groupCount != outputCount) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_EXTRACT_GROUPS_SHOULD_MATCH_OUTPUT_COUNT,
                    tc.getTransform(), outputCount, arg, groupCount);
            throw new IllegalArgumentException(msg);
        }
    }
}
