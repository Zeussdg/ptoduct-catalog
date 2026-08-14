package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PartnerView(
        String id,
        String name,
        String category,
        String description,
        String website,
        String logo,
        String accent
) {
    public String initials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
