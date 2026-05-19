/// <reference types="vitest" />
import { defineConfig } from 'vitest/config';

// Vitest config — runs *.test.ts in pure Node (no browser, no Karma).
// Used for crypto/stream code that doesn't need the DOM or Angular DI
// (libsodium-wrappers, client-zip, ReadableStream, File and Blob are all
// available as Node 20+ globals). The existing Karma+Jasmine flow still owns
// *.spec.ts for component / browser-shaped tests; the two test runners coexist
// because they look at different filename patterns.
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
    // Stay out of Karma's territory, and skip `environment.test.ts` — that
    // file is an Angular env config (production-like settings used by
    // `ng test`), not a test suite, but the *.test.ts glob would otherwise
    // pull it in.
    exclude: ['node_modules/**', 'dist/**', 'src/**/*.spec.ts', 'src/environments/**'],
    globals: false,
    testTimeout: 20000,
  },
  esbuild: {
    target: 'es2022',
  },
});
