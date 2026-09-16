import { describe, it, expect, beforeEach } from 'vitest';
import { DatabaseSync } from 'node:sqlite';
import { getDatabase } from '../src/db/connection.js';
import { runMigrations } from '../src/db/migrate.js';
import { CarryForwardService } from '../src/services/carryForwardService.js';
import { InventoryService } from '../src/services/inventoryService.js';

describe('Carry Forward Service & Isolation', () => {
  let db: DatabaseSync;

  beforeEach(() => {
    db = new DatabaseSync(':memory:');
    db.exec('PRAGMA foreign_keys = ON;');
    
    // Create tables
    db.exec(`
      CREATE TABLE shops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, active INTEGER NOT NULL DEFAULT 1);
      CREATE TABLE brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, display_order INTEGER NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1);
      CREATE TABLE sizes (id INTEGER PRIMARY KEY AUTOINCREMENT, volume_ml INTEGER NOT NULL UNIQUE);
      CREATE TABLE rates (id INTEGER PRIMARY KEY AUTOINCREMENT, brand_id INTEGER NOT NULL, size_id INTEGER NOT NULL, rate REAL NOT NULL, effective_from TEXT NOT NULL, UNIQUE(brand_id, size_id, effective_from));
      CREATE TABLE opening_stock (id INTEGER PRIMARY KEY AUTOINCREMENT, shop_id INTEGER NOT NULL, brand_id INTEGER NOT NULL, size_id INTEGER NOT NULL, quantity INTEGER NOT NULL, as_of_date TEXT NOT NULL, UNIQUE(shop_id, brand_id, size_id));
      CREATE TABLE inventory_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        record_date TEXT NOT NULL,
        shop_id INTEGER NOT NULL,
        brand_id INTEGER NOT NULL,
        size_id INTEGER NOT NULL,
        previous_balance INTEGER NOT NULL,
        aamad INTEGER NOT NULL DEFAULT 0,
        total INTEGER NOT NULL,
        bikri INTEGER NOT NULL DEFAULT 0,
        remaining INTEGER NOT NULL,
        rate REAL NOT NULL,
        value REAL NOT NULL,
        finalized INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL DEFAULT (datetime('now')),
        updated_at TEXT NOT NULL DEFAULT (datetime('now')),
        UNIQUE(record_date, shop_id, brand_id, size_id)
      );
    `);

    // Seed minimal shops, brands, sizes
    db.exec(`
      INSERT INTO shops (id, name) VALUES (1, 'Shop A'), (2, 'Shop B');
      INSERT INTO brands (id, name, display_order) VALUES (1, 'Royal Stag', 1), (2, 'Imperial Blue', 2);
      INSERT INTO sizes (id, volume_ml) VALUES (1, 180), (2, 375), (3, 750);
      INSERT INTO rates (brand_id, size_id, rate, effective_from) VALUES 
        (1, 1, 160, '2026-08-01'), (1, 2, 320, '2026-08-01'), (1, 3, 640, '2026-08-01'),
        (2, 1, 130, '2026-08-01'), (2, 2, 260, '2026-08-01'), (2, 3, 520, '2026-08-01');
    `);
  });

  it('uses opening_stock if no previous inventory record exists', () => {
    db.exec(`INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES (1, 1, 1, 100, '2026-08-01')`);
    
    const balance = CarryForwardService.getPreviousBalance(db, 1, 1, 1, '2026-08-02');
    expect(balance).toBe(100);
  });

  it('returns null if neither previous record nor opening_stock exists (never silently invent 0)', () => {
    const balance = CarryForwardService.getPreviousBalance(db, 1, 1, 1, '2026-08-02');
    expect(balance).toBeNull();
  });

  it('automatically carries forward Day 1 remaining to Day 2 previous_balance', () => {
    // Day 1: previous = 100, aamad = 20, total = 120, bikri = 50, remaining = 70
    db.exec(`
      INSERT INTO inventory_records (record_date, shop_id, brand_id, size_id, previous_balance, aamad, total, bikri, remaining, rate, value)
      VALUES ('2026-08-02', 1, 1, 1, 100, 20, 120, 50, 70, 160, 11200);
    `);

    const day2Previous = CarryForwardService.getPreviousBalance(db, 1, 1, 1, '2026-08-03');
    expect(day2Previous).toBe(70);
  });

  it('maintains strict isolation across shops: Shop A closing stock does NOT leak into Shop B', () => {
    db.exec(`
      INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES 
        (1, 1, 1, 100, '2026-08-01'),
        (2, 1, 1, 40, '2026-08-01');
      INSERT INTO inventory_records (record_date, shop_id, brand_id, size_id, previous_balance, aamad, total, bikri, remaining, rate, value)
      VALUES ('2026-08-02', 1, 1, 1, 100, 20, 120, 50, 70, 160, 11200);
    `);

    // Shop A should resolve to 70 for 2026-08-03
    expect(CarryForwardService.getPreviousBalance(db, 1, 1, 1, '2026-08-03')).toBe(70);
    // Shop B should resolve to its own opening stock 40 (since it has no record for 2026-08-02)
    expect(CarryForwardService.getPreviousBalance(db, 2, 1, 1, '2026-08-03')).toBe(40);
  });

  it('maintains strict isolation across brands and bottle sizes', () => {
    db.exec(`
      INSERT INTO inventory_records (record_date, shop_id, brand_id, size_id, previous_balance, aamad, total, bikri, remaining, rate, value)
      VALUES 
        ('2026-08-02', 1, 1, 1, 100, 0, 100, 10, 90, 160, 14400), -- Brand 1, Size 1 (180ml) -> 90
        ('2026-08-02', 1, 1, 2, 50, 0, 50, 5, 45, 320, 14400),   -- Brand 1, Size 2 (375ml) -> 45
        ('2026-08-02', 1, 2, 1, 80, 0, 80, 20, 60, 130, 7800);   -- Brand 2, Size 1 (180ml) -> 60
    `);

    expect(CarryForwardService.getPreviousBalance(db, 1, 1, 1, '2026-08-03')).toBe(90);
    expect(CarryForwardService.getPreviousBalance(db, 1, 1, 2, '2026-08-03')).toBe(45);
    expect(CarryForwardService.getPreviousBalance(db, 1, 2, 1, '2026-08-03')).toBe(60);
  });
});
