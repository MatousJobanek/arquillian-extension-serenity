package org.jboss.arquillian.extension.serenity;

import com.google.inject.Injector;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.junit.runners.FailureDetectingStepListener;
import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.junit.runners.TestConfiguration;
import net.serenitybdd.junit.runners.TestMethodConfiguration;
import net.thucydides.core.batches.BatchManager;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.tags.TagScanner;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.ThucydidesWebDriverSupport;
import net.thucydides.core.webdriver.WebdriverManager;
import net.thucydides.junit.listeners.JUnitStepListener;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static net.serenitybdd.core.Serenity.initializeTestSession;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class ArquillianSerenityRunner extends SerenityRunner {

    private ArquillianSerenityNotifier notifier;
    private TagScanner tagScanner;
    private final TestConfiguration theTest;

    public ArquillianSerenityRunner(Class<?> klass) throws InitializationError {
        this(klass, Injectors.getInjector());


    }

    public ArquillianSerenityRunner(final Class<?> klass,
        final Injector injector) throws InitializationError {
        this(klass,
             ThucydidesWebDriverSupport.getWebdriverManager(),
             injector.getInstance(Configuration.class),
             injector.getInstance(BatchManager.class)
        );
    }

    public ArquillianSerenityRunner(final Class<?> klass,
        final WebdriverManager webDriverManager,
        final Configuration configuration,
        final BatchManager batchManager) throws InitializationError {

        super(klass, webDriverManager, configuration, batchManager);

        this.tagScanner = new TagScanner(configuration.getEnvironmentVariables());
        notifier = new ArquillianSerenityNotifier(getSerenityStepListener());
        this.theTest = TestConfiguration.forClass(klass).withSystemConfiguration(configuration);

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
        return describeChild(method);
    }


    public boolean prepareForIncontainerChild(FrameworkMethod method, final Object test) {

        TestMethodConfiguration theMethod = TestMethodConfiguration.forMethod(method);

        clearMetadataIfRequired();

        if (shouldSkipTest(method)) {
            return false;
        }

        if (theMethod.isManual()) {
            markAsManual(method);
            notifier.fireTestIgnored(describeChild(method));
            return false;
        } else if (theMethod.isPending()) {
            markAsPending(method);
            notifier.fireTestIgnored(describeChild(method));
            return false;
        } else {
            processTestMethodAnnotationsFor(method);
        }

        FailureDetectingStepListener failureDetectingStepListener = new FailureDetectingStepListener();
        StepEventBus.getEventBus().registerListener(failureDetectingStepListener);

        initializeTestSession();
        failureDetectingStepListener.reset();

        injectScenarioStepsInto(test);
        injectEnvironmentVariablesInto(test);
        return true;
    }

    private boolean shouldSkipTest(FrameworkMethod method) {
        return !tagScanner.shouldRunMethod(getTestClass().getJavaClass(), method.getName());
    }

    private void markAsPending(FrameworkMethod method) {
        testStarted(method);
        StepEventBus.getEventBus().testPending();
        StepEventBus.getEventBus().testFinished();
    }

    private void markAsManual(FrameworkMethod method) {
        testStarted(method);
        StepEventBus.getEventBus().testIsManual();
        StepEventBus.getEventBus().testFinished();
    }

    private void testStarted(FrameworkMethod method) {
        getStepListener().testStarted(Description.createTestDescription(method.getMethod().getDeclaringClass(), testName(method)));
    }

    private void clearMetadataIfRequired() {
        if (theTest.shouldClearMetadata()) {
            Serenity.getCurrentSession().clearMetaData();
        }
    }

    private void processTestMethodAnnotationsFor(FrameworkMethod method) {
        if (isIgnored(method)) {
            testStarted(method);
            StepEventBus.getEventBus().testIgnored();
        }
    }

}
