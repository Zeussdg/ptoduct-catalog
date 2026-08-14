import { apiClient } from "./apiClient";

export const userApi = {
  getMe: () => apiClient.get("/users/me"),
  updateMe: (data) => apiClient.put("/users/me", data),
  changePassword: (currentPassword, newPassword) =>
    apiClient.put("/users/me/password", { currentPassword, newPassword }),
};
