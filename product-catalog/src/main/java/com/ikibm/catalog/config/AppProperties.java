package com.ikibm.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Seed seed = new Seed();
    private Upload upload = new Upload();

    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }

    public static class Seed {
        private String superAdminEmail = "superadmin@2mbilisim.local";
        private String superAdminPassword = "ChangeMe123!";
        public String getSuperAdminEmail() { return superAdminEmail; }
        public void setSuperAdminEmail(String v) { this.superAdminEmail = v; }
        public String getSuperAdminPassword() { return superAdminPassword; }
        public void setSuperAdminPassword(String v) { this.superAdminPassword = v; }
    }

    /** Yerel disk görsel deposu kök klasörü. */
    public static class Upload {
        private String dir = "uploads";
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
    }
}
