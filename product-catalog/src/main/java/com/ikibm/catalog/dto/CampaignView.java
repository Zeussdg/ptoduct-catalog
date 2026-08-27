package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignView(
        String id,
        String brand,
        String badge,
        String eyebrow,
        String title,
        String subtitle,
        String cta,
        String to,
        String icon,
        String accent,
        String accent2,
        String image,
        Integer targetCategoryId
) {
    public CampaignView withImage(String img) {
        return new CampaignView(id, brand, badge, eyebrow, title, subtitle, cta, to, icon, accent, accent2, img, targetCategoryId);
    }

    /** Admin panelinde seçilen hedef kategoriye göre gerçek yönlendirme linki ve seçili kategori id'si. */
    public CampaignView withLink(String newTo, Integer newTargetCategoryId) {
        return new CampaignView(id, brand, badge, eyebrow, title, subtitle, cta, newTo, icon, accent, accent2, image, newTargetCategoryId);
    }
}
