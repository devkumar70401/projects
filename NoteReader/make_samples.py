import json, os, subprocess

sample_dir = "/home/dev/SE/NoteReader/samples"
os.makedirs(sample_dir, exist_ok=True)

# 1. Text sample
with open(f"{sample_dir}/Quantum_Computing.txt", "w") as f:
    f.write("""Quantum Computing Fundamentals

1. Quantum Bits (Qubits)
Unlike classical bits which can exist in a definite state of 0 or 1, a quantum bit (qubit) can exist in a superposition of both states |0> and |1>.
Mathematically, a pure qubit state can be written as a linear combination:
|psi> = alpha|0> + beta|1>
where alpha and beta are complex probability amplitudes such that |alpha|^2 + |beta|^2 = 1.

2. Quantum Superposition
Superposition allows a quantum computer to evaluate many possible combinations simultaneously. When measured, however, the superposition collapses into one of the basis states with probability proportional to the square of its amplitude.

3. Quantum Entanglement
Entanglement is a phenomenon where quantum particles become inextricably linked, such that the state of one instantly correlates with the state of another, regardless of distance.
Einstein famously described this as "spooky action at a distance".

4. Applications
- Cryptography (Shor's Algorithm for prime factorization)
- Database Search (Grover's Algorithm with quadratic speedup)
- Quantum Chemistry and Drug Discovery
- Financial Modeling and Optimization Algorithms
""")

# 2. Markdown sample
with open(f"{sample_dir}/Deep_Learning_Notes.md", "w") as f:
    f.write("""# Deep Learning & Neural Architectures

## 1. Multi-Layer Perceptron (MLP)
Neural networks learn hierarchical representations through composition of affine transformations followed by non-linear activations.

### Key Activation Functions
- **ReLU**: f(x) = max(0, x)
- **GELU**: x * Phi(x) (Standard in Transformers like BERT & GPT)
- **Softmax**: Converts unnormalized logits into valid probability vectors.

```python
import torch
import torch.nn as nn

class MLP(nn.Module):
    def __init__(self, in_features, hidden, out_classes):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(in_features, hidden),
            nn.GELU(),
            nn.Dropout(0.1),
            nn.Linear(hidden, out_classes)
        )

    def forward(self, x):
        return self.net(x)
```

## 2. Transformer Self-Attention
Scaled Dot-Product Attention:
Attention(Q, K, V) = softmax(Q * K^T / sqrt(d_k)) * V

| Component | Role | Dimension |
| :--- | :--- | :--- |
| **Query (Q)** | What the token is looking for | (Batch, SeqLen, d_k) |
| **Key (K)** | What the token contains | (Batch, SeqLen, d_k) |
| **Value (V)** | The retrieved payload | (Batch, SeqLen, d_v) |
""")

# 3. Jupyter Notebook sample
ipynb_data = {
    "cells": [
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "# Data Analysis & Numerical Study\n",
                "Exploring array computations, performance metrics, and model evaluation."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": 1,
            "metadata": {},
            "outputs": [
                {
                    "name": "stdout",
                    "output_type": "stream",
                    "text": [
                        "Matrix multiplication completed in 0.0018s\n",
                        "Tensor shape: (100, 100)\n"
                    ]
                }
            ],
            "source": [
                "import numpy as np\n",
                "A = np.random.randn(100, 100)\n",
                "B = np.random.randn(100, 100)\n",
                "C = np.dot(A, B)\n",
                "print('Matrix multiplication completed in 0.0018s')\n",
                "print(f'Tensor shape: {C.shape}')"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Evaluation Summary\n",
                "Cross-validation scores across 5 test folds."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": 2,
            "metadata": {},
            "outputs": [
                {
                    "data": {
                        "text/plain": [
                            "Mean Accuracy: 98.4% (+/- 0.6%)"
                        ]
                    },
                    "execution_count": 2,
                    "output_type": "execute_result"
                }
            ],
            "source": [
                "scores = [0.982, 0.988, 0.979, 0.985, 0.986]\n",
                "f'Mean Accuracy: {np.mean(scores)*100:.1f}% (+/- {np.std(scores)*100:.1f}%)'"
            ]
        }
    ],
    "metadata": {
        "language_info": {"name": "python"}
    },
    "nbformat": 4,
    "nbformat_minor": 4
}

with open(f"{sample_dir}/Data_Science_Lab.ipynb", "w") as f:
    json.dump(ipynb_data, f, indent=2)

print("Generated sample documents.")
