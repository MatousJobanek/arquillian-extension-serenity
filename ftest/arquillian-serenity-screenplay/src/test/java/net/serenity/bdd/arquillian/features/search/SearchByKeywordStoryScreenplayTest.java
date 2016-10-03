package net.serenity.bdd.arquillian.features.search;

import net.serenity.bdd.arquillian.tasks.OpenTheApplication;
import net.serenity.bdd.arquillian.tasks.Search;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.questions.page.TheWebPage;
import net.thucydides.core.annotations.Managed;
import net.thucydides.core.annotations.Steps;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import static net.serenitybdd.screenplay.EventualConsequence.eventually;
import static net.serenitybdd.screenplay.GivenWhenThen.givenThat;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.GivenWhenThen.then;
import static net.serenitybdd.screenplay.GivenWhenThen.when;
import static org.hamcrest.Matchers.containsString;

@RunWith(Arquillian.class)
@RunAsClient
public class SearchByKeywordStoryScreenplayTest {

    @Deployment
    public static WebArchive createTestArchive() {
        WebArchive archive = ShrinkWrap
            .create(WebArchive.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    Actor anna = Actor.named("Anna");

    @Managed(uniqueSession = true)
    public WebDriver herBrowser;

    @Steps
    OpenTheApplication openTheApplication;

    @Before
    public void annaCanBrowseTheWeb() {
        anna.can(BrowseTheWeb.with(herBrowser));
    }

    @Test
    public void search_results_should_show_the_search_term_in_the_title() {

        givenThat(anna).wasAbleTo(openTheApplication);

        when(anna).attemptsTo(Search.forTheTerm("BDD In Action"));

        then(anna).should(eventually(seeThat(TheWebPage.title(), containsString("BDD In Action"))));
    }
}
