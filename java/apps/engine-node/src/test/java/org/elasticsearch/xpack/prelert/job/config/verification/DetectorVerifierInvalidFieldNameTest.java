
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import java.util.Arrays;
import java.util.Collection;

public class DetectorVerifierInvalidFieldNameTest extends ESTestCase {
    private static final String SUFFIX = "suffix";

    private Detector detector;
    private String character;
    private boolean isValid;

    public static Collection<Object[]> getCharactersAndValidity() {
        return Arrays.asList(new Object[][]{
            // char, isValid?
            {"a", true},
            {"[", true},
            {"]", true},
            {"(", true},
            {")", true},
            {"=", true},
            {"-", true},
            {" ", true},
            {"\"", false},
            {"\\", false},
            {"\t", false},
            {"\n", false},
        });
    }


    public void testVerifyFieldNames_givenInvalidChars() {

        Collection<Object[]> testCaseArguments = getCharactersAndValidity();

        for (Object [] args : testCaseArguments) {
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();

            setCharacter((String)args[0]);
            setIsValid((boolean)args[1]);

            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_FieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_ByFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_OverFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_PartitionFieldName();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
        }
    }

    public void testVerifyFunction_forPreSummariedInput() {

        Collection<Object[]> testCaseArguments = getCharactersAndValidity();

        for (Object [] args : testCaseArguments) {
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();

            setCharacter((String)args[0]);
            setIsValid((boolean)args[1]);

            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_FieldNameGivenPresummarised();
            detector = DetectorVerifierInvalidFieldNameTest.createDetectorWithValidFieldNames();
            verify_ByFieldNameGivenPresummarised();
            verify_OverFieldNameGivenPresummarised();
            verify_ByFieldNameGivenPresummarised();
            verify_PartitionFieldNameGivenPresummarised();
        }
    }

    public void verify_FieldName() {
        detector = createDetectorWithSpecificFieldName(detector.getFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, () -> DetectorVerifier.verify(detector, false));
    }


    public void verify_ByFieldName() {
        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, () -> DetectorVerifier.verify(detector, false));
    }


    public void verify_OverFieldName() {
        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, () -> DetectorVerifier.verify(detector, false));
    }


    public void verify_PartitionFieldName() {
        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseExceptionWhenCharIsInvalid(
                ErrorCodes.PROHIBITIED_CHARACTER_IN_FIELD_NAME, () -> DetectorVerifier.verify(detector, false));
    }


    public void verify_FieldNameGivenPresummarised() {
        detector = createDetectorWithSpecificFieldName(detector.getFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseException(ErrorCodes.INVALID_FUNCTION, () -> DetectorVerifier.verify(detector, true));
    }


    public void verify_ByFieldNameGivenPresummarised() {
        detector.setByFieldName(detector.getByFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseException(ErrorCodes.INVALID_FUNCTION, () -> DetectorVerifier.verify(detector, true));
    }


    public void verify_OverFieldNameGivenPresummarised() {
        detector.setOverFieldName(detector.getOverFieldName() + getCharacterPlusSuffix());

        expectElasticsearchParseException(ErrorCodes.INVALID_FUNCTION, () -> DetectorVerifier.verify(detector, true));
    }


    public void verify_PartitionFieldNameGivenPresummarised() {
        detector.setPartitionFieldName(detector.getPartitionFieldName() + getCharacterPlusSuffix());
        expectElasticsearchParseException(ErrorCodes.INVALID_FUNCTION, () -> DetectorVerifier.verify(detector, true));
    }

    private String getCharacterPlusSuffix() {
        return character + SUFFIX;
    }

    private void setCharacter(String character ) {
        this.character = character;
    }

    private boolean isValid() { return this.isValid; }

    private void setIsValid(boolean valid) { this.isValid = valid; }

    private void expectElasticsearchParseExceptionWhenCharIsInvalid(ErrorCodes errorCode, LuceneTestCase.ThrowingRunnable runner) {
        if (!isValid()) {
            expectElasticsearchParseException(errorCode, runner);
        }
    }

    private void expectElasticsearchParseException(ErrorCodes errorCode, LuceneTestCase.ThrowingRunnable runner) {

        ElasticsearchParseException e = ESTestCase.expectThrows(ElasticsearchParseException.class, runner);
        assertEquals(1, e.getHeader("errorCode").size());
        assertEquals(errorCode.getValueString(), e.getHeader("errorCode").get(0));
    }

    private static Detector createDetectorWithValidFieldNames() {
        Detector d = new Detector("metric", "field");
        d.setByFieldName("by");
        d.setOverFieldName("over");
        d.setPartitionFieldName("partition");
        return d;
    }

    private static Detector createDetectorWithSpecificFieldName(String fieldName) {
        Detector d = new Detector("metric", fieldName);
        d.setByFieldName("by");
        d.setOverFieldName("over");
        d.setPartitionFieldName("partition");
        return d;
    }
}
