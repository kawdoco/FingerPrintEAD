import { useEffect, useState } from "react";
import api, { errorMessage } from "../api/client.js";
import { card, input, btnSecondary, tableHead, tableDivide, errorText, PERSON_TYPE_LABEL, PERSON_TYPE_BADGE } from "../theme/classes.js";

const today = () => {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

export default function AttendanceHistory() {
  const [date, setDate] = useState(today());
  const [records, setRecords] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    setError("");
    api
      .get(`/attendance?date=${date}`)
      .then((res) => setRecords(res.data.data))
      .catch((err) => setError(errorMessage(err, "Could not load attendance for that date")));
  }, [date]);

  const checkIns = records.filter((r) => r.checkType === "CHECK_IN").length;
  const checkOuts = records.filter((r) => r.checkType === "CHECK_OUT").length;

  const [exporting, setExporting] = useState(false);

  const exportToExcel = async () => {
    setExporting(true);
    try {
      const XLSX = await import("xlsx");
      const rows = records.map((r) => ({
        Name: r.personName,
        "ID Number": r.idNumber || "",
        Type: PERSON_TYPE_LABEL[r.personType] || r.personType,
        Department: r.department || "",
        Check: r.checkType === "CHECK_IN" ? "Check in" : "Check out",
        Time: new Date(r.scannedAt).toLocaleTimeString("en-GB"),
        "Scanned at": new Date(r.scannedAt).toLocaleString("en-GB"),
        Device: r.deviceCode || "",
      }));

      const sheet = XLSX.utils.json_to_sheet(rows);
      sheet["!cols"] = [
        { wch: 24 }, { wch: 14 }, { wch: 12 }, { wch: 18 }, { wch: 10 }, { wch: 10 }, { wch: 20 }, { wch: 18 },
      ];

      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, sheet, "Attendance");
      XLSX.writeFile(workbook, `attendance-${date}.xlsx`);
    } catch (err) {
      setError("Could not build the Excel file. Try again.");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-1">Attendance history</h1>
      <p className="text-slate-500 dark:text-slate-400 text-sm mb-6">Browse check-ins and check-outs for any date.</p>

      <div className="mb-4 flex items-center gap-4 flex-wrap">
        <label className="text-sm text-slate-500 dark:text-slate-400">Date</label>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className={`${input} w-auto`} />
        <span className="text-slate-500 dark:text-slate-400 text-sm">
          {checkIns} check-ins · {checkOuts} check-outs · {records.length} total
        </span>
        <button
          type="button"
          onClick={exportToExcel}
          disabled={records.length === 0 || exporting}
          className={`${btnSecondary} w-auto ml-auto`}
        >
          {exporting ? "Preparing…" : "Export to Excel"}
        </button>
      </div>

      {error && <p className={`${errorText} mb-4`}>{error}</p>}

      <div className={`${card} p-6`}>
        {records.length === 0 ? (
          <p className="text-slate-400 dark:text-slate-500 text-sm">No records for this date.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className={tableHead}>
                <th className="pb-2 font-normal">Name</th>
                <th className="pb-2 font-normal">Type</th>
                <th className="pb-2 font-normal">Department</th>
                <th className="pb-2 font-normal">Check</th>
                <th className="pb-2 font-normal">Time</th>
              </tr>
            </thead>
            <tbody className={tableDivide}>
              {records.map((r) => (
                <tr key={r.id}>
                  <td className="py-2">
                    {r.personName}
                    {r.idNumber && <span className="text-slate-400 dark:text-slate-500 text-xs block">{r.idNumber}</span>}
                  </td>
                  <td className="py-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${PERSON_TYPE_BADGE[r.personType] || ""}`}>
                      {PERSON_TYPE_LABEL[r.personType] || r.personType}
                    </span>
                  </td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">{r.department || "—"}</td>
                  <td className="py-2">
                    {r.checkType === "CHECK_IN" ? (
                      <span className="text-teal-600 dark:text-teal-400">Check in</span>
                    ) : (
                      <span className="text-slate-500 dark:text-slate-400">Check out</span>
                    )}
                  </td>
                  <td className="py-2 text-slate-500 dark:text-slate-400">{new Date(r.scannedAt).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
