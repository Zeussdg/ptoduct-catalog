package com.ikibm.catalog.entity;

/** DB'de saklanmaz — CariAccountService tarafından paidAmount/amount/dueDate'ten türetilir. */
public enum CariDueStatus {
    BEKLIYOR, VADESI_GELDI, VADESI_GECTI, ODENDI, KISMEN_ODENDI
}
