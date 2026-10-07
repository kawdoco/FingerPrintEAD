import { useEffect, useState } from "react";
import api, { errorMessage } from "../api/client.js";
import StatCard from "../components/StatCard.jsx";
import { card, tableHead, tableDivide, errorText } from "../theme/classes.js";

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [devices, setDevices] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = () =>
      Promise.all([api.get("/attendance/today/summary"), api.get("/devices")])
        .then(([summaryRes, devicesRes]) => {
          setSummary(summaryRes.data.data);
          setDevices(devicesRes.data.data);
        })
        .catch((err) => setError(errorMessage(err, "Could not load dashboard data")));
    load();
    const id = setInterval(load, 15000);
    return () => clearInterval(id);
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-1">Dashboard</h1>
      <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">Today's attendance at a glance</p>

      {error && <p className={`${errorText} mb-4`}>{error}</p>}

      {summary && (
        <div className="grid grid-cols-3 gap-4 mb-8">
          <StatCard label="In the lab now" value={summary.inLabCount} tone="teal" />
          <StatCard label="Total scans today" value={summary.totalScansToday} tone="sky" />
          <StatCard label="Registered people" value={summary.totalPeople} tone="slate" />
        </div>
      )}

      <div className={`${card} p-6`}>
        <p className="text-slate-500 dark:text-slate-400 text-sm mb-4">Fingerprint devices</p>
        {devices.length === 0 ? (
          <p className="text-slate-400 dark:text-slate-500 text-sm">No devices registered yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className={tableHead}>
                <th className="pb-2 font-normal">Device code</th>
                <th className="pb-2 font-normal">Location</th>
                <th className="pb-2 font-normal">Status</th>
                <th className="pb-2 font-normal">Last seen</th>
              </tr>
            </thead>
            <tbody className={tableDivide}>
              {devices.map((d) => (
                <tr key={d.id}>
                  <td className="py-2">{d.deviceCode}</td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">{d.location}</td>
                  <td className="py-2">
                    <span className={d.status === "ONLINE" ? "text-teal-600 dark:text-teal-400" : "text-rose-600 dark:text-rose-400"}>
                      {d.status}
                    </span>
                  </td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">
                    {d.lastSeenAt ? new Date(d.lastSeenAt).toLocaleString() : "Never"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
