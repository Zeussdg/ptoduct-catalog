package com.ikibm.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.ikibm.catalog.dto.CampaignView;
import com.ikibm.catalog.dto.HeroSlideView;
import com.ikibm.catalog.dto.PartnerView;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;

/**
 * Statik sunum verisi (partnerler, hero slaytları, kampanya meta verisi).
 * resources/seed/*.json dosyalarından uygulama başlarken yüklenir.
 */
@Component
public class PresentationData {

    private final ObjectMapper mapper;
    private List<PartnerView> partners = List.of();
    private List<HeroSlideView> heroSlides = List.of();
    private List<CampaignView> campaigns = List.of();

    public PresentationData(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void load() {
        partners = read("seed/partners.json", PartnerView.class);
        heroSlides = read("seed/hero-slides.json", HeroSlideView.class);
        campaigns = read("seed/campaigns.json", CampaignView.class);
    }

    private <T> List<T> read(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            CollectionType ct = mapper.getTypeFactory().constructCollectionType(List.class, type);
            return mapper.readValue(in, ct);
        } catch (Exception e) {
            throw new IllegalStateException("Sunum verisi okunamadı: " + path, e);
        }
    }

    public List<PartnerView> getPartners() { return partners; }
    public List<HeroSlideView> getHeroSlides() { return heroSlides; }
    public List<CampaignView> getCampaigns() { return campaigns; }
}
