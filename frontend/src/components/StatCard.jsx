export default function StatCard({ label, value, tone = "teal" }) {
  const toneClasses = {
    teal: "text-teal-600 dark:text-teal-400",
    amber: "text-amber-600 dark:text-amber-400",
    rose: "text-rose-600 dark:text-rose-400",
    sky: "text-sky-600 dark:text-sky-400",
    slate: "text-slate-900 dark:text-slate-100",
  };

  return (
    <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-lg p-6">
      <p className="text-slate-500 dark:text-slate-400 text-sm mb-2">{label}</p>
      <p className={`text-3xl font-semibold tabular-nums ${toneClasses[tone]}`}>{value}</p>
    </div>
  );
}
