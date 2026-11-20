// apps/demo-panel.js
// Shared Interactive Demo Section Component for all CONDUIT Ecosystem elements

function DemoSection({ onUpdate, currentOppId = 'OPP-9001' }) {
  const [isOpen, setIsOpen] = React.useState(false);
  const [activeStep, setActiveStep] = React.useState(1);
  const [audit, setAudit] = React.useState(null);
  const [loading, setLoading] = React.useState(false);

  const steps = [
    { num: 1, title: '1. Baseline Deficit', desc: 'Idle capacity detected; unverified credit hold.' },
    { num: 2, title: '2. Buyer Escrow', desc: 'Buyer confirms ₹375k PO & locks 35% proof.' },
    { num: 3, title: '3. Supplier Stock', desc: 'Fabric supplier allocates ₹160k stock @ ₹320.' },
    { num: 4, title: '4. Full Unlock', desc: '100% Quad consensus reached. Auto-approved.' }
  ];

  const runStep = (stepNum) => {
    setLoading(true);
    fetch(`http://127.0.0.1:8088/api/demo/execute-step?step=${stepNum}&opportunity_id=${currentOppId}`)
      .then(r => r.json())
      .then(res => {
        setLoading(false);
        setActiveStep(stepNum);
        setAudit(res.audit);
        if (onUpdate) onUpdate(res);
      })
      .catch(err => {
        setLoading(false);
        console.error(err);
      });
  };

  React.useEffect(() => {
    fetch(`http://127.0.0.1:8088/api/score?id=${currentOppId}`)
      .then(r => r.json())
      .then(res => {
        if (res.data) {
          const s = res.data.score;
          if (s >= 800) setActiveStep(4);
          else if (s >= 700) setActiveStep(3);
          else if (s >= 600) setActiveStep(2);
          else setActiveStep(1);
        }
      })
      .catch(() => {});
  }, [currentOppId]);

  return (
    <div style={{
      position: 'fixed',
      bottom: '20px',
      right: '20px',
      zIndex: 9999,
      fontFamily: "'JetBrains Mono', monospace"
    }}>
      {!isOpen ? (
        <button
          onClick={() => setIsOpen(true)}
          style={{
            background: '#11141c',
            color: '#fff',
            border: '1px solid rgba(255,255,255,0.3)',
            padding: '0.65rem 1.1rem',
            borderRadius: '4px',
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 4px 20px rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#38bdf8' }}></span>
          Demo Walkthrough
        </button>
      ) : (
        <div style={{
          width: '380px',
          background: '#0d1017',
          border: '1px solid rgba(255,255,255,0.2)',
          borderRadius: '6px',
          boxShadow: '0 10px 40px rgba(0,0,0,0.8)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column'
        }}>
          {/* Header */}
          <div style={{
            background: '#131824',
            padding: '0.75rem 1rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid rgba(255,255,255,0.1)'
          }}>
            <div>
              <div style={{ fontSize: '12px', fontWeight: 700, color: '#fff' }}>Interactive Demo Simulation</div>
              <div style={{ fontSize: '10px', color: '#94a3b8' }}>Live SQLite Mutation &amp; Credit Recompute</div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94a3b8',
                fontSize: '16px',
                cursor: 'pointer'
              }}
            >
              &times;
            </button>
          </div>

          {/* Stepper Buttons */}
          <div style={{ padding: '0.8rem', display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.4rem', background: '#0a0d14' }}>
            {steps.map(s => (
              <button
                key={s.num}
                onClick={() => runStep(s.num)}
                disabled={loading}
                style={{
                  background: activeStep === s.num ? '#1e293b' : '#11141c',
                  color: activeStep === s.num ? '#fff' : '#64748b',
                  border: activeStep === s.num ? '1px solid #38bdf8' : '1px solid rgba(255,255,255,0.06)',
                  padding: '0.45rem 0.2rem',
                  borderRadius: '3px',
                  fontSize: '10px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  textAlign: 'center'
                }}
              >
                Step {s.num}
              </button>
            ))}
          </div>

          {/* Detailed Audit & Formulation Explanation */}
          <div style={{ padding: '1rem', maxHeight: '340px', overflowY: 'auto', fontSize: '11px', color: '#cbd5e1', lineHeight: '1.5' }}>
            <div style={{ fontWeight: 700, color: '#fff', fontSize: '12px', marginBottom: '0.4rem' }}>
              {steps[activeStep - 1].title}
            </div>

            {audit ? (
              <div>
                <div style={{ background: '#111622', padding: '0.6rem', borderRadius: '3px', marginBottom: '0.6rem', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ color: '#94a3b8', fontSize: '10px', textTransform: 'uppercase' }}>Computed Parameters</div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', margin: '0.2rem 0' }}>
                    <span>Consensus Ratio:</span>
                    <strong style={{ color: '#fff' }}>{(audit.consensus_ratio * 100).toFixed(0)}%</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', margin: '0.2rem 0' }}>
                    <span>Live Credit Score:</span>
                    <strong style={{ color: '#fff' }}>{audit.score} / 900 ({audit.tier})</strong>
                  </div>
                </div>

                <div style={{ color: '#94a3b8', marginBottom: '0.6rem' }}>
                  <strong style={{ color: '#cbd5e1' }}>Deterministic Action:</strong><br />
                  {audit.explanation}
                </div>

                <div style={{ background: '#151922', padding: '0.6rem', borderRadius: '3px', borderLeft: '2px solid #38bdf8' }}>
                  <div style={{ fontSize: '10px', color: '#94a3b8', textTransform: 'uppercase' }}>Lender Verdict</div>
                  <div style={{ fontWeight: 600, color: '#fff', marginTop: '0.1rem' }}>{audit.lender_verdict}</div>
                </div>
              </div>
            ) : (
              <div>
                <p style={{ color: '#94a3b8', marginBottom: '0.8rem' }}>{steps[activeStep - 1].desc}</p>
                <button
                  onClick={() => runStep(activeStep)}
                  disabled={loading}
                  style={{
                    width: '100%',
                    background: '#f8fafc',
                    color: '#090a0d',
                    border: 'none',
                    padding: '0.5rem',
                    borderRadius: '2px',
                    fontWeight: 700,
                    fontSize: '11px',
                    cursor: 'pointer'
                  }}
                >
                  {loading ? 'Computing...' : `Execute Step ${activeStep}`}
                </button>
              </div>
            )}
          </div>

          {/* Quick Footer Links to Other Elements */}
          <div style={{
            background: '#0a0d14',
            padding: '0.6rem 1rem',
            borderTop: '1px solid rgba(255,255,255,0.06)',
            fontSize: '10px',
            color: '#64748b',
            display: 'flex',
            justifyContent: 'space-between'
          }}>
            <span>Target: {currentOppId}</span>
            <span style={{ color: '#38bdf8' }}>SQLite Verified</span>
          </div>
        </div>
      )}
    </div>
  );
}
