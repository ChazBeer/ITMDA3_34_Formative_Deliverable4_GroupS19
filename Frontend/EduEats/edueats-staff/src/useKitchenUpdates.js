// src/useKitchenUpdates.js
// Live updates for the staff board.
// The backend sends every new order, status change and cancellation to the
// kitchen channel /topic/orders/active (STOMP over a WebSocket at /ws-orders).
//
// onOrder(order) is called with the order the backend sent.
// Returns true while the live connection is up (false = only the backup
// refresh is running; it reconnects by itself).

import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { API_BASE_URL, getFreshToken } from "./api";

// ws://localhost:5174/ws-orders locally (Vite forwards it to the backend)
function getSocketUrl() {
  if (API_BASE_URL) {
    return `${API_BASE_URL.replace(/^http/, "ws")}/ws-orders`;
  }
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws-orders`;
}

function useKitchenUpdates(enabled, onOrder) {
  const [live, setLive] = useState(false);

  // Always call the newest onOrder without reconnecting
  const onOrderRef = useRef(onOrder);
  useEffect(() => {
    onOrderRef.current = onOrder;
  }, [onOrder]);

  useEffect(() => {
    if (!enabled) return undefined;

    let connectedBefore = false;

    const client = new Client({
      brokerURL: getSocketUrl(),
      reconnectDelay: 5000, // try again every 5 seconds if the connection drops
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {}, // keep the console quiet

      // The backend checks the login token when we connect.
      // Get a fresh one each time (it may have run out while connected).
      beforeConnect: async () => {
        const token = await getFreshToken();
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },

      onConnect: () => {
        setLive(true);
        client.subscribe("/topic/orders/active", (message) => {
          try {
            onOrderRef.current?.(JSON.parse(message.body));
          } catch {
            onOrderRef.current?.(null); // not JSON - just reload
          }
        });

        // Reconnected after a drop: something may have changed meanwhile
        if (connectedBefore) onOrderRef.current?.(null);
        connectedBefore = true;
      },

      onWebSocketClose: () => setLive(false),
      onStompError: () => setLive(false),
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [enabled]);

  return enabled && live;
}

export default useKitchenUpdates;
