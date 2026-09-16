import React, { useState } from 'react';
import { Settings, Moon, Sun, BookOpen, RotateCcw, Download, Sparkles, Check } from 'lucide-react';
import { ReaderSettings, ThemeMode, Typeface } from '../types';

interface SettingsViewProps {
  onResetSampleData: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({ onResetSampleData }) => {
  const [defaultTheme, setDefaultTheme] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('notereader_settings');
    if (saved) {
      try {
        return JSON.parse(saved).theme || 'light';
      } catch (e) {}
    }
    return 'light';
  });

  const [defaultTypeface, setDefaultTypeface] = useState<Typeface>('Literata');
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSavePreferences = () => {
    const current = localStorage.getItem('notereader_settings');
    let parsed: ReaderSettings = {
      theme: defaultTheme,
      fontSize: 18,
      typeface: defaultTypeface,
      lineHeight: 'relaxed',
      marginWidth: 'standard',
    };
    if (current) {
      try {
        parsed = { ...JSON.parse(current), theme: defaultTheme, typeface: defaultTypeface };
      } catch (e) {}
    }
    localStorage.setItem('notereader_settings', JSON.stringify(parsed));
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2000);
  };

  const handleExportLibrary = () => {
    const data = localStorage.getItem('notereader_notebooks');
    const blob = new Blob([data || '{}'], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `notereader-library-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 min-h-screen">
      <header className="mb-8">
        <h2 className="text-[36px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] tracking-tight font-inter">
          Settings
        </h2>
        <p className="text-[#414846] dark:text-[#c1c8c5] text-[16px] mt-1 font-inter">
          Library configuration and reading preferences.
        </p>
      </header>

      <div className="max-w-2xl space-y-6 pb-20">
        {/* Default Reading Environment */}
        <div className="p-6 bg-white dark:bg-[#242726] rounded-2xl border border-[#c1c8c5]/40 shadow-xs space-y-5">
          <h3 className="text-base font-bold text-[#1a1c1b] dark:text-[#f1f1ef] flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-[#264a42] dark:text-[#a8cfc3]" />
            Default Reader Appearance
          </h3>

          <div>
            <label className="block text-xs font-mono font-semibold uppercase text-[#717976] mb-2">
              Default Palette
            </label>
            <div className="grid grid-cols-3 gap-3">
              {(['light', 'sepia', 'dark'] as ThemeMode[]).map((theme) => (
                <button
                  key={theme}
                  onClick={() => setDefaultTheme(theme)}
                  className={`py-3 px-3 rounded-xl border text-xs font-semibold uppercase tracking-wider transition-all capitalize ${
                    defaultTheme === theme
                      ? 'border-2 border-[#264a42] bg-[#264a42]/10 text-[#264a42] dark:text-[#a8cfc3]'
                      : 'border-[#c1c8c5]/50 text-[#717976] hover:bg-black/5'
                  }`}
                >
                  {theme}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-mono font-semibold uppercase text-[#717976] mb-2">
              Default Typeface
            </label>
            <div className="grid grid-cols-3 gap-3">
              {(['Literata', 'Inter', 'IBM Plex Sans'] as Typeface[]).map((tf) => (
                <button
                  key={tf}
                  onClick={() => setDefaultTypeface(tf)}
                  className={`py-2.5 px-3 rounded-xl border text-xs font-medium transition-all ${
                    defaultTypeface === tf
                      ? 'border-2 border-[#264a42] bg-[#264a42]/10 text-[#264a42] dark:text-[#a8cfc3] font-bold'
                      : 'border-[#c1c8c5]/50 text-[#717976] hover:bg-black/5'
                  }`}
                >
                  {tf}
                </button>
              ))}
            </div>
          </div>

          <button
            onClick={handleSavePreferences}
            className="w-full py-2.5 bg-[#264a42] text-white rounded-xl text-xs font-semibold hover:bg-[#31685a] transition-colors flex items-center justify-center gap-2"
          >
            {savedSuccess ? <Check className="w-4 h-4" /> : null}
            <span>{savedSuccess ? 'Preferences Saved!' : 'Save Reading Preferences'}</span>
          </button>
        </div>

        {/* Library Storage & Reset */}
        <div className="p-6 bg-white dark:bg-[#242726] rounded-2xl border border-[#c1c8c5]/40 shadow-xs space-y-4">
          <h3 className="text-base font-bold text-[#1a1c1b] dark:text-[#f1f1ef]">
            Library Data Management
          </h3>
          <p className="text-xs text-[#717976] leading-relaxed">
            Your reading positions, bookmarks, notes, and custom notebooks are stored in client persistence.
          </p>

          <div className="flex flex-col sm:flex-row gap-3 pt-2">
            <button
              onClick={handleExportLibrary}
              className="flex-1 py-2.5 px-4 bg-[#f4f4f2] dark:bg-[#2f3130] hover:bg-[#e8e8e6] text-[#1a1c1b] dark:text-[#f1f1ef] rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition-colors"
            >
              <Download className="w-4 h-4 text-[#717976]" />
              <span>Export Library JSON</span>
            </button>

            <button
              onClick={onResetSampleData}
              className="py-2.5 px-4 bg-[#ffdad6]/40 hover:bg-[#ffdad6] text-[#ba1a1a] rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Reset Sample Library</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
