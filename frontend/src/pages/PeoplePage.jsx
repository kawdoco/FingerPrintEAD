import { useEffect, useRef, useState } from "react";
import api, { errorMessage } from "../api/client.js";
import {
  card, input, select, btnPrimary, btnSecondary, label, errorText, successText,
  tableHead, tableDivide, PERSON_TYPE_LABEL, PERSON_TYPE_BADGE,
} from "../theme/classes.js";

const PERSON_TYPES = ["STUDENT", "LECTURER", "REV_FATHER", "GUEST"];

const emptyForm = {
  personType: "STUDENT",
  fullName: "",
  idNumber: "",
  email: "",
  phone: "",
  departmentName: "",
  note: "",
  fingerprintTemplateId: "",
};

const POLL_MS = 1500;

export default function PeoplePage() {
  const [people, setPeople] = useState([]);
  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [manualSlot, setManualSlot] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Scan session state: idle | starting | pending | waiting_finger | captured | failed | expired
  const [scan, setScan] = useState({ state: "idle", session: null });
  const pollRef = useRef(null);

  const loadPeople = () => {
    api.get("/people").then((res) => setPeople(res.data.data)).catch((err) => setError(errorMessage(err)));
  };
  const loadDevices = () => {
    api.get("/devices").then((res) => {
      setDevices(res.data.data);
      if (res.data.data.length > 0 && !selectedDevice) setSelectedDevice(res.data.data[0].deviceCode);
    });
  };
  const suggestSlot = () => {
    api
      .get("/people/next-template-id")
      .then((res) => setForm((f) => ({ ...f, fingerprintTemplateId: String(res.data.data.templateId) })))
      .catch(() => {});
  };

  useEffect(() => {
    loadPeople();
    loadDevices();
    return () => clearInterval(pollRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const stopPolling = () => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  };

  const startScan = async () => {
    setError("");
    setMessage("");
    setScan({ state: "starting", session: null });
    try {
      const res = await api.post("/enrollment/start", { deviceCode: selectedDevice || null });
      const session = res.data.data;
      setScan({ state: session.status.toLowerCase(), session });
      pollRef.current = setInterval(() => pollSession(session.id), POLL_MS);
    } catch (err) {
      setScan({ state: "idle", session: null });
      setError(errorMessage(err, "Could not start a scan"));
    }
  };

  const pollSession = async (id) => {
    try {
      const res = await api.get(`/enrollment/${id}`);
      const session = res.data.data;
      const state = session.status.toLowerCase();
      setScan({ state, session });
      if (["captured", "failed", "expired"].includes(state)) {
        stopPolling();
        if (state === "captured") {
          setForm((f) => ({ ...f, fingerprintTemplateId: String(session.templateId) }));
        }
      }
    } catch {
      // transient network hiccup - keep polling
    }
  };

  const cancelScan = () => {
    stopPolling();
    setScan({ state: "idle", session: null });
  };

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const resetForm = () => {
    setForm(emptyForm);
    setScan({ state: "idle", session: null });
    stopPolling();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    const payload = { ...form, fingerprintTemplateId: Number(form.fingerprintTemplateId) };
    try {
      if (scan.state === "captured" && scan.session) {
        await api.post(`/people/from-enrollment/${scan.session.id}`, payload);
      } else {
        await api.post("/people", payload);
      }
      setMessage(`Registered ${form.fullName}`);
      resetForm();
      loadPeople();
      if (!manualSlot) suggestSlot();
    } catch (err) {
      setError(errorMessage(err, "Could not register person"));
    }
  };

  useEffect(() => {
    if (manualSlot) suggestSlot();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [manualSlot]);

  const toggleActive = async (p) => {
    setError("");
    try {
      await api.put(`/people/${p.id}`, { ...p, active: !p.active });
      loadPeople();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async (p) => {
    if (!window.confirm(`Remove ${p.fullName}? People with attendance records can only be deactivated.`)) return;
    setError("");
    try {
      await api.delete(`/people/${p.id}`);
      loadPeople();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const scanReady = scan.state === "captured";
  const scanning = ["starting", "pending", "waiting_finger"].includes(scan.state);

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-1">People</h1>
      <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">
        Students, lecturers, Rev. Fathers and guests who can scan into the lab.
      </p>

      <div className="grid grid-cols-3 gap-6 items-start">
        <div className={`${card} p-6 space-y-4 col-span-1`}>
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-500 dark:text-slate-400">Enroll a fingerprint</p>
            <button
              type="button"
              onClick={() => { setManualSlot((v) => !v); cancelScan(); }}
              className="text-xs text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-200 underline"
            >
              {manualSlot ? "Use kiosk scan instead" : "Enter slot manually"}
            </button>
          </div>

          {!manualSlot && (
            <div className="space-y-3">
              {devices.length > 1 && (
                <div>
                  <label className={label}>Kiosk</label>
                  <select value={selectedDevice} onChange={(e) => setSelectedDevice(e.target.value)} className={select}>
                    {devices.map((d) => (
                      <option key={d.deviceCode} value={d.deviceCode}>
                        {d.deviceCode} — {d.location} ({d.status})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {scan.state === "idle" && (
                <button type="button" onClick={startScan} className={`w-full ${btnPrimary}`} disabled={devices.length === 0}>
                  {devices.length === 0 ? "No kiosk registered yet" : "Scan finger"}
                </button>
              )}

              {scanning && (
                <div className="rounded-md border border-teal-500/30 bg-teal-500/10 p-4 text-center">
                  <div className="mx-auto mb-2 h-3 w-3 rounded-full bg-teal-500 animate-pulse" />
                  <p className="text-sm font-medium text-teal-700 dark:text-teal-300">
                    {scan.state === "starting" ? "Starting…" : "Place a finger on the scanner"}
                  </p>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Slot {scan.session?.templateId} on {scan.session?.deviceCode}. Scan the same finger twice when prompted.
                  </p>
                  <button type="button" onClick={cancelScan} className="mt-3 text-xs text-slate-500 dark:text-slate-400 underline">
                    Cancel
                  </button>
                </div>
              )}

              {scanReady && (
                <div className="rounded-md border border-teal-500/40 bg-teal-500/10 p-3 text-sm text-teal-700 dark:text-teal-300">
                  Fingerprint captured in slot {scan.session?.templateId}. Fill in their details below.
                </div>
              )}

              {(scan.state === "failed" || scan.state === "expired") && (
                <div className="rounded-md border border-rose-500/40 bg-rose-500/10 p-3 text-sm text-rose-600 dark:text-rose-400">
                  {scan.state === "expired" ? "No finger was scanned in time." : scan.session?.message || "Scan failed."}
                  <button type="button" onClick={startScan} className="block mt-2 underline">
                    Try again
                  </button>
                </div>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-3 pt-2 border-t border-slate-200 dark:border-slate-800">
            {error && <p className={`${errorText} text-xs`}>{error}</p>}
            {message && <p className={`${successText} text-xs`}>{message}</p>}

            <div>
              <label className={label}>Person type</label>
              <select name="personType" value={form.personType} onChange={handleChange} className={select}>
                {PERSON_TYPES.map((t) => (
                  <option key={t} value={t}>{PERSON_TYPE_LABEL[t]}</option>
                ))}
              </select>
            </div>

            <input name="fullName" value={form.fullName} onChange={handleChange} placeholder="Full name" required className={input} />
            <input name="idNumber" value={form.idNumber} onChange={handleChange} placeholder="ID / student number (optional)" className={input} />
            <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="Email (optional)" className={input} />
            <input name="phone" value={form.phone} onChange={handleChange} placeholder="Phone (optional)" className={input} />
            <input name="departmentName" value={form.departmentName} onChange={handleChange} placeholder="Department (optional)" className={input} />
            <input name="note" value={form.note} onChange={handleChange} placeholder="Note, e.g. visiting organisation (optional)" className={input} />

            <div>
              <label className={label}>Fingerprint slot</label>
              <input
                name="fingerprintTemplateId"
                type="number"
                min={1}
                max={127}
                value={form.fingerprintTemplateId}
                onChange={handleChange}
                required
                readOnly={!manualSlot}
                className={`${input} ${!manualSlot ? "opacity-70" : ""}`}
                placeholder={manualSlot ? "1-127" : "Captured automatically"}
              />
            </div>

            <button
              type="submit"
              disabled={!manualSlot && !scanReady}
              className={`w-full ${btnPrimary}`}
            >
              Register person
            </button>
          </form>
        </div>

        <div className={`col-span-2 ${card} p-6`}>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-4">All people ({people.length})</p>
          <table className="w-full text-sm">
            <thead>
              <tr className={tableHead}>
                <th className="pb-2 font-normal">Name</th>
                <th className="pb-2 font-normal">Type</th>
                <th className="pb-2 font-normal">Department</th>
                <th className="pb-2 font-normal">Slot</th>
                <th className="pb-2 font-normal">Status</th>
                <th className="pb-2 font-normal" />
              </tr>
            </thead>
            <tbody className={tableDivide}>
              {people.map((p) => (
                <tr key={p.id}>
                  <td className="py-2">
                    {p.fullName}
                    {p.idNumber && <span className="text-slate-400 dark:text-slate-500 text-xs block">{p.idNumber}</span>}
                  </td>
                  <td className="py-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${PERSON_TYPE_BADGE[p.personType] || ""}`}>
                      {PERSON_TYPE_LABEL[p.personType] || p.personType}
                    </span>
                  </td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">{p.departmentName || "—"}</td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">{p.fingerprintTemplateId}</td>
                  <td className="py-2">
                    <span className={p.active ? "text-teal-600 dark:text-teal-400" : "text-slate-400 dark:text-slate-500"}>
                      {p.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="py-2 text-right whitespace-nowrap">
                    <button onClick={() => toggleActive(p)} className="text-xs text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 mr-3">
                      {p.active ? "Deactivate" : "Activate"}
                    </button>
                    <button onClick={() => remove(p)} className="text-xs text-rose-600 dark:text-rose-400 hover:underline">
                      Delete
                    </button>
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
