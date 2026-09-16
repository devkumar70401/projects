import React, { useState } from 'react';
import { 
  Home, 
  ChevronRight, 
  FileText, 
  FileCode, 
  BookOpen, 
  Plus, 
  MoreVertical, 
  CheckCircle, 
  Bookmark as BookmarkIcon,
  Trash2,
  ExternalLink,
  Upload
} from 'lucide-react';
import { Notebook, DocumentItem, DocType } from '../types';

interface NotebookDetailViewProps {
  notebook: Notebook;
  onBackToShelf: () => void;
  onSelectDocument: (doc: DocumentItem, notebook: Notebook) => void;
  onAddSection: (notebookId: string, sectionName: string) => void;
  onAddDocument: (notebookId: string, sectionId: string, doc: Partial<DocumentItem>) => void;
  onDeleteDocument: (notebookId: string, sectionId: string, docId: string) => void;
}

export const NotebookDetailView: React.FC<NotebookDetailViewProps> = ({
  notebook,
  onBackToShelf,
  onSelectDocument,
  onAddSection,
  onAddDocument,
  onDeleteDocument,
}) => {
  const [activeSectionId, setActiveSectionId] = useState<string>(
    notebook.sections[0]?.id || ''
  );
  const [isAddingSection, setIsAddingSection] = useState(false);
  const [newSectionName, setNewSectionName] = useState('');
  const [activeDocMenuId, setActiveDocMenuId] = useState<string | null>(null);
  const [showNewPageModal, setShowNewPageModal] = useState(false);

  // New Document modal form states
  const [newDocTitle, setNewDocTitle] = useState('');
  const [newDocType, setNewDocType] = useState<DocType>('md');
  const [newDocContent, setNewDocContent] = useState('');

  const currentSection =
    notebook.sections.find((s) => s.id === activeSectionId) ||
    notebook.sections[0];

  const handleCreateSectionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSectionName.trim()) return;
    onAddSection(notebook.id, newSectionName.trim());
    setNewSectionName('');
    setIsAddingSection(false);
  };

  const handleCreateDocSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDocTitle.trim()) return;

    const newDoc: Partial<DocumentItem> = {
      title: newDocTitle.endsWith(`.${newDocType}`)
        ? newDocTitle
        : `${newDocTitle}.${newDocType}`,
      type: newDocType,
      lastRead: 'Just now',
      progressPercentage: 0,
      currentPage: 1,
      totalPages: newDocType === 'pdf' ? 24 : newDocType === 'ipynb' ? 4 : 8,
      bookmarked: false,
      bookmarks: [],
      toc: [
        { id: 'toc-1', title: newDocTitle, level: 1, pageNumber: 1 }
      ],
      content: {
        authors: 'You',
        publishedDate: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
        readingTime: '5 min read',
        sections: [
          {
            id: 'sec-1',
            heading: newDocTitle,
            paragraphs: newDocContent
              ? newDocContent.split('\n\n')
              : [
                  'Start writing your research notes or thoughts here. You can format this document with headers, markdown syntax, quotes, and diagrams.',
                ],
            codeCells: newDocType === 'ipynb' ? [
              {
                id: 'cell-init',
                inputNumber: 1,
                code: '# New python computational notebook cell\nimport math\nprint("Initialized notebook environment")',
                outputType: 'text',
                outputText: 'Initialized notebook environment'
              }
            ] : undefined
          },
        ],
      },
    };

    onAddDocument(notebook.id, currentSection.id, newDoc);
    setNewDocTitle('');
    setNewDocContent('');
    setShowNewPageModal(false);
  };

  const getDocTypeBadge = (type: DocType) => {
    switch (type) {
      case 'pdf':
        return (
          <span className="bg-[#E55B3C] text-white text-[11px] font-semibold font-ibm px-2.5 py-0.5 rounded tracking-wide uppercase">
            PDF
          </span>
        );
      case 'md':
        return (
          <span className="bg-[#7C5EB1] text-white text-[11px] font-semibold font-ibm px-2.5 py-0.5 rounded tracking-wide uppercase">
            MD
          </span>
        );
      case 'ipynb':
        return (
          <span className="bg-[#D99026] text-white text-[11px] font-semibold font-ibm px-2.5 py-0.5 rounded tracking-wide uppercase">
            IPYNB
          </span>
        );
      default:
        return (
          <span className="bg-[#31685a] text-white text-[11px] font-semibold font-ibm px-2.5 py-0.5 rounded tracking-wide uppercase">
            DOC
          </span>
        );
    }
  };

  const getDocIcon = (type: DocType) => {
    switch (type) {
      case 'pdf':
        return <FileText className="w-5 h-5 text-[#717976]" />;
      case 'md':
        return <FileText className="w-5 h-5 text-[#717976]" />;
      case 'ipynb':
        return <FileCode className="w-5 h-5 text-[#717976]" />;
      default:
        return <BookOpen className="w-5 h-5 text-[#717976]" />;
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-[#f9f9f7] dark:bg-[#1a1c1b] overflow-hidden min-h-screen">
      {/* Header & Breadcrumbs */}
      <header className="pt-6 px-6 md:px-8 flex flex-col gap-4 bg-[#f9f9f7] dark:bg-[#242726] z-10 border-b border-[#c1c8c5]/40 shadow-[0_1px_0_rgba(0,0,0,0.03)]">
        {/* Breadcrumb row */}
        <div className="flex items-center gap-2 text-[#717976] text-[13px] font-ibm font-medium">
          <button
            onClick={onBackToShelf}
            className="flex items-center gap-1.5 hover:text-[#264a42] dark:hover:text-[#a8cfc3] transition-colors"
          >
            <Home className="w-4 h-4" />
            <span>Home</span>
          </button>
          <span className="text-[#c1c8c5]">/</span>
          <button
            onClick={onBackToShelf}
            className="hover:text-[#264a42] dark:hover:text-[#a8cfc3] transition-colors"
          >
            Shelf
          </button>
          <span className="text-[#c1c8c5]">/</span>
          <span className="text-[#1a1c1b] dark:text-[#f1f1ef] font-semibold">
            {notebook.title}
          </span>
        </div>

        {/* Section Tabs */}
        <div className="flex items-end gap-1.5 mt-3 overflow-x-auto scrollbar-hide">
          {notebook.sections.map((section) => {
            const isActive = section.id === currentSection?.id;
            return (
              <button
                key={section.id}
                onClick={() => setActiveSectionId(section.id)}
                className={`px-6 py-2.5 transition-all text-sm font-semibold flex items-center gap-2 relative rounded-t-lg border-t border-x border-[#c1c8c5]/50 -mb-[1px] ${
                  isActive
                    ? 'bg-[#5B6B7A] text-white shadow-sm z-10 border-b-0'
                    : 'bg-[#f4f4f2] dark:bg-[#2f3130] text-[#414846] dark:text-[#c1c8c5] hover:bg-[#e8e8e6] opacity-80'
                }`}
              >
                <span>{section.name}</span>
                <span
                  className={`text-[11px] px-1.5 py-0.2 rounded-full font-mono ${
                    isActive
                      ? 'bg-white/20 text-white'
                      : 'bg-[#dadad8] dark:bg-[#414846] text-[#414846] dark:text-[#f1f1ef]'
                  }`}
                >
                  {section.documents.length}
                </span>
              </button>
            );
          })}

          {/* Add Section Button */}
          {isAddingSection ? (
            <form
              onSubmit={handleCreateSectionSubmit}
              className="flex items-center gap-1.5 ml-2 mb-1"
            >
              <input
                type="text"
                placeholder="Section name..."
                value={newSectionName}
                onChange={(e) => setNewSectionName(e.target.value)}
                className="px-2.5 py-1 text-xs bg-white dark:bg-[#2f3130] border border-[#c1c8c5] rounded outline-none text-[#1a1c1b] dark:text-white"
                autoFocus
              />
              <button
                type="submit"
                className="px-2.5 py-1 bg-[#264a42] text-white text-xs rounded font-medium"
              >
                Add
              </button>
              <button
                type="button"
                onClick={() => setIsAddingSection(false)}
                className="px-2 py-1 text-xs text-[#717976] hover:text-[#1a1c1b]"
              >
                Cancel
              </button>
            </form>
          ) : (
            <button
              id="btn-add-section"
              onClick={() => setIsAddingSection(true)}
              className="p-2 ml-1 text-[#717976] hover:text-[#264a42] hover:bg-[#e8e8e6] dark:hover:bg-[#2f3130] rounded-full transition-colors mb-1"
              title="Add Section"
            >
              <Plus className="w-4 h-4" />
            </button>
          )}
        </div>
      </header>

      {/* Page List Container */}
      <div className="flex-1 overflow-y-auto px-6 md:px-8 py-8 bg-[#ffffff] dark:bg-[#1f2120]">
        <div className="max-w-4xl mx-auto flex flex-col gap-3.5 pb-24">
          {(!currentSection || currentSection.documents.length === 0) && (
            <div className="py-20 text-center flex flex-col items-center">
              <FileText className="w-12 h-12 text-[#c1c8c5] mb-3" />
              <h4 className="text-base font-semibold text-[#1a1c1b] dark:text-[#f1f1ef]">
                This section is empty
              </h4>
              <p className="text-xs text-[#717976] mt-1 max-w-sm">
                Add research papers, markdown notes, or computational analysis notebooks to this section.
              </p>
              <button
                onClick={() => setShowNewPageModal(true)}
                className="mt-5 px-4 py-2 bg-[#264a42] text-white rounded-lg text-xs font-semibold hover:bg-[#31685a] transition-colors"
              >
                + Add Document
              </button>
            </div>
          )}

          {currentSection?.documents.map((doc) => {
            const isMenuOpen = activeDocMenuId === doc.id;
            return (
              <div
                key={doc.id}
                id={`doc-row-${doc.id}`}
                onClick={() => onSelectDocument(doc, notebook)}
                className="flex items-center justify-between p-4 bg-[#f9f9f7] dark:bg-[#272a29] hover:bg-[#f4f4f2] dark:hover:bg-[#2f3331] rounded-lg transition-colors border border-[#c1c8c5]/40 group cursor-pointer shadow-sm relative"
              >
                <div className="flex flex-col gap-1 w-full pr-8">
                  {/* Title and Icon */}
                  <div className="flex items-center gap-3">
                    {getDocIcon(doc.type)}
                    <span className="text-[17px] font-semibold text-[#1a1c1b] dark:text-[#f1f1ef] group-hover:text-[#264a42] dark:group-hover:text-[#a8cfc3] transition-colors font-inter">
                      {doc.title}
                    </span>
                  </div>

                  {/* Metadata & Progress Bar */}
                  <div className="flex flex-wrap items-center gap-4 text-[13px] font-mono text-[#717976] mt-1.5">
                    <span>Last read {doc.lastRead}</span>
                    <div className="flex items-center gap-2.5 flex-1 max-w-[220px]">
                      <div className="h-1.5 bg-[#e2e2e1] dark:bg-[#414846] rounded-full flex-1 overflow-hidden">
                        <div
                          className={`h-full ${
                            doc.progressPercentage === 100
                              ? 'bg-[#264a42] opacity-70'
                              : 'bg-[#264a42]'
                          }`}
                          style={{ width: `${doc.progressPercentage}%` }}
                        />
                      </div>
                      {doc.progressPercentage === 100 ? (
                        <span className="text-[#264a42] dark:text-[#a8cfc3] flex items-center gap-1 font-semibold">
                          <CheckCircle className="w-3.5 h-3.5" /> 100%
                        </span>
                      ) : (
                        <span>{doc.progressPercentage}%</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Badge & Options */}
                <div className="flex items-center gap-4 shrink-0">
                  {getDocTypeBadge(doc.type)}

                  <div className="relative">
                    <button
                      id={`doc-menu-btn-${doc.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveDocMenuId(isMenuOpen ? null : doc.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 transition-opacity text-[#717976] hover:text-[#264a42] p-1.5 rounded hover:bg-[#eeeeec] dark:hover:bg-[#383c3a]"
                      aria-label="Document options"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    {isMenuOpen && (
                      <>
                        <div
                          className="fixed inset-0 z-20"
                          onClick={(e) => {
                            e.stopPropagation();
                            setActiveDocMenuId(null);
                          }}
                        />
                        <div className="absolute right-0 top-full mt-1 w-40 bg-white dark:bg-[#2f3130] rounded-lg shadow-xl border border-[#c1c8c5]/40 py-1.5 z-30 text-xs font-inter">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveDocMenuId(null);
                              onSelectDocument(doc, notebook);
                            }}
                            className="w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-[#f4f4f2] dark:hover:bg-[#383c3a] text-[#1a1c1b] dark:text-[#f1f1ef]"
                          >
                            <ExternalLink className="w-3.5 h-3.5 text-[#3e6259]" />
                            <span>Read Document</span>
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveDocMenuId(null);
                              onDeleteDocument(notebook.id, currentSection.id, doc.id);
                            }}
                            className="w-full px-3 py-2 text-left flex items-center gap-2 hover:bg-[#ffdad6]/40 text-[#ba1a1a]"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
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
      </div>

      {/* Floating Action Button: + New Page */}
      <button
        id="fab-new-page"
        onClick={() => setShowNewPageModal(true)}
        className="fixed bottom-8 right-8 bg-[#264a42] text-white flex items-center justify-center gap-2 px-6 py-4 rounded-full shadow-[0_8px_24px_rgba(31,29,27,0.18)] hover:bg-[#31685a] active:scale-95 transition-all duration-200 z-40"
      >
        <Plus className="w-5 h-5" />
        <span className="text-xs font-semibold font-ibm tracking-wider uppercase">
          New Page
        </span>
      </button>

      {/* Modal: New Document / Page */}
      {showNewPageModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs">
          <div className="bg-white dark:bg-[#242726] w-full max-w-lg rounded-2xl shadow-2xl border border-[#c1c8c5]/40 p-6">
            <h3 className="text-xl font-bold text-[#1a1c1b] dark:text-[#f1f1ef] font-inter">
              Add New Document
            </h3>
            <p className="text-xs text-[#717976] mt-1 mb-5">
              Create a new markdown note, PDF document, or computational notebook in{' '}
              <span className="font-semibold text-[#264a42] dark:text-[#a8cfc3]">
                {currentSection?.name}
              </span>
            </p>

            <form onSubmit={handleCreateDocSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] mb-1.5 uppercase">
                  Document Title
                </label>
                <input
                  type="text"
                  placeholder="e.g., Diffusion Models Survey"
                  value={newDocTitle}
                  onChange={(e) => setNewDocTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#f9f9f7] dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-lg text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none focus:ring-2 focus:ring-[#264a42]"
                  required
                  autoFocus
                />
              </div>

              <div>
                <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] mb-1.5 uppercase">
                  Format Type
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setNewDocType('pdf')}
                    className={`py-2 px-3 rounded-lg border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                      newDocType === 'pdf'
                        ? 'border-[#E55B3C] bg-[#E55B3C]/10 text-[#E55B3C]'
                        : 'border-[#c1c8c5]/60 hover:bg-[#f4f4f2] dark:hover:bg-[#2f3130] text-[#717976]'
                    }`}
                  >
                    <span className="w-2 h-2 rounded-full bg-[#E55B3C]" />
                    PDF Paper
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewDocType('md')}
                    className={`py-2 px-3 rounded-lg border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                      newDocType === 'md'
                        ? 'border-[#7C5EB1] bg-[#7C5EB1]/10 text-[#7C5EB1]'
                        : 'border-[#c1c8c5]/60 hover:bg-[#f4f4f2] dark:hover:bg-[#2f3130] text-[#717976]'
                    }`}
                  >
                    <span className="w-2 h-2 rounded-full bg-[#7C5EB1]" />
                    Markdown Note
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewDocType('ipynb')}
                    className={`py-2 px-3 rounded-lg border text-xs font-semibold flex items-center justify-center gap-2 transition-all ${
                      newDocType === 'ipynb'
                        ? 'border-[#D99026] bg-[#D99026]/10 text-[#D99026]'
                        : 'border-[#c1c8c5]/60 hover:bg-[#f4f4f2] dark:hover:bg-[#2f3130] text-[#717976]'
                    }`}
                  >
                    <span className="w-2 h-2 rounded-full bg-[#D99026]" />
                    Jupyter IPYNB
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold font-ibm text-[#414846] dark:text-[#c1c8c5] mb-1.5 uppercase">
                  Initial Content / Abstract (Optional)
                </label>
                <textarea
                  rows={3}
                  placeholder="Draft your initial insights or paste document text..."
                  value={newDocContent}
                  onChange={(e) => setNewDocContent(e.target.value)}
                  className="w-full px-3.5 py-2 bg-[#f9f9f7] dark:bg-[#2f3130] border border-[#c1c8c5] dark:border-[#414846] rounded-lg text-sm text-[#1a1c1b] dark:text-[#f1f1ef] outline-none focus:ring-2 focus:ring-[#264a42] resize-none"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-[#c1c8c5]/40">
                <button
                  type="button"
                  onClick={() => setShowNewPageModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-[#717976] hover:bg-[#eeeeec] dark:hover:bg-[#2f3130] rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-semibold bg-[#264a42] text-white hover:bg-[#31685a] rounded-lg transition-colors"
                >
                  Create Document
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
