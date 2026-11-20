<?php
// backend/database/seed.php

require_once __DIR__ . '/../src/Database.php';

use Conduit\Database;

$db = Database::getConnection();

echo "Initializing schema and seeding realistic MSME ecosystem data...\n";

// Clear existing tables
$db->exec("DELETE FROM verification_proofs;");
$db->exec("DELETE FROM loan_contracts;");
$db->exec("DELETE FROM opportunities;");
$db->exec("DELETE FROM demand_orders;");
$db->exec("DELETE FROM suppliers;");
$db->exec("DELETE FROM buyers;");
$db->exec("DELETE FROM msmes;");

// 1. Seed MSMEs (Small businesses with high equipment capacity but credit/working-capital constraints)
$insertMsme = $db->prepare("INSERT INTO msmes (id, business_name, industry, location, max_monthly_capacity, current_production, capital_bottleneck) VALUES (?, ?, ?, ?, ?, ?, ?)");

$msmes = [
    ['MSME-101', 'Aarav Textile Craft', 'Apparel & Garments', 'Tirupur, TN', 800, 250, 'Raw Material Fabric Liquidity'],
    ['MSME-102', 'Precision Precision Techworks', 'Light Engineering & Auto Components', 'Coimbatore, TN', 1200, 400, 'Tooling & High-Grade Steel Inventory'],
    ['MSME-103', 'Sunrise Agro Packagers', 'Food Processing & Packaging', 'Nashik, MH', 5000, 1800, 'Cold-chain Bulk Container Working Capital']
];

foreach ($msmes as $m) {
    $insertMsme->execute($m);
}

// 2. Seed Verified Buyers (Schools, Hospitals, Municipalities, Retailers)
$insertBuyer = $db->prepare("INSERT INTO buyers (id, name, organization_type, contact_email) VALUES (?, ?, ?, ?)");

$buyers = [
    ['BUY-201', 'Apex Regional Public Schools Council', 'Educational Institution', 'procurement@apexschools.org'],
    ['BUY-202', 'Metro Health & Clinic Consortium', 'Healthcare Provider', 'supplies@metrohealth.co.in'],
    ['BUY-203', 'Evergreen Retail Stores Pvt Ltd', 'Commercial Supermarket Chain', 'orders@evergreenretail.com']
];

foreach ($buyers as $b) {
    $insertBuyer->execute($b);
}

// 3. Seed Certified Raw Material Suppliers
$insertSupplier = $db->prepare("INSERT INTO suppliers (id, name, material_type, unit_price, available_stock) VALUES (?, ?, ?, ?, ?)");

$suppliers = [
    ['SUP-301', 'Coimbatore Cotton Spinners Ltd', 'Grade-A Poly-Cotton Fabric', 320.00, 12000],
    ['SUP-302', 'Salem Alloy & Steel Traders', 'EN-8 Precision Steel Rods', 680.00, 4500],
    ['SUP-303', 'BioPolymer Packing Materials', 'Food Grade Eco-Film Bags', 45.00, 80000]
];

foreach ($suppliers as $s) {
    $insertSupplier->execute($s);
}

// 4. Seed Real Market Demand Orders
$insertOrder = $db->prepare("INSERT INTO demand_orders (id, buyer_id, item_description, units_demanded, unit_target_price, delivery_deadline, buyer_verified) VALUES (?, ?, ?, ?, ?, ?, ?)");

$orders = [
    ['ORD-501', 'BUY-201', 'Heavy Duty Standardized School Uniform Sets', 500, 750.00, '2026-12-15', 1],
    ['ORD-502', 'BUY-202', 'Surgical Cotton Scrubs & Lab Coats', 350, 680.00, '2026-12-28', 1],
    ['ORD-503', 'BUY-203', 'Precision Brass Valve Bushings', 800, 1100.00, '2027-01-10', 1]
];

foreach ($orders as $o) {
    $insertOrder->execute($o);
}

// 5. Seed Initial Opportunity for MSME-101 (The Uniform Opportunity)
// Demand: 500 units. MSME Free Capacity: 800 - 250 = 550. Gap: 500 units needing raw material financing.
// Financing needed: 500 units * 320/unit raw material = 160,000 INR
$insertOpp = $db->prepare("INSERT INTO opportunities (id, demand_order_id, msme_id, supplier_id, demanded_units, msme_capacity_gap, estimated_financing_required, status, consensus_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

$insertOpp->execute([
    'OPP-9001',
    'ORD-501',
    'MSME-101',
    'SUP-301',
    500,
    300, // producing 200 currently for others, needs capital for remaining 300
    160000.00,
    'DETECTED',
    0.0
]);

echo "Database seeded successfully at backend/database/conduit.sqlite!\n";
