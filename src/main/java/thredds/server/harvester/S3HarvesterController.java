package thredds.server.harvester;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.informaticslab.ThreddsServerUtils;
import uk.co.informaticslab.Constants;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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
public class S3HarvesterController {
    private static final Logger LOGGER = Logger.getLogger(S3HarvesterController.class);

    private static final File CONFIG_ROOT_DIR = new File("/usr/local/tomcat/content/thredds");
    private static final File S3_HARVESTER_CONFIG_FILE = new File(CONFIG_ROOT_DIR, "s3harvester.xml");

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

    @RequestMapping("**")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (this.config == null) {
            LOGGER.warn("The S3 harvesting was not configured");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The S3 harvesting was not configured");

        } else {

            try {
                // TODO Security?
                // http://localhost:8888/thredds/s3harvester/


                // TODO
                // Generate in temp dir, to prevent breaking the system AND allow removal of S3 files

                for (S3HarvesterConfiguration.S3HarvesterBucketConfiguration bucketConfig : this.config.getBucketConfigurations()) {
                    String bucket = bucketConfig.getBucket();
                    List<String> paths = bucketConfig.getPaths();

                    // If the bucket is configured with no path, add a "null" path,
                    // to indicate that we want to harvest the whole bucket
                    if (paths == null || paths.size() <= 0) {
                        paths = new ArrayList<String>();
                        paths.add(null);
                    }

                    if (!this.s3Client.doesBucketExist(bucket)) {
                        LOGGER.error(String.format("Bucket %s does not exist", bucket));
                        return;
                    }

                    Set<String> netCDFFilePaths = new TreeSet<String>();
                    for (String path : paths) {
                        netCDFFilePaths.addAll(this.getNetCDFFilePaths(bucket, path));
                    }

                    S3File netCDFFileTree = S3HarvesterController.parseFilePaths(bucket, netCDFFilePaths);
                    this.createCatalogs(netCDFFileTree);
                }

                ThreddsServerUtils.reloadCatalogue();

                // Send a "Harvesting done" message as a response text
                try (ServletOutputStream outputStream = response.getOutputStream()) {
                    outputStream.println("Harvesting done");
                    outputStream.flush();
                }
            } catch (Exception ex) {
                LOGGER.error("Exception occurred while harvesting the S3 buckets", ex);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.format("Exception occurred while harvesting the S3 buckets: %s", ex.getMessage()));
                throw ex;
            }
        }
    }

    private File createCatalogs(S3File netCDFFile) throws IOException, TemplateException {
        if (netCDFFile == null || !netCDFFile.isDirectory()) {
            return null;
        }

        File catalogDir = new File(this.config.getS3CatalogueDirectory(), netCDFFile.path);
        File catalogFile = new File(catalogDir, "catalog.xml");

        try {
            CatalogTemplate catalogTemplate = new CatalogTemplate();

            for (S3File child : netCDFFile.listFiles()) {
                if (child.isDirectory()) {
                    this.createCatalogs(child);
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
