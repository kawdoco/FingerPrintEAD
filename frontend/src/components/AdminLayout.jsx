import { NavLink, Outlet, useNavigate } from "react-router-dom";
import ThemeToggle from "./ThemeToggle.jsx";

const links = [
  { to: "/admin", label: "Dashboard", end: true },
  { to: "/admin/people", label: "People" },
  { to: "/admin/attendance", label: "Attendance" },
  { to: "/admin/devices", label: "Devices" },
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const username = localStorage.getItem("bci_admin_username") || "Admin";

  const logout = () => {
    localStorage.removeItem("bci_admin_token");
    localStorage.removeItem("bci_admin_username");
    navigate("/admin/login");
  };

  return (
    <div
      className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex"
      style={{ fontFamily: "'Space Grotesk', sans-serif" }}
    >
      <aside className="w-60 shrink-0 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 p-6 flex flex-col gap-8">
        <div>
          <p className="text-lg font-semibold tracking-tight">BCI Research Lab</p>
          <p className="text-slate-500 dark:text-slate-400 text-xs mt-1">Attendance admin</p>
        </div>
        <nav className="flex flex-col gap-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `px-3 py-2 rounded-md text-sm font-medium ${
                  isActive
                    ? "bg-teal-500/10 text-teal-600 dark:text-teal-400"
                    : "text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-800"
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto flex flex-col gap-3">
          <ThemeToggle />
          <a
            href="/"
            target="_blank"
            rel="noreferrer"
            className="block text-xs text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100"
          >
            Open live display ↗
          </a>
          <p className="text-slate-400 dark:text-slate-500 text-xs">Signed in as {username}</p>
          <button
            onClick={logout}
            className="w-full text-sm font-medium text-rose-600 dark:text-rose-400 hover:text-rose-500 border border-slate-200 dark:border-slate-800 rounded-md py-2"
          >
            Log out
          </button>
        </div>
      </aside>
      <main className="flex-1 p-8 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
