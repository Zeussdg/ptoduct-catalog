import { apiClient } from "./apiClient";

export const authApi = {
  login: (identifier, password, rememberMe) =>
    apiClient.post("/auth/login", { identifier, password, rememberMe }),
  logout: () => apiClient.post("/auth/logout"),
  me: () => apiClient.get("/auth/me"),
};
