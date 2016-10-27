package org.elasticsearch.xpack.prelert.job.transform;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransformConfigsTests extends AbstractSerializingTestCase<TransformConfigs> {

    @Override
    protected TransformConfigs createTestInstance() {
        int size = randomInt(10);
        List<TransformConfig> transforms = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TransformType transformType = randomFrom(TransformType.values());
            TransformConfig config = new TransformConfig(transformType.prettyName());
            if (randomBoolean()) {
                config.setInputs(Arrays.asList(generateRandomStringArray(0, 10, false)));
            }
            if (randomBoolean()) {
                config.setOutputs(Arrays.asList(generateRandomStringArray(0, 10, false)));
            }
            if (randomBoolean()) {
                config.setArguments(Arrays.asList(generateRandomStringArray(0, 10, false)));
            }
            if (randomBoolean()) {
                // no need to randomize, it is properly randomily tested in ConditionTest
                config.setCondition(new Condition(Operator.EQ, Double.toString(randomDouble())));
            }
            transforms.add(config);
        }
        return new TransformConfigs(transforms);
    }

    @Override
    protected Reader<TransformConfigs> instanceReader() {
        return TransformConfigs::new;
    }

    @Override
    protected TransformConfigs parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return TransformConfigs.PARSER.apply(parser, () -> matcher);
    }

    @Test
    public void test_Input_Output_FieldNames() {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(Arrays.asList("a", "b", "c"), Arrays.asList("c1")));
        transforms.add(createConcatTransform(Arrays.asList("d", "e", "c"), Arrays.asList("c2")));
        transforms.add(createConcatTransform(Arrays.asList("f", "a", "c"), Arrays.asList("c3")));

        TransformConfigs tcs = new TransformConfigs(transforms);

        List<String> inputNames = Arrays.asList("a", "b", "c", "d", "e", "f");
        Set<String> inputSet = new HashSet<>(inputNames);
        assertEquals(inputSet, tcs.inputFieldNames());

        List<String> outputNames = Arrays.asList("c1", "c2", "c3");
        Set<String> outputSet = new HashSet<>(outputNames);
        assertEquals(outputSet, tcs.outputFieldNames());
    }

    private TransformConfig createConcatTransform(List<String> inputs, List<String> outputs) {
        TransformConfig concat = new TransformConfig(TransformType.CONCAT.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }

}
