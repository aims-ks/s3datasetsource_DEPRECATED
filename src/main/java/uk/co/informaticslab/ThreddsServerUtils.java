package uk.co.informaticslab;

import org.apache.log4j.Logger;
import thredds.servlet.DataRootHandler;

public class ThreddsServerUtils {
    private static final Logger LOGGER = Logger.getLogger(ThreddsServerUtils.class);

    /**
     * Reload THREDDS catalogue configuration files.
     *
     * This very simple method took a long time to implement because THREDDS code is quite messy,
     * not well documented and not widely used enough to find the answer using Google...
     *
     * Spring webmvc framework - "refresh" method(s)
     *   The "refresh" method is available in many context classes, but it doesn't seem to be possible to get
     *     the running instance of those class.
     *   Some examples online create a new context class, but that means the refresh in executed on an empty shell,
     *     not on the running webapp.
     *   Some examples instanciate the class using the application XML config, which is not suitable in
     *     this case since this is a plugin. The project configuration XML file is defined in THREDDS.
     *   Some people suggest to use the Tomcat Manager app, which we usually delete for security reason.
     *     It can be called programmatically, using HTTP calls. That solution seems to be the worst so far.
     *
     *   There is the list of classes I have been experimenting with:
     *       AbstractRefreshableWebApplicationContext refreshableWebApplicationContext = null;
     *       refreshableWebApplicationContext.refresh();
     *
     *       AbstractRefreshableApplicationContext refreshableApplicationContext = null;
     *       refreshableApplicationContext.refresh();
     *
     *       AbstractRefreshableConfigApplicationContext refreshableConfigApplicationContext = null;
     *       refreshableConfigApplicationContext.refresh();
     *
     *       AbstractApplicationContext applicationContext = null;
     *       applicationContext.refresh();
     *
     *       AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = new AnnotationConfigWebApplicationContext();
     *       annotationConfigWebApplicationContext.refresh();
     *
     *       ConfigurableApplicationContext configurableApplicationContext = null;
     *       configurableApplicationContext.refresh();
     *
     *       ConfigurableWebApplicationContext configurableWebApplicationContext = null;
     *       configurableWebApplicationContext.refresh();
     *
     *
     * Touching "web.xml"
     *   Webapps can be configured to get automatically "redeploy" by tomcat when its "web.xml" file is modified.
     *   This is known as a "hot redeploy". It works, but it's not good practice to do this on a production server:
     *   - Static variables can not be free, which leads to memory leaks.
     *   This solution could be used as last resort solution. Just keep in mind that THREDDS uses static
     *     variables intensively (cached maps, singleton classes, etc)
     *
     *       File webAppRoot = new File(System.getProperty("catalina.base"));
     *       File webXml = new File(webAppRoot, "webapps/thredds/WEB-INF/web.xml");
     *       if (webXml.exists()) {
     *           // touch the file
     *           LOGGER.warn(String.format("Touching file: %s", webXml));
     *           webXml.setLastModified(System.currentTimeMillis());
     *       } else {
     *           LOGGER.error(String.format("Can not restart the webapp. The web.xml file can not be found: %s", webXml));
     *       }
     *
     *
     * THREDDS "reinit" method(s)
     *   THREDDS have a complex initialisation process. To make it worst, many classes are copy - paste
     *   in different packages (dead code?).
     *
     *   There are the calls I have tried so far:
     *       // Have no effect
     *       thredds.core.DatasetHandler.reinit();
     *
     *       // Throws exception:
     *       //   java.lang.IllegalStateException: setInstance() must be called first.
     *       thredds.core.DataRootHandler.getInstance().reinit();
     *
     *       // Have no effect
     *       thredds.servlet.ThreddsConfig.init("/usr/local/tomcat/content/thredds/threddsConfig.xml");
     *
     *       // After discovering that the Class DataRootHandler was defined in multiple packages,
     *       // I tried with this one and it worked!!!
     *       thredds.servlet.DataRootHandler.getInstance().reinit();
     */
    public static void reloadCatalogue() {
        LOGGER.warn("Reloading THREDDS catalogue");
        DataRootHandler.getInstance().reinit();
    }
}
