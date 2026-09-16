import React, { useState } from 'react';
import { X, Layers } from 'lucide-react';
import { Notebook } from '../types';

interface NewNotebookModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateNotebook: (notebook: Partial<Notebook>) => void;
}

const COLOR_PALETTES = [
  { name: 'Terracotta', color: '#7a5148', text: '#ffffff' },
  { name: 'Forest Green', color: '#31685a', text: '#ffffff' },
  { name: 'Dusty Sage', color: '#3e6259', text: '#c3ebdf' },
  { name: 'Warm Plum', color: '#5f3a32', text: '#ffffff' },
  { name: 'Slate Indigo', color: '#3b4d61', text: '#ffffff' },
  { name: 'Ochre Gold', color: '#8a622a', text: '#ffffff' },
];

export const NewNotebookModal: React.FC<NewNotebookModalProps> = ({
  isOpen,
  onClose,
  onCreateNotebook,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedColor, setSelectedColor] = useState(COLOR_PALETTES[0]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onCreateNotebook({
      title: title.trim(),
      description: description.trim(),
      color: selectedColor.color,
      textColor: selectedColor.text,
      pagesCount: 0,
      updatedAt: 'Just now',
      sections: [
        {
          id: `sec-${Date.now()}`,
          name: 'General Notes',
          isDefault: true,
          documents: [],
        },
      ],
    });

    setTitle('');
    setDescription('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs">
      <div className="bg-white dark:bg-[#242726] w-full max-w-md rounded-2xl shadow-2xl border border-[#c1c8c5]/40 p-6 animate-fade-in">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-[#264a42] dark:text-[#a8cfc3]" />
            <h3 className="text-xl font-bold text-[#1a1c1b] dark:text-[#f1f1ef] font-inter">
              New Notebook
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full text-[#717976] hover:bg-black/5"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] uppercase tracking-wider mb-1.5">
              Notebook Title
            </label>
            <input
              type="text"
              placeholder="e.g., Quantum Computing Notes"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#f9f9f7] dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-xl text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none focus:ring-2 focus:ring-[#264a42]"
              required
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] uppercase tracking-wider mb-1.5">
              Description (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g., Research papers, notes, and algorithms"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#f9f9f7] dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-xl text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none focus:ring-2 focus:ring-[#264a42]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] uppercase tracking-wider mb-2">
              Cover Palette
            </label>
            <div className="grid grid-cols-6 gap-2">
              {COLOR_PALETTES.map((palette) => (
                <button
                  key={palette.name}
                  type="button"
                  onClick={() => setSelectedColor(palette)}
                  style={{ backgroundColor: palette.color }}
                  className={`h-10 rounded-lg transition-transform relative ${
                    selectedColor.name === palette.name
                      ? 'scale-110 ring-2 ring-[#264a42] ring-offset-2 shadow-sm'
                      : 'hover:scale-105'
                  }`}
                  title={palette.name}
                />
              ))}
            </div>
          </div>

          {/* Preview Card */}
          <div className="p-3 bg-[#f4f4f2] dark:bg-[#2f3130] rounded-xl flex items-center gap-3">
            <div
              className="w-8 h-11 rounded shadow-xs relative overflow-hidden shrink-0"
              style={{ backgroundColor: selectedColor.color }}
            >
              <div className="absolute inset-0 spine-shadow" />
            </div>
            <div className="truncate">
              <span className="text-xs font-bold text-[#1a1c1b] dark:text-[#f1f1ef] block truncate">
                {title || 'Untitled Notebook'}
              </span>
              <span className="text-[11px] text-[#717976]">{selectedColor.name} Cover</span>
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-[#c1c8c5]/40">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-[#717976] hover:bg-[#eeeeec] dark:hover:bg-[#2f3130] rounded-lg"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 text-xs font-semibold bg-[#264a42] text-white hover:bg-[#31685a] rounded-lg transition-colors"
            >
              Create Notebook
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
