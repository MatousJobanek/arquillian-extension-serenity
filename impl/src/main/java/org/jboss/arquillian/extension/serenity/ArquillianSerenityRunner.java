package org.jboss.arquillian.extension.serenity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.junit.runners.TestMethodConfiguration;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.junit.listeners.JUnitStepListener;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class ArquillianSerenityRunner extends SerenityRunner {

    private ArquillianSerenityNotifier notifier;

    private List<Description> readyToStart = new ArrayList<Description>();

    public ArquillianSerenityRunner(Class<?> klass) throws InitializationError {
        super(klass);
        notifier = new ArquillianSerenityNotifier(getSerenityStepListener());

    }

    public JUnitStepListener getSerenityStepListener(){
        return getStepListener();
    }

    public void runSerenityChild(FrameworkMethod frameworkMethod){
        runChild(frameworkMethod, notifier);
    }

    public Statement serenityMethodInvoker(FrameworkMethod method, Object test){
        return methodInvoker(method, test);
    }

    protected void generateSerenityReports() {
        generateReports();
    }

    public void closeSerenityDrivers() {
        getWebdriverManager().closeAllDrivers();
    }

    public ArquillianSerenityNotifier getNotifier(){
        return notifier;
    }

    public Description describeSerenityChild(FrameworkMethod method) {
        return this.describeChild(method);
    }

    public void addTestReadyToStart(FrameworkMethod method){
        readyToStart.add(describeChild(method));
    }

    public boolean shouldBeIgnored(FrameworkMethod method) {
        TestMethodConfiguration theMethod = TestMethodConfiguration.forMethod(method);
        Description description = describeChild(method);
        if (!readyToStart.contains(description)){
            return false;
        }

        if (notifier.getListOfIgnoredTests().contains(description)){
            return true;
        }

        if (theMethod.isManual()) {
            markAsManual(method);
            notifier.fireTestIgnored(description);
            clearMetadataIfRequired();
            return true;
        } else if (theMethod.isPending()) {
            markAsPending(method);
            notifier.fireTestIgnored(description);
            clearMetadataIfRequired();
            return true;
        }
        return false;
    }

    private void clearMetadataIfRequired(){
        try {
            Method clearMetadataIfRequired = SerenityRunner.class.getDeclaredMethod("clearMetadataIfRequired");
            clearMetadataIfRequired.setAccessible(true);
            clearMetadataIfRequired.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markAsManual(FrameworkMethod method) {
        testStarted(method);
        StepEventBus.getEventBus().testIsManual();
        StepEventBus.getEventBus().testFinished();
    }

    private void testStarted(FrameworkMethod method) {
        getStepListener().testStarted(Description.createTestDescription(method.getMethod().getDeclaringClass(), testName(method)));
    }

    private void markAsPending(FrameworkMethod method) {
        testStarted(method);
        StepEventBus.getEventBus().testPending();
        StepEventBus.getEventBus().testFinished();
    }
}
