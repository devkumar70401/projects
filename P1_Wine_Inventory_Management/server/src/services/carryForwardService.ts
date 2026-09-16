import { DatabaseSync } from 'node:sqlite';

export class CarryForwardService {
  /**
   * Retrieves the previous balance (opening balance) for a specific shop, brand, and size on a target date.
   * Priority:
   * 1. The most recent inventory record strictly before target date (record_date < date).
   * 2. The initial opening_stock configured for this shop/brand/size.
   * 3. null (never silently invent a zero balance).
   */
  static getPreviousBalance(
    db: DatabaseSync,
    shopId: number,
    brandId: number,
    sizeId: number,
    targetDate: string
  ): number | null {
    // 1. Look for the most recent inventory record prior to target date
    const priorRecordStmt = db.prepare(`
      SELECT remaining
      FROM inventory_records
      WHERE shop_id = ? AND brand_id = ? AND size_id = ? AND record_date < ?
      ORDER BY record_date DESC
      LIMIT 1
    `);
    const priorRecord = priorRecordStmt.get(shopId, brandId, sizeId, targetDate) as { remaining: number } | undefined;

    if (priorRecord !== undefined && priorRecord.remaining !== undefined) {
      return priorRecord.remaining;
    }

    // 2. If no previous daily record exists, look in opening_stock
    const openingStockStmt = db.prepare(`
      SELECT quantity
      FROM opening_stock
      WHERE shop_id = ? AND brand_id = ? AND size_id = ?
      LIMIT 1
    `);
    const openingStock = openingStockStmt.get(shopId, brandId, sizeId) as { quantity: number } | undefined;

    if (openingStock !== undefined && openingStock.quantity !== undefined) {
      return openingStock.quantity;
    }

    // 3. Neither exists: return null so the UI can flag missing opening stock
    return null;
  }
}
