import { apiClient } from "./apiClient";

export const cartApi = {
  get: () => apiClient.get("/cart"),
  addItem: (productId, qty = 1) => apiClient.post("/cart/items", { productId, qty }),
  updateItem: (itemId, qty) => apiClient.put(`/cart/items/${itemId}`, { qty }),
  removeItem: (itemId) => apiClient.delete(`/cart/items/${itemId}`),
  clear: () => apiClient.delete("/cart"),
};
