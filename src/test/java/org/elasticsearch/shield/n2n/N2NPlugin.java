package org.elasticsearch.shield.n2n;

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.shield.n2n.N2NModule;

import java.util.Collection;

/**
 * a plugin that just loads the N2NModule (required for transport integration tests)
 */
public class N2NPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "test-n2n-plugin";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        return ImmutableSet.<Class<? extends Module>>of(N2NModule.class);
    }
}
