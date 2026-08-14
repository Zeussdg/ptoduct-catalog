import { ExternalCustomerAdapter } from "./ExternalCustomerAdapter.js";

// Yalnızca development/test amaçlıdır. Aşağıdaki veriler SAHTEDİR ve
// hiçbir gerçek sistemin yapısını temsil etmez (bkz. plan §6.8).
const MOCK_CUSTOMERS = [
  {
    externalId: "mock-001",
    source: "mock",
    email: "test@example.com",
    firstName: "Test",
    lastName: "User",
    companyName: "Test Company",
    phone: "+90 555 000 00 00",
    status: "active",
  },
];

export class MockExternalCustomerAdapter extends ExternalCustomerAdapter {
  async findByEmail(email) {
    return MOCK_CUSTOMERS.find((c) => c.email?.toLowerCase() === email.toLowerCase()) ?? null;
  }

  async findByExternalId(externalId) {
    return MOCK_CUSTOMERS.find((c) => c.externalId === externalId) ?? null;
  }
}
