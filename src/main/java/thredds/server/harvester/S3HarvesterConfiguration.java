package thredds.server.harvester;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class S3HarvesterConfiguration {
    private File configFile;

    // Directory where the S3 "catalog.xml" files are generated
    private File s3CatalogueDirectory;

    private List<S3HarvesterBucketConfiguration> bucketConfigs;

    public S3HarvesterConfiguration(File configFile) {
        this.configFile = configFile;
    }

    public void init() throws Exception {
        this.bucketConfigs = new ArrayList<S3HarvesterBucketConfiguration>();
        this.s3CatalogueDirectory = null;

        if (this.configFile == null) {
            throw new Exception("Invalid S3Harvester configuration file. The configuration file is null.");
        }
        if (!this.configFile.exists()) {
            throw new FileNotFoundException(String.format("Invalid S3Harvester configuration file: %s. The file doesn't exists.", this.configFile));
        }
        if (!this.configFile.canRead()) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. The file is not readable.", this.configFile));
        }
        if (!this.configFile.isFile()) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. The file is not a normal file.", this.configFile));
        }

        // Parse the XML config file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try (
            InputStream configFileInputStream = new FileInputStream(this.configFile)
        ) {
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(configFileInputStream);

            this.parseCatalogueDirectory(doc);
            this.parseBuckets(doc);
        }
    }

    private void parseCatalogueDirectory(Document doc) throws Exception {
        NodeList catalogueDirectoryNodes = doc.getElementsByTagName("catalogueDirectory");
        int nbCatalogueDirectoryNodes = catalogueDirectoryNodes == null ? 0 : catalogueDirectoryNodes.getLength();
        if (catalogueDirectoryNodes == null || nbCatalogueDirectoryNodes <= 0) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Missing element \"s3HarvesterConfig.catalogueDirectory\".", this.configFile));
        }
        if (nbCatalogueDirectoryNodes > 1) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. More than one \"s3HarvesterConfig.catalogueDirectory\" element found.", this.configFile));
        }

        String s3CatalogueDirectoryStr = catalogueDirectoryNodes.item(0).getTextContent();
        if (s3CatalogueDirectoryStr == null || s3CatalogueDirectoryStr.isEmpty()) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Element \"s3HarvesterConfig.catalogueDirectory\" contains no value.", this.configFile));
        }
        this.s3CatalogueDirectory = new File(s3CatalogueDirectoryStr);
    }

    private void parseBuckets(Document doc) throws Exception {
        NodeList bucketsNodes = doc.getElementsByTagName("buckets");
        int nbBucketsNodes = bucketsNodes == null ? 0 : bucketsNodes.getLength();
        if (bucketsNodes == null || nbBucketsNodes <= 0) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Missing element \"s3HarvesterConfig.buckets\".", this.configFile));
        }
        if (nbBucketsNodes > 1) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. More than one \"s3HarvesterConfig.buckets\" element found.", this.configFile));
        }

        Element bucketsElement = (Element)(bucketsNodes.item(0));
        NodeList bucketNodes = bucketsElement.getElementsByTagName("bucket");
        int nbBucketNodes = bucketNodes == null ? 0 : bucketNodes.getLength();
        if (bucketNodes == null || nbBucketNodes <= 0) {
            throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Missing element \"s3HarvesterConfig.buckets.bucket\".", this.configFile));
        }

        for (int i=0; i<nbBucketNodes; i++) {
            S3HarvesterBucketConfiguration bucket = this.parseBucket((Element)bucketNodes.item(i));
            this.bucketConfigs.add(bucket);
        }
    }

    private S3HarvesterBucketConfiguration parseBucket(Element bucketElement) throws Exception {
        NamedNodeMap bucketAttributes = bucketElement.getAttributes();
        Node nameNode = bucketAttributes.getNamedItem("name");
        String bucketName = nameNode.getTextContent();

        S3HarvesterBucketConfiguration bucketConfig = new S3HarvesterBucketConfiguration(bucketName);

        NodeList bucketPaths = bucketElement.getElementsByTagName("paths");
        int nbBucketPaths = bucketPaths == null ? 0 : bucketPaths.getLength();
        if (bucketPaths != null && nbBucketPaths > 0) {
            if (nbBucketPaths > 1) {
                throw new Exception(String.format("Invalid S3Harvester configuration file: %s. More than one \"s3HarvesterConfig.buckets.bucket.paths\" element found for bucket %s.", this.configFile, bucketName));
            }

            Element bucketPathsElement = (Element)(bucketPaths.item(0));
            NodeList bucketPathNodes = bucketPathsElement.getElementsByTagName("path");
            int nbBucketPathNodes = bucketPathNodes == null ? 0 : bucketPathNodes.getLength();
            if (bucketPathNodes == null || nbBucketPathNodes <= 0) {
                throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Missing element \"s3HarvesterConfig.buckets.bucket.paths.path\" for bucket %s.", this.configFile, bucketName));
            }

            for (int i=0; i<nbBucketPathNodes; i++) {
                String bucketPath = this.parseBucketPath((Element)bucketPathNodes.item(i));
                bucketConfig.addPath(bucketPath);
            }
        }

        return bucketConfig;
    }

    private String parseBucketPath(Element bucketPathElement) throws Exception {
        return bucketPathElement.getTextContent();
    }

    public File getConfigFile() {
        return this.configFile;
    }

    public File getS3CatalogueDirectory() {
        return this.s3CatalogueDirectory;
    }

    public List<S3HarvesterBucketConfiguration> getBucketConfigurations() {
        return this.bucketConfigs;
    }

    public static class S3HarvesterBucketConfiguration {
        private String bucket;
        private List<String> paths;

        public S3HarvesterBucketConfiguration(String bucket) {
            this.bucket = bucket;
        }

        protected void addPath(String path) {
            if (this.paths == null) {
                this.paths = new ArrayList<String>();
            }
            this.paths.add(path);
        }

        public String getBucket() {
            return this.bucket;
        }

        public List<String> getPaths() {
            return this.paths;
        }
    }
}
