import { validateApplicant, validateImport } from './validation.js';

const API_ROOT = import.meta.env.VITE_API_BASE_URL ?? '';
let storagePromise;
let databasePromise;

async function apiAvailable() {
  const response = await fetch(`${API_ROOT}/api/health`, { headers: { Accept: 'application/json' } });
  const type = response.headers.get('content-type') ?? '';
  if (!response.ok || !type.includes('application/json')) return false;
  const info = await response.json();
  return info.ok === true;
}

function storageMode() {
  if (globalThis.location?.protocol === 'file:') return Promise.resolve('indexeddb');
  if (!storagePromise) {
    storagePromise = apiAvailable().then((available) => available ? 'api' : API_ROOT ? 'api' : 'indexeddb').catch(() => API_ROOT ? 'api' : 'indexeddb');
  }
  return storagePromise;
}

function openDatabase() {
  if (!globalThis.indexedDB) return Promise.reject(new Error('This browser does not support persistent storage.'));
  if (!databasePromise) {
    databasePromise = new Promise((resolve, reject) => {
      const request = indexedDB.open('KarnatakaLoanIntelligence', 1);
      request.onupgradeneeded = () => {
        const database = request.result;
        if (!database.objectStoreNames.contains('applicants')) database.createObjectStore('applicants', { keyPath: 'id' });
        if (!database.objectStoreNames.contains('activity')) database.createObjectStore('activity', { keyPath: 'id', autoIncrement: true });
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error('Could not open browser storage.'));
    });
  }
  return databasePromise;
}

function idbRequest(store, method, value) {
  return openDatabase().then((database) => new Promise((resolve, reject) => {
    const transaction = database.transaction(store, method === 'getAll' ? 'readonly' : 'readwrite');
    const request = method === 'getAll' ? transaction.objectStore(store).getAll() : transaction.objectStore(store)[method](value);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('Could not read browser storage.'));
  }));
}

function idbLog(transaction, action, applicant) {
  transaction.objectStore('activity').add({ action, applicantId: applicant.id, applicantName: applicant.name, happenedAt: new Date().toISOString() });
}

async function idbSave(applicant, existing) {
  const database = await openDatabase();
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(['applicants', 'activity'], 'readwrite');
    const applicants = transaction.objectStore('applicants');
    const now = new Date().toISOString();
    const value = { ...applicant, createdAt: existing?.createdAt ?? now, updatedAt: now };
    applicants.put(value);
    idbLog(transaction, existing ? (existing.status === value.status ? 'updated' : 'status') : 'created', value);
    transaction.oncomplete = () => resolve(value);
    transaction.onerror = () => reject(transaction.error ?? new Error('Could not save applicant.'));
    transaction.onabort = () => reject(transaction.error ?? new Error('Applicant save was cancelled.'));
  });
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status}).`);
  return payload;
}

export async function getApplicants() {
  return (await storageMode()) === 'api' ? requestJson('/api/applicants') : idbRequest('applicants', 'getAll');
}

export async function getActivity() {
  return (await storageMode()) === 'api' ? requestJson('/api/activity') : (await idbRequest('activity', 'getAll')).sort((a, b) => b.id - a.id).slice(0, 30);
}

export async function saveApplicant(applicant, existing = null) {
  const errors = validateApplicant(applicant);
  if (Object.keys(errors).length) throw new Error('Please check the applicant information.');
  if ((await storageMode()) === 'api') {
    return requestJson(existing ? `/api/applicants/${encodeURIComponent(existing.id)}` : '/api/applicants', {
      method: existing ? 'PUT' : 'POST', body: JSON.stringify(applicant),
    });
  }
  const current = await idbRequest('applicants', 'getAll');
  if (!existing && current.some((record) => record.id === applicant.id)) throw new Error('Applicant ID already exists.');
  return idbSave(applicant, existing);
}

export async function deleteApplicant(applicant) {
  if ((await storageMode()) === 'api') return requestJson(`/api/applicants/${encodeURIComponent(applicant.id)}`, { method: 'DELETE' });
  const database = await openDatabase();
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(['applicants', 'activity'], 'readwrite');
    transaction.objectStore('applicants').delete(applicant.id);
    idbLog(transaction, 'deleted', applicant);
    transaction.oncomplete = () => resolve({ deleted: true });
    transaction.onerror = () => reject(transaction.error ?? new Error('Could not remove applicant.'));
  });
}

export async function importApplicants(applicants) {
  const existing = await getApplicants();
  const error = validateImport(applicants, new Set(existing.map(({ id }) => id)));
  if (error) throw new Error(error);
  if ((await storageMode()) === 'api') return requestJson('/api/import', { method: 'POST', body: JSON.stringify({ applicants }) });
  const database = await openDatabase();
  return new Promise((resolve, reject) => {
    const transaction = database.transaction(['applicants', 'activity'], 'readwrite');
    applicants.forEach((applicant) => {
      const now = new Date().toISOString();
      const value = { ...applicant, amount: Number(applicant.amount), createdAt: now, updatedAt: now };
      transaction.objectStore('applicants').add(value);
      idbLog(transaction, 'created', value);
    });
    transaction.oncomplete = () => resolve({ imported: applicants.length });
    transaction.onerror = () => reject(transaction.error ?? new Error('Import could not be completed.'));
    transaction.onabort = () => reject(transaction.error ?? new Error('Import could not be completed.'));
  });
}

export async function getStorageMode() {
  return (await storageMode()) === 'api' ? 'sqlite' : 'indexeddb';
}
