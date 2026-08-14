package com.ikibm.catalog.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Kategori sınıflandırma (scripts/categoryMap.js portu). Excel'deki ham kategori
 * metinleri güvenilmez olduğundan, sınıflandırma ham kategori + ürün adı üzerinde
 * anahtar kelime eşleşmesiyle yapılır; ilk eşleşen kural kazanır.
 */
@Component
public class CategoryClassifier {

    public record Result(String mainCategory, String mainCategorySlug,
                         String category, String categorySlug, boolean classified) {}

    private record Sub(String name, List<String> keywords) {}
    private record Rule(String main, List<Sub> subs) {}

    private static final String FALLBACK_MAIN = "Diğer Ürünler";
    private static final String FALLBACK_SUB = "Sınıflandırılmamış";

    private static final List<Rule> RULES = List.of(
        new Rule("Ağ & Network", List.of(
            new Sub("PoE Switchler", List.of("poe switch", "switch poe", "poe port switch")),
            new Sub("Endüstriyel Switchler", List.of("endustriyel switch", "industrial switch", "din rail switch")),
            new Sub("Switchler", List.of("switch")),
            new Sub("Access Point", List.of("access point", "erisim noktasi", "wifi ap", "eap")),
            new Sub("Menzil Genişletici", List.of("range extender", "menzil", "repeater", "extender")),
            new Sub("Point to Point Radyolink", List.of("point to point", "ptp", "radyolink", "cpe", "outdoor bridge")),
            new Sub("Router & Gateway", List.of("router", "gateway", "load balance")),
            new Sub("Powerline Adaptörler", List.of("powerline", "plc adapter", "av1000", "av2000")),
            new Sub("Wireless Adaptörler", List.of("usb adapter", "wireless adapter", "wifi adaptor", "pico", "nano adapter", "usb wifi")),
            new Sub("SFP Modüller", List.of("sfp", "gbic", "transceiver")),
            new Sub("PoE Enjektörler", List.of("poe injector", "poe enjektor", "injector", "enjektor")),
            new Sub("Modemler", List.of("modem", "vdsl", "adsl", "gpon", " ont ")),
            new Sub("Network Aksesuarları", List.of("anten", "antenna", "network"))
        )),
        new Rule("Güvenlik Sistemleri", List.of(
            new Sub("SpeedDome Kameralar", List.of("speed dome", "speeddome", "ptz")),
            new Sub("Dome Kameralar", List.of("dome")),
            new Sub("Bullet Kameralar", List.of("bullet")),
            new Sub("Kayıt Cihazları (DVR/NVR)", List.of("dvr", "nvr", "kayit cihaz", "recorder")),
            new Sub("Panel & Erişim Kontrol", List.of("access control", "erisim kontrol", "kartli gecis", "turnike", "interkom", "intercom", "panel")),
            new Sub("Alarm & Dedektör", List.of("alarm", "dedektor", "detector", "sensor", "pir", "siren")),
            new Sub("Ekranlar", List.of("monitor", "monitör", "ekran", "display", "led ekran")),
            new Sub("Güvenlik HDD", List.of("purple", "surveillance", "guvenlik hdd", "skyhawk", "harddisk", "hdd", "ssd")),
            new Sub("Diğer Kameralar", List.of("kamera", "camera", "ipc", "cctv", "fisheye")),
            new Sub("Kamera Aksesuarları", List.of("bracket", "aparat", "montaj", "kamera aksesuar"))
        )),
        new Rule("Fiber Optik", List.of(
            new Sub("Fiber Patch Cordlar", List.of("patch cord", "patchcord", "fiber patch cord")),
            new Sub("Fiber Patch Panel", List.of("fiber patch panel", "odf", "sonlandirma kutusu")),
            new Sub("SM & MM Pigtailler", List.of("pigtail")),
            new Sub("Fiber Adaptörler", List.of("fiber adaptor", "sc adapter", "lc adapter", "fc adapter", "st adapter", "kupler")),
            new Sub("Fiber Kablolar", List.of("fiber kablo", "fiber cable", "fiber optik kablo", "loose tube", "drop kablo")),
            new Sub("Fiber Aksesuar", List.of("fiber", "otdr", "makas", "cleaver"))
        )),
        new Rule("Kablolama", List.of(
            new Sub("Keystone & Patch Panel", List.of("keystone", "patch panel")),
            new Sub("Patch Kablolar", List.of("patch kablo", "patch cable", "patch cord cat", "utp patch")),
            new Sub("Data & Lan Kabloları", List.of("cat5", "cat6", "cat7", "cat 5", "cat 6", "utp kablo", "ftp kablo", "sftp", "lan kablo", "data kablo")),
            new Sub("Kablo Düzenleyiciler", List.of("kablo duzenle", "cable manager", "organizer", "kablo kanal", "spiral"))
        )),
        new Rule("Kabinet Sistemleri", List.of(
            new Sub("IP55 Outdoor Kabinler", List.of("ip55", "outdoor kabin", "dis ortam kabin", "duvar tipi outdoor")),
            new Sub("Raf Modülleri", List.of("raf modul", "sabit raf", "kizakli raf", "shelf")),
            new Sub("Fan Modülleri", List.of("fan modul", "fan unit", "fan")),
            new Sub("Prizler", List.of("pdu", "priz grup", "priz")),
            new Sub("Tekerlek & Ayak Grubu", List.of("tekerlek", "caster", "ayak grup", "ayak seti")),
            new Sub("Kabinetler", List.of("kabinet", "kabin", "rack cabinet", "server kabin")),
            new Sub("Kabin Aksesuarları", List.of("blank panel", "kabin aksesuar", "kor kapak"))
        )),
        new Rule("Güç & Enerji", List.of(
            new Sub("Online UPS", List.of("online ups", "online kesintisiz")),
            new Sub("Line Interactive UPS", List.of("line interactive", "ups", "kesintisiz guc")),
            new Sub("Aküler", List.of("aku", "akü", "battery", "batarya"))
        )),
        new Rule("Yazıcı & Sarf", List.of(
            new Sub("Çok Fonksiyonlu Lazer Yazıcılar", List.of("cok fonksiyon", "mfp", "multifunction", "tarayici yazici")),
            new Sub("Tanklı Yazıcılar", List.of("tank", "ecotank", "ink tank", "mürekkep tank")),
            new Sub("Mono Lazer Yazıcılar", List.of("lazer", "laser", "yazici", "printer", "toner", "kartus", "drum"))
        ))
    );

