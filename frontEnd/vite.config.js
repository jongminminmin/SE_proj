import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  build: {
    outDir: 'dist',      // 기본값이 'dist'
    base: './',
    assetsDir: 'assets', // 기본값이 'assets'
    emptyOutDir: true
  },


  plugins: [react()],
})
