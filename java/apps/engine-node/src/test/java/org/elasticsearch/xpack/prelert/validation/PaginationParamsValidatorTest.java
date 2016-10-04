/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.validation;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

public class PaginationParamsValidatorTest extends ESTestCase {

    public void testValidate_GivenSkipIsMinusOne() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PaginationParamsValidator(-1, 100).validate());
        assertEquals("Parameter 'skip' cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_SKIP_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenSkipIsMinusTen() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PaginationParamsValidator(-10, 100).validate());
        assertEquals("Parameter 'skip' cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_SKIP_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenTakeIsMinusOne() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PaginationParamsValidator(0, -1).validate());
        assertEquals("Parameter 'take' cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenTakeIsMinusHundred() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PaginationParamsValidator(0, -100).validate());
        assertEquals("Parameter 'take' cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenSkipAndTakeSumIsMoreThan10000() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class, () -> new PaginationParamsValidator(0, 10001).validate());
        assertEquals("The sum of parameters 'skip' and 'take' cannot be higher than 10,000. " +
                "Please use filters to reduce the number of results.", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testValidate_GivenSkipAndTakeAreValid() {
        new PaginationParamsValidator(0, 0).validate();
        new PaginationParamsValidator(100, 0).validate();
        new PaginationParamsValidator(0, 10000).validate();
        new PaginationParamsValidator(50, 500).validate();
    }
}
