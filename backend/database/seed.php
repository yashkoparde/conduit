<?php
// backend/database/seed.php

require_once __DIR__ . '/../src/Database.php';

use Conduit\Database;

$db = Database::getConnection();

echo "Initializing schema and seeding realistic rich MSME ecosystem data...\n";

// Clear existing tables
$db->exec("DELETE FROM verification_proofs;");
$db->exec("DELETE FROM loan_contracts;");
$db->exec("DELETE FROM opportunities;");
$db->exec("DELETE FROM demand_orders;");
$db->exec("DELETE FROM suppliers;");
$db->exec("DELETE FROM buyers;");
$db->exec("DELETE FROM msmes;");

// 1. Seed Diverse MSMEs across sectors
$insertMsme = $db->prepare("INSERT INTO msmes (id, business_name, industry, location, max_monthly_capacity, current_production, capital_bottleneck) VALUES (?, ?, ?, ?, ?, ?, ?)");

$msmes = [
    ['MSME-101', 'Aarav Textile Craft', 'Apparel & Garments', 'Tirupur, TN', 800, 250, 'Raw Material Fabric Liquidity'],
    ['MSME-102', 'Precision Precision Techworks', 'Light Engineering & Auto Components', 'Coimbatore, TN', 1200, 400, 'Tooling & High-Grade Steel Inventory'],
    ['MSME-103', 'Sunrise Agro Packagers', 'Food Processing & Packaging', 'Nashik, MH', 5000, 1800, 'Cold-chain Bulk Container Working Capital'],
    ['MSME-104', 'Kaveri Precision Looms', 'Apparel & Garments', 'Erode, TN', 1500, 600, 'Yarn Working Capital & Dye Inventory'],
    ['MSME-105', 'Sahyadri Auto Castings', 'Light Engineering & Auto Components', 'Pune, MH', 2200, 850, 'Alloy Raw Ingot Cash Constraints'],
    ['MSME-106', 'Malabar Organic Processing', 'Food Processing & Packaging', 'Kozhikode, KL', 3800, 1400, 'Aseptic Pouch Packaging Pre-Financing']
];

foreach ($msmes as $m) {
    $insertMsme->execute($m);
}

// 2. Seed Verified Buyers (Public Schools, Hospitals, Retail Chains, Auto Tier-1s, FMCG)
$insertBuyer = $db->prepare("INSERT INTO buyers (id, name, organization_type, contact_email) VALUES (?, ?, ?, ?)");

$buyers = [
    ['BUY-201', 'Apex Regional Public Schools Council', 'Educational Institution', 'procurement@apexschools.org'],
    ['BUY-202', 'Metro Health & Clinic Consortium', 'Healthcare Provider', 'supplies@metrohealth.co.in'],
    ['BUY-203', 'Evergreen Retail Stores Pvt Ltd', 'Commercial Supermarket Chain', 'orders@evergreenretail.com'],
    ['BUY-204', 'Bharat Heavy Mobility Tier-1', 'Automotive OEM Tier-1', 'vendor-desk@bharatmobility.com'],
    ['BUY-205', 'Heritage Spice & Organics Exports', 'FMCG Exporter', 'sourcing@heritagespice.in']
];

foreach ($buyers as $b) {
    $insertBuyer->execute($b);
}

// 3. Seed Certified Raw Material Suppliers
$insertSupplier = $db->prepare("INSERT INTO suppliers (id, name, material_type, unit_price, available_stock) VALUES (?, ?, ?, ?, ?)");

$suppliers = [
    ['SUP-301', 'Coimbatore Cotton Spinners Ltd', 'Grade-A Poly-Cotton Fabric', 320.00, 12000],
    ['SUP-302', 'Salem Alloy & Steel Traders', 'EN-8 Precision Steel Rods', 680.00, 4500],
    ['SUP-303', 'BioPolymer Packing Materials', 'Food Grade Eco-Film Bags', 45.00, 80000],
    ['SUP-304', 'Deccan Spun Thread & Twill Corp', 'Combed Cotton Yarn 40s', 210.00, 25000],
    ['SUP-305', 'Maharashtra Aluminum & Alloys Ltd', 'Die-Cast ADC12 Ingots', 540.00, 18000]
];

foreach ($suppliers as $s) {
    $insertSupplier->execute($s);
}

// 4. Seed Verified Real Market Demand Orders
$insertOrder = $db->prepare("INSERT INTO demand_orders (id, buyer_id, item_description, units_demanded, unit_target_price, delivery_deadline, buyer_verified) VALUES (?, ?, ?, ?, ?, ?, ?)");

$orders = [
    ['ORD-501', 'BUY-201', 'Heavy Duty Standardized School Uniform Sets', 500, 750.00, '2026-12-15', 1],
    ['ORD-502', 'BUY-202', 'Surgical Cotton Scrubs & Lab Coats', 350, 680.00, '2026-12-28', 1],
    ['ORD-503', 'BUY-203', 'Precision Brass Valve Bushings', 800, 1100.00, '2027-01-10', 1],
    ['ORD-504', 'BUY-204', 'High-Tensile Aluminum Wheel Flanges', 650, 1450.00, '2027-01-20', 1],
    ['ORD-505', 'BUY-205', 'Export Grade Spices Vacuum Packs', 1200, 185.00, '2026-12-20', 1]
];

