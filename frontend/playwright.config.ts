import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 120000,
  use: {
    baseURL: 'http://127.0.0.1:8080',
    headless: true
  },
  webServer: {
    command: 'cd .. && ./gradlew :backend:quarkusDev',
    url: 'http://127.0.0.1:8080',
    timeout: 240000,
    reuseExistingServer: true
  }
});
