
package org.elasticsearch.xpack.prelert.job.condition;

public class UnknownOperatorException extends Exception {
    private static final long serialVersionUID = 4691115581549035110L;

    private final String name;

    /**
     * @param operatorName The unrecognised operator name
     */
    public UnknownOperatorException(String operatorName) {
        name = operatorName;
    }

    /**
     * Get the unknown operator name
     *
     * @return
     */
    public String getName() {
        return name;
    }
}
