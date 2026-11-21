import assert from 'node:assert/strict';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { test } from 'node:test';
import { pathToFileURL } from 'node:url';
import { chromium } from 'playwright';
import { buildCsv } from '../src/validation.js';
import { availablePort, startProcess, stopProcess, waitForHealth } from '../scripts/test-helpers.mjs';

const BROWSERS = [
  process.env.KLI_BROWSER_PATH,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
].filter(Boolean);

function applicant(id, name, phone, amount) {
  const now = new Date();
  const follow = new Date(now);
  follow.setDate(follow.getDate() + 5);
  const iso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  return {
    id, name, phone, district: 'Bengaluru Urban', taluk: 'Bengaluru South', locality: 'Jayanagar', amount,
    purpose: 'Tailoring equipment', applicationDate: iso(now), status: 'New', officer: 'Lending Desk',
    remarks: 'Synthetic browser test', followUpDate: iso(follow),
  };
}

test('production interface works in Chrome or Edge across login, applicant flows, translation, CSV, and mobile layout', { timeout: 90000 }, async (context) => {
  const browserPath = BROWSERS.find((path) => path && existsSync(path));
  if (!browserPath) { context.skip('Chrome and Edge executables were not found.'); return; }
  const workDir = resolve(process.cwd(), 'work', 'browser-test');
  mkdirSync(workDir, { recursive: true });
  const dbPath = resolve(workDir, `ui-${process.pid}.sqlite`);
  const port = await availablePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const processHandle = await startProcess('scripts/start.mjs', { HOST: '127.0.0.1', PORT: String(port), KLI_DB_PATH: dbPath });
  let browser;
  try {
    await waitForHealth(baseUrl, processHandle, 25000);
    browser = await chromium.launch({ executablePath: browserPath, headless: true, args: ['--disable-gpu'] });
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, acceptDownloads: true });
    const pageErrors = [];
    const consoleErrors = [];
    await page.addInitScript(() => {
      window.__kliOutbound = [];
      window.open = (url) => { window.__kliOutbound.push(String(url)); return null; };
    });
    page.on('pageerror', (error) => pageErrors.push(error.message));
    page.on('console', (message) => { if (message.type() === 'error') consoleErrors.push(message.text()); });

    await page.goto(`${baseUrl}/#/login`, { waitUntil: 'networkidle' });
    assert.match(await page.title(), /Karnataka Loan Intelligence/);
    assert.equal(await page.locator('.art-scene').evaluate((element) => getComputedStyle(element).transformStyle), 'preserve-3d', 'login artwork should use layered CSS 3D');
    await page.screenshot({ path: resolve(workDir, 'login-screen.png'), fullPage: true });
    await page.getByLabel('Email or username').fill('wrong@example.com');
    await page.getByLabel('Password', { exact: true }).fill('not-the-demo-password');
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await page.getByText('Enter the demo username and password shown below.', { exact: true }).waitFor();
    await page.getByLabel('Email or username').fill('demo@kli.in');
    await page.getByLabel('Password', { exact: true }).fill('KarnatakaDemo26');
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await page.getByText('No applicants yet', { exact: true }).waitFor();
    assert.equal(await page.getByRole('button', { name: 'Dashboard' }).count(), 1);

    await page.getByRole('button', { name: 'Add applicant' }).first().click();
    let dialog = page.getByRole('dialog');
    assert.equal(await dialog.locator('select').first().locator('option').count(), 32, 'district selector should contain all 31 districts plus its prompt');
    const newApplicant = applicant('KLI-26-0001', 'Asha Rao', '+91 98765 43210', 250000);
    await dialog.getByLabel('Full name').fill(newApplicant.name);
    await dialog.getByLabel('Contact number').fill(newApplicant.phone);
    await dialog.getByLabel('District').selectOption(newApplicant.district);
    await dialog.getByLabel('Taluk').fill(newApplicant.taluk);
    await dialog.getByLabel('Village or city').fill(newApplicant.locality);
    await dialog.getByLabel('Loan amount requested').fill(String(newApplicant.amount));
    await dialog.getByLabel('Loan purpose').fill(newApplicant.purpose);
    await dialog.getByLabel('Application date').fill(newApplicant.applicationDate);
    await dialog.getByLabel('Assigned loan officer').fill(newApplicant.officer);
    await dialog.getByLabel('Remarks').fill(newApplicant.remarks);
    await dialog.getByLabel('Follow-up date').fill(newApplicant.followUpDate);
    await dialog.getByRole('button', { name: 'Save applicant' }).click();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();
    assert.match(await page.locator('.applicant-table').innerText(), /Bengaluru Urban/);
    assert.match(await page.locator('.applicant-table').innerText(), /₹2,50,000/);
    await page.screenshot({ path: resolve(workDir, 'desktop-dashboard.png'), fullPage: true });

    await page.getByRole('textbox', { name: 'Search applications' }).fill('Asha');
    await page.getByRole('button', { name: 'Filters' }).click();
    await page.getByLabel('District').selectOption('Bengaluru Urban');
    assert.equal(await page.getByRole('row', { name: /Asha Rao/ }).count(), 1);
    await page.getByRole('textbox', { name: 'Search applications' }).fill('No match');
    await page.getByText('No matching applications', { exact: true }).waitFor();
    await page.getByRole('button', { name: 'Clear filters' }).first().click();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();

    await page.locator('.applicant-identity').filter({ hasText: 'Asha Rao' }).click();
    dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'WhatsApp' }).click();
    const sentUrl = new URL(await page.evaluate(() => window.__kliOutbound.at(-1)));
    assert.equal(sentUrl.host, 'wa.me');
    assert.equal(sentUrl.pathname, '/919876543210');
    assert.match(sentUrl.searchParams.get('text'), /Asha Rao.*KLI-26-0001/);
    await dialog.locator('.status-update-row select').selectOption('Under Review');
    await dialog.getByRole('button', { name: 'Save changes' }).click();
    await page.getByRole('row', { name: /Under review/ }).waitFor();
    await page.getByRole('button', { name: 'Pending requests' }).click();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();

    await page.locator('.applicant-identity').filter({ hasText: 'Asha Rao' }).click();
    await page.locator('.status-update-row select').selectOption('Documents Pending');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await page.getByRole('button', { name: 'Documents pending' }).click();
    await page.getByRole('heading', { name: 'Documents pending' }).waitFor();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();
    await page.getByRole('button', { name: 'Pending requests' }).click();

    await page.locator('.applicant-identity').filter({ hasText: 'Asha Rao' }).click();
    await page.locator('.status-update-row select').selectOption('Approved');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await page.getByRole('button', { name: 'Approved applications' }).click();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();

    await page.locator('.applicant-identity').filter({ hasText: 'Asha Rao' }).click();
    await page.getByRole('button', { name: 'Edit applicant' }).click();
    dialog = page.getByRole('dialog');
    await dialog.getByLabel('Loan amount requested').fill('275000');
    await dialog.getByLabel('Remarks').fill('Property document follow-up');
    await dialog.getByRole('button', { name: 'Save changes' }).click();
    await page.getByText('₹2,75,000', { exact: false }).first().waitFor();
    await page.reload({ waitUntil: 'networkidle' });
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor();

    await page.getByRole('button', { name: 'Follow-ups' }).click();
    await page.getByText('Asha Rao', { exact: true }).waitFor();
    await page.getByRole('button', { name: 'Dashboard' }).click();
    const exportPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Export CSV' }).click();
    const download = await exportPromise;
    const exported = readFileSync(await download.path(), 'utf8');
    assert.match(exported, /KLI-26-0001/);
    assert.match(exported, /Asha Rao/);

    const second = applicant('KLI-26-9002', 'Nisha, Test', '98765 43211', 180000);
    const csv = `\uFEFF${buildCsv([second])}`;
    await page.getByRole('button', { name: 'Import CSV' }).click();
    dialog = page.getByRole('dialog');
    await dialog.locator('input[type="file"]').setInputFiles({ name: 'synthetic-applicants.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
    await dialog.getByText('1 requests ready to add').waitFor();
    await dialog.getByRole('button', { name: 'Import applicants' }).click();
    await page.getByRole('row', { name: /Nisha, Test/ }).waitFor();
    assert.equal(await page.getByRole('row').count(), 3);

    await page.getByRole('button', { name: 'ಕನ್ನಡ' }).click();
    await page.getByText('ಮತ್ತೆ ಭೇಟಿಯಾಗಿದ್ದು ಸಂತೋಷ', { exact: false }).waitFor();
    await page.getByRole('button', { name: 'ದಾಖಲೆಗಳು ಬಾಕಿ' }).waitFor();
    await page.getByRole('button', { name: 'ಅರ್ಜಿದಾರರು' }).click();
    await page.getByRole('heading', { name: 'ಅರ್ಜಿದಾರರು' }).waitFor();
    assert.equal(await page.getByRole('button', { name: 'ಡ್ಯಾಶ್‌ಬೋರ್ಡ್' }).count(), 1);
    await page.getByRole('button', { name: 'EN', exact: true }).click();

    await page.setViewportSize({ width: 390, height: 844 });
    await page.locator('.mobile-applicant-card').first().waitFor({ state: 'visible' });
    await page.waitForTimeout(260);
    assert.equal(await page.locator('.sidebar-open').count(), 0, 'mobile navigation should stay closed after a route change');
    assert.equal(await page.locator('.mobile-scrim').count(), 0, 'closed mobile navigation should not leave a page overlay');
    assert.equal(await page.locator('.applicant-table').isVisible(), false);
    await page.screenshot({ path: resolve(workDir, 'mobile-dashboard.png'), fullPage: true });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(`${baseUrl}/#/applications`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: 'Loan applications' }).waitFor();
    await page.reload({ waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: 'Loan applications' }).waitFor();
    await page.goto(`${baseUrl}/#/unknown-page`, { waitUntil: 'networkidle' });
    await page.getByRole('heading', { name: 'Page not found' }).waitFor();
    await page.getByRole('button', { name: 'Go to dashboard' }).click();

    await page.locator('.applicant-identity').filter({ hasText: 'Asha Rao' }).click();
    await page.getByRole('button', { name: 'Delete applicant' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Cancel' }).click();
    await page.getByRole('dialog').waitFor();
    await page.getByRole('button', { name: 'Delete applicant' }).click();
    dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Delete applicant' }).click();
    await page.getByRole('row', { name: /Asha Rao/ }).waitFor({ state: 'detached' });

    await page.getByRole('button', { name: 'Log out' }).click();
    await page.getByRole('heading', { name: 'Welcome back' }).waitFor();

    const standalone = resolve(process.cwd(), 'outputs', 'karnataka-loan-intelligence.html');
    assert.ok(existsSync(standalone), 'standalone HTML should be generated by the build');
    await page.goto(pathToFileURL(standalone).href, { waitUntil: 'load' });
    await page.getByLabel('Email or username').fill('demo@kli.in');
    await page.getByLabel('Password', { exact: true }).fill('KarnatakaDemo26');
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await page.getByText('No applicants yet', { exact: true }).waitFor();
    await page.getByRole('button', { name: 'Settings' }).click();
    await page.getByText('This browser (IndexedDB)', { exact: true }).waitFor();

    assert.deepEqual(pageErrors, [], `browser page errors: ${pageErrors.join('; ')}`);
    assert.deepEqual(consoleErrors, [], `browser console errors: ${consoleErrors.join('; ')}`);
  } finally {
    if (browser) await browser.close();
    await stopProcess(processHandle);
  }
});
