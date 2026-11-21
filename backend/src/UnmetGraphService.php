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
                'name' => $b['name'],
                'label' => $b['name'],
                'type' => 'BUYER',
                'category' => $b['organization_type'],
                'size' => 24,
                'color' => '#38bdf8',
                'details' => [
                    'organization' => $b['organization_type'],
                    'contact' => $b['contact_email'],
                    'network_role' => 'Institutional Demand Ingest'
                ]
            ];
        }

        foreach ($msmes as $m) {
            $maxCap = (int)$m['max_monthly_capacity'];
            $curProd = (int)$m['current_production'];
            $idle = $maxCap - $curProd;
            $util = $maxCap > 0 ? round(($curProd / $maxCap) * 100, 1) : 0;
            $nodes[] = [
                'id' => $m['id'],
                'name' => $m['business_name'],
                'label' => $m['business_name'],
                'type' => 'MSME',
                'category' => $m['industry'],
                'location' => $m['location'],
                'idle_capacity' => $idle,
                'max_capacity' => $maxCap,
                'current_production' => $curProd,
                'utilization' => $util . '%',
                'bottleneck' => $m['capital_bottleneck'],
                'size' => 28,
                'color' => '#00e599',
                'details' => [
                    'industry' => $m['industry'],
                    'cluster_location' => $m['location'],
                    'monthly_capacity' => $maxCap,
                    'active_output' => $curProd,
                    'idle_headroom' => $idle,
                    'utilization_rate' => $util . '%',
                    'capital_bottleneck' => $m['capital_bottleneck']
                ]
            ];
        }

        foreach ($suppliers as $s) {
            $unitPrice = (float)$s['unit_price'];
            $stock = (int)$s['available_stock'];
            $nodes[] = [
                'id' => $s['id'],
                'name' => $s['name'],
                'label' => $s['name'],
                'type' => 'SUPPLIER',
                'category' => $s['material_type'],
                'unit_price' => $unitPrice,
                'available_stock' => $stock,
                'size' => 20,
                'color' => '#f59e0b',
                'details' => [
                    'material_spec' => $s['material_type'],
                    'catalog_unit_price_inr' => $unitPrice,
                    'earmarked_inventory_stock' => $stock,
                    'escrow_eligible' => 'Certified Vendor'
                ]
            ];
        }

        foreach ($opportunities as $opp) {
            $d = current(array_filter($orders, fn($x) => $x['id'] === $opp['demand_order_id']));
            $financing = (float)$opp['estimated_financing_required'];
            $units = (int)$opp['demanded_units'];
            $status = $opp['status'];

            if ($d) {
                $links[] = [
                    'source' => $d['buyer_id'],
                    'target' => $opp['msme_id'],
                    'type' => 'DEMAND_CONTRACT',
                    'relationship' => 'UNFULFILLED_DEMAND',
                    'opportunity_id' => $opp['id'],
                    'volume_units' => $units,
                    'capital_gap' => $financing,
                    'capital_required_inr' => $financing,
                    'item' => $d['item_description'],
                    'status' => $status,
                    'color' => ($status === 'FINANCED') ? '#00e599' : 'rgba(56, 189, 248, 0.5)'
                ];
            }

            if ($opp['supplier_id']) {
                $links[] = [
                    'source' => $opp['msme_id'],
                    'target' => $opp['supplier_id'],
                    'type' => 'MATERIAL_ALLOCATION',
                    'relationship' => 'MATERIAL_ALLOCATION',
                    'opportunity_id' => $opp['id'],
                    'volume_units' => $units,
                    'capital_gap' => $financing,
                    'capital_required_inr' => $financing,
                    'status' => $status,
                    'color' => ($status === 'FINANCED') ? '#00e599' : 'rgba(245, 158, 11, 0.5)'
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
