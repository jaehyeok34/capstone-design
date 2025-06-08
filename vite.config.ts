import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/data': {
        target: 'http://localhost:1780',
        changeOrigin: true,
      },
      '/csv': {
        target: 'http://localhost:1780',
        changeOrigin: true,
      },
      '/event': {
        target: 'http://localhost:1780',
        changeOrigin: true,
      },
    },
  },
})
