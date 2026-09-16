import React, { useState } from 'react';
import { Search, BookOpen, FileText, ChevronRight, Hash } from 'lucide-react';
import { Notebook, DocumentItem } from '../types';

interface GlobalSearchViewProps {
  notebooks: Notebook[];
  onSelectDocument: (doc: DocumentItem, notebook: Notebook) => void;
  onSelectNotebook: (notebook: Notebook) => void;
}

export const GlobalSearchView: React.FC<GlobalSearchViewProps> = ({
  notebooks,
  onSelectDocument,
  onSelectNotebook,
}) => {
  const [query, setQuery] = useState('');

  const cleanQuery = query.toLowerCase().trim();

  // Search notebooks
  const matchedNotebooks = notebooks.filter((nb) =>
    nb.title.toLowerCase().includes(cleanQuery) ||
    (nb.description && nb.description.toLowerCase().includes(cleanQuery))
  );

  // Search documents & document text
  const matchedDocs: { doc: DocumentItem; notebook: Notebook; matchSnippet?: string }[] = [];

  notebooks.forEach((nb) => {
    nb.sections.forEach((sec) => {
      sec.documents.forEach((doc) => {
        const titleMatch = doc.title.toLowerCase().includes(cleanQuery);
        let snippetMatch = '';

        if (!titleMatch && cleanQuery.length > 2) {
          doc.content.sections.forEach((s) => {
            s.paragraphs.forEach((p) => {
              if (p.toLowerCase().includes(cleanQuery) && !snippetMatch) {
                snippetMatch = p;
              }
            });
          });
        }

        if (titleMatch || snippetMatch) {
          matchedDocs.push({
            doc,
            notebook: nb,
            matchSnippet: snippetMatch,
          });
        }
      });
    });
  });

  return (
    <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 min-h-screen">
      <header className="mb-8">
        <h2 className="text-[36px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] tracking-tight font-inter">
          Search
        </h2>
        <p className="text-[#414846] dark:text-[#c1c8c5] text-[16px] mt-1 font-inter">
          Find papers, markdown notes, code cells, or bookmarks.
        </p>

        <div className="mt-6 max-w-2xl relative">
          <Search className="w-5 h-5 text-[#717976] absolute left-4 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search keywords, titles, or concepts (e.g., 'attention', 'regression', 'models')..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3.5 bg-white dark:bg-[#242726] border border-[#c1c8c5] dark:border-[#414846] rounded-2xl text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none focus:ring-2 focus:ring-[#264a42] shadow-xs"
            autoFocus
          />
        </div>
      </header>

      {query.trim().length === 0 ? (
        <div className="max-w-2xl py-12 text-[#717976] space-y-6">
          <div>
            <span className="text-xs font-mono font-semibold uppercase tracking-wider block mb-3">
              Suggested Searches
            </span>
            <div className="flex flex-wrap gap-2">
              {['Transformers', 'Linear Regression', 'Architecture of Thought', 'Neural Networks', 'Attention'].map(
                (term) => (
                  <button
                    key={term}
                    onClick={() => setQuery(term)}
                    className="px-3.5 py-1.5 bg-white dark:bg-[#242726] border border-[#c1c8c5]/50 rounded-full text-xs font-medium hover:border-[#264a42] transition-colors"
                  >
                    {term}
                  </button>
                )
              )}
            </div>
          </div>
        </div>
      ) : (
        <div className="max-w-3xl space-y-8 pb-20">
          {/* Notebook Matches */}
          {matchedNotebooks.length > 0 && (
            <div>
              <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-[#717976] mb-3">
                Matching Notebooks ({matchedNotebooks.length})
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {matchedNotebooks.map((nb) => (
                  <div
                    key={nb.id}
                    onClick={() => onSelectNotebook(nb)}
                    className="p-4 bg-white dark:bg-[#242726] rounded-xl border border-[#c1c8c5]/40 hover:border-[#264a42] cursor-pointer flex items-center justify-between group transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className="w-4 h-10 rounded shadow-xs"
                        style={{ backgroundColor: nb.color }}
                      />
                      <div>
                        <h4 className="font-semibold text-sm text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42]">
                          {nb.title}
                        </h4>
                        <p className="text-xs text-[#717976]">{nb.pagesCount} pages</p>
                      </div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-[#717976] group-hover:translate-x-0.5 transition-transform" />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Document Matches */}
          <div>
            <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-[#717976] mb-3">
              Matching Documents & Passages ({matchedDocs.length})
            </h3>
            {matchedDocs.length === 0 ? (
              <p className="text-sm text-[#717976] py-4">No matching documents found.</p>
            ) : (
              <div className="space-y-3">
                {matchedDocs.map(({ doc, notebook, matchSnippet }) => (
                  <div
                    key={doc.id}
                    onClick={() => onSelectDocument(doc, notebook)}
                    className="p-4 bg-white dark:bg-[#242726] rounded-xl border border-[#c1c8c5]/40 hover:border-[#264a42] cursor-pointer transition-all group"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-[11px] font-mono text-[#717976] uppercase">
                        {notebook.title}
                      </span>
                      <span className="text-[11px] font-mono font-semibold text-[#264a42] dark:text-[#a8cfc3]">
                        {doc.totalPages} pages
                      </span>
                    </div>
                    <h4 className="text-base font-bold text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42] transition-colors">
                      {doc.title}
                    </h4>
                    {matchSnippet && (
                      <p className="text-xs text-[#717976] mt-2 line-clamp-2 italic bg-[#f9f9f7] dark:bg-[#2f3130] p-2.5 rounded-lg border border-[#c1c8c5]/30">
                        "...{matchSnippet}..."
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
