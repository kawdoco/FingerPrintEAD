import { useEffect, useState } from "react";
import api, { errorMessage } from "../api/client.js";
import { card, input, btnPrimary, tableHead, tableDivide, errorText } from "../theme/classes.js";

export default function DevicesPage() {
  const [devices, setDevices] = useState([]);
  const [form, setForm] = useState({ deviceCode: "", location: "" });
  const [created, setCreated] = useState(null);
  const [error, setError] = useState("");

  const load = () => {
    api.get("/devices").then((res) => setDevices(res.data.data)).catch((err) => setError(errorMessage(err)));
  };
  useEffect(load, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setCreated(null);
    try {
      const res = await api.post("/devices", form);
      setCreated(res.data.data);
      setForm({ deviceCode: "", location: "" });
      load();
    } catch (err) {
      setError(errorMessage(err, "Could not register device"));
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-1">Devices</h1>
      <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">
        Each ESP32 fingerprint kiosk has its own device code and secret API key.
      </p>

      <div className="grid grid-cols-3 gap-6">
        <form onSubmit={handleSubmit} className={`${card} p-6 space-y-3 self-start`}>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-2">Register a kiosk</p>
          {error && <p className={`${errorText} text-xs`}>{error}</p>}
          <input
            value={form.deviceCode}
            onChange={(e) => setForm({ ...form, deviceCode: e.target.value })}
            placeholder="Device code (e.g. LAB204-R307S-02)"
            required
            className={input}
          />
          <input
            value={form.location}
            onChange={(e) => setForm({ ...form, location: e.target.value })}
            placeholder="Location"
            required
            className={input}
          />
          <button type="submit" className={`w-full ${btnPrimary}`}>
            Register device
          </button>

          {created && (
            <div className="mt-3 border border-amber-500/40 bg-amber-500/10 rounded-md p-3">
              <p className="text-amber-600 dark:text-amber-400 text-xs mb-2">
                Copy this API key into the firmware now - it is shown only once.
              </p>
              <p className="font-mono text-xs break-all text-slate-800 dark:text-slate-100 select-all">{created.apiKey}</p>
              <button
                type="button"
                onClick={() => navigator.clipboard?.writeText(created.apiKey)}
                className="mt-2 text-xs text-amber-600 dark:text-amber-400 hover:underline"
              >
                Copy
              </button>
            </div>
          )}
        </form>

        <div className={`col-span-2 ${card} p-6`}>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">Registered devices ({devices.length})</p>
          <table className="w-full text-sm">
            <thead>
              <tr className={tableHead}>
                <th className="pb-2 font-normal">Code</th>
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
        </div>
      </div>
    </div>
  );
}
