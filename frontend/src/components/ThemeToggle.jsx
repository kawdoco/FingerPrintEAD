import { useTheme } from "../theme/ThemeProvider.jsx";

export default function ThemeToggle({ className = "" }) {
  const { theme, toggle } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      onClick={toggle}
      aria-pressed={isDark}
      className={`flex items-center gap-2 text-sm font-medium px-3 py-1.5 rounded-md border
        border-slate-300 dark:border-slate-800 text-slate-600 dark:text-slate-300
        hover:bg-slate-100 dark:hover:bg-slate-800 ${className}`}
    >
      <span>{isDark ? "🌙" : "☀️"}</span>
      {isDark ? "Dark" : "Light"}
    </button>
  );
}
