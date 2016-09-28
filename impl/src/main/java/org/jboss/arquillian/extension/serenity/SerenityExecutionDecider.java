package org.jboss.arquillian.extension.serenity;

import java.lang.reflect.Method;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.execution.ExecutionDecision;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;
import org.junit.runners.model.FrameworkMethod;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class SerenityExecutionDecider implements TestExecutionDecider {

    @Inject
    private Instance<ArquillianSerenityRunner> runnerInstance;


    @Override public ExecutionDecision decide(Method method) {
        System.err.println("------------------------------------------->");
        if (runnerInstance != null) {
            ArquillianSerenityRunner runner = runnerInstance.get();
            if (runner != null && runner.getNotifier() != null) {
//                Description description = runner.describeSerenityChild(new FrameworkMethod(method));
//                if (runner.getNotifier().getListOfIgnoredTests().contains(description)) {
                if (runner.shouldBeIgnored(new FrameworkMethod(method)))
                    return ExecutionDecision.dontExecute("The Serenity test is marked either as manual or as pending");
//                }
            }
        }
        return ExecutionDecision.execute();
    }

    @Override public int precedence() {
        return 0;
    }
}
