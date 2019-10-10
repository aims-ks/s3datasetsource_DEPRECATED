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

import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

public class S3HarvesterControllerTest {

    @Test
    public void testParseFilePaths() {
        String bucket = "bucket";
        Set<String> filePaths = new TreeSet<String>();
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/all-one/gbr4_bgc_924-all-one.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2014.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/annual-annual/gbr4_bgc_924-annual-annual-2015.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2014-12.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/daily-monthly/gbr4_bgc_924-daily-monthly-2015-01.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2014-12.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/monthly-monthly/gbr4_bgc_924-monthly-monthly-2015-01.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/all-one/gbr4_v2-all-one.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/annual-annual/gbr4_v2-annual-annual-2010.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/daily-monthly/gbr4_v2-daily-monthly-2010-10.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/ongoing/monthly-monthly/gbr4_v2-monthly-monthly-2010-10.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/current/gbr4_v2-raw-current-2010-10.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/salt/gbr4_v2-raw-salt-2010-10.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/temp/gbr4_v2-raw-temp-2010-10.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-09.nc");
        filePaths.add("derived/ncaggregate/ereefs/gbr4_v2/raw/wspeed/gbr4_v2-raw-wspeed-2010-10.nc");

        S3HarvesterController.S3File s3FileTree = S3HarvesterController.parseFilePaths(bucket, filePaths);

        System.out.println(s3FileTree.toString());
    }
}
