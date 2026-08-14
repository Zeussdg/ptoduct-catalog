import { Routes, Route } from "react-router-dom";
import PublicLayout from "./layouts/PublicLayout";
import AdminLayout from "./layouts/AdminLayout";
import ProtectedRoute from "./routes/ProtectedRoute";
import RoleBasedRoute from "./routes/RoleBasedRoute";

import ProductsPage from "./pages/ProductsPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import PartnersPage from "./pages/PartnersPage";
import QuoteBuilderPage from "./pages/QuoteBuilderPage";
import LoginPage from "./pages/LoginPage";
import AccountPage from "./pages/AccountPage";
import CartPage from "./pages/CartPage";
import QuotesListPage from "./pages/QuotesListPage";
import QuoteDetailPage from "./pages/QuoteDetailPage";

import DashboardPage from "./pages/admin/DashboardPage";
import ProductsAdminPage from "./pages/admin/ProductsAdminPage";
import ProductFormPage from "./pages/admin/ProductFormPage";
import CategoriesAdminPage from "./pages/admin/CategoriesAdminPage";
import CampaignBannersAdminPage from "./pages/admin/CampaignBannersAdminPage";
import PriceListsAdminPage from "./pages/admin/PriceListsAdminPage";
import QuotesAdminPage from "./pages/admin/QuotesAdminPage";
import QuoteDetailAdminPage from "./pages/admin/QuoteDetailAdminPage";
import CustomersAdminPage from "./pages/admin/CustomersAdminPage";
import CustomerDetailAdminPage from "./pages/admin/CustomerDetailAdminPage";
import UsersAdminPage from "./pages/admin/UsersAdminPage";
import AuditLogsPage from "./pages/admin/AuditLogsPage";

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<ProductsPage />} />
        <Route path="/urun/:id" element={<ProductDetailPage />} />
        <Route path="/teklif-sihirbazi" element={<QuoteBuilderPage />} />
        <Route path="/is-ortaklarimiz" element={<PartnersPage />} />
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/account" element={<AccountPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/quotes" element={<QuotesListPage />} />
          <Route path="/quotes/:id" element={<QuoteDetailPage />} />
        </Route>
      </Route>

      <Route
        path="/admin"
        element={
          <RoleBasedRoute roles={["ADMIN", "SUPER_ADMIN"]}>
            <AdminLayout />
          </RoleBasedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="products" element={<ProductsAdminPage />} />
        <Route path="products/new" element={<ProductFormPage />} />
        <Route path="products/:id/edit" element={<ProductFormPage />} />
        <Route path="categories" element={<CategoriesAdminPage />} />
        <Route path="campaign-banners" element={<CampaignBannersAdminPage />} />
        <Route path="price-lists" element={<PriceListsAdminPage />} />
        <Route path="quotes" element={<QuotesAdminPage />} />
        <Route path="quotes/:id" element={<QuoteDetailAdminPage />} />
        <Route path="customers" element={<CustomersAdminPage />} />
        <Route path="customers/:id" element={<CustomerDetailAdminPage />} />

        <Route element={<RoleBasedRoute roles={["SUPER_ADMIN"]} />}>
          <Route path="users" element={<UsersAdminPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
