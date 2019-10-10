/*
 *  Copyright (C) 2019 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package thredds.server.harvester;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.servlet.DataRootHandler;
import uk.co.informaticslab.Constants;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generate THREDDS catalog.xml configuration for given S3 buckets
 *
 * handles /s3harvester/*
 */
@Controller
@RequestMapping("/s3harvester")
public class S3HarvesterController implements ApplicationContextAware {
    private static final Logger LOGGER = Logger.getLogger(S3HarvesterController.class);

    private static final File CONFIG_ROOT_DIR = new File("/usr/local/tomcat/content/thredds");
    private static final File S3_HARVESTER_CONFIG_FILE = new File(CONFIG_ROOT_DIR, "s3harvester.xml");

    private ApplicationContext applicationContext;

    private final AmazonS3 s3Client = Constants.getS3Client();

    private S3HarvesterConfiguration config;

    public S3HarvesterController() {
        this.config = new S3HarvesterConfiguration(S3_HARVESTER_CONFIG_FILE);
        try {
            this.config.init();
        } catch (Throwable ex) {
            LOGGER.error(String.format("Exception occurred while parsing the S3 harvester configuration file: %s", S3_HARVESTER_CONFIG_FILE), ex);
            this.config = null;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @RequestMapping("**")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (this.config == null) {
            LOGGER.warn("The S3 harvesting was not configured");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The S3 harvesting was not configured");

        } else {

            File temporaryConfigDirectory = Files.createTempDirectory("s3harvester_").toFile();
            File currentConfigDirectory = this.config.getS3CatalogueDirectory();

            try {
                // TODO Security?
                // http://localhost:8888/thredds/s3harvester/

                // Generate new catalogue config in a temporary directory, to prevent breaking the system and allow removal of S3 files:
                //   Some "catalog.xml" files may represent S3 directories that no longer exists. They need to be deleted.
                for (S3HarvesterConfiguration.S3HarvesterBucketConfiguration bucketConfig : this.config.getBucketConfigurations()) {
                    String bucket = bucketConfig.getBucket();
                    List<String> paths = bucketConfig.getPaths();

                    // If the bucket is configured with no path, add a "null" path,
                    // to indicate that we want to harvest the whole bucket
                    if (paths == null || paths.size() <= 0) {
                        paths = new ArrayList<String>();
                        paths.add(null);
                    }

                    try {
                        if (!this.s3Client.doesBucketExist(bucket)) {
                            LOGGER.error(String.format("Bucket %s does not exist", bucket));
                        } else {
                            Set<String> netCDFFilePaths = new TreeSet<String>();
                            for (String path : paths) {
                                netCDFFilePaths.addAll(this.getNetCDFFilePaths(bucket, path));
                            }

                            S3File netCDFFileTree = S3HarvesterController.parseFilePaths(bucket, netCDFFilePaths);
                            this.createCatalogs(temporaryConfigDirectory, netCDFFileTree);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(String.format("Exception occurred while harvesting the S3 bucket: %s", bucket), ex);
                    }
                }

                // Try to empty current THREDDS catalogue configuration
                // NOTE: If the operation fail, at least let the "Files.move" attempt to replace existing files.
                LOGGER.info(String.format("Emptying current catalogue directory %s", currentConfigDirectory));
                try {
                    FileUtils.cleanDirectory(currentConfigDirectory);
                } catch (Exception ex) {
                    LOGGER.error(String.format("Exception occurred while emptying current catalogue directory: %s", currentConfigDirectory), ex);
                }

                // Move the new catalogue configuration to where THREDDS expect it to be
                LOGGER.info(String.format("Moving new catalogue directory content from %s to %s", temporaryConfigDirectory, currentConfigDirectory));
                File[] temporaryConfigDirectoryContent = temporaryConfigDirectory.listFiles();
                if (temporaryConfigDirectoryContent != null && temporaryConfigDirectoryContent.length > 0) {
                    for (File temporaryConfigDirectoryFile : temporaryConfigDirectoryContent) {
                        LOGGER.debug(String.format("Moving new catalogue file from %s to %s", temporaryConfigDirectoryFile, currentConfigDirectory));
                        FileUtils.moveToDirectory(temporaryConfigDirectoryFile, currentConfigDirectory, true);
                    }
                }

                // Reload THREDDS configuration
                this.reloadThreddsCatalogue();

                // Send a "Harvesting done" message as a response text
                try (ServletOutputStream outputStream = response.getOutputStream()) {
                    outputStream.println("Harvesting done");
                    outputStream.flush();
                }


            } catch (Exception ex) {
                LOGGER.error("Exception occurred while harvesting the S3 buckets", ex);

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Exception occurred while harvesting the S3 buckets: %s", ex.getMessage()));
                throw ex;
            } finally {
                // Clean-up - Delete temporary config directory.
                try {
                    FileUtils.deleteDirectory(temporaryConfigDirectory);
                } catch (Exception ex2) {
                    LOGGER.error(String.format("Exception occurred while deleting temporary directory: %s", temporaryConfigDirectory), ex2);
                }
            }
        }
    }

    private File createCatalogs(File directory, S3File netCDFFile) throws IOException, TemplateException {
        if (netCDFFile == null || !netCDFFile.isDirectory()) {
            return null;
        }

        File catalogDir = new File(directory, netCDFFile.path);
        File catalogFile = new File(catalogDir, "catalog.xml");

        try {
            CatalogTemplate catalogTemplate = new CatalogTemplate();

            for (S3File child : netCDFFile.listFiles()) {
                if (child.isDirectory()) {
                    this.createCatalogs(directory, child);
                    String directoryName = child.getFilename();

                    catalogTemplate.addCatalogRef(
                            directoryName,
                            directoryName + "/catalog.xml");
                } else {
                    catalogTemplate.addDataset(
                            child.getFilename(),
                            child.getFilename(),
                            "s3/" + child.getPath());
                }
            }

            catalogTemplate.process(catalogFile);
        } catch(Exception ex) {
            LOGGER.error(String.format("Error occurred while generating the catalogue: %s", catalogFile), ex);
            throw ex;
        }

        return catalogFile;
    }

    private Set<String> getNetCDFFilePaths(String bucket, String path) {
        Set<String> netCDFFilePaths = new TreeSet<String>();

        // Inspired from:
        //     https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingObjectKeysUsingJava.html
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withMaxKeys(1000);
        if (path != null && !path.isEmpty()) {
            req.withPrefix(path);
        }

        ListObjectsV2Result result;
        do {
            result = this.s3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String filename = objectSummary.getKey();
                if (".nc".equalsIgnoreCase(filename.substring(filename.length() - 3))) {
                    netCDFFilePaths.add(filename);
                }
            }
            // If there are more than maxKeys keys in the bucket, get a continuation token
            // and list the next objects.
            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);
        } while (result.isTruncated());

        return netCDFFilePaths;
    }

