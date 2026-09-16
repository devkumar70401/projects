import React from 'react';
import { History, BookOpen, Clock, ArrowRight, CheckCircle } from 'lucide-react';
import { Notebook, DocumentItem } from '../types';

interface RecentViewProps {
  notebooks: Notebook[];
  onSelectDocument: (doc: DocumentItem, notebook: Notebook) => void;
}

export const RecentView: React.FC<RecentViewProps> = ({ notebooks, onSelectDocument }) => {
  // Collect all documents with their parent notebook
  const recentDocs: { doc: DocumentItem; notebook: Notebook }[] = [];

  notebooks.forEach((nb) => {
    nb.sections.forEach((sec) => {
      sec.documents.forEach((doc) => {
        recentDocs.push({ doc, notebook: nb });
      });
    });
  });

  return (
    <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 min-h-screen">
      <header className="mb-8">
        <h2 className="text-[36px] font-bold text-[#1a1c1b] dark:text-[#f1f1ef] tracking-tight font-inter">
          Recent
        </h2>
        <p className="text-[#414846] dark:text-[#c1c8c5] text-[16px] mt-1 font-inter">
          Jump straight back into your reading flow.
        </p>
      </header>

      {recentDocs.length === 0 ? (
        <div className="text-center py-24 text-[#717976]">
          <History className="w-12 h-12 mx-auto mb-3 opacity-50" />
          <p className="text-base font-semibold">No recent reading history yet</p>
          <p className="text-xs mt-1">Open any document on your shelf to track your progress here.</p>
        </div>
      ) : (
        <div className="max-w-4xl space-y-4 pb-20">
          {recentDocs.map(({ doc, notebook }) => (
            <div
              key={doc.id}
              onClick={() => onSelectDocument(doc, notebook)}
              className="p-5 bg-white dark:bg-[#242726] rounded-xl border border-[#c1c8c5]/40 hover:border-[#264a42] hover:shadow-md transition-all cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 group"
            >
              <div className="flex items-start gap-4">
                <div
                  className="w-3 h-14 rounded-full shrink-0"
                  style={{ backgroundColor: notebook.color }}
                  title={notebook.title}
                />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-semibold text-[#717976] dark:text-[#a8cfc3] uppercase">
                      {notebook.title}
                    </span>
                    <span className="text-xs text-[#c1c8c5]">•</span>
                    <span className="text-xs text-[#717976] flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" /> {doc.lastRead}
                    </span>
                  </div>
                  <h3 className="text-lg font-semibold text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42] dark:group-hover:text-[#a8cfc3] transition-colors mt-0.5">
                    {doc.title}
                  </h3>
                  <div className="flex items-center gap-3 mt-2 text-xs font-mono text-[#717976]">
                    <span>Page {doc.currentPage} of {doc.totalPages}</span>
                    <div className="w-24 h-1.5 bg-[#eeeeec] dark:bg-[#414846] rounded-full overflow-hidden">
                      <div
                        className="h-full bg-[#264a42] dark:bg-[#a8cfc3]"
                        style={{ width: `${doc.progressPercentage}%` }}
                      />
                    </div>
                    <span>{doc.progressPercentage}%</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3 self-end md:self-center">
                {doc.progressPercentage === 100 ? (
                  <span className="px-3 py-1 bg-[#264a42]/10 text-[#264a42] dark:text-[#a8cfc3] rounded-full text-xs font-semibold flex items-center gap-1">
                    <CheckCircle className="w-3.5 h-3.5" /> Completed
                  </span>
                ) : (
                  <button className="px-4 py-2 bg-[#f4f4f2] dark:bg-[#2f3130] group-hover:bg-[#264a42] group-hover:text-white rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5">
                    <span>Resume</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
