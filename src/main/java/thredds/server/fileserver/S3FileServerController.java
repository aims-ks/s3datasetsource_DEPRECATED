package thredds.server.fileserver;

import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.servlet.DatasetHandler;
import thredds.servlet.Debug;
import thredds.util.TdsPathUtils;
import ucar.nc2.util.IO;
import uk.co.informaticslab.Constants;
import uk.co.informaticslab.S3DatasetSource;
import uk.co.informaticslab.S3RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * HTTP File Serving for S3 files
 *
 * handles /s3FileServer/*
 */
@Controller
@RequestMapping("/s3FileServer")
public class S3FileServerController implements LastModified {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileServerController.class);

    private final AmazonS3 s3Client = Constants.getS3Client();

    private Map<String, byte[]> cache = new HashMap<String, byte[]>();
    private LinkedList<String> index = new LinkedList<String>();


    @Override
    public long getLastModified(HttpServletRequest request) {
        String reqPath = TdsPathUtils.extractPath(request, "s3FileServer/");
        if (reqPath == null) {
            return -1;
        }

        try (S3RandomAccessFile file = this.getS3RandomAccessFile(reqPath)) {
            if (file != null) {
                return file.getLastModified();
            }
        } catch(IOException ex) {
            LOGGER.error(String.format("Error occurred while accessing the file %s", reqPath), ex);
        }

        return -1;
    }

    @RequestMapping("**")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String reqPath = TdsPathUtils.extractPath(request, "s3FileServer/");
        if (reqPath == null) {
            return;
        }

        if (!DatasetHandler.resourceControlOk(request, response, reqPath)) {
            return;
        }

        try (S3RandomAccessFile file = this.getS3RandomAccessFile(reqPath)) {
            if (file != null) {
                S3FileServerController.returnS3RandomAccessFile(request, response, file, null);
            }
        }
    }

    private S3RandomAccessFile getS3RandomAccessFile(String reqPath) throws IOException {
        if (!reqPath.startsWith("/")) {
            reqPath = "/" + reqPath;
        }
        String s3Url = S3DatasetSource.createS3UrlFromPath(reqPath);

        if (s3Url == null || s3Url.isEmpty()) {
            LOGGER.error("Request path is null");
            return null;
        }

        try {
            return new S3RandomAccessFile(this.index, this.cache, this.s3Client, s3Url);
        } catch (Throwable ex) {
            LOGGER.error("Error occurred while initialising the S3RandomAccessFile", ex);
            throw ex;
        }
    }


    /**
     * Write a S3RandomAccessFile to the response stream. Handles Range requests.
     * Inspired from
     *     thredds.servlet.ServletUtil.returnFile(HttpServletRequest req, HttpServletResponse res, File file, String contentType)
     *
     * @param request request
     * @param response response
     * @param randomAccessFile must exist
     * @param contentType must not be null
     * @throws IOException or error
     */
    public static void returnS3RandomAccessFile(HttpServletRequest request, HttpServletResponse response, S3RandomAccessFile randomAccessFile, String contentType) throws IOException {
        response.setContentType(contentType);

        String filePath = randomAccessFile.getLocation();
        String filename = filePath.substring(filePath.lastIndexOf('/') + 1);

        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));

        // see if its a Range Request
        boolean isRangeRequest = false;
        long startPos = 0, endPos = Long.MAX_VALUE;
        String rangeRequest = request.getHeader("Range");
        if (rangeRequest != null) { // bytes=12-34 or bytes=12-
            int pos = rangeRequest.indexOf("=");
            if (pos > 0) {
                int pos2 = rangeRequest.indexOf("-");
                if (pos2 > 0) {
                    String startString = rangeRequest.substring(pos + 1, pos2);
                    String endString = rangeRequest.substring(pos2 + 1);
                    startPos = Long.parseLong(startString);
                    if (endString.length() > 0)
                        endPos = Long.parseLong(endString) + 1;
                    isRangeRequest = true;
                }
            }
        }

        // set content length
        long fileSize = randomAccessFile.length();
        long contentLength = fileSize;
        if (isRangeRequest) {
            endPos = Math.min(endPos, fileSize);
            contentLength = endPos - startPos;
        }

        // when compression is turned on, ContentLength has to be overridden
        // this is also true for HEAD, since this must be the same as GET without the body
        if (contentLength > Integer.MAX_VALUE) {
            response.addHeader("Content-Length", Long.toString(contentLength));  // allow content length > MAX_INT
        } else {
            response.setContentLength((int)contentLength);
        }

        boolean debugRequest = Debug.isSet("returnFile");
        if (debugRequest) {
            LOGGER.debug(String.format("returnFile(): filename = %s contentType = %s contentLength = %d",
                    filename, contentType, contentLength));
        }

        // indicate we allow Range Requests
        response.addHeader("Accept-Ranges", "bytes");

        if (request.getMethod().equals("HEAD")) {
            return;
        }

        // @todo Split up this exception handling: those from file access vs those from dealing with response
        //       File access: catch and res.sendError()
        //       response: don't catch (let bubble up out of doGet() etc)
        try {

            IO.copyRafB(randomAccessFile, startPos, contentLength, response.getOutputStream(), new byte[60000]);

            if (debugRequest) {
                LOGGER.debug(String.format("returnFile(): returnFile ok = %s", filename));
            }

        } catch (FileNotFoundException e) {
            LOGGER.error(String.format("returnFile(): FileNotFoundException= %s", filename));
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (java.net.SocketException e) {
            LOGGER.info(String.format("returnFile(): SocketException sending file: %s %s", filename, e.getMessage()));

        } catch (IOException e) {
            String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
            if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
                LOGGER.debug(String.format("returnFile(): ClientAbortException while sending file: %s %s", filename, e.getMessage()));
                return;
            }

            if (e.getMessage().startsWith("File transfer not complete")) { // coming from FileTransfer.transferTo()
                LOGGER.debug(String.format("returnFile() %s", e.getMessage()));
                return;
            }

            LOGGER.error(String.format("returnFile(): IOException (%s) sending file", e.getClass().getName()), e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("Problem sending file: %s", e.getMessage()));
            }
        }
    }
}
