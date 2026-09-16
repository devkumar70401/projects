import { DatabaseSync } from 'node:sqlite';
import { runTransaction } from '../db/connection.js';
import { CarryForwardService } from './carryForwardService.js';
import { RateService } from './rateService.js';
import {
  RegisterRowData,
  RegisterCellData,
  SaveRegisterRowPayload,
  ValidationErrorItem,
  InventoryRecord,
} from '../models/types.js';

export class InventoryService {
  /**
   * Core formula for Yog (Total available stock)
   */
  static computeTotal(previousBalance: number, aamad: number): number {
    return previousBalance + aamad;
  }

  /**
   * Core formula for Shesh (Closing / Remaining stock)
   */
  static computeRemaining(total: number, bikri: number): number {
    return total - bikri;
  }

  /**
   * Core formula for Keemat (Monetary value)
   * // TODO-CONFIRM: Check with client whether Keemat represents stock-on-hand value (remaining * rate)
   * // or daily sales value (bikri * rate).
   * // Default per Section 8 is remaining * rate (stock on hand value).
   */
  static computeValue(
    _previous: number,
    _aamad: number,
    _bikri: number,
    _total: number,
    remaining: number,
    rate: number
  ): number {
    return Math.round(remaining * rate * 100) / 100;
  }

  /**
   * Calculates sales value specifically for reports / daily sales figures (bikri * rate)
   */
  static computeSalesValue(bikri: number, rate: number): number {
    return Math.round(bikri * rate * 100) / 100;
  }

  /**
   * Loads the full daily register sheet for a given shop and date.
   * Returns one row per active brand, with columns grouped by bottle size (180, 375, 750).
   */
  static getDailySheet(db: DatabaseSync, shopId: number, date: string): RegisterRowData[] {
    // 1. Get active brands ordered by display_order
    const brands = db.prepare(`
      SELECT id, name, display_order
      FROM brands
      WHERE active = 1
      ORDER BY display_order ASC, name ASC
    `).all() as unknown as { id: number; name: string; display_order: number }[];

    // 2. Get active sizes ordered by volume
    const sizes = db.prepare(`
      SELECT id, volume_ml
      FROM sizes
      ORDER BY volume_ml ASC
    `).all() as unknown as { id: number; volume_ml: number }[];

    // 3. Get existing inventory records for this shop and date
    const existingRecords = db.prepare(`
      SELECT *
      FROM inventory_records
      WHERE shop_id = ? AND record_date = ?
    `).all(shopId, date) as unknown as InventoryRecord[];

    const recordMap = new Map<string, InventoryRecord>();
    existingRecords.forEach(r => recordMap.set(`${r.brand_id}_${r.size_id}`, r));

    // 4. Build the grid
    const sheet: RegisterRowData[] = [];

    for (const brand of brands) {
      const sizesMap: { [volume_ml: number]: RegisterCellData } = {};

      for (const size of sizes) {
        const key = `${brand.id}_${size.id}`;
        const existing = recordMap.get(key);

        if (existing) {
          sizesMap[size.volume_ml] = {
            brand_id: brand.id,
            brand_name: brand.name,
            size_id: size.id,
            volume_ml: size.volume_ml,
            previous_balance: existing.previous_balance,
            aamad: existing.aamad,
            total: existing.total,
            bikri: existing.bikri,
            remaining: existing.remaining,
            rate: existing.rate,
            value: existing.value,
            record_id: existing.id,
            has_opening_stock: true,
          };
        } else {
          // New day or unentered row: get carry-forward previous balance and rate
          const prev = CarryForwardService.getPreviousBalance(db, shopId, brand.id, size.id, date);
          const currentRate = RateService.getCurrentRate(db, brand.id, size.id, date);
          const prevBalanceNum = prev ?? 0;
          const aamad = 0;
          const total = this.computeTotal(prevBalanceNum, aamad);
          const bikri = 0;
          const remaining = this.computeRemaining(total, bikri);
          const value = this.computeValue(prevBalanceNum, aamad, bikri, total, remaining, currentRate);

          sizesMap[size.volume_ml] = {
            brand_id: brand.id,
            brand_name: brand.name,
            size_id: size.id,
            volume_ml: size.volume_ml,
            previous_balance: prev,
            aamad,
            total,
            bikri,
            remaining,
            rate: currentRate,
            value,
            has_opening_stock: prev !== null,
          };
        }
      }

      sheet.push({
        brand_id: brand.id,
        brand_name: brand.name,
        display_order: brand.display_order,
        sizes: sizesMap,
      });
    }

    return sheet;
  }

