import { apiClient } from "./apiClient";

export const productsApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ""))
    ).toString();
    return apiClient.get(`/products${qs ? `?${qs}` : ""}`);
  },
  getById: (id) => apiClient.get(`/products/${id}`),
  create: (data) => apiClient.post("/products", data),
  update: (id, data) => apiClient.put(`/products/${id}`, data),
  remove: (id) => apiClient.delete(`/products/${id}`),
  uploadImage: (id, file, isPrimary) => {
    const form = new FormData();
    form.append("image", file);
    if (isPrimary) form.append("isPrimary", "true");
    return apiClient.post(`/products/${id}/images`, form, { isFormData: true });
  },
  updateImage: (id, imageId, data) => apiClient.put(`/products/${id}/images/${imageId}`, data),
  deleteImage: (id, imageId) => apiClient.delete(`/products/${id}/images/${imageId}`),
};
