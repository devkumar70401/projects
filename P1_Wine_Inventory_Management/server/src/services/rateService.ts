import { DatabaseSync } from 'node:sqlite';
import { Rate } from '../models/types.js';

export class RateService {
  /**
   * Fetches the current effective rate for a specific brand and size.
   */
  static getCurrentRate(
    db: DatabaseSync,
    brandId: number,
    sizeId: number,
    asOfDate?: string
  ): number {
    const targetDate = asOfDate || new Date().toISOString().split('T')[0];
    const stmt = db.prepare(`
      SELECT rate
      FROM rates
      WHERE brand_id = ? AND size_id = ? AND effective_from <= ?
      ORDER BY effective_from DESC, id DESC
      LIMIT 1
    `);
    const row = stmt.get(brandId, sizeId, targetDate) as { rate: number } | undefined;
    return row?.rate ?? 0;
  }

  /**
   * Updates/sets a new rate without mutating old records, preserving price history.
   */
  static setRate(
    db: DatabaseSync,
    brandId: number,
    sizeId: number,
    rate: number,
    effectiveFrom?: string
  ): void {
    if (rate < 0) {
      throw new Error('Rate must be non-negative.');
    }
    const effectiveDate = effectiveFrom || new Date().toISOString().split('T')[0];
    const stmt = db.prepare(`
      INSERT INTO rates (brand_id, size_id, rate, effective_from)
      VALUES (?, ?, ?, ?)
      ON CONFLICT(brand_id, size_id, effective_from) DO UPDATE SET rate = excluded.rate
    `);
    stmt.run(brandId, sizeId, rate, effectiveDate);
  }

  /**
   * Fetches all current rates mapped by `${brand_id}_${size_id}`.
   */
  static getAllCurrentRates(db: DatabaseSync, asOfDate?: string): Map<string, number> {
    const targetDate = asOfDate || new Date().toISOString().split('T')[0];
    const stmt = db.prepare(`
      SELECT r.brand_id, r.size_id, r.rate
      FROM rates r
      INNER JOIN (
        SELECT brand_id, size_id, MAX(effective_from) as max_date
        FROM rates
        WHERE effective_from <= ?
        GROUP BY brand_id, size_id
      ) latest ON r.brand_id = latest.brand_id AND r.size_id = latest.size_id AND r.effective_from = latest.max_date
    `);
    const rows = stmt.all(targetDate) as unknown as { brand_id: number; size_id: number; rate: number }[];
    const map = new Map<string, number>();
    rows.forEach(r => map.set(`${r.brand_id}_${r.size_id}`, r.rate));
    return map;
  }
}