  /**
   * Saves the entire daily sheet transactionally.
   * Server validates every row and re-computes all derived fields.
   * If any single row fails, rolls back completely and returns errors.
   */
  static saveDailySheet(
    db: DatabaseSync,
    shopId: number,
    date: string,
    rows: SaveRegisterRowPayload[]
  ): { success: boolean; saved_count?: number; errors?: ValidationErrorItem[] } {
    // 1. Basic Date & Shop validation
    if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return {
        success: false,
        errors: [{ brand_id: 0, brand_name: 'All', size_id: 0, volume_ml: 0, message: 'Invalid date format (must be YYYY-MM-DD).' }],
      };
    }

    const todayStr = new Date().toISOString().split('T')[0];
    if (date > todayStr) {
      return {
        success: false,
        errors: [{ brand_id: 0, brand_name: 'All', size_id: 0, volume_ml: 0, message: 'Cannot save records for a future date.' }],
      };
    }

    const shop = db.prepare('SELECT id, name, active FROM shops WHERE id = ?').get(shopId) as { id: number; name: string; active: number } | undefined;
    if (!shop || shop.active === 0) {
      return {
        success: false,
        errors: [{ brand_id: 0, brand_name: 'Shop', size_id: 0, volume_ml: 0, message: 'Shop does not exist or is inactive.' }],
      };
    }

    // 2. Pre-fetch metadata
    const brandsMap = new Map<number, { name: string; active: number }>();
    const allBrands = db.prepare('SELECT id, name, active FROM brands').all() as unknown as { id: number; name: string; active: number }[];
    allBrands.forEach(b => brandsMap.set(b.id, { name: b.name, active: b.active }));

    const sizesMap = new Map<number, number>();
    const allSizes = db.prepare('SELECT id, volume_ml FROM sizes').all() as unknown as { id: number; volume_ml: number }[];
    allSizes.forEach(s => sizesMap.set(s.id, s.volume_ml));

    const errors: ValidationErrorItem[] = [];
    interface PreparedRow {
      shopId: number;
      brandId: number;
      sizeId: number;
      recordDate: string;
      previousBalance: number;
      aamad: number;
      total: number;
      bikri: number;
      remaining: number;
      rate: number;
      value: number;
    }
    const preparedRows: PreparedRow[] = [];

