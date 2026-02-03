var _a;
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
var apiTarget = (_a = process.env.VITE_API_BASE_URL) !== null && _a !== void 0 ? _a : "http://localhost:8080";
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        strictPort: true,
        proxy: {
            "/api": {
                target: apiTarget,
                changeOrigin: true
            },
            "/ws-chat": {
                target: apiTarget,
                ws: true,
                changeOrigin: true
            }
        }
    }
});
