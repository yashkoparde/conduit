import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity, ArrowDownWideNarrow, ArrowLeft, ArrowRight, ArrowUpRight, CalendarDays, CalendarClock,
  Check, CheckCircle2, ChevronDown, ChevronRight, CircleAlert, Clipboard, Clock3, Download,
  Eye, EyeOff, FileDown, FileSpreadsheet, Filter, Globe2, IndianRupee, LayoutDashboard,
  LogOut, MapPin, Menu, MessageCircle, Moon, MoreHorizontal, Pencil, Phone, Plus, Search,
  Settings as SettingsIcon, ShieldCheck, SlidersHorizontal, Sparkles, Sun, Trash2, Upload,
  UserRound, UsersRound, X,
} from 'lucide-react';
import { DISTRICTS, DISTRICT_KN, STATUSES, STATUS_KEY, TALUKS, translations } from './data.js';
import { buildCallUrl, buildCsv, buildWhatsAppUrl, parseCsv, validateApplicant, validateImport } from './validation.js';
import { deleteApplicant, getActivity, getApplicants, getStorageMode, importApplicants, saveApplicant } from './storage.js';

const AUTH_KEY = 'kli-demo-auth';
const PROFILE_KEY = 'kli-profile-name';
const routeNames = new Set(['dashboard', 'applications', 'applicants', 'pending', 'documents-pending', 'approved', 'rejected', 'followups', 'settings']);
const demoUser = 'demo@kli.in';
const demoPass = 'KarnatakaDemo26';

function readPreference(key, fallback) {
  try { return localStorage.getItem(key) ?? fallback; } catch { return fallback; }
}

function readAuth() {
  try { return Boolean(sessionStorage.getItem(AUTH_KEY) || localStorage.getItem(AUTH_KEY)); } catch { return false; }
}

