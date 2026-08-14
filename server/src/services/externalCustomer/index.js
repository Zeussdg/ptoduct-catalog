import { env } from "../../config/env.js";
import { MockExternalCustomerAdapter } from "./MockExternalCustomerAdapter.js";

// Factory: authService (ve gelecekteki onboarding akışı) somut adapter
// sınıfını hiçbir zaman doğrudan bilmez — sadece bu factory'nin döndürdüğü
// ExternalCustomerAdapter arayüzünü kullanır. Gerçek bağlantı bilgisi
// (EXTERNAL_DB_*) geldiğinde, burada gerçek bir adapter implementasyonu
// (örn. `new MySqlExternalCustomerAdapter(config)`) eklenmesi yeterlidir —
// başka hiçbir modül değişmez. Bkz. README.md — discovery tamamlanmadan
// gerçek adapter yazılmayacaktır.
export function getExternalCustomerAdapter() {
  if (!env.EXTERNAL_DB_HOST) {
    return new MockExternalCustomerAdapter();
  }

  throw new Error(
    "EXTERNAL_DB_HOST tanımlı ama gerçek ExternalCustomerAdapter implementasyonu henüz yok. " +
      "Discovery aşaması (bkz. README.md) tamamlanmadan gerçek bağlantı kodu yazılmamıştır."
  );
}
