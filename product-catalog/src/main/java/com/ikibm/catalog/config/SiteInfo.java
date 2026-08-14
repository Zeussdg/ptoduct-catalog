package com.ikibm.catalog.config;

import org.springframework.stereotype.Component;

/**
 * Kurumsal site bilgileri (Footer + head title). React src/config/site.js karşılığı.
 * Boş bırakılan iletişim/sosyal alanlar arayüzde gösterilmez.
 */
@Component
public class SiteInfo {

    public String getBrandName() { return "2M Bilişim"; }
    public String getTagline() { return "Ürün Kataloğu"; }
    public String getLogo() { return "/logo.jpeg"; }
    public String getDescription() {
        return "Profesyonel teknoloji ürünleri, ağ çözümleri, güvenlik sistemleri ve "
                + "elektronik ürünler için güvenilir ürün kataloğu.";
    }

    public String getInstagram() { return ""; }
    public String getLinkedin() { return ""; }
    public String getFacebook() { return ""; }
    public String getYoutube() { return ""; }

    public String getAddress() { return ""; }
    public String getPhone() { return ""; }
    public String getEmail() { return ""; }
    public String getHours() { return ""; }
}
