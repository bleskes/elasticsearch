
package org.elasticsearch.xpack.prelert.job.errorcodes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ErrorCodeMatcher extends TypeSafeMatcher<HasErrorCode> {

    private ErrorCodes expectedErrorCode;
    private ErrorCodes actualErrorCode;

    public static ErrorCodeMatcher hasErrorCode(ErrorCodes expected) {
        return new ErrorCodeMatcher(expected);
    }

    private ErrorCodeMatcher(ErrorCodes expectedErrorCode) {
        this.expectedErrorCode = expectedErrorCode;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(actualErrorCode)
                .appendText(" was found instead of ")
                .appendValue(expectedErrorCode);
    }

    @Override
    public boolean matchesSafely(HasErrorCode item) {
        actualErrorCode = item.getErrorCode();
        return actualErrorCode.equals(expectedErrorCode);
    }

}