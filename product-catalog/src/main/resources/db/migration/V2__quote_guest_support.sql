-- Sepet drawer'ındaki anonim "Teklifi PDF'e geçir" akışının da quotes'a
-- kaydedilebilmesi için: giriş yapılmamışsa user_id boş kalabilir, müşteri
-- kimliği "Teklifi Alan" form alanlarından (firma/yetkili) metin olarak tutulur.

ALTER TABLE quotes
  MODIFY user_id INT NULL,
  ADD COLUMN guest_company VARCHAR(255) NULL AFTER user_id,
  ADD COLUMN guest_contact VARCHAR(255) NULL AFTER guest_company;
