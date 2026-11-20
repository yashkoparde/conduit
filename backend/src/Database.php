<?php
// backend/src/Database.php

namespace Conduit;

use PDO;
use PDOException;

class Database {
    private static ?PDO $instance = null;

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            $dbPath = __DIR__ . '/../database/conduit.sqlite';
            $isNew = !file_exists($dbPath);

            try {
                self::$instance = new PDO("sqlite:" . $dbPath);
                self::$instance->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                self::$instance->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
                self::$instance->exec("PRAGMA foreign_keys = ON;");

                if ($isNew || filesize($dbPath) === 0) {
                    self::initSchema(self::$instance);
                } else {
                    self::migrateSchema(self::$instance);
                }
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
                exit;
            }
        }

        return self::$instance;
    }

    public static function migrateSchema(PDO $db): void {
        // Add credit scoring fields to opportunities if missing
        try {
            $db->exec("ALTER TABLE opportunities ADD COLUMN credit_score INTEGER DEFAULT 0;");
        } catch (PDOException $e) {}
        try {
            $db->exec("ALTER TABLE opportunities ADD COLUMN score_breakdown_json TEXT;");
        } catch (PDOException $e) {}
        try {
            $db->exec("ALTER TABLE opportunities ADD COLUMN simulated_intervention TEXT;");
        } catch (PDOException $e) {}
        try {
            $db->exec("ALTER TABLE opportunities ADD COLUMN simulated_roi REAL DEFAULT 0.0;");
        } catch (PDOException $e) {}
    }

    public static function initSchema(PDO $db): void {
        $schema = <<<SQL
        CREATE TABLE IF NOT EXISTS msmes (
            id TEXT PRIMARY KEY,
            business_name TEXT NOT NULL,
            industry TEXT NOT NULL,
            location TEXT NOT NULL,
            max_monthly_capacity INTEGER NOT NULL,
            current_production INTEGER NOT NULL,
            capital_bottleneck TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS buyers (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            organization_type TEXT NOT NULL,
            contact_email TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS suppliers (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            material_type TEXT NOT NULL,
            unit_price REAL NOT NULL,
            available_stock INTEGER NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS demand_orders (
            id TEXT PRIMARY KEY,
            buyer_id TEXT NOT NULL,
            item_description TEXT NOT NULL,
            units_demanded INTEGER NOT NULL,
            unit_target_price REAL NOT NULL,
            delivery_deadline DATE NOT NULL,
            buyer_verified INTEGER DEFAULT 1,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (buyer_id) REFERENCES buyers(id)
        );

        CREATE TABLE IF NOT EXISTS opportunities (
            id TEXT PRIMARY KEY,
            demand_order_id TEXT NOT NULL,
            msme_id TEXT NOT NULL,
            supplier_id TEXT,
            demanded_units INTEGER NOT NULL,
            msme_capacity_gap INTEGER NOT NULL,
            estimated_financing_required REAL NOT NULL,
            status TEXT DEFAULT 'DETECTED',
            consensus_score REAL DEFAULT 0.0,
            credit_score INTEGER DEFAULT 0,
            score_breakdown_json TEXT,
            simulated_intervention TEXT,
            simulated_roi REAL DEFAULT 0.0,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (demand_order_id) REFERENCES demand_orders(id),
            FOREIGN KEY (msme_id) REFERENCES msmes(id),
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        );

        CREATE TABLE IF NOT EXISTS verification_proofs (
            id TEXT PRIMARY KEY,
            opportunity_id TEXT NOT NULL,
            participant_type TEXT NOT NULL,
            participant_id TEXT NOT NULL,
            proof_type TEXT NOT NULL,
            parameters_json TEXT NOT NULL,
            digital_signature TEXT NOT NULL,
            verified_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (opportunity_id) REFERENCES opportunities(id)
        );

        CREATE TABLE IF NOT EXISTS loan_contracts (
            id TEXT PRIMARY KEY,
            opportunity_id TEXT NOT NULL UNIQUE,
            msme_id TEXT NOT NULL,
            supplier_id TEXT NOT NULL,
            approved_amount REAL NOT NULL,
            disbursement_type TEXT NOT NULL,
            interest_rate REAL NOT NULL,
            tenure_months INTEGER NOT NULL,
            status TEXT DEFAULT 'COMPILED',
            repayment_terms TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (opportunity_id) REFERENCES opportunities(id),
            FOREIGN KEY (msme_id) REFERENCES msmes(id),
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        );
SQL;
        $db->exec($schema);
    }
}
