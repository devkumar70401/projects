import React, { useState } from 'react';
import { Filter, LayoutGrid, List, Plus, MoreVertical, Trash2, Edit3, BookOpen, Layers } from 'lucide-react';
import { Notebook } from '../types';

interface ShelfViewProps {
  notebooks: Notebook[];
  onSelectNotebook: (notebook: Notebook) => void;
  onOpenNewNotebookModal: () => void;
  onDeleteNotebook: (notebookId: string) => void;
}

export const ShelfView: React.FC<ShelfViewProps> = ({
  notebooks,
  onSelectNotebook,
  onOpenNewNotebookModal,
  onDeleteNotebook,
}) => {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);
  const [filterQuery, setFilterQuery] = useState('');
  const [showFilterInput, setShowFilterInput] = useState(false);

  const filteredNotebooks = notebooks.filter((nb) =>
    nb.title.toLowerCase().includes(filterQuery.toLowerCase()) ||
    (nb.description && nb.description.toLowerCase().includes(filterQuery.toLowerCase()))
  );

  return (
    <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 relative min-h-screen pb-32">
      {/* Header */}
      <header className="mb-10 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="text-[36px] md:text-[40px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] tracking-tight font-inter">
            Shelf
          </h2>
          <p className="text-[#414846] dark:text-[#c1c8c5] text-[16px] mt-1 font-inter">
            Your collected notebooks and research.
          </p>
        </div>

        {/* Contextual Actions */}
        <div className="flex items-center gap-2 self-start md:self-auto">
          {showFilterInput && (
            <input
              type="text"
              placeholder="Filter notebooks..."
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
              className="px-3 py-1.5 text-sm bg-white dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-full outline-none focus:ring-2 focus:ring-[#264a42] text-[#1a1c1b] dark:text-[#f1f1ef]"
              autoFocus
            />
          )}
          <button
            id="shelf-filter-btn"
            onClick={() => setShowFilterInput(!showFilterInput)}
            className={`p-2.5 rounded-full text-[#414846] dark:text-[#c1c8c5] hover:bg-[#e8e8e6] dark:hover:bg-[#2f3130] transition-colors ${
              showFilterInput ? 'bg-[#e8e8e6] dark:bg-[#2f3130] text-[#264a42]' : ''
            }`}
            title="Filter notebooks"
            aria-label="Filter"
          >
            <Filter className="w-5 h-5" />
          </button>
          <button
            id="shelf-view-toggle-btn"
            onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
            className="p-2.5 rounded-full text-[#414846] dark:text-[#c1c8c5] hover:bg-[#e8e8e6] dark:hover:bg-[#2f3130] transition-colors"
            title={viewMode === 'grid' ? 'Switch to list view' : 'Switch to grid view'}
            aria-label="Toggle grid or list view"
          >
            {viewMode === 'grid' ? <LayoutGrid className="w-5 h-5" /> : <List className="w-5 h-5" />}
          </button>
        </div>
      </header>

      {/* Empty State */}
      {filteredNotebooks.length === 0 && (
        <div className="flex flex-col items-center justify-center py-24 text-center">
          <Layers className="w-12 h-12 text-[#717976] mb-3 opacity-60" />
          <p className="text-lg font-medium text-[#1a1c1b] dark:text-[#f1f1ef]">No notebooks found</p>
          <p className="text-sm text-[#717976] mt-1">Create a new notebook to start organizing your documents.</p>
          <button
            onClick={onOpenNewNotebookModal}
            className="mt-6 px-5 py-2.5 bg-[#264a42] text-white rounded-lg font-medium text-sm hover:bg-[#31685a] transition-colors"
          >
            + Create Notebook
          </button>
        </div>
      )}

      {/* Grid View */}
      {viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 pb-12">
          {filteredNotebooks.map((notebook) => {
            // Calculate total pages from sections and docs
            const calculatedPages = notebook.sections.reduce(
              (acc, sec) => acc + sec.documents.reduce((dAcc, d) => dAcc + d.totalPages, 0),
              0
            ) || notebook.pagesCount;

            const isMenuOpen = activeMenuId === notebook.id;

            return (
              <div
                key={notebook.id}
                id={`notebook-card-${notebook.id}`}
                className="group relative flex flex-col"
              >
                {/* Book Cover Card */}
                <div
                  onClick={() => onSelectNotebook(notebook)}
                  style={{ backgroundColor: notebook.color }}
                  className="aspect-[3/4] rounded-lg shadow-sm overflow-hidden relative cursor-pointer transition-all duration-300 group-hover:scale-[1.02] group-hover:shadow-md border border-[#c1c8c5]/30 flex flex-col justify-end p-5 select-none"
                >
                  {/* Realistic spine shading */}
                  <div className="absolute inset-0 spine-shadow pointer-events-none" />
                  <div className="absolute top-0 right-0 bottom-0 w-2 spine-shadow-right pointer-events-none opacity-40" />

                  {/* Notebook Title & Meta */}
                  <div className="relative z-10">
                    <h3
                      className="text-[19px] font-semibold tracking-tight line-clamp-3 leading-snug font-inter"
                      style={{ color: notebook.textColor || '#ffffff' }}
                    >
                      {notebook.title}
                    </h3>
                    {notebook.description && (
                      <p
                        className="text-[12px] opacity-80 mt-1 line-clamp-2 font-inter"
                        style={{ color: notebook.textColor || '#ffffff' }}
                      >
                        {notebook.description}
                      </p>
                    )}
                  </div>
                </div>

                {/* Footer Meta Row */}
                <div className="mt-3 flex justify-between items-center px-1">
                  <p className="text-[13px] font-mono text-[#414846] dark:text-[#c1c8c5]">
                    {calculatedPages} {calculatedPages === 1 ? 'page' : 'pages'}
                  </p>

                  <div className="relative">
                    <button
                      id={`notebook-menu-btn-${notebook.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveMenuId(isMenuOpen ? null : notebook.id);
                      }}
                      className="text-[#414846] dark:text-[#c1c8c5] opacity-80 group-hover:opacity-100 transition-opacity p-1 hover:bg-[#eeeeec] dark:hover:bg-[#2f3130] rounded"
                      aria-label="Notebook options"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    {/* Context Menu Dropdown */}
                    {isMenuOpen && (
                      <>
                        <div
                          className="fixed inset-0 z-20"
                          onClick={() => setActiveMenuId(null)}
                        />
                        <div className="absolute right-0 bottom-full mb-1 w-44 bg-white dark:bg-[#2f3130] rounded-lg shadow-lg border border-[#c1c8c5]/40 py-1.5 z-30 text-sm">
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              onSelectNotebook(notebook);
                            }}
                            className="w-full px-3 py-2 text-left flex items-center gap-2 text-[#1a1c1b] dark:text-[#f1f1ef] hover:bg-[#f4f4f2] dark:hover:bg-[#383c3a]"
                          >
                            <BookOpen className="w-4 h-4 text-[#3e6259]" />
                            <span>Open Notebook</span>
                          </button>
                          <button
                            onClick={() => {
                              setActiveMenuId(null);
                              onDeleteNotebook(notebook.id);
                            }}
                            className="w-full px-3 py-2 text-left flex items-center gap-2 text-[#ba1a1a] hover:bg-[#ffdad6]/40"
                          >
                            <Trash2 className="w-4 h-4" />
                            <span>Delete</span>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* List View */
        <div className="flex flex-col gap-3 pb-12">
          {filteredNotebooks.map((notebook) => (
            <div
              key={notebook.id}
              onClick={() => onSelectNotebook(notebook)}
              className="flex items-center justify-between p-4 bg-white dark:bg-[#242726] hover:bg-[#f4f4f2] dark:hover:bg-[#2f3130] rounded-lg transition-colors border border-[#c1c8c5]/30 cursor-pointer shadow-sm group"
            >
              <div className="flex items-center gap-4">
                <div
                  className="w-10 h-14 rounded shadow-sm border border-black/10 shrink-0 relative overflow-hidden"
                  style={{ backgroundColor: notebook.color }}
                >
                  <div className="absolute inset-0 spine-shadow" />
                </div>
                <div>
                  <h3 className="text-base font-semibold text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42] transition-colors">
                    {notebook.title}
                  </h3>
                  <p className="text-xs text-[#717976] mt-0.5">
                    {notebook.sections.length} {notebook.sections.length === 1 ? 'section' : 'sections'} • Updated {notebook.updatedAt}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-xs font-mono text-[#717976]">
                  {notebook.pagesCount} pages
                </span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteNotebook(notebook.id);
                  }}
                  className="opacity-0 group-hover:opacity-100 p-2 text-[#717976] hover:text-[#ba1a1a] transition-all"
                  title="Delete notebook"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Floating Action Button: + New Notebook */}
      <button
        id="fab-new-notebook"
        onClick={onOpenNewNotebookModal}
        className="fixed bottom-8 right-8 z-40 flex items-center gap-2.5 bg-[#264a42] text-white px-6 py-4 rounded-xl shadow-[0_8px_24px_rgba(31,29,27,0.18)] hover:bg-[#31685a] active:scale-95 transition-all duration-200"
      >
        <Plus className="w-5 h-5" />
        <span className="font-semibold text-[15px] font-inter">New Notebook</span>
      </button>
    </div>
  );
};
