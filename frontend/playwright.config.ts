import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 120000,
  projects: [
    { name: 'desktop', use: { viewport: { width: 1280, height: 800 } } },
    { name: 'mobile', use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true } }
  ],
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
