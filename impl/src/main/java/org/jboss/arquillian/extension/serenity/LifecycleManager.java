package org.jboss.arquillian.extension.serenity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.StepEventBus;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.test.impl.execution.event.ExecutionEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.Test;
import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class LifecycleManager {

    @Inject
    @ApplicationScoped
    private InstanceProducer<ArquillianSerenityRunner> arquillianSerenity;

    @Inject
    private Event<ExecutionEvent> executionEvent;

    @Inject
    private Instance<Deployment> deployment;

    private static Logger log = Logger.getLogger(LifecycleManager.class.getName());

    public void observeBeforeClass(@Observes BeforeClass beforeClass) throws InitializationError {
        if (arquillianSerenity.get() == null) {
            ArquillianSerenityRunner arqSerenityRunner =
                new ArquillianSerenityRunner(beforeClass.getTestClass().getJavaClass());
            arquillianSerenity.set(arqSerenityRunner);
            arqSerenityRunner.getSerenityStepListener();
        }

    }

    public void observeTest(@Observes(precedence = 99) EventContext<Test> context)
        throws NoSuchFieldException, IllegalAccessException {

        final Test event = context.getEvent();
        final Method method = event.getTestMethod();
        final TestClass test = event.getTestClass();
        final ArquillianSerenityRunner runner = arquillianSerenity.get();
        final FrameworkMethod frameworkMethod = new FrameworkMethod(method);

        //        runner.shouldBeIgnored(frameworkMethod);
        runner.addTestReadyToStart(frameworkMethod);

        TestMethodExecutor testMethodExecutor = new TestMethodExecutor() {
            @Override public Method getMethod() {
                return method;
            }

            @Override public Object getInstance() {
                return test;
            }

            @Override public void invoke(Object... objects) throws Throwable {
                runner.runSerenityChild(frameworkMethod);
            }
        };

        Field methodExecutor = event.getClass().getDeclaredField("testMethodExecutor");
        methodExecutor.setAccessible(true);
        methodExecutor.set(context.getEvent(), testMethodExecutor);
        context.proceed();
    }

    public void observeTestResult(@Observes TestResult testResult) {
        ArquillianSerenityRunner runner = arquillianSerenity.get();
        BaseStepListener publisher = runner.getSerenityStepListener().getBaseStepListener();

        if (publisher.aStepHasFailed()) {
            String message =
                publisher.getTestFailureCause().getErrorType() + ": " + publisher.getTestFailureCause().getMessage();
            System.out.println(message);
            testResult.setThrowable(publisher.getTestFailureCause().toException());

        } else if (StepEventBus.getEventBus().assumptionViolated()) {
            testResult
                .setThrowable(
                    new AssumptionViolatedException(StepEventBus.getEventBus().getAssumptionViolatedMessage()));
        } else if (runner.getNotifier().getListOfIgnoredTests().size() > 0) {
            testResult.setStatus(TestResult.Status.SKIPPED);
            runner.getNotifier().getListOfIgnoredTests().clear();
//            if (testResult.getThrowable() instanceof AssumptionViolatedException) {
//                testResult.setThrowable(null);
//            }
        }
    }

    public void observeAfterSuite(@Observes AfterSuite afterSuite) {
        try {
            StepEventBus.getEventBus().testSuiteFinished();
        } catch (Throwable listenerException) {
            // We report and ignore listener exceptions so as not to mess up the rest of the test mechanics.
            log.log(Level.SEVERE, "Test event bus error: " + listenerException.getMessage(), listenerException);
        }
        ArquillianSerenityRunner runner = arquillianSerenity.get();
        runner.generateSerenityReports();
        runner.closeSerenityDrivers();
    }

}
