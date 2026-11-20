<?php
// backend/src/OpportunityService.php

namespace Conduit;

use PDO;
use Exception;

class OpportunityService {
    private PDO $db;

    public function __construct(PDO $db) {
        $this->db = $db;
    }

    /**
     * Element 03: Compute demand-capacity matching from database tables
     */
    public function detectOpportunities(): array {
        $orders = $this->db->query("SELECT * FROM demand_orders WHERE buyer_verified = 1")->fetchAll();
        $msmes = $this->db->query("SELECT * FROM msmes")->fetchAll();
        $suppliers = $this->db->query("SELECT * FROM suppliers")->fetchAll();

        $created = [];

        foreach ($orders as $order) {
            foreach ($msmes as $msme) {
                $itemDesc = strtolower($order['item_description']);
                $isCompatible = false;

                if (str_contains($itemDesc, 'uniform') || str_contains($itemDesc, 'garment') || str_contains($itemDesc, 'scrubs')) {
                    $isCompatible = ($msme['industry'] === 'Apparel & Garments');
                } elseif (str_contains($itemDesc, 'valve') || str_contains($itemDesc, 'steel') || str_contains($itemDesc, 'bushing')) {
                    $isCompatible = ($msme['industry'] === 'Light Engineering & Auto Components');
                } elseif (str_contains($itemDesc, 'pack') || str_contains($itemDesc, 'film') || str_contains($itemDesc, 'food')) {
                    $isCompatible = ($msme['industry'] === 'Food Processing & Packaging');
                }

                if (!$isCompatible) continue;

                $freeCapacity = (int)$msme['max_monthly_capacity'] - (int)$msme['current_production'];
                $unitsDemanded = (int)$order['units_demanded'];

                if ($freeCapacity >= $unitsDemanded) {
                    $check = $this->db->prepare("SELECT id FROM opportunities WHERE demand_order_id = ? AND msme_id = ?");
                    $check->execute([$order['id'], $msme['id']]);
                    if ($check->fetch()) continue;

                    $matchedSupplier = null;
                    foreach ($suppliers as $s) {
                        $mat = strtolower($s['material_type']);
                        if ($msme['industry'] === 'Apparel & Garments' && str_contains($mat, 'fabric')) {
                            $matchedSupplier = $s; break;
                        } elseif ($msme['industry'] === 'Light Engineering & Auto Components' && str_contains($mat, 'steel')) {
                            $matchedSupplier = $s; break;
                        } elseif ($msme['industry'] === 'Food Processing & Packaging' && str_contains($mat, 'film')) {
                            $matchedSupplier = $s; break;
                        }
                    }
                    if (!$matchedSupplier) $matchedSupplier = $suppliers[0] ?? null;

                    $unitCost = $matchedSupplier ? (float)$matchedSupplier['unit_price'] : 320.00;
                    $estFinancing = $unitsDemanded * $unitCost;
                    $capacityGap = max(100, $unitsDemanded - ($freeCapacity - $unitsDemanded));
                    $oppId = 'OPP-' . strtoupper(bin2hex(random_bytes(3)));

                    $insert = $this->db->prepare("
                        INSERT INTO opportunities 
                        (id, demand_order_id, msme_id, supplier_id, demanded_units, msme_capacity_gap, estimated_financing_required, status, consensus_score)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'DETECTED', 0.0)
                    ");
                    $insert->execute([$oppId, $order['id'], $msme['id'], $matchedSupplier['id'] ?? null, $unitsDemanded, $capacityGap, $estFinancing]);

                    $created[] = [
                        'opportunity_id' => $oppId,
                        'order_id' => $order['id'],
                        'units' => $unitsDemanded,
                        'msme_id' => $msme['id'],
                        'supplier_id' => $matchedSupplier['id'] ?? null,
                        'capital_required_inr' => $estFinancing
                    ];
                }
            }
        }

        return $created;
    }

