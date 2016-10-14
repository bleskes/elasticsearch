
package org.elasticsearch.xpack.prelert.job.transform;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;

import java.util.ArrayList;
import java.util.Arrays;

public class TransformConfigTest extends ESTestCase {

    public void testGetOutputs_GivenEmptyTransformConfig() {
        assertTrue(new TransformConfig().getOutputs().isEmpty());
    }


    public void testGetOutputs_GivenNoExplicitOutputsSpecified() {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");

        assertEquals(Arrays.asList("concat"), config.getOutputs());
    }


    public void testGetOutputs_GivenEmptyOutputsSpecified() {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");
        config.setOutputs(new ArrayList<>());

        assertEquals(Arrays.asList("concat"), config.getOutputs());
    }


    public void testGetOutputs_GivenOutputsSpecified() {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");
        config.setOutputs(Arrays.asList("o1", "o2"));

        assertEquals(Arrays.asList("o1", "o2"), config.getOutputs());
    }


    public void testVerify_GivenUnknownTransform() {

        TransformConfig tr = new TransformConfig();
        tr.setInputs(Arrays.asList("f1", "f2"));
        tr.setTransform("unknown+transform");

        ESTestCase.expectThrows(IllegalArgumentException.class, () -> tr.type());
    }


    public void testEquals_GivenSameReference() {
        TransformConfig config = new TransformConfig();
        assertTrue(config.equals(config));
    }


    public void testEquals_GivenDifferentClass() {
        TransformConfig config = new TransformConfig();
        assertFalse(config.equals("a string"));
    }


    public void testEquals_GivenNull() {
        TransformConfig config = new TransformConfig();
        assertFalse(config.equals(null));
    }


    public void testEquals_GivenEqualTransform() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));
        config1.setCondition(Condition.NONE);

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("-"));
        config2.setCondition(Condition.NONE);

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
    }


    public void testEquals_GivenDifferentType() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("lowercase");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentInputs() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input3"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentOutputs() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output1"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output2"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentArguments() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("--"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }


    public void testEquals_GivenDifferentConditions() {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));
        config1.setCondition(new Condition(Operator.EQ, "foo"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("-"));
        config2.setCondition(Condition.NONE);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }
}
