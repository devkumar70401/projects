# Calculator Capability (Capability-Oriented Architecture)

**Capability Name:** Scientific & Programmable Calculator  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.calculator`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* Mathematical expression tokenization and AST parsing (`ExpressionLexer`, `ExpressionParser`, `ASTNodes`).
* Arbitrary-precision evaluation via `BigDecimal` and `BigInteger` (`CalculatorEngine`).
* Complex numbers arithmetic ($a + bi$) and polar conversions (`ComplexNumber`).
* Matrix algebra (LU decomposition, inverse, determinant, Gaussian elimination) (`MatrixEngine`).
* 3D Vector mechanics (cross product, dot product, normalization) (`Vector3D`).
* Dimensional analysis and unit conversion across 13 physical domains (`DimensionalQuantity`, `UnitRegistry`).
* Programmer radix modes (HEX, DEC, OCT, BIN) with bitwise logic, 8/16/32/64-bit toggles (`ProgrammingEngine`).
* CODATA 2022 physical constants registry (`PhysicalConstants`).
* 2D function graphing and interactive canvas plotter (`InteractiveGraphView`).
* Calculation history state persistence (`CalculatorHistoryManager`).

### Non-Responsibilities:
* Does NOT own the application-level navigation shell (managed by `MainActivity` and `core/features`).
* Does NOT persist or mutate state in unrelated capabilities.
* Does NOT perform network operations.

---

## 2. Directory Layout

```text
features/calculator/
├── README.md                      # Capability architectural specification
└── contract/
    └── CalculatorContract.java    # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/calculator/
├── ASTNodes.java
├── AngleMode.java
├── CalculatorCardController.java
├── CalculatorEngine.java
├── CalculatorHistoryManager.java
├── ComplexNumber.java
├── Dimension.java
├── DimensionalQuantity.java
├── EvaluationContext.java
├── ExpressionLexer.java
├── ExpressionParser.java
├── ExpressionToken.java
├── InteractiveGraphView.java
├── MathContextConfig.java
├── MatrixEngine.java
├── NumberTheory.java
├── PhysicalConstants.java
├── PremiumKeypadButton.java
├── ProgrammingEngine.java
├── StatisticsEngine.java
├── UnitDefinition.java
├── UnitRegistry.java
└── Vector3D.java
```
