<?php
// backend/src/VerificationEngine.php

namespace Conduit;

use PDO;
use Exception;

class VerificationEngine {
    private PDO $db;
    private const SECRET_SALT = 'CONDUIT_ED25519_CORE_PROOF_2026';

    public function __construct(PDO $db) {
        $this->db = $db;
    }

    /**
     * Submit an independent verification proof with cryptographic hash and rigorous bounds.
     */
    public function submitProof(string $opportunityId, string $participantType, string $participantId, array $parameters): array {
        $stmt = $this->db->prepare("
            SELECT o.*, d.units_demanded, d.unit_target_price, d.delivery_deadline,
                   m.max_monthly_capacity, m.current_production, s.unit_price as supplier_unit_price, s.available_stock
            FROM opportunities o
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN msmes m ON o.msme_id = m.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            WHERE o.id = ?
        ");
        $stmt->execute([$opportunityId]);
        $opp = $stmt->fetch();

        if (!$opp) {
            throw new Exception("Opportunity not found: " . $opportunityId);
        }

        $proofType = '';
        $auditMetrics = [];

        switch ($participantType) {
            case 'BUYER':
                $proofType = 'DEMAND_CONFIRMATION';
                $confirmedUnits = (int)($parameters['confirmed_units'] ?? 0);
                if ($confirmedUnits <= 0) {
                    throw new Exception("Buyer proof rejected: confirmed_units must be > 0");
                }
                if ($confirmedUnits > (int)$opp['units_demanded']) {
                    throw new Exception("Buyer proof rejected: confirmed_units ({$confirmedUnits}) exceeds order demand ({$opp['units_demanded']})");
                }
                if (empty($parameters['payment_escrow_ready'])) {
                    throw new Exception("Buyer proof rejected: payment_escrow_ready flag must be true");
                }
                $auditMetrics = [
                    'contract_value_inr' => $confirmedUnits * (float)$opp['unit_target_price'],
                    'escrow_allocated' => true,
                    'inspection_window_days' => (int)($parameters['delivery_inspection_window_days'] ?? 7)
                ];
                break;

            case 'SUPPLIER':
                $proofType = 'INVENTORY_ALLOCATION';
                $allocatedStock = (int)($parameters['allocated_stock_units'] ?? 0);
                $unitCost = (float)($parameters['confirmed_unit_cost'] ?? 0);
                if ($allocatedStock < (int)$opp['units_demanded']) {
                    throw new Exception("Supplier proof rejected: allocated_stock ({$allocatedStock}) insufficient for order volume ({$opp['units_demanded']})");
                }
                if ($unitCost <= 0) {
                    throw new Exception("Supplier proof rejected: confirmed_unit_cost must be > 0");
                }
                $auditMetrics = [
                    'inventory_locked_units' => $allocatedStock,
                    'unit_cost_inr' => $unitCost,
                    'total_material_value_inr' => $allocatedStock * $unitCost,
                    'dispatch_readiness_days' => (int)($parameters['ready_to_dispatch_days'] ?? 3)
                ];
                break;

            case 'MSME':
                $proofType = 'CAPACITY_PLEDGE';
                $pledgedHours = (int)($parameters['pledged_shift_hours'] ?? 0);
                $activeOperators = (int)($parameters['active_operators'] ?? 0);
                if ($pledgedHours < 50) {
                    throw new Exception("MSME proof rejected: minimum 50 shift hours required for batch run");
                }
                if ($activeOperators < 2) {
                    throw new Exception("MSME proof rejected: minimum 2 operators required");
                }
                $auditMetrics = [
                    'pledged_shift_hours' => $pledgedHours,
                    'active_operators' => $activeOperators,
                    'idle_capacity_headroom' => (int)$opp['max_monthly_capacity'] - (int)$opp['current_production'],
                    'inspection_ready_date' => $parameters['inspection_ready_date'] ?? $opp['delivery_deadline']
                ];
                break;

            case 'LOGISTICS':
                $proofType = 'ROUTE_AFFIDAVIT';
                $corridorScore = (int)($parameters['corridor_delivery_rating'] ?? 0);
                if ($corridorScore < 80) {
                    throw new Exception("Logistics proof rejected: corridor delivery reliability score must be >= 80%");
                }
                $auditMetrics = [
                    'corridor_score' => $corridorScore,
                    'vehicle_assigned' => !empty($parameters['vehicle_assigned']),
                    'transit_hours_estimate' => (int)($parameters['transit_hours_estimate'] ?? 24)
                ];
                break;

            default:
                throw new Exception("Unsupported participant type: " . $participantType);
        }

        // Cryptographic integrity seal
        $timestamp = date('c');
        $rawPayload = json_encode([
            'opportunity_id' => $opportunityId,
            'participant_type' => $participantType,
            'participant_id' => $participantId,
            'proof_type' => $proofType,
            'metrics' => $auditMetrics,
            'timestamp' => $timestamp
        ], JSON_UNESCAPED_SLASHES);

        $digitalSignature = hash('sha256', $rawPayload . self::SECRET_SALT);
        $proofId = 'PRF-' . strtoupper(bin2hex(random_bytes(4)));

        // Atomic Upsert
        $this->db->beginTransaction();
        try {
            $deleteOld = $this->db->prepare("DELETE FROM verification_proofs WHERE opportunity_id = ? AND participant_type = ?");
            $deleteOld->execute([$opportunityId, $participantType]);

            $insertProof = $this->db->prepare("
                INSERT INTO verification_proofs 
                (id, opportunity_id, participant_type, participant_id, proof_type, parameters_json, digital_signature) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            ");
            $insertProof->execute([
                $proofId,
                $opportunityId,
                $participantType,
                $participantId,
                $proofType,
                json_encode($auditMetrics),
                $digitalSignature
            ]);

            $audit = $this->recomputeConsensus($opportunityId);
            $this->db->commit();
        } catch (Exception $e) {
            $this->db->rollBack();
            throw $e;
        }

        return [
            'proof_id' => $proofId,
            'opportunity_id' => $opportunityId,
            'participant_type' => $participantType,
            'digital_signature' => $digitalSignature,
            'audit' => $audit
        ];
    }

    /**
     * Compute multi-party consensus score with strict thresholds:
     * BUYER (35%), SUPPLIER (30%), MSME (25%), LOGISTICS (10%)
     */
    public function recomputeConsensus(string $opportunityId): array {
        $stmt = $this->db->prepare("SELECT * FROM verification_proofs WHERE opportunity_id = ?");
        $stmt->execute([$opportunityId]);
        $proofs = $stmt->fetchAll();

        $weights = [
            'BUYER' => 0.35,
            'SUPPLIER' => 0.30,
            'MSME' => 0.25,
            'LOGISTICS' => 0.10
        ];

        $submitted = [];
        $totalScore = 0.0;

        foreach ($proofs as $p) {
            $type = $p['participant_type'];
            $submitted[$type] = true;
            $totalScore += $weights[$type] ?? 0.0;
        }

        $consensusScore = round($totalScore, 2);
        
        // Check current opportunity status to avoid overriding FINANCED
        $curStmt = $this->db->prepare("SELECT status FROM opportunities WHERE id = ?");
        $curStmt->execute([$opportunityId]);
        $currentOpp = $curStmt->fetch();

        $newStatus = ($currentOpp && $currentOpp['status'] === 'FINANCED') 
            ? 'FINANCED' 
            : (($consensusScore >= 0.90) ? 'VERIFIED' : 'DETECTED');

        $updateOpp = $this->db->prepare("UPDATE opportunities SET consensus_score = ?, status = ? WHERE id = ?");
        $updateOpp->execute([$consensusScore, $newStatus, $opportunityId]);

        return [
            'consensus_score' => $consensusScore,
            'percentage' => round($consensusScore * 100) . '%',
            'status' => $newStatus,
            'proofs_count' => count($proofs),
            'verified_parties' => array_keys($submitted),
            'missing_parties' => array_values(array_diff(array_keys($weights), array_keys($submitted)))
        ];
    }

    public function getVerificationDossier(string $opportunityId): array {
        $stmt = $this->db->prepare("
            SELECT o.*, 
                   m.business_name AS msme_name, m.industry, m.location, m.max_monthly_capacity, m.current_production,
                   b.name AS buyer_name, b.organization_type AS buyer_type,
                   s.name AS supplier_name, s.material_type, s.unit_price AS supplier_unit_price,
                   d.item_description, d.units_demanded, d.unit_target_price, d.delivery_deadline
            FROM opportunities o
            JOIN msmes m ON o.msme_id = m.id
            JOIN demand_orders d ON o.demand_order_id = d.id
            JOIN buyers b ON d.buyer_id = b.id
            LEFT JOIN suppliers s ON o.supplier_id = s.id
            WHERE o.id = ?
        ");
        $stmt->execute([$opportunityId]);
        $opportunity = $stmt->fetch();

        if (!$opportunity) {
            throw new Exception("Opportunity not found: " . $opportunityId);
        }

        $proofStmt = $this->db->prepare("SELECT * FROM verification_proofs WHERE opportunity_id = ? ORDER BY verified_at ASC");
        $proofStmt->execute([$opportunityId]);
        $proofs = $proofStmt->fetchAll();

        foreach ($proofs as &$p) {
            $p['parameters'] = json_decode($p['parameters_json'], true);
            unset($p['parameters_json']);
        }

        return [
            'opportunity' => $opportunity,
            'proofs' => $proofs,
            'consensus' => $this->recomputeConsensus($opportunityId)
        ];
    }
}
