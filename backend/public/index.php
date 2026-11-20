<?php
// backend/public/index.php

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/VerificationEngine.php';
require_once __DIR__ . '/../src/OpportunityService.php';
require_once __DIR__ . '/../src/GapEngineService.php';
require_once __DIR__ . '/../src/UnmetGraphService.php';
require_once __DIR__ . '/../src/CreditScoreEngine.php';

use Conduit\Database;
use Conduit\VerificationEngine;
use Conduit\OpportunityService;
use Conduit\GapEngineService;
use Conduit\UnmetGraphService;
use Conduit\CreditScoreEngine;

$db = Database::getConnection();
$verificationEngine = new VerificationEngine($db);
$opportunityService = new OpportunityService($db);
$gapEngineService = new GapEngineService($db);
$unmetGraphService = new UnmetGraphService($db);
$creditScoreEngine = new CreditScoreEngine($db);

$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'];
$input = json_decode(file_get_contents('php://input'), true) ?? [];

// If browser visits root directly, serve clean interactive console
if ($uri === '/' && (!isset($_SERVER['HTTP_ACCEPT']) || str_contains($_SERVER['HTTP_ACCEPT'], 'text/html'))) {
    header("Content-Type: text/html; charset=UTF-8");
    ?>
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <title>CONDUIT // Backend Core</title>
      <link rel="preconnect" href="https://fonts.googleapis.com">
      <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
      <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body { background: #08090c; color: #f1f3f7; font-family: 'JetBrains Mono', monospace; font-size: 13px; padding: 2.5rem; line-height: 1.6; }
        a { color: #00e599; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .head { border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 1rem; margin-bottom: 2rem; display: flex; justify-content: space-between; align-items: baseline; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; margin-top: 1.5rem; }
        .card { background: #0e1219; border: 1px solid rgba(255,255,255,0.08); padding: 1.2rem; border-radius: 4px; }
        .status-dot { display: inline-block; width: 7px; height: 7px; background: #00e599; border-radius: 50%; margin-right: 6px; }
        .row { display: flex; justify-content: space-between; padding: 0.35rem 0; border-bottom: 1px solid rgba(255,255,255,0.04); font-size: 12px; }
        .val { color: #fff; font-weight: 600; }
        .tag { font-size: 11px; color: #717d91; }
      </style>
    </head>
    <body>
      <div class="head">
        <div><span class="status-dot"></span><strong>CONDUIT // ENGINE CORE</strong> [PHP 8.3 + SQLite3 PDO]</div>
        <div style="color: #717d91;">PORT: 8088 &bull; ONLINE</div>
      </div>

      <div style="background: #0e1219; border: 1px solid rgba(255,255,255,0.08); padding: 1rem 1.4rem; border-radius: 4px; margin-bottom: 2rem; display: flex; justify-content: space-between;">
        <div>SYSTEM: <span class="val">Demand-to-Capacity Gap Intelligence</span></div>
        <div>DRIVER: <span class="val">SQLite3 (conduit.sqlite)</span></div>
        <div>TIMESTAMP: <span class="val"><?php echo date('c'); ?></span></div>
      </div>

      <div style="color: #717d91; font-size: 11px; margin-bottom: 0.5rem;">ACTIVE ECOSYSTEM ELEMENTS &bull; DIRECT REACT APPS</div>
      <div class="grid">
        <div class="card">
          <div class="tag">ELEMENT 01 // INTERFACE</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/msme-app/index.html">MSME Mobile Application &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Capacity &amp; shift profiling</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 02 // CONSENSUS</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/demand-verification/index.html">Demand Verification &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Multi-party cryptographic proof engine</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 03 // DISCOVERY</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/demand-detector/index.html">Unfulfilled Demand Detector &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Reverse borrower discovery algorithm</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 04 // SIMULATION</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/capital-simulator/index.html">Capital Unlock Simulator &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Mathematical bottleneck ROI analysis</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 05 // COMPILATION</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/loan-designer/index.html">Loan Structure Compiler &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Closed-loop supplier disbursement contracts</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 06 // ENGINE</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/gap-engine/index.html">Gap Intelligence Engine &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Cross-enterprise triad &amp; health diagnostics</div>
        </div>

        <div class="card">
          <div class="tag">ELEMENT 07 // GRAPH</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/apps/unmet-graph/index.html">Unmet-Economy Graph &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">Topological network transaction visualizer</div>
        </div>

        <div class="card">
          <div class="tag">MAIN EXPERIENCE</div>
          <div style="font-weight: 600; color: #fff; margin: 0.3rem 0;"><a href="/index.html">Scroll Kinetic Experience &rarr;</a></div>
          <div style="color: #94a3b8; font-size: 12px;">30-frame sequence &amp; narrative</div>
        </div>
      </div>
    </body>
    </html>
    <?php
    exit;
}

// REST API JSON headers for API calls
header("Content-Type: application/json; charset=UTF-8");

try {
    // Healthcheck
    if ($uri === '/' || $uri === '/api/health') {
        echo json_encode([
            'status' => 'ONLINE',
            'system' => 'CONDUIT Demand-to-Capacity Gap Intelligence Engine',
            'version' => '1.0.0',
            'db_driver' => 'SQLite3 via PDO',
            'timestamp' => date('c')
        ]);
        exit;
    }

    // Opportunities List
    if ($uri === '/api/opportunities' && $method === 'GET') {
        $stmt = $db->query("
            SELECT o.*, 
                   m.business_name AS msme_name, m.industry, m.location,
                   d.item_description, d.units_demanded, d.unit_target_price,
                   b.name AS buyer_name,
                   s.name AS supplier_name, s.unit_price as supplier_price
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN buyers b ON d.buyer_id = b.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            ORDER BY o.created_at DESC
        ");
        echo json_encode(['opportunities' => $stmt->fetchAll()]);
        exit;
    }

    // Verification Dossier
    if ($uri === '/api/verification/dossier' && $method === 'GET') {
        $oppId = $_GET['id'] ?? 'OPP-9001';
        echo json_encode($verificationEngine->getVerificationDossier($oppId));
        exit;
    }

    // Submit Verification Proof
    if ($uri === '/api/verification/submit' && $method === 'POST') {
        $oppId = $input['opportunity_id'] ?? null;
        $party = $input['participant_type'] ?? null;
        $partyId = $input['participant_id'] ?? 'SYS-AUTO';
        $params = $input['parameters'] ?? [];

        if (!$oppId || !$party) {
            http_response_code(400);
            echo json_encode(['error' => 'Missing opportunity_id or participant_type']);
            exit;
        }

        $res = $verificationEngine->submitProof($oppId, $party, $partyId, $params);
        echo json_encode(['success' => true, 'data' => $res]);
        exit;
    }

    // Demand Detector Feed & Run
    if ($uri === '/api/demand-detector/feed' && $method === 'GET') {
        echo json_encode(['success' => true, 'feed' => $opportunityService->getDetectorFeed()]);
        exit;
    }

    if ($uri === '/api/demand-detector/run' && $method === 'POST') {
        $detected = $opportunityService->detectOpportunities();
        $feed = $opportunityService->getDetectorFeed();
        echo json_encode([
            'success' => true,
            'newly_detected_count' => count($detected),
            'new_opportunities' => $detected,
            'stats' => $feed['stats']
        ]);
        exit;
    }

    // Capital Simulator
    if ($uri === '/api/simulator' && ($method === 'GET' || $method === 'POST')) {
        $oppId = $_GET['opportunity_id'] ?? ($input['opportunity_id'] ?? 'OPP-9001');
        $customParams = ($method === 'POST') ? $input['custom_parameters'] ?? null : null;
        echo json_encode(['success' => true, 'data' => $opportunityService->simulateUnlock($oppId, $customParams)]);
        exit;
    }

    // Loan Compiler
    if ($uri === '/api/loan-designer/compile' && $method === 'POST') {
        $oppId = $input['opportunity_id'] ?? null;
        $customTerms = $input['custom_terms'] ?? null;
        if (!$oppId) {
            http_response_code(400);
            echo json_encode(['error' => 'Missing opportunity_id']);
            exit;
        }

        echo json_encode(['success' => true, 'contract' => $opportunityService->compileLoanContract($oppId, $customTerms)]);
        exit;
    }

    if ($uri === '/api/loan-designer/contract' && $method === 'GET') {
        $oppId = $_GET['opportunity_id'] ?? 'OPP-9001';
        echo json_encode(['success' => true, 'contract' => $opportunityService->getLoanContract($oppId)]);
        exit;
    }

    // Gap Engine Analysis
    if ($uri === '/api/gap-engine/analyze' && $method === 'GET') {
        echo json_encode(['success' => true, 'data' => $gapEngineService->analyzeEconomicRelationships()]);
        exit;
    }

    // Graph Topology
    if ($uri === '/api/graph' && $method === 'GET') {
        echo json_encode(['success' => true, 'data' => $unmetGraphService->getGraphTopology()]);
        exit;
    }

    // Opportunity Credit Score & Methodology
    if ($uri === '/api/score' && $method === 'GET') {
        $oppId = $_GET['id'] ?? 'OPP-9001';
        echo json_encode(['success' => true, 'data' => $creditScoreEngine->computeScore($oppId)]);
        exit;
    }

    // Create New Demand Order (Real Data Input)
    if ($uri === '/api/demand-orders/create' && $method === 'POST') {
        $buyerId = $input['buyer_id'] ?? null;
        $itemDesc = $input['item_description'] ?? null;
        $units = (int)($input['units_demanded'] ?? 0);
        $targetPrice = (float)($input['unit_target_price'] ?? 0);
        $deadline = $input['delivery_deadline'] ?? date('Y-m-d', strtotime('+30 days'));

        if (!$buyerId || !$itemDesc || $units <= 0 || $targetPrice <= 0) {
            http_response_code(400);
            echo json_encode(['error' => 'Missing or invalid fields (buyer_id, item_description, units_demanded, unit_target_price required)']);
            exit;
        }

        $orderId = 'ORD-' . strtoupper(bin2hex(random_bytes(3)));
        $stmt = $db->prepare("
            INSERT INTO demand_orders (id, buyer_id, item_description, units_demanded, unit_target_price, delivery_deadline, buyer_verified)
            VALUES (?, ?, ?, ?, ?, ?, 1)
        ");
        $stmt->execute([$orderId, $buyerId, $itemDesc, $units, $targetPrice, $deadline]);

        // Auto-run detector
        $opportunityService->detectOpportunities();

        echo json_encode([
            'success' => true,
            'order_id' => $orderId,
            'message' => 'Order created and demand detector triggered'
        ]);
        exit;
    }

    // Create/Update MSME Capacity (Real Data Input)
    if ($uri === '/api/msmes/create' && $method === 'POST') {
        $name = $input['business_name'] ?? null;
        $industry = $input['industry'] ?? null;
        $location = $input['location'] ?? null;
        $maxCap = (int)($input['max_monthly_capacity'] ?? 0);
        $currProd = (int)($input['current_production'] ?? 0);
        $bottleneck = $input['capital_bottleneck'] ?? 'RAW_MATERIALS';

        if (!$name || !$industry || $maxCap <= 0) {
            http_response_code(400);
            echo json_encode(['error' => 'Missing required fields (business_name, industry, max_monthly_capacity)']);
            exit;
        }

        $msmeId = 'MSME-' . strtoupper(bin2hex(random_bytes(3)));
        $stmt = $db->prepare("
            INSERT INTO msmes (id, business_name, industry, location, max_monthly_capacity, current_production, capital_bottleneck)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([$msmeId, $name, $industry, $location, $maxCap, $currProd, $bottleneck]);

        $opportunityService->detectOpportunities();

        echo json_encode([
            'success' => true,
            'msme_id' => $msmeId,
            'message' => 'MSME registered and capacity matched'
        ]);
        exit;
    }

    // Reset/Clear Verification Proofs (For real interactive testing)
    if ($uri === '/api/verification/reset' && $method === 'POST') {
        $oppId = $input['opportunity_id'] ?? 'OPP-9001';
        $db->prepare("DELETE FROM verification_proofs WHERE opportunity_id = ?")->execute([$oppId]);
        $db->prepare("UPDATE opportunities SET consensus_score = 0.0, status = 'DETECTED' WHERE id = ?")->execute([$oppId]);
        $creditScoreEngine->computeScore($oppId);
        echo json_encode(['success' => true, 'message' => "Proofs reset for {$oppId}"]);
        exit;
    }

    // Interactive Demo Simulation Engine: Step-by-Step execution
    if ($uri === '/api/demo/execute-step' && ($method === 'POST' || $method === 'GET')) {
        $step = (int)($_GET['step'] ?? ($input['step'] ?? 1));
        $oppId = $_GET['opportunity_id'] ?? ($input['opportunity_id'] ?? 'OPP-9001');

        $auditLog = [];

        if ($step == 1) {
            // Step 1: Baseline Unverified State
            $db->prepare("DELETE FROM verification_proofs WHERE opportunity_id = ?")->execute([$oppId]);
            $db->prepare("UPDATE opportunities SET consensus_score = 0.0, status = 'DETECTED' WHERE id = ?")->execute([$oppId]);
            $scoreRes = $creditScoreEngine->computeScore($oppId);
            $auditLog = [
                'action' => 'INITIAL_DEFICIT_DISCOVERY',
                'explanation' => 'MSME has 550 idle units headroom but lacks working capital (₹160,000) for supplier fabric. Institutional buyer has ₹375,000 order waiting. No proofs are attested yet.',
                'consensus_ratio' => 0.0,
                'score' => $scoreRes['score'],
                'tier' => $scoreRes['tier'],
                'lender_verdict' => 'HOLD // Unverified counterparties and absent escrow pledge'
            ];
        } elseif ($step == 2) {
            // Step 2: Buyer commits demand & escrow
            $verificationEngine->submitProof($oppId, 'BUYER', 'BUYER-01', [
                'confirmed_units' => 500,
                'payment_escrow_ready' => true,
                'delivery_inspection_window_days' => 7
            ]);
            $scoreRes = $creditScoreEngine->computeScore($oppId);
            $auditLog = [
                'action' => 'BUYER_ESCROW_ATTESTED',
                'explanation' => 'Institutional buyer locks purchase order in smart escrow. Consensus increases by +35%. Credit score rises by +63 points as off-take default risk drops to near zero.',
                'consensus_ratio' => 0.35,
                'score' => $scoreRes['score'],
                'tier' => $scoreRes['tier'],
                'lender_verdict' => 'MONITORED // Buyer verified; waiting on supplier fabric stock guarantee'
            ];
        } elseif ($step == 3) {
            // Step 3: Supplier allocates stock & locks cost
            $verificationEngine->submitProof($oppId, 'SUPPLIER', 'SUP-301', [
                'allocated_stock_units' => 500,
                'confirmed_unit_cost' => 320,
                'ready_to_dispatch_days' => 3
            ]);
            $scoreRes = $creditScoreEngine->computeScore($oppId);
            $auditLog = [
                'action' => 'SUPPLIER_STOCK_RESERVED',
                'explanation' => 'Supplier locks ₹320/unit raw material allocation. Eliminates inflation/shortage risk. Consensus hits 65%. Credit score reaches Investment Grade (700+).',
                'consensus_ratio' => 0.65,
                'score' => $scoreRes['score'],
                'tier' => $scoreRes['tier'],
                'lender_verdict' => 'FAST_TRACK // 80% LTV approved for raw-material disbursement'
            ];
        } elseif ($step == 4) {
            // Step 4: MSME pledges machinery shift hours + Logistics corridor locks GPS
            $verificationEngine->submitProof($oppId, 'MSME', 'MSME-101', [
                'pledged_shift_hours' => 180,
                'active_operators' => 14,
                'inspection_ready_date' => date('Y-m-d', strtotime('+20 days'))
            ]);
            $verificationEngine->submitProof($oppId, 'LOGISTICS', 'LOG-TN-04', [
                'corridor_delivery_rating' => 98,
                'vehicle_assigned' => true,
                'transit_hours_estimate' => 18
            ]);
            $scoreRes = $creditScoreEngine->computeScore($oppId);
            $contract = $opportunityService->compileLoanContract($oppId);
            $auditLog = [
                'action' => 'FULL_QUAD_CONSENSUS_AND_DISBURSEMENT',
                'explanation' => '100% Quad consensus reached. MSME commits 180 hours, logistics locks corridor transit. Credit score hits 900 (TIER_1_PRIME). Closed-loop tri-party loan contract compiled automatically to disburse directly to supplier.',
                'consensus_ratio' => 1.0,
                'score' => $scoreRes['score'],
                'tier' => $scoreRes['tier'],
                'lender_verdict' => 'AUTO_APPROVE // Disburse ₹160,000 directly to Supplier Escrow'
            ];
        }

        echo json_encode([
            'success' => true,
            'step' => (int)$step,
            'audit' => $auditLog,
            'dossier' => $verificationEngine->getVerificationDossier($oppId)
        ]);
        exit;
    }

    http_response_code(404);
    echo json_encode(['error' => 'Endpoint not found', 'path' => $uri]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
