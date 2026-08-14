import { apiClient } from "./apiClient";

export const campaignsApi = {
  listBanners: () => apiClient.get("/campaigns/banners"),
  uploadBanner: (campaignId, file) => {
    const form = new FormData();
    form.append("image", file);
    return apiClient.post(`/campaigns/banners/${campaignId}/image`, form, { isFormData: true });
  },
  removeBanner: (campaignId) => apiClient.delete(`/campaigns/banners/${campaignId}`),
};
