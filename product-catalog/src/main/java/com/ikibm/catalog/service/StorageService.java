package com.ikibm.catalog.service;

import com.ikibm.catalog.config.AppProperties;
import com.ikibm.catalog.exception.ConflictException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * S3 uyumlu görsel deposu (AWS SDK v2). Node s3Service anahtar şemasını yansıtır:
 *   products/{productId}/{uuid}-{ad}  ·  campaigns/{campaignId}/{uuid}-{ad}
 * S3 yapılandırılmamışsa (dev) yükleme açık bir hata ile reddedilir.
 */
@Service
public class StorageService {

    private final AppProperties.S3 props;
    private volatile S3Client client;

    public StorageService(AppProperties appProperties) {
        this.props = appProperties.getS3();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    private S3Client client() {
        if (!props.isConfigured()) {
            throw new ConflictException("S3 yapılandırılmamış — görsel yükleme devre dışı (S3_* ayarlarını verin).");
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    var b = S3Client.builder()
                            .region(Region.of(props.getRegion() == null || props.getRegion().isBlank() ? "us-east-1" : props.getRegion()))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey())))
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(props.isForcePathStyle()).build());
                    if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
                        b = b.endpointOverride(URI.create(props.getEndpoint()));
                    }
                    client = b.build();
                }
            }
        }
        return client;
    }

    public Uploaded uploadProductImage(Integer productId, byte[] bytes, String contentType, String originalName) {
        return put("products/" + productId + "/" + UUID.randomUUID() + "-" + sanitize(originalName), bytes, contentType);
    }

    public Uploaded uploadCampaignBanner(String campaignId, byte[] bytes, String contentType, String originalName) {
        return put("campaigns/" + sanitize(campaignId) + "/" + UUID.randomUUID() + "-" + sanitize(originalName), bytes, contentType);
    }

    private Uploaded put(String key, byte[] bytes, String contentType) {
        client().putObject(PutObjectRequest.builder()
                .bucket(props.getBucket()).key(key).contentType(contentType).build(), RequestBody.fromBytes(bytes));
        String base = props.getPublicUrlBase();
        String url = (base != null && !base.isBlank()) ? base + "/" + key : key;
        return new Uploaded(key, url);
    }

    public void deleteObject(String key) {
        client().deleteObject(DeleteObjectRequest.builder().bucket(props.getBucket()).key(key).build());
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record Uploaded(String key, String url) {}
}
