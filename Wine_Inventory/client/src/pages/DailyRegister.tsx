import { useState, useEffect, useCallback } from 'react';
import { Shop, RegisterRowData, ValidationErrorItem } from '../types/index.js';
import { shopsApi, inventoryApi, reportsApi } from '../api/client.js';
import ShopDateSelector from '../components/ShopDateSelector.js';
import RegisterTable from '../components/RegisterTable.js';
import ColorLegend from '../components/ColorLegend.js';
import toast from 'react-hot-toast';
import { AlertCircle, CheckCircle2, ShieldAlert } from 'lucide-react';
import { format } from 'date-fns';

export default function DailyRegister() {
  const [shops, setShops] = useState<Shop[]>([]);
  const [selectedShopId, setSelectedShopId] = useState<number>(1);
  const [selectedDate, setSelectedDate] = useState<string>(format(new Date(), 'yyyy-MM-dd'));
  const [sheetRows, setSheetRows] = useState<RegisterRowData[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [validationErrors, setValidationErrors] = useState<ValidationErrorItem[]>([]);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

  // Load shops list
  useEffect(() => {
    shopsApi.getShops().then(res => {
      if (res.shops && res.shops.length > 0) {
        setShops(res.shops);
        setSelectedShopId(res.shops[0].id);
      }
    });
  }, []);

  // Load daily sheet when shop or date changes
  const loadSheet = useCallback(async () => {
    if (!selectedShopId) return;
    setLoading(true);
    setValidationErrors([]);
    try {
      const res = await inventoryApi.getSheet(selectedShopId, selectedDate);
      setSheetRows(res.sheet);
      setHasUnsavedChanges(false);
    } catch (err: any) {
      toast.error(err.message || 'दैनिक रजिस्टर लोड करने में असमर्थ');
    } finally {
      setLoading(false);
    }
  }, [selectedShopId, selectedDate]);

  useEffect(() => {
    loadSheet();
  }, [loadSheet]);

  // Handle live client-side edits in Aamad or Bikri
  const handleCellChange = (
    brandId: number,
    volumeMl: number,
    field: 'aamad' | 'bikri',
    value: number
  ) => {
    setHasUnsavedChanges(true);
    setSheetRows(prevRows =>
      prevRows.map(row => {
        if (row.brand_id !== brandId) return row;

        const currentCell = row.sizes[volumeMl];
        if (!currentCell) return row;

        const updatedAamad = field === 'aamad' ? value : currentCell.aamad;
        const updatedBikri = field === 'bikri' ? value : currentCell.bikri;
        const prevBal = currentCell.previous_balance ?? 0;

        // Core formulas
        const updatedTotal = prevBal + updatedAamad;
        const updatedRemaining = updatedTotal - updatedBikri;
        const updatedValue = Math.round(updatedRemaining * currentCell.rate * 100) / 100;

        return {
          ...row,
          sizes: {
            ...row.sizes,
            [volumeMl]: {
              ...currentCell,
              aamad: updatedAamad,
              total: updatedTotal,
              bikri: updatedBikri,
              remaining: updatedRemaining,
              value: updatedValue,
            },
          },
        };
      })
    );
  };

  // Save full sheet transactionally
  const handleSave = async () => {
    if (saving) return;

    // Collect all rows payload
    const payloadRows: { brand_id: number; size_id: number; aamad: number; bikri: number; rate: number }[] = [];
    const missingOpeningStock: string[] = [];

    sheetRows.forEach(row => {
      [180, 375, 750].forEach(vol => {
        const cell = row.sizes[vol];
        if (cell) {
          if (cell.previous_balance === null) {
            missingOpeningStock.push(`${row.brand_name} (${vol}ml)`);
          }
          payloadRows.push({
            brand_id: row.brand_id,
            size_id: cell.size_id,
            aamad: cell.aamad,
            bikri: cell.bikri,
            rate: cell.rate,
          });
        }
      });
    });

    if (missingOpeningStock.length > 0) {
      toast.error(
        `आरंभिक स्टॉक दर्ज नहीं है: ${missingOpeningStock.slice(0, 3).join(', ')}${
          missingOpeningStock.length > 3 ? '...' : ''
        }. कृपया पहले मास्टर डेटा में आरंभिक स्टॉक भरें।`,
        { duration: 6000 }
      );
      return;
    }

    setSaving(true);
    setValidationErrors([]);

    try {
      const res = await inventoryApi.saveSheet({
        shop_id: selectedShopId,
        date: selectedDate,
        rows: payloadRows,
      });

      toast.success(res.message || 'आज का हिसाब सफलतापूर्वक सेव हो गया!', {
        icon: <CheckCircle2 className="w-5 h-5 text-emerald-600" />,
        duration: 4000,
      });
      setHasUnsavedChanges(false);
      // Reload sheet from server to confirm persisted state
      loadSheet();
    } catch (err: any) {
      if (err.errors && Array.isArray(err.errors)) {
        setValidationErrors(err.errors);
        toast.error(`सेव विफल: ${err.errors.length} प्रविष्टियों में त्रुटि है।`, {
          icon: <ShieldAlert className="w-5 h-5 text-red-600" />,
          duration: 5000,
        });
      } else {
        toast.error(err.message || 'सेव करने में त्रुटि आई।');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleExportPdf = () => {
    const url = reportsApi.getPdfUrl(selectedShopId, selectedDate);
    window.open(url, '_blank');
  };

  return (
    <div className="space-y-3">
      {/* Selector & Actions */}
      <ShopDateSelector
        shops={shops}
        selectedShopId={selectedShopId}
        onShopChange={setSelectedShopId}
        selectedDate={selectedDate}
        onDateChange={setSelectedDate}
        onRefresh={loadSheet}
        onSave={handleSave}
        onExportPdf={handleExportPdf}
        saving={saving}
        hasUnsavedChanges={hasUnsavedChanges}
      />

      {/* Color Code Legend */}
      <ColorLegend />

      {/* Validation Errors Notice (if any) */}
      {validationErrors.length > 0 && (
        <div className="p-4 bg-red-50 border-l-4 border-red-600 rounded-r-lg text-red-800 text-xs">
          <div className="flex items-center space-x-2 font-bold font-devanagari text-sm text-red-900 mb-1">
            <AlertCircle className="w-5 h-5 text-red-600" />
            <span>सेव करने में निम्नलिखित त्रुटियां पाई गईं (कोई भी डेटा सेव नहीं हुआ):</span>
          </div>
          <ul className="list-disc list-inside space-y-0.5 ml-2 mt-1">
            {validationErrors.map((err, i) => (
              <li key={i}>{err.message}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Loading state / Table */}
      {loading ? (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-12 flex flex-col items-center justify-center space-y-3">
          <div className="w-10 h-10 border-4 border-red-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 font-devanagari font-medium text-sm">
            रजिस्टर डेटा लोड हो रहा है...
          </p>
        </div>
      ) : (
        <RegisterTable
          rows={sheetRows}
          onCellChange={handleCellChange}
          validationErrors={validationErrors}
        />
      )}
    </div>
  );
}
