import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";
import CartDrawer from "../components/CartDrawer";

export default function PublicLayout() {
  return (
    <>
      <Header />
      <Outlet />
      <Footer />
      <CartDrawer />
    </>
  );
}
