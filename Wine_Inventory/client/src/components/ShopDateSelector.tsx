import { Shop } from '../types/index.js';
import { Store, Calendar, ChevronLeft, ChevronRight, RefreshCw, Save, FileText } from 'lucide-react';
import { format, subDays, addDays } from 'date-fns';

interface Props {
  shops: Shop[];
  selectedShopId: number;
  onShopChange: (shopId: number) => void;
  selectedDate: string;
  onDateChange: (date: string) => void;
  onRefresh: () => void;
  onSave: () => void;
  onExportPdf: () => void;
  saving: boolean;
  hasUnsavedChanges?: boolean;
}

export default function ShopDateSelector({
  shops,
  selectedShopId,
  onShopChange,
  selectedDate,
  onDateChange,
  onRefresh,
  onSave,
  onExportPdf,
  saving,
  hasUnsavedChanges = false,
}: Props) {
  const currentDate = new Date(selectedDate || new Date().toISOString().split('T')[0]);
  const todayStr = format(new Date(), 'yyyy-MM-dd');
  const yesterdayStr = format(subDays(new Date(), 1), 'yyyy-MM-dd');

  const handlePrevDay = () => {
    const prev = format(subDays(currentDate, 1), 'yyyy-MM-dd');
    onDateChange(prev);
  };

  const handleNextDay = () => {
    const next = format(addDays(currentDate, 1), 'yyyy-MM-dd');
    if (next <= todayStr) {
      onDateChange(next);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-3 sm:p-4 mb-4">
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
        {/* Shop & Date Selection */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Shop Selector */}
          <div className="flex items-center space-x-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-300">
            <Store className="w-4 h-4 text-red-600 shrink-0" />
            <label className="text-xs font-semibold text-slate-600 font-devanagari">दुकान:</label>
            <select
              value={selectedShopId}
              onChange={e => onShopChange(Number(e.target.value))}
              className="bg-transparent text-sm font-bold text-slate-800 focus:outline-none cursor-pointer font-devanagari"
            >
              {shops.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Date Selector with quick toggles */}
          <div className="flex items-center space-x-1.5 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-300">
            <Calendar className="w-4 h-4 text-red-600 shrink-0" />
            <label className="text-xs font-semibold text-slate-600 font-devanagari">दिनांक:</label>
            <button
              onClick={handlePrevDay}
              title="पिछला दिन"
              className="p-1 hover:bg-slate-200 rounded text-slate-600 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <input
              type="date"
              value={selectedDate}
              max={todayStr}
              onChange={e => onDateChange(e.target.value)}
              className="bg-white border border-slate-300 rounded px-2 py-0.5 text-sm font-bold text-slate-800 focus:outline-none focus:ring-1 focus:ring-red-600"
            />
            <button
              onClick={handleNextDay}
              disabled={selectedDate >= todayStr}
              title="अगला दिन"
              className="p-1 hover:bg-slate-200 rounded text-slate-600 transition-colors disabled:opacity-30 disabled:hover:bg-transparent"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Quick Date Buttons */}
          <div className="flex items-center space-x-1 text-xs">
            <button
              onClick={() => onDateChange(todayStr)}
              className={`px-2.5 py-1.5 rounded-md font-medium transition-colors font-devanagari ${
                selectedDate === todayStr
                  ? 'bg-slate-800 text-white font-bold'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              आज (Today)
            </button>
            <button
              onClick={() => onDateChange(yesterdayStr)}
              className={`px-2.5 py-1.5 rounded-md font-medium transition-colors font-devanagari ${
                selectedDate === yesterdayStr
                  ? 'bg-slate-800 text-white font-bold'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              कल (Yesterday)
            </button>
          </div>
        </div>

        {/* Actions (Refresh, PDF, Save) */}
        <div className="flex items-center space-x-2">
          <button
            onClick={onRefresh}
            title="रिफ्रेश करें"
            className="p-2 text-slate-600 hover:bg-slate-100 border border-slate-300 rounded-lg transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={onExportPdf}
            title="प्रिंट / PDF डाउनलोड"
            className="flex items-center space-x-1.5 px-3 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-300 rounded-lg transition-colors font-devanagari"
          >
            <FileText className="w-4 h-4 text-red-600" />
            <span>रजिस्टर PDF</span>
          </button>

          <button
            onClick={onSave}
            disabled={saving}
            className={`flex items-center space-x-2 px-5 py-2 rounded-lg text-sm font-bold text-white shadow-md transition-all font-devanagari ${
              hasUnsavedChanges
                ? 'bg-emerald-600 hover:bg-emerald-700 ring-2 ring-emerald-400/50 animate-pulse'
                : 'bg-red-600 hover:bg-red-700'
            } disabled:opacity-50`}
          >
            <Save className="w-4 h-4" />
            <span>{saving ? 'सेव हो रहा है...' : 'हिसाब सेव करें (Save Sheet)'}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
