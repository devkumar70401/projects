import { describe, it, expect, beforeEach } from 'vitest';
import { DatabaseSync } from 'node:sqlite';
import { InventoryService } from '../src/services/inventoryService.js';
import { RateService } from '../src/services/rateService.js';

describe('Validation, Transactionality & Rate Snapshot Integrity', () => {
  let db: DatabaseSync;

  beforeEach(() => {
    db = new DatabaseSync(':memory:');
    db.exec('PRAGMA foreign_keys = ON;');
    
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

      INSERT INTO shops (id, name) VALUES (1, 'Shop A');
      INSERT INTO brands (id, name, display_order) VALUES (1, 'Brand 1', 1), (2, 'Brand 2', 2), (3, 'Brand 3', 3);
      INSERT INTO sizes (id, volume_ml) VALUES (1, 180), (2, 375), (3, 750);
      INSERT INTO rates (brand_id, size_id, rate, effective_from) VALUES 
        (1, 1, 100, '2026-08-01'), (2, 1, 200, '2026-08-01'), (3, 1, 300, '2026-08-01');
      INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES
        (1, 1, 1, 50, '2026-08-01'),
        (1, 2, 1, 50, '2026-08-01'),
        (1, 3, 1, 50, '2026-08-01');
    `);
  });

  it('rejects save when Bikri exceeds available stock (bikri > total)', () => {
    // Brand 1 has opening stock = 50. Aamad = 10 -> Total = 60. Try Bikri = 70.
    const result = InventoryService.saveDailySheet(db, 1, '2026-08-02', [
      { brand_id: 1, size_id: 1, aamad: 10, bikri: 70 },
    ]);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    expect(result.errors![0].message).toContain('Bikri (70) exceeds available stock (60');
  });

  it('enforces whole-sheet transactionality: if one row fails, nothing is saved', () => {
    // 3 rows: row 1 valid, row 2 invalid (bikri > total), row 3 valid
    const rows = [
      { brand_id: 1, size_id: 1, aamad: 5, bikri: 10 }, // Valid: total 55, rem 45
      { brand_id: 2, size_id: 1, aamad: 5, bikri: 999 }, // INVALID: bikri 999 > 55
      { brand_id: 3, size_id: 1, aamad: 5, bikri: 10 }, // Valid
    ];

    const result = InventoryService.saveDailySheet(db, 1, '2026-08-02', rows);
    expect(result.success).toBe(false);

    // Confirm that NO rows were inserted into inventory_records
    const count = (db.prepare('SELECT COUNT(*) as c FROM inventory_records').get() as { c: number }).c;
    expect(count).toBe(0);
  });

  it('preserves rate snapshot integrity when rates are updated later', () => {
    // 1. Save Day 1 with current rate = 100
    const saveDay1 = InventoryService.saveDailySheet(db, 1, '2026-08-02', [
      { brand_id: 1, size_id: 1, aamad: 10, bikri: 20 },
    ]);
    expect(saveDay1.success).toBe(true);

    const day1Record = db.prepare('SELECT rate, value, remaining FROM inventory_records WHERE record_date = ?').get('2026-08-02') as any;
    expect(day1Record.rate).toBe(100);
    expect(day1Record.value).toBe(40 * 100); // remaining=40, value=4000

    // 2. Change rate from 100 to 180 on Day 2
    RateService.setRate(db, 1, 1, 180, '2026-08-03');

    // 3. Confirm Day 1 record still has rate 100 and value 4000
    const day1RecordAfter = db.prepare('SELECT rate, value FROM inventory_records WHERE record_date = ?').get('2026-08-02') as any;
    expect(day1RecordAfter.rate).toBe(100);
    expect(day1RecordAfter.value).toBe(4000);

    // 4. Save Day 2: should snapshot new rate 180
    const saveDay2 = InventoryService.saveDailySheet(db, 1, '2026-08-03', [
      { brand_id: 1, size_id: 1, aamad: 0, bikri: 10 },
    ]);
    expect(saveDay2.success).toBe(true);

    const day2Record = db.prepare('SELECT rate, value, remaining FROM inventory_records WHERE record_date = ?').get('2026-08-03') as any;
    expect(day2Record.rate).toBe(180);
    expect(day2Record.remaining).toBe(30);
    expect(day2Record.value).toBe(30 * 180); // 5400
  });
});
