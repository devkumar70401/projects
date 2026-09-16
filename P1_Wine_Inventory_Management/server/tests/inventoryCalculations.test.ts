import { describe, it, expect } from 'vitest';
import { InventoryService } from '../src/services/inventoryService.js';

describe('Inventory Calculation Formulas', () => {
  it('computes normal case: previous=100, aamad=20, bikri=50 -> total=120, remaining=70', () => {
    const previous = 100;
    const aamad = 20;
    const bikri = 50;
    const rate = 160;

    const total = InventoryService.computeTotal(previous, aamad);
    expect(total).toBe(120);

    const remaining = InventoryService.computeRemaining(total, bikri);
    expect(remaining).toBe(70);

    const value = InventoryService.computeValue(previous, aamad, bikri, total, remaining, rate);
    expect(value).toBe(70 * 160); // 11200
  });

  it('computes no incoming stock: aamad=0 -> total=previous', () => {
    const previous = 45;
    const aamad = 0;
    const total = InventoryService.computeTotal(previous, aamad);
    expect(total).toBe(45);
  });

  it('computes no sales: bikri=0 -> remaining=total', () => {
    const total = 115;
    const bikri = 0;
    const remaining = InventoryService.computeRemaining(total, bikri);
    expect(remaining).toBe(115);
  });

  it('computes sales value correctly: bikri * rate', () => {
    const bikri = 15;
    const rate = 320;
    const salesVal = InventoryService.computeSalesValue(bikri, rate);
    expect(salesVal).toBe(4800);
  });
});
