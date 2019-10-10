package uk.co.informaticslab;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.log4j.Logger;
import thredds.servlet.DatasetSource;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.hdf5.H5iosp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * {@link DatasetSource} implementation to read directly from s3
 */
public class S3DatasetSource implements DatasetSource {
    private static final Logger LOGGER = Logger.getLogger(S3DatasetSource.class);

    // Prefix for S3 URLs
    public static final String S3_PREFIX = "s3://";

    // THREDDS tends to break S3 URLs.
    //   S3 URL:
    //     s3://bucket/file.nc
    //   THREDDS path (depending on context):
    //     /s3:/bucket/file.nc
    //     s3:/bucket/file.nc
    private static final String BROKEN_PATH_PREFIX = "s3:/";

    private final AmazonS3 client = Constants.getS3Client();

    private Map<String, byte[]> cache = new HashMap<String, byte[]>();
    private LinkedList<String> index = new LinkedList<String>();

    @Override
    public boolean isMine(HttpServletRequest req) {
        String path = req.getPathInfo();
        boolean isMine = S3DatasetSource.isS3Path(path);
        LOGGER.debug(String.format("Path [%s] is mine [%b]", path, isMine));
        return isMine;
    }

    @Override
    public NetcdfFile getNetcdfFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String s3Url = S3DatasetSource.createS3UrlFromPath(request.getPathInfo());
        LOGGER.debug(String.format("Accessing NetCDF file in S3 on url [%s]", s3Url));

        S3RandomAccessFile s3RandomAccessFile = new S3RandomAccessFile(this.index, this.cache, this.client, s3Url);

        // If file not found:
        if (!s3RandomAccessFile.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("S3 file not found: %s", s3Url));
            return null;
        }

        IOServiceProvider iosp = new H5iosp();
        NetcdfFile ncf = new NetcdfFileSubclass(iosp, s3RandomAccessFile, null, null) {
            // Fix the getLastModified method, but that's a bit pointless
            // since THREDDS don't even use it.
            @Override
            public long getLastModified() {
                return s3RandomAccessFile.getLastModified();
            }
        };

        return ncf;
    }

    public static boolean isS3Path(String path) {
        // Remove "/" at the beginning
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path.startsWith(S3_PREFIX) ||
                path.startsWith(BROKEN_PATH_PREFIX);
    }

    public static String createS3UrlFromPath(String path) {
        // Remove "/" at the beginning
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Fix S3 URL
        if (S3DatasetSource.isS3Path(path) && !path.startsWith(S3_PREFIX)) {
            if (path.startsWith(BROKEN_PATH_PREFIX)) {
                path = path.substring(BROKEN_PATH_PREFIX.length());
            }
            path = S3_PREFIX + path;
        }

        // Remove ".html" (or anything else) following the ".nc"
        if (!path.endsWith(".nc")) {
            path = path.substring(0, path.lastIndexOf(".nc") + 3);
        }
        return path;
    }
}
