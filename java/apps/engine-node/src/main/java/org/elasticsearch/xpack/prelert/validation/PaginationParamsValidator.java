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

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

public class PaginationParamsValidator {
    /**
     * This is a limit imposed by elasticsearch since version 2.1.0.
     * The reason is to avoid loading too many documents in memory.
     */
    private static final int MAX_SKIP_TAKE_SUM = 10000;
    private static final String MAX_SKIP_TAKE_SUM_STRING = "10,000";

    private final int skip;
    private final int take;

    public PaginationParamsValidator(int skip, int take) {
        this.skip = skip;
        this.take = take;
    }

    public void validate() {
        if (skip < 0) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.REST_INVALID_SKIP), ErrorCodes.INVALID_SKIP_PARAM);
        }
        if (take < 0) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.REST_INVALID_TAKE), ErrorCodes.INVALID_TAKE_PARAM);
        }
        if (skip + take > MAX_SKIP_TAKE_SUM) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(
                    Messages.REST_INVALID_SKIP_TAKE_SUM, MAX_SKIP_TAKE_SUM_STRING), ErrorCodes.INVALID_TAKE_PARAM);
        }
    }
}
