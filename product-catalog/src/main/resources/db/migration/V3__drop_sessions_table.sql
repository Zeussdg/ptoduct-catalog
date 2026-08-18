-- Session.java entity kaldırıldı (Spring Security kendi oturum mekanizmasını kullanıyor,
-- bu tablo hiç kullanılmıyordu — eski Node/Prisma refresh-token oturumlarının kalıntısıydı).
DROP TABLE IF EXISTS sessions;
