package com.ikibm.catalog.dto;

/** MailService.send(...) çağrısının özeti — kalıcı bir kayıt DEĞİL, sadece o anki HTTP yanıtı için. */
public record MailSendResult(int sent, int failed, String message) {
}
