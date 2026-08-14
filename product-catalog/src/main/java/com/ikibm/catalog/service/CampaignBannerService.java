package com.ikibm.catalog.service;

import com.ikibm.catalog.config.PresentationData;
import com.ikibm.catalog.constants.CampaignIds;
import com.ikibm.catalog.dto.CampaignView;
import com.ikibm.catalog.entity.CampaignBanner;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CampaignBannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CampaignBannerService {

    private final CampaignBannerRepository bannerRepository;
    private final PresentationData presentationData;
    private final StorageService storageService;

    public CampaignBannerService(CampaignBannerRepository bannerRepository, PresentationData presentationData,
                                 StorageService storageService) {
        this.bannerRepository = bannerRepository;
        this.presentationData = presentationData;
        this.storageService = storageService;
    }

    /** Statik kampanya meta verisi + DB'den yüklenmiş banner görsel URL'leri. */
    @Transactional(readOnly = true)
    public List<CampaignView> campaignsWithImages() {
        Map<String, String> urls = new HashMap<>();
        for (CampaignBanner b : bannerRepository.findAll()) {
            urls.put(b.getCampaignId(), b.getUrl());
        }
        return presentationData.getCampaigns().stream()
                .map(c -> c.withImage(urls.get(c.id())))
                .toList();
    }

    @Transactional
    public void setBannerImage(String campaignId, byte[] bytes, String contentType, String originalName) {
        if (!CampaignIds.isValid(campaignId)) throw new NotFoundException("Kampanya bulunamadı");
        StorageService.Uploaded up = storageService.uploadCampaignBanner(campaignId, bytes, contentType, originalName);
        CampaignBanner existing = bannerRepository.findByCampaignId(campaignId).orElse(null);
        if (existing != null) {
            storageService.deleteObject(existing.getKey());
            existing.setKey(up.key());
            existing.setUrl(up.url());
            bannerRepository.save(existing);
        } else {
            CampaignBanner b = new CampaignBanner();
            b.setCampaignId(campaignId);
            b.setKey(up.key());
            b.setUrl(up.url());
            bannerRepository.save(b);
        }
    }

    @Transactional
    public void deleteBanner(String campaignId) {
        CampaignBanner b = bannerRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new NotFoundException("Kampanya görseli bulunamadı"));
        storageService.deleteObject(b.getKey());
        bannerRepository.delete(b);
    }
}
