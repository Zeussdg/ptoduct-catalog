package com.ikibm.catalog.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Sayfalama numaraları (ilk, son, aktifin komşuları + "…"). React Pagination.jsx
 * pageList mantığını yansıtır. Thymeleaf'te @paginations.items(current,total).
 */
@Component("paginations")
public class Paginations {

    public List<String> items(int current, int total) {
        List<String> out = new ArrayList<>();
        if (total <= 7) {
            for (int i = 1; i <= total; i++) out.add(String.valueOf(i));
            return out;
        }
        TreeSet<Integer> pages = new TreeSet<>();
        for (int p : new int[]{1, total, current, current - 1, current + 1}) {
            if (p >= 1 && p <= total) pages.add(p);
        }
        int prev = 0;
        for (int p : pages) {
            if (p - prev > 1) out.add("…");
            out.add(String.valueOf(p));
            prev = p;
        }
        return out;
    }
}
