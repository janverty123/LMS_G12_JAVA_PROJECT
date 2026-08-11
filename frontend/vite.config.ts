import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev-server proxy means the frontend can call same-origin "/api/..." paths;
// Vite forwards them to Spring Boot. This avoids relying on CORS during local
// development (CORS config in the backend still exists for non-proxied use,
// e.g. hitting the API directly from a browser tab or Postman).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
