-- ExternalIdentity.java entity kaldırıldı. Sistemde dışarıdan bağımsız
-- self-servis kayıt/OAuth girişi olmayacak (tüm kullanıcılar admin panelinden
-- ekleniyor) — bu tablo hiç kullanılmıyordu, Session.java ile aynı gerekçeyle
-- kaldırılıyor.
DROP TABLE IF EXISTS external_identities;
