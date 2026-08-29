import { expect, test } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const REGISTRY_INTERFACE_NS = 'http://www.ivoa.net/xml/RegistryInterface/v1.0';

test('imports VOResource.xml with preloaded RegistryInterface schema', async ({ page }) => {
  const currentDir = path.dirname(fileURLToPath(import.meta.url));
  const voResourcePath = path.resolve(currentDir, '../../backend/src/test/resources/VOResource.xml');

  await page.goto('/');

  const schemaSelect = page.getByLabel('Select a preloaded IVOA schema');
  await expect(schemaSelect).toBeVisible();

  const parseResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/schema/parse') &&
      response.request().method() === 'POST' &&
      response.status() === 200
  );
  await schemaSelect.selectOption(REGISTRY_INTERFACE_NS);
  await parseResponse;

  await expect(page.getByText('Load XML instance')).toBeVisible();

  const xmlUploadInput = page.locator('input[type="file"][accept=".xml,text/xml,application/xml"]');
  await xmlUploadInput.setInputFiles(voResourcePath);

  const importResponse = page.waitForResponse(
    (response) => response.url().includes('/api/instance/import') && response.request().method() === 'POST'
  );
  await page.getByRole('button', { name: 'Import XML into editor' }).click();

  await expect.poll(async () => (await importResponse).status()).toBe(200);
  await expect(page.getByText('Failed to import XML instance')).toHaveCount(0);
});
