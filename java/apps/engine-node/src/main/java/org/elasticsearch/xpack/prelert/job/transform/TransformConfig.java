/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package org.elasticsearch.xpack.prelert.job.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.xpack.prelert.job.condition.Condition;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an API data transform
 */
@JsonInclude(Include.NON_NULL)
public class TransformConfig {
    // Serialisation strings
    public static final String TYPE = "transform";
    public static final String TRANSFORM = "transform";
    public static final String ARGUMENTS = "arguments";
    public static final String INPUTS = "inputs";
    public static final String OUTPUTS = "outputs";


    private List<String> inputs;
    private String name;
    private List<String> arguments;
    private List<String> outputs;
    private TransformType type;
    private Condition condition;


    public TransformConfig() {
        arguments = Collections.emptyList();
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> fields) {
        inputs = fields;
    }

    /**
     * Transform name see {@linkplain TransformType.Names}
     *
     * @return
     */
    public String getTransform() {
        return name;
    }

    public void setTransform(String type) {
        name = type;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> args) {
        arguments = args;
    }

    public List<String> getOutputs() {
        if (outputs == null || outputs.isEmpty()) {
            try {
                outputs = type().defaultOutputNames();
            } catch (IllegalArgumentException e) {
                outputs = Collections.emptyList();
            }
        }

        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    /**
     * The condition object which may or may not be defined for this
     * transform
     *
     * @return May be <code>null</code>
     */
    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    /**
     * This field shouldn't be serialised as its created dynamically
     * Type may be null when the class is constructed.
     *
     * @return
     */
    public TransformType type() throws IllegalArgumentException {
        if (type == null) {
            type = TransformType.fromString(name);
        }

        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, name, outputs, type, arguments, condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        TransformConfig other = (TransformConfig) obj;

        return Objects.equals(this.type, other.type)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.inputs, other.inputs)
                && Objects.equals(this.outputs, other.outputs)
                && Objects.equals(this.arguments, other.arguments)
                && Objects.equals(this.condition, other.condition);
    }
}
