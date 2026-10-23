// src/useOrderUpdates.js
// Live order updates: listens on the backend's WebSocket (STOMP) channel
// /topic/orders/{userId}. The backend sends a message there every time
// staff change one of this student's orders (Accepted, Preparing, Ready...).
//
// onUpdate(order) is called with the updated order each time
// (wrapped messages like ORDER_TIMEOUT are unwrapped first).
// Returns true while the live connection is up (false = using the
// backup check only, e.g. Wi-Fi dropped; it reconnects by itself).

import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { API_BASE_URL, getFreshToken } from "./api";

// ws://localhost:5173/ws-orders locally (Vite forwards it to the backend),
// or the hosted backend's address when VITE_API_URL is set
function getSocketUrl() {
  if (API_BASE_URL) {
    return `${API_BASE_URL.replace(/^http/, "ws")}/ws-orders`;
  }
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws-orders`;
}

// NEW: some messages come wrapped as { eventType, message, payload }
// (e.g. eventType "ORDER_TIMEOUT" when an order wasn't accepted in 15 minutes).
// Unwrap them so the rest of the app always gets a plain order with "status".
export function toOrderUpdate(data) {
  if (!data || typeof data !== "object") return null;
  if (!data.payload || !data.eventType) return data;

  const order = { ...data.payload };
  order.status = order.status || order.statusName || "";
  order.eventType = data.eventType;
  order.eventMessage = data.message || "";
  if (String(data.eventType).toUpperCase().includes("TIMEOUT")) {
    order.cancelReason = "timeout";
  }
  return order;
}

function useOrderUpdates(userId, onUpdate) {
  const [live, setLive] = useState(false);

  // Always call the newest onUpdate without reconnecting
  const onUpdateRef = useRef(onUpdate);
  useEffect(() => {
    onUpdateRef.current = onUpdate;
  }, [onUpdate]);

  useEffect(() => {
    if (!userId) return undefined;

    let connectedBefore = false;

    const client = new Client({
      brokerURL: getSocketUrl(),
      reconnectDelay: 5000, // try again every 5 seconds if the connection drops
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {}, // keep the console quiet

      // The backend checks the login token when we connect.
      // Get it fresh each time: it may have run out while we were connected
      // (it's refreshed first if so), e.g. when reconnecting after Wi-Fi drops.
      beforeConnect: async () => {
        const token = await getFreshToken();
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },

      onConnect: () => {
        setLive(true);
        client.subscribe(`/topic/orders/${userId}`, (message) => {
          let order = null;
          try {
            order = toOrderUpdate(JSON.parse(message.body));
          } catch {
            // not JSON - still tell the page something changed
          }
          onUpdateRef.current?.(order);
        });

        // Reconnected after a drop: something may have changed meanwhile
        if (connectedBefore) onUpdateRef.current?.(null);
        connectedBefore = true;
      },

      onWebSocketClose: () => setLive(false),
      onStompError: () => setLive(false),
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [userId]);

  return live;
}

export default useOrderUpdates;