    /**
     * Create a tree like structure with file paths
     * Example:
     *   dir1/file1.nc
     *   dir1/file2.nc
     *   dir1/dir11/file3.nc
     *   dir2/file4.nc
     *   file5.nc
     * Result:
     *   dir1
     *     file1.nc
     *     file2.nc
     *     dir11
     *       file3.nc
     *   dir2
     *     file4.nc
     *   file5.nc
     * @param bucket
     * @param filePaths
     * @return
     */
    protected static S3File parseFilePaths(String bucket, Set<String> filePaths) {
        Map<String, S3File> directories = new HashMap<String, S3File>();
        S3File root = new S3File(bucket, bucket);

        for (String filePath : filePaths) {
            S3File parent = root;
            String[] filePathParts = filePath.split("/");

            for (String filePart : filePathParts) {
                String path = parent.getPath() + "/" + filePart;
                S3File dir = directories.get(path);
                if (dir == null) {
                    dir = new S3File(bucket, path);
                    directories.put(path, dir);
                    parent.addChild(dir);
                }
                parent = dir;
            }
        }

        return root;
    }


    /**
     * Reload THREDDS catalogue configuration files.
     *
     * This very simple method took a long time to implement because THREDDS code is quite messy,
     * not well documented and not widely used enough to find the answer using Google...
     *
     * Spring webmvc framework - "refresh" method(s)
     *   The "refresh" method is available in many context classes, but it's not strait forward to get
     *     the running instance of those classes.
     *
     *   There is the list of classes I have been experimenting with:
     *       AbstractRefreshableWebApplicationContext refreshableWebApplicationContext = ...;
     *       refreshableWebApplicationContext.refresh();
     *
     *       AbstractRefreshableApplicationContext refreshableApplicationContext = ...;
     *       refreshableApplicationContext.refresh();
     *
     *       AbstractRefreshableConfigApplicationContext refreshableConfigApplicationContext = ...;
     *       refreshableConfigApplicationContext.refresh();
     *
     *       // Refresh THREDDS services, but doesn't seems to reload the catalogue
     *       AbstractApplicationContext applicationContext = ...;
     *       applicationContext.refresh();
     *
     *       AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext = ...;
     *       annotationConfigWebApplicationContext.refresh();
     *
     *       ConfigurableApplicationContext configurableApplicationContext = ...;
     *       configurableApplicationContext.refresh();
     *
     *       ConfigurableWebApplicationContext configurableWebApplicationContext = ...;
     *       configurableWebApplicationContext.refresh();
     *
     *       Usage example:
     *         public class S3HarvesterController implements ApplicationContextAware {
     *           private ApplicationContext applicationContext;
     *
     *           @Override
     *           public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
     *             this.applicationContext = applicationContext;
     *           }
     *
     *           public void refresh() {
     *             ((AbstractApplicationContext)this.applicationContext).refresh();
     *           }
     *         }

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
     *       // I tried those after discovering that some classes are defined in multiple packages
     *
     *       // Have no effect
     *       thredds.servlet.DatasetHandler.reinit();
     *
     *       // It reload the catalogue, but breaks the services (such as OpenDAP service)
     *       thredds.servlet.DataRootHandler.getInstance().reinit();
     *
     *
     * Tomcat Manager
     *   Some people suggested to use the Tomcat Manager app, which we usually delete for security reason.
     *     It can be called programmatically, using HTTP calls. That solution seems to be the worst so far.
     *
     *
     * Solution:
     *   Implement the interface "ApplicationContextAware" and create the method "setApplicationContext()"
     *     This method is automatically called by Spring Framework.
     *   Call "thredds.servlet.DataRootHandler.getInstance().reinit()" to reload THREDDS catalogue,
     *     and break Spring Framework services.
     *   Cast the "ApplicationContext" into an "AbstractApplicationContext", to get access to the "refresh" method,
     *     and call the "refresh" method to reload Spring Framework services.
     */
    public void reloadThreddsCatalogue() {
        // Reload THREDDS catalogue
        //   This will remove old catalogue entries and add the new ones
        DataRootHandler.getInstance().reinit();

        // Refresh Spring Framework context
        //   This reset the REST API listeners, otherwise some services stop to work (such as the OPeNDAP service)
        ((AbstractApplicationContext)this.applicationContext).refresh();
    }

