import { Notebook, ReaderSettings } from '../types';

export const DEFAULT_READER_SETTINGS: ReaderSettings = {
  theme: 'light',
  fontSize: 18,
  typeface: 'Literata',
  lineHeight: 'relaxed',
  marginWidth: 'standard',
};

export const INITIAL_NOTEBOOKS: Notebook[] = [
  {
    id: 'nb-research',
    title: 'Research',
    color: '#3e6259', // Sage / Forest fixed dim
    textColor: '#c3ebdf',
    pagesCount: 8,
    description: 'Core research papers, architectures, and analysis',
    updatedAt: '2 days ago',
    sections: [
      {
        id: 'sec-current',
        name: 'Current Research',
        isDefault: true,
        documents: [
          {
            id: 'doc-deep-learning',
            title: 'Deep Learning Paper.pdf',
            type: 'pdf',
            lastRead: '2 days ago',
            progressPercentage: 63,
            currentPage: 42,
            totalPages: 189,
            bookmarked: true,
            bookmarks: [
              {
                id: 'bm-1',
                pageNumber: 15,
                title: 'Section 1: Multi-Head Attention Formulations',
                snippet: 'Scaled dot-product attention computed efficiently across multiple subspaces...',
                timestamp: '3 days ago',
              },
              {
                id: 'bm-2',
                pageNumber: 42,
                title: 'Figure 1: The Transformer Architecture',
                snippet: 'The encoder is composed of a stack of N=6 identical layers...',
                timestamp: '2 days ago',
              },
              {
                id: 'bm-3',
                pageNumber: 78,
                title: 'Section 3.2: Positional Encoding',
                snippet: 'Since our model contains no recurrence and no convolution, we must inject some information...',
                timestamp: '1 week ago',
              },
            ],
            toc: [
              { id: 'toc-title', title: 'Attention Is All You Need: A Retrospective', level: 1, pageNumber: 1 },
              { id: 'toc-intro', title: '1. Introduction', level: 2, pageNumber: 1 },
              { id: 'toc-bg', title: '2. Background', level: 2, pageNumber: 2 },
              { id: 'toc-arch', title: '3. Model Architecture', level: 2, pageNumber: 42 },
              { id: 'toc-enc-dec', title: '3.1 Encoder and Decoder Stacks', level: 3, pageNumber: 43 },
              { id: 'toc-attn', title: '3.2 Attention Mechanism', level: 3, pageNumber: 44 },
              { id: 'toc-training', title: '4. Training & Optimization', level: 2, pageNumber: 85 },
              { id: 'toc-results', title: '5. Benchmark Results & Translation Quality', level: 2, pageNumber: 120 },
              { id: 'toc-conclusion', title: '6. Conclusion & Retrospective Outlook', level: 2, pageNumber: 180 },
            ],
            content: {
              arxivId: 'ArXiv:2104.12345',
              authors: 'A Vaswani, N Shazeer, N Parmar, J Uszkoreit, L Jones...',
              publishedDate: 'Jun 12, 2017 (Revised Oct 2023)',
              readingTime: '45 min read',
              abstract: 'The dominant sequence transduction models are based on complex recurrent or convolutional neural networks that include an encoder and a decoder. The best performing models also connect the encoder and decoder through an attention mechanism. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely.',
              sections: [
                {
                  id: 'sec-fig-1',
                  paragraphs: [
                    'The dominant sequence transduction models are based on complex recurrent or convolutional neural networks that include an encoder and a decoder. The best performing models also connect the encoder and decoder through an attention mechanism. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely.',
                  ],
                  figure: {
                    src: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB8Kwp5rH5rH2kh-N2gPPplh_oWDNdoJWMEFArNFp5nIOTE1W5xk_Y5KErT1frtgRixbhuhonAPOQi1chUJhdTQNtRykr9uMtd2Z5322kAEbjo_Eq5P97ox30OcaTBHh0edd7a5uNlARZZnDmYnKilR31kXZBVokV5Dld2Cqzcdgo_yNwRsKd2Mx4kr5VG9IXzoG_52Cz8TebGpNDyoCylhcHzv9FjN_EWbuAp5p95XtCrjKH5UrcTZFQ',
                    caption: 'Figure 1: The Transformer - model architecture. Note the absence of recurrent layers.',
                    alt: 'Transformer neural network architecture diagram',
                  },
                },
                {
                  id: 'sec-intro',
                  heading: '1. Introduction',
                  paragraphs: [
                    'Recurrent neural networks, long short-term memory and gated recurrent neural networks in particular, have been firmly established as state of the art approaches in sequence modeling and transduction problems such as language modeling and machine translation. Numerous efforts have since continued to push the boundaries of recurrent language models and encoder-decoder architectures.',
                    'Recurrence models typically factor computation along the symbol positions of the input and output sequences. Aligning the positions to steps in computation time, they generate a sequence of hidden states h_t, as a function of the previous hidden state h_{t-1} and the input for position t. This inherently sequential nature precludes parallelization within training examples, which becomes critical at longer sequence lengths, as memory constraints limit batching across examples.',
                  ],
                  quote: {
                    text: 'Attention mechanisms have become an integral part of compelling sequence modeling and transduction models in various tasks, allowing modeling of dependencies without regard to their distance in the input or output sequences.',
                  },
                },
                {
                  id: 'sec-bg',
                  heading: '2. Background',
                  paragraphs: [
                    'In this work we propose the Transformer, a model architecture eschewing recurrence and instead relying entirely on an attention mechanism to draw global dependencies between input and output. The Transformer allows for significantly more parallelization and can reach a new state of the art in translation quality after being trained for as little as twelve hours on eight P100 GPUs.',
                    'The goal of reducing sequential computation also forms the foundation of the Extended Neural GPU, ByteNet and ConvS2S, all of which use convolutional neural networks as basic building block, computing hidden representations in parallel for all input and output positions. In these models, the number of operations required to relate signals from two arbitrary input or output positions grows in the distance between positions, linearly for ConvS2S and logarithmically for ByteNet. This makes it more difficult to learn dependencies between distant positions.',
                    'Self-attention, sometimes called intra-attention is an attention mechanism relating different positions of a single sequence in order to compute a representation of the sequence. Self-attention has been used successfully in a variety of tasks including reading comprehension, abstractive summarization, textual entailment and learning task-independent sentence representations.',
                  ],
                },
              ],
            },
          },
          {
            id: 'doc-notes-transformers',
            title: 'Notes on Transformers.md',
            type: 'md',
            lastRead: '1 week ago',
            progressPercentage: 100,
            currentPage: 15,
            totalPages: 15,
            bookmarked: false,
            bookmarks: [
              {
                id: 'bm-md-1',
                pageNumber: 3,
                title: 'Alignment and Human Flourishing',
                snippet: 'The defining challenge of the 21st century is not the creation of artificial general intelligence...',
                timestamp: '1 week ago',
              },
            ],
            toc: [
              { id: 'toc-f-1', title: 'The Future of AI: Navigating the Next Epoch', level: 1, pageNumber: 1 },
              { id: 'toc-f-2', title: 'The Cognitive Revolution', level: 2, pageNumber: 3 },
              { id: 'toc-f-3', title: 'Architectural Paradigms', level: 2, pageNumber: 7 },
              { id: 'toc-f-4', title: 'Ethical & Societal Implications', level: 2, pageNumber: 12 },
            ],
            content: {
              authors: 'By Dr. Aris Vang',
              publishedDate: 'Published Oct 24, 2023',
              readingTime: '15 min read',
              sections: [
                {
                  id: 'md-title-sec',
                  heading: 'The Future of AI: Navigating the Next Epoch',
                  paragraphs: [
                    'Artificial Intelligence stands at a precipice, not merely of technological capability, but of societal integration. As we transition from specialized algorithms to increasingly general systems, the questions we face evolve from "Can we build it?" to "How do we coexist with it?" The dawn of this new epoch demands a rigorous examination of the architectures, ethics, and economic paradigms that will define the coming decades.',
                    'Historically, technological revolutions have automated physical labor or specific computational tasks. The current wave, characterized by Large Language Models (LLMs) and generative architectures, threatens—or promises—to automate cognitive labor. This shift requires a re-evaluation of human utility in an ecosystem where synthetic cognition rivals or surpasses biological benchmarks in specialized domains.',
                  ],
                  quote: {
                    text: 'The defining challenge of the 21st century is not the creation of artificial general intelligence, but the alignment of its imperatives with human flourishing.',
                    author: 'Dr. E. L. Mercer',
                    citation: 'Institute for Advanced Study',
                  },
                },
                {
                  id: 'md-convergence-sec',
                  heading: 'The Cognitive Triad',
                  paragraphs: [
                    'Understanding this trajectory requires us to dissect the underlying mechanisms driving this rapid acceleration. It is not a single breakthrough, but the convergence of vast datasets, specialized hardware, and novel transformer architectures.',
                    'When dense neural representations are grounded across symbolic layers, unexpected emergent phenomena arise: associative recall improves exponentially, reasoning chains stabilize over hundreds of inference tokens, and latent semantic geometry reflects human conceptual hierarchies with remarkable fidelity.',
                  ],
                },
              ],
            },
          },
          {
            id: 'doc-linear-regression',
            title: 'Data Analysis.ipynb',
            type: 'ipynb',
            lastRead: 'today',
            progressPercentage: 15,
            currentPage: 1,
            totalPages: 4,
            bookmarked: true,
            bookmarks: [
              {
                id: 'bm-py-1',
                pageNumber: 1,
                title: 'In [3]: Linear Regression Plot Cell',
                snippet: 'plt.plot(X_new, y_predict, "r-", linewidth=2, label="Predictions")',
                timestamp: 'Today',
              },
            ],
            toc: [
              { id: 'toc-lr-1', title: 'Linear Regression Analysis', level: 1, pageNumber: 1 },
              { id: 'toc-lr-2', title: 'In [3]: Synthetic Data & Fit', level: 2, pageNumber: 1 },
              { id: 'toc-lr-3', title: 'In [4]: Coefficients & Intercept', level: 2, pageNumber: 2 },
              { id: 'toc-lr-4', title: 'In [5]: Residual Analysis', level: 2, pageNumber: 3 },
            ],
            content: {
              authors: 'Notebook Author: Research Team',
              publishedDate: 'Updated Today',
              readingTime: '8 min interactive',
              sections: [
                {
                  id: 'ipynb-sec-1',
                  heading: 'Linear Regression Analysis',
                  paragraphs: [
                    'In this notebook, we explore the foundational concepts of linear regression, a fundamental technique in statistical modeling and machine learning. We will build a simple model to predict an outcome based on a single independent variable.',
                    'The core idea is to find the "line of best fit" that minimizes the sum of squared residuals between the observed data and the predicted values generated by our linear equation.',
                  ],
                  codeCells: [
                    {
                      id: 'cell-3',
                      inputNumber: 3,
                      code: `import numpy as np
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression

# Generate synthetic data
np.random.seed(42)
X = 2 * np.random.rand(100, 1)
y = 4 + 3 * X + np.random.randn(100, 1)

# Fit the model
lin_reg = LinearRegression()
lin_reg.fit(X, y)

# Predictions for plotting
X_new = np.array([[0], [2]])
y_predict = lin_reg.predict(X_new)

# Visualization
plt.plot(X_new, y_predict, "r-", linewidth=2, label="Predictions")
plt.plot(X, y, "b.")
plt.xlabel("$x_1$", fontsize=14)
plt.ylabel("$y$", rotation=0, fontsize=14)
plt.axis([0, 2, 0, 15])
plt.legend(loc="upper left")
plt.show()`,
                      outputType: 'image',
                      outputImage: 'https://lh3.googleusercontent.com/aida-public/AB6AXuD4TvywTnIEyYOOGHbZMguryeOpdz1pxfQpZfuLQIa8nnAlDZKDnHiNC16jnoaRp8Q9FhilGxwfO395EMYFmamjbNnvmnQviirmBCV7gi6OZrJfra6pn7df00vJWfD9Skvhy7LtyJuIYkBY0N8erry4bHyfA6j3F9ltyyOZDn8ZyXNUxinhLnHSNBTDxWTRNFEsgdLh0KBSi52exr_VNMKzTn6E_Cf_H-zOa4uMUZVgDxmwhxbjfM90-A',
                      outputAlt: 'Scatter plot of synthetic data with fitted linear regression line in red',
                    },
                    {
                      id: 'cell-4',
                      inputNumber: 4,
                      code: `# Intercept and Coefficient
print("Intercept:", lin_reg.intercept_)
print("Coefficient:", lin_reg.coef_)`,
                      outputType: 'text',
                      outputText: `Intercept: [4.21509616]
Coefficient: [[2.77011339]]`,
                    },
                  ],
                },
              ],
            },
          },
        ],
      },
      {
        id: 'sec-archives',
        name: 'Archives',
        documents: [
          {
            id: 'doc-cog-models',
            title: 'Chapter 4: The Architecture of Thought',
            type: 'article',
            lastRead: '3 weeks ago',
            progressPercentage: 53,
            currentPage: 8,
            totalPages: 15,
            bookmarked: true,
            bookmarks: [
              {
                id: 'bm-cog-1',
                pageNumber: 8,
                title: 'Cognitive Geographies quote',
                snippet: 'The map is not the territory, but a map created with faulty instruments...',
                timestamp: '3 weeks ago',
              },
            ],
            toc: [
              { id: 'toc-cog-1', title: 'Constructing the Mental Models', level: 1, pageNumber: 8 },
              { id: 'toc-cog-2', title: 'Epistemic Binary vs Gradient', level: 2, pageNumber: 9 },
              { id: 'toc-cog-3', title: 'The Anchoring Effect', level: 2, pageNumber: 11 },
            ],
            content: {
              authors: 'Dr. Aris Thorne',
              publishedDate: 'Cognitive Geographies (2022)',
              readingTime: '20 min read',
              sections: [
                {
                  id: 'cog-sec-1',
                  heading: 'Constructing the Mental Models',
                  paragraphs: [
                    'The framework we use to interpret reality is inherently flawed by its own architecture. When we consider the structural integrity of mental models, we must first acknowledge the foundational biases upon which they are built. These biases are not merely glitches in the system; they are the mortar holding the bricks together.',
                    'In classical epistemology, the relationship between the observer and the observed is often treated as a binary system. However, modern cognitive frameworks suggest a more fluid gradient. The observer\'s state actively reconfigures the incoming data, much like how a colored lens alters the perception of a landscape.',
                  ],
                  quote: {
                    text: 'The map is not the territory, but a map created with faulty instruments will inevitably lead the traveler off a cliff.',
                    author: 'Dr. Aris Thorne',
                    citation: 'Cognitive Geographies',
                  },
                },
                {
                  id: 'cog-sec-2',
                  heading: 'Structural Dissonance & Metabolic Costs',
                  paragraphs: [
                    'Consider the phenomenon of structural dissonance. When new information directly contradicts a deeply embedded model, the brain experiences a quantifiable metabolic spike. The energy required to dismantle an old model and construct a new one is significant. Therefore, the path of least resistance is almost always assimilation—forcing the new, anomalous data into the existing structure, regardless of how poorly it fits.',
                    'This brings us to the concept of the \'Anchoring Effect\' in complex systems. It is not just that we rely too heavily on the first piece of information offered; it is that the first piece of information establishes the coordinate system for everything that follows.',
                  ],
                },
              ],
            },
          },
        ],
      },
      {
        id: 'sec-drafts',
        name: 'Drafts',
        documents: [],
      },
    ],
  },
  {
    id: 'nb-project-alpha',
    title: 'Project Alpha',
    color: '#7a5148', // Terracotta
    textColor: '#ffffff',
    pagesCount: 42,
    description: 'System specifications, architecture roadmaps, and sprint logs',
    updatedAt: '3 days ago',
    sections: [
      {
        id: 'sec-alpha-main',
        name: 'Sprint Docs',
        isDefault: true,
        documents: [
          {
            id: 'doc-alpha-spec',
            title: 'System Architecture Specification v2.md',
            type: 'md',
            lastRead: '3 days ago',
            progressPercentage: 45,
            currentPage: 19,
            totalPages: 42,
            bookmarked: false,
            bookmarks: [],
            toc: [
              { id: 'toc-a-1', title: 'System Overview', level: 1, pageNumber: 1 },
              { id: 'toc-a-2', title: 'Service Topology', level: 2, pageNumber: 12 },
              { id: 'toc-a-3', title: 'Data Flow Ingestion', level: 2, pageNumber: 24 },
            ],
            content: {
              authors: 'Core Architecture Guild',
              publishedDate: 'Aug 15, 2023',
              readingTime: '30 min read',
              sections: [
                {
                  id: 'a-sec-1',
                  heading: 'System Architecture Specification v2',
                  paragraphs: [
                    'This document specifies the target architecture for Project Alpha, detailing the microservices boundary, pub/sub pipeline, and event-driven data streaming layers.',
                    'High throughput stream processing is achieved through partitioned distributed queues with backpressure guarantees.',
                  ],
                },
              ],
            },
          },
        ],
      },
    ],
  },
  {
    id: 'nb-reading-list',
    title: 'Reading List',
    color: '#31685a', // Forest Green
    textColor: '#ffffff',
    pagesCount: 15,
    description: 'Curated papers and book summaries for technical leadership',
    updatedAt: 'Yesterday',
    sections: [
      {
        id: 'sec-rl-main',
        name: 'In Progress',
        isDefault: true,
        documents: [
          {
            id: 'doc-rl-1',
            title: 'Designing Data-Intensive Applications Summary.md',
            type: 'md',
            lastRead: 'Yesterday',
            progressPercentage: 80,
            currentPage: 12,
            totalPages: 15,
            bookmarked: true,
            bookmarks: [
              {
                id: 'bm-ddia-1',
                pageNumber: 12,
                title: 'Consensus & Total Order Broadcast',
                snippet: 'The equivalence between linearizability and atomic broadcast...',
                timestamp: 'Yesterday',
              },
            ],
            toc: [
              { id: 'toc-dd-1', title: 'Reliability, Scalability, Maintainability', level: 1, pageNumber: 1 },
              { id: 'toc-dd-2', title: 'Data Models & Query Languages', level: 2, pageNumber: 4 },
              { id: 'toc-dd-3', title: 'Distributed Transactions & Consensus', level: 2, pageNumber: 10 },
            ],
            content: {
              authors: 'Martin Kleppmann Notes',
              publishedDate: 'Updated Oct 2023',
              readingTime: '25 min read',
              sections: [
                {
                  id: 'dd-sec-1',
                  heading: 'Designing Data-Intensive Applications Summary',
                  paragraphs: [
                    'Reliability means continuing to work correctly, even when things go wrong. Hardware faults, software bugs, and human error are the three major fault categories to defend against.',
                    'Replication involves keeping a copy of the same data on multiple machines that are connected via a network.',
                  ],
                },
              ],
            },
          },
        ],
      },
    ],
  },
  {
    id: 'nb-journal',
    title: 'Journal',
    color: '#5f3a32', // Warm Plum / Deep Brown
    textColor: '#ffffff',
    pagesCount: 120,
    description: 'Personal reflections, engineering logbooks, and daily insights',
    updatedAt: '4 days ago',
    sections: [
      {
        id: 'sec-j-2023',
        name: '2023 Entries',
        isDefault: true,
        documents: [
          {
            id: 'doc-journal-q4',
            title: 'Q4 Engineering Reflections.md',
            type: 'md',
            lastRead: '4 days ago',
            progressPercentage: 92,
            currentPage: 110,
            totalPages: 120,
            bookmarked: false,
            bookmarks: [],
            toc: [
              { id: 'toc-j-1', title: 'October Reflections', level: 1, pageNumber: 1 },
              { id: 'toc-j-2', title: 'November Deep Work', level: 1, pageNumber: 60 },
              { id: 'toc-j-3', title: 'Year-End Review', level: 1, pageNumber: 110 },
            ],
            content: {
              authors: 'Personal Log',
              publishedDate: 'Nov 2023',
              readingTime: '15 min read',
              sections: [
                {
                  id: 'j-sec-1',
                  heading: 'Year-End Engineering Insights',
                  paragraphs: [
                    'Building tools with tactile, matte aesthetics reduces visual fatigue and fosters deep focus. The transition towards minimalist typography in code reading environments has substantially improved retention.',
                    'Consistent habits compound exponentially over quarters. Protecting large unbroken blocks for deep reading is essential.',
                  ],
                },
              ],
            },
          },
        ],
      },
    ],
  },
];

export const USER_PROFILE = {
  name: 'NoteReader',
  subtitle: 'My Library',
  avatarUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDlW8nLmsPoUBmMkYgoaPyEG8eoQzMJ5_H7OUCFATFQSNum6MgGHB6VIwXsJ8JPMKz0Iic86M61d7XIBwkIW9t98uG5F9ohleR0ksRCKU1TWaJWUWbHALzSVyq4ZPwVMa4rwnXrMExXIlaj9V2n9V4ExoH80nm3cQwBpRN_5d2Obe-U9zIbdzcGrU_1Tkw3PsAfNJFSiy-NDLqYx0QITqUk6d3kv6f1Z8DjYLwL-ct77GIkRVYRu2rFlw',
};
