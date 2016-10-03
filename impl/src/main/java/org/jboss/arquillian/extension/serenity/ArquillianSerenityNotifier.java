package org.jboss.arquillian.extension.serenity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.thucydides.junit.listeners.JUnitStepListener;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class ArquillianSerenityNotifier extends RunNotifier {

    private JUnitStepListener jUnitStepListener;

    private List<Description> listOfIgnoredTests = new ArrayList<Description>();
    private List<Method> listOfInContainerTests = new ArrayList<Method>();

    public ArquillianSerenityNotifier(JUnitStepListener jUnitStepListener){
        this.jUnitStepListener = jUnitStepListener;
        addListener(jUnitStepListener);
    }

    public void fireTestIgnored(final Description description) {
        listOfIgnoredTests.add(description);
        try {
            jUnitStepListener.testIgnored(description);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Description> getListOfIgnoredTests(){
        return listOfIgnoredTests;
    }

    public List<Method> getListOfInContainerTests(){
        return listOfInContainerTests;
    }

}
