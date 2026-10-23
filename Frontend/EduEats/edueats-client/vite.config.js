import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// Forwards backend calls from the frontend's own address to the
// Spring Boot backend on this laptop. This lets the app work on a phone
// on the same Wi-Fi (the phone can't reach "localhost:8080" itself).
const BACKEND_URL = "http://localhost:8080";

// The browser's Origin header is removed before forwarding, so the backend
// treats these as normal (non-CORS) requests from any address.
// proxyReqWs does the same for the live-updates WebSocket, so it also
// works on a phone (192.168.x.x), not only on localhost.
const removeOrigin = (proxy) => {
  proxy.on("proxyReq", (proxyReq) => {
    proxyReq.removeHeader("origin");
  });
  proxy.on("proxyReqWs", (proxyReq) => {
    proxyReq.removeHeader("origin");
  });
};

export default defineConfig({
  plugins: [
    react(),

    // NEW: makes EduEats installable as an app ("Install app" / "Add to Home screen")
    VitePWA({
      registerType: "autoUpdate", // a new version is picked up automatically
      injectRegister: "auto",
      includeAssets: ["apple-touch-icon.png"],

      manifest: {
        name: "EduEats – Campus Cafeteria",
        short_name: "EduEats",
        description: "Pre-order your meals from the Eduvos Tygervalley cafeteria.",
        start_url: "/",
        scope: "/",
        display: "standalone",
        orientation: "portrait",
        background_color: "#f4f7fb",
        theme_color: "#173563",
        icons: [
          { src: "pwa-192x192.png", sizes: "192x192", type: "image/png" },
          { src: "pwa-512x512.png", sizes: "512x512", type: "image/png" },
          {
            src: "maskable-512x512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable",
          },
        ],
      },

      workbox: {
        // Only the app's own files are saved for offline use.
        // Backend calls (menu, orders, login) always go to the server.
        navigateFallbackDenylist: [/^\/api/, /^\/ws-orders/],
        cleanupOutdatedCaches: true,
        // Tapping the "Your order is ready!" notification opens My Orders
        importScripts: ["sw-notifications.js"],
      },
    }),
  ],

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
      // For live order updates later (WebSockets)
      "/ws-orders": {
        target: BACKEND_URL,
        changeOrigin: true,
        ws: true,
        configure: removeOrigin,
      },
    },
  },
});
