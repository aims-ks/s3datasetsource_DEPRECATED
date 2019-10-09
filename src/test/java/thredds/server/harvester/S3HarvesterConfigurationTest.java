package thredds.server.harvester;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

public class S3HarvesterConfigurationTest {

    @Test
    public void testParseConfigFile() throws Exception {
        // Find the config file in the test/resources directory
        URL configFileURL = S3HarvesterConfigurationTest.class.getClassLoader().getResource("s3harvester.xml");
        File configFile = new File(configFileURL.toURI());

        // Parse the config file
        S3HarvesterConfiguration config = new S3HarvesterConfiguration(configFile);
        config.init();


        // Test parsed values

        File s3CatalogueDirectory = config.getS3CatalogueDirectory();
        Assert.assertNotNull("S3CatalogueDirectory is null", s3CatalogueDirectory);
        Assert.assertEquals("Wrong S3CatalogueDirectory", "/usr/local/tomcat/content/thredds/s3catalogue", s3CatalogueDirectory.getAbsolutePath());

        List<S3HarvesterConfiguration.S3HarvesterBucketConfiguration> bucketConfigurations = config.getBucketConfigurations();
        Assert.assertNotNull("Bucket configurations is null", bucketConfigurations);
        Assert.assertEquals("Wrong number of bucket configurations", 1, bucketConfigurations.size());

        S3HarvesterConfiguration.S3HarvesterBucketConfiguration publicBucket = bucketConfigurations.get(0);
        Assert.assertNotNull("First bucket is missing", publicBucket);
        Assert.assertEquals("Wrong first bucket name", "aims-ereefs-public-test", publicBucket.getBucket());

        List<String> firstBucketPaths = publicBucket.getPaths();
        Assert.assertNotNull("First bucket have no paths", firstBucketPaths);
        Assert.assertEquals("Wrong number of paths in first bucket", 1, firstBucketPaths.size());

        String firstBucketPath = firstBucketPaths.get(0);
        Assert.assertNotNull("First path of first bucket is null", firstBucketPath);
        Assert.assertEquals("Wrong first path of first bucket", "derived/ncaggregate", firstBucketPath);
    }
}
