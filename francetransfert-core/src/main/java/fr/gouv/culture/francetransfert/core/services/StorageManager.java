/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.StringUtils;

import fr.gouv.culture.francetransfert.core.exception.RetryStorageException;
import fr.gouv.culture.francetransfert.core.exception.StorageException;
import fr.gouv.culture.francetransfert.core.utils.AmazonS3Utils;
import jakarta.annotation.PostConstruct;

@Service
public class StorageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);

    private final static String ZIPPED_ENCLOSURE_NAME_PREFIX = "francetransfert-";

    @Value("${storage.access.key}")
    private String accessKey;

    @Value("${storage.env}")
    private String env;

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.secret.key}")
    private String secretKey;

    @Value("${bucket.sequestre}")
    private String sequestreBucket;

    private AmazonS3 conn;

    @PostConstruct
    private void setUpProps() throws StorageException {

        setEndpoint(endpoint);
        setAccessKey(accessKey);
        setSecretKey(secretKey);
        setStorageEnv(env);
        getConnection();
    }

    @SuppressWarnings("deprecation")
    // Function given by OSU documentation, Newer implementations buggy with OSU
    public AmazonS3 getConnection() throws StorageException {

        if (conn == null) {
            try {
                ClientConfiguration opts = new ClientConfiguration();
                opts.setSignerOverride("S3SignerType"); // NOT "AWS3SignerType"
                AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
                conn = new AmazonS3Client(credentials, opts);
                Region region = Region.getRegion(Regions.EU_WEST_2);
                conn.setRegion(region);
                conn.setEndpoint(getEndpoint());
            } catch (Exception e) {
                throw new StorageException(e);
            }
        }

        return conn;
    }

    public List<Bucket> listBuckets() throws StorageException {

        List<Bucket> buckets = null;

        try {
            buckets = conn.listBuckets();
            for (Bucket bucket : buckets) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Bucket : {} \t {}", bucket.getName(), StringUtils.fromDate(bucket.getCreationDate()));
                }
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return buckets;
    }

    public ArrayList<String> listBucketContent(String bucketName) throws StorageException {

        ArrayList<String> list = new ArrayList<String>();

        try {

            // DHO15464 24/04 correcting with same new pattern as in e.g.
            // getUploadedEnclosureFiles; see TODO there
            ObjectListing objects = conn.listObjects(bucketName);
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                list.add(AmazonS3Utils.unescapeProblemCharsForObjectKey(objectSummary.getKey()));
            }
            do {
                objects = conn.listNextBatchOfObjects(objects);
                for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                    list.add(AmazonS3Utils.unescapeProblemCharsForObjectKey(objectSummary.getKey()));
                }
            } while (objects.isTruncated());
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return list;
    }

    public void createFile(String bucketName, File fileToUpload, String objectKey) throws StorageException {

        try {
            ByteArrayInputStream input = new ByteArrayInputStream(Files.readAllBytes(fileToUpload.toPath()));
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            conn.putObject(bucketName, escapedObjectKey, input, new ObjectMetadata());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    public String getEtag(String bucketName, String objectKey) throws StorageException {

        ObjectMetadata obj = null;
        S3Object s3Object = null;
        String etag = null;

        try {
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, escapedObjectKey);
            s3Object = conn.getObject(getObjectRequest);
            s3Object.getObjectContent().abort();
            s3Object.getObjectContent().close();
            obj = s3Object.getObjectMetadata();
            etag = obj.getETag();
            s3Object.close();
        } catch (Exception e) {
            if (s3Object != null) {
                try {
                    s3Object.close();
                } catch (Exception e2) {
                    LOGGER.error("Error closing S3object", e);
                }
            }
            throw new StorageException(e);
        }

        return etag;
    }

    public void setFileACLPublic(String bucketName, String objectKey) {

        String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
        conn.setObjectAcl(bucketName, escapedObjectKey, CannedAccessControlList.PublicReadWrite);
    }

    public S3Object getObjectByName(String bucketName, String objectKey) throws StorageException {

        S3Object obj = null;

        try {
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            obj = conn.getObject(bucketName, escapedObjectKey);
            obj.setKey(AmazonS3Utils.unescapeProblemCharsForObjectKey(obj.getKey()));
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return obj;
    }

    public ArrayList<String> getUploadedEnclosureFiles(String bucketName, String prefix) throws StorageException {

        ArrayList<String> list = new ArrayList<String>();

        try {
            // In this project the prefix passed to this method is usually a GUID, so not
            // subject to problematic
            // characters, but we escape it for completeness since it is technically part of
            // the key of target objects.
            String escapedPrefix = AmazonS3Utils.escapeProblemCharsForObjectKey(prefix);

            // TODO: avoid duplicating core logic; rely on listNextBatchOfObjects return
            // empty and not null?
            ObjectListing objects = conn.listObjects(bucketName, escapedPrefix);
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                list.add(AmazonS3Utils.unescapeProblemCharsForObjectKey(objectSummary.getKey()));
            }
            do {
                objects = conn.listNextBatchOfObjects(objects);
                for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                    list.add(AmazonS3Utils.unescapeProblemCharsForObjectKey(objectSummary.getKey()));
                }
            } while (objects.isTruncated());
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return list;
    }

    public void deleteFilesWithPrefix(String bucketName, String prefix) throws RetryStorageException {

        try {
            // In this project the prefix passed to this method is usually a GUID, so not
            // subject to problematic
            // characters, but we escape it for completeness since it is technically part of
            // the key of target objects.
            String escapedPrefix = AmazonS3Utils.escapeProblemCharsForObjectKey(prefix);

            // N.B. not using AmazonS3Utils.{un}escapeProblemCharsForObjectKey() on the
            // object keys here since
            // they are being passed straight back to the S3 API for deletion.
            // TODO: avoid duplicating core logic; rely on listNextBatchOfObjects return
            // empty and not null?
            ObjectListing objects = conn.listObjects(bucketName, escapedPrefix);
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                conn.deleteObject(bucketName, objectSummary.getKey());
            }
            do {
                objects = conn.listNextBatchOfObjects(objects);
                for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                    conn.deleteObject(bucketName, objectSummary.getKey());
                }
            } while (objects.isTruncated());
        } catch (Exception e) {
            throw new RetryStorageException(e);
        }
    }

    public S3Object getObjectFromObjRequest(String bucketName, String objectKey) throws StorageException {

        S3Object obj = null;

        try {
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, escapedObjectKey);
            obj = conn.getObject(getObjectRequest);
            obj.setKey(AmazonS3Utils.unescapeProblemCharsForObjectKey(obj.getKey()));
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return obj;
    }

    public void deleteBucket(String bucketName) throws StorageException {

        try {
            conn.deleteBucket(bucketName);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    public void deleteObject(String bucketName, String objectKey) throws StorageException {
        try {
            conn.deleteObject(bucketName, AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey));
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    public URL generateDownloadURL(String bucketName, String objectKey, int expireInMinutes) throws StorageException {

        URL url = null;

        try {

            long ONE_MINUTE_IN_MILLIS = 60000; // millisecs
            long now = Calendar.getInstance().getTimeInMillis();
            Date afterAddingMins = new Date(now + (expireInMinutes * ONE_MINUTE_IN_MILLIS));

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
                    AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey)).withMethod(HttpMethod.GET)
                    .withExpiration(afterAddingMins);

            url = conn.generatePresignedUrl(generatePresignedUrlRequest);
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return url;
    }

    public Bucket createBucket(String bucketName) throws StorageException {

        Bucket bucket = null;

        try {
            bucket = conn.createBucket(bucketName);
        } catch (Exception e) {
            throw new StorageException(e);
        }

        return bucket;
    }

    public PartETag uploadMultiPartFileToOsuBucket(String bucketName, int partNumber, String objectKey,
            InputStream inputStream, long partSize, String uploadId) throws IOException, StorageException {

        PartETag partETag = null;

        try {

            UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName)
                    .withKey(AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey)).withUploadId(uploadId)
                    .withInputStream(inputStream).withPartNumber(partNumber).withPartSize(partSize);

            // Upload the part and add the response's ETag to our list.
            UploadPartResult uploadResult = conn.uploadPart(uploadRequest);
            partETag = uploadResult.getPartETag();
        } catch (SdkClientException e) {
            throw new StorageException(e);
        }

        return partETag;
    }

    public String generateUploadIdOsu(String bucketName, String objectKey) throws StorageException {
        InitiateMultipartUploadRequest initRequest = null;
        InitiateMultipartUploadResult initResponse = null;

        // Try creating bucket if continue if not working because bucket can be already
        // created
        try {
            if (!conn.listBuckets().stream().filter(bucket -> bucketName != null && bucketName.equals(bucket.getName()))
                    .findFirst().isPresent()) {
                conn.createBucket(bucketName);
            }
        } catch (Exception e) {
            LOGGER.info("generateUploadIdOsu Error while creating bucket : " + e.getMessage(), e);
        }

        try {
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            initRequest = new InitiateMultipartUploadRequest(bucketName, escapedObjectKey);
            initResponse = conn.initiateMultipartUpload(initRequest);
            return initResponse.getUploadId();
        } catch (Exception e) {
            LOGGER.error("Error while generate multipart ID : " + e.getMessage(), e);
            throw new StorageException(e);
        }
    }

    public void generateBucketSequestre(String bucketName) throws StorageException {
        // Try creating bucket if continue if not working because bucket can be already
        // created
        try {
            if (!conn.listBuckets().stream().filter(bucket -> bucketName != null && bucketName.equals(bucket.getName()))
                    .findFirst().isPresent()) {
                conn.createBucket(bucketName);
            }
        } catch (Exception e) {
            LOGGER.error("Error while creating bucket of sequestre: " + e.getMessage(), e);
        }
    }

    public String completeMultipartUpload(String bucketName, String objectKey, String uploadId,
            List<PartETag> partETags) throws StorageException {

        try {
            String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName,
                    escapedObjectKey, uploadId, partETags);
            conn.completeMultipartUpload(compRequest);
            return objectKey;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    public void uploadMultipartForZip(String bucketName, String objectKey, String localFilePath)
            throws RetryStorageException {
        File file = new File(localFilePath);
        long contentLength = file.length();
        long partSize = 10L * 1024L * 1024L; // Set part size to 10 MB.

        // In this project, this method is used to upload a Zip file whose name is
        // normalized and not subject to
        // problematic chars. However, we escape the key for completeness.
        String escapedObjectKey = AmazonS3Utils.escapeProblemCharsForObjectKey(objectKey);

        try {
            // Create a list of ETag objects. You retrieve ETags for each object part
            // uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<PartETag>();

            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName,
                    escapedObjectKey);
            InitiateMultipartUploadResult initResponse = conn.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as
                // needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName)
                        .withKey(escapedObjectKey).withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition).withFile(file).withPartSize(partSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = conn.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName,
                    escapedObjectKey, initResponse.getUploadId(), partETags);
            conn.completeMultipartUpload(compRequest);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but OSU couldn't process
            // it, so it returned an error response.
            // TODO: add log
            throw new RetryStorageException("The call was transmitted successfully, but OSU couldn't process", e);
        } catch (SdkClientException e) {
            // OSU couldn't be contacted for a response, or the client
            // couldn't parse the response from OSU.
            // TODO: add log
            throw new RetryStorageException("OSU couldn't be contacted for a response, or the client", e);
        }
    }

    public void moveOnSequestre(String nameBucketSource, String fileName) throws StorageException {
        try {
            conn.copyObject(nameBucketSource, fileName, sequestreBucket, fileName);
            conn.deleteObject(nameBucketSource, fileName);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    private void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    private void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    private void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getStorageEnv() {
        return env;
    }

    private void setStorageEnv(String env) {
        this.env = env;
    }

    public String getZippedEnclosureName(String prefix) {
        Checksum checksum = new CRC32();
        byte bytes[] = prefix.getBytes();
        checksum.update(bytes, 0, bytes.length);
        String enc = Long.toHexString(checksum.getValue());
        String zippedFileName = ZIPPED_ENCLOSURE_NAME_PREFIX + Long.parseLong(enc, 16);
        return zippedFileName + ".zip";
    }

    public boolean healthCheckQuery() {
        ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
        List<Bucket> bucketList = conn.listBuckets(listBucketsRequest);
        return !CollectionUtils.isEmpty(bucketList);
    }

}
