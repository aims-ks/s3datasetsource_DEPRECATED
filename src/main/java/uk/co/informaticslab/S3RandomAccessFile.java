package uk.co.informaticslab;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.log4j.Logger;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Map;

/**
 * Provides random access to files in S3 via byte ranged requests.
 * <p>
 * originally written by @jamesmcclain
 */
public class S3RandomAccessFile extends RandomAccessFile {
    private static final Logger LOGGER = Logger.getLogger(S3RandomAccessFile.class);

    public static final int DEFAULT_S3_BUFFER_SIZE = Constants.MEGABYTE * 2;
    public static final int DEFAULT_MAX_CACHE_SIZE = Constants.MEGABYTE * 55;

    private final AmazonS3URI uri;
    private final AmazonS3 s3Client;
    private final String bucket;
    private final String key;
    private final ObjectMetadata metadata;

    private int cacheBlockSize = -1;
    private int maxCacheBlocks = -1;

    private Map<String, byte[]> cache;
    private LinkedList<String> index;

    public S3RandomAccessFile(LinkedList<String> index, Map<String, byte[]> cache, AmazonS3 client, String url) throws IOException {
        this(index, cache, client, url, DEFAULT_S3_BUFFER_SIZE);
    }

    public S3RandomAccessFile(LinkedList<String> index, Map<String, byte[]> cache, AmazonS3 client, String url, int bufferSize) throws IOException {
        this(index, cache, client, url, bufferSize, DEFAULT_MAX_CACHE_SIZE);
    }

    public S3RandomAccessFile(LinkedList<String> index, Map<String, byte[]> cache, AmazonS3 client, String url, int bufferSize, int maxCacheSize) throws IOException {
        super(bufferSize);
        this.cache = cache;
        this.index = index;
        this.file = null;
        this.location = url;


        // Only enable cache if given size is at least twice the buffer size
        if (maxCacheSize >= 2 * bufferSize) {
            this.cacheBlockSize = 2 * bufferSize;
            this.maxCacheBlocks = maxCacheSize / this.cacheBlockSize;
        } else {
            this.cacheBlockSize = this.maxCacheBlocks = -1;
        }

        this.s3Client = client;
        this.uri = new AmazonS3URI(url);
        this.bucket = this.uri.getBucket();
        this.key = this.uri.getKey();
        this.metadata = client.getObjectMetadata(this.bucket, this.key); // does a head request on the data
    }

    public boolean exists() {
        if (this.bucket == null || this.key == null || this.s3Client == null) {
            return false;
        }

        LOGGER.debug(String.format("Check if file exists on S3: %s:%s", this.bucket, this.key));
        return this.s3Client.doesObjectExist(this.bucket, this.key);
    }

    public void close() throws IOException {
        this.cache.clear();
        this.index.clear();
    }

    /**
     * After execution of this function, the given block is guaranteed to
     * be in the cache.
     */
    private void ensure(long key) throws IOException {
        if (!this.cache.containsKey(this.getCacheKey(key))) {
            long position = key * this.cacheBlockSize;
            long toEOF = (this.length() - position);
            long bytes = toEOF < this.cacheBlockSize ? toEOF : this.cacheBlockSize;
            byte[] buffer = new byte[(int) bytes];

            this.read__(position, buffer, 0, this.cacheBlockSize);
            this.cache.put(this.getCacheKey(key), buffer);
            this.index.add(this.getCacheKey(key));
            assert (this.cache.size() == this.index.size());
            while (this.cache.size() > this.maxCacheBlocks) {
                String id = this.index.removeFirst();
                this.cache.remove(id);
            }
        }
    }

    private String getCacheKey(long oldKey) {
        return this.key + oldKey;
    }

    /**
     * Read directly from S3 [1], without going through the buffer.
     * All reading goes through here or readToByteChannel;
     * <p>
     * 1. https://docs.aws.amazon.com/AmazonS3/latest/dev/RetrievingObjectUsingJava.html
     *
     * @param pos    start here in the file
     * @param buff   put data into this buffer
     * @param offset buffer offset
     * @param len    this number of bytes
     * @return actual number of bytes read
     * @throws IOException on io error
     */
    @Override
    protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
        if (!(this.cacheBlockSize > 0) || !(this.maxCacheBlocks > 0)) {
            return this.read__(pos, buff, offset, len);
        }

        long start = pos / this.cacheBlockSize;
        long end = (pos + len - 1) / this.cacheBlockSize;

        if (pos >= this.length()) { // Do not read past end of the file
            return 0;
        } else if (end - start > 1) { // If the request touches more than two cache blocks, punt (should never happen)
            return this.read__(pos, buff, offset, len);
        } else if (end - start == 1) { // If the request touches two cache blocks, split it
            int length1 = (int) ((end * this.cacheBlockSize) - pos);
            int length2 = (int) ((pos + len) - (end * this.cacheBlockSize));
            return this.read_(pos, buff, offset, length1) + this.read_(pos + length1, buff, offset + length1, length2);
        }

        // Service a request that touches only one cache block
        long key = start;
        this.ensure(key);

        byte[] src = this.cache.get(this.getCacheKey(key));
        int srcPos = (int) (pos - (key * this.cacheBlockSize));
        int toEOB = src.length - srcPos;
        int length = toEOB < len ? toEOB : len;
        System.arraycopy(src, srcPos, buff, offset, length);

        return len;
    }

    private int read__(long pos, byte[] buff, int offset, int len) throws IOException {
        LOGGER.debug(String.format("Read chunk from %d, len %d from S3: %s:%s", pos, len, this.bucket, this.key));
        GetObjectRequest rangeObjectRequest = new GetObjectRequest(this.bucket, this.key);
        rangeObjectRequest.setRange(pos, pos + len - 1);

        S3Object objectPortion = this.s3Client.getObject(rangeObjectRequest);
        InputStream objectData = objectPortion.getObjectContent();
        int bytes = 0;
        int totalBytes = 0;

        bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
        while ((bytes > 0) && ((len - totalBytes) > 0)) {
            totalBytes += bytes;
            bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
        }

        objectData.close();
        objectPortion.close();

        return totalBytes;
    }

    @Override
    public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
        LOGGER.debug(String.format("Reading %d bytes from offset %d to byte channel from S3: %s:%s", nbytes, offset, this.bucket, this.key));

        int n = (int) nbytes;
        byte[] buff = new byte[n];
        int done = this.read_(offset, buff, 0, n);
        dest.write(ByteBuffer.wrap(buff));
        return done;
    }

    @Override
    public long length() throws IOException {
        return this.metadata.getContentLength();
    }

    @Override
    public long getLastModified() {
        return this.metadata.getLastModified().getTime();
    }
}
