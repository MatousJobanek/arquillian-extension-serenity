package org.jboss.arquillian.extension.serenity;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class ArquillianSerenityExtension implements LoadableExtension {
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.observer(LifecycleManager.class);
        extensionBuilder.service(TestExecutionDecider.class, SerenityExecutionDecider.class);
    }
}
