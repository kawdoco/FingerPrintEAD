import axios from "axios";

// Leave VITE_API_BASE_URL empty in development: requests go to the Vite dev server,
// which proxies /api and /ws to the Spring Boot backend on :8080 (no CORS problems).
// In production set it to the backend's public URL, e.g. https://attendance.example.lk
const base = import.meta.env.VITE_API_BASE_URL || "";

export const api = axios.create({
  baseURL: `${base}/api/v1`,
});

export function wsUrl() {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL;
  const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = base ? base.replace(/^https?:\/\//, "") : window.location.host;
  return `${proto}//${host}/ws/attendance`;
}

// Attach the admin JWT (set after /auth/login) to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("bci_admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Expired/invalid token on an admin page -> back to the login screen.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const onAdminPage = window.location.pathname.startsWith("/admin");
    if (error.response?.status === 401 && onAdminPage && window.location.pathname !== "/admin/login") {
      localStorage.removeItem("bci_admin_token");
      window.location.href = "/admin/login";
    }
    return Promise.reject(error);
  }
);

export function errorMessage(err, fallback = "Something went wrong") {
  return err?.response?.data?.message || fallback;
}

export default api;
