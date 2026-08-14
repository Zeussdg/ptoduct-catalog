import { apiClient } from "./apiClient";

function qs(params = {}) {
  const s = new URLSearchParams(
    Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ""))
  ).toString();
  return s ? `?${s}` : "";
}

export const adminApi = {
  quotes: {
    list: (params) => apiClient.get(`/admin/quotes${qs(params)}`),
    getById: (id) => apiClient.get(`/admin/quotes/${id}`),
    updateStatus: (id, status, adminNote) => apiClient.put(`/admin/quotes/${id}/status`, { status, adminNote }),
  },
  customers: {
    list: (params) => apiClient.get(`/admin/customers${qs(params)}`),
    getById: (id) => apiClient.get(`/admin/customers/${id}`),
  },
  users: {
    list: (params) => apiClient.get(`/admin/users${qs(params)}`),
    create: (data) => apiClient.post("/admin/users", data),
    updateRole: (id, role) => apiClient.put(`/admin/users/${id}/role`, { role }),
    updateStatus: (id, status) => apiClient.put(`/admin/users/${id}/status`, { status }),
    remove: (id) => apiClient.delete(`/admin/users/${id}`),
  },
  auditLogs: {
    list: (params) => apiClient.get(`/admin/audit-logs${qs(params)}`),
  },
  priceLists: {
    list: () => apiClient.get("/admin/price-lists"),
    create: (data) => apiClient.post("/admin/price-lists", data),
    setCustomerPrice: (data) => apiClient.post("/admin/customer-prices", data),
  },
};
