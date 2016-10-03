package org.jboss.arquillian.extension.serenity;

import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

/**
 * @author <a href="mailto:mjobanek@redhat.com">Matous Jobanek</a>
 */
public class SerenityArchiveAppender extends CachedAuxilliaryArchiveAppender {
    @Override protected Archive<?> buildArchive() {

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "arquillian-serenity.jar");
        PomEquippedResolveStage pomEquipped = Maven.configureResolver().loadPomFromFile("pom.xml");

//        JavaArchive[] core = pomEquipped.resolve("net.serenity-bdd:serenity-core:1.1.38").withoutTransitivity().as(
//            JavaArchive.class);
////
//        JavaArchive[] screenplay =
//            pomEquipped.resolve("net.serenity-bdd:serenity-junit:1.1.38").withoutTransitivity().as(
//                JavaArchive.class);

//        for (JavaArchive jar : core) {
//            archive.merge(jar);
//        }
//        for (JavaArchive jar : screenplay) {
//            archive.merge(jar);
//        }

        archive.addPackages(true, "org/openqa/");
        archive.addPackages(true, "net/serenity/");
        archive.addPackages(true, "net/thucydides");
        archive.addClass(IgnoreTest.class);



        return archive;
    }
}
