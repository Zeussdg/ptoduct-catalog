package com.ikibm.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeroSlideView(
        String slug,
        String eyebrow,
        String title,
        String desc,
        String icon,
        String to,
        String accent,
        String accent2
) {
}
