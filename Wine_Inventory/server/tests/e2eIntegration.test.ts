import { describe, it, expect, beforeEach } from 'vitest';
import { DatabaseSync } from 'node:sqlite';
import { InventoryService } from '../src/services/inventoryService.js';
import { RateService } from '../src/services/rateService.js';
import { ReportService } from '../src/services/reportService.js';

describe('End-to-End Business Flow & Register Lifecycle', () => {
  let db: DatabaseSync;

  beforeEach(() => {
    db = new DatabaseSync(':memory:');
    db.exec('PRAGMA foreign_keys = ON;');
    db.exec(`
      CREATE TABLE shops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, active INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT (datetime('now')));
      CREATE TABLE brands (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, display_order INTEGER NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT (datetime('now')));
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
      CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'owner', created_at TEXT NOT NULL DEFAULT (datetime('now')));
    `);

    // Seed 2 shops, 3 sizes, 35 brands, rates, opening stocks
    db.exec(`
      INSERT INTO shops (id, name) VALUES (1, 'S.A. वाइन शॉप (Main)'), (2, 'शाखा दुकान 2');
      INSERT INTO sizes (id, volume_ml) VALUES (1, 180), (2, 375), (3, 750);
    `);

    for (let b = 1; b <= 35; b++) {
      db.exec(`INSERT INTO brands (id, name, display_order) VALUES (${b}, 'Brand ${b}', ${b});`);
      for (let s = 1; s <= 3; s++) {
        const rate = (s === 1 ? 160 : s === 2 ? 320 : 640) + b * 2;
        db.exec(`INSERT INTO rates (brand_id, size_id, rate, effective_from) VALUES (${b}, ${s}, ${rate}, '2026-08-01');`);
        db.exec(`INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES (1, ${b}, ${s}, 50, '2026-08-01');`);
        db.exec(`INSERT INTO opening_stock (shop_id, brand_id, size_id, quantity, as_of_date) VALUES (2, ${b}, ${s}, 30, '2026-08-01');`);
      }
    }
  });

  it('executes the full multi-day carry-forward, rate change, and reporting lifecycle', async () => {
    // 1. Day 1: Load sheet for 2026-08-10 for Shop 1
    const day1Sheet = InventoryService.getDailySheet(db, 1, '2026-08-10');
    expect(day1Sheet.length).toBe(35);
    expect(day1Sheet[0].sizes[180].previous_balance).toBe(50); // From opening stock

    // 2. Day 1: Save sheet with sales and incoming stock
    const day1Payload = [
      { brand_id: 1, size_id: 1, aamad: 20, bikri: 30 }, // Prev=50, aamad=20 -> total=70, bikri=30 -> rem=40
      { brand_id: 1, size_id: 2, aamad: 10, bikri: 15 }, // Prev=50, aamad=10 -> total=60, bikri=15 -> rem=45
      { brand_id: 1, size_id: 3, aamad: 0, bikri: 10 },  // Prev=50, aamad=0 -> total=50, bikri=10 -> rem=40
    ];

    const saveDay1 = InventoryService.saveDailySheet(db, 1, '2026-08-10', day1Payload);
    expect(saveDay1.success).toBe(true);

    // 3. Day 2: Load sheet for 2026-08-11 for Shop 1
    // Pichla Shesh MUST automatically equal Day 1's remaining (40, 45, 40)
    const day2Sheet = InventoryService.getDailySheet(db, 1, '2026-08-11');
    expect(day2Sheet[0].sizes[180].previous_balance).toBe(40);
    expect(day2Sheet[0].sizes[375].previous_balance).toBe(45);
    expect(day2Sheet[0].sizes[750].previous_balance).toBe(40);

    // 4. Rate Update: Change Brand 1 (180ml) rate from 162 to 200 on 2026-08-11
    RateService.setRate(db, 1, 1, 200, '2026-08-11');

    // Confirm Day 1 historical record is unchanged
    const day1Saved = db.prepare('SELECT rate, value, remaining FROM inventory_records WHERE record_date = ? AND brand_id = 1 AND size_id = 1').get('2026-08-10') as any;
    expect(day1Saved.rate).toBe(162);
    expect(day1Saved.remaining).toBe(40);
    expect(day1Saved.value).toBe(40 * 162);

    // 5. Day 2: Save sheet with new rate applied
    const day2Payload = [
      { brand_id: 1, size_id: 1, aamad: 10, bikri: 20 }, // Prev=40, aamad=10 -> total=50, bikri=20 -> rem=30
    ];
    const saveDay2 = InventoryService.saveDailySheet(db, 1, '2026-08-11', day2Payload);
    expect(saveDay2.success).toBe(true);

    const day2Saved = db.prepare('SELECT rate, value, remaining FROM inventory_records WHERE record_date = ? AND brand_id = 1 AND size_id = 1').get('2026-08-11') as any;
    expect(day2Saved.rate).toBe(200);
    expect(day2Saved.remaining).toBe(30);
    expect(day2Saved.value).toBe(30 * 200);

    // 6. Reports & Analytics
    const dashboard = ReportService.getDashboardSummary(db, '2026-08-11', 1);
    expect(dashboard.total_sales_value).toBe(20 * 200); // 4000
    expect(dashboard.total_bikri_qty).toBe(20);

    // 7. Excel & PDF export generation
    const records = InventoryService.getRecordHistory(db, 1);
    expect(records.length).toBe(4);

    const excelBuffer = await ReportService.generateExcelReport(records);
    expect(excelBuffer.length).toBeGreaterThan(1000);

    const csvContent = ReportService.generateCsvReport(records);
    expect(csvContent).toContain('Brand 1');
    expect(csvContent).toContain('2026-08-10');
    expect(csvContent).toContain('2026-08-11');

    const pdfBuffer = await ReportService.generatePdfReport('S.A. वाइन शॉप (Main)', '2026-08-11', day2Sheet);
    expect(pdfBuffer.length).toBeGreaterThan(1000);
  });
});
