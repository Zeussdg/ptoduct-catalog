import { Routes, Route } from "react-router-dom";
import Header from "./components/Header";
import Footer from "./components/Footer";
import CartDrawer from "./components/CartDrawer";
import ProductsPage from "./pages/ProductsPage";
import ProductDetailPage from "./pages/ProductDetailPage";
import PartnersPage from "./pages/PartnersPage";
import QuoteBuilderPage from "./pages/QuoteBuilderPage";

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<ProductsPage />} />
        <Route path="/urun/:id" element={<ProductDetailPage />} />
        <Route path="/teklif-sihirbazi" element={<QuoteBuilderPage />} />
        <Route path="/is-ortaklarimiz" element={<PartnersPage />} />
      </Routes>
      <Footer />
      <CartDrawer />
    </>
  );
}
