package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.CampaignBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignBannerRepository extends JpaRepository<CampaignBanner, Integer> {

    Optional<CampaignBanner> findByCampaignId(String campaignId);
}
