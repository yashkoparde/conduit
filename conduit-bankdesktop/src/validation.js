import { CSV_FIELDS, DISTRICTS, STATUSES } from './data.js';

export function normalizeIndianMobile(input) {
  const compact = String(input ?? '').replace(/[\s().-]/g, '');
  let national = compact;
  if (national.startsWith('+91')) national = national.slice(3);
  if (national.length === 11 && national.startsWith('0')) national = national.slice(1);
  if (!/^[6-9]\d{9}$/.test(national) || (compact.startsWith('+') && !compact.startsWith('+91'))) return null;
  return national;
}

export function buildCallUrl(input) {
  const national = normalizeIndianMobile(input);
  return national ? `tel:+91${national}` : null;
}

export function buildWhatsAppUrl(applicant, lang = 'en') {
  const national = normalizeIndianMobile(applicant?.phone);
  if (!national) return null;
  const message = lang === 'kn'
    ? `ನಮಸ್ಕಾರ ${applicant.name}, ನಿಮ್ಮ ಸಾಲದ ಅರ್ಜಿ ${applicant.id} ಕುರಿತು ನಾವು ಸಂಪರ್ಕಿಸುತ್ತಿದ್ದೇವೆ. ಹೆಚ್ಚಿನ ವಿವರಗಳಿಗಾಗಿ ದಯವಿಟ್ಟು ಪ್ರತಿಕ್ರಿಯಿಸಿ.`
    : `Hello ${applicant.name}, we’re following up on your loan application ${applicant.id}. Please reply when convenient so we can help with the next steps.`;
  return `https://wa.me/91${national}?text=${encodeURIComponent(message)}`;
}

export function validateApplicant(input) {
  const errors = {};
  if (!String(input.name ?? '').trim()) errors.name = 'validationName';
  if (!normalizeIndianMobile(input.phone)) errors.phone = 'validationPhone';
  if (!DISTRICTS.includes(input.district)) errors.district = 'validationDistrict';
  if (!String(input.taluk ?? '').trim()) errors.taluk = 'validationTaluk';
  const amount = Number(input.amount);
  if (!Number.isFinite(amount) || amount < 1 || amount > 1_000_000_000) errors.amount = 'validationAmount';
  if (!String(input.purpose ?? '').trim()) errors.purpose = 'validationPurpose';
  if (!String(input.officer ?? '').trim()) errors.officer = 'validationOfficer';
  if (!STATUSES.includes(input.status)) errors.status = 'validationGeneric';
  if (!isIsoDate(input.applicationDate)) errors.applicationDate = 'validationDate';
  if (input.followUpDate && !isIsoDate(input.followUpDate)) errors.followUpDate = 'validationDate';
  if (String(input.name ?? '').length > 100 || String(input.taluk ?? '').length > 80 || String(input.locality ?? '').length > 100 || String(input.purpose ?? '').length > 120 || String(input.remarks ?? '').length > 1000 || String(input.officer ?? '').length > 100) errors.form = 'validationGeneric';
  return errors;
}

function isIsoDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value ?? '')) return false;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

export function parseCsv(text) {
  const source = String(text).replace(/^\uFEFF/, '');
  const rows = [];
  let row = [];
  let cell = '';
  let quoted = false;
  for (let i = 0; i < source.length; i += 1) {
    const ch = source[i];
    if (quoted) {
      if (ch === '"' && source[i + 1] === '"') { cell += '"'; i += 1; }
      else if (ch === '"') quoted = false;
      else cell += ch;
    } else if (ch === '"' && cell.length === 0) quoted = true;
    else if (ch === ',') { row.push(cell); cell = ''; }
    else if (ch === '\n' || ch === '\r') {
      if (ch === '\r' && source[i + 1] === '\n') i += 1;
      row.push(cell); rows.push(row); row = []; cell = '';
    } else cell += ch;
  }
  if (quoted) throw new Error('CSV contains an unclosed quote.');
  if (cell.length || row.length) { row.push(cell); rows.push(row); }
  const nonEmpty = rows.filter((cells) => cells.some((value) => value.trim() !== ''));
  if (nonEmpty.length < 1) throw new Error('CSV is empty.');
  const headers = nonEmpty.shift().map((header) => header.trim());
  if (headers.length !== CSV_FIELDS.length || CSV_FIELDS.some((field) => !headers.includes(field))) throw new Error(`CSV headers must be: ${CSV_FIELDS.join(', ')}`);
  return nonEmpty.map((cells, index) => {
    if (cells.length !== headers.length) throw new Error(`Row ${index + 2} has ${cells.length} columns; expected ${headers.length}.`);
    return Object.fromEntries(headers.map((header, column) => [header, cells[column].trim()]));
  });
}

export function buildCsv(applicants) {
  const quote = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`;
  return [CSV_FIELDS.map(quote).join(','), ...applicants.map((applicant) => CSV_FIELDS.map((field) => quote(applicant[field])).join(','))].join('\r\n');
}

export function validateImport(applicants, existingIds = new Set()) {
  const seen = new Set(existingIds);
  for (let index = 0; index < applicants.length; index += 1) {
    const applicant = applicants[index];
    const rowNumber = index + 2;
    if (!String(applicant.id ?? '').trim()) return `Row ${rowNumber}: applicant ID is required.`;
    if (seen.has(applicant.id)) return `Row ${rowNumber}: duplicate applicant ID ${applicant.id}.`;
    const errors = validateApplicant(applicant);
    const first = Object.values(errors)[0];
    if (first) return `Row ${rowNumber}: ${first}.`;
    seen.add(applicant.id);
  }
  return null;
}
