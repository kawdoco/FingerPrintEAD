import { useEffect, useState } from "react";
import { useAttendanceFeed } from "../hooks/useAttendanceFeed.js";
import ThemeToggle from "../components/ThemeToggle.jsx";
import { PERSON_TYPE_LABEL, PERSON_TYPE_BADGE } from "../theme/classes.js";

function useClock() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);
  return now;
}

export default function LiveDisplay() {
  const now = useClock();
  const { feed, summary, connected } = useAttendanceFeed(8);

  const timeStr = now.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  const dateStr = now.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" });

  return (
    <div
      className="w-full min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 p-8 flex flex-col gap-6"
      style={{ fontFamily: "'Space Grotesk', sans-serif" }}
    >
      <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-5">
        <div>
          <p className="text-2xl font-semibold tracking-tight">BCI Research Lab</p>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Attendance display</p>
        </div>
        <div className="flex items-center gap-6">
          <ThemeToggle />
          <div className="text-right">
            <p className="text-4xl font-semibold tabular-nums" style={{ fontFamily: "'JetBrains Mono', monospace" }}>
              {timeStr}
            </p>
            <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">{dateStr}</p>
          </div>
        </div>
      </div>

      <div className="flex-1 grid grid-cols-3 gap-6">
        <div className="col-span-2 bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-6 flex flex-col">
          <p className="text-slate-500 dark:text-slate-400 text-sm mb-4">Live check-ins</p>
          {feed.length === 0 ? (
            <p className="text-slate-400 dark:text-slate-500 text-sm">No check-ins yet today.</p>
          ) : (
            <div className="flex flex-col divide-y divide-slate-200 dark:divide-slate-800">
              {feed.map((entry) => {
                const isOut = entry.checkType === "CHECK_OUT";
                const dot = isOut ? "bg-slate-400 dark:bg-slate-500" : "bg-teal-500 dark:bg-teal-400";
                const text = isOut ? "text-slate-500 dark:text-slate-400" : "text-teal-600 dark:text-teal-400";
                return (
                  <div key={entry.id} className="flex items-center justify-between py-3">
                    <div className="flex items-center gap-3">
                      <span className={`w-2 h-2 rounded-full ${dot}`} />
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="text-base font-medium">{entry.personName}</p>
                          <span className={`text-[10px] px-1.5 py-0.5 rounded-full ${PERSON_TYPE_BADGE[entry.personType] || ""}`}>
                            {PERSON_TYPE_LABEL[entry.personType] || entry.personType}
                          </span>
                        </div>
                        <p className="text-slate-500 dark:text-slate-400 text-xs mt-0.5">{entry.department || "—"}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <span
                        className="text-slate-500 dark:text-slate-300 text-sm tabular-nums"
                        style={{ fontFamily: "'JetBrains Mono', monospace" }}
                      >
                        {new Date(entry.scannedAt).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}
                      </span>
                      <span className={`text-sm font-medium w-20 text-right ${text}`}>
                        {isOut ? "Check out" : "Check in"}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-6">
          <div className="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-6 flex flex-col gap-4">
            <Row label="In the lab now" value={summary.inLabCount} color="text-teal-600 dark:text-teal-400" />
            <Row label="Total scans today" value={summary.totalScansToday} color="text-sky-600 dark:text-sky-400" />
            <Row label="Registered people" value={summary.totalPeople} color="text-slate-900 dark:text-slate-100" />
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-800 p-6">
            <p className="text-slate-500 dark:text-slate-400 text-sm mb-3">Connection</p>
            <div className="flex items-center gap-2">
              <span className={`w-2.5 h-2.5 rounded-full ${connected ? "bg-teal-500 dark:bg-teal-400" : "bg-amber-500 dark:bg-amber-400"}`} />
              <span className={`text-sm font-medium ${connected ? "text-teal-600 dark:text-teal-400" : "text-amber-600 dark:text-amber-400"}`}>
                {connected ? "Live" : "Reconnecting (polling)"}
              </span>
            </div>
            <p className="text-slate-400 dark:text-slate-500 text-xs mt-3">R307S · Lab Room 204</p>
          </div>
        </div>
      </div>
    </div>
  );
}

function Row({ label, value, color }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-slate-500 dark:text-slate-400 text-sm">{label}</span>
      <span className={`text-2xl font-semibold tabular-nums ${color}`}>{value}</span>
    </div>
  );
}
