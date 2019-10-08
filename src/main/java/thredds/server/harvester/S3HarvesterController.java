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

    // Directory where catalog.xml files are generated
    private static final File S3_CATALOGUE_DIR = new File("/usr/local/tomcat/content/thredds/s3catalogue");

    private final AmazonS3 s3Client = Constants.getS3Client();

    @RequestMapping("**")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            // TODO Security?
            // http://localhost:8888/thredds/s3harvester/


            // TODO
            // 1. Find the proper way to send an array of values (S3 Buckets) using REST API <=== Maybe I need a proper config file for that...
            // 2. Extract the bucket names from the reqPath
            // 3. Generate catalog.xml files for the given buckets (in "/tmp" maybe?)
            // 4. Delete old catalog.xml files for the given buckets (and move temp config from "/tmp" to the config folder)
            // 5. Reload THREDDS config (how??)
            //     touch web.xml



            // TODO put in config
            String bucket = "aims-ereefs-public-test";
            List<String> paths = new ArrayList<String>();
            paths.add("derived/ncaggregate");



            if (paths.size() <= 0) {
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

            // TODO Send a "Harvesting done" message as a response text
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                outputStream.println("Harvesting done");
                outputStream.flush();
            }

            ThreddsServerUtils.restart();
        } catch(Exception ex) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("Exception occurred while harvesting the S3 buckets: %s", ex.getMessage()));
            LOGGER.error("Exception occurred while harvesting the S3 buckets", ex);
            throw ex;
        }
    }

    private void createCatalogs(S3File netCDFFile) throws IOException, TemplateException {
        if (netCDFFile == null || !netCDFFile.isDirectory()) {
            return;
        }

        File catalogDir = new File(S3_CATALOGUE_DIR, netCDFFile.path);
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
