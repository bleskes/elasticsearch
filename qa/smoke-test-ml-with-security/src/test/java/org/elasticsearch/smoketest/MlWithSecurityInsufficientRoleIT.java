/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

package org.elasticsearch.smoketest;

import com.carrotsearch.randomizedtesting.annotations.Name;

import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;

public class MlWithSecurityInsufficientRoleIT extends MlWithSecurityIT {

    public MlWithSecurityInsufficientRoleIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @Override
    public void test() throws IOException {
        AssertionError ae = expectThrows(AssertionError.class, super::test);
        assertThat(ae.getMessage(),
                either(containsString("action [cluster:monitor/xpack/ml")).or(containsString("action [cluster:admin/xpack/ml")));
        assertThat(ae.getMessage(), containsString("returned [403 Forbidden]"));
        assertThat(ae.getMessage(), containsString("is unauthorized for user [no_ml]"));
    }

    @Override
    protected String[] getCredentials() {
        return new String[]{"no_ml", "changeme"};
    }
}

