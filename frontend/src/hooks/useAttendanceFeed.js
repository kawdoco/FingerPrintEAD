import { useEffect, useRef, useState } from "react";
import api, { wsUrl } from "../api/client.js";

const POLL_INTERVAL_MS = 5000;
const EMPTY_SUMMARY = { inLabCount: 0, totalScansToday: 0, totalPeople: 0 };

/**
 * Live check-in feed + today's counts for the lab screen.
 * Primary: WebSocket (instant). Fallback: poll every 5 s, and keep retrying the socket.
 */
export function useAttendanceFeed(limit = 8) {
  const [feed, setFeed] = useState([]);
  const [summary, setSummary] = useState(EMPTY_SUMMARY);
  const [connected, setConnected] = useState(false);
  const pollRef = useRef(null);

  useEffect(() => {
    let socket = null;
    let retryTimer = null;
    let disposed = false;

    const fetchSnapshot = async () => {
      try {
        const [liveRes, summaryRes] = await Promise.all([
          api.get(`/attendance/live?limit=${limit}`),
          api.get("/attendance/today/summary"),
        ]);
        if (disposed) return;
        setFeed(liveRes.data.data);
        setSummary(summaryRes.data.data);
      } catch {
        // keep showing the last good data
      }
    };

    const startPolling = () => {
      if (!pollRef.current) pollRef.current = setInterval(fetchSnapshot, POLL_INTERVAL_MS);
    };
    const stopPolling = () => {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
    };

    const connect = () => {
      if (disposed) return;
      try {
        socket = new WebSocket(wsUrl());
      } catch {
        startPolling();
        retryTimer = setTimeout(connect, 10000);
        return;
      }
      socket.onopen = () => {
        setConnected(true);
        stopPolling();
        fetchSnapshot();
      };
      socket.onmessage = (event) => {
        try {
          const entry = JSON.parse(event.data);
          setFeed((prev) => [entry, ...prev.filter((p) => p.id !== entry.id)].slice(0, limit));
          api.get("/attendance/today/summary").then((res) => setSummary(res.data.data)).catch(() => {});
        } catch {
          // ignore malformed frames
        }
      };
      socket.onclose = () => {
        setConnected(false);
        startPolling();
        if (!disposed) retryTimer = setTimeout(connect, 10000);
      };
      socket.onerror = () => socket && socket.close();
    };

    fetchSnapshot();
    connect();

    return () => {
      disposed = true;
      if (retryTimer) clearTimeout(retryTimer);
      stopPolling();
      if (socket) {
        socket.onclose = null;
        socket.close();
      }
    };
  }, [limit]);

  return { feed, summary, connected };
}
