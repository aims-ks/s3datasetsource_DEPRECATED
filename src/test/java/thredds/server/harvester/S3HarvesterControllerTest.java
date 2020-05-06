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

import javax.xml.transform.TransformerException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class S3HarvesterControllerTest {

    @Test
    public void testParseFilePaths() throws TransformerException {
        String bucket = "bucket";
        Set<S3HarvesterConfiguration.S3HarvesterPathConfiguration> filePaths = new TreeSet<S3HarvesterConfiguration.S3HarvesterPathConfiguration>();
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/all-one/gbr4_bgc_924-all-one.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2014.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2015.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2014-12.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2015-01.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2014-12.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2015-01.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/all-one/gbr4_v2-all-one.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/annual-annual/gbr4_v2-annual-annual-2010.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-10.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-10.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-10.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-10.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-10.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-09.nc", null));
        filePaths.add(new S3HarvesterConfiguration.S3HarvesterPathConfiguration("derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-10.nc", null));

        S3HarvesterController.S3File s3FileTree = S3HarvesterController.parseFilePaths(bucket, filePaths);


        // Test tree structure

        this.assertTreeNode(s3FileTree, "bucket", "bucket", "bucket", 1);

        for (S3HarvesterController.S3File lvl1File : s3FileTree.listFiles()) {
            this.assertTreeNode(lvl1File, "bucket", "derived", "bucket/derived", 1);

            for (S3HarvesterController.S3File lvl2File : lvl1File.listFiles()) {
                this.assertTreeNode(lvl2File, "bucket", "ncaggregate", "bucket/derived/ncaggregate", 1);

                for (S3HarvesterController.S3File lvl3File : lvl2File.listFiles()) {
                    this.assertTreeNode(lvl3File, "bucket", "ereefs", "bucket/derived/ncaggregate/ereefs", 2);

                    for (S3HarvesterController.S3File lvl4File : lvl3File.listFiles()) {
                        switch (lvl4File.getFilename()) {
                            case "gbr4_bgc_924":
                                this.testGbr4BgcTree(lvl4File);
                                break;

                            case "gbr4_v2":
                                this.testGbr4V2Tree(lvl4File);
                                break;

                            default:
                                Assert.fail(String.format("Unexpected file %s found in path %s", lvl4File.getFilename(), lvl3File.getPath()));
                        }
                    }
                }
            }
        }
    }

    private void testGbr4BgcTree(S3HarvesterController.S3File gbr4BgcFile) {
        this.assertTreeNode(gbr4BgcFile, "bucket", "gbr4_bgc_924", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924", 1);

        for (S3HarvesterController.S3File lvl5File : gbr4BgcFile.listFiles()) {
            this.assertTreeNode(lvl5File, "bucket", "ongoing", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing", 4);

            for (S3HarvesterController.S3File lvl6File : lvl5File.listFiles()) {
                switch (lvl6File.getFilename()) {
                    case "all-one":
                        this.assertTreeNode(lvl6File, "bucket", "all-one", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/all-one", 1);

                        for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                            this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-all-one.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/all-one/gbr4_bgc_924-all-one.nc", 0);
                        }
                        break;

                    case "annual-annual":
                        this.assertTreeNode(lvl6File, "bucket", "annual-annual", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual", 2);

                        for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                            switch (lvl7File.getFilename()) {
                                case "gbr4_bgc_924-annual-annual-2014.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-annual-annual-2014.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2014.nc", 0);
                                    break;

                                case "gbr4_bgc_924-annual-annual-2015.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-annual-annual-2015.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2015.nc", 0);
                                    break;

                                default:
                                    Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                            }
                        }
                        break;

                    case "daily-monthly":
                        this.assertTreeNode(lvl6File, "bucket", "daily-monthly", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly", 2);

                        for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                            switch (lvl7File.getFilename()) {
                                case "gbr4_bgc_924-daily-monthly-2014-12.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-daily-monthly-2014-12.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2014-12.nc", 0);
                                    break;

                                case "gbr4_bgc_924-daily-monthly-2015-01.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-daily-monthly-2015-01.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2015-01.nc", 0);
                                    break;

                                default:
                                    Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                            }
                        }
                        break;

                    case "monthly-monthly":
                        this.assertTreeNode(lvl6File, "bucket", "monthly-monthly", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly", 2);

                        for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                            switch (lvl7File.getFilename()) {
                                case "gbr4_bgc_924-monthly-monthly-2014-12.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-monthly-monthly-2014-12.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2014-12.nc", 0);
                                    break;

                                case "gbr4_bgc_924-monthly-monthly-2015-01.nc":
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_bgc_924-monthly-monthly-2015-01.nc", "bucket/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2015-01.nc", 0);
                                    break;

                                default:
                                    Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                            }
                        }
                        break;

                    default:
                        Assert.fail(String.format("Unexpected file %s found in path %s", lvl6File.getFilename(), lvl5File.getPath()));
                }
            }
        }
    }

    private void testGbr4V2Tree(S3HarvesterController.S3File gbr4V2File) {
        this.assertTreeNode(gbr4V2File, "bucket", "gbr4_v2", "bucket/derived/ncaggregate/ereefs/gbr4_v2", 2);

        for (S3HarvesterController.S3File lvl5File : gbr4V2File.listFiles()) {
            switch (lvl5File.getFilename()) {
                case "ongoing":
                    this.assertTreeNode(lvl5File, "bucket", "ongoing", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing", 4);

                    for (S3HarvesterController.S3File lvl6File : lvl5File.listFiles()) {
                        switch (lvl6File.getFilename()) {
                            case "all-one":
                                this.assertTreeNode(lvl6File, "bucket", "all-one", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/all-one", 1);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-all-one.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/all-one/gbr4_v2-all-one.nc", 0);
                                }
                                break;

                            case "annual-annual":
                                this.assertTreeNode(lvl6File, "bucket", "annual-annual", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/annual-annual", 1);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-annual-annual-2010.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/annual-annual/gbr4_v2-annual-annual-2010.nc", 0);
                                }
                                break;

                            case "daily-monthly":
                                this.assertTreeNode(lvl6File, "bucket", "daily-monthly", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-daily-monthly-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-daily-monthly-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-daily-monthly-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-daily-monthly-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            case "monthly-monthly":
                                this.assertTreeNode(lvl6File, "bucket", "monthly-monthly", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-monthly-monthly-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-monthly-monthly-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-monthly-monthly-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-monthly-monthly-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            default:
                                Assert.fail(String.format("Unexpected file %s found in path %s", lvl6File.getFilename(), lvl5File.getPath()));
                        }
                    }
                    break;

                case "raw":
                    this.assertTreeNode(lvl5File, "bucket", "raw", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw", 4);

                    for (S3HarvesterController.S3File lvl6File : lvl5File.listFiles()) {
                        switch (lvl6File.getFilename()) {
                            case "current":
                                this.assertTreeNode(lvl6File, "bucket", "current", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/current", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-raw-current-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-current-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-raw-current-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-current-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            case "salt":
                                this.assertTreeNode(lvl6File, "bucket", "salt", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/salt", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-raw-salt-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-salt-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-raw-salt-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-salt-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            case "temp":
                                this.assertTreeNode(lvl6File, "bucket", "temp", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/temp", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-raw-temp-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-temp-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-raw-temp-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-temp-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            case "wspeed":
                                this.assertTreeNode(lvl6File, "bucket", "wspeed", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed", 2);

                                for (S3HarvesterController.S3File lvl7File : lvl6File.listFiles()) {
                                    switch (lvl7File.getFilename()) {
                                        case "gbr4_v2-raw-wspeed-2010-09.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-wspeed-2010-09.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-09.nc", 0);
                                            break;

                                        case "gbr4_v2-raw-wspeed-2010-10.nc":
                                            this.assertTreeNode(lvl7File, "bucket", "gbr4_v2-raw-wspeed-2010-10.nc", "bucket/derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-10.nc", 0);
                                            break;

                                        default:
                                            Assert.fail(String.format("Unexpected file %s found in path %s", lvl7File.getFilename(), lvl6File.getPath()));
                                    }
                                }
                                break;

                            default:
                                Assert.fail(String.format("Unexpected file %s found in path %s", lvl6File.getFilename(), lvl5File.getPath()));
                        }
                    }
                    break;

                default:
                    Assert.fail(String.format("Unexpected file %s found in path %s", lvl5File.getFilename(), gbr4V2File.getPath()));
            }
        }
    }

    private void assertTreeNode(
            S3HarvesterController.S3File treeNode,
            String bucket,
            String filename,
            String path,
            int numberOfChildren) {

        Assert.assertEquals(String.format("Wrong bucket for node %s", path), bucket, treeNode.getBucket());
        Assert.assertEquals(String.format("Wrong filename for node %s", path), filename, treeNode.getFilename());
        Assert.assertEquals(String.format("Wrong path for node %s", path), path, treeNode.getPath());

        List<S3HarvesterController.S3File> files = treeNode.listFiles();
        if (numberOfChildren > 0) {
            Assert.assertNotNull(String.format("List of files is null for node %s", path), files);
            Assert.assertFalse(String.format("Node %s contains no files", path), files.isEmpty());
            Assert.assertEquals(String.format("Node %s contains wrong number of files", path), numberOfChildren, files.size());
        } else {
            Assert.assertTrue(String.format("Node %s contains files", path), files == null || files.isEmpty());
        }
    }
}
