package org.jboss.arquillian.extension.serenity;

import java.util.ArrayList;
import java.util.List;

import net.serenitybdd.junit.runners.SerenityRunner;
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
        return describeChild(method);
    }

    public void addTestReadyToStart(FrameworkMethod method){
        readyToStart.add(describeChild(method));
    }

}
