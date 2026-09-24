package com.devendra.healthtracker.calculator;

import com.devendra.healthtracker.R;
import com.devendra.healthtracker.theme.ThemeManager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.design.TouchFeedback;
import com.devendra.healthtracker.ui.components.PremiumCard;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Scientific, Engineering & Programmable Calculator Card.
 * Manages UI creation and user interactions across:
 * - Simple & Scientific Modes (Trigonometry, Logarithms, Roots, Factorial, Complex numbers)
 * - Programmer Mode (HEX, DEC, OCT, BIN, bit widths, signed/unsigned, bit toggles, bitwise ops)
 * - Unit Engine & Dimensional Analysis (13 categories, conversions, dimensional arithmetic)
 * - Matrices & 3D Vectors (LU decomposition, inverse, determinant, system solve, cross/dot)
 * - Interactive Function Plotter (2D canvas with pan/zoom)
 * - Physical Constants Registry (CODATA 2022 constants with insert button)
 * - Calculation History
 */
public class CalculatorCardController {

    private final Activity activity;
    private final CalculatorEngine engine;
    private final CalculatorHistoryManager historyManager;

    // Sub-mode tabs
    public enum SubMode {
        SIMPLE("Simple"),
        SCIENTIFIC("Scientific"),
        PROGRAMMER("Programmer"),
        UNITS("Units"),
        MATRIX("Matrix"),
        GRAPH("Graph"),
        CONSTANTS("Constants"),
        HISTORY("History");

        public final String label;
        SubMode(String label) { this.label = label; }
    }

    private SubMode currentMode = SubMode.SCIENTIFIC;
    private LinearLayout contentContainer;
    private LinearLayout tabsContainer;
    private final List<TextView> tabViews = new ArrayList<>();

    // Standard/Scientific Display
    private TextView formulaView;
    private TextView resultView;
    private TextView angleModeBtn;
    private TextView secondFnBtn;
    private TextView memoryBadge;
    private StringBuilder currentFormula = new StringBuilder();
    private boolean isSecondFnActive = false;

    // Programmer Mode State
    private ProgrammingEngine.BitWidth progWidth = ProgrammingEngine.BitWidth.DWORD;
    private boolean progSigned = true;
    private long progCurrentValue = 0L;
    private String progActiveRadix = "HEX";
    private TextView progHexView;
    private TextView progDecView;
    private TextView progOctView;
    private TextView progBinView;
    private LinearLayout progBitGridContainer;
    private TextView progStatusBadge;

    // Units State
    private String selectedUnitCategory = "Length";
    private Spinner unitFromSpinner;
    private Spinner unitToSpinner;
    private EditText unitFromInput;
    private TextView unitResultView;
    private TextView unitFormulaView;

    // Matrix State
    private int matrixDim = 3;
    private EditText[][] matrixAInputs;
    private EditText[] vectorBInputs;
    private TextView matrixResultView;

    // Vector State
    private EditText vecAx, vecAy, vecAz;
    private EditText vecBx, vecBy, vecBz;
    private TextView vecResultView;

    // Graph State
    private InteractiveGraphView graphView;
    private EditText graphFunctionInput;

    public CalculatorCardController(Activity activity) {
        this.activity = activity;
        this.historyManager = new CalculatorHistoryManager(activity);
        this.engine = new CalculatorEngine(historyManager);
    }

