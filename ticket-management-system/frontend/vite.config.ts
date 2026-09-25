import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_API_PROXY_TARGET;

  if (command === 'serve' && !proxyTarget) {
    throw new Error(
      'VITE_API_PROXY_TARGET is not set. Copy frontend/.env.example to frontend/.env',
    );
  }

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: proxyTarget
        ? {
            '/api': {
              target: proxyTarget,
              changeOrigin: true,
            },
          }
        : undefined,
    },
  };
});
