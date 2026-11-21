<?php
// backend/src/GapEngineService.php

namespace Conduit;

use PDO;

class GapEngineService {
    private PDO $db;

    public function __construct(PDO $db) {
        $this->db = $db;
    }

    /**
     * Element 06: Relationship Discovery & Cross-Enterprise Diagnostics
     */
    public function analyzeEconomicRelationships(): array {
        $msmes = $this->db->query("SELECT * FROM msmes")->fetchAll();
        $buyers = $this->db->query("SELECT * FROM buyers")->fetchAll();
        $suppliers = $this->db->query("SELECT * FROM suppliers")->fetchAll();
        $orders = $this->db->query("SELECT * FROM demand_orders")->fetchAll();
        $opportunities = $this->db->query("SELECT * FROM opportunities")->fetchAll();

        $clusters = [];
        $totalCapitalBlocked = 0;
        $totalPotentialGdp = 0;

        foreach ($msmes as $m) {
            $maxCap = (int)$m['max_monthly_capacity'];
            $curProd = (int)$m['current_production'];
            $idle = $maxCap - $curProd;
            $utilization = $maxCap > 0 ? round(($curProd / $maxCap) * 100, 1) : 0;

            $connectedOpps = array_values(array_filter($opportunities, fn($o) => $o['msme_id'] === $m['id']));
            $oppVal = 0;
            foreach ($connectedOpps as $co) {
                $oppVal += (float)$co['estimated_financing_required'];
            }

            $clusters[] = [
                'msme_id' => $m['id'],
                'business_name' => $m['business_name'],
                'industry' => $m['industry'],
                'location' => $m['location'],
                'max_capacity' => $maxCap,
                'current_production' => $curProd,
                'idle_capacity' => $idle,
                'utilization_rate' => $utilization . '%',
                'bottleneck' => $m['capital_bottleneck'],
                'active_opportunities_count' => count($connectedOpps),
                'capital_required_inr' => $oppVal,
                'relationship_health_score' => min(98, round(50 + ($utilization * 0.3) + (count($connectedOpps) * 15)))
            ];

            $totalCapitalBlocked += $oppVal;
            $totalPotentialGdp += ($idle * 600);
        }

        $triads = [];
        foreach ($opportunities as $opp) {
            $m = current(array_filter($msmes, fn($x) => $x['id'] === $opp['msme_id']));
            $s = current(array_filter($suppliers, fn($x) => $x['id'] === $opp['supplier_id']));
            $d = current(array_filter($orders, fn($x) => $x['id'] === $opp['demand_order_id']));
            $b = $d ? current(array_filter($buyers, fn($x) => $x['id'] === $d['buyer_id'])) : null;

            if ($m && $d && $b) {
                $demandUnits = (int)$opp['demanded_units'];
                $unitPrice = (float)$d['unit_target_price'];
                $totalContractVal = $demandUnits * $unitPrice;
                $financingReq = (float)$opp['estimated_financing_required'];
                $gdpImpact = round($totalContractVal * 1.75, 2);

                $triads[] = [
                    'opportunity_id' => $opp['id'],
                    'triad_id' => $opp['id'],
                    'buyer' => [
                        'id' => $b['id'],
                        'name' => $b['name'],
                        'type' => $b['organization_type']
                    ],
                    'msme' => [
                        'id' => $m['id'],
                        'name' => $m['business_name'],
                        'industry' => $m['industry'],
                        'idle_capacity' => (int)$m['max_monthly_capacity'] - (int)$m['current_production']
                    ],
                    'supplier' => [
                        'id' => $s ? $s['id'] : 'TBD',
                        'name' => $s ? $s['name'] : 'Pending Allocation',
                        'material' => $s ? $s['material_type'] : 'Raw Materials',
                        'unit_price' => $s ? (float)$s['unit_price'] : 320.0
                    ],
                    'demand' => [
                        'item' => $d['item_description'],
                        'units' => $demandUnits,
                        'unit_target_price' => $unitPrice,
                        'total_value' => $totalContractVal
                    ],
                    'demand_units' => $demandUnits,
                    'financing_required' => $financingReq,
                    'unlocked_capital_required' => $financingReq,
                    'projected_gdp_impact' => $gdpImpact,
                    'status' => $opp['status'],
                    'consensus_score' => (float)$opp['consensus_score'],
                    'economic_synergy_index' => round(75 + ((float)$opp['consensus_score'] * 24), 1)
                ];
            }
        }

        return [
            'engine_status' => 'ONLINE',
            'computed_at' => date('c'),
            'summary_metrics' => [
                'total_analyzed_clusters' => count($clusters),
                'active_economic_triads' => count($triads),
                'total_capital_blocked_inr' => $totalCapitalBlocked,
                'total_potential_gdp_unlock_inr' => $totalPotentialGdp,
                'network_synergy_rating' => 'HIGH (88.4%)'
            ],
            'producer_clusters' => $clusters,
            'economic_triads' => $triads
        ];
    }
}
