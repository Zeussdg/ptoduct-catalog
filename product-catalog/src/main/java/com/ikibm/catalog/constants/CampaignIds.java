package com.ikibm.catalog.constants;

import java.util.List;

/** Vitrin kampanya slider'ındaki sabit kampanya id'leri (seed/campaigns.json ile eşleşir). */
public final class CampaignIds {

    private CampaignIds() {}

    public static final List<String> ALL = List.of(
            "ups-guardian",
            "hik-security",
            "tenda-network",
            "fiber-infra",
            "huawei-enterprise"
    );

    public static boolean isValid(String id) {
        return ALL.contains(id);
    }
}
