import { apiClient } from "./apiClient";

export const categoriesApi = {
  list: () => apiClient.get("/categories"),
  create: (data) => apiClient.post("/categories", data),
  update: (id, data) => apiClient.put(`/categories/${id}`, data),
  remove: (id) => apiClient.delete(`/categories/${id}`),
};
