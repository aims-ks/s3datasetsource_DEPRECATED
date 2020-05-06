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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class S3HarvesterConfiguration {
    private static final File DEFAULT_S3_CATALOGUE_DIRECTORY = new File("/usr/local/tomcat/content/thredds/s3catalogue");

    private File configFile;

    // Directory where the S3 "catalog.xml" files are generated
    private File s3CatalogueDirectory;

    private List<S3HarvesterBucketConfiguration> bucketConfigs;

    public S3HarvesterConfiguration(File configFile) {
        this.configFile = configFile;
    }

    public void init() throws Exception {
        this.bucketConfigs = new ArrayList<S3HarvesterBucketConfiguration>();
        this.s3CatalogueDirectory = DEFAULT_S3_CATALOGUE_DIRECTORY;

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
        if (catalogueDirectoryNodes != null && nbCatalogueDirectoryNodes > 0) {
            if (nbCatalogueDirectoryNodes > 1) {
                throw new Exception(String.format("Invalid S3Harvester configuration file: %s. More than one \"s3HarvesterConfig.catalogueDirectory\" element found.", this.configFile));
            }

            String s3CatalogueDirectoryStr = catalogueDirectoryNodes.item(0).getTextContent();
            if (s3CatalogueDirectoryStr == null || s3CatalogueDirectoryStr.isEmpty()) {
                throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Element \"s3HarvesterConfig.catalogueDirectory\" contains no value.", this.configFile));
            }
            this.s3CatalogueDirectory = new File(s3CatalogueDirectoryStr);
        }
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

        S3HarvesterBucketConfiguration bucketConfig = new S3HarvesterBucketConfiguration(bucketName, this.configFile);

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
                Element bucketPathElement = (Element)bucketPathNodes.item(i);
                bucketConfig.addPath(bucketPathElement);
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
        private File configFile;
        private String bucket;
        private List<S3HarvesterPathConfiguration> paths;

        public S3HarvesterBucketConfiguration(String bucket, File configFile) {
            this.bucket = bucket;
            this.configFile = configFile;
        }

        protected void addPath(Element pathEl) throws Exception {
            if (this.paths == null) {
                this.paths = new ArrayList<S3HarvesterPathConfiguration>();
            }
            this.paths.add(new S3HarvesterPathConfiguration(pathEl, this.bucket, this.configFile));
        }

        public String getBucket() {
            return this.bucket;
        }

        public List<S3HarvesterPathConfiguration> getPaths() {
            return this.paths;
        }
    }

    public static class S3HarvesterPathConfiguration implements Comparable<S3HarvesterPathConfiguration> {
        private static Transformer transformer;

        private File configFile;
        private String bucket;
        private String path;
        private Element metadata;

        public S3HarvesterPathConfiguration(Element pathEl, String bucket, File configFile) throws Exception {
            this.bucket = bucket;
            this.configFile = configFile;

            this.path = pathEl.getAttribute("value");

            NodeList metadataNodes = pathEl.getElementsByTagName("metadata");
            if (metadataNodes != null) {
                int nbMetadataNodes = metadataNodes.getLength();
                if (nbMetadataNodes > 0) {
                    if (nbMetadataNodes > 1) {
                        throw new Exception(String.format("Invalid S3Harvester configuration file: %s. Too many metadata elements in \"s3HarvesterConfig.buckets.bucket.paths.path\" for bucket %s.",
                                this.configFile, this.bucket));
                    }

                    this.metadata = (Element)metadataNodes.item(0);
                }
            }
        }

        public S3HarvesterPathConfiguration(String path, Element metadata) {
            this.path = path;
            this.metadata = metadata;
        }

        public String getPath() {
            return this.path;
        }

        public Element getMetadata() {
            return this.metadata;
        }

        public String getMetadataStr() throws TransformerException {
            if (this.metadata == null) {
                return null;
            }

            if (transformer == null) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            StringWriter buffer = new StringWriter();
            transformer.transform(new DOMSource(this.metadata),
                  new StreamResult(buffer));
            return buffer.toString();
        }

        @Override
        public int compareTo(S3HarvesterPathConfiguration other) {
            if (other == null) {
                return 1;
            }

            // Same instance
            if (this == other) {
                return 0;
            }

            String otherPath = other.getPath();

            // Both null or same String instance
            if (this.path == otherPath) {
                return 0;
            }

            if (this.path == null) {
                return -1;
            }
            if (otherPath == null) {
                return 1;
            }

            return this.path.compareTo(otherPath);
        }
    }
}
