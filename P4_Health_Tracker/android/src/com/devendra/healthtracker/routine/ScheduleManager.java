package com.devendra.healthtracker.routine;

import com.devendra.healthtracker.AppEngineManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ScheduleManager {

    public static final String PREF_NAME = "RoseSchedulePrefs";
    public static final String KEY_WAKE_HOUR = "wake_hour";
    public static final String KEY_WAKE_MIN = "wake_minute";
    public static final String KEY_SLEEP_HOUR = "sleep_hour";
    public static final String KEY_SLEEP_MIN = "sleep_minute";

    public static class Phase {
        public final String id;
        public final int startHour;
        public final int startMinute;
        public final int endHour;
        public final int endMinute;
        public final String romanWindow;
        public final String title;
        public final String decodedProtocol;
        public final String biologicalWhy;
        public final List<RoutineBlock> granularBlocks;

        public Phase(String id, int startHour, int startMinute, int endHour, int endMinute,
                     String romanWindow, String title, String decodedProtocol, String biologicalWhy) {
            this(id, startHour, startMinute, endHour, endMinute, romanWindow, title, decodedProtocol, biologicalWhy, new ArrayList<RoutineBlock>());
        }

        public Phase(String id, int startHour, int startMinute, int endHour, int endMinute,
                     String romanWindow, String title, String decodedProtocol, String biologicalWhy,
                     List<RoutineBlock> granularBlocks) {
            this.id = id;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
            this.romanWindow = romanWindow;
            this.title = title;
            this.decodedProtocol = decodedProtocol;
            this.biologicalWhy = biologicalWhy;
            this.granularBlocks = granularBlocks != null ? new ArrayList<RoutineBlock>(granularBlocks) : new ArrayList<RoutineBlock>();
        }

        public List<RoutineBlock> getGranularBlocks() {
            return Collections.unmodifiableList(granularBlocks);
        }

        public String getDisplayTitle() {
            if ("phase-08".equals(id)) {
                Calendar now = Calendar.getInstance();
                String timePrefix = formatTime12(startHour, startMinute);
                if (now.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
                    return "Evening Fuel: " + timePrefix + " Paneer (Tuesday Zero-Egg)";
                } else {
                    return "Evening Fuel: " + timePrefix + " Eggs & Creatine";
                }
            }
            return title;
        }

        public String getDisplayProtocol() {
            if ("phase-08".equals(id)) {
                Calendar now = Calendar.getInstance();
                if (now.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
                    return "Tuesday (Zero-Egg): 100g Fresh Paneer (or Tofu) + 25g–30g Roasted Peanuts + 35g–40g Roasted Chana + 15g Pumpkin Seeds + 10g Lemon Juice + 3–5g Creatine in water.";
                } else {
                    return "4 Whole Boiled Eggs (~200g) with rock salt & pepper + 25g–30g Roasted Peanuts + 35g–40g Roasted Chana + 15g Pumpkin Seeds + 10g Lemon Juice + 3–5g Creatine in water.";
                }
            }
            return decodedProtocol;
        }

        public String getFormattedTime12() {
            return formatTime12(startHour, startMinute) + " – " + formatTime12(endHour, endMinute);
        }

        public String getFormattedTime24() {
            return String.format("%02d:%02d – %02d:%02d", startHour, startMinute, endHour, endMinute);
        }

        public static String formatTime12(int h, int m) {
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12;
            if (h12 == 0) h12 = 12;
            return String.format("%02d:%02d %s", h12, m, ampm);
        }
    }

    public static class PhaseTemplate {
        public final String id;
        public final String title;
        public final String protocol;
        public final String why;
        public final int baseDuration; // in minutes
        public final List<RoutineBlock> defaultBlocks;

        public PhaseTemplate(String id, String title, String protocol, String why, int baseDuration) {
            this(id, title, protocol, why, baseDuration, new ArrayList<RoutineBlock>());
        }

        public PhaseTemplate(String id, String title, String protocol, String why, int baseDuration, List<RoutineBlock> defaultBlocks) {
            this.id = id;
            this.title = title;
            this.protocol = protocol;
            this.why = why;
            this.baseDuration = baseDuration;
            this.defaultBlocks = defaultBlocks != null ? defaultBlocks : new ArrayList<RoutineBlock>();
        }
    }

    private static final PhaseTemplate[] WAKING_TEMPLATES = new PhaseTemplate[]{
        new PhaseTemplate(
            "phase-01",
            "Wakeup, Hygiene (Brush, Face Wash & Bath) & Hydration",
            "Wake up without snooze + brush teeth & tongue + gentle face wash + refreshing bath/shower + 500g warm water with 1.5g-2g Sendha Namak & 10g-15g fresh lemon.",
            "Clears overnight oral biofilm, removes facial sebum, stimulates autonomic alertness via shower, rehydrates brain neurons, and initiates cortisol awakening.",
            30,
            Arrays.asList(
                new RoutineBlock("block-01-wakeup", "phase-01", "☀️ Wake Up & Out of Bed", "Immediate rise upon alarm to initiate Cortisol Awakening Response (CAR)", "WAKE", "08:00 AM"),
                new RoutineBlock("block-01-brush", "phase-01", "🪥 Brush Teeth", "Oral hygiene & tongue cleaning to clear overnight bacterial biofilm and stimulate salivary flow", "ROUTINE", "08:05 AM"),
                new RoutineBlock("block-01-facewash", "phase-01", "🧼 Face Wash", "Gentle facial wash to clear overnight sebum, unblock pores, and refresh skin before shower", "ROUTINE", "08:10 AM"),
                new RoutineBlock("block-01-bath", "phase-01", "🚿 Bath / Shower", "Invigorating shower to elevate alertness, stimulate sympathetic tone, and complete morning hygiene", "ROUTINE", "08:15 AM"),
                new RoutineBlock("block-01-water", "phase-01", "💧 500g (500ml) Warm Water", "Rehydrates brain neurons, expands blood volume, flushes overnight cellular wastes", "HYDRATION", "08:20 AM"),
                new RoutineBlock("block-01-salt", "phase-01", "🧂 1.5g–2g Sendha Namak (Rock Salt)", "Provides pure sodium, potassium & trace minerals for cellular osmotic balance", "INGREDIENT", "08:25 AM"),
                new RoutineBlock("block-01-lemon", "phase-01", "🍋 10g–15g Fresh Lemon Juice Squeeze", "Ascorbic acid & citrate prime digestive enzymatic pathways without insulin spike", "INGREDIENT", "08:28 AM")
            )
        ),
        new PhaseTemplate(
            "phase-02",
            "Morning Sunlight (15 min Outdoor)",
            "15–20 minutes direct outdoor sunlight exposure on skin and eyes (no sunglasses).",
            "Resets circadian clock, suppresses residual melatonin, and triggers dopamine synthesis.",
            15,
            Arrays.asList(
                new RoutineBlock("block-02-sunlight", "phase-02", "☀️ 15-Minute Direct Morning Sunlight", "Outdoor sky/sun exposure (UV 0-1) to reset SCN circadian clock & suppress residual melatonin", "ROUTINE", "08:30 – 08:45 AM")
            )
        ),
        new PhaseTemplate(
            "phase-03",
            "Breakfast: Morning Brain Fuel",
            "1 Atta Roti (40g) + 25g–30g PB/Peanuts + 35g-40g Roasted Chana + 1 Banana (100g) + 15g Soaked Almonds + 15g–20g Walnuts + 12g Chia + 1.5g-3g Moringa (+ Sun: D3).",
            "Supplies L-Tyrosine, Vitamin B6, ALA Omega-3, MUFAs, Zinc, and Magnesium to power 4h AI/ML sprint.",
            45,
            Arrays.asList(
                new RoutineBlock("block-03-chia", "phase-03", "💧 12g Pre-soaked Chia Tonic", "12g soaked in 150ml water: hydrophilic mucilage gel coats gut & flattens glycemic spikes", "INGREDIENT", "08:45 AM"),
                new RoutineBlock("block-03-almonds", "phase-03", "🌰 15g Soaked & Peeled Almonds (12–14 Kernels)", "Peeled to remove enzyme-inhibiting skins, guarantees 90%+ mineral absorption, MUFAs & Vitamin E", "INGREDIENT", "08:50 AM"),
                new RoutineBlock("block-03-walnuts", "phase-03", "🧠 15g–20g Walnut Kernels (Shelled / 6–8 Halves)", "Edible meat without woody shell (keep thin brown skin on for polyphenols); ALA Omega-3", "INGREDIENT", "08:55 AM"),
                new RoutineBlock("block-03-banana", "phase-03", "🍌 100g Fresh Banana (1 medium)", "Potassium, Vitamin B6, and quick cellular glycogen for morning brain priming", "INGREDIENT", "09:05 AM"),
                new RoutineBlock("block-03-pb-roti", "phase-03", "🍞 40g Atta Roti + 25g–30g Natural PB (or Besan Chilla)", "Slow-burning complex carbohydrates & clean peanut monounsaturated fats supplying L-Tyrosine", "INGREDIENT", "09:15 AM"),
                new RoutineBlock("block-03-peanuts", "phase-03", "🥜 25g–30g Roasted Peanuts (Shelled / No Pods)", "High L-Tyrosine (dopamine precursor) & Resveratrol for 4h AI/ML math sprint", "INGREDIENT", "09:18 AM"),
                new RoutineBlock("block-03-chana", "phase-03", "🥜 35g–40g Roasted Chana", "Slow-burning complex carbs & zinc: flat mental stamina curve for 4h sprint", "INGREDIENT", "09:22 AM"),
                new RoutineBlock("block-03-moringa", "phase-03", "🌿 1.5g–3g (1/2–1 tsp) Organic India Moringa Powder", "Stirred into morning warm lemon water/breakfast; lemon ascorbic acid unlocks iron", "INGREDIENT", "09:25 AM"),
                new RoutineBlock("block-03-d3", "phase-03", "☀️ Dr. Excel D3 (60,000 IU Softgel - SUNDAYS ONLY)", "1 softgel weekly with breakfast fats (PB/nuts) for 80%+ micellar uptake; never daily", "INGREDIENT", "09:28 AM")
            )
        ),
        new PhaseTemplate(
            "phase-04",
            "Deep Work Block 1 (AI/ML & Core Math)",
            "4-hour uninterrupted flow state: Tensor mathematics, ML model pipelines, and deep debugging. Sip mineralized water.",
            "Peak cognitive executive function powered by morning tyrosine and dopamine reserves with zero insulin volatility.",
            240,
            Arrays.asList(
                new RoutineBlock("block-04-aiml-math", "phase-04", "💻 AI/ML Sprint 1: Deep Math & Algorithmic Focus", "4-hour uninterrupted flow state: Tensor mathematics, ML pipelines, deep debugging", "WORK", "09:30 AM – 01:30 PM"),
                new RoutineBlock("block-04-hydration", "phase-04", "💧 Mid-Sprint Mineralized Hydration", "Sip clean mineralized water throughout deep work to sustain cognitive stamina", "HYDRATION", "11:30 AM")
            )
        ),
        new PhaseTemplate(
            "phase-05",
            "Lunch: Anti-Coma Power Fuel",
            "50g-60g raw Soya Chunks (Double-boiled, water dumped, squeezed twice) + 150g-200g Sautéed Veggies in 5g ghee/mustard oil + 1 Roti (40g cap) + 10g-15g Lemon Juice.",
            "30g clean protein. Fresh Lemon Vitamin C converts non-heme iron to absorbable Fe2+ (+300% gain). Single roti cap prevents 2 PM food coma.",
            30,
            Arrays.asList(
                new RoutineBlock("block-05-soya", "phase-05", "🌱 50g–60g Soya Chunks (Double-Boiled & Squeezed Twice)", "Water dumped twice to eliminate flatulence oligosaccharides; supplies 30g pure clean protein", "INGREDIENT", "01:30 PM"),
                new RoutineBlock("block-05-veggies", "phase-05", "🥗 150g–200g Sautéed Veggies in 5g Ghee/Mustard Oil", "Quick-sautéed tender-crisp in 1 tsp fat: preserves polyphenols & optimizes fat-soluble vitamins", "INGREDIENT", "01:35 PM"),
                new RoutineBlock("block-05-roti", "phase-05", "🍞 40g Atta Roti (Single-Roti Glycemic Cap)", "Strict single-roti limit prevents insulin surge, tryptophan influx, and 2 PM food coma", "INGREDIENT", "01:40 PM"),
                new RoutineBlock("block-05-lemon", "phase-05", "🍋 10g–15g Fresh Lemon Juice Squeeze", "Ascorbic acid converts non-heme ferric iron to absorbable Fe2+ (+300% uptake)", "INGREDIENT", "01:45 PM")
            )
        ),
        new PhaseTemplate(
            "phase-06",
            "Post-Lunch 10-Minute Walk",
            "10-15 minutes brisk walking in corridor, room, or terrace. Avoid sitting down immediately.",
            "Translocates muscular GLUT4 glucose transporters, clearing post-meal blood sugar without demanding high insulin.",
            30,
            Arrays.asList(
                new RoutineBlock("block-06-glut4-walk", "phase-06", "🚶 GLUT4 Activation: 10-Minute Brisk Walk", "10-15 min walking immediately post-meal to dispose glucose directly into muscles without insulin spike", "EXERCISE", "02:00 – 02:30 PM")
            )
        ),
        new PhaseTemplate(
            "phase-07",
            "Deep Work Block 2 (Robotics & Systems)",
            "3-hour technical execution: ROS2 nodes, Isaac Sim, state estimation, and systems architecture.",
            "High cerebral blood flow maintained by post-lunch walk and stable glycemic curve.",
            180,
            Arrays.asList(
                new RoutineBlock("block-07-robotics", "phase-07", "🤖 Robotics Sprint 2: Kinematics, ROS2 & Isaac Sim", "3 hours technical execution: coordinate frames, state estimation, systems architecture", "WORK", "02:30 – 05:30 PM")
            )
        ),
        new PhaseTemplate(
            "phase-08",
            "Evening Fuel: Eggs, Pumpkin Seeds & Creatine",
            "WED-MON: 4 Whole Boiled Eggs (~200g) with rock salt & pepper + 15g Pumpkin Seeds + 10g Lemon + 3-5g Tata 1mg Creatine in water. (TUESDAY: Replace 4 eggs with 100g Fresh Paneer/Tofu).",
            "560mg Choline for acetylcholine synthesis + Evening Magnesium & Tryptophan for sleep + Creatine for cerebral ATP.",
            60,
            Arrays.asList(
                new RoutineBlock("block-08-eggs-paneer", "phase-08", "🥚 4 Whole Boiled Eggs (~200g Peeled) (Tue: 100g Fresh Paneer/Tofu)", "560mg Choline for acetylcholine, lutein, zeaxanthin, DHA & 24g complete protein", "INGREDIENT", "05:30 PM"),
                new RoutineBlock("block-08-pumpkin", "phase-08", "🎃 15g Raw Pumpkin Seeds (Shelled Pepitas)", "15g green hulled seeds (no white shell): ~90mg elemental Magnesium + Tryptophan for GABA calming", "INGREDIENT", "05:45 PM"),
                new RoutineBlock("block-08-lemon", "phase-08", "🍋 10g Fresh Lemon Juice Squeeze", "Ascorbic acid catalyst optimizing mineral absorption and flavor on boiled eggs", "INGREDIENT", "05:48 PM"),
                new RoutineBlock("block-08-creatine", "phase-08", "⚡ 3g–5g Tata 1mg Micronized Creatine in Water", "Regenerates cerebral ATP; paired with egg protein for insulin-stimulated CreaT uptake", "INGREDIENT", "05:50 PM")
            )
        ),
        new PhaseTemplate(
            "phase-09",
            "Deep Work Block 3 / Review / Workout",
            "1-hour focused execution: Engineering pull requests, research papers, hardware calibration, or physical training.",
            "Sustained cognitive stamina powered by newly digested acetylcholine precursors and cellular creatine.",
            60,
            Arrays.asList(
                new RoutineBlock("block-09-core-review", "phase-09", "⚙️ Core Sprint 3: Code Review, Hardware Calibration or Posture", "1 hour focused execution powered by freshly synthesized acetylcholine & cellular creatine", "WORK", "06:30 – 07:30 PM")
            )
        ),
        new PhaseTemplate(
            "phase-10",
            "1-Hour English Practice & Active Fluency",
            "Active verbal drills: Shadowing native audio, pronunciation/phonetics refinement, technical presentation speaking, and active vocabulary retrieval.",
            "Stimulates Broca's and Wernicke's speech-motor pathways; shifts brain from silent mathematical logic to expressive verbal fluidity and cross-hemispheric communication.",
            60,
            Arrays.asList(
                new RoutineBlock("block-10-english", "phase-10", "🗣️ 1-Hour English Practice & Active Fluency", "Vocal drills, shadowing native audio, pronunciation refinement, presentation delivery", "WORK", "07:30 – 08:30 PM")
            )
        ),
        new PhaseTemplate(
            "phase-11",
            "Dinner: Light Khichdi & Curd",
            "Warm Moong Dal Khichdi (~200g cooked from 40g dal + 20g rice) + 100g-120g Fresh Curd/Dahi. MUST FINISH BEFORE BED.",
            "Supplies dietary tryptophan and probiotics. Curd calcium consumed here without blocking daytime plant iron.",
            30,
            Arrays.asList(
                new RoutineBlock("block-11-khichdi", "phase-11", "🍲 200g Warm Moong Dal Khichdi (40g Dal + 20g Rice dry)", "Light, easily digestible tryptophan source; MUST finish before 21:00 to guarantee fasting gap", "INGREDIENT", "08:30 PM"),
                new RoutineBlock("block-11-curd", "phase-11", "🥛 100g–120g Fresh Curd / Dahi", "Probiotics & bioavailable calcium; consumed ONLY at dinner so calcium doesn't block daytime iron", "INGREDIENT", "08:45 PM")
            )
        ),
        new PhaseTemplate(
            "phase-12",
            "Fasting Gap Before Bed",
            "Displays switched to warm/amber <=15% brightness. Zero calorie intake. Thermal dump walk or reading.",
            "Stomach empties completely. Prevents late-night acid and melatonin suppression.",
            150,
            Arrays.asList(
                new RoutineBlock("block-12-fasting", "phase-12", "⏳ 3-Hour Night Fasting Gap (Zero Calorie Intake)", "Zero food intake between 21:00 and 00:00: ensures empty GI tract for slow-wave sleep", "ROUTINE", "09:00 – 11:30 PM"),
                new RoutineBlock("block-12-amber-display", "phase-12", "🌙 Amber Display (2000K, <= 15% Brightness)", "Eliminates 460nm blue photons to protect pineal gland melatonin production", "ROUTINE", "09:30 PM")
            )
        ),
        new PhaseTemplate(
            "phase-13",
            "Night Wind-Down & Hydration",
            "Sip 200ml room-temperature water. Whisk 1/2 tsp starter into lukewarm milk for next-day dahi. Dark environment, cool room temperature.",
            "Lowers core body temperature, signaling hypothalamic sleep center, and preps fresh probiotic curd for tomorrow.",
            30,
            Arrays.asList(
                new RoutineBlock("block-13-night-water", "phase-13", "💧 Night Hydration: 200ml Room-Temp Water", "Prevents nocturnal blood viscosity and dehydration without waking for urination", "HYDRATION", "11:30 PM"),
                new RoutineBlock("block-13-dahi-prep", "phase-13", "🥛 Next-Day Fresh Dahi Prep (Set Curd)", "Whisk starter into lukewarm milk; 7-8h overnight lactic fermentation for tomorrow's dinner", "ROUTINE", "11:40 PM"),
                new RoutineBlock("block-13-sleep-prep", "phase-13", "🛏️ Dark & Cool Bedroom Preparation", "Cool room environment, pitch dark space: lowers core body temp for hypothalamic sleep trigger", "ROUTINE", "11:45 PM")
            )
        )
    };

    public static final List<Phase> PHASES = new ArrayList<Phase>();
    private static boolean initialized = false;

    static {
        recalculatePhases(8, 0, 0, 0);
    }

    public static void ensureInitialized(Context context) {
        if (!initialized && context != null) {
            init(context);
        }
    }

    public static synchronized void init(Context context) {
        if (context != null) {
            int wakeH = getWakeHour(context);
            int wakeM = getWakeMinute(context);
            int sleepH = getSleepHour(context);
            int sleepM = getSleepMinute(context);
            recalculatePhases(wakeH, wakeM, sleepH, sleepM);
        } else {
            recalculatePhases(8, 0, 0, 0);
        }
        initialized = true;
    }

    public static int getWakeHour(Context context) {
        if (context == null) return 8;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_WAKE_HOUR, 8);
    }

    public static int getWakeMinute(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_WAKE_MIN, 0);
    }

    public static int getSleepHour(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_SLEEP_HOUR, 0);
    }

    public static int getSleepMinute(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_SLEEP_MIN, 0);
    }

    public static void setScheduleTimings(Context context, int wakeH, int wakeM, int sleepH, int sleepM) {
        if (context != null) {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.putInt(KEY_WAKE_HOUR, wakeH);
            editor.putInt(KEY_WAKE_MIN, wakeM);
            editor.putInt(KEY_SLEEP_HOUR, sleepH);
            editor.putInt(KEY_SLEEP_MIN, sleepM);
            editor.apply();
        }
        recalculatePhases(wakeH, wakeM, sleepH, sleepM);
        if (context != null && AppEngineManager.isAppEnabled(context)) {
            scheduleAllAlarms(context);
        }
    }

    public static synchronized void recalculatePhases(int wakeH, int wakeM, int sleepH, int sleepM) {
        PHASES.clear();

        int wakeTotalMin = ((wakeH * 60 + wakeM) % 1440 + 1440) % 1440;
        int sleepTotalMin = ((sleepH * 60 + sleepM) % 1440 + 1440) % 1440;

        int wakingMinutes = (sleepTotalMin - wakeTotalMin + 1440) % 1440;
        // Enforce healthy circadian bounds: at least 6 hours awake (360 min) and at most 21 hours awake (1260 min)
        if (wakingMinutes < 360 || wakingMinutes > 1260) {
            wakingMinutes = 960; // 16h healthy circadian default
            sleepTotalMin = (wakeTotalMin + wakingMinutes) % 1440;
            sleepH = sleepTotalMin / 60;
            sleepM = sleepTotalMin % 60;
        }

        int[] durations = new int[WAKING_TEMPLATES.length];
        int sum = 0;

        for (int i = 0; i < WAKING_TEMPLATES.length; i++) {
            if (wakingMinutes == 960) {
                durations[i] = WAKING_TEMPLATES[i].baseDuration;
            } else {
                float scaled = WAKING_TEMPLATES[i].baseDuration * ((float) wakingMinutes / 960.0f);
                durations[i] = Math.max(5, Math.round(scaled / 5.0f) * 5);
            }
            sum += durations[i];
        }

        // Absorb rounding remainder into primary Deep Work Block 1 (index 3)
        // while preserving exact sum(durations) == wakingMinutes to eliminate any phase overlap
        int diff = wakingMinutes - sum;
        if (durations[3] + diff >= 15) {
            durations[3] += diff;
        } else {
            durations[3] = 15;
            int remainingDiff = wakingMinutes - (sum - durations[3] + 15);
            for (int i = 0; i < durations.length && remainingDiff != 0; i++) {
                if (i != 3 && durations[i] > 10) {
                    int adj = Math.min(durations[i] - 10, -remainingDiff);
                    durations[i] -= adj;
                    remainingDiff += adj;
                }
            }
        }

        int currentMin = wakeTotalMin;
        for (int i = 0; i < WAKING_TEMPLATES.length; i++) {
            int startMin = (currentMin % 1440 + 1440) % 1440;
            int endMin = ((currentMin + durations[i]) % 1440 + 1440) % 1440;
            currentMin += durations[i];

            int sH = startMin / 60;
            int sM = startMin % 60;
            int eH = endMin / 60;
            int eM = endMin % 60;
            String timeStr = Phase.formatTime12(sH, sM) + " – " + Phase.formatTime12(eH, eM);

            PhaseTemplate t = WAKING_TEMPLATES[i];
            PHASES.add(new Phase(t.id, sH, sM, eH, eM, timeStr, t.title, t.protocol, t.why, t.defaultBlocks));
        }

        // Phase 14: Deep Sleep (from sleepTotalMin to wakeTotalMin) - Explicit Sleep Block
        int sleepDurationMin = (wakeTotalMin - sleepTotalMin + 1440) % 1440;
        float sleepHours = Math.round((sleepDurationMin / 60.0f) * 10.0f) / 10.0f;
        String sleepHourStr = (sleepHours == (int) sleepHours) ? String.valueOf((int) sleepHours) : String.format("%.1f", sleepHours);
        String sleepTimeStr = Phase.formatTime12(sleepH, sleepM) + " – " + Phase.formatTime12(wakeH, wakeM);
        String sleepTitle = "Deep Restorative Sleep (" + sleepHourStr + " Hours)";
        String sleepProtocol = sleepHourStr + " hours deep restorative sleep in complete darkness and cool air (" + sleepTimeStr + ").";
        String sleepWhy = "Zero digestion allows the Glymphatic System to expand and flush cerebral metabolic waste.";

        List<RoutineBlock> sleepBlocks = new ArrayList<RoutineBlock>();
        sleepBlocks.add(new RoutineBlock("block-14-deep-sleep", "phase-14", "😴 Sleep Block: " + sleepHourStr + " Hours Deep Restorative Sleep", sleepProtocol, "SLEEP", sleepTimeStr));

        PHASES.add(new Phase("phase-14", sleepH, sleepM, wakeH, wakeM, sleepTimeStr, sleepTitle, sleepProtocol, sleepWhy, sleepBlocks));
    }

    public static List<RoutineBlock> getAllGranularBlocks() {
        List<RoutineBlock> all = new ArrayList<RoutineBlock>();
        for (Phase p : PHASES) {
            if (p != null && p.granularBlocks != null) {
                all.addAll(p.granularBlocks);
            }
        }
        return all;
    }

    public static Phase getCurrentPhase() {
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (Phase p : PHASES) {
            int startMin = p.startHour * 60 + p.startMinute;
            int endMin = p.endHour * 60 + p.endMinute;

            if (startMin > endMin) { // Crosses midnight
                if (nowMinutes >= startMin || nowMinutes < endMin) {
                    return p;
                }
            } else {
                if (nowMinutes >= startMin && nowMinutes < endMin) {
                    return p;
                }
            }
        }
        return null;
    }

    public static Phase getNextPhase() {
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        Phase bestNext = null;
        int minDiff = Integer.MAX_VALUE;

        for (Phase p : PHASES) {
            int startMin = p.startHour * 60 + p.startMinute;
            int diff = startMin - nowMinutes;
            if (diff <= 0) diff += 1440; // Next day
            if (diff < minDiff) {
                minDiff = diff;
                bestNext = p;
            }
        }
        return bestNext;
    }

    public static void scheduleAllAlarms(Context context) {
        ensureInitialized(context);
        if (!AppEngineManager.isAppEnabled(context)) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar now = Calendar.getInstance();

        for (int i = 0; i < PHASES.size(); i++) {
            Phase p = PHASES.get(i);
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, p.startHour);
            target.set(Calendar.MINUTE, p.startMinute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.setAction("com.devendra.healthtracker.ALARM_TRIGGER");
            intent.putExtra("phase_id", p.id);
            intent.putExtra("title", p.getDisplayTitle());
            intent.putExtra("roman", p.romanWindow);
            intent.putExtra("time", p.getFormattedTime12());
            intent.putExtra("protocol", p.getDisplayProtocol());
            intent.putExtra("request_code", 1000 + i);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getBroadcast(context, 1000 + i, intent, flags);

            boolean canExact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canExact = alarmManager.canScheduleExactAlarms();
            }

            try {
                if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                }
            } catch (SecurityException se) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                }
            }
        }
    }

    public static void cancelAllAlarms(Context context) {
        ensureInitialized(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < PHASES.size(); i++) {
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.setAction("com.devendra.healthtracker.ALARM_TRIGGER");
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, 1000 + i, intent, flags);
            try {
                alarmManager.cancel(pi);
                pi.cancel();
            } catch (Exception ignored) {}
        }
    }
}
