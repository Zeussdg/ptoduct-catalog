-- guest_company/guest_contact aslında her zaman "Teklifi Alan" (3. taraf müşteri) bilgisini
-- tutuyordu (bkz. V2 açıklaması), sadece girişsiz akış için doldurulan alanlardı. Giriş artık
-- zorunlu olduğundan bu alanlar kullanılmaz hale geldi; anlamını netleştirmek ve tüm tekliflerde
-- (girişli/girişsiz fark etmeksizin) doldurulabilmesini sağlamak için yeniden adlandırılıyor,
-- ayrıca form'da toplanan ama hiç saklanmayan telefon alanı ekleniyor.
ALTER TABLE quotes
  CHANGE COLUMN guest_company recipient_company VARCHAR(255) NULL,
  CHANGE COLUMN guest_contact recipient_contact VARCHAR(255) NULL,
  ADD COLUMN recipient_phone VARCHAR(64) NULL AFTER recipient_contact;
