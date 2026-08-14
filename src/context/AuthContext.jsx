import { createContext, useCallback, useEffect, useState } from "react";
import { authApi } from "../services/authApi";
import { ApiClientError } from "../services/apiClient";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshSession = useCallback(async () => {
    try {
      const { user: currentUser } = await authApi.me();
      setUser(currentUser);
    } catch (err) {
      if (!(err instanceof ApiClientError) || err.status !== 401) {
        console.error("Oturum kontrolü başarısız", err);
      }
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  const login = useCallback(async (identifier, password, rememberMe) => {
    const { user: loggedInUser } = await authApi.login(identifier, password, rememberMe);
    setUser(loggedInUser);
    return loggedInUser;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = { user, loading, isAuthenticated: Boolean(user), login, logout, refreshSession };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
