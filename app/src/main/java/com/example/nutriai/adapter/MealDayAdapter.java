package com.example.nutriai.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutriai.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MealDayAdapter extends RecyclerView.Adapter<MealDayAdapter.ViewHolder> {

    public static class MealEntry {
        public final String type;
        public final String name;
        public final String calories;
        public final List<String> ingredients;

        public MealEntry(String type, String name, String calories, List<String> ingredients) {
            this.type = type;
            this.name = name;
            this.calories = calories;
            this.ingredients = ingredients;
        }
    }

    public static class DayEntry {
        public final String header;
        public final List<MealEntry> meals;
        public final boolean[] mealExpanded = {false, false, false, false};

        public DayEntry(String header, List<MealEntry> meals) {
            this.header = header;
            this.meals = meals;
        }
    }

    private static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Dinner", "Snack"};

    // Matches "Day 1", "Day 1:", "**Day 1**", "**Day 1:**" — colon is NOT required
    private static final Pattern DAY_HEADER = Pattern.compile(
            "(?i)\\*{0,2}(Day\\s+\\d+)\\*{0,2}\\s*[:\\-–—]?");

    // Matches the start of any day section — used for splitting
    private static final Pattern DAY_SPLIT = Pattern.compile(
            "(?i)(?=\\*{0,2}Day\\s+\\d+\\b)");

    // Matches "Day 1" in any format — used as a content anchor
    private static final Pattern DAY1_ANCHOR = Pattern.compile(
            "(?i)\\*{0,2}Day\\s+1\\b");

    private static final Pattern CAL_PATTERN = Pattern.compile(
            "\\((\\d+)\\s*(?:kcal|cal)\\)");

    private final List<DayEntry> days = new ArrayList<>();

    public void setMealPlanText(String text) {
        days.clear();
        if (text == null || text.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Strip Llama 3 control tokens (e.g. <|eot_id|>, <|start_header_id|>assistant<|end_header_id|>)
        text = text.replaceAll("<\\|[a-zA-Z0-9_]+\\|>", "").trim();

        // Strip [MEAL_PLAN] marker
        text = text.replaceFirst("(?i)\\[MEAL_PLAN\\]\\s*", "").trim();

        // Cut off shopping-list / ingredients section if present
        for (String marker : new String[]{"[INGREDIENTS]", "*INGREDIENTS*", "\nIngredients:", "\nShopping"}) {
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                text = text.substring(0, idx);
                break;
            }
        }

        // Anchor to "Day 1" in any format to skip any preamble the model may emit
        Matcher anchor = DAY1_ANCHOR.matcher(text);
        String content = anchor.find() ? text.substring(anchor.start()) : text;
        content = content.trim();

        if (content.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Split into per-day sections — colon after day number is NOT required
        String[] sections = DAY_SPLIT.split(content);
        for (String section : sections) {
            DayEntry entry = parseDay(section.trim());
            if (entry != null && !entry.meals.isEmpty()) days.add(entry);
        }
        notifyDataSetChanged();
    }

    private DayEntry parseDay(String section) {
        if (section.isEmpty()) return null;

        Matcher m = DAY_HEADER.matcher(section);
        String header;
        String body;
        if (m.find()) {
            // Group 1 captures just "Day N" without any markdown or punctuation
            header = m.group(1).trim();
            body = section.substring(m.end());
        } else {
            int nl = section.indexOf('\n');
            // Strip markdown asterisks from the raw header text
            header = (nl != -1 ? section.substring(0, nl) : section)
                    .replaceAll("\\*", "").trim();
            body = nl != -1 ? section.substring(nl + 1) : "";
        }

        // Normalise all meal-type prefixes to exact-case "Type:" with no leading markup:
        //   "**Breakfast:**" → "Breakfast:"
        //   "- lunch:"       → "\nLunch:"
        //   "breakfast:"     → "Breakfast:"
        for (String type : MEAL_TYPES) {
            // Strip markdown bold around the type label
            body = body.replaceAll("(?i)\\*{1,2}" + type + "\\*{1,2}\\s*:", type + ":");
            // Normalise dash/bullet prefixes
            body = body.replaceAll("(?i)\\s*[-–—•]+\\s*" + type + "\\s*:", "\n" + type + ":");
            // Normalise any remaining lowercase variants
            body = body.replaceAll("(?i)(?<![a-zA-Z])" + type + ":", type + ":");
        }

        List<MealEntry> meals = new ArrayList<>();
        for (String type : MEAL_TYPES) {
            MealEntry entry = parseMealEntry(body, type);
            if (entry != null) meals.add(entry);
        }
        return new DayEntry(header, meals);
    }

    private MealEntry parseMealEntry(String body, String type) {
        int idx = body.indexOf(type + ":");
        if (idx == -1) return null;
        int start = idx + type.length() + 1;

        // Find where this meal entry ends
        int end = body.length();
        for (String other : MEAL_TYPES) {
            if (other.equals(type)) continue;
            int next = body.indexOf("\n" + other + ":", start);
            if (next != -1 && next < end) end = next;
        }
        int nextDay = body.indexOf("\nDay ", start);
        if (nextDay != -1 && nextDay < end) end = nextDay;
        for (String marker : new String[]{"[INGREDIENTS]", "*INGREDIENTS*", "[READY]", "\nIngredients:"}) {
            int mi = body.indexOf(marker, start);
            if (mi != -1 && mi < end) end = mi;
        }

        // Take only the first content line — prevents Snack from swallowing trailing text
        String raw = body.substring(start, end).trim();
        int nl = raw.indexOf('\n');
        if (nl != -1) raw = raw.substring(0, nl).trim();

        // Extract calorie count before stripping "(X kcal)" from the string
        String calories = "";
        Matcher calM = CAL_PATTERN.matcher(raw);
        if (calM.find()) {
            calories = calM.group(1) + " kcal";
        }
        raw = raw.replaceAll("\\(\\d+[^)]*\\)", "").replaceAll("\\s+", " ").trim();
        if (raw.isEmpty()) return null;

        // Parse ingredients: "[ingr1, ingr2]" bracket format (new), "| ingr1, ingr2" pipe (legacy)
        String mealName;
        List<String> ingredients;

        int bracket = raw.indexOf('[');
        int pipe    = raw.indexOf('|');

        if (bracket != -1) {
            mealName = raw.substring(0, bracket).trim();
            String ingrRaw = raw.substring(bracket + 1);
            int close = ingrRaw.indexOf(']');
            if (close != -1) ingrRaw = ingrRaw.substring(0, close);
            ingredients = splitByComma(ingrRaw);
        } else if (pipe != -1) {
            mealName    = raw.substring(0, pipe).trim();
            ingredients = splitByComma(raw.substring(pipe + 1));
        } else {
            mealName    = raw;
            ingredients = inferFromName(raw);
        }

        if (mealName.isEmpty()) return null;
        return new MealEntry(type, mealName, calories, ingredients);
    }

    private static List<String> splitByComma(String raw) {
        String[] parts = raw.split(",");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            p = p.trim().replaceAll("\\(.*?\\)", "").trim();
            if (p.length() > 1) {
                result.add(Character.toUpperCase(p.charAt(0)) + p.substring(1));
            }
        }
        return result;
    }

    private static List<String> inferFromName(String mealName) {
        String[] parts = mealName.split("\\s+with\\s+|\\s+and\\s+|\\s*,\\s*|\\s+on\\s+");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (p.length() > 2) {
                result.add(Character.toUpperCase(p.charAt(0)) + p.substring(1));
            }
        }
        return result;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayHeader;
        final LinearLayout[] rows        = new LinearLayout[4];
        final TextView[]     tvMealNames = new TextView[4];
        final TextView[]     tvCalories  = new TextView[4];
        final LinearLayout[] containers  = new LinearLayout[4];
        final ImageView[]    chevrons    = new ImageView[4];

        ViewHolder(View v) {
            super(v);
            tvDayHeader    = v.findViewById(R.id.tvDayHeader);
            rows[0]        = v.findViewById(R.id.rowBreakfast);
            rows[1]        = v.findViewById(R.id.rowLunch);
            rows[2]        = v.findViewById(R.id.rowDinner);
            rows[3]        = v.findViewById(R.id.rowSnack);
            tvMealNames[0] = v.findViewById(R.id.tvBreakfastName);
            tvMealNames[1] = v.findViewById(R.id.tvLunchName);
            tvMealNames[2] = v.findViewById(R.id.tvDinnerName);
            tvMealNames[3] = v.findViewById(R.id.tvSnackName);
            tvCalories[0]  = v.findViewById(R.id.tvCalBreakfast);
            tvCalories[1]  = v.findViewById(R.id.tvCalLunch);
            tvCalories[2]  = v.findViewById(R.id.tvCalDinner);
            tvCalories[3]  = v.findViewById(R.id.tvCalSnack);
            containers[0]  = v.findViewById(R.id.ingredientsBreakfast);
            containers[1]  = v.findViewById(R.id.ingredientsLunch);
            containers[2]  = v.findViewById(R.id.ingredientsDinner);
            containers[3]  = v.findViewById(R.id.ingredientsSnack);
            chevrons[0]    = v.findViewById(R.id.chevronBreakfast);
            chevrons[1]    = v.findViewById(R.id.chevronLunch);
            chevrons[2]    = v.findViewById(R.id.chevronDinner);
            chevrons[3]    = v.findViewById(R.id.chevronSnack);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        DayEntry day = days.get(position);
        h.tvDayHeader.setText(day.header);

        // Hide all rows first — show only the ones that were parsed successfully
        for (int i = 0; i < 4; i++) h.rows[i].setVisibility(View.GONE);

        // Map each meal to the correct row by TYPE, not by list position
        for (MealEntry meal : day.meals) {
            int i = typeIndex(meal.type);
            if (i < 0) continue;

            h.tvMealNames[i].setText(meal.name);
            if (meal.calories != null && !meal.calories.isEmpty()) {
                h.tvCalories[i].setText(meal.calories);
                h.tvCalories[i].setVisibility(View.VISIBLE);
            } else {
                h.tvCalories[i].setVisibility(View.GONE);
            }
            h.rows[i].setVisibility(View.VISIBLE);

            boolean exp = day.mealExpanded[i];
            h.containers[i].setVisibility(exp ? View.VISIBLE : View.GONE);
            h.chevrons[i].setRotation(exp ? 180f : 0f);

            populateIngredients(h.containers[i], meal.ingredients);

            final int idx = i;
            h.rows[i].setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                days.get(pos).mealExpanded[idx] = !days.get(pos).mealExpanded[idx];
                notifyItemChanged(pos);
            });
        }
    }

    private static int typeIndex(String type) {
        switch (type) {
            case "Breakfast": return 0;
            case "Lunch":     return 1;
            case "Dinner":    return 2;
            case "Snack":     return 3;
            default:          return -1;
        }
    }

    private void populateIngredients(LinearLayout container, List<String> items) {
        container.removeAllViews();
        Context ctx = container.getContext();
        float dp = ctx.getResources().getDisplayMetrics().density;

        for (String item : items) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (4 * dp), 0, (int) (4 * dp));

            View dot = new View(ctx);
            int dotSize = (int) (6 * dp);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMarginEnd((int) (10 * dp));
            dot.setLayoutParams(dotParams);
            dot.setBackground(ContextCompat.getDrawable(ctx, R.drawable.bg_day_badge));

            TextView tv = new TextView(ctx);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setText(item);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextSecondary));

            row.addView(dot);
            row.addView(tv);
            container.addView(row);
        }
    }

    @Override
    public int getItemCount() { return days.size(); }

    /**
     * Returns a deduplicated ingredient map across all 7 days.
     * Key = canonical ingredient name (first-seen casing), Value = number of meals it appears in.
     */
    public java.util.LinkedHashMap<String, Integer> getConsolidatedIngredients() {
        java.util.LinkedHashMap<String, Integer> result = new java.util.LinkedHashMap<>();
        for (DayEntry day : days) {
            for (MealEntry meal : day.meals) {
                for (String ing : meal.ingredients) {
                    String key = ing.trim();
                    if (key.isEmpty()) continue;
                    String existingKey = null;
                    for (String k : result.keySet()) {
                        if (k.equalsIgnoreCase(key)) { existingKey = k; break; }
                    }
                    if (existingKey != null) {
                        result.put(existingKey, result.get(existingKey) + 1);
                    } else {
                        result.put(key, 1);
                    }
                }
            }
        }
        return result;
    }
}
