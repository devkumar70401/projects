import { useState, useEffect } from 'react';
import { Shop, Brand } from '../types/index.js';
import { shopsApi, brandsApi, reportsApi } from '../api/client.js';
import toast from 'react-hot-toast';
import {
  FileSpreadsheet,
  FileText,
  Download,
} from 'lucide-react';
import { format, subDays } from 'date-fns';

export default function Reports() {
  const [shops, setShops] = useState<Shop[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);

  // Filter criteria
  const [selectedShopId, setSelectedShopId] = useState<number | undefined>(undefined);
  const [selectedBrandId, setSelectedBrandId] = useState<number | undefined>(undefined);
  const [selectedSizeId, setSelectedSizeId] = useState<number | undefined>(undefined);
  const [startDate, setStartDate] = useState<string>(format(subDays(new Date(), 7), 'yyyy-MM-dd'));
  const [endDate, setEndDate] = useState<string>(format(new Date(), 'yyyy-MM-dd'));

  const [records, setRecords] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    shopsApi.getShops().then(res => setShops(res.shops));
    brandsApi.getBrands().then(res => setBrands(res.brands));
  }, []);

  const loadReportData = async () => {
    setLoading(true);
    try {
      const res = await reportsApi.getData({
        shop_id: selectedShopId,
        brand_id: selectedBrandId,
        size_id: selectedSizeId,
        start_date: startDate,
        end_date: endDate,
      });
      setRecords(res.records);
    } catch (err: any) {
      toast.error(err.message || 'रिपोर्ट डेटा लोड करने में असमर्थ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReportData();
  }, [selectedShopId, selectedBrandId, selectedSizeId, startDate, endDate]);

  const handleExportExcel = () => {
    const url = reportsApi.getExcelUrl({
      shop_id: selectedShopId,
      brand_id: selectedBrandId,
      size_id: selectedSizeId,
      start_date: startDate,
      end_date: endDate,
    });
    window.open(url, '_blank');
  };

  const handleExportCsv = () => {
    const url = reportsApi.getCsvUrl({
      shop_id: selectedShopId,
      brand_id: selectedBrandId,
      size_id: selectedSizeId,
      start_date: startDate,
      end_date: endDate,
    });
    window.open(url, '_blank');
  };

  const handleExportPdf = () => {
    const shopId = selectedShopId || (shops.length > 0 ? shops[0].id : 1);
    const url = reportsApi.getPdfUrl(shopId, endDate);
    window.open(url, '_blank');
  };

  // Summary calculations
  const totalBikri = records.reduce((sum, r) => sum + (r.bikri || 0), 0);
  const totalSalesValue = records.reduce((sum, r) => sum + (r.bikri || 0) * (r.rate || 0), 0);

  return (
    <div className="space-y-4">
      {/* Header & Controls */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 space-y-4">
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-extrabold text-slate-800 font-devanagari">
              रिपोर्ट्स व डेटा एक्सपोर्ट (Reports & Export)
            </h1>
            <p className="text-xs text-slate-500">
              दुकान, ब्रांड और तिथि अनुसार ऐतिहासिक रिकॉर्ड देखें और PDF/Excel/CSV में डाउनलोड करें।
            </p>
          </div>

          {/* Export Buttons */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={handleExportPdf}
              className="flex items-center space-x-1.5 px-3 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-semibold font-devanagari transition-colors shadow-sm"
            >
              <FileText className="w-4 h-4" />
              <span>रजिस्टर PDF</span>
            </button>

            <button
              onClick={handleExportExcel}
              className="flex items-center space-x-1.5 px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold font-devanagari transition-colors shadow-sm"
            >
              <FileSpreadsheet className="w-4 h-4" />
              <span>Excel (.xlsx)</span>
            </button>

            <button
              onClick={handleExportCsv}
              className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-xs font-semibold font-devanagari transition-colors shadow-sm"
            >
              <Download className="w-4 h-4" />
              <span>CSV</span>
            </button>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 pt-2 border-t border-slate-100">
          {/* Shop */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 font-devanagari mb-1">
              दुकान (Shop)
            </label>
            <select
              value={selectedShopId || ''}
              onChange={e => setSelectedShopId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold font-devanagari"
            >
              <option value="">सभी दुकानें (All Shops)</option>
              {shops.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Brand */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 font-devanagari mb-1">
              ब्रांड (Brand)
            </label>
            <select
              value={selectedBrandId || ''}
              onChange={e => setSelectedBrandId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold font-devanagari"
            >
              <option value="">सभी ब्रांड्स (All Brands)</option>
              {brands.map(b => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>

          {/* Bottle Size */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 font-devanagari mb-1">
              साइज़ (Size)
            </label>
            <select
              value={selectedSizeId || ''}
              onChange={e => setSelectedSizeId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold"
            >
              <option value="">सभी साइज़ (All Sizes)</option>
              <option value="1">180 ml (क्वार्टर)</option>
              <option value="2">375 ml (अद्धा)</option>
              <option value="3">750 ml (बोतल)</option>
            </select>
          </div>

          {/* Start Date */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 font-devanagari mb-1">
              प्रारंभिक तिथि (From)
            </label>
            <input
              type="date"
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-mono font-bold"
            />
          </div>

          {/* End Date */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 font-devanagari mb-1">
              अंतिम तिथि (To)
            </label>
            <input
              type="date"
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-xs font-mono font-bold"
            />
          </div>
        </div>
      </div>

      {/* Summary Chips */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900 text-white px-4 py-2.5 rounded-xl text-xs">
        <div className="flex items-center space-x-4">
          <span>
            कुल रिकॉर्ड्स:{' '}
            <strong className="font-mono text-amber-300">{records.length}</strong>
          </span>
          <span>
            कुल बिक्री बोतलें:{' '}
            <strong className="font-mono text-blue-400">{totalBikri}</strong>
          </span>
        </div>
        <div>
          <span>
            कुल बिक्री मूल्य:{' '}
            <strong className="font-mono text-emerald-400 text-sm">
              ₹{totalSalesValue.toLocaleString()}
            </strong>
          </span>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 font-devanagari">
            डेटा लोड हो रहा है...
          </div>
        ) : records.length === 0 ? (
          <div className="p-12 text-center text-slate-400 font-devanagari text-sm">
            चुने गए फ़िल्टर के अनुसार कोई रिकॉर्ड उपलब्ध नहीं है।
          </div>
        ) : (
          <div className="overflow-x-auto max-h-[600px] overflow-y-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="sticky top-0 bg-slate-800 text-white z-10 font-devanagari uppercase text-[11px]">
                <tr>
                  <th className="px-3 py-2">दिनांक</th>
                  <th className="px-3 py-2">दुकान</th>
                  <th className="px-3 py-2">ब्रांड</th>
                  <th className="px-2 py-2">साइज़</th>
                  <th className="px-2 py-2 text-right text-red-400">पि.शेष</th>
                  <th className="px-2 py-2 text-right text-blue-400">आमद</th>
                  <th className="px-2 py-2 text-right text-slate-300">योग</th>
                  <th className="px-2 py-2 text-right text-blue-400">बिक्री</th>
                  <th className="px-2 py-2 text-right text-red-400">शेष</th>
                  <th className="px-2 py-2 text-right">दर</th>
                  <th className="px-3 py-2 text-right text-amber-300">स्टॉक कीमत</th>
                  <th className="px-3 py-2 text-right text-emerald-400">बिक्री कीमत</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                {records.map((r, idx) => {
                  const salesValue = (r.bikri || 0) * (r.rate || 0);
                  return (
                    <tr key={r.id || idx} className="hover:bg-slate-50">
                      <td className="px-3 py-2 font-bold text-slate-700">{r.record_date}</td>
                      <td className="px-3 py-2 font-medium text-slate-800 font-devanagari">{r.shop_name}</td>
                      <td className="px-3 py-2 font-semibold text-slate-900 font-devanagari">{r.brand_name}</td>
                      <td className="px-2 py-2 text-slate-600">{r.volume_ml} ml</td>
                      <td className="px-2 py-2 text-right text-red-600 font-bold">{r.previous_balance}</td>
                      <td className="px-2 py-2 text-right text-blue-600 font-bold">{r.aamad}</td>
                      <td className="px-2 py-2 text-right text-slate-700 font-semibold">{r.total}</td>
                      <td className="px-2 py-2 text-right text-blue-600 font-bold">{r.bikri}</td>
                      <td className="px-2 py-2 text-right text-red-600 font-bold">{r.remaining}</td>
                      <td className="px-2 py-2 text-right text-slate-700">₹{r.rate}</td>
                      <td className="px-3 py-2 text-right font-bold text-slate-800">
                        ₹{(r.value || 0).toLocaleString()}
                      </td>
                      <td className="px-3 py-2 text-right font-bold text-emerald-700">
                        ₹{salesValue.toLocaleString()}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