    public String slugify(String input) {
        String s = stripTr(input == null ? "" : input).toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s;
    }

    /** TR duyarlı fold: TR karakter sadeleştir + küçük harf + iç boşluk tekilleştir. */
    public String fold(String input, boolean trim) {
        String s = stripTr(input == null ? "" : input).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return trim ? s.trim() : s;
    }

    public String normalizeText(String input) {
        return fold(input, true);
    }

    public Result classify(String name, String rawCategory) {
        String joined = ((rawCategory == null ? "" : rawCategory) + " " + (name == null ? "" : name)).trim();
        String haystack = " " + fold(joined, true) + " ";
        for (Rule rule : RULES) {
            for (Sub sub : rule.subs()) {
                for (String kw : sub.keywords()) {
                    if (haystack.contains(fold(kw, false))) {
                        String mainSlug = slugify(rule.main());
                        return new Result(rule.main(), mainSlug, sub.name(),
                                mainSlug + "--" + slugify(sub.name()), true);
                    }
                }
            }
        }
        String mainSlug = slugify(FALLBACK_MAIN);
        return new Result(FALLBACK_MAIN, mainSlug, FALLBACK_SUB,
                mainSlug + "--" + slugify(FALLBACK_SUB), false);
    }

    private String stripTr(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'ç', 'Ç' -> sb.append('c');
                case 'ğ', 'Ğ' -> sb.append('g');
                case 'ı', 'I', 'İ' -> sb.append('i');
                case 'ö', 'Ö' -> sb.append('o');
                case 'ş', 'Ş' -> sb.append('s');
                case 'ü', 'Ü' -> sb.append('u');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
