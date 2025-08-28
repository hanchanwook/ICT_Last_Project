import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import path from "path"
import { defineConfig } from "vite"


// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  esbuild: {
    // drop: ["console", "debugger"], // console.log 제거
  },
  define: {
    global: "window",
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:19091',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
})