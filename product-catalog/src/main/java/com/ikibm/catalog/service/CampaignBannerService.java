package com.ikibm.catalog.service;

import com.ikibm.catalog.config.PresentationData;
import com.ikibm.catalog.constants.CampaignIds;
import com.ikibm.catalog.dto.CampaignView;
import com.ikibm.catalog.entity.CampaignBanner;
import com.ikibm.catalog.entity.Category;
import com.ikibm.catalog.exception.NotFoundException;
import com.ikibm.catalog.repository.CampaignBannerRepository;
import com.ikibm.catalog.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;

    public CampaignBannerService(CampaignBannerRepository bannerRepository, PresentationData presentationData,
                                 StorageService storageService, CategoryRepository categoryRepository) {
        this.bannerRepository = bannerRepository;
        this.presentationData = presentationData;
        this.storageService = storageService;
        this.categoryRepository = categoryRepository;
    }

    /** Statik kampanya meta verisi + DB'den yüklenmiş banner görsel URL'leri; admin bir hedef kategori
     * seçtiyse tıklama linki (to) statik varsayılan yerine o kategoriye gider. */
    @Transactional(readOnly = true)
    public List<CampaignView> campaignsWithImages() {
        Map<String, CampaignBanner> banners = new HashMap<>();
        for (CampaignBanner b : bannerRepository.findAll()) {
            banners.put(b.getCampaignId(), b);
        }
        return presentationData.getCampaigns().stream()
                .map(c -> {
                    CampaignBanner b = banners.get(c.id());
                    String image = b != null ? b.getUrl() : null;
                    c = c.withImage(image);
                    if (b != null && b.getTargetCategory() != null) {
                        c = c.withLink(categoryLink(b.getTargetCategory()), b.getTargetCategory().getId());
                    }
                    return c;
                })
                .toList();
    }

    private String categoryLink(Category category) {
        Category parent = category.getParent();
        if (parent != null) {
            return "/?kategori=" + parent.getSlug() + "&altkategori=" + category.getSlug();
        }
        return "/?kategori=" + category.getSlug();
    }

    /** Bannerin tıklanınca gideceği hedef kategoriyi ayarlar — kategori seçilmemiş bir kampanyaya
     * link tanımlanamaz, önce görsel yüklenmelidir (CampaignBanner satırı görselle birlikte oluşur). */
    @Transactional
    public void setTargetCategory(String campaignId, Integer categoryId) {
        CampaignBanner b = bannerRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new NotFoundException("Önce bu kampanya için görsel yüklenmeli"));
        b.setTargetCategory(categoryId != null ? categoryRepository.getReferenceById(categoryId) : null);
        bannerRepository.save(b);
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