    // 3. Validation and preparation pass
    for (const row of rows) {
      const brand = brandsMap.get(row.brand_id);
      const brandName = brand ? brand.name : `Brand #${row.brand_id}`;
      const volumeMl = sizesMap.get(row.size_id) || 0;

      if (!brand) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Brand not found: ${brandName}`,
        });
        continue;
      }

      if (!volumeMl) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Invalid bottle size id: ${row.size_id}`,
        });
        continue;
      }

      const aamad = Number(row.aamad);
      const bikri = Number(row.bikri);

      if (isNaN(aamad) || aamad < 0 || !Number.isInteger(aamad)) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Aamad (आमद) must be a non-negative integer for ${brandName} ${volumeMl}ml.`,
        });
      }

      if (isNaN(bikri) || bikri < 0 || !Number.isInteger(bikri)) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Bikri (बिक्री) must be a non-negative integer for ${brandName} ${volumeMl}ml.`,
        });
      }

      // Determine previous balance
      // If there's an existing record on the same date, its previous_balance is fixed unless recalculated
      const existingOnDate = db.prepare(`
        SELECT previous_balance
        FROM inventory_records
        WHERE shop_id = ? AND brand_id = ? AND size_id = ? AND record_date = ?
      `).get(shopId, row.brand_id, row.size_id, date) as { previous_balance: number } | undefined;

      let prevBalance: number | null = null;
      if (existingOnDate !== undefined) {
        prevBalance = existingOnDate.previous_balance;
      } else {
        prevBalance = CarryForwardService.getPreviousBalance(db, shopId, row.brand_id, row.size_id, date);
      }

      if (prevBalance === null) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Opening stock not set for ${brandName} ${volumeMl}ml. Please configure opening stock first.`,
        });
        continue;
      }

      const total = this.computeTotal(prevBalance, aamad);

      // Enforce: bikri cannot exceed total available
      if (bikri > total) {
        errors.push({
          brand_id: row.brand_id,
          brand_name: brandName,
          size_id: row.size_id,
          volume_ml: volumeMl,
          message: `Bikri (${bikri}) exceeds available stock (${total} = Pichla Shesh ${prevBalance} + Aamad ${aamad}) for ${brandName} ${volumeMl}ml.`,
        });
        continue;
      }

      const remaining = this.computeRemaining(total, bikri);

      // Snapshot rate
      const rate = (row.rate !== undefined && row.rate >= 0)
        ? row.rate
        : RateService.getCurrentRate(db, row.brand_id, row.size_id, date);

      const value = this.computeValue(prevBalance, aamad, bikri, total, remaining, rate);

      preparedRows.push({
        shopId,
        brandId: row.brand_id,
        sizeId: row.size_id,
        recordDate: date,
        previousBalance: prevBalance,
        aamad,
        total,
        bikri,
        remaining,
        rate,
        value,
      });
    }

    if (errors.length > 0) {
      return { success: false, errors };
    }

    // 4. Atomic Transactional Upsert
    try {
      runTransaction(db, () => {
        const upsertStmt = db.prepare(`
          INSERT INTO inventory_records (
            record_date, shop_id, brand_id, size_id,
            previous_balance, aamad, total, bikri, remaining,
            rate, value, finalized, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, datetime('now'))
          ON CONFLICT(record_date, shop_id, brand_id, size_id) DO UPDATE SET
            previous_balance = excluded.previous_balance,
            aamad = excluded.aamad,
            total = excluded.total,
            bikri = excluded.bikri,
            remaining = excluded.remaining,
            rate = excluded.rate,
            value = excluded.value,
            finalized = excluded.finalized,
            updated_at = datetime('now')
        `);

        for (const pr of preparedRows) {
          upsertStmt.run(
            pr.recordDate,
            pr.shopId,
            pr.brandId,
            pr.sizeId,
            pr.previousBalance,
            pr.aamad,
            pr.total,
            pr.bikri,
            pr.remaining,
            pr.rate,
            pr.value
          );
        }
      });

      return { success: true, saved_count: preparedRows.length };
    } catch (err: any) {
      return {
        success: false,
        errors: [{ brand_id: 0, brand_name: 'Database', size_id: 0, volume_ml: 0, message: err.message || 'Database transaction error.' }],
      };
    }
  }

  /**
   * Get history for reporting / audit
   */
  static getRecordHistory(
    db: DatabaseSync,
    shopId?: number,
    brandId?: number,
    sizeId?: number,
    startDate?: string,
    endDate?: string
  ): InventoryRecord[] {
    let query = `
      SELECT ir.*, b.name as brand_name, s.name as shop_name, sz.volume_ml
      FROM inventory_records ir
      JOIN brands b ON ir.brand_id = b.id
      JOIN shops s ON ir.shop_id = s.id
      JOIN sizes sz ON ir.size_id = sz.id
      WHERE 1=1
    `;
    const params: any[] = [];

    if (shopId) {
      query += ' AND ir.shop_id = ?';
      params.push(shopId);
    }
    if (brandId) {
      query += ' AND ir.brand_id = ?';
      params.push(brandId);
    }
    if (sizeId) {
      query += ' AND ir.size_id = ?';
      params.push(sizeId);
    }
    if (startDate) {
      query += ' AND ir.record_date >= ?';
      params.push(startDate);
    }
    if (endDate) {
      query += ' AND ir.record_date <= ?';
      params.push(endDate);
    }

    query += ' ORDER BY ir.record_date DESC, b.display_order ASC, sz.volume_ml ASC';

    return db.prepare(query).all(...params) as unknown as InventoryRecord[];
  }
}
