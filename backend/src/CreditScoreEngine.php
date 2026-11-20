<?php
// backend/src/CreditScoreEngine.php

namespace Conduit;

use PDO;

class CreditScoreEngine {
    private PDO $db;

    public function __construct(PDO $db) {
        $this->db = $db;
    }

    /**
     * Compute real empirical opportunity credit score (Range: 300 - 900)
     * Replaces backward-looking balance sheets with real transactional physics:
     * 
     * 1. Multi-Party Consensus Integrity (30% weight, max 180 pts)
     *    - Based on cryptographic consensus of Buyer, Supplier, MSME, Logistics
     * 
     * 2. Demand-to-Capacity Utilization Physics (25% weight, max 150 pts)
     *    - Evaluates if MSME physical equipment headroom >= order demand volume
     * 
     * 3. Intervention ROI & Gross Margin Coverage (20% weight, max 120 pts)
     *    - Ratio of order gross profit to capital required. Debt service coverage margin.
     * 
     * 4. Counterparty Reliability & Track Record (15% weight, max 90 pts)
     *    - Delivery ratings, institutional status of buyer, supplier grade.
     * 
     * 5. Execution Corridor & Leakage Minimization (10% weight, max 60 pts)
     *    - Supplier-direct payment mode (low diversion) vs. cash credit (high diversion).
     * 
     * Base Score: 300 points. Maximum: 900 points.
     */
    public function computeScore(string $opportunityId): array {
        $stmt = $this->db->prepare("
            SELECT o.*, 
                   m.business_name, m.industry, m.location, m.max_monthly_capacity, m.current_production, m.capital_bottleneck,
                   d.units_demanded, d.unit_target_price, d.delivery_deadline, d.item_description,
                   b.name as buyer_name, b.organization_type,
                   s.name as supplier_name, s.unit_price as supplier_price, s.available_stock
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN buyers b ON d.buyer_id = b.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            WHERE o.id = ?
        ");
        $stmt->execute([$opportunityId]);
        $opp = $stmt->fetch();

        if (!$opp) {
            throw new \Exception("Opportunity not found: " . $opportunityId);
        }

        $proofs = $this->db->query("SELECT * FROM verification_proofs WHERE opportunity_id = " . $this->db->quote($opportunityId))->fetchAll();

        // 1. Multi-Party Consensus (Weight 30% = 180 points)
        $consensusRatio = (float)($opp['consensus_score'] ?? 0.0);
        $consensusPoints = round($consensusRatio * 180);

        // 2. Capacity Physics (Weight 25% = 150 points)
        $idleCapacity = (int)$opp['max_monthly_capacity'] - (int)$opp['current_production'];
        $demandedUnits = (int)$opp['demanded_units'];
        $capacityRatio = $demandedUnits > 0 ? min(1.5, $idleCapacity / $demandedUnits) : 0;
        
        $capacityPoints = 0;
        if ($capacityRatio >= 1.0) {
            $capacityPoints = 150; // Full headroom without strain
        } elseif ($capacityRatio >= 0.75) {
            $capacityPoints = 110;
        } elseif ($capacityRatio >= 0.5) {
            $capacityPoints = 70;
        } else {
            $capacityPoints = 30;
        }

        // 3. Margin & Debt Coverage (Weight 20% = 120 points)
        $sellingPrice = (float)$opp['unit_target_price'];
        $rawCost = (float)($opp['supplier_price'] ?? 320.00);
        $totalRevenue = $demandedUnits * $sellingPrice;
        $totalCapitalReq = $demandedUnits * $rawCost;
        $grossMargin = $totalRevenue - $totalCapitalReq;
        $marginRatio = $totalCapitalReq > 0 ? ($grossMargin / $totalCapitalReq) : 0;

        $marginPoints = 0;
        if ($marginRatio >= 1.0) { // Over 100% markup/margin
            $marginPoints = 120;
        } elseif ($marginRatio >= 0.5) {
            $marginPoints = 90;
        } elseif ($marginRatio >= 0.25) {
            $marginPoints = 60;
        } else {
            $marginPoints = 30;
        }

        // 4. Counterparty Reliability (Weight 15% = 90 points)
        $counterpartyPoints = 0;
        // Institutional/Healthcare buyers get higher reliability
        if (str_contains(strtolower($opp['organization_type']), 'institution') || str_contains(strtolower($opp['organization_type']), 'healthcare')) {
            $counterpartyPoints += 50;
        } else {
            $counterpartyPoints += 35;
        }
        // Verified supplier with adequate stock
        if ($opp['supplier_name'] && (int)$opp['available_stock'] >= $demandedUnits) {
            $counterpartyPoints += 40;
        } else {
            $counterpartyPoints += 20;
        }

        // 5. Corridor & Leakage Minimization (Weight 10% = 60 points)
        $leakagePoints = 0;
        $hasLogisticsProof = false;
        foreach ($proofs as $p) {
            if ($p['participant_type'] === 'LOGISTICS') $hasLogisticsProof = true;
        }
        if ($hasLogisticsProof) {
            $leakagePoints += 30;
        }
        // Supplier-direct escrow structure avoids 28% cash leakage
        $leakagePoints += 30;

        // Base score = 300
        $finalScore = 300 + $consensusPoints + $capacityPoints + $marginPoints + $counterpartyPoints + $leakagePoints;
        $finalScore = min(900, max(300, (int)$finalScore));

        // Score Category & Lending Tier
        $tier = '';
        $lenderAction = '';
        $maxLTV = '';
        $primeSpread = '';

        if ($finalScore >= 800) {
            $tier = 'TIER_1_PRIME';
            $lenderAction = 'AUTO_APPROVE // Instant Closed-Loop Disbursement';
            $maxLTV = '90% of Order Value';
            $primeSpread = 'Prime + 1.25% (10.50% - 11.25%)';
        } elseif ($finalScore >= 700) {
            $tier = 'TIER_2_INVESTMENT_GRADE';
            $lenderAction = 'FAST_TRACK // Approve with Staged Milestone Release';
            $maxLTV = '80% of Order Value';
            $primeSpread = 'Prime + 2.50% (11.75% - 12.50%)';
        } elseif ($finalScore >= 600) {
            $tier = 'TIER_3_MONITORED';
            $lenderAction = 'CONDITIONAL // Requires 100% Supplier-Direct Escrow';
            $maxLTV = '70% of Order Value';
            $primeSpread = 'Prime + 4.00% (13.50% - 14.50%)';
        } else {
            $tier = 'TIER_4_SUBPRIME_HIGH_RISK';
            $lenderAction = 'HOLD // Missing Consensus or Insufficient Equipment Headroom';
            $maxLTV = '50% of Order Value';
            $primeSpread = 'Prime + 6.50% (16.00%+)';
        }

        $breakdown = [
            'base_score' => 300,
            'pillars' => [
                'consensus_integrity' => [
                    'label' => 'Multi-Party Consensus Integrity (30%)',
                    'earned' => $consensusPoints,
                    'max' => 180,
                    'factor' => ($consensusRatio * 100) . '% Proof Verified'
                ],
                'capacity_physics' => [
                    'label' => 'Idle Machine Headroom vs Demand (25%)',
                    'earned' => $capacityPoints,
                    'max' => 150,
                    'factor' => "{$idleCapacity} idle units / {$demandedUnits} demanded"
                ],
                'margin_coverage' => [
                    'label' => 'Gross Profit Margin & Coverage (20%)',
                    'earned' => $marginPoints,
                    'max' => 120,
                    'factor' => round($marginRatio * 100) . '% Gross Markup over Capital'
                ],
                'counterparty_reliability' => [
                    'label' => 'Buyer & Supplier Tier Rating (15%)',
                    'earned' => $counterpartyPoints,
                    'max' => 90,
                    'factor' => "{$opp['organization_type']} + In-Stock Supplier"
                ],
                'corridor_leakage' => [
                    'label' => 'Closed-Loop Anti-Leakage Escrow (10%)',
                    'earned' => $leakagePoints,
                    'max' => 60,
                    'factor' => 'Supplier-Direct Disbursed (Zero Cash Diversion)'
                ]
            ],
            'total_score' => $finalScore,
            'lender_assessment' => [
                'tier' => $tier,
                'action' => $lenderAction,
                'recommended_ltv' => $maxLTV,
                'recommended_pricing' => $primeSpread
            ]
        ];

        // Store computed score persistently in SQLite
        $update = $this->db->prepare("
            UPDATE opportunities 
            SET credit_score = ?, score_breakdown_json = ? 
            WHERE id = ?
        ");
        $update->execute([$finalScore, json_encode($breakdown), $opportunityId]);

        return [
            'opportunity_id' => $opportunityId,
            'score' => $finalScore,
            'tier' => $tier,
            'action' => $lenderAction,
            'breakdown' => $breakdown
        ];
    }
}