    public View createView() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dp(24));

        // 1. Sleek Amber Header Badge Card
        LinearLayout headerCard = createCardLayout(Color.parseColor("#FFFBEB"), Color.parseColor("#FDE68A"));
        headerCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        headerCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout hRow = new LinearLayout(activity);
        hRow.setOrientation(LinearLayout.HORIZONTAL);
        hRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView hTitle = new TextView(activity);
        hTitle.setText("🧮 SCIENTIFIC & PROGRAMMABLE CALCULATOR");
        hTitle.setTextSize(11);
        hTitle.setTypeface(Typeface.DEFAULT_BOLD);
        hTitle.setTextColor(isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706"));
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        hTitle.setLayoutParams(htLp);
        hRow.addView(hTitle);

        TextView statusBadge = new TextView(activity);
        statusBadge.setText("IEEE-754 • 34 DIGITS");
        statusBadge.setTextSize(9);
        statusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        statusBadge.setTextColor(isDarkTheme() ? 0xFFFDE68A : Color.parseColor("#D97706"));
        statusBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        statusBadge.setBackground(createPillDrawable(isDarkTheme() ? 0xFF3D3018 : Color.WHITE, dp(4)));
        hRow.addView(statusBadge);
        headerCard.addView(hRow);

        TextView hSub = new TextView(activity);
        hSub.setText("High-precision numerical computing, dimensional unit engine, bitwise programmer tools & 2D grapher.");
        hSub.setTextSize(10);
        hSub.setTextColor(isDarkTheme() ? 0xFFFDE68A : Color.parseColor("#78350F"));
        hSub.setPadding(0, dp(4), 0, 0);
        headerCard.addView(hSub);
        root.addView(headerCard);

        // 2. Mode Switcher (2 rows of 4 chips each for 100% visibility & instant access)
        LinearLayout tabsGrid = new LinearLayout(activity);
        tabsGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tgLp.setMargins(0, dp(10), 0, dp(8));
        tabsGrid.setLayoutParams(tgLp);

        LinearLayout row1 = new LinearLayout(activity);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, 0, 0, dp(4));

        LinearLayout row2 = new LinearLayout(activity);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        tabViews.clear();
        SubMode[] allModes = SubMode.values();
        for (int i = 0; i < allModes.length; i++) {
            final SubMode mode = allModes[i];
            TextView tab = new TextView(activity);
            tab.setText(mode.label);
            tab.setTextSize(10);
            tab.setPadding(dp(4), dp(6), dp(4), dp(6));
            tab.setGravity(Gravity.CENTER);
            tab.setClickable(true);
            tab.setFocusable(true);

            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tLp.setMargins(dp(2), 0, dp(2), 0);
            tab.setLayoutParams(tLp);

            TouchFeedback.attach(tab, v -> switchMode(mode), 0.94f);
            tabViews.add(tab);
            if (i < 4) {
                row1.addView(tab);
            } else {
                row2.addView(tab);
            }
        }
        tabsGrid.addView(row1);
        tabsGrid.addView(row2);
        root.addView(tabsGrid);

        // 3. Dynamic Sub-mode Content Container
        contentContainer = new LinearLayout(activity);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(contentContainer);

        // Initial render
        renderSubModeUI();
        updateTabSelectionUI();

        return root;
    }

    private void switchMode(SubMode mode) {
        this.currentMode = mode;
        updateTabSelectionUI();
        renderSubModeUI();
    }

    private void updateTabSelectionUI() {
        boolean dark = isDarkTheme();
        for (int i = 0; i < SubMode.values().length; i++) {
            SubMode mode = SubMode.values()[i];
            TextView tab = tabViews.get(i);
            if (mode == currentMode) {
                tab.setTextColor(dark ? 0xFF0B0E14 : Color.WHITE);
                tab.setTypeface(Typeface.DEFAULT_BOLD);
                tab.setBackground(createPillDrawable(dark ? DesignTokens.ACCENT_AMBER_DARK : DesignTokens.ACCENT_AMBER_PRIMARY, dp(12)));
            } else {
                tab.setTextColor(dark ? DesignTokens.getTextSecondary(true) : 0xFF64748B);
                tab.setTypeface(Typeface.DEFAULT);
                tab.setBackground(createPillDrawable(dark ? DesignTokens.getSurfaceElevated(true) : 0xFFF1F5F9, dp(12)));
            }
        }
    }

    private void renderSubModeUI() {
        contentContainer.removeAllViews();
        switch (currentMode) {
            case SIMPLE:
                contentContainer.addView(createCalculatorPanel(false));
                break;
            case SCIENTIFIC:
                contentContainer.addView(createCalculatorPanel(true));
                break;
            case PROGRAMMER:
                contentContainer.addView(createProgrammerPanel());
                break;
            case UNITS:
                contentContainer.addView(createUnitsPanel());
                break;
            case MATRIX:
                contentContainer.addView(createMatrixVectorPanel());
                break;
            case GRAPH:
                contentContainer.addView(createGraphPanel());
                break;
            case CONSTANTS:
                contentContainer.addView(createConstantsPanel());
                break;
            case HISTORY:
                contentContainer.addView(createHistoryPanel());
                break;
        }
    }

    // =========================================================================
    // 1. SIMPLE & SCIENTIFIC CALCULATOR PANEL
    // =========================================================================
    private View createCalculatorPanel(boolean isScientific) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 1. Display Screen Card
        LinearLayout screenCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        screenCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        screenCard.setOrientation(LinearLayout.VERTICAL);

        // Top Status row inside screen
        LinearLayout statusRow = new LinearLayout(activity);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        angleModeBtn = new TextView(activity);
        angleModeBtn.setText(engine.getAngleMode().name());
        angleModeBtn.setTextSize(10);
        angleModeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        angleModeBtn.setTextColor(isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706"));
        angleModeBtn.setPadding(dp(8), dp(3), dp(8), dp(3));
        angleModeBtn.setBackground(createPillDrawable(isDarkTheme() ? 0xFF3D3018 : Color.parseColor("#FEF3C7"), dp(6)));
        angleModeBtn.setClickable(true);
        TouchFeedback.attach(angleModeBtn, v -> {
            AngleMode current = engine.getAngleMode();
            AngleMode next = (current == AngleMode.DEG) ? AngleMode.RAD : (current == AngleMode.RAD ? AngleMode.GRAD : AngleMode.DEG);
            engine.setAngleMode(next);
            angleModeBtn.setText(next.name());
            evaluateFormulaLive();
        }, 0.94f);
        statusRow.addView(angleModeBtn);

        if (isScientific) {
            secondFnBtn = new TextView(activity);
            secondFnBtn.setText("2nd");
            secondFnBtn.setTextSize(10);
            secondFnBtn.setTypeface(Typeface.DEFAULT_BOLD);
            secondFnBtn.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#475569"));
            secondFnBtn.setPadding(dp(8), dp(3), dp(8), dp(3));
            secondFnBtn.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9"), dp(6)));
            LinearLayout.LayoutParams sfLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sfLp.setMargins(dp(6), 0, 0, 0);
            secondFnBtn.setLayoutParams(sfLp);
            secondFnBtn.setClickable(true);
            TouchFeedback.attach(secondFnBtn, v -> {
                isSecondFnActive = !isSecondFnActive;
                secondFnBtn.setBackground(createPillDrawable(isSecondFnActive ? (isDarkTheme() ? 0xFF3D3018 : Color.parseColor("#FEF3C7")) : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
                secondFnBtn.setTextColor(isSecondFnActive ? (isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706")) : (isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#475569")));
                renderSubModeUI(); // Refresh buttons
            }, 0.94f);
            statusRow.addView(secondFnBtn);
        }

        memoryBadge = new TextView(activity);
        memoryBadge.setText("M");
        memoryBadge.setTextSize(10);
        memoryBadge.setTypeface(Typeface.DEFAULT_BOLD);
        memoryBadge.setTextColor(engine.hasMemoryValue() ? (isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706")) : (isDarkTheme() ? 0xFF94A3B8 : Color.parseColor("#94A3B8")));
        memoryBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
        memoryBadge.setBackground(createPillDrawable(engine.hasMemoryValue() ? (isDarkTheme() ? 0xFF3D3018 : Color.parseColor("#FEF3C7")) : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
        LinearLayout.LayoutParams mbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mbLp.setMargins(dp(6), 0, 0, 0);
        memoryBadge.setLayoutParams(mbLp);
        statusRow.addView(memoryBadge);

        View spacer = new View(activity);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 0, 1.0f);
        spacer.setLayoutParams(spLp);
        statusRow.addView(spacer);

        TextView formatBadge = new TextView(activity);
        formatBadge.setText(engine.getContextConfig().getNotation().name());
        formatBadge.setTextSize(9);
        formatBadge.setTypeface(Typeface.DEFAULT_BOLD);
        formatBadge.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        formatBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        formatBadge.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F8FAFC"), dp(4)));
        formatBadge.setClickable(true);
        TouchFeedback.attach(formatBadge, v -> {
            MathContextConfig.DisplayNotation cur = engine.getContextConfig().getNotation();
            MathContextConfig.DisplayNotation next = (cur == MathContextConfig.DisplayNotation.STANDARD) ? MathContextConfig.DisplayNotation.SCIENTIFIC : (cur == MathContextConfig.DisplayNotation.SCIENTIFIC ? MathContextConfig.DisplayNotation.ENGINEERING : MathContextConfig.DisplayNotation.STANDARD);
            engine.getContextConfig().setNotation(next);
            formatBadge.setText(next.name());
            evaluateFormulaLive();
        }, 0.94f);
        statusRow.addView(formatBadge);
        screenCard.addView(statusRow);

        // Formula View
        formulaView = new TextView(activity);
        formulaView.setText(currentFormula.length() == 0 ? "0" : currentFormula.toString());
        formulaView.setTextSize(15);
        formulaView.setTypeface(Typeface.MONOSPACE);
        formulaView.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        formulaView.setGravity(Gravity.END);
        formulaView.setPadding(0, dp(10), 0, dp(2));
        screenCard.addView(formulaView);

        // Result View
        resultView = new TextView(activity);
        resultView.setText("0");
        resultView.setTextSize(26);
        resultView.setTypeface(Typeface.DEFAULT_BOLD);
        resultView.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        resultView.setGravity(Gravity.END);
        resultView.setPadding(0, 0, 0, dp(4));
        resultView.setTextIsSelectable(true);
        screenCard.addView(resultView);

        panel.addView(screenCard);

        // 2. Memory Register Bar
        LinearLayout memRow = new LinearLayout(activity);
        memRow.setOrientation(LinearLayout.HORIZONTAL);
        memRow.setPadding(0, dp(8), 0, dp(8));
        String[] memKeys = {"MC", "MR", "M+", "M-", "MS"};
        for (String mk : memKeys) {
            Button mBtn = createCalcButton(mk, "#F8FAFC", "#64748B", 11, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            mBtn.setLayoutParams(lp);
            mBtn.setOnClickListener(v -> handleMemoryOp(mk));
            memRow.addView(mBtn);
        }
        panel.addView(memRow);

        // 3. Extended Scientific Function Rows (if Scientific)
        if (isScientific) {
            String[][] sciRows = isSecondFnActive ? new String[][]{
                    {"asin", "acos", "atan", "10^x", "e^x"},
                    {"cbrt", "x^3", "1/x", "sinh", "cosh"},
                    {"nPr", "nCr", "gcd", "lcm", "tanh"}
            } : new String[][]{
                    {"sin", "cos", "tan", "log", "ln"},
                    {"sqrt", "x^2", "x^y", "(", ")"},
                    {"pi", "e", "!", "mod", "abs"}
            };

            for (String[] sRow : sciRows) {
                LinearLayout rowLayout = new LinearLayout(activity);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(0, 0, 0, dp(4));
                for (String sk : sRow) {
                    Button sBtn = createCalcButton(sk, "#F1F5F9", "#0F172A", 12, false);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
                    lp.setMargins(dp(2), 0, dp(2), 0);
                    sBtn.setLayoutParams(lp);
                    sBtn.setOnClickListener(v -> handleScientificKey(sk));
                    rowLayout.addView(sBtn);
                }
                panel.addView(rowLayout);
            }
        }

        // 4. Standard Keypad Grid (4 columns)
        String[][] stdRows = {
                {"C", "DEL", "%", "/"},
                {"7", "8", "9", "*"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"+/-", "0", ".", "="}
        };

        for (String[] row : stdRows) {
            LinearLayout rowLayout = new LinearLayout(activity);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, 0, 0, dp(4));

            for (String key : row) {
                boolean isOp = key.equals("/") || key.equals("*") || key.equals("-") || key.equals("+");
                boolean isEq = key.equals("=");
                boolean isClear = key.equals("C") || key.equals("DEL");

                String bg = isEq ? "#D97706" : (isOp ? "#FEF3C7" : (isClear ? "#FEE2E2" : "#FFFFFF"));
                String fg = isEq ? "#FFFFFF" : (isOp ? "#D97706" : (isClear ? "#DC2626" : "#0F172A"));

                Button btn = createCalcButton(key, bg, fg, 16, true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
                lp.setMargins(dp(2), 0, dp(2), 0);
                btn.setLayoutParams(lp);

                btn.setOnClickListener(v -> handleStandardKey(key));
                rowLayout.addView(btn);
            }
            panel.addView(rowLayout);
        }

        return panel;
    }

    private void handleStandardKey(String key) {
        switch (key) {
            case "C":
                currentFormula.setLength(0);
                formulaView.setText("0");
                resultView.setText("0");
                break;
            case "DEL":
                if (currentFormula.length() > 0) {
                    currentFormula.deleteCharAt(currentFormula.length() - 1);
                    formulaView.setText(currentFormula.length() == 0 ? "0" : currentFormula.toString());
                    evaluateFormulaLive();
                }
                break;
            case "=":
                evaluateFormulaFinal();
                break;
            case "+/-":
                if (currentFormula.length() > 0) {
                    if (currentFormula.charAt(0) == '-') {
                        currentFormula.deleteCharAt(0);
                    } else {
                        currentFormula.insert(0, '-');
                    }
                    formulaView.setText(currentFormula.toString());
                    evaluateFormulaLive();
                }
                break;
            default:
                currentFormula.append(key);
                formulaView.setText(currentFormula.toString());
                evaluateFormulaLive();
                break;
        }
    }

    private void handleScientificKey(String key) {
        switch (key) {
            case "x^2":
                currentFormula.append("^2");
                break;
            case "x^3":
                currentFormula.append("^3");
                break;
            case "x^y":
                currentFormula.append("^");
                break;
            case "1/x":
                currentFormula.insert(0, "1/(");
                currentFormula.append(")");
                break;
            case "10^x":
                currentFormula.append("10^(");
                break;
            case "e^x":
                currentFormula.append("e^(");
                break;
            case "sqrt":
            case "cbrt":
            case "sin":
            case "cos":
            case "tan":
            case "asin":
            case "acos":
            case "atan":
            case "sinh":
            case "cosh":
            case "tanh":
            case "log":
            case "ln":
            case "abs":
            case "gcd":
            case "lcm":
            case "nPr":
            case "nCr":
                currentFormula.append(key).append("(");
                break;
            case "pi":
                currentFormula.append("pi");
                break;
            case "e":
                currentFormula.append("e");
                break;
            case "!":
                currentFormula.append("!");
                break;
            case "mod":
                currentFormula.append(" % ");
                break;
            default:
                currentFormula.append(key);
                break;
        }
        formulaView.setText(currentFormula.toString());
        evaluateFormulaLive();
    }

    private void handleMemoryOp(String op) {
        try {
            double currentVal = 0.0;
            String resStr = resultView.getText().toString();
            try {
                currentVal = Double.parseDouble(resStr);
            } catch (Exception ignored) {}

            switch (op) {
                case "MC":
                    engine.memoryClear();
                    break;
                case "MR":
                    BigDecimal recalled = engine.memoryRecall();
                    currentFormula.append(engine.getContextConfig().format(recalled));
                    formulaView.setText(currentFormula.toString());
                    evaluateFormulaLive();
                    break;
                case "M+":
                    engine.memoryAdd(currentVal);
                    break;
                case "M-":
                    engine.memorySubtract(currentVal);
                    break;
                case "MS":
                    engine.memoryStore(currentVal);
                    break;
            }
            memoryBadge.setTextColor(engine.hasMemoryValue() ? (isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706")) : (isDarkTheme() ? 0xFF94A3B8 : Color.parseColor("#94A3B8")));
            memoryBadge.setBackground(createPillDrawable(engine.hasMemoryValue() ? (isDarkTheme() ? 0xFF3D3018 : Color.parseColor("#FEF3C7")) : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
        } catch (Exception e) {
            Toast.makeText(activity, "Memory error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void evaluateFormulaLive() {
        if (currentFormula.length() == 0) {
            resultView.setText("0");
            return;
        }
        try {
            String expr = currentFormula.toString();
            ASTNodes.ExpressionNode ast = ExpressionParser.parse(expr);
            ComplexNumber c = ast.evaluate(engine.getEvaluationContext());
            resultView.setText(engine.getContextConfig().format(c));
        } catch (Exception ignored) {
            // Live parsing ignores incomplete syntax
        }
    }

    private void evaluateFormulaFinal() {
        if (currentFormula.length() == 0) return;
        String expr = currentFormula.toString();
        String result = engine.evaluateExpression(expr);
        resultView.setText(result);
        if (!result.startsWith("Error")) {
            currentFormula.setLength(0);
            currentFormula.append(result);
            formulaView.setText(expr + " =");
        }
    }

    // =========================================================================
    // 2. PROGRAMMER MODE PANEL
    // =========================================================================
    private View createProgrammerPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 1. Bit-width & Signedness Header
        LinearLayout configRow = new LinearLayout(activity);
        configRow.setOrientation(LinearLayout.HORIZONTAL);
        configRow.setGravity(Gravity.CENTER_VERTICAL);
        configRow.setPadding(0, 0, 0, dp(8));

        // Bit widths: BYTE, WORD, DWORD, QWORD
        for (ProgrammingEngine.BitWidth w : ProgrammingEngine.BitWidth.values()) {
            TextView wBtn = new TextView(activity);
            wBtn.setText(w.name());
            wBtn.setTextSize(10);
            wBtn.setTypeface(Typeface.DEFAULT_BOLD);
            boolean isCur = (w == progWidth);
            wBtn.setTextColor(Color.parseColor(isCur ? "#FFFFFF" : (isDarkTheme() ? "#CBD5E1" : "#475569")));
            wBtn.setBackground(createPillDrawable(isCur ? Color.parseColor("#D97706") : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
            wBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(6), 0);
            wBtn.setLayoutParams(lp);
            wBtn.setClickable(true);
            wBtn.setOnClickListener(v -> {
                progWidth = w;
                progCurrentValue = progCurrentValue & progWidth.getMask();
                updateProgrammerDisplays();
                renderSubModeUI();
            });
            configRow.addView(wBtn);
        }

        View sp = new View(activity);
        configRow.addView(sp, new LinearLayout.LayoutParams(0, 0, 1.0f));

        // Signed/Unsigned toggle
        TextView signToggle = new TextView(activity);
        signToggle.setText(progSigned ? "SIGNED" : "UNSIGNED");
        signToggle.setTextSize(10);
        signToggle.setTypeface(Typeface.DEFAULT_BOLD);
        signToggle.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        signToggle.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#E2E8F0"), dp(6)));
        signToggle.setPadding(dp(8), dp(4), dp(8), dp(4));
        signToggle.setClickable(true);
        signToggle.setOnClickListener(v -> {
            progSigned = !progSigned;
            signToggle.setText(progSigned ? "SIGNED" : "UNSIGNED");
            updateProgrammerDisplays();
        });
        configRow.addView(signToggle);
        panel.addView(configRow);

        // 2. Radix Representations Card (HEX, DEC, OCT, BIN)
        LinearLayout radixCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        radixCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        radixCard.setOrientation(LinearLayout.VERTICAL);

        progHexView = createRadixRow(radixCard, "HEX", "0x0");
        progDecView = createRadixRow(radixCard, "DEC", "0");
        progOctView = createRadixRow(radixCard, "OCT", "0o0");
        progBinView = createRadixRow(radixCard, "BIN", "0000 0000");
        panel.addView(radixCard);

        // 3. Status Badge (Overflow info)
        progStatusBadge = new TextView(activity);
        progStatusBadge.setText("Ready (" + progWidth.bits + "-bit)");
        progStatusBadge.setTextSize(10);
        progStatusBadge.setTextColor(Color.parseColor("#10B981"));
        progStatusBadge.setPadding(0, dp(6), 0, dp(4));
        panel.addView(progStatusBadge);

        // 4. Interactive Visual Bit Toggle Grid
        progBitGridContainer = new LinearLayout(activity);
        progBitGridContainer.setOrientation(LinearLayout.VERTICAL);
        progBitGridContainer.setPadding(0, dp(4), 0, dp(8));
        panel.addView(progBitGridContainer);
        renderProgrammerBitGrid();

        // 5. Bitwise Operations Bar
        LinearLayout bitwiseRow = new LinearLayout(activity);
        bitwiseRow.setOrientation(LinearLayout.HORIZONTAL);
        bitwiseRow.setPadding(0, 0, 0, dp(6));
        String[] bitOps = {"AND", "OR", "XOR", "NOT", "SHL", "SHR"};
        for (String bOp : bitOps) {
            Button bBtn = createCalcButton(bOp, "#F1F5F9", "#0F172A", 11, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            bBtn.setLayoutParams(lp);
            bBtn.setOnClickListener(v -> handleBitwiseOp(bOp));
            bitwiseRow.addView(bBtn);
        }
        panel.addView(bitwiseRow);

        // 6. Hex Keypad (A-F)
        LinearLayout hexRow = new LinearLayout(activity);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setPadding(0, 0, 0, dp(6));
        String[] hexKeys = {"A", "B", "C", "D", "E", "F"};
        for (String hk : hexKeys) {
            Button hBtn = createCalcButton(hk, "#FEF3C7", "#D97706", 13, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            hBtn.setLayoutParams(lp);
            hBtn.setOnClickListener(v -> handleHexDigit(hk));
            hexRow.addView(hBtn);
        }
        panel.addView(hexRow);

        // Standard 0-9 & Clear Keypad
        String[][] progKeypad = {
                {"C", "DEL", "%", "/"},
                {"7", "8", "9", "*"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"0", "ROL", "ROR", "="}
        };
        for (String[] pRow : progKeypad) {
            LinearLayout rowLayout = new LinearLayout(activity);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, 0, 0, dp(4));
            for (String key : pRow) {
                boolean isOp = key.equals("/") || key.equals("*") || key.equals("-") || key.equals("+") || key.equals("=");
                String bg = isOp ? "#D97706" : (key.equals("C") || key.equals("DEL") ? "#FEE2E2" : "#FFFFFF");
                String fg = isOp ? "#FFFFFF" : (key.equals("C") || key.equals("DEL") ? "#DC2626" : "#0F172A");

                Button btn = createCalcButton(key, bg, fg, 14, true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
                lp.setMargins(dp(2), 0, dp(2), 0);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> handleProgrammerKey(key));
                rowLayout.addView(btn);
            }
            panel.addView(rowLayout);
        }

        updateProgrammerDisplays();
        return panel;
    }

    private TextView createRadixRow(LinearLayout parent, String radixName, String initialVal) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView label = new TextView(activity);
        label.setText(radixName);
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        label.setLayoutParams(new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(label);

        TextView val = new TextView(activity);
        val.setText(initialVal);
        val.setTextSize(13);
        val.setTypeface(Typeface.MONOSPACE);
        val.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        val.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        val.setTextIsSelectable(true);
        row.addView(val);

        row.setClickable(true);
        row.setOnClickListener(v -> {
            progActiveRadix = radixName;
            Toast.makeText(activity, "Active Radix: " + radixName, Toast.LENGTH_SHORT).show();
        });

        parent.addView(row);
        return val;
    }

    private void renderProgrammerBitGrid() {
        progBitGridContainer.removeAllViews();
        int totalBits = progWidth.bits;

        // Group into rows of 16 bits (or 8 bits for BYTE)
        int bitsPerRow = (totalBits <= 16) ? 8 : 16;
        int rows = totalBits / bitsPerRow;

        for (int r = 0; r < rows; r++) {
            LinearLayout rowLayout = new LinearLayout(activity);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            rowLayout.setPadding(0, dp(2), 0, dp(2));

            int startBit = totalBits - 1 - (r * bitsPerRow);
            int endBit = startBit - bitsPerRow + 1;

            TextView label = new TextView(activity);
            label.setText(startBit + "..");
            label.setTextSize(9);
            label.setTextColor(isDarkTheme() ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
            label.setLayoutParams(new LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));
            rowLayout.addView(label);

            for (int b = startBit; b >= endBit; b--) {
                final int bitIdx = b;
                boolean isSet = (progCurrentValue & (1L << bitIdx)) != 0;

                TextView bitView = new TextView(activity);
                bitView.setText(isSet ? "1" : "0");
                bitView.setTextSize(10);
                bitView.setTypeface(Typeface.MONOSPACE);
                bitView.setGravity(Gravity.CENTER);
                bitView.setTextColor(isSet ? Color.parseColor("#D97706") : (isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B")));
                bitView.setBackground(createPillDrawable(isSet ? (isDarkTheme() ? 0xFF3D3018 : Color.parseColor("#FEF3C7")) : (isDarkTheme() ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F1F5F9")), dp(4)));

                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, dp(24), 1.0f);
                bLp.setMargins(dp(1), 0, dp(1), 0);
                bitView.setLayoutParams(bLp);
                bitView.setClickable(true);
                bitView.setOnClickListener(v -> {
                    progCurrentValue = ProgrammingEngine.toggleBit(progCurrentValue, bitIdx, progWidth);
                    updateProgrammerDisplays();
                    renderProgrammerBitGrid();
                });

                rowLayout.addView(bitView);
            }
            progBitGridContainer.addView(rowLayout);
        }
    }

    private void updateProgrammerDisplays() {
        ProgrammingEngine.EvaluationResult res = new ProgrammingEngine.EvaluationResult(progCurrentValue, progWidth, progSigned, false, null);
        if (progHexView != null) progHexView.setText(res.toHexString());
        if (progDecView != null) progDecView.setText(res.toDecimalString());
        if (progOctView != null) progOctView.setText(res.toOctalString());
        if (progBinView != null) progBinView.setText(res.toBinaryString());
    }

    private void handleHexDigit(String digit) {
        try {
            int d = Integer.parseInt(digit, 16);
            progCurrentValue = ((progCurrentValue << 4) | d) & progWidth.getMask();
            updateProgrammerDisplays();
            renderProgrammerBitGrid();
        } catch (Exception ignored) {}
    }

    private void handleBitwiseOp(String op) {
        switch (op) {
            case "NOT":
                progCurrentValue = ProgrammingEngine.bitwiseNot(progCurrentValue, progWidth);
                break;
            case "SHL":
                progCurrentValue = ProgrammingEngine.shiftLeft(progCurrentValue, 1, progWidth);
                break;
            case "SHR":
                progCurrentValue = ProgrammingEngine.shiftRightLogical(progCurrentValue, 1, progWidth);
                break;
            default:
                Toast.makeText(activity, "Operator " + op + " armed", Toast.LENGTH_SHORT).show();
                break;
        }
        updateProgrammerDisplays();
        renderProgrammerBitGrid();
    }

    private void handleProgrammerKey(String key) {
        switch (key) {
            case "C":
                progCurrentValue = 0L;
                updateProgrammerDisplays();
                renderProgrammerBitGrid();
                break;
            case "DEL":
                progCurrentValue = (progCurrentValue >>> 4) & progWidth.getMask();
                updateProgrammerDisplays();
                renderProgrammerBitGrid();
                break;
            case "ROL":
                progCurrentValue = ProgrammingEngine.rotateLeft(progCurrentValue, 1, progWidth);
                updateProgrammerDisplays();
                renderProgrammerBitGrid();
                break;
            case "ROR":
                progCurrentValue = ProgrammingEngine.rotateRight(progCurrentValue, 1, progWidth);
                updateProgrammerDisplays();
                renderProgrammerBitGrid();
                break;
            default:
                if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
                    int d = Integer.parseInt(key);
                    progCurrentValue = ((progCurrentValue * 10) + d) & progWidth.getMask();
                    updateProgrammerDisplays();
                    renderProgrammerBitGrid();
                }
                break;
        }
    }

    // =========================================================================
    // 3. UNITS & DIMENSIONAL ANALYSIS PANEL
    // =========================================================================
    private View createUnitsPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 1. Category Selector Chips
        HorizontalScrollView catScroll = new HorizontalScrollView(activity);
        catScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout catContainer = new LinearLayout(activity);
        catContainer.setOrientation(LinearLayout.HORIZONTAL);
        catContainer.setPadding(0, dp(4), 0, dp(8));

        List<String> categories = UnitRegistry.getCategories();
        for (String cat : categories) {
            TextView catChip = new TextView(activity);
            catChip.setText(cat);
            catChip.setTextSize(11);
            boolean isCur = cat.equals(selectedUnitCategory);
            catChip.setTextColor(Color.parseColor(isCur ? "#FFFFFF" : (isDarkTheme() ? "#CBD5E1" : "#475569")));
            catChip.setBackground(createPillDrawable(isCur ? Color.parseColor("#D97706") : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(10)));
            catChip.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(6), 0);
            catChip.setLayoutParams(lp);
            catChip.setClickable(true);
            catChip.setOnClickListener(v -> {
                selectedUnitCategory = cat;
                renderSubModeUI();
            });
            catContainer.addView(catChip);
        }
        catScroll.addView(catContainer);
        panel.addView(catScroll);

        // 2. Conversion Card
        LinearLayout convCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        convCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        convCard.setOrientation(LinearLayout.VERTICAL);

        // From row
        LinearLayout fromRow = new LinearLayout(activity);
        fromRow.setOrientation(LinearLayout.HORIZONTAL);
        fromRow.setGravity(Gravity.CENTER_VERTICAL);

        unitFromInput = new EditText(activity);
        unitFromInput.setText("1");
        unitFromInput.setTextSize(16);
        unitFromInput.setTypeface(Typeface.DEFAULT_BOLD);
        unitFromInput.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        unitFromInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        fromRow.addView(unitFromInput);

        unitFromSpinner = new Spinner(activity);
        fromRow.addView(unitFromSpinner);
        convCard.addView(fromRow);

        // Arrow indicator
        TextView arrow = new TextView(activity);
        arrow.setText("↓ converts to");
        arrow.setTextSize(11);
        arrow.setTextColor(isDarkTheme() ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
        arrow.setPadding(dp(4), dp(6), dp(4), dp(6));
        convCard.addView(arrow);

        // To row
        LinearLayout toRow = new LinearLayout(activity);
        toRow.setOrientation(LinearLayout.HORIZONTAL);
        toRow.setGravity(Gravity.CENTER_VERTICAL);

        unitResultView = new TextView(activity);
        unitResultView.setText("...");
        unitResultView.setTextSize(18);
        unitResultView.setTypeface(Typeface.DEFAULT_BOLD);
        unitResultView.setTextColor(isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706"));
        unitResultView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        unitResultView.setTextIsSelectable(true);
        toRow.addView(unitResultView);

        unitToSpinner = new Spinner(activity);
        toRow.addView(unitToSpinner);
        convCard.addView(toRow);

        // Formula / canonical factor readout
        unitFormulaView = new TextView(activity);
        unitFormulaView.setText("SI Base Conversion Factor");
        unitFormulaView.setTextSize(10);
        unitFormulaView.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        unitFormulaView.setPadding(0, dp(8), 0, 0);
        convCard.addView(unitFormulaView);

        panel.addView(convCard);

        // Populate Spinners for Selected Category
        List<UnitDefinition> units = UnitRegistry.getUnitsForCategory(selectedUnitCategory);
        List<String> unitNames = new ArrayList<>();
        for (UnitDefinition u : units) {
            unitNames.add(u.symbol + " (" + u.name + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, unitNames);
        unitFromSpinner.setAdapter(adapter);
        unitToSpinner.setAdapter(adapter);
        if (units.size() > 1) {
            unitToSpinner.setSelection(1);
        }

        AdapterView.OnItemSelectedListener spinListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performUnitConversion();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        unitFromSpinner.setOnItemSelectedListener(spinListener);
        unitToSpinner.setOnItemSelectedListener(spinListener);

        unitFromInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performUnitConversion();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        performUnitConversion();
        return panel;
    }

    private void performUnitConversion() {
        if (unitFromSpinner == null || unitToSpinner == null || unitFromInput == null || unitResultView == null) return;
        List<UnitDefinition> units = UnitRegistry.getUnitsForCategory(selectedUnitCategory);
        int fromIdx = unitFromSpinner.getSelectedItemPosition();
        int toIdx = unitToSpinner.getSelectedItemPosition();

        if (fromIdx < 0 || fromIdx >= units.size() || toIdx < 0 || toIdx >= units.size()) return;

        UnitDefinition from = units.get(fromIdx);
        UnitDefinition to = units.get(toIdx);

        try {
            double val = Double.parseDouble(unitFromInput.getText().toString().trim());
            double converted = engine.getUnitRegistry().convert(val, from, to);
            unitResultView.setText(engine.getContextConfig().format(converted) + " " + to.symbol);
            unitFormulaView.setText("1 " + from.symbol + " = " + engine.getContextConfig().format(from.convertTo(1.0, to)) + " " + to.symbol);
        } catch (Exception e) {
            unitResultView.setText("---");
        }
    }

    // =========================================================================
    // 4. MATRIX & VECTOR PANEL
    // =========================================================================
    private View createMatrixVectorPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 1. Matrix Sub-card
        LinearLayout mCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        mCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        mCard.setOrientation(LinearLayout.VERTICAL);

        TextView mTitle = new TextView(activity);
        mTitle.setText("🔲 MATRIX COMPUTATION ENGINE (LU DECOMPOSITION)");
        mTitle.setTextSize(11);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setTextColor(Color.parseColor("#D97706"));
        mCard.addView(mTitle);

        // Dim Selector (2x2 or 3x3)
        LinearLayout dimRow = new LinearLayout(activity);
        dimRow.setOrientation(LinearLayout.HORIZONTAL);
        dimRow.setPadding(0, dp(6), 0, dp(6));

        TextView dim2Btn = new TextView(activity);
        dim2Btn.setText("2x2");
        dim2Btn.setTextSize(10);
        dim2Btn.setTypeface(Typeface.DEFAULT_BOLD);
        dim2Btn.setTextColor(Color.parseColor(matrixDim == 2 ? "#FFFFFF" : (isDarkTheme() ? "#CBD5E1" : "#475569")));
        dim2Btn.setBackground(createPillDrawable(matrixDim == 2 ? Color.parseColor("#D97706") : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
        dim2Btn.setPadding(dp(10), dp(4), dp(10), dp(4));
        dim2Btn.setClickable(true);
        dim2Btn.setOnClickListener(v -> {
            matrixDim = 2;
            renderSubModeUI();
        });
        dimRow.addView(dim2Btn);

        TextView dim3Btn = new TextView(activity);
        dim3Btn.setText("3x3");
        dim3Btn.setTextSize(10);
        dim3Btn.setTypeface(Typeface.DEFAULT_BOLD);
        dim3Btn.setTextColor(Color.parseColor(matrixDim == 3 ? "#FFFFFF" : (isDarkTheme() ? "#CBD5E1" : "#475569")));
        dim3Btn.setBackground(createPillDrawable(matrixDim == 3 ? Color.parseColor("#D97706") : (isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9")), dp(6)));
        dim3Btn.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams d3Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        d3Lp.setMargins(dp(6), 0, 0, 0);
        dim3Btn.setLayoutParams(d3Lp);
        dim3Btn.setClickable(true);
        dim3Btn.setOnClickListener(v -> {
            matrixDim = 3;
            renderSubModeUI();
        });
        dimRow.addView(dim3Btn);
        mCard.addView(dimRow);

        // Matrix Inputs Grid
        matrixAInputs = new EditText[matrixDim][matrixDim];
        for (int i = 0; i < matrixDim; i++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(2), 0, dp(2));
            for (int j = 0; j < matrixDim; j++) {
                EditText cell = new EditText(activity);
                cell.setText(i == j ? "1" : "0");
                cell.setTextSize(13);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
                cell.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F8FAFC"), dp(6)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
                lp.setMargins(dp(2), 0, dp(2), 0);
                cell.setLayoutParams(lp);
                matrixAInputs[i][j] = cell;
                row.addView(cell);
            }
            mCard.addView(row);
        }

        // Action Buttons: Det, Inv, Transpose, Rank, Trace
        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(8), 0, dp(4));
        String[] mActions = {"Det", "Inverse", "Transpose", "Rank", "Trace"};
        for (String act : mActions) {
            Button aBtn = createCalcButton(act, "#FEF3C7", "#D97706", 11, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            aBtn.setLayoutParams(lp);
            aBtn.setOnClickListener(v -> executeMatrixAction(act));
            actionRow.addView(aBtn);
        }
        mCard.addView(actionRow);

        // Result display
        matrixResultView = new TextView(activity);
        matrixResultView.setText("Result will appear here");
        matrixResultView.setTextSize(12);
        matrixResultView.setTypeface(Typeface.MONOSPACE);
        matrixResultView.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        matrixResultView.setPadding(0, dp(8), 0, dp(4));
        matrixResultView.setTextIsSelectable(true);
        mCard.addView(matrixResultView);
        panel.addView(mCard);

        // 2. 3D Vector Sub-card
        LinearLayout vCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        vCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams vcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vcLp.setMargins(0, dp(10), 0, 0);
        vCard.setLayoutParams(vcLp);
        vCard.setOrientation(LinearLayout.VERTICAL);

        TextView vTitle = new TextView(activity);
        vTitle.setText("📐 3D VECTOR COMPUTATION ENGINE");
        vTitle.setTextSize(11);
        vTitle.setTypeface(Typeface.DEFAULT_BOLD);
        vTitle.setTextColor(isDarkTheme() ? 0xFF38BDF8 : Color.parseColor("#0284C7"));
        vCard.addView(vTitle);

        // Vector A inputs
        LinearLayout rowA = new LinearLayout(activity);
        rowA.setOrientation(LinearLayout.HORIZONTAL);
        rowA.setGravity(Gravity.CENTER_VERTICAL);
        rowA.setPadding(0, dp(4), 0, dp(2));
        TextView lblA = new TextView(activity);
        lblA.setText("A: ");
        lblA.setTextSize(11);
        lblA.setTypeface(Typeface.DEFAULT_BOLD);
        lblA.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        rowA.addView(lblA);

        vecAx = createVectorCell("1");
        vecAy = createVectorCell("2");
        vecAz = createVectorCell("3");
        rowA.addView(vecAx);
        rowA.addView(vecAy);
        rowA.addView(vecAz);
        vCard.addView(rowA);

        // Vector B inputs
        LinearLayout rowB = new LinearLayout(activity);
        rowB.setOrientation(LinearLayout.HORIZONTAL);
        rowB.setGravity(Gravity.CENTER_VERTICAL);
        rowB.setPadding(0, dp(2), 0, dp(4));
        TextView lblB = new TextView(activity);
        lblB.setText("B: ");
        lblB.setTextSize(11);
        lblB.setTypeface(Typeface.DEFAULT_BOLD);
        lblB.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        rowB.addView(lblB);

        vecBx = createVectorCell("4");
        vecBy = createVectorCell("5");
        vecBz = createVectorCell("6");
        rowB.addView(vecBx);
        rowB.addView(vecBy);
        rowB.addView(vecBz);
        vCard.addView(rowB);

        // Vector operations: Dot, Cross, Mag(A), Norm(A), Angle
        LinearLayout vOps = new LinearLayout(activity);
        vOps.setOrientation(LinearLayout.HORIZONTAL);
        vOps.setPadding(0, dp(6), 0, dp(4));
        String[] vActions = {"A · B", "A × B", "|A|", "Norm A", "Angle"};
        for (String va : vActions) {
            Button vBtn = createCalcButton(va, "#E0F2FE", "#0284C7", 11, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            vBtn.setLayoutParams(lp);
            vBtn.setOnClickListener(v -> executeVectorAction(va));
            vOps.addView(vBtn);
        }
        vCard.addView(vOps);

        vecResultView = new TextView(activity);
        vecResultView.setText("Vector result will appear here");
        vecResultView.setTextSize(12);
        vecResultView.setTypeface(Typeface.MONOSPACE);
        vecResultView.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        vecResultView.setPadding(0, dp(6), 0, 0);
        vCard.addView(vecResultView);
        panel.addView(vCard);

        return panel;
    }

    private EditText createVectorCell(String def) {
        EditText et = new EditText(activity);
        et.setText(def);
        et.setTextSize(12);
        et.setGravity(Gravity.CENTER);
        et.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        et.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F8FAFC"), dp(6)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        et.setLayoutParams(lp);
        return et;
    }

    private void executeMatrixAction(String action) {
        try {
            double[][] data = new double[matrixDim][matrixDim];
            for (int i = 0; i < matrixDim; i++) {
                for (int j = 0; j < matrixDim; j++) {
                    data[i][j] = Double.parseDouble(matrixAInputs[i][j].getText().toString().trim());
                }
            }
            MatrixEngine M = new MatrixEngine(data);
            switch (action) {
                case "Det":
                    matrixResultView.setText("det(A) = " + engine.getContextConfig().format(M.determinant()));
                    break;
                case "Inverse":
                    matrixResultView.setText("inv(A) =\n" + M.inverse().format(4));
                    break;
                case "Transpose":
                    matrixResultView.setText("A^T =\n" + M.transpose().format(4));
                    break;
                case "Rank":
                    matrixResultView.setText("rank(A) = " + M.rank());
                    break;
                case "Trace":
                    matrixResultView.setText("trace(A) = " + engine.getContextConfig().format(M.trace()));
                    break;
            }
        } catch (Exception e) {
            matrixResultView.setText("Error: " + e.getMessage());
        }
    }

    private void executeVectorAction(String action) {
        try {
            Vector3D A = new Vector3D(
                    Double.parseDouble(vecAx.getText().toString().trim()),
                    Double.parseDouble(vecAy.getText().toString().trim()),
                    Double.parseDouble(vecAz.getText().toString().trim())
            );
            Vector3D B = new Vector3D(
                    Double.parseDouble(vecBx.getText().toString().trim()),
                    Double.parseDouble(vecBy.getText().toString().trim()),
                    Double.parseDouble(vecBz.getText().toString().trim())
            );

            switch (action) {
                case "A · B":
                    vecResultView.setText("A · B = " + engine.getContextConfig().format(A.dot(B)));
                    break;
                case "A × B":
                    Vector3D c = A.cross(B);
                    vecResultView.setText(String.format(Locale.US, "A × B = (%.4f, %.4f, %.4f)", c.x, c.y, c.z));
                    break;
                case "|A|":
                    vecResultView.setText("|A| = " + engine.getContextConfig().format(A.magnitude()));
                    break;
                case "Norm A":
                    Vector3D n = A.normalize();
                    vecResultView.setText(String.format(Locale.US, "u_A = (%.4f, %.4f, %.4f)", n.x, n.y, n.z));
                    break;
                case "Angle":
                    double deg = A.angleBetween(B) * 180.0 / Math.PI;
                    vecResultView.setText(String.format(Locale.US, "θ = %.4f°", deg));
                    break;
            }
        } catch (Exception e) {
            vecResultView.setText("Error: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. INTERACTIVE FUNCTION GRAPHER PANEL
    // =========================================================================
    private View createGraphPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        // 1. Function input & presets card
        LinearLayout inputCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
        inputCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        inputCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout inRow = new LinearLayout(activity);
        inRow.setOrientation(LinearLayout.HORIZONTAL);
        inRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView yEq = new TextView(activity);
        yEq.setText("y = f(x) = ");
        yEq.setTextSize(12);
        yEq.setTypeface(Typeface.DEFAULT_BOLD);
        yEq.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        inRow.addView(yEq);

        graphFunctionInput = new EditText(activity);
        graphFunctionInput.setText(graphView != null ? graphView.getFunction() : "sin(x)");
        graphFunctionInput.setTextSize(13);
        graphFunctionInput.setTypeface(Typeface.MONOSPACE);
        graphFunctionInput.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
        graphFunctionInput.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceInput(true) : Color.parseColor("#F8FAFC"), dp(6)));
        graphFunctionInput.setPadding(dp(8), dp(4), dp(8), dp(4));
        graphFunctionInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        inRow.addView(graphFunctionInput);

        Button plotBtn = createCalcButton("Plot", "#D97706", "#FFFFFF", 12, true);
        plotBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(36)));
        plotBtn.setOnClickListener(v -> {
            if (graphView != null) {
                graphView.setFunction(graphFunctionInput.getText().toString().trim());
            }
        });
        inRow.addView(plotBtn);
        inputCard.addView(inRow);

        // Preset function chips
        HorizontalScrollView preScroll = new HorizontalScrollView(activity);
        preScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout preContainer = new LinearLayout(activity);
        preContainer.setOrientation(LinearLayout.HORIZONTAL);
        preContainer.setPadding(0, dp(6), 0, 0);

        String[] presets = {"sin(x)", "cos(x)", "tan(x)", "x^2 - 4", "1/x", "exp(-x^2)", "x^3 - 3*x"};
        for (String p : presets) {
            TextView pChip = new TextView(activity);
            pChip.setText(p);
            pChip.setTextSize(10);
            pChip.setTextColor(Color.parseColor(isDarkTheme() ? "#CBD5E1" : "#475569"));
            pChip.setBackground(createPillDrawable(isDarkTheme() ? DesignTokens.getSurfaceElevated(true) : Color.parseColor("#F1F5F9"), dp(6)));
            pChip.setPadding(dp(8), dp(3), dp(8), dp(3));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(4), 0);
            pChip.setLayoutParams(lp);
            pChip.setClickable(true);
            pChip.setOnClickListener(v -> {
                graphFunctionInput.setText(p);
                if (graphView != null) graphView.setFunction(p);
            });
            preContainer.addView(pChip);
        }
        preScroll.addView(preContainer);
        inputCard.addView(preScroll);
        panel.addView(inputCard);

        // 2. Interactive Graph View Canvas
        if (graphView == null) {
            graphView = new InteractiveGraphView(activity);
        }
        graphView.setDarkTheme(isDarkTheme());
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260));
        gLp.setMargins(0, dp(10), 0, dp(6));
        graphView.setLayoutParams(gLp);
        panel.addView(graphView);

        // 3. Zoom / Reset Controls Bar
        LinearLayout ctrlRow = new LinearLayout(activity);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER);

        Button zoomInBtn = createCalcButton("+ Zoom In", "#F1F5F9", "#0F172A", 11, false);
        zoomInBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(90), dp(34)));
        zoomInBtn.setOnClickListener(v -> { if (graphView != null) graphView.zoom(0.7); });
        ctrlRow.addView(zoomInBtn);

        Button zoomOutBtn = createCalcButton("- Zoom Out", "#F1F5F9", "#0F172A", 11, false);
        LinearLayout.LayoutParams zoLp = new LinearLayout.LayoutParams(dp(90), dp(34));
        zoLp.setMargins(dp(8), 0, dp(8), 0);
        zoomOutBtn.setLayoutParams(zoLp);
        zoomOutBtn.setOnClickListener(v -> { if (graphView != null) graphView.zoom(1.4); });
        ctrlRow.addView(zoomOutBtn);

        Button resetBtn = createCalcButton("⟲ Reset", "#F1F5F9", "#0F172A", 11, false);
        resetBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(34)));
        resetBtn.setOnClickListener(v -> { if (graphView != null) graphView.resetView(); });
        ctrlRow.addView(resetBtn);

        panel.addView(ctrlRow);
        return panel;
    }

    // =========================================================================
    // 6. PHYSICAL CONSTANTS PANEL (CODATA 2022)
    // =========================================================================
    private View createConstantsPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        TextView intro = new TextView(activity);
        intro.setText("CODATA 2022 RECOMMENDED PHYSICAL & ASTRONOMICAL CONSTANTS");
        intro.setTextSize(10);
        intro.setTypeface(Typeface.DEFAULT_BOLD);
        intro.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        intro.setPadding(dp(2), 0, dp(2), dp(8));
        panel.addView(intro);

        for (PhysicalConstants.ConstantItem item : PhysicalConstants.getAllConstants()) {
            LinearLayout cCard = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            cCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            cCard.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            cCard.setLayoutParams(lp);

            LinearLayout top = new LinearLayout(activity);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView sym = new TextView(activity);
            sym.setText(item.symbol);
            sym.setTextSize(13);
            sym.setTypeface(Typeface.DEFAULT_BOLD);
            sym.setTextColor(isDarkTheme() ? 0xFFF59E0B : Color.parseColor("#D97706"));
            top.addView(sym);

            TextView name = new TextView(activity);
            name.setText(" • " + item.name);
            name.setTextSize(11);
            name.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            top.addView(name);

            Button insBtn = createCalcButton("Insert", "#FEF3C7", "#D97706", 10, true);
            insBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(54), dp(28)));
            insBtn.setOnClickListener(v -> {
                currentFormula.append(item.symbol);
                switchMode(SubMode.SCIENTIFIC);
                Toast.makeText(activity, "Inserted " + item.symbol, Toast.LENGTH_SHORT).show();
            });
            top.addView(insBtn);
            cCard.addView(top);

            TextView val = new TextView(activity);
            val.setText(String.format(Locale.US, "%.8e %s", item.value, item.unit));
            val.setTextSize(11);
            val.setTypeface(Typeface.MONOSPACE);
            val.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#475569"));
            val.setPadding(0, dp(2), 0, 0);
            cCard.addView(val);

            panel.addView(cCard);
        }

        return panel;
    }

    // =========================================================================
    // 7. CALCULATION HISTORY PANEL
    // =========================================================================
    private View createHistoryPanel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);

        LinearLayout hRow = new LinearLayout(activity);
        hRow.setOrientation(LinearLayout.HORIZONTAL);
        hRow.setGravity(Gravity.CENTER_VERTICAL);
        hRow.setPadding(0, 0, 0, dp(8));

        TextView title = new TextView(activity);
        title.setText("RECENT CALCULATIONS");
        title.setTextSize(11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        hRow.addView(title);

        Button clearBtn = createCalcButton("Clear All", "#FEE2E2", "#DC2626", 10, true);
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(30)));
        clearBtn.setOnClickListener(v -> {
            historyManager.clearHistory();
            renderSubModeUI();
        });
        hRow.addView(clearBtn);
        panel.addView(hRow);

        List<CalculatorHistoryManager.HistoryItem> entries = historyManager.getHistory();
        if (entries.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No calculation history yet.");
            empty.setTextSize(12);
            empty.setTextColor(isDarkTheme() ? 0xFF94A3B8 : Color.parseColor("#94A3B8"));
            empty.setPadding(dp(8), dp(20), dp(8), dp(20));
            empty.setGravity(Gravity.CENTER);
            panel.addView(empty);
            return panel;
        }

        for (CalculatorHistoryManager.HistoryItem e : entries) {
            LinearLayout item = createCardLayout(Color.WHITE, Color.parseColor("#E2E8F0"));
            item.setPadding(dp(12), dp(10), dp(12), dp(10));
            item.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            item.setLayoutParams(lp);

            TextView expr = new TextView(activity);
            expr.setText(e.expression);
            expr.setTextSize(12);
            expr.setTypeface(Typeface.MONOSPACE);
            expr.setTextColor(isDarkTheme() ? 0xFFCBD5E1 : Color.parseColor("#64748B"));
            item.addView(expr);

            TextView res = new TextView(activity);
            res.setText("= " + e.result);
            res.setTextSize(14);
            res.setTypeface(Typeface.DEFAULT_BOLD);
            res.setTextColor(isDarkTheme() ? 0xFFF8FAFC : Color.parseColor("#0F172A"));
            item.addView(res);

            item.setClickable(true);
            item.setOnClickListener(v -> {
                currentFormula.setLength(0);
                currentFormula.append(e.expression);
                switchMode(SubMode.SCIENTIFIC);
            });

            panel.addView(item);
        }

        return panel;
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================
    private boolean isDarkTheme() {
        return com.devendra.healthtracker.theme.ThemeManager.isDarkMode(activity);
    }

    private Button createCalcButton(String text, String bgColor, String fgColor, int textSizeSp, boolean isBold) {
        PremiumKeypadButton btn = new PremiumKeypadButton(activity);
        btn.setText(text);
        btn.setTextSize(textSizeSp);
        btn.setDarkTheme(isDarkTheme());

        PremiumKeypadButton.KeyType type;
        if (text.equals("=") || text.equalsIgnoreCase("Plot")) {
            type = PremiumKeypadButton.KeyType.ACTION_EQUALS;
        } else if (text.equals("+") || text.equals("-") || text.equals("*") || text.equals("/") ||
                   text.equals("%") || text.equals("+/-") || text.equalsIgnoreCase("mod")) {
            type = PremiumKeypadButton.KeyType.OPERATOR;
        } else if (text.equalsIgnoreCase("C") || text.equalsIgnoreCase("DEL") || text.equalsIgnoreCase("AC") ||
                   text.toLowerCase().contains("clear")) {
            type = PremiumKeypadButton.KeyType.ACTION_CLEAR;
        } else if (text.equals("MC") || text.equals("MR") || text.equals("M+") || text.equals("M-") || text.equals("MS")) {
            type = PremiumKeypadButton.KeyType.MEMORY;
        } else if (text.equals("A") || text.equals("B") || text.equals("C") || text.equals("D") || text.equals("E") || text.equals("F")
                   || text.equals("AND") || text.equals("OR") || text.equals("XOR") || text.equals("NOT") || text.equals("LSH") || text.equals("RSH")) {
            type = PremiumKeypadButton.KeyType.RADIX;
        } else if (text.equals(".") || text.matches("\\d+")) {
            type = PremiumKeypadButton.KeyType.NUMBER;
        } else {
            type = PremiumKeypadButton.KeyType.FUNCTION;
        }

        btn.setKeyType(type);
        return btn;
    }

    private LinearLayout createCardLayout(int bgColor, int strokeColor) {
        PremiumCard layout = new PremiumCard(activity, isDarkTheme());
        int finalBg = bgColor;
        int finalStroke = strokeColor;
        if (isDarkTheme()) {
            if (bgColor == Color.WHITE || bgColor == 0xFFFFFFFF) {
                finalBg = DesignTokens.getSurfaceCard(true);
                finalStroke = DesignTokens.getBorder(true);
            } else if (bgColor == Color.parseColor("#FFFBEB")) {
                finalBg = DesignTokens.ACCENT_AMBER_CONTAINER_DARK;
                finalStroke = DesignTokens.getBorderSubtle(true);
            } else {
                finalBg = DesignTokens.getSurfaceCard(true);
                finalStroke = DesignTokens.getBorder(true);
            }
        }
        layout.setCustomColors(finalBg, finalStroke);
        layout.setCornerRadiusDp(DesignTokens.RADIUS_MD);
        return layout;
    }

    private GradientDrawable createPillDrawable(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private int dp(int val) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (val * density + 0.5f);
    }
}
