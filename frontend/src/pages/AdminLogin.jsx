import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/client.js";
import ThemeToggle from "../components/ThemeToggle.jsx";
import { card, input, btnPrimary, label, errorText } from "../theme/classes.js";

export default function AdminLogin() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await api.post("/auth/login", { username, password });
      const { token, username: loggedInAs } = res.data.data;
      localStorage.setItem("bci_admin_token", token);
      localStorage.setItem("bci_admin_username", loggedInAs);
      navigate("/admin");
    } catch (err) {
      setError(err.response ? errorMessage(err, "Login failed") : "Cannot reach the server - is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex items-center justify-center p-6"
      style={{ fontFamily: "'Space Grotesk', sans-serif" }}
    >
      <div className="absolute top-6 right-6">
        <ThemeToggle />
      </div>
      <form onSubmit={handleSubmit} className={`w-full max-w-sm ${card} p-8`}>
        <p className="text-xl font-semibold mb-1">BCI Research Lab</p>
        <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">Admin sign in</p>

        {error && (
          <p className={`${errorText} mb-4 bg-rose-500/10 border border-rose-500/30 rounded-md px-3 py-2`}>{error}</p>
        )}

        <label className={label}>Username</label>
        <input value={username} onChange={(e) => setUsername(e.target.value)} className={`${input} mb-4`} autoFocus required />

        <label className={label}>Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={`${input} mb-6`}
          required
        />

        <button type="submit" disabled={loading} className={`w-full ${btnPrimary}`}>
          {loading ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </div>
  );
}
