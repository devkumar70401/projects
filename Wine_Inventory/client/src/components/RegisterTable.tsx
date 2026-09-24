import { useRef, KeyboardEvent } from 'react';
import { RegisterRowData, ValidationErrorItem } from '../types/index.js';
import { AlertCircle, AlertTriangle } from 'lucide-react';

interface Props {
  rows: RegisterRowData[];
  onCellChange: (brandId: number, volumeMl: number, field: 'aamad' | 'bikri', value: number) => void;
  validationErrors: ValidationErrorItem[];
}

export default function RegisterTable({ rows, onCellChange, validationErrors }: Props) {
  const volumes = [180, 375, 750];
  const tableRef = useRef<HTMLTableElement>(null);

  // Map errors for fast lookup: `${brand_id}_${volume_ml}`
  const errorMap = new Map<string, string>();
  validationErrors.forEach(err => {
    errorMap.set(`${err.brand_id}_${err.volume_ml}`, err.message);
  });

  // Calculate totals for footer
  const totals: {
    [vol: number]: {
      pShesh: number;
      aamad: number;
      total: number;
      bikri: number;
      shesh: number;
      value: number;
      salesValue: number;
    };
  } = {
    180: { pShesh: 0, aamad: 0, total: 0, bikri: 0, shesh: 0, value: 0, salesValue: 0 },
    375: { pShesh: 0, aamad: 0, total: 0, bikri: 0, shesh: 0, value: 0, salesValue: 0 },
    750: { pShesh: 0, aamad: 0, total: 0, bikri: 0, shesh: 0, value: 0, salesValue: 0 },
  };

  let grandTotalSalesValue = 0;
  let grandTotalStockValue = 0;

  rows.forEach(r => {
    volumes.forEach(v => {
      const cell = r.sizes[v];
      if (cell) {
        totals[v].pShesh += cell.previous_balance || 0;
        totals[v].aamad += cell.aamad || 0;
        totals[v].total += cell.total || 0;
        totals[v].bikri += cell.bikri || 0;
        totals[v].shesh += cell.remaining || 0;
        totals[v].value += cell.value || 0;
        const sales = (cell.bikri || 0) * (cell.rate || 0);
        totals[v].salesValue += sales;
        grandTotalSalesValue += sales;
        grandTotalStockValue += cell.value || 0;
      }
    });
  });

  // Handle keyboard navigation between editable cells
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>, rowIndex: number, volIndex: number, field: 'aamad' | 'bikri') => {
    if (e.key === 'Enter' || (e.key === 'Tab' && !e.shiftKey)) {
      e.preventDefault();
      let nextRow = rowIndex;
      let nextVol = volIndex;
      let nextField: 'aamad' | 'bikri' = field === 'aamad' ? 'bikri' : 'aamad';

      if (field === 'bikri') {
        if (volIndex < volumes.length - 1) {
          nextVol = volIndex + 1;
        } else {
          nextRow = rowIndex + 1;
          nextVol = 0;
        }
      }

      const nextInputId = `input-${nextRow}-${volumes[nextVol]}-${nextField}`;
      const nextEl = document.getElementById(nextInputId) as HTMLInputElement | null;
      if (nextEl) {
        nextEl.focus();
        nextEl.select();
      }
    } else if (e.key === 'Tab' && e.shiftKey) {
      e.preventDefault();
      let prevRow = rowIndex;
      let prevVol = volIndex;
      let prevField: 'aamad' | 'bikri' = field === 'bikri' ? 'aamad' : 'bikri';

      if (field === 'aamad') {
        if (volIndex > 0) {
          prevVol = volIndex - 1;
        } else {
          prevRow = rowIndex - 1;
          prevVol = volumes.length - 1;
        }
      }

      const prevInputId = `input-${prevRow}-${volumes[prevVol]}-${prevField}`;
      const prevEl = document.getElementById(prevInputId) as HTMLInputElement | null;
      if (prevEl) {
        prevEl.focus();
        prevEl.select();
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      const nextDownId = `input-${rowIndex + 1}-${volumes[volIndex]}-${field}`;
      const nextEl = document.getElementById(nextDownId) as HTMLInputElement | null;
      if (nextEl) {
        nextEl.focus();
        nextEl.select();
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      const prevUpId = `input-${rowIndex - 1}-${volumes[volIndex]}-${field}`;
      const prevEl = document.getElementById(prevUpId) as HTMLInputElement | null;
      if (prevEl) {
        prevEl.focus();
        prevEl.select();
      }
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden flex flex-col">
      {/* Scrollable Container with sticky headers */}
      <div className="overflow-x-auto overflow-y-auto max-h-[calc(100vh-250px)] register-table">
        <table ref={tableRef} className="w-full border-collapse text-xs text-slate-800 select-none">
          {/* Main Header */}
          <thead className="sticky top-0 z-20 bg-slate-900 text-white">
            {/* Top Header: Brand and Size Groups */}
            <tr className="border-b border-slate-700 text-center font-bold">
              <th
                rowSpan={2}
                className="sticky left-0 z-30 bg-slate-900 px-3 py-2 text-left font-devanagari text-sm font-extrabold border-r border-slate-700 min-w-[200px]"
              >
                ब्राण्ड (Brand)
              </th>
              {volumes.map(vol => (
                <th
                  key={vol}
                  colSpan={7}
                  className="px-2 py-1.5 border-r border-slate-700 bg-slate-800 text-center font-mono font-bold text-sm tracking-wide text-amber-300"
                >
                  {vol} ml {vol === 180 ? '(क्वार्टर / Quarter)' : vol === 375 ? '(अद्धा / Half)' : '(बोतल / Bottle)'}
                </th>
              ))}
            </tr>

            {/* Sub Header: Pichla Shesh, Aamad, Yog, Bikri, Shesh, Dar, Keemat */}
            <tr className="border-b border-slate-700 text-center font-medium font-devanagari text-[11px] bg-slate-900">
              {volumes.map((vol, vIdx) => (
                <div key={`sub-${vol}`} className="contents">
                  <th className="px-1.5 py-1.5 text-red-400 bg-slate-900 font-bold border-r border-slate-800 min-w-[48px]" title="पिछला शेष / Opening Balance">
                    पि.शेष
                  </th>
                  <th className="px-1.5 py-1.5 text-blue-400 bg-blue-950/40 font-bold border-r border-slate-800 min-w-[48px]" title="आमद / Stock In">
                    आमद
                  </th>
                  <th className="px-1.5 py-1.5 text-slate-300 bg-slate-900 font-semibold border-r border-slate-800 min-w-[48px]" title="योग / Total">
                    योग
                  </th>
                  <th className="px-1.5 py-1.5 text-blue-400 bg-blue-950/40 font-bold border-r border-slate-800 min-w-[48px]" title="बिक्री / Sales">
                    बिक्री
                  </th>
                  <th className="px-1.5 py-1.5 text-red-400 bg-slate-900 font-bold border-r border-slate-800 min-w-[48px]" title="शेष / Closing Balance">
                    शेष
                  </th>
                  <th className="px-1.5 py-1.5 text-slate-300 bg-slate-900 border-r border-slate-800 min-w-[45px]" title="दर / Rate">
                    दर
                  </th>
                  <th className={`px-2 py-1.5 text-slate-200 bg-slate-900 font-semibold min-w-[65px] ${vIdx < volumes.length - 1 ? 'border-r-2 border-slate-600' : ''}`} title="कीमत / Stock Value">
                    कीमत
                  </th>
                </div>
              ))}
            </tr>
          </thead>

          {/* Table Body */}
          <tbody className="divide-y divide-slate-200">
            {rows.map((row, rIdx) => {
              const isEven = rIdx % 2 === 0;
              return (
                <tr
                  key={row.brand_id}
                  className={`hover:bg-amber-50/60 transition-colors ${
                    isEven ? 'bg-white' : 'bg-slate-50/50'
                  }`}
                >
                  {/* Brand Name Column (Sticky left) */}
                  <td className="sticky left-0 z-10 px-3 py-1.5 font-medium text-slate-900 bg-inherit border-r border-slate-300 text-xs truncate max-w-[220px]">
                    <div className="flex items-center space-x-1.5">
                      <span className="text-[10px] text-slate-400 font-mono w-4">{rIdx + 1}.</span>
                      <span className="font-semibold text-slate-800 font-devanagari">{row.brand_name}</span>
                    </div>
                  </td>

                  {/* 3 Size Groups */}
                  {volumes.map((vol, vIdx) => {
                    const cell = row.sizes[vol];
                    if (!cell) {
                      return (
                        <td key={vol} colSpan={7} className="text-center text-slate-400 py-2 border-r border-slate-300">
                          -
                        </td>
                      );
                    }

                    const errKey = `${row.brand_id}_${vol}`;
                    const hasError = errorMap.has(errKey);
                    const errorMessage = errorMap.get(errKey);
                    const isBikriInvalid = cell.bikri > cell.total;
                    const noOpeningStock = cell.previous_balance === null;

                    return (
                      <div key={vol} className="contents">
                        {/* Pichla Shesh (Red, Read-Only) */}
                        <td className="px-1.5 py-1 text-right font-mono font-bold text-red-600 border-r border-slate-200 bg-red-50/20">
                          {noOpeningStock ? (
                            <span
                              title="आरंभिक स्टॉक दर्ज नहीं है (Opening stock not set)"
                              className="inline-flex items-center text-amber-600 text-[10px]"
                            >
                              <AlertTriangle className="w-3.5 h-3.5 mr-0.5 text-amber-500" />
                              <span className="text-[9px]">नदारद</span>
                            </span>
                          ) : (
                            cell.previous_balance
                          )}
                        </td>

                        {/* Aamad (Blue, Editable Input) */}
                        <td className="p-0 border-r border-slate-200 bg-blue-50/20">
                          <input
                            id={`input-${rIdx}-${vol}-aamad`}
                            type="number"
                            min={0}
                            inputMode="numeric"
                            value={cell.aamad === 0 ? '' : cell.aamad}
                            placeholder="0"
                            disabled={noOpeningStock}
                            onChange={e => {
                              const val = e.target.value === '' ? 0 : Math.max(0, parseInt(e.target.value, 10) || 0);
                              onCellChange(row.brand_id, vol, 'aamad', val);
                            }}
                            onKeyDown={e => handleKeyDown(e, rIdx, vIdx, 'aamad')}
                            onFocus={e => e.target.select()}
                            className="w-full h-8 px-1.5 text-right font-mono font-bold text-blue-600 focus:bg-blue-100/60 focus:ring-2 focus:ring-blue-500 focus:outline-none transition-all disabled:opacity-40"
                          />
                        </td>

                        {/* Yog (Black / Slate, Computed) */}
                        <td className="px-1.5 py-1 text-right font-mono font-semibold text-slate-700 border-r border-slate-200 bg-slate-100/40">
                          {noOpeningStock ? '-' : cell.total}
                        </td>

                        {/* Bikri (Blue, Editable Input) */}
                        <td className={`p-0 border-r border-slate-200 ${isBikriInvalid ? 'bg-red-100 ring-1 ring-red-500' : 'bg-blue-50/20'}`}>
                          <input
                            id={`input-${rIdx}-${vol}-bikri`}
                            type="number"
                            min={0}
                            inputMode="numeric"
                            value={cell.bikri === 0 ? '' : cell.bikri}
                            placeholder="0"
                            disabled={noOpeningStock}
                            onChange={e => {
                              const val = e.target.value === '' ? 0 : Math.max(0, parseInt(e.target.value, 10) || 0);
                              onCellChange(row.brand_id, vol, 'bikri', val);
                            }}
                            onKeyDown={e => handleKeyDown(e, rIdx, vIdx, 'bikri')}
                            onFocus={e => e.target.select()}
                            className={`w-full h-8 px-1.5 text-right font-mono font-bold text-blue-600 focus:bg-blue-100/60 focus:ring-2 focus:ring-blue-500 focus:outline-none transition-all disabled:opacity-40 ${
                              isBikriInvalid ? 'text-red-700 font-extrabold' : ''
                            }`}
                            title={isBikriInvalid ? `बिक्री (${cell.bikri}) कुल स्टॉक (${cell.total}) से अधिक नहीं हो सकती!` : undefined}
                          />
                        </td>

                        {/* Shesh (Red, Computed) */}
                        <td className={`px-1.5 py-1 text-right font-mono font-bold border-r border-slate-200 ${
                          cell.remaining < 0 ? 'text-red-700 bg-red-100' : 'text-red-600 bg-red-50/20'
                        }`}>
                          {noOpeningStock ? (
                            '-'
                          ) : (
                            <span className="flex items-center justify-end space-x-1">
                              <span>{cell.remaining}</span>
                              {(hasError || isBikriInvalid) && (
                                <span title={errorMessage || 'स्टॉक त्रुटि'}>
                                  <AlertCircle className="w-3.5 h-3.5 text-red-500 inline" />
                                </span>
                              )}
                            </span>
                          )}
                        </td>

                        {/* Dar (Black, Rate from master) */}
                        <td className="px-1.5 py-1 text-right font-mono text-slate-700 border-r border-slate-200 text-[11px]">
                          {cell.rate}
                        </td>

                        {/* Keemat (Stock Value, Computed) */}
                        <td className={`px-2 py-1 text-right font-mono font-medium text-slate-900 text-[11px] ${
                          vIdx < volumes.length - 1 ? 'border-r-2 border-slate-400' : ''
                        }`}>
                          {noOpeningStock ? '-' : cell.value.toLocaleString()}
                        </td>
                      </div>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>

          {/* Table Footer: Column Totals */}
          <tfoot className="sticky bottom-0 z-20 bg-slate-900 text-white font-bold border-t-2 border-slate-600">
            <tr>
              <td className="sticky left-0 z-30 bg-slate-900 px-3 py-2 font-devanagari text-amber-300 font-extrabold text-sm border-r border-slate-700">
                कुल योग (Total)
              </td>
              {volumes.map((vol, vIdx) => (
                <div key={`foot-${vol}`} className="contents font-mono text-xs">
                  <td className="px-1.5 py-2 text-right text-red-400 border-r border-slate-800">
                    {totals[vol].pShesh}
                  </td>
                  <td className="px-1.5 py-2 text-right text-blue-400 border-r border-slate-800">
                    {totals[vol].aamad}
                  </td>
                  <td className="px-1.5 py-2 text-right text-slate-300 border-r border-slate-800">
                    {totals[vol].total}
                  </td>
                  <td className="px-1.5 py-2 text-right text-blue-400 border-r border-slate-800">
                    {totals[vol].bikri}
                  </td>
                  <td className="px-1.5 py-2 text-right text-red-400 border-r border-slate-800">
                    {totals[vol].shesh}
                  </td>
                  <td className="px-1.5 py-2 text-right text-slate-400 border-r border-slate-800 text-[10px]">
                    -
                  </td>
                  <td className={`px-2 py-2 text-right text-amber-300 ${vIdx < volumes.length - 1 ? 'border-r-2 border-slate-600' : ''}`}>
                    ₹{totals[vol].value.toLocaleString()}
                  </td>
                </div>
              ))}
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Summary Banner below the register table */}
      <div className="bg-slate-800 text-white px-4 py-3 border-t border-slate-700 flex flex-wrap items-center justify-between gap-4 text-xs">
        <div className="flex flex-wrap items-center gap-6">
          <div className="flex items-center space-x-2">
            <span className="text-slate-400 font-devanagari">कुल बिकी बोतलें (Total Sold):</span>
            <span className="font-mono font-bold text-blue-400 text-sm">
              {totals[180].bikri + totals[375].bikri + totals[750].bikri}
            </span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-slate-400 font-devanagari">कुल शेष स्टॉक (Closing Stock):</span>
            <span className="font-mono font-bold text-red-400 text-sm">
              {totals[180].shesh + totals[375].shesh + totals[750].shesh}
            </span>
          </div>
        </div>

        <div className="flex items-center space-x-6">
          <div className="flex items-center space-x-2 bg-slate-900/80 px-3 py-1.5 rounded-lg border border-slate-700">
            <span className="text-slate-300 font-devanagari font-semibold">आज की कुल बिक्री (Today Sales):</span>
            <span className="font-mono font-extrabold text-emerald-400 text-base">
              ₹{grandTotalSalesValue.toLocaleString()}
            </span>
          </div>
          <div className="flex items-center space-x-2 bg-slate-900/80 px-3 py-1.5 rounded-lg border border-slate-700">
            <span className="text-slate-300 font-devanagari font-semibold">कुल स्टॉक कीमत (Stock Value):</span>
            <span className="font-mono font-extrabold text-amber-400 text-base">
              ₹{grandTotalStockValue.toLocaleString()}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
