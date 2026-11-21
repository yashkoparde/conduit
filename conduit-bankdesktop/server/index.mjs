import { createServer } from 'node:http';
import { createReadStream, existsSync, mkdirSync, statSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { DatabaseSync } from 'node:sqlite';
import { extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const dist = join(root, 'dist');
const dataDir = join(root, 'data');
const dbFile = process.env.KLI_DB_PATH ? resolve(process.env.KLI_DB_PATH) : join(dataDir, 'loan-intelligence.sqlite');
const port = Number(process.env.PORT || 4173);
const host = process.env.HOST || '127.0.0.1';

if (!existsSync(join(dist, 'index.html'))) {
  console.error('Production files are missing. Run npm run build, or use npm start to build and launch.');
  process.exit(1);
}
mkdirSync(resolve(dbFile, '..'), { recursive: true });

const db = new DatabaseSync(dbFile);
db.exec(`
  PRAGMA journal_mode = WAL;
  PRAGMA foreign_keys = ON;
  CREATE TABLE IF NOT EXISTS applicants (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    district TEXT NOT NULL,
    taluk TEXT NOT NULL,
    locality TEXT NOT NULL DEFAULT '',
    amount INTEGER NOT NULL,
    purpose TEXT NOT NULL,
    applicationDate TEXT NOT NULL,
    status TEXT NOT NULL,
    officer TEXT NOT NULL,
    remarks TEXT NOT NULL DEFAULT '',
    followUpDate TEXT NOT NULL DEFAULT '',
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL
  );
  CREATE TABLE IF NOT EXISTS activity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    action TEXT NOT NULL,
    applicantId TEXT NOT NULL,
    applicantName TEXT NOT NULL,
    happenedAt TEXT NOT NULL
  );
`);

const STATUSES = new Set(['New', 'Under Review', 'Documents Pending', 'Approved', 'Rejected', 'Disbursed']);
const DISTRICTS = new Set([
  'Bagalkote', 'Ballari', 'Belagavi', 'Bengaluru Rural', 'Bengaluru Urban', 'Bidar', 'Chamarajanagar',
  'Chikkaballapura', 'Chikkamagaluru', 'Chitradurga', 'Dakshina Kannada', 'Davanagere', 'Dharwad', 'Gadag',
  'Hassan', 'Haveri', 'Kalaburagi', 'Kodagu', 'Kolar', 'Koppal', 'Mandya', 'Mysuru', 'Raichur', 'Ramanagara',
  'Shivamogga', 'Tumakuru', 'Udupi', 'Uttara Kannada', 'Vijayapura', 'Vijayanagara', 'Yadgir',
]);
const FIELDS = ['id', 'name', 'phone', 'district', 'taluk', 'locality', 'amount', 'purpose', 'applicationDate', 'status', 'officer', 'remarks', 'followUpDate'];

function sendJson(res, status, payload) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'no-referrer',
  });
  res.end(JSON.stringify(payload));
}

function validateApplicant(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) return 'Applicant must be a JSON object.';
  const values = Object.fromEntries(FIELDS.map((field) => [field, input[field] == null ? '' : String(input[field]).trim()]));
  const phoneCompact = values.phone.replace(/[\s()-]/g, '');
  const phoneDigits = phoneCompact.replace(/^\+91/, '').replace(/^0(?=\d{10}$)/, '');
  if (!values.id || values.id.length > 40) return 'Applicant ID is required and must be 40 characters or fewer.';
  if (!values.name || values.name.length > 100) return 'Applicant name is required and must be 100 characters or fewer.';
  if (!/^(?:\+91)?[6-9]\d{9}$/.test(phoneDigits) || (phoneCompact.startsWith('+') && !phoneCompact.startsWith('+91'))) return 'Enter a valid Indian mobile number.';
  if (!DISTRICTS.has(values.district)) return 'Choose one of the 31 Karnataka districts.';
  if (!values.taluk || values.taluk.length > 80) return 'Taluk is required and must be 80 characters or fewer.';
  if (values.locality.length > 100) return 'Village or city must be 100 characters or fewer.';
  if (!/^\d+$/.test(String(input.amount ?? '')) || Number(input.amount) < 1 || Number(input.amount) > 1000000000) return 'Loan amount must be between ₹1 and ₹1,00,00,00,000.';
  if (!values.purpose || values.purpose.length > 120) return 'Loan purpose is required and must be 120 characters or fewer.';
  if (!isIsoDate(values.applicationDate)) return 'Enter a valid application date.';
  if (!STATUSES.has(values.status)) return 'Choose a valid application status.';
  if (!values.officer || values.officer.length > 100) return 'Assigned loan officer is required.';
  if (values.remarks.length > 1000) return 'Remarks must be 1,000 characters or fewer.';
  if (values.followUpDate && !isIsoDate(values.followUpDate)) return 'Enter a valid follow-up date.';
  return { ...values, amount: Number(input.amount) };
}

function isIsoDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

function rows(sql, ...params) { return db.prepare(sql).all(...params); }
function row(sql, ...params) { return db.prepare(sql).get(...params); }
function logActivity(action, applicant) {
  db.prepare('INSERT INTO activity (action, applicantId, applicantName, happenedAt) VALUES (?, ?, ?, ?)')
    .run(action, applicant.id, applicant.name, new Date().toISOString());
}
function insertApplicant(value) {
  const now = new Date().toISOString();
  const applicant = { ...value, createdAt: now, updatedAt: now };
  db.prepare(`INSERT INTO applicants (${FIELDS.join(',')},createdAt,updatedAt) VALUES (${FIELDS.map(() => '?').join(',')},?,?)`)
    .run(...FIELDS.map((field) => applicant[field]), now, now);
  logActivity('created', applicant);
  return applicant;
}

