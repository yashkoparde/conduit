# Test report — Karnataka Loan Intelligence

**Run date:** 21 November 2026  
**Result:** Passed

## Build and run checks

- `npm run build` — passed. Vite generated the production site in `dist/` and refreshed the standalone deliverable.
- `npm test` — 4 passed, 0 failed, 0 skipped. This covered the full browser flow plus validation and local API/data tests.
- Edge browser workflow — 1 passed, 0 failed, 0 skipped.
- Final Chrome browser workflow after the last visual adjustment — 1 passed, 0 failed, 0 skipped. Login and dashboard screenshots were reviewed.
- Chrome and Edge runs reported no page exceptions or browser console errors. The login scene's layered CSS 3D transform was also checked in the browser.
- Local app at `http://127.0.0.1:4173` returned HTTP 200; its health endpoint reported SQLite storage.

## Coverage

- Demo login, invalid-login feedback, logout, sidebar navigation, hash-route refresh, and the custom not-found page.
- Applicant create, edit, delete confirmation/cancel, deletion, status changes, search, district filtering, follow-up listing, and dedicated Documents Pending filtering/navigation.
- District selector contains all 31 Karnataka districts.
- CSV export/import, quoted applicant names, duplicate prevention, and invalid-row checks.
- English/Kannada switching while navigating; 390 px mobile applicant-card layout.
- WhatsApp URL host, assigned phone number, and prefilled message were checked with a local browser stub. No external message was sent or call placed.
- Indian mobile-number normalization, call URI construction, and invalid-number rejection.
- Local HTTP API validation, duplicate IDs, create/update/delete, activity history, CSV import, direct-route fallback, and SQLite persistence after server restart.
- Standalone HTML opened directly from disk and used IndexedDB storage.

## Failures

None in the final runs.