    protected static class S3File {
        private List<S3File> children;
        private String bucket;
        private String path;

        public S3File(String bucket, String path) {
            this.bucket = bucket;
            this.path = path;
            this.children = null;

            while (this.path.endsWith("/")) {
                this.path = this.path.substring(0, this.path.length() - 2);
            }
        }

        public void addChild(S3File child) {
            if (this.children == null) {
                this.children = new ArrayList<S3File>();
            }

            this.children.add(child);
        }

        public boolean isDirectory() {
            return this.children != null && !this.children.isEmpty();
        }

        public List<S3File> listFiles() {
            return this.children;
        }

        public String getBucket() {
            return this.bucket;
        }

        public String getFilename() {
            return this.path.substring(this.path.lastIndexOf("/") + 1);
        }

        public String getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            return this.toString(0, "  ");
        }

        public String toString(int indent, String tab) {
            String nl = System.lineSeparator();
            String indentStr = "";
            if (indent > 0 && tab != null && tab.length() > 0) {
                StringBuilder indentSb = new StringBuilder();
                for (int i = 0; i<indent; i++) {
                    indentSb.append(tab);
                }
                indentStr = indentSb.toString();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(indentStr).append("{").append(nl);
            sb.append(indentStr).append(tab).append("bucket: ").append(this.bucket).append(nl);
            sb.append(indentStr).append(tab).append("path: ").append(this.path).append(nl);
            sb.append(indentStr).append(tab).append("filename: ").append(this.getFilename()).append(nl);
            if (this.isDirectory()) {
                sb.append(indentStr).append(tab).append("children: [").append(nl);
                for (S3File child : this.children) {
                    sb.append(child.toString(indent + 2, tab)).append(nl);
                }
                sb.append(indentStr).append(tab).append("]").append(nl);
            }
            sb.append(indentStr).append("}");

            return sb.toString();
        }
    }
}
