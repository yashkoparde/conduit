<?php
// backend/src/UnmetGraphService.php

namespace Conduit;

use PDO;

class UnmetGraphService {
    private PDO $db;

    public function __construct(PDO $db) {
        $this->db = $db;
    }

    /**
     * Element 07: Unmet-Economy Graph Topology
     */
    public function getGraphTopology(): array {
        $msmes = $this->db->query("SELECT * FROM msmes")->fetchAll();
        $buyers = $this->db->query("SELECT * FROM buyers")->fetchAll();
        $suppliers = $this->db->query("SELECT * FROM suppliers")->fetchAll();
        $orders = $this->db->query("SELECT * FROM demand_orders")->fetchAll();
        $opportunities = $this->db->query("SELECT * FROM opportunities")->fetchAll();
        $contracts = $this->db->query("SELECT * FROM loan_contracts")->fetchAll();

        $nodes = [];
        $links = [];

        foreach ($buyers as $b) {
            $nodes[] = [
                'id' => $b['id'],
                'label' => $b['name'],
                'type' => 'BUYER',
                'category' => $b['organization_type'],
                'size' => 24,
                'color' => '#60a5fa'
            ];
        }

        foreach ($msmes as $m) {
            $idle = (int)$m['max_monthly_capacity'] - (int)$m['current_production'];
            $nodes[] = [
                'id' => $m['id'],
                'label' => $m['business_name'],
                'type' => 'MSME',
                'category' => $m['industry'],
                'location' => $m['location'],
                'idle_capacity' => $idle,
                'bottleneck' => $m['capital_bottleneck'],
                'size' => 28,
                'color' => '#00e599'
            ];
        }

        foreach ($suppliers as $s) {
            $nodes[] = [
                'id' => $s['id'],
                'label' => $s['name'],
                'type' => 'SUPPLIER',
                'category' => $s['material_type'],
                'unit_price' => (float)$s['unit_price'],
                'size' => 20,
                'color' => '#f59e0b'
            ];
        }

        foreach ($opportunities as $opp) {
            $d = current(array_filter($orders, fn($x) => $x['id'] === $opp['demand_order_id']));
            if ($d) {
                $links[] = [
                    'source' => $d['buyer_id'],
                    'target' => $opp['msme_id'],
                    'relationship' => 'UNFULFILLED_DEMAND',
                    'opportunity_id' => $opp['id'],
                    'volume_units' => (int)$opp['demanded_units'],
                    'capital_required_inr' => (float)$opp['estimated_financing_required'],
                    'status' => $opp['status'],
                    'color' => ($opp['status'] === 'FINANCED') ? '#00e599' : 'rgba(96, 165, 250, 0.5)'
                ];
            }

            if ($opp['supplier_id']) {
                $links[] = [
                    'source' => $opp['msme_id'],
                    'target' => $opp['supplier_id'],
                    'relationship' => 'MATERIAL_ALLOCATION',
                    'opportunity_id' => $opp['id'],
                    'volume_units' => (int)$opp['demanded_units'],
                    'capital_required_inr' => (float)$opp['estimated_financing_required'],
                    'status' => $opp['status'],
                    'color' => ($opp['status'] === 'FINANCED') ? '#00e599' : 'rgba(245, 158, 11, 0.5)'
                ];
            }
        }

        $totalNodes = count($nodes);
        $totalEdges = count($links);
        $density = $totalNodes > 1 ? round(($totalEdges / ($totalNodes * ($totalNodes - 1))) * 100, 2) . '%' : '0%';

        return [
            'network_status' => 'ACTIVE_GRAPH',
            'timestamp' => date('c'),
            'metrics' => [
                'total_nodes' => $totalNodes,
                'total_edges' => $totalEdges,
                'active_opportunities' => count($opportunities),
                'compiled_contracts' => count($contracts),
                'network_density' => $density
            ],
            'nodes' => $nodes,
            'links' => $links
        ];
    }
}
