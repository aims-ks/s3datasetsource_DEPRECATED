package thredds.server.harvester;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;

public class S3HarvesterStartupEventListener implements LifecycleListener, ApplicationContextAware {
    private static final Logger LOGGER = Logger.getLogger(S3HarvesterStartupEventListener.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent lifecycleEvent) {
        String type = lifecycleEvent.getType();
        if (type == null) {
            return;
        }
LOGGER.warn("LIFE CYCLE: " + type);
System.out.println("LIFE CYCLE: " + type);

        if (type.equals(Lifecycle.AFTER_START_EVENT)) {
            S3HarvesterController controller = new S3HarvesterController();
            controller.setApplicationContext(this.applicationContext);
LOGGER.warn("--- RELOAD CATALOG. Context: " + (this.applicationContext == null ? "NULL" : "NOT NULL"));
System.out.println("--- RELOAD CATALOG. Context: " + (this.applicationContext == null ? "NULL" : "NOT NULL"));

            try {
                controller.harvest();
            } catch (IOException ex) {
                LOGGER.error("Exception occurred while harvesting the S3 buckets", ex);
ex.printStackTrace();
            }
        }
    }
}