    public function getDetectorFeed(): array {
        $orders = $this->db->query("SELECT d.*, b.name as buyer_name FROM demand_orders d JOIN buyers b ON d.buyer_id = b.id")->fetchAll();
        $msmes = $this->db->query("SELECT * FROM msmes")->fetchAll();
        $opps = $this->db->query("
            SELECT o.id, o.demand_order_id, o.demanded_units, o.estimated_financing_required, o.status, o.consensus_score,
                   m.business_name, m.industry, m.location, (m.max_monthly_capacity - m.current_production) as idle_capacity,
                   d.item_description, d.unit_target_price, d.delivery_deadline,
                   b.name as buyer_name, s.name as supplier_name, s.unit_price as supplier_unit_price
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN buyers b ON d.buyer_id = b.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            ORDER BY o.created_at DESC
        ")->fetchAll();

        return [
            'orders' => $orders,
            'msmes' => $msmes,
            'opportunities' => $opps,
            'stats' => [
                'demand_units' => (int)array_sum(array_column($orders, 'units_demanded')),
                'idle_capacity_units' => (int)array_sum(array_map(fn($m) => (int)$m['max_monthly_capacity'] - (int)$m['current_production'], $msmes)),
                'opportunity_count' => count($opps),
                'capital_required_inr' => (float)array_sum(array_column($opps, 'estimated_financing_required'))
            ]
        ];
    }

    /**
     * Element 04: Exact mathematical compute of 4 interventions
     */
    public function simulateUnlock(string $opportunityId, ?array $customParams = null): array {
        $stmt = $this->db->prepare("
            SELECT o.*, m.business_name, m.max_monthly_capacity, m.current_production,
                   d.units_demanded, d.unit_target_price, s.unit_price as supplier_price
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            WHERE o.id = ?
        ");
        $stmt->execute([$opportunityId]);
        $opp = $stmt->fetch();

        if (!$opp) throw new Exception("Opportunity not found: " . $opportunityId);

        $units = (int)($customParams['units'] ?? $opp['units_demanded']);
        $sellingPrice = (float)($customParams['selling_price'] ?? $opp['unit_target_price']);
        $rawUnitCost = (float)($customParams['raw_cost'] ?? ($opp['supplier_price'] ?? 320.00));

        $revenue = $units * $sellingPrice;
        $rawCapital = $units * $rawUnitCost;
        $rawMargin = $revenue - $rawCapital;
        $rawRoi = $rawCapital > 0 ? round(($rawMargin / $rawCapital) * 100, 1) : 0;

        $machineryCost = 450000.00;
        $idleCap = (int)$opp['max_monthly_capacity'] - (int)$opp['current_production'];
        $machineryAddedUnits = ($idleCap < $units) ? ($units - $idleCap) : 0;
        $machineryMargin = $machineryAddedUnits * ($sellingPrice - $rawUnitCost);
        $machineryRoi = round(($machineryMargin / $machineryCost) * 100, 1);

        $storageCost = 120000.00;
        $spoilageSavedUnits = (int)round($units * 0.18);
        $storageMargin = $spoilageSavedUnits * $sellingPrice;
        $storageRoi = round(($storageMargin / $storageCost) * 100, 1);

        $wcLine = $rawCapital * 1.25;
        $wcFulfilledUnits = (int)round($units * 0.75);
        $wcMargin = ($wcFulfilledUnits * $sellingPrice) - ($wcFulfilledUnits * $rawUnitCost);
        $wcRoi = $wcLine > 0 ? round(($wcMargin / $wcLine) * 100, 1) : 0;

        return [
            'opportunity_id' => $opp['id'],
            'inputs' => [
                'units' => $units,
                'selling_price' => $sellingPrice,
                'raw_cost' => $rawUnitCost,
                'revenue' => $revenue
            ],
            'results' => [
                [
                    'key' => 'raw_materials_direct',
                    'name' => 'Supplier-Direct Material Finance',
                    'capital_required' => $rawCapital,
                    'units_output' => $units,
                    'gross_margin' => $rawMargin,
                    'roi' => $rawRoi,
                    'cycle_days' => 45,
                    'leakage_pct' => 5,
                    'efficiency_score' => 96
                ],
                [
                    'key' => 'machinery_capex',
                    'name' => 'Equipment Capex Expansion',
                    'capital_required' => $machineryCost,
                    'units_output' => $machineryAddedUnits,
                    'gross_margin' => $machineryMargin,
                    'roi' => $machineryRoi,
                    'cycle_days' => 720,
                    'leakage_pct' => 10,
                    'efficiency_score' => 38
                ],
                [
                    'key' => 'storage_logistics',
                    'name' => 'Staging & Spoilage Reduction',
                    'capital_required' => $storageCost,
                    'units_output' => $spoilageSavedUnits,
                    'gross_margin' => $storageMargin,
                    'roi' => $storageRoi,
                    'cycle_days' => 180,
                    'leakage_pct' => 8,
                    'efficiency_score' => 64
                ],
                [
                    'key' => 'working_capital_line',
                    'name' => 'General Cash Credit Line',
                    'capital_required' => $wcLine,
                    'units_output' => $wcFulfilledUnits,
                    'gross_margin' => $wcMargin,
                    'roi' => $wcRoi,
                    'cycle_days' => 120,
                    'leakage_pct' => 28,
                    'efficiency_score' => 58
                ]
            ]
        ];
    }

    /**
     * Element 05: Mathematical Contract Milestone Computation
     */
    public function compileLoanContract(string $opportunityId, ?array $customTerms = null): array {
        $stmt = $this->db->prepare("
            SELECT o.*, m.business_name, s.name as supplier_name, b.name as buyer_name, d.delivery_deadline
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN buyers b ON d.buyer_id = b.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            WHERE o.id = ?
        ");
        $stmt->execute([$opportunityId]);
        $opp = $stmt->fetch();

        if (!$opp) throw new Exception("Opportunity not found: " . $opportunityId);

        if ((float)$opp['consensus_score'] < 0.90) {
            throw new Exception("Consensus below 90% (Current: " . round((float)$opp['consensus_score'] * 100) . "%)");
        }

        $contractId = 'LOAN-' . strtoupper(bin2hex(random_bytes(4)));
        $principal = (float)($customTerms['amount'] ?? $opp['estimated_financing_required']);
        $rate = 11.25;
        $tenure = 3;

        $t1 = round($principal * 0.70, 2);
        $t2 = round($principal * 0.30, 2);
        $interest = round($principal * ($rate / 100) * ($tenure / 12), 2);
        $repayment = $principal + $interest;

        $milestones = [
            ['stage' => 'T1_SUPPLIER_RAW_LOCK', 'pct' => 70, 'amount' => $t1, 'target' => $opp['supplier_name'], 'condition' => 'Supplier inventory allocation receipt'],
            ['stage' => 'T2_MSME_LABOUR_WAGES', 'pct' => 30, 'amount' => $t2, 'target' => $opp['business_name'], 'condition' => 'Mid-batch production QA confirmation'],
            ['stage' => 'T3_BUYER_ESCROW_SWEEP', 'pct' => 100, 'amount' => $repayment, 'target' => 'Lender Account', 'condition' => 'Delivery signoff by ' . $opp['buyer_name']]
        ];

        $terms = json_encode(['milestones' => $milestones, 'repayment' => ['principal' => $principal, 'interest' => $interest, 'total' => $repayment, 'due' => $opp['delivery_deadline']]]);

        $ins = $this->db->prepare("
            INSERT OR REPLACE INTO loan_contracts 
            (id, opportunity_id, msme_id, supplier_id, approved_amount, disbursement_type, interest_rate, tenure_months, status, repayment_terms)
            VALUES (?, ?, ?, ?, ?, 'SUPPLIER_DIRECT', ?, ?, 'COMPILED', ?)
        ");
        $ins->execute([$contractId, $opportunityId, $opp['msme_id'], $opp['supplier_id'], $principal, $rate, $tenure, $terms]);

        $this->db->prepare("UPDATE opportunities SET status = 'FINANCED' WHERE id = ?")->execute([$opportunityId]);

        return [
            'contract_id' => $contractId,
            'opportunity_id' => $opportunityId,
            'principal' => $principal,
            'rate_pct' => $rate,
            'tenure_months' => $tenure,
            'total_repayment' => $repayment,
            'interest' => $interest,
            'status' => 'COMPILED',
            'milestones' => $milestones
        ];
    }

    public function getLoanContract(string $opportunityId): ?array {
        $stmt = $this->db->prepare("
            SELECT l.*, m.business_name, s.name as supplier_name, b.name as buyer_name, d.delivery_deadline, d.units_demanded
            FROM loan_contracts l
            JOIN opportunities o ON l.opportunity_id = o.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN msmes m ON l.msme_id = m.id
            JOIN suppliers s ON l.supplier_id = s.id
            JOIN buyers b ON d.buyer_id = b.id
            WHERE l.opportunity_id = ? OR l.id = ?
        ");
        $stmt->execute([$opportunityId, $opportunityId]);
        $row = $stmt->fetch();
        if (!$row) return null;

        $terms = json_decode($row['repayment_terms'], true);

        return [
            'contract_id' => $row['id'],
            'opportunity_id' => $row['opportunity_id'],
            'status' => $row['status'],
            'msme' => $row['business_name'],
            'supplier' => $row['supplier_name'],
            'buyer' => $row['buyer_name'],
            'principal' => (float)$row['approved_amount'],
            'rate_pct' => (float)$row['interest_rate'],
            'tenure_months' => (int)$row['tenure_months'],
            'milestones' => $terms['milestones'] ?? [],
            'repayment' => $terms['repayment'] ?? []
        ];
    }
}
