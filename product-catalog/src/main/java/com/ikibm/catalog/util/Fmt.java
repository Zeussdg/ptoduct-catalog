package com.ikibm.catalog.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Tarih biçimleme yardımcısı. Thymeleaf'te @fmt.date(...) / @fmt.dateTime(...). */
@Component("fmt")
public class Fmt {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr", "TR"));
    private static final DateTimeFormatter DATETIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("tr", "TR"));

    public String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(ZONE));
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATETIME.format(instant.atZone(ZONE));
    }
}
