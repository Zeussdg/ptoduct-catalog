import { env } from "./config/env.js";
import { app } from "./app.js";

app.listen(env.PORT, () => {
  console.log(`API sunucusu http://localhost:${env.PORT} adresinde çalışıyor (${env.NODE_ENV})`);
});
