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
package uk.co.informaticslab;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class S3RandomAccessFileTest {

    private static final String URL = "s3://mogreps-g/prods_op_mogreps-g_20160101_00_00_015.nc";

    private AmazonS3 client;
    private S3RandomAccessFile raf;
    private Map<String, byte[]> cache;
    private LinkedList<String> index;


    @Before
    public void setUp() throws IOException {
        client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();
        cache = new HashMap<>();
        index = new LinkedList<>();
        raf = new S3RandomAccessFile(index, cache, client, URL);
    }

    // TODO FIX
    @Ignore
    @Test
    public void testLength() throws IOException {
        assertEquals("file length", 35948814, raf.length());
        raf.close();
    }

}
