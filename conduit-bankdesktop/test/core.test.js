import assert from 'node:assert/strict';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { test } from 'node:test';
import { buildCallUrl, buildCsv, buildWhatsAppUrl, normalizeIndianMobile, parseCsv, validateApplicant, validateImport } from '../src/validation.js';
import { CSV_FIELDS } from '../src/data.js';
import { availablePort, startProcess, stopProcess, waitForHealth } from '../scripts/test-helpers.mjs';

function sample(id = 'KLI-26-9001') {
  return {
    id,
    name: 'Asha Rao',
    phone: '+91 98765 43210',
    district: 'Bengaluru Urban',
    taluk: 'Bengaluru South',
    locality: 'Jayanagar',
    amount: 250000,
    purpose: 'Tailoring equipment',
    applicationDate: '2026-11-20',
    status: 'New',
    officer: 'Lending Desk',
    remarks: 'Synthetic test record',
    followUpDate: '2026-12-01',
  };
}

test('Indian mobile validation and contact links use the assigned number', () => {
  assert.equal(normalizeIndianMobile('98765 43210'), '9876543210');
  assert.equal(normalizeIndianMobile('+91 (98765) 43210'), '9876543210');
  assert.equal(normalizeIndianMobile('09876543210'), '9876543210');
  assert.equal(normalizeIndianMobile('+1 415 555 0132'), null);
  assert.equal(normalizeIndianMobile('1234567890'), null);
  assert.equal(buildCallUrl(sample().phone), 'tel:+919876543210');
  const whatsApp = new URL(buildWhatsAppUrl(sample(), 'en'));
  assert.equal(whatsApp.host, 'wa.me');
  assert.equal(whatsApp.pathname, '/919876543210');
  assert.match(whatsApp.searchParams.get('text'), /Asha Rao.*KLI-26-9001/);
  assert.match(buildWhatsAppUrl(sample(), 'kn'), /text=/);
  assert.equal(buildCallUrl('+1 415 555 0132'), null);
});

test('CSV parsing preserves quoted commas and import validation catches bad and duplicate rows', () => {
  const value = sample();
  value.purpose = 'Equipment, sewing and repair';
  value.remarks = 'Asked for "two" quotes';
  const roundTrip = parseCsv(buildCsv([value]));
  assert.equal(roundTrip.length, 1);
  assert.equal(roundTrip[0].purpose, value.purpose);
  assert.equal(roundTrip[0].remarks, value.remarks);
  assert.equal(parseCsv(buildCsv([])).length, 0);
  assert.equal(validateImport([sample()], new Set()), null);
  assert.match(validateImport([sample(), sample()], new Set()), /duplicate applicant ID/);
  assert.match(validateImport([{ ...sample(), phone: '12345' }], new Set()), /Row 2: validationPhone/);
  assert.throws(() => parseCsv('id,name\nKLI-1,"Unclosed'), /unclosed quote/i);
  assert.throws(() => parseCsv('wrong,headers\na,b'), /CSV headers must/);
  assert.equal(CSV_FIELDS.length, 13);
  assert.ok(validateApplicant({ ...sample(), applicationDate: '2026-02-31' }).applicationDate);
});

test('production server supports applicant CRUD, validated CSV import, refresh routes, and SQLite persistence', { timeout: 30000 }, async () => {
  const testDataDir = resolve(process.cwd(), 'work', 'test-data');
  mkdirSync(testDataDir, { recursive: true });
  const dbPath = resolve(testDataDir, `api-${process.pid}.sqlite`);
  const port = await availablePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const start = async () => {
    const proc = await startProcess('server/index.mjs', { HOST: '127.0.0.1', PORT: String(port), KLI_DB_PATH: dbPath });
    await waitForHealth(baseUrl, proc);
    return proc;
  };
  let proc = await start();
  try {
    const root = await fetch(baseUrl);
    assert.equal(root.status, 200);
    assert.match(root.headers.get('content-type'), /text\/html/);
    assert.match(root.headers.get('content-security-policy'), /frame-ancestors 'none'/);
    const deepRoute = await fetch(`${baseUrl}/applications`);
    assert.equal(deepRoute.status, 200);
    assert.match(await deepRoute.text(), /Karnataka Loan Intelligence/);

    const createdResponse = await fetch(`${baseUrl}/api/applicants`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(sample()) });
    assert.equal(createdResponse.status, 201);
    const created = await createdResponse.json();
    assert.equal(created.phone, sample().phone);
    assert.ok(created.createdAt);
    const duplicate = await fetch(`${baseUrl}/api/applicants`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(sample()) });
    assert.equal(duplicate.status, 409);
    const invalid = await fetch(`${baseUrl}/api/applicants`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...sample('KLI-26-9002'), phone: '12345' }) });
    assert.equal(invalid.status, 422);

    const updated = await fetch(`${baseUrl}/api/applicants/${encodeURIComponent(created.id)}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...created, status: 'Under Review', amount: 275000, remarks: 'Updated in test' }) });
    assert.equal(updated.status, 200);
    assert.equal((await updated.json()).status, 'Under Review');
    const activity = await (await fetch(`${baseUrl}/api/activity`)).json();
    assert.ok(activity.some((item) => item.action === 'status' && item.applicantId === created.id));

    const importResponse = await fetch(`${baseUrl}/api/import`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ applicants: [sample('KLI-26-9003')] }) });
    assert.equal(importResponse.status, 201);
    assert.equal((await importResponse.json()).imported, 1);
    const duplicateImport = await fetch(`${baseUrl}/api/import`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ applicants: [sample(created.id)] }) });
    assert.equal(duplicateImport.status, 409);
    const rows = await (await fetch(`${baseUrl}/api/applicants`)).json();
    assert.equal(rows.length, 2);

    await stopProcess(proc);
    proc = await start();
    const afterRestart = await (await fetch(`${baseUrl}/api/applicants`)).json();
    assert.equal(afterRestart.length, 2);
    assert.ok(afterRestart.some((item) => item.id === created.id && item.status === 'Under Review'));

    const deleted = await fetch(`${baseUrl}/api/applicants/${encodeURIComponent(created.id)}`, { method: 'DELETE' });
    assert.equal(deleted.status, 200);
    assert.equal((await (await fetch(`${baseUrl}/api/applicants`)).json()).length, 1);
  } finally {
    await stopProcess(proc);
  }
});
