# Deep Learning & Neural Architectures

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
