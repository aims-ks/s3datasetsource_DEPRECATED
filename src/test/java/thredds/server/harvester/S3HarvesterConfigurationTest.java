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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

public class S3HarvesterConfigurationTest {

    @Test
    public void testParseSmallConfigFile() throws Exception {
        // Find the config file in the test/resources directory
        URL configFileURL = S3HarvesterConfigurationTest.class.getClassLoader().getResource("s3harvester.xml");
        File configFile = new File(configFileURL.toURI());
        Assert.assertTrue(String.format("Can not find config file: %s", configFile), configFile.canRead());

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

    @Test(expected = Exception.class)
    public void testParseEmptyConfigFile() throws Exception {
        // Find the config file in the test/resources directory
        URL configFileURL = S3HarvesterConfigurationTest.class.getClassLoader().getResource("s3harvester_empty.xml");
        File configFile = new File(configFileURL.toURI());
        Assert.assertTrue(String.format("Can not find config file: %s", configFile), configFile.canRead());

        // Parse the config file
        S3HarvesterConfiguration config = new S3HarvesterConfiguration(configFile);
        config.init();
    }

    @Test
    public void testParseLargeConfigFile() throws Exception {
        // Find the config file in the test/resources directory
        URL configFileURL = S3HarvesterConfigurationTest.class.getClassLoader().getResource("s3harvester_large.xml");
        File configFile = new File(configFileURL.toURI());
        Assert.assertTrue(String.format("Can not find config file: %s", configFile), configFile.canRead());

        // Parse the config file
        S3HarvesterConfiguration config = new S3HarvesterConfiguration(configFile);
        config.init();


        // Test parsed values

        File s3CatalogueDirectory = config.getS3CatalogueDirectory();
        Assert.assertNotNull("S3CatalogueDirectory is null", s3CatalogueDirectory);
        Assert.assertEquals("Wrong S3CatalogueDirectory", "/usr/local/tomcat/content/thredds/s3catalogue", s3CatalogueDirectory.getAbsolutePath());

        List<S3HarvesterConfiguration.S3HarvesterBucketConfiguration> bucketConfigurations = config.getBucketConfigurations();
        Assert.assertNotNull("Bucket configurations is null", bucketConfigurations);
        Assert.assertEquals("Wrong number of bucket configurations", 3, bucketConfigurations.size());


        S3HarvesterConfiguration.S3HarvesterBucketConfiguration publicBucket = bucketConfigurations.get(0);
        Assert.assertNotNull("First bucket is missing", publicBucket);
        Assert.assertEquals("Wrong first bucket name", "aims-ereefs-public-test", publicBucket.getBucket());

        List<String> publicBucketPaths = publicBucket.getPaths();
        Assert.assertNotNull("First bucket have no paths", publicBucketPaths);
        Assert.assertEquals("Wrong number of paths in first bucket", 1, publicBucketPaths.size());

        String publicBucketPath = publicBucketPaths.get(0);
        Assert.assertNotNull("First path of first bucket is null", publicBucketPath);
        Assert.assertEquals("Wrong first path of first bucket", "derived/ncaggregate", publicBucketPath);


        S3HarvesterConfiguration.S3HarvesterBucketConfiguration privateBucket = bucketConfigurations.get(1);
        Assert.assertNotNull("2nd bucket is missing", privateBucket);
        Assert.assertEquals("Wrong 2nd bucket name", "aims-ereefs-private-test", privateBucket.getBucket());

        List<String> privateBucketPaths = privateBucket.getPaths();
        Assert.assertNotNull("2nd bucket have no paths", privateBucketPaths);
        Assert.assertEquals("Wrong number of paths in 2nd bucket", 2, privateBucketPaths.size());

        String privateBucketPath0 = privateBucketPaths.get(0);
        Assert.assertNotNull("First path of 2nd bucket is null", privateBucketPath0);
        Assert.assertEquals("Wrong first path of 2nd bucket", "ongoing/ncaggregate", privateBucketPath0);

        String privateBucketPath1 = privateBucketPaths.get(1);
        Assert.assertNotNull("2nd path of 2nd bucket is null", privateBucketPath1);
        Assert.assertEquals("Wrong 2nd path of 2nd bucket", "ongoing/ncanimate", privateBucketPath1);


        S3HarvesterConfiguration.S3HarvesterBucketConfiguration ereefsBucket = bucketConfigurations.get(2);
        Assert.assertNotNull("3rd bucket is missing", ereefsBucket);
        Assert.assertEquals("Wrong 3rd bucket name", "aims-ereefs", ereefsBucket.getBucket());

        List<String> ereefsBucketPaths = ereefsBucket.getPaths();
        Assert.assertTrue("3rd bucket have paths", ereefsBucketPaths == null || ereefsBucketPaths.isEmpty());
    }
}
