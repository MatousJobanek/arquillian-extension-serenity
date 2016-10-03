package org.jboss.arquillian.extension.serenity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.steps.BaseStepListener;
import net.thucydides.core.steps.StepEventBus;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.test.impl.RunModeUtils;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.Test;
import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
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
    private Instance<Deployment> deployment;

    @Inject
    private Instance<TestClass> testClassInstance;

    @Inject
    private Event<TestResult> testResultEvent;

    @Inject
    private Event<AssumptionViolatedException> assumptionViolatedExceptionEvent;



    private static Logger log = Logger.getLogger(LifecycleManager.class.getName());

    public void prepareArquillianSerenityRunner(@Observes BeforeClass beforeClass) throws InitializationError {
        if (arquillianSerenity.get() == null) {
            ArquillianSerenityRunner arqSerenityRunner =
                new ArquillianSerenityRunner(beforeClass.getTestClass().getJavaClass());
            arquillianSerenity.set(arqSerenityRunner);
            arqSerenityRunner.getSerenityStepListener();
        }

    }

    public void invokeTest(@Observes(precedence = 99) Test event)
        throws NoSuchFieldException, IllegalAccessException {

        final Method method = event.getTestMethod();
        final TestClass test = event.getTestClass();
        final ArquillianSerenityRunner runner = arquillianSerenity.get();
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Description description = runner.describeSerenityChild(frameworkMethod);

        boolean runAsClient = RunModeUtils.isRunAsClient(
            this.deployment.get(),
            event.getTestClass().getJavaClass(),
            event.getTestMethod());

        Field methodExecutor = event.getClass().getDeclaredField("testMethodExecutor");
        methodExecutor.setAccessible(true);
        if (runAsClient) {
            methodExecutor.set(event, client(runner, method, event.getTestInstance()));
        } else {
            //            methodExecutor.set(event, client(runner, method, event.getTestInstance()));
            //            StepEventBus.getEventBus().testStarted(description.getMethodName(), description.getTestClass());
            runner.getNotifier().getListOfInContainerTests().add(method);
            methodExecutor.set(event, inContainer(runner, method, event.getTestInstance(),
                                                  runner.getSerenityStepListener().getBaseStepListener()));
        }
    }

    private TestMethodExecutor inContainer(final ArquillianSerenityRunner runner, final Method method,
        final Object test, final BaseStepListener baseStepListener) {
        final boolean shouldRun = runner.prepareForIncontainerChild(new FrameworkMethod(method), test);
        final Description description = runner.describeSerenityChild(new FrameworkMethod(method));
        final IgnoreTest ignoreTest = new IgnoreTest();

        return new TestMethodExecutor() {
            @Override public Method getMethod() {
                if (shouldRun) {
                    return method;
                } else {
                    try {
                        return IgnoreTest.class.getDeclaredMethod("ignoreTest");
                    } catch (NoSuchMethodException e) {
//                        e.printStackTrace();
                    }
                    throw new AssumptionViolatedException(
                        "The Serenity test is marked either as pending or manual");
                }

            }

            @Override public Object getInstance() {
                if (shouldRun) {
                    return test;
                }else{
                    return ignoreTest;
                }
            }

            @Override public void invoke(Object... objects) throws Throwable {

                //TODO: move to more logic place
                //                checkForStepFailures(baseStepListener);
                //                checkForAssumptionViolations(baseStepListener);
            }

        };
    }

    private void checkForStepFailures(BaseStepListener baseStepListener) throws Throwable {
        if (baseStepListener.aStepHasFailed()) {
            String message = baseStepListener.getTestFailureCause().getErrorType() + ": " + baseStepListener
                .getTestFailureCause().getMessage();
            System.out.println(message);
            throw baseStepListener.getTestFailureCause().toException();
        }
    }

    private void checkForAssumptionViolations(BaseStepListener baseStepListener) {
        if (StepEventBus.getEventBus().assumptionViolated()) {
            throw new org.junit.internal.AssumptionViolatedException(
                StepEventBus.getEventBus().getAssumptionViolatedMessage());
        }
    }

    private TestMethodExecutor client(final ArquillianSerenityRunner runner, final Method method,
        final Object test) {
        return new TestMethodExecutor() {
            @Override public Method getMethod() {
                return method;
            }

            @Override public Object getInstance() {
                return test;
            }

            @Override public void invoke(Object... objects) throws Throwable {
                runner.runSerenityChild(new FrameworkMethod(method));
            }
        };
    }

    public void setTestResult(@Observes TestResult testResult) {
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
        }

        boolean inContainer = runner.getNotifier().getListOfInContainerTests().size() > 0;
        if (inContainer) {
            FrameworkMethod lastMethod =
                new FrameworkMethod(runner.getNotifier().getListOfInContainerTests().remove(0));
            Description description = runner.describeSerenityChild(lastMethod);
            if (testResult.getStatus() == TestResult.Status.FAILED) {
                if (testResult.getThrowable() instanceof AssumptionViolatedException
                    || testResult.getThrowable() instanceof org.junit.internal.AssumptionViolatedException) {
                    StepEventBus.getEventBus().assumptionViolated(testResult.getThrowable().getMessage());
                    StepEventBus.getEventBus().testFinished();
                } else {

                    publisher.testFailed(new TestOutcome(description.getMethodName(), description.getTestClass()),
                                         testResult.getThrowable());
                    StepEventBus.getEventBus().testFailed(testResult.getThrowable());
                }
            } else if (testResult.getStatus() == TestResult.Status.SKIPPED) {
            } else {
                StepEventBus.getEventBus()
                    .testFinished(TestOutcome.forTest(description.getMethodName(), description.getTestClass()));
            }
        }
    }

    public void cleanupAndReportGeneration(@Observes AfterSuite afterSuite) {
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
