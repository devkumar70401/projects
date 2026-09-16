export type DocType = 'pdf' | 'md' | 'ipynb' | 'article';

export interface TableOfContentsItem {
  id: string;
  title: string;
  level: number;
  pageNumber: number;
}

export interface Bookmark {
  id: string;
  pageNumber: number;
  title: string;
  snippet: string;
  timestamp: string;
  note?: string;
}

export interface CodeCell {
  id: string;
  inputNumber: number;
  code: string;
  outputType?: 'image' | 'text' | 'both';
  outputImage?: string;
  outputText?: string;
  outputAlt?: string;
  isRunning?: boolean;
}

export interface DocContent {
  arxivId?: string;
  authors?: string;
  publishedDate?: string;
  readingTime?: string;
  abstract?: string;
  sections: {
    id: string;
    heading?: string;
    paragraphs: string[];
    figure?: {
      src: string;
      caption: string;
      alt: string;
    };
    quote?: {
      text: string;
      author?: string;
      citation?: string;
    };
    codeCells?: CodeCell[];
  }[];
}

export interface DocumentItem {
  id: string;
  title: string;
  type: DocType;
  lastRead: string;
  progressPercentage: number;
  currentPage: number;
  totalPages: number;
  bookmarked: boolean;
  bookmarks: Bookmark[];
  toc: TableOfContentsItem[];
  content: DocContent;
}

export interface NotebookSection {
  id: string;
  name: string;
  isDefault?: boolean;
  documents: DocumentItem[];
}

export interface Notebook {
  id: string;
  title: string;
  color: string; // Hex or tailwind class
  textColor?: string;
  pagesCount: number;
  description?: string;
  updatedAt: string;
  sections: NotebookSection[];
}

export type ThemeMode = 'light' | 'sepia' | 'dark';
export type Typeface = 'Literata' | 'Inter' | 'IBM Plex Sans';

export interface ReaderSettings {
  theme: ThemeMode;
  fontSize: number; // e.g. 18, 20, 24
  typeface: Typeface;
  lineHeight: 'normal' | 'relaxed';
  marginWidth: 'standard' | 'wide' | 'narrow';
}

export type ActiveNavTab = 'home' | 'recent' | 'bookmarks' | 'search' | 'settings';