foreach ($orders as $o) {
    $insertOrder->execute($o);
}

// 5. Seed Realistic Opportunities & Triads
$insertOpp = $db->prepare("INSERT INTO opportunities (id, demand_order_id, msme_id, supplier_id, demanded_units, msme_capacity_gap, estimated_financing_required, status, consensus_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

$opps = [
    // OPP-9001: Prime Demo Triad (Financed / Auto-Approved 100% consensus)
    ['OPP-9001', 'ORD-501', 'MSME-101', 'SUP-301', 500, 300, 160000.00, 'FINANCED', 1.0],
    // OPP-9002: Medical Apparel Order for Aarav Textile
    ['OPP-9002', 'ORD-502', 'MSME-101', 'SUP-301', 350, 150, 112000.00, 'DETECTED', 0.0],
    // OPP-9003: Valve Bushings for Precision Techworks
    ['OPP-9003', 'ORD-503', 'MSME-102', 'SUP-302', 800, 800, 544000.00, 'DETECTED', 0.0],
    // OPP-9004: Auto wheel flanges for Sahyadri Auto Castings
    ['OPP-9004', 'ORD-504', 'MSME-105', 'SUP-305', 650, 650, 351000.00, 'DETECTED', 0.0],
    // OPP-9005: Vacuum spice packs for Sunrise Agro Packagers
    ['OPP-9005', 'ORD-505', 'MSME-103', 'SUP-303', 1200, 1200, 54000.00, 'DETECTED', 0.0]
];

foreach ($opps as $opp) {
    $insertOpp->execute($opp);
}

// 6. Seed Proofs for OPP-9001 (100% verified state)
$insertProof = $db->prepare("INSERT INTO verification_proofs (id, opportunity_id, participant_type, participant_id, proof_type, parameters_json, digital_signature, verified_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

$proofs = [
    ['PRF-846A4183', 'OPP-9001', 'SUPPLIER', 'SUP-301', 'INVENTORY_ALLOCATION', json_encode(['inventory_locked_units' => 500, 'unit_cost_inr' => 320, 'total_material_value_inr' => 160000]), '93b8fee21b3b39fbf28b255a2825bf36990f6948449df319cc1dcc663a232cd3', '2026-11-21 03:32:16'],
    ['PRF-2CA69A81', 'OPP-9001', 'MSME', 'MSME-101', 'CAPACITY_PLEDGE', json_encode(['pledged_shift_hours' => 180, 'active_operators' => 14, 'idle_capacity_headroom' => 550]), '659dd10dea8dcdd229987b08042652d002b9c6325d65f26a1d5a6d5cdfd0a6ca', '2026-11-21 03:32:21'],
    ['PRF-F2604C3A', 'OPP-9001', 'LOGISTICS', 'LOG-TN-04', 'ROUTE_AFFIDAVIT', json_encode(['corridor_delivery_rating' => 98, 'vehicle_assigned' => true]), '9c2a96f6d5c7a5971950409c8aa5d30fc7c18d8b5435f8b490ad73d846f8f764', '2026-11-21 03:32:21'],
    ['PRF-7495DC90', 'OPP-9001', 'BUYER', 'BUY-201', 'DEMAND_CONFIRMATION', json_encode(['confirmed_units' => 500, 'payment_escrow_ready' => true]), '53161e9e5c7aa1ca91d2198bf92367e38ed4f27cc17592d402e03ba6734b76de', '2026-11-21 03:49:39']
];

foreach ($proofs as $p) {
    $insertProof->execute($p);
}

// 7. Seed Compiled Loan Contract for OPP-9001
$insertContract = $db->prepare("INSERT INTO loan_contracts (id, opportunity_id, msme_id, supplier_id, approved_amount, disbursement_type, interest_rate, tenure_months, status, repayment_terms) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
$insertContract->execute([
    'LOAN-1CCE6355',
    'OPP-9001',
    'MSME-101',
    'SUP-301',
    160000.00,
    'CLOSED_LOOP_SUPPLIER_ESCROW',
    11.25,
    3,
    'COMPILED',
    json_encode([
        'principal' => 160000.00,
        'interest' => 4500.00,
        'total' => 164500.00,
        'due' => '2026-12-15'
    ])
]);

// 8. Recompute Credit Scores for all Opportunities
require_once __DIR__ . '/../src/CreditScoreEngine.php';
$scorer = new Conduit\CreditScoreEngine($db);
foreach (['OPP-9001', 'OPP-9002', 'OPP-9003', 'OPP-9004', 'OPP-9005'] as $oppId) {
    $scorer->computeScore($oppId);
}

echo "Database seeded successfully with rich demo mesh at backend/database/conduit.sqlite!\n";
