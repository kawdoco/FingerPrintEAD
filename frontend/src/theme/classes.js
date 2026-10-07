// Shared Tailwind class strings so every admin page stays visually consistent
// and theme-aware (light by default, dark via the .dark class on <html>).
export const card = "bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg";
export const input =
  "w-full bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-800 rounded-md px-3 py-2 text-sm " +
  "outline-none focus:border-teal-500 dark:focus:border-teal-400 text-slate-900 dark:text-slate-100 " +
  "placeholder:text-slate-400 dark:placeholder:text-slate-500";
export const select = input + " appearance-none";
export const btnPrimary =
  "bg-teal-500 hover:bg-teal-400 text-white dark:text-slate-950 font-medium rounded-md py-2 px-4 text-sm disabled:opacity-60";
export const btnSecondary =
  "border border-slate-300 dark:border-slate-800 text-slate-600 dark:text-slate-300 " +
  "hover:bg-slate-100 dark:hover:bg-slate-800 font-medium rounded-md py-2 px-4 text-sm disabled:opacity-60";
export const label = "block text-slate-500 dark:text-slate-400 text-xs mb-1";
export const pageTitle = "text-2xl font-semibold mb-1";
export const pageSub = "text-slate-500 dark:text-slate-400 text-sm mb-6";
export const tableHead = "text-left text-slate-500 dark:text-slate-500";
export const tableDivide = "divide-y divide-slate-200 dark:divide-slate-800";
export const errorText = "text-rose-600 dark:text-rose-400 text-sm";
export const successText = "text-teal-600 dark:text-teal-400 text-sm";

export const PERSON_TYPE_LABEL = {
  STUDENT: "Student",
  LECTURER: "Lecturer",
  REV_FATHER: "Rev. Father",
  GUEST: "Guest",
};

export const PERSON_TYPE_BADGE = {
  STUDENT: "bg-sky-500/10 text-sky-600 dark:text-sky-400",
  LECTURER: "bg-violet-500/10 text-violet-600 dark:text-violet-400",
  REV_FATHER: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  GUEST: "bg-slate-500/10 text-slate-600 dark:text-slate-400",
};
