import { apiClient } from "./apiClient";

export const quotesApi = {
  create: (note) => apiClient.post("/quotes", { note }),
  listMine: () => apiClient.get("/quotes"),
  getById: (id) => apiClient.get(`/quotes/${id}`),
};
