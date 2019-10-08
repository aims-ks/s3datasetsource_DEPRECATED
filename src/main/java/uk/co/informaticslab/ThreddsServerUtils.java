package uk.co.informaticslab;

import org.apache.log4j.Logger;

import java.io.File;

public class ThreddsServerUtils {
    private static final Logger LOGGER = Logger.getLogger(ThreddsServerUtils.class);

    public static void restart() {
        LOGGER.warn("Restarting THREDDS webapp");

        // The refresh method is available in many classes, but it doesn't seem to be possible to get
        //   the Application instance of those class.
        // Some examples online create a new class, but that means the refresh in executed on an empty shell,
        //   not on the running webapp.
        // Some examples instanciate the class using the application XML config, which is not possible in
        //   this case since this is a plugin. The configuration XML file in not defined here.
        // Some people suggest to use the Tomcat Manager app, which we usually delete for security reason.
        //   It can be called programmatically, but that's even worse than touching web.xml.

        /*
        AbstractRefreshableWebApplicationContext webContext = null;
        webContext.refresh();

        AbstractRefreshableApplicationContext context = null;
        context.refresh();

        AbstractRefreshableConfigApplicationContext configContext = null;
        configContext.refresh();

        AbstractApplicationContext c = null;
        c.refresh();

        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.refresh();

        ConfigurableApplicationContext cc = null;
        cc.refresh();

        ConfigurableWebApplicationContext ccc = null;
        ccc.refresh();
        */

        // THREDDS API calls that seems like they should do what we want, but are not working...
        /*
        DatasetHandler.reinit();
        DataRootHandler.getInstance().reinit();
        */

        // That works, but that's stupidly messy. It trigger a refresh withing the next 5 seconds
        File webAppRoot = new File(System.getProperty("catalina.base"));
        File webXml = new File(webAppRoot, "webapps/thredds/WEB-INF/web.xml");
        if (webXml.exists()) {
            // touch the file
            LOGGER.warn(String.format("Touching file: %s", webXml));
            webXml.setLastModified(System.currentTimeMillis());
        } else {
            LOGGER.error(String.format("Can not restart the webapp. The web.xml file can not be found: %s", webXml));
        }
    }
}