function initialRoute() {
  const route = window.location.hash.replace(/^#\/?/, '').split('?')[0];
  return route || 'dashboard';
}

function todayIso() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function formatCurrency(value, lang) {
  return new Intl.NumberFormat(lang === 'kn' ? 'kn-IN' : 'en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(value || 0));
}

function formatDate(value, lang, opts = {}) {
  if (!value) return '—';
  const date = new Date(`${value.slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(lang === 'kn' ? 'kn-IN' : 'en-IN', { day: '2-digit', month: 'short', year: 'numeric', ...opts }).format(date);
}

function relativeTime(value, lang) {
  if (!value) return '';
  const date = new Date(value);
  const days = Math.floor((Date.now() - date.getTime()) / 86400000);
  if (days <= 0) return lang === 'kn' ? 'ಇಂದು' : 'Today';
  if (days === 1) return lang === 'kn' ? 'ನಿನ್ನೆ' : 'Yesterday';
  return formatDate(value, lang);
}

function statusText(status, t) {
  return t(STATUS_KEY[status] || status);
}

function districtText(district, lang) {
  if (lang === 'kn') return DISTRICT_KN[district] ? `${DISTRICT_KN[district]} · ${district}` : district;
  return DISTRICT_KN[district] ? `${district} · ${DISTRICT_KN[district]}` : district;
}

function friendlyError(error, t, lang = 'en') {
  const message = error?.message || '';
  if (message === 'Applicant ID already exists.') return t('validationDuplicate');
  if (message === 'Please check the applicant information.') return t('validationGeneric');
  if (message.startsWith('Row ')) {
    if (/duplicate applicant ID/i.test(message)) return `${t('importDuplicate')}: ${message.match(/ID\s+([^.]*)/i)?.[1] || ''}`.trim();
    return message.replace(/\bvalidation(Name|Phone|District|Taluk|Amount|Purpose|Officer|Date|Generic)\b/g, (_, name) => t(`validation${name}`));
  }
  const serverErrors = {
    'Enter a valid Indian mobile number.': 'validationPhone',
    'Choose one of the 31 Karnataka districts.': 'validationDistrict',
    'Taluk is required and must be 80 characters or fewer.': 'validationTaluk',
    'Loan amount must be between ₹1 and ₹1,00,00,00,000.': 'validationAmount',
    'Enter a valid application date.': 'validationDate',
    'Applicant name is required and must be 100 characters or fewer.': 'validationName',
    'Loan purpose is required and must be 120 characters or fewer.': 'validationPurpose',
    'Assigned loan officer is required.': 'validationOfficer',
    'CSV is empty.': 'importInvalid',
    'CSV contains an unclosed quote.': 'importInvalid',
    'Could not open browser storage.': 'networkError',
    'The request could not be completed.': 'networkError',
  };
  if (serverErrors[message]) return t(serverErrors[message]);
  if (/^(CSV headers must|CSV |Row \d+ has|Import must)/.test(message)) return t('importInvalid');
  return message ? (lang === 'kn' ? t('validationGeneric') : message) : t('validationGeneric');
}

function IconWord({ icon: Icon, children }) {
  return <><Icon size={15} strokeWidth={1.8} aria-hidden="true" />{children}</>;
}

function BrandMark({ compact = false }) {
  return (
    <div className={`brand-lockup ${compact ? 'brand-compact' : ''}`}>
      <div className="brand-mark" aria-hidden="true"><span>C</span><i /></div>
      {!compact && <div className="brand-name-wrap"><span className="brand-name">CONDUIT <b>BANK</b></span><span className="brand-caption">LOAN INTELLIGENCE</span></div>}
    </div>
  );
}

function LocaleSwitch({ lang, setLang, compact = false }) {
  return (
    <div className={`locale-switch ${compact ? 'locale-compact' : ''}`} role="group" aria-label={lang === 'kn' ? 'ಭಾಷೆ' : 'Language'}>
      <button className={lang === 'en' ? 'selected' : ''} onClick={() => setLang('en')} aria-pressed={lang === 'en'}>EN</button>
      <span aria-hidden="true" />
      <button className={lang === 'kn' ? 'selected' : ''} onClick={() => setLang('kn')} aria-pressed={lang === 'kn'}>ಕನ್ನಡ</button>
    </div>
  );
}

function ThemeToggle({ theme, setTheme, t }) {
  const next = theme === 'dark' ? 'light' : 'dark';
  return <button type="button" className="icon-button theme-toggle" onClick={() => setTheme(next)} title={`${t('theme')}: ${t(next)}`} aria-label={`${t('theme')}: ${t(next)}`}>{theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}</button>;
}

function App() {
  const [lang, setLang] = useState(() => readPreference('kli-language', 'en'));
  const [theme, setTheme] = useState(() => readPreference('kli-theme', 'dark'));
  const [route, setRoute] = useState(initialRoute);
  const [loggedIn, setLoggedIn] = useState(readAuth);
  const [applicants, setApplicants] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [toast, setToast] = useState('');
  const [modal, setModal] = useState(null);
  const [profileName, setProfileName] = useState(() => readPreference(PROFILE_KEY, 'Loan officer'));
  const [storageMode, setStorageMode] = useState('sqlite');
  const [alertsEnabled, setAlertsEnabled] = useState(() => readPreference('kli-followup-alerts', 'true') === 'true');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const toastTimer = useRef(null);
  const t = useCallback((key) => translations[lang]?.[key] ?? translations.en[key] ?? key, [lang]);

  useEffect(() => {
    try { localStorage.setItem('kli-language', lang); } catch { /* Browser privacy mode */ }
    document.documentElement.lang = lang === 'kn' ? 'kn' : 'en';
  }, [lang]);

  useEffect(() => {
    try { localStorage.setItem('kli-theme', theme); } catch { /* Browser privacy mode */ }
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    const syncRoute = () => setRoute(initialRoute());
    window.addEventListener('hashchange', syncRoute);
    return () => window.removeEventListener('hashchange', syncRoute);
  }, []);

  useEffect(() => {
    if (!loggedIn || route === 'login') return;
    let active = true;
    setLoading(true);
    setLoadError('');
    Promise.all([getApplicants(), getActivity(), getStorageMode()]).then(([list, activity, mode]) => {
      if (!active) return;
      setApplicants(list);
      setActivities(activity);
      setStorageMode(mode);
    }).catch(() => {
      if (!active) return;
      setLoadError(t('networkError'));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loggedIn, route, t]);

  useEffect(() => () => clearTimeout(toastTimer.current), []);

  const notify = useCallback((message) => {
    setToast(message);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(''), 3600);
  }, []);

  const navigate = useCallback((next) => {
    setMobileNavOpen(false);
    setRoute(next);
    window.location.hash = `/${next}`;
  }, []);

  const refresh = useCallback(async () => {
    const [list, activity, mode] = await Promise.all([getApplicants(), getActivity(), getStorageMode()]);
    setApplicants(list);
    setActivities(activity);
    setStorageMode(mode);
  }, []);

  const login = (username, password, remember) => {
    if (username.trim().toLowerCase() !== demoUser || password !== demoPass) return false;
    sessionStorage.setItem(AUTH_KEY, '1');
    if (remember) localStorage.setItem(AUTH_KEY, '1');
    else localStorage.removeItem(AUTH_KEY);
    setLoggedIn(true);
    navigate('dashboard');
    return true;
  };

  const logout = () => {
    sessionStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(AUTH_KEY);
    setLoggedIn(false);
    setApplicants([]);
    setActivities([]);
    setMobileNavOpen(false);
    navigate('login');
  };

  const persistApplicant = async (value, existing) => {
    const errors = validateApplicant(value);
    if (Object.keys(errors).length) return { errors };
    try {
      await saveApplicant(value, existing);
      await refresh();
      setModal(null);
      notify(existing ? t('activityUpdated') : t('activityCreated'));
      return { ok: true };
    } catch (error) {
      return { error: friendlyError(error, t, lang) };
    }
  };

  const removeApplicant = async (applicant) => {
    try {
      await deleteApplicant(applicant);
      await refresh();
      setModal(null);
      notify(t('activityDeleted'));
    } catch (error) { notify(friendlyError(error, t, lang)); }
  };

  const updateSettings = ({ name, alerts }) => {
    const cleaned = name.trim() || (lang === 'kn' ? 'ಸಾಲ ಅಧಿಕಾರಿ' : 'Loan officer');
    setProfileName(cleaned);
    setAlertsEnabled(alerts);
    try {
      localStorage.setItem(PROFILE_KEY, cleaned);
      localStorage.setItem('kli-followup-alerts', String(alerts));
    } catch { /* Browser privacy mode */ }
    notify(t('preferencesSaved'));
  };

  if (!loggedIn || route === 'login') {
    return <LoginScreen lang={lang} setLang={setLang} t={t} onLogin={login} />;
  }

  const routeValid = routeNames.has(route);
  return (
    <div className="app-shell">
      <Sidebar route={route} navigate={navigate} logout={logout} lang={lang} t={t} profileName={profileName} mobileOpen={mobileNavOpen} setMobileOpen={setMobileNavOpen} />
      {mobileNavOpen && <button className="mobile-scrim" aria-label={t('close')} onClick={() => setMobileNavOpen(false)} />}
      <main className="main-shell">
        <Topbar lang={lang} setLang={setLang} theme={theme} setTheme={setTheme} t={t} profileName={profileName} onMenu={() => setMobileNavOpen(true)} onSettings={() => navigate('settings')} />
        <div className="workspace">
          {loadError ? <ErrorState message={loadError} retry={() => { setLoadError(''); refresh().catch(() => setLoadError(t('networkError'))); }} t={t} /> : loading ? <LoadingState t={t} /> : !routeValid ? <NotFound t={t} navigate={navigate} /> : route === 'settings' ? (
            <SettingsPage lang={lang} setLang={setLang} theme={theme} setTheme={setTheme} name={profileName} alerts={alertsEnabled} onSave={updateSettings} mode={storageMode} t={t} />
          ) : route === 'followups' ? (
            <FollowupsPage applicants={applicants} lang={lang} t={t} onOpen={(person) => setModal({ kind: 'details', applicant: person })} onContact={(person, type) => contactApplicant(person, type, lang, t, notify)} />
          ) : (
            <DashboardPage route={route} applicants={applicants} activities={activities} lang={lang} t={t} profileName={profileName} onAdd={() => setModal({ kind: 'form' })} onImport={() => setModal({ kind: 'import' })} onExport={() => exportApplicants(applicants, notify, lang)} onOpen={(person) => setModal({ kind: 'details', applicant: person })} onEdit={(person) => setModal({ kind: 'form', applicant: person })} onContact={(person, type) => contactApplicant(person, type, lang, t, notify)} />
          )}
        </div>
      </main>
      {modal?.kind === 'form' && <ApplicantForm applicant={modal.applicant} applicants={applicants} lang={lang} t={t} onClose={() => setModal(null)} onSave={persistApplicant} />}
      {modal?.kind === 'details' && <ApplicantDetails applicant={modal.applicant} lang={lang} t={t} onClose={() => setModal(null)} onEdit={(person) => setModal({ kind: 'form', applicant: person })} onDelete={(person) => setModal({ kind: 'confirm', applicant: person })} onSave={persistApplicant} onContact={(person, type) => contactApplicant(person, type, lang, t, notify)} onNotify={notify} />}
      {modal?.kind === 'confirm' && <ConfirmDialog applicant={modal.applicant} t={t} onCancel={() => setModal({ kind: 'details', applicant: modal.applicant })} onConfirm={() => removeApplicant(modal.applicant)} />}
      {modal?.kind === 'import' && <ImportDialog applicants={applicants} lang={lang} t={t} onClose={() => setModal(null)} onImport={async (records) => {
        try { const result = await importApplicants(records); await refresh(); setModal(null); notify(`${t('importSuccess')} · ${result.imported}`); }
        catch (error) { throw new Error(friendlyError(error, t, lang)); }
      }} />}
      {toast && <div className="toast" role="status"><CheckCircle2 size={17} />{toast}</div>}
      <div className="app-backdrop" aria-hidden="true" />
    </div>
  );
}

function Sidebar({ route, navigate, logout, lang, t, profileName, mobileOpen }) {
  const nav = [
    { id: 'dashboard', label: t('dashboard'), icon: LayoutDashboard },
    { id: 'applications', label: t('applications'), icon: FileSpreadsheet },
    { id: 'applicants', label: t('applicants'), icon: UsersRound },
    { id: 'pending', label: t('pending'), icon: Clock3 },
    { id: 'documents-pending', label: t('documentsPending'), icon: Clipboard },
    { id: 'approved', label: t('approved'), icon: CheckCircle2 },
    { id: 'rejected', label: t('rejected'), icon: CircleAlert },
    { id: 'followups', label: t('followups'), icon: CalendarClock },
  ];
  return (
    <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
      <div className="sidebar-head"><BrandMark /><div className="sidebar-live"><span />{lang === 'kn' ? 'ಕಾಂಡ್ಯೂಟ್' : 'CONDUIT'}</div></div>
      <div className="sidebar-section-label">{t('navSection')}</div>
      <nav className="sidebar-nav" aria-label={t('navSection')}>
        {nav.map(({ id, label, icon: Icon }) => <button key={id} className={`nav-item ${route === id ? 'active' : ''}`} onClick={() => navigate(id)} aria-current={route === id ? 'page' : undefined}><Icon size={17} strokeWidth={1.75} /><span>{label}</span>{id === 'dashboard' && <span className="nav-shortcut">⌘ 1</span>}</button>)}
      </nav>
      <div className="sidebar-bottom">
        <div className="regional-card"><div className="regional-emblem"><Sparkles size={15} /></div><div><b>{t('regionalNote')}</b><span>{t('madeFor')}</span></div></div>
        <button className={`nav-item ${route === 'settings' ? 'active' : ''}`} onClick={() => navigate('settings')}><SettingsIcon size={17} /><span>{t('settings')}</span></button>
        <button className="nav-item logout-item" onClick={logout}><LogOut size={17} /><span>{t('logout')}</span></button>
        <div className="sidebar-profile"><div className="avatar avatar-side">{profileName.trim().slice(0, 1).toUpperCase() || 'L'}</div><div className="profile-copy"><b>{profileName}</b><span>{t('demoWorkspace')}</span></div><MoreHorizontal size={18} className="profile-more" /></div>
      </div>
    </aside>
  );
}

function Topbar({ lang, setLang, theme, setTheme, t, profileName, onMenu, onSettings }) {
  return (
    <header className="topbar">
      <button className="mobile-menu icon-button" aria-label={t('navSection')} onClick={onMenu}><Menu size={19} /></button>
      <div className="top-breadcrumb"><span className="breadcrumb-root">CONDUIT</span><ChevronRight size={14} /><b>{({ dashboard: t('dashboard'), applications: t('applications'), applicants: t('applicants'), pending: t('pending'), 'documents-pending': t('documentsPending'), approved: t('approved'), rejected: t('rejected'), followups: t('followups'), settings: t('settings') })[initialRoute()] || t('dashboard')}</b></div>
      <div className="top-actions"><span className="secure-pill"><ShieldCheck size={14} />{lang === 'kn' ? 'ಸ್ಥಳೀಯ ಡೆಮೊ' : 'LOCAL DEMO'}</span><LocaleSwitch lang={lang} setLang={setLang} compact /><ThemeToggle theme={theme} setTheme={setTheme} t={t} /><button className="top-avatar" onClick={onSettings} title={profileName}>{profileName.slice(0, 1).toUpperCase()}</button></div>
    </header>
  );
}

function LoginScreen({ lang, setLang, t, onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [visible, setVisible] = useState(false);
  const [showCredentials, setShowCredentials] = useState(true);
  const [forgot, setForgot] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    await new Promise((resolve) => setTimeout(resolve, 360));
    if (!onLogin(username, password, remember)) setError(t('invalidLogin'));
    setBusy(false);
  };

  return (
    <div className="login-page">
      <section className="login-art" onPointerMove={(event) => {
        const bounds = event.currentTarget.getBoundingClientRect();
        const x = (event.clientX - bounds.left) / bounds.width - 0.5;
        const y = (event.clientY - bounds.top) / bounds.height - 0.5;
        event.currentTarget.style.setProperty('--scene-x', `${-x * 14}px`);
        event.currentTarget.style.setProperty('--scene-y', `${-y * 10}px`);
      }} onPointerLeave={(event) => {
        event.currentTarget.style.setProperty('--scene-x', '0px');
        event.currentTarget.style.setProperty('--scene-y', '0px');
      }}>
        <div className="login-top"><BrandMark /><LocaleSwitch lang={lang} setLang={setLang} /></div>
        <div className="art-scene" aria-hidden="true">
          <div className="art-depth depth-back" /><div className="art-depth depth-front" />
          <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" /><div className="art-red-orbit" />
          <div className="art-star"><span>C</span><i /><b /></div>
          <span className="scene-particle particle-one" /><span className="scene-particle particle-two" /><span className="scene-particle particle-three" />
        </div>
        <div className="login-art-copy"><div className="eyebrow"><span className="green-dot" /> {lang === 'kn' ? 'ಕಾಂಡ್ಯೂಟ್ ಬ್ಯಾಂಕ್ · ಸಾಲ ಬುದ್ಧಿಮತ್ತೆ' : 'CONDUIT-BANK LOAN INTELLIGENCE'}</div><h1>{t('loginTitle')}</h1><p>{t('loginSubtitle')}</p><div className="art-rule"><span /><span /><span /></div></div>
        <div className="art-footer"><span>12°58′ N &nbsp; 77°35′ E</span><span>ಕನ್ನಡ · ENGLISH</span></div>
      </section>
      <section className="login-panel"><div className="login-mobile-brand"><BrandMark /></div><div className="login-card">
        <div className="login-icon"><ShieldCheck size={23} /></div>
        <p className="eyebrow muted-eyebrow">{t('demoNotice')}</p>
        <h2>{forgot ? t('forgotTitle') : t('login')}</h2><p className="login-hint">{forgot ? t('forgotDescription') : t('loginHint')}</p>
        {forgot ? <button className="button button-primary wide-button" onClick={() => { setForgot(false); setError(''); }}>{t('backToLogin')} <ArrowLeft size={16} /></button> : <form onSubmit={submit} noValidate>
          <label className="field-label" htmlFor="username">{t('username')}</label><div className="input-wrap"><UserRound size={16} /><input id="username" name="username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="name@example.com" /></div>
          <label className="field-label" htmlFor="password">{t('password')}</label><div className="input-wrap"><ShieldCheck size={16} /><input id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="••••••••••" /><button type="button" className="input-end-button" onClick={() => setVisible(!visible)} aria-label={visible ? t('hidePassword') : t('showPassword')}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></div>
          <div className="login-extras"><label className="check-label"><input type="checkbox" checked={remember} onChange={(event) => setRemember(event.target.checked)} /><span className="fake-checkbox"><Check size={12} /></span>{t('remember')}</label><button className="text-button" type="button" onClick={() => setForgot(true)}>{t('forgotPassword')}</button></div>
          {error && <div className="inline-error"><CircleAlert size={15} />{error}</div>}
          <button className="button button-primary wide-button login-submit" disabled={busy} type="submit">{busy ? t('signingIn') : t('signIn')} {!busy && <ArrowRight size={16} />}</button>
        </form>}
        {!forgot && <div className="demo-credentials"><button className="credentials-toggle" onClick={() => setShowCredentials(!showCredentials)} aria-expanded={showCredentials}><span className="demo-badge">DEMO</span>{t('demoCredentials')}<ChevronDown className={showCredentials ? 'rotate-chevron' : ''} size={15} /></button>{showCredentials && <div className="credentials-content"><div><span>{t('username')}</span><b>{demoUser}</b></div><div><span>{t('password')}</span><b>{demoPass}</b></div><button className="fill-demo" onClick={() => { setUsername(demoUser); setPassword(demoPass); setError(''); }}>{lang === 'kn' ? 'ಡೆಮೊ ವಿವರಗಳನ್ನು ತುಂಬಿಸಿ' : 'Fill demo credentials'}<ArrowRight size={14} /></button></div>}</div>}
        <p className="login-disclaimer"><CircleAlert size={14} />{t('demoAuthDescription')}</p>
      </div><div className="login-bottom">{t('versionNote')}<span>•</span>{t('regionalNote')}</div></section>
    </div>
  );
}

function LoadingState({ t }) {
  return <div className="loading-state"><div className="loading-ring" /><span>{t('loading')}</span></div>;
}

function ErrorState({ message, retry, t }) {
  return <div className="empty-state error-state"><div className="empty-icon error-icon"><CircleAlert size={23} /></div><h2>{t('networkError')}</h2><p>{message}</p><button className="button button-secondary" onClick={retry}>{t('clearFilters')} <ArrowRight size={15} /></button></div>;
}

function NotFound({ t, navigate }) {
  return <div className="empty-state not-found"><div className="not-found-code">404</div><h1>{t('notFound')}</h1><p>{t('notFoundDescription')}</p><button className="button button-primary" onClick={() => navigate('dashboard')}>{t('goDashboard')} <ArrowRight size={15} /></button></div>;
}

function DashboardPage({ route, applicants, activities, lang, t, profileName, onAdd, onImport, onExport, onOpen, onEdit, onContact }) {
  const [search, setSearch] = useState('');
  const [district, setDistrict] = useState('');
  const [status, setStatus] = useState('');
  const [sort, setSort] = useState('updated');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const isPending = route === 'pending';
  const routeTitle = route === 'approved' ? t('approved') : route === 'rejected' ? t('rejected') : route === 'documents-pending' ? t('documentsPending') : isPending ? t('pending') : route === 'applicants' ? t('applicants') : route === 'applications' ? t('applications') : t('dashboard');
  const scoped = useMemo(() => applicants.filter((item) => {
    if (route === 'pending' && !['New', 'Under Review', 'Documents Pending'].includes(item.status)) return false;
    if (route === 'documents-pending' && item.status !== 'Documents Pending') return false;
    if (route === 'approved' && !['Approved', 'Disbursed'].includes(item.status)) return false;
    if (route === 'rejected' && item.status !== 'Rejected') return false;
    return true;
  }), [applicants, route]);
  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    const result = scoped.filter((item) => {
      const matchesSearch = !query || [item.id, item.name, item.phone, item.district, item.taluk, item.locality, item.purpose].some((value) => String(value || '').toLowerCase().includes(query));
      return matchesSearch && (!district || item.district === district) && (!status || item.status === status);
    });
    return result.sort((a, b) => {
      if (sort === 'oldest') return a.applicationDate.localeCompare(b.applicationDate);
      if (sort === 'name') return a.name.localeCompare(b.name);
      if (sort === 'amount') return Number(b.amount) - Number(a.amount);
      return (b.updatedAt || b.applicationDate).localeCompare(a.updatedAt || a.applicationDate);
    });
  }, [scoped, search, district, status, sort]);

  const clearFilters = () => { setSearch(''); setDistrict(''); setStatus(''); };
  const noFiltered = applicants.length > 0 && filtered.length === 0;
  const defaultProfile = profileName === 'Loan officer' || profileName === 'ಸಾಲ ಅಧಿಕಾರಿ';
  const greetingName = defaultProfile ? t('greetingRole') : profileName.trim().split(/\s+/)[0];
  return (
    <>
      <div className="page-intro"><div><div className="eyebrow page-eyebrow"><span className="green-dot" /> {lang === 'kn' ? 'ಕಾಂಡ್ಯೂಟ್ ಬ್ಯಾಂಕ್ · ಸಾಲ ನಿರ್ವಹಣೆ' : 'CONDUIT-BANK · LOAN OPERATIONS'}</div><h1>{t('welcome')}, <span>{greetingName}.</span></h1><p>{t('workspace')}</p></div><button className="button button-primary add-main-button" onClick={onAdd}><Plus size={17} />{t('addApplicant')}</button></div>
      <div className="desk-layout">
        <section className="desk-main">
          <div className="section-heading"><div><div className="section-kicker">{t('applicationDesk')}</div><h2>{routeTitle}</h2></div><span className="result-count"><b>{filtered.length}</b> {filtered.length === 1 ? t('request') : t('requests')}</span></div>
          <div className="filter-panel">
            <div className="search-input"><Search size={17} /><input aria-label={t('searchLabel')} value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('search')} /><kbd>⌘ K</kbd></div>
            <button className={`filter-button ${filtersOpen ? 'filter-active' : ''}`} onClick={() => setFiltersOpen(!filtersOpen)}><SlidersHorizontal size={16} />{t('filterLabel')}<span className="filter-indicator">{(district ? 1 : 0) + (status ? 1 : 0) || ''}</span></button>
            <div className="sort-select-wrap"><ArrowDownWideNarrow size={15} /><select aria-label={t('sortLabel')} value={sort} onChange={(event) => setSort(event.target.value)}><option value="updated">{t('newest')}</option><option value="oldest">{t('oldest')}</option><option value="name">{t('nameAZ')}</option><option value="amount">{t('amountHigh')}</option></select><ChevronDown size={13} className="select-chevron" /></div>
            <button className="icon-button export-button" onClick={onExport} title={t('exportCSV')} aria-label={t('exportCSV')}><FileDown size={17} /></button>
          </div>
          {filtersOpen && <div className="filter-row"><label><span>{t('district')}</span><select value={district} onChange={(event) => setDistrict(event.target.value)}><option value="">{t('allDistricts')}</option>{DISTRICTS.map((item) => <option value={item} key={item}>{districtText(item, lang)}</option>)}</select></label><label><span>{t('status')}</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">{t('allStatuses')}</option>{STATUSES.map((item) => <option value={item} key={item}>{statusText(item, t)}</option>)}</select></label><button className="text-button clear-button" onClick={clearFilters}>{t('clearFilters')}</button></div>}
          {applicants.length === 0 ? <EmptyApplicants onAdd={onAdd} onImport={onImport} t={t} /> : noFiltered ? <div className="compact-empty"><div className="empty-icon small-empty-icon"><Search size={19} /></div><h3>{t('noResults')}</h3><button className="text-button" onClick={clearFilters}>{t('clearFilters')}</button></div> : <ApplicantTable applicants={filtered} lang={lang} t={t} onOpen={onOpen} onEdit={onEdit} onContact={onContact} />}
          <div className="desk-footer"><span>{t('showing')} <b>{filtered.length}</b> {t('of')} <b>{scoped.length}</b> {t('requests')}</span><button className="text-button import-inline" onClick={onImport}><Upload size={14} />{t('importCSV')}</button></div>
        </section>
        <aside className="activity-panel"><div className="activity-heading"><div><span className="section-kicker">{lang === 'kn' ? 'ಕಾರ್ಯಕ್ಷೇತ್ರ ಫೀಡ್' : 'WORKSPACE FEED'}</span><h3>{t('recentActivity')}</h3></div><span className="activity-live"><span /></span></div>
          {activities.length ? <div className="activity-list">{activities.slice(0, 6).map((item) => <div className="activity-item" key={item.id || `${item.applicantId}-${item.happenedAt}`}><div className={`activity-icon activity-${item.action}`}><Activity size={15} /></div><div className="activity-copy"><p><b>{item.applicantName}</b> {t(item.action === 'created' ? 'activityCreated' : item.action === 'status' ? 'activityStatus' : item.action === 'deleted' ? 'activityDeleted' : 'activityUpdated').toLowerCase()}</p><span>{item.applicantId} <i>·</i> {relativeTime(item.happenedAt, lang)}</span></div></div>)}</div> : <div className="activity-empty"><div className="activity-empty-icon"><Activity size={18} /></div><p>{t('noActivity')}</p></div>}
          <div className="region-note"><div className="region-note-top"><MapPin size={14} /><span>{lang === 'kn' ? 'ಕಾಂಡ್ಯೂಟ್ ಬ್ಯಾಂಕ್' : 'CONDUIT-BANK'}</span></div><strong className="region-word">{lang === 'kn' ? 'ಕರ್ನಾಟಕ' : 'Karnataka'}</strong><span>{lang === 'kn' ? 'ಸ್ಥಳೀಯ ಸಾಲ ಕಾರ್ಯಾಚರಣೆ' : 'Local lending operations'}</span><small>{lang === 'kn' ? 'ಜಿಲ್ಲೆ · ತಾಲೂಕು · ಗ್ರಾಮ' : 'District · Taluk · Village'}</small></div>
        </aside>
      </div>
    </>
  );
}

function EmptyApplicants({ onAdd, onImport, t }) {
  return <div className="empty-state table-empty"><div className="empty-visual"><div className="empty-visual-ring ring-a" /><div className="empty-visual-ring ring-b" /><div className="empty-visual-icon"><FileSpreadsheet size={24} /></div><span className="empty-spark spark-a" /><span className="empty-spark spark-b" /></div><h3>{t('noApplicants')}</h3><p>{t('emptyDescription')}</p><div className="empty-actions"><button className="button button-primary" onClick={onAdd}><Plus size={16} />{t('addApplicant')}</button><button className="button button-secondary" onClick={onImport}><Upload size={16} />{t('importCSV')}</button></div></div>;
}

function ApplicantTable({ applicants, lang, t, onOpen, onEdit, onContact }) {
  return (
    <div className="table-wrap"><table className="applicant-table"><thead><tr><th>{lang === 'kn' ? 'ಅರ್ಜಿದಾರ' : 'APPLICANT'}</th><th>{t('district')}</th><th>{t('requested')}</th><th>{t('status')}</th><th>{t('followUp')}</th><th aria-label={t('applicantDetails')}></th></tr></thead><tbody>
      {applicants.map((person) => <tr key={person.id} className="applicant-row"><td><button className="applicant-identity" onClick={() => onOpen(person)}><span className="avatar avatar-person">{person.name.trim().slice(0, 1).toUpperCase()}</span><span><b>{person.name}</b><small>{person.id}</small></span></button></td><td><button className="district-cell" onClick={() => onOpen(person)} title={districtText(person.district, lang)}><MapPin size={13} />{lang === 'kn' ? DISTRICT_KN[person.district] || person.district : person.district}</button></td><td><button className="amount-cell" onClick={() => onOpen(person)}>{formatCurrency(person.amount, lang)}</button></td><td><span className={`status-badge status-${person.status.toLowerCase().replaceAll(' ', '-')}`}><i />{statusText(person.status, t)}</span></td><td><span className={`followup-cell ${person.followUpDate && person.followUpDate < todayIso() ? 'followup-late' : ''}`}>{person.followUpDate ? <><CalendarDays size={13} />{formatDate(person.followUpDate, lang, { day: '2-digit', month: 'short' })}</> : '—'}</span></td><td><div className="row-actions"><button className="row-action" title={t('callApplicant')} aria-label={t('callApplicant')} onClick={() => onContact(person, 'call')}><Phone size={15} /></button><button className="row-action whatsapp-action" title={t('whatsappApplicant')} aria-label={t('whatsappApplicant')} onClick={() => onContact(person, 'whatsapp')}><MessageCircle size={15} /></button><button className="row-action row-more" title={t('applicantDetails')} aria-label={t('applicantDetails')} onClick={() => onOpen(person)}><MoreHorizontal size={17} /></button></div></td></tr>)}
    </tbody></table>
      <div className="mobile-applicants">{applicants.map((person) => <article className="mobile-applicant-card" key={person.id}><button className="mobile-card-main" onClick={() => onOpen(person)}><div className="mobile-person-line"><span className="avatar avatar-person">{person.name.trim().slice(0, 1).toUpperCase()}</span><div><b>{person.name}</b><small>{person.id}</small></div><ChevronRight size={17} /></div><div className="mobile-card-data"><span><small>{t('district')}</small><b>{lang === 'kn' ? DISTRICT_KN[person.district] || person.district : person.district}</b></span><span><small>{t('requested')}</small><b>{formatCurrency(person.amount, lang)}</b></span></div><div className="mobile-card-bottom"><span className={`status-badge status-${person.status.toLowerCase().replaceAll(' ', '-')}`}><i />{statusText(person.status, t)}</span><span className="followup-cell">{person.followUpDate ? <><CalendarDays size={13} />{formatDate(person.followUpDate, lang, { day: '2-digit', month: 'short' })}</> : '—'}</span></div></button><div className="mobile-card-actions"><button onClick={() => onContact(person, 'call')}><Phone size={15} />{t('call')}</button><button onClick={() => onContact(person, 'whatsapp')}><MessageCircle size={15} />{t('whatsapp')}</button><button onClick={() => onEdit(person)}><Pencil size={15} />{t('editApplicant')}</button></div></article>)}</div>
    </div>
  );
}

function contactApplicant(person, type, lang, t, notify) {
  if (type === 'call') {
    const url = buildCallUrl(person.phone);
    if (!url) { notify(t('contactInvalid')); return; }
    window.location.href = url;
    return;
  }
  const url = buildWhatsAppUrl(person, lang);
  if (!url) { notify(t('contactInvalid')); return; }
  window.open(url, '_blank', 'noopener,noreferrer');
}

async function copyText(value) {
  if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(String(value));
  const input = document.createElement('textarea');
  input.value = String(value); input.setAttribute('readonly', ''); input.style.position = 'fixed'; input.style.opacity = '0';
  document.body.appendChild(input); input.select(); const ok = document.execCommand('copy'); input.remove();
  if (!ok) throw new Error('Clipboard unavailable');
}

function generateApplicantId(applicants) {
  const year = String(new Date().getFullYear()).slice(-2);
  const prefix = `KLI-${year}-`;
  const max = applicants.reduce((highest, item) => item.id.startsWith(prefix) ? Math.max(highest, Number(item.id.slice(prefix.length)) || 0) : highest, 0);
  return `${prefix}${String(max + 1).padStart(4, '0')}`;
}

function ApplicantForm({ applicant, applicants, lang, t, onClose, onSave }) {
  const isEdit = Boolean(applicant);
  const [form, setForm] = useState(() => applicant ? { ...applicant, amount: String(applicant.amount) } : {
    id: generateApplicantId(applicants), name: '', phone: '', district: '', taluk: '', locality: '', amount: '', purpose: '', applicationDate: todayIso(), status: 'New', officer: readPreference(PROFILE_KEY, 'Loan officer'), remarks: '', followUpDate: '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const update = (key, value) => { setForm((previous) => ({ ...previous, [key]: value })); setErrors((previous) => ({ ...previous, [key]: undefined, form: undefined })); setFormError(''); };
  const submit = async (event) => {
    event.preventDefault();
    const validation = validateApplicant(form);
    if (!isEdit && applicants.some((item) => item.id === form.id)) validation.id = 'validationDuplicate';
    if (Object.keys(validation).length) { setErrors(validation); return; }
    setSaving(true);
    const result = await onSave({ ...form, amount: Number(form.amount) }, applicant || null);
    if (result?.errors) setErrors(result.errors);
    if (result?.error) setFormError(result.error);
    setSaving(false);
  };
  const fieldError = (key) => errors[key] ? <span className="field-error">{t(errors[key])}</span> : null;
  const districtsSorted = DISTRICTS;
  return (
    <ModalFrame title={isEdit ? t('editApplicant') : t('newApplicant')} onClose={onClose} closeLabel={t('close')} className="form-modal">
      <form className="applicant-form" onSubmit={submit} noValidate>
        <div className="form-modal-intro"><span className="form-intro-icon"><FileSpreadsheet size={17} /></span><div><b>{isEdit ? applicant.name : t('applicationDesk')}</b><span>{isEdit ? applicant.id : `${t('applicantId')} · ${form.id}`}</span></div>{isEdit && <span className="immutable-id">{applicant.id}</span>}</div>
        <div className="form-section-label"><span>01</span>{t('applicantInformation')}</div>
        <div className="form-grid">
          <FormField label={t('applicantId')} error={fieldError('id')}><input className="form-control readonly-input" value={form.id} readOnly aria-readonly="true" /></FormField>
          <FormField label={t('fullName')} required error={fieldError('name')}><input autoFocus className={`form-control ${errors.name ? 'input-error' : ''}`} value={form.name} onChange={(event) => update('name', event.target.value)} maxLength={100} placeholder={lang === 'kn' ? 'ಅರ್ಜಿದಾರರ ಪೂರ್ಣ ಹೆಸರು' : 'Applicant’s full name'} /></FormField>
          <FormField label={t('contactNumber')} required hint={t('phoneHint')} error={fieldError('phone')}><input className={`form-control ${errors.phone ? 'input-error' : ''}`} value={form.phone} onChange={(event) => update('phone', event.target.value)} maxLength={20} inputMode="tel" autoComplete="tel" placeholder="+91 98765 43210" /></FormField>
          <FormField label={t('district')} required error={fieldError('district')}><select className={`form-control ${errors.district ? 'input-error' : ''}`} value={form.district} onChange={(event) => { update('district', event.target.value); update('taluk', ''); }}><option value="">{t('selectDistrict')}</option>{districtsSorted.map((item) => <option key={item} value={item}>{districtText(item, lang)}</option>)}</select></FormField>
          <FormField label={t('taluk')} required error={fieldError('taluk')}><input list="taluk-options" className={`form-control ${errors.taluk ? 'input-error' : ''}`} value={form.taluk} onChange={(event) => update('taluk', event.target.value)} maxLength={80} placeholder={t('talukPlaceholder')} /><datalist id="taluk-options">{(TALUKS[form.district] || []).map((item) => <option value={item} key={item} />)}</datalist></FormField>
          <FormField label={t('villageCity')}><input className="form-control" value={form.locality} onChange={(event) => update('locality', event.target.value)} maxLength={100} placeholder={t('localityPlaceholder')} /></FormField>
        </div>
        <div className="form-section-label section-label-gap"><span>02</span>{t('loanRequest')}</div>
        <div className="form-grid">
          <FormField label={t('loanAmount')} required error={fieldError('amount')}><div className="currency-input"><IndianRupee size={15} /><input className={`form-control ${errors.amount ? 'input-error' : ''}`} type="number" min="1" max="1000000000" step="1" value={form.amount} onChange={(event) => update('amount', event.target.value)} placeholder="1,00,000" /></div></FormField>
          <FormField label={t('purpose')} required error={fieldError('purpose')}><input className={`form-control ${errors.purpose ? 'input-error' : ''}`} value={form.purpose} onChange={(event) => update('purpose', event.target.value)} maxLength={120} placeholder={t('purposePlaceholder')} /></FormField>
          <FormField label={t('applicationDate')} required error={fieldError('applicationDate')}><input className={`form-control ${errors.applicationDate ? 'input-error' : ''}`} type="date" value={form.applicationDate} onChange={(event) => update('applicationDate', event.target.value)} /></FormField>
          <FormField label={t('status')} required><select className="form-control" value={form.status} onChange={(event) => update('status', event.target.value)}>{STATUSES.map((item) => <option key={item} value={item}>{statusText(item, t)}</option>)}</select></FormField>
          <FormField label={t('officer')} required error={fieldError('officer')}><input className={`form-control ${errors.officer ? 'input-error' : ''}`} value={form.officer} onChange={(event) => update('officer', event.target.value)} maxLength={100} /></FormField>
          <FormField label={t('followUpDate')} error={fieldError('followUpDate')}><input className={`form-control ${errors.followUpDate ? 'input-error' : ''}`} type="date" value={form.followUpDate} onChange={(event) => update('followUpDate', event.target.value)} /></FormField>
          <FormField label={t('remarks')} wide><textarea className="form-control form-textarea" value={form.remarks} onChange={(event) => update('remarks', event.target.value)} maxLength={1000} placeholder={t('remarksPlaceholder')} rows={3} /></FormField>
        </div>
        {formError && <div className="inline-error form-inline-error"><CircleAlert size={15} />{formError}</div>}
        {errors.form && <div className="inline-error form-inline-error"><CircleAlert size={15} />{t(errors.form)}</div>}
        <div className="modal-footer form-footer"><button type="button" className="button button-secondary" onClick={onClose}>{t('cancel')}</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? t('signingIn') : isEdit ? t('saveChanges') : t('saveApplicant')} {!saving && <Check size={16} />}</button></div>
      </form>
    </ModalFrame>
  );
}

function FormField({ label, required, hint, error, wide, children }) {
  return <label className={`form-field ${wide ? 'form-field-wide' : ''}`}><span className="form-field-label">{label}{required && <i>*</i>}</span>{children}{hint && <small className="field-hint">{hint}</small>}{error}</label>;
}

function ModalFrame({ title, onClose, closeLabel = 'Close', children, className = '' }) {
  useEffect(() => {
    const onKey = (event) => { if (event.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { window.removeEventListener('keydown', onKey); document.body.style.overflow = previous; };
  }, [onClose]);
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className={`modal-card ${className}`} role="dialog" aria-modal="true" aria-label={title}><button className="modal-close icon-button" onClick={onClose} aria-label={closeLabel}><X size={18} /></button>{children}</section></div>;
}

function ApplicantDetails({ applicant, lang, t, onClose, onEdit, onDelete, onSave, onContact, onNotify }) {
  const [status, setStatus] = useState(applicant.status);
  const [updating, setUpdating] = useState(false);
  const updateStatus = async () => {
    if (status === applicant.status) return;
    setUpdating(true);
    const result = await onSave({ ...applicant, status, amount: Number(applicant.amount) }, applicant);
    setUpdating(false);
    if (result?.ok) onClose(); else if (result?.error) onNotify(result.error);
  };
  const copyPhone = async () => { try { await copyText(applicant.phone); onNotify(t('copied')); } catch { onNotify(t('validationGeneric')); } };
  const copyId = async () => { try { await copyText(applicant.id); onNotify(t('referenceCopied')); } catch { onNotify(t('validationGeneric')); } };
  return <ModalFrame title={t('applicantDetails')} onClose={onClose} closeLabel={t('close')} className="details-modal">
    <div className="details-topline"><span className="section-kicker">{t('applicantDetails')}</span><div className="details-quick-actions"><button className="icon-button" onClick={() => onEdit(applicant)} aria-label={t('editApplicant')} title={t('editApplicant')}><Pencil size={16} /></button><button className="icon-button danger-icon" onClick={() => onDelete(applicant)} aria-label={t('delete')} title={t('delete')}><Trash2 size={16} /></button></div></div>
    <div className="details-hero"><div className="avatar avatar-large">{applicant.name.trim().slice(0, 1).toUpperCase()}</div><div className="details-name"><h2>{applicant.name}</h2><div className="details-ref"><button onClick={copyId}>{applicant.id}<Clipboard size={13} /></button><span>·</span><span>{districtText(applicant.district, lang)}</span></div></div><span className={`status-badge status-${applicant.status.toLowerCase().replaceAll(' ', '-')}`}><i />{statusText(applicant.status, t)}</span></div>
    <div className="details-contact-row"><button className="button button-primary" onClick={() => onContact(applicant, 'whatsapp')}><MessageCircle size={16} />{t('whatsapp')}</button><button className="button button-secondary" onClick={() => onContact(applicant, 'call')}><Phone size={16} />{t('call')}</button><button className="button button-secondary" onClick={copyPhone}><Clipboard size={15} />{t('copyNumber')}</button></div>
    <div className="details-separator" />
    <div className="details-grid"><DetailCell label={t('contactNumber')} value={applicant.phone} /><DetailCell label={t('district')} value={districtText(applicant.district, lang)} /><DetailCell label={t('taluk')} value={applicant.taluk} /><DetailCell label={t('villageCity')} value={applicant.locality || '—'} /><DetailCell label={t('loanAmount')} value={formatCurrency(applicant.amount, lang)} prominent /><DetailCell label={t('purpose')} value={applicant.purpose} /><DetailCell label={t('applicationDate')} value={formatDate(applicant.applicationDate, lang)} /><DetailCell label={t('officer')} value={applicant.officer} /><DetailCell label={t('followUpDate')} value={applicant.followUpDate ? formatDate(applicant.followUpDate, lang) : '—'} /></div>
    <div className="remarks-block"><span className="detail-label">{t('remarks')}</span><p>{applicant.remarks || t('noRemarks')}</p></div>
    <div className="status-update-row"><label><span>{t('statusChanged')}</span><select value={status} onChange={(event) => setStatus(event.target.value)}>{STATUSES.map((item) => <option key={item} value={item}>{statusText(item, t)}</option>)}</select><ChevronDown size={14} /></label><button className="button button-primary" disabled={status === applicant.status || updating} onClick={updateStatus}>{t('saveChanges')} <ArrowRight size={15} /></button></div>
  </ModalFrame>;
}

function DetailCell({ label, value, prominent }) {
  return <div className={`detail-cell ${prominent ? 'detail-prominent' : ''}`}><span className="detail-label">{label}</span><b>{value || '—'}</b></div>;
}

function ConfirmDialog({ applicant, t, onCancel, onConfirm }) {
  return <ModalFrame title={t('deleteConfirm')} onClose={onCancel} closeLabel={t('close')} className="confirm-modal"><div className="confirm-icon"><Trash2 size={22} /></div><h2>{t('deleteConfirm')}</h2><p>{applicant.name} · {applicant.id}</p><p className="confirm-description">{t('deleteDescription')}</p><div className="modal-footer"><button className="button button-secondary" onClick={onCancel}>{t('cancel')}</button><button className="button button-danger" onClick={onConfirm}><Trash2 size={15} />{t('delete')}</button></div></ModalFrame>;
}

function ImportDialog({ applicants, lang, t, onClose, onImport }) {
  const [records, setRecords] = useState(null);
  const [fileName, setFileName] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const fileRef = useRef(null);
  const readFile = async (file) => {
    if (!file) return;
    setRecords(null); setError(''); setFileName(file.name);
    try {
      const parsed = parseCsv(await file.text());
      const issue = validateImport(parsed, new Set(applicants.map((person) => person.id)));
      if (issue) throw new Error(friendlyError(new Error(issue), t, lang));
      setRecords(parsed);
    } catch (err) { setError(friendlyError(err, t, lang)); }
  };
  const downloadTemplate = () => {
    const csv = buildCsv([]);
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([`\uFEFF${csv}\r\n`], { type: 'text/csv;charset=utf-8' }));
    link.download = 'kli-applicant-template.csv'; link.click(); URL.revokeObjectURL(link.href);
  };
  const submit = async () => {
    setBusy(true); setError('');
    try { await onImport(records); }
    catch (err) { setError(err.message); }
    setBusy(false);
  };
  return <ModalFrame title={t('importTitle')} onClose={onClose} closeLabel={t('close')} className="import-modal">
    <div className="import-heading"><div className="import-icon"><Upload size={20} /></div><div><span className="section-kicker">{t('csvRegister')}</span><h2>{t('importTitle')}</h2></div></div><p className="import-description">{t('importDescription')}</p>
    <input ref={fileRef} type="file" accept=".csv,text/csv" className="visually-hidden" onChange={(event) => readFile(event.target.files?.[0])} />
    <button className={`file-drop ${fileName ? 'file-selected' : ''}`} onClick={() => fileRef.current?.click()}><div className="file-drop-icon"><FileSpreadsheet size={19} /></div><b>{fileName || t('chooseFile')}</b><span>{fileName && records ? `${records.length} ${t('requests')}` : 'CSV · UTF-8'}</span></button>
    <button className="text-button template-button" onClick={downloadTemplate}><Download size={14} />{t('downloadTemplate')}</button>
    <div className="csv-help"><span>{t('csvHelp')}</span></div>
    {error && <div className="inline-error import-error"><CircleAlert size={15} />{error}</div>}
    {records && <div className="import-preview"><CheckCircle2 size={16} /><span><b>{records.length}</b> {t('requests')} {t('importReady')}</span></div>}
    <div className="modal-footer"><button className="button button-secondary" onClick={onClose}>{t('cancel')}</button><button className="button button-primary" disabled={!records?.length || busy} onClick={submit}>{busy ? t('signingIn') : t('importCount')} <ArrowRight size={15} /></button></div>
  </ModalFrame>;
}

function exportApplicants(applicants, notify, lang) {
  if (!applicants.length) { notify(lang === 'kn' ? 'ರಫ್ತು ಮಾಡಲು ಅರ್ಜಿದಾರರಿಲ್ಲ' : 'No applicants to export'); return; }
  const blob = new Blob([`\uFEFF${buildCsv(applicants)}`], { type: 'text/csv;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob); link.download = `kli-applicants-${todayIso()}.csv`; link.click(); URL.revokeObjectURL(link.href);
  notify(lang === 'kn' ? 'CSV ರಫ್ತು ಮಾಡಲಾಗಿದೆ' : 'CSV exported');
}

function SettingsPage({ lang, setLang, theme, setTheme, name, alerts, onSave, mode, t }) {
  const [draftName, setDraftName] = useState(name);
  const [draftAlerts, setDraftAlerts] = useState(alerts);
  useEffect(() => { setDraftName(name); setDraftAlerts(alerts); }, [name, alerts]);
  return <div className="settings-page"><div className="page-intro settings-intro"><div><div className="eyebrow page-eyebrow"><span className="green-dot" />{lang === 'kn' ? 'ನಿಮ್ಮ ಕಾರ್ಯಕ್ಷೇತ್ರ' : 'YOUR WORKSPACE'}</div><h1>{t('settings')}</h1><p>{lang === 'kn' ? 'ನಿಮ್ಮ ಅನುಭವ ಮತ್ತು ಆದ್ಯತೆಗಳನ್ನು ಹೊಂದಿಸಿ.' : 'Shape your experience and workspace preferences.'}</p></div><button className="button button-primary" onClick={() => onSave({ name: draftName, alerts: draftAlerts })}><Check size={16} />{t('saveSettings')}</button></div>
    <div className="settings-grid">
      <section className="settings-card"><div className="settings-card-heading"><div className="settings-card-icon"><Globe2 size={18} /></div><div><h3>{t('language')}</h3><p>{lang === 'kn' ? 'ಅಪ್ಲಿಕೇಶನ್ ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ.' : 'Choose your interface language.'}</p></div></div><div className="settings-options"><button className={`setting-option ${lang === 'en' ? 'option-selected' : ''}`} onClick={() => setLang('en')}><span className="radio-dot" /><span><b>English</b><small>English interface</small></span>{lang === 'en' && <Check size={16} />}</button><button className={`setting-option ${lang === 'kn' ? 'option-selected' : ''}`} onClick={() => setLang('kn')}><span className="radio-dot" /><span><b>ಕನ್ನಡ</b><small>Kannada interface</small></span>{lang === 'kn' && <Check size={16} />}</button></div></section>
      <section className="settings-card"><div className="settings-card-heading"><div className="settings-card-icon theme-setting-icon">{theme === 'dark' ? <Moon size={18} /> : <Sun size={18} />}</div><div><h3>{t('theme')}</h3><p>{lang === 'kn' ? 'ನಿಮ್ಮ ಕಾರ್ಯಕ್ಷೇತ್ರದ ಬೆಳಕು ಹೊಂದಿಸಿ.' : 'Choose a comfortable workspace appearance.'}</p></div></div><div className="theme-options"><button className={`theme-choice ${theme === 'dark' ? 'theme-choice-active' : ''}`} onClick={() => setTheme('dark')}><span className="theme-swatch swatch-dark"><i /></span><span>{t('dark')}</span>{theme === 'dark' && <Check size={15} />}</button><button className={`theme-choice ${theme === 'light' ? 'theme-choice-active' : ''}`} onClick={() => setTheme('light')}><span className="theme-swatch swatch-light"><i /></span><span>{t('light')}</span>{theme === 'light' && <Check size={15} />}</button></div></section>
      <section className="settings-card"><div className="settings-card-heading"><div className="settings-card-icon"><UserRound size={18} /></div><div><h3>{t('profile')}</h3><p>{lang === 'kn' ? 'ಈ ಸಾಧನದಲ್ಲಿನ ಪ್ರದರ್ಶನ ಹೆಸರು.' : 'Name shown in this local workspace.'}</p></div></div><label className="settings-field"><span>{t('profileName')}</span><input className="form-control" value={draftName} maxLength={80} onChange={(event) => setDraftName(event.target.value)} /></label></section>
      <section className="settings-card"><div className="settings-card-heading"><div className="settings-card-icon"><CalendarClock size={18} /></div><div><h3>{t('notificationPreferences')}</h3><p>{lang === 'kn' ? 'ನಿಮ್ಮ ಫಾಲೋ-ಅಪ್ ವೇಳಾಪಟ್ಟಿಯ ಜ್ಞಾಪನೆಗಳು.' : 'Reminders for your follow-up schedule.'}</p></div></div><label className="preference-toggle"><span><b>{t('followupAlerts')}</b><small>{lang === 'kn' ? 'ಡ್ಯಾಶ್‌ಬೋರ್ಡ್‌ನಲ್ಲಿ ಬಾಕಿ ದಿನಾಂಕಗಳನ್ನು ತೋರಿಸಿ.' : 'Show due items in the follow-up list.'}</small></span><input type="checkbox" checked={draftAlerts} onChange={(event) => setDraftAlerts(event.target.checked)} /><i /></label></section>
      <section className="settings-card settings-info-card"><div className="settings-card-heading"><div className="settings-card-icon"><ShieldCheck size={18} /></div><div><h3>{t('appInfo')}</h3><p>{lang === 'kn' ? 'ಈ ಅಪ್ಲಿಕೇಶನ್ ಕುರಿತು.' : 'About this application.'}</p></div></div><div className="info-lines"><div><span>{t('appVersion')}</span><b>1.0.0 <span className="beta-pill">LOCAL DEMO</span></b></div><div><span>{t('storageMode')}</span><b>{mode === 'sqlite' ? t('localDatabase') : t('browserStorage')}</b></div><div className="auth-info"><span>{t('demoAuthentication')}</span><p>{t('demoAuthDescription')}</p></div></div></section>
    </div>
  </div>;
}

function FollowupsPage({ applicants, lang, t, onOpen, onContact }) {
  const items = useMemo(() => applicants.filter((item) => item.followUpDate).sort((a, b) => a.followUpDate.localeCompare(b.followUpDate)), [applicants]);
  const today = todayIso();
  return <div className="followups-page"><div className="page-intro"><div><div className="eyebrow page-eyebrow"><span className="green-dot" />{lang === 'kn' ? 'ಮುಂದಿನ ಸಂಪರ್ಕ' : 'NEXT CONTACTS'}</div><h1>{t('followups')}</h1><p>{lang === 'kn' ? 'ಪ್ರತಿ ಅರ್ಜಿಗೆ ಸ್ಪಷ್ಟ ಮುಂದಿನ ಹಂತ.' : 'A clear next step for every application.'}</p></div><span className="followup-total"><CalendarClock size={16} />{items.length} {t('requests')}</span></div>
    {items.length ? <div className="followup-list">{items.map((person) => <article className="followup-card" key={person.id}><div className={`followup-date-block ${person.followUpDate < today ? 'due-overdue' : person.followUpDate === today ? 'due-today' : ''}`}><span>{formatDate(person.followUpDate, lang, { day: '2-digit' })}</span><small>{new Intl.DateTimeFormat(lang === 'kn' ? 'kn-IN' : 'en-IN', { month: 'short' }).format(new Date(`${person.followUpDate}T00:00:00`))}</small></div><button className="followup-person" onClick={() => onOpen(person)}><span className="avatar avatar-person">{person.name.trim().slice(0, 1).toUpperCase()}</span><span><b>{person.name}</b><small>{person.id} · {districtText(person.district, lang)}</small></span></button><span className={`due-label ${person.followUpDate < today ? 'due-label-overdue' : person.followUpDate === today ? 'due-label-today' : ''}`}>{person.followUpDate < today ? t('overdue') : person.followUpDate === today ? t('today') : t('upcoming')}</span><div className="followup-amount"><small>{t('requested')}</small><b>{formatCurrency(person.amount, lang)}</b></div><button className="icon-button followup-contact" title={t('callApplicant')} onClick={() => onContact(person, 'call')}><Phone size={16} /></button><button className="icon-button followup-open" title={t('applicantDetails')} onClick={() => onOpen(person)}><ArrowUpRight size={16} /></button></article>)}</div> : <div className="empty-state followup-empty"><div className="empty-icon"><CalendarClock size={23} /></div><h2>{t('noFollowups')}</h2><p>{t('noFollowupsDescription')}</p></div>}
  </div>;
}

export default App;
