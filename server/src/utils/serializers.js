// Kullanıcı nesnelerini API'ye dönmeden önce hassas alanlardan arındırır.
// passwordHash hiçbir response'ta yer almamalı.
export function toSafeUser(user) {
  if (!user) return null;
  const { passwordHash: _passwordHash, ...safe } = user;
  return safe;
}
