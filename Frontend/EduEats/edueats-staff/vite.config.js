import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
 
// Forwards backend calls from the frontend's own address to the
// Spring Boot backend on this laptop. This lets the app work on a phone
// on the same Wi-Fi (the phone can't reach "localhost:8080" itself).
const BACKEND_URL = "http://localhost:8080";
 
// The browser's Origin header is removed before forwarding, so the backend
// treats these as normal (non-CORS) requests from any address.
// proxyReqWs does the same for the live-updates WebSocket, so it also
// works on a phone or tablet (192.168.x.x), not only on localhost.
const removeOrigin = (proxy) => {
  proxy.on("proxyReq", (proxyReq) => {
    proxyReq.removeHeader("origin");
  });
  proxy.on("proxyReqWs", (proxyReq) => {
    proxyReq.removeHeader("origin");
  });
};
 
export default defineConfig({
  plugins: [react()],
  server: {
    // Makes the app reachable from other devices on the same Wi-Fi
    host: true,
    // Lets the app open through a Cloudflare tunnel address
    // (e.g. https://something.trycloudflare.com) for the demo
    allowedHosts: [".trycloudflare.com"],
    proxy: {
      "/api": {
        target: BACKEND_URL,
        changeOrigin: true,
        configure: removeOrigin,
      },
      // Live order updates (WebSocket)
      "/ws-orders": {
        target: BACKEND_URL,
        changeOrigin: true,
        ws: true,
        configure: removeOrigin,
      },
    },
  },
});