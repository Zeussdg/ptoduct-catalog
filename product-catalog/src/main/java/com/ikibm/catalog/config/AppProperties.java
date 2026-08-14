package com.ikibm.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Seed seed = new Seed();
    private S3 s3 = new S3();

    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }
    public S3 getS3() { return s3; }
    public void setS3(S3 s3) { this.s3 = s3; }

    public static class Seed {
        private String superAdminEmail = "superadmin@2mbilisim.local";
        private String superAdminPassword = "ChangeMe123!";
        public String getSuperAdminEmail() { return superAdminEmail; }
        public void setSuperAdminEmail(String v) { this.superAdminEmail = v; }
        public String getSuperAdminPassword() { return superAdminPassword; }
        public void setSuperAdminPassword(String v) { this.superAdminPassword = v; }
    }

    public static class S3 {
        private String endpoint = "";
        private String region = "auto";
        private String bucket = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private String publicUrlBase = "";
        private boolean forcePathStyle = true;
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String v) { this.endpoint = v; }
        public String getRegion() { return region; }
        public void setRegion(String v) { this.region = v; }
        public String getBucket() { return bucket; }
        public void setBucket(String v) { this.bucket = v; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String v) { this.accessKeyId = v; }
        public String getSecretAccessKey() { return secretAccessKey; }
        public void setSecretAccessKey(String v) { this.secretAccessKey = v; }
        public String getPublicUrlBase() { return publicUrlBase; }
        public void setPublicUrlBase(String v) { this.publicUrlBase = v; }
        public boolean isForcePathStyle() { return forcePathStyle; }
        public void setForcePathStyle(boolean v) { this.forcePathStyle = v; }
        public boolean isConfigured() {
            return bucket != null && !bucket.isBlank()
                    && accessKeyId != null && !accessKeyId.isBlank()
                    && secretAccessKey != null && !secretAccessKey.isBlank();
        }
    }
}
