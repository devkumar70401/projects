import React, { useState } from 'react';
import { Bookmark, BookOpen, ExternalLink, Trash2 } from 'lucide-react';
import { Notebook, DocumentItem, Bookmark as BookmarkType } from '../types';

interface BookmarksViewProps {
  notebooks: Notebook[];
  onSelectDocumentAndPage: (doc: DocumentItem, notebook: Notebook, page: number) => void;
  onRemoveBookmark: (docId: string, pageNumber: number) => void;
}

export const BookmarksView: React.FC<BookmarksViewProps> = ({
  notebooks,
  onSelectDocumentAndPage,
  onRemoveBookmark,
}) => {
  const [selectedNotebookFilter, setSelectedNotebookFilter] = useState<string>('all');

  // Gather all bookmarks with their parent doc and notebook
  const allBookmarks: {
    bookmark: BookmarkType;
    doc: DocumentItem;
    notebook: Notebook;
  }[] = [];

  notebooks.forEach((nb) => {
    nb.sections.forEach((sec) => {
      sec.documents.forEach((doc) => {
        doc.bookmarks.forEach((bm) => {
          allBookmarks.push({ bookmark: bm, doc, notebook: nb });
        });
      });
    });
  });

  const filteredBookmarks = allBookmarks.filter((item) => {
    if (selectedNotebookFilter === 'all') return true;
    return item.notebook.id === selectedNotebookFilter;
  });

  return (
    <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 min-h-screen">
      <header className="mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="text-[36px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] tracking-tight font-inter">
            Bookmarks
          </h2>
          <p className="text-[#414846] dark:text-[#c1c8c5] text-[16px] mt-1 font-inter">
            All your saved anchors, passages, and notes.
          </p>
        </div>

        {/* Notebook Filter */}
        <div className="flex items-center gap-2">
          <label className="text-xs font-mono text-[#717976]">Filter:</label>
          <select
            value={selectedNotebookFilter}
            onChange={(e) => setSelectedNotebookFilter(e.target.value)}
            className="px-3 py-1.5 bg-white dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-lg text-xs font-medium text-[#1a1c1b] dark:text-[#f1f1ef] outline-none"
          >
            <option value="all">All Notebooks ({allBookmarks.length})</option>
            {notebooks.map((nb) => (
              <option key={nb.id} value={nb.id}>
                {nb.title}
              </option>
            ))}
          </select>
        </div>
      </header>

      {filteredBookmarks.length === 0 ? (
        <div className="text-center py-24 text-[#717976]">
          <Bookmark className="w-12 h-12 mx-auto mb-3 opacity-40 text-[#264a42]" />
          <p className="text-base font-semibold">No bookmarks found</p>
          <p className="text-xs mt-1">Tap the bookmark ribbon while reading any paper to pin key insights.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-5xl pb-20">
          {filteredBookmarks.map(({ bookmark, doc, notebook }) => (
            <div
              key={`${doc.id}-${bookmark.id}`}
              onClick={() => onSelectDocumentAndPage(doc, notebook, bookmark.pageNumber)}
              className="p-5 bg-white dark:bg-[#242726] rounded-xl border border-[#c1c8c5]/40 hover:border-[#264a42] hover:shadow-md transition-all cursor-pointer flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ backgroundColor: notebook.color }}
                    />
                    <span className="text-[11px] font-mono font-semibold text-[#717976] dark:text-[#a8cfc3] uppercase truncate">
                      {notebook.title} / {doc.title}
                    </span>
                  </div>
                  <span className="text-[11px] font-mono font-bold bg-[#264a42]/10 text-[#264a42] dark:text-[#a8cfc3] px-2 py-0.5 rounded">
                    Page {bookmark.pageNumber}
                  </span>
                </div>

                <h3 className="text-base font-bold text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42] dark:group-hover:text-[#a8cfc3] transition-colors mb-2 font-inter">
                  {bookmark.title}
                </h3>

                <p className="text-xs text-[#717976] dark:text-[#c1c8c5] leading-relaxed line-clamp-3 font-literata italic">
                  "{bookmark.snippet}"
                </p>
              </div>

              <div className="mt-4 pt-3 border-t border-[#c1c8c5]/30 flex items-center justify-between text-xs text-[#717976]">
                <span>{bookmark.timestamp}</span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onRemoveBookmark(doc.id, bookmark.pageNumber);
                    }}
                    className="p-1 text-[#717976] hover:text-[#ba1a1a] rounded hover:bg-black/5"
                    title="Delete bookmark"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                  <span className="text-[#264a42] dark:text-[#a8cfc3] font-semibold flex items-center gap-1 group-hover:underline">
                    Jump <ExternalLink className="w-3 h-3" />
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