async function readBody(req, limit = 2_000_000) {
  let raw = '';
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > limit) throw Object.assign(new Error('Request is too large.'), { status: 413 });
  }
  try { return JSON.parse(raw || '{}'); }
  catch { throw Object.assign(new Error('Request must contain valid JSON.'), { status: 400 }); }
}

async function api(req, res, url) {
  const pathname = url.pathname;
  if (req.method === 'GET' && pathname === '/api/health') return sendJson(res, 200, { ok: true, storage: 'sqlite' });
  if (req.method === 'GET' && pathname === '/api/applicants') {
    return sendJson(res, 200, rows('SELECT * FROM applicants ORDER BY updatedAt DESC'));
  }
  if (req.method === 'GET' && pathname === '/api/activity') {
    return sendJson(res, 200, rows('SELECT * FROM activity ORDER BY id DESC LIMIT 30'));
  }
  if (req.method === 'POST' && pathname === '/api/applicants') {
    const value = validateApplicant(await readBody(req));
    if (typeof value === 'string') return sendJson(res, 422, { error: value });
    if (row('SELECT id FROM applicants WHERE id = ?', value.id)) return sendJson(res, 409, { error: 'Applicant ID already exists.' });
    try { return sendJson(res, 201, insertApplicant(value)); }
    catch (error) { if (error.code === 'ERR_SQLITE_CONSTRAINT_PRIMARYKEY') return sendJson(res, 409, { error: 'Applicant ID already exists.' }); throw error; }
  }
  if (req.method === 'POST' && pathname === '/api/import') {
    const payload = await readBody(req);
    if (!Array.isArray(payload.applicants) || payload.applicants.length > 5000) return sendJson(res, 422, { error: 'Import must contain at most 5,000 applicants.' });
    const clean = [];
    const seen = new Set();
    for (let i = 0; i < payload.applicants.length; i += 1) {
      const value = validateApplicant(payload.applicants[i]);
      if (typeof value === 'string') return sendJson(res, 422, { error: `Row ${i + 2}: ${value}` });
      if (seen.has(value.id) || row('SELECT id FROM applicants WHERE id = ?', value.id)) return sendJson(res, 409, { error: `Row ${i + 2}: duplicate applicant ID ${value.id}.` });
      seen.add(value.id);
      clean.push(value);
    }
    db.exec('BEGIN IMMEDIATE');
    try {
      const imported = clean.map(insertApplicant);
      db.exec('COMMIT');
      return sendJson(res, 201, { imported: imported.length });
    } catch (error) {
      db.exec('ROLLBACK');
      throw error;
    }
  }
  const match = pathname.match(/^\/api\/applicants\/([^/]+)$/);
  if (match) {
    const id = decodeURIComponent(match[1]);
    const existing = row('SELECT * FROM applicants WHERE id = ?', id);
    if (!existing) return sendJson(res, 404, { error: 'Applicant not found.' });
    if (req.method === 'PUT') {
      const value = validateApplicant({ ...(await readBody(req)), id });
      if (typeof value === 'string') return sendJson(res, 422, { error: value });
      const now = new Date().toISOString();
      const fields = FIELDS.filter((field) => field !== 'id');
      db.prepare(`UPDATE applicants SET ${fields.map((field) => `${field} = ?`).join(',')}, updatedAt = ? WHERE id = ?`)
        .run(...fields.map((field) => value[field]), now, id);
      const updated = row('SELECT * FROM applicants WHERE id = ?', id);
      logActivity(existing.status === updated.status ? 'updated' : 'status', updated);
      return sendJson(res, 200, updated);
    }
    if (req.method === 'DELETE') {
      db.prepare('DELETE FROM applicants WHERE id = ?').run(id);
      logActivity('deleted', existing);
      return sendJson(res, 200, { deleted: true });
    }
  }
  return sendJson(res, 404, { error: 'API route not found.' });
}

const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon', '.json': 'application/json; charset=utf-8', '.woff2': 'font/woff2' };
const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url || '/', `http://${req.headers.host || `${host}:${port}`}`);
    if (url.pathname.startsWith('/api/')) return await api(req, res, url);
    const requested = url.pathname === '/' ? join(dist, 'index.html') : resolve(dist, `.${decodeURIComponent(url.pathname)}`);
    if (!requested.startsWith(`${resolve(dist)}${sep}`) && requested !== join(dist, 'index.html')) return sendJson(res, 400, { error: 'Invalid path.' });
    const file = existsSync(requested) && statSync(requested).isFile() ? requested : join(dist, 'index.html');
    if (!existsSync(file)) return sendJson(res, 404, { error: 'Not found.' });
    res.writeHead(200, {
      'Content-Type': mime[extname(file)] || 'application/octet-stream',
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'X-Frame-Options': 'DENY',
      'Content-Security-Policy': "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'",
      'Cache-Control': file.endsWith('index.html') ? 'no-cache' : 'public, max-age=31536000, immutable',
    });
    createReadStream(file).pipe(res);
  } catch (error) {
    if (!res.headersSent) sendJson(res, error.status || 500, { error: error.status ? error.message : 'The request could not be completed.' });
    if (!error.status) console.error(`Request failed: ${error.message}`);
  }
});

server.listen(port, host, () => {
  console.log(`Conduit-Bank is ready at http://${host}:${port}`);
  console.log(`Local records are stored in ${dbFile}`);
  console.log('Demo mode only. Do not enter live banking credentials or sensitive financial records.');
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    server.close(() => { db.close(); process.exit(0); });
  });
}
