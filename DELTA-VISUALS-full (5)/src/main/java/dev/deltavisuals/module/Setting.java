package dev.deltavisuals.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.deltavisuals.config.ConfigManager;

/** Base class for every module option. Subclasses are nested for convenience. */
public abstract class Setting {
    public final String name;
    /** Hidden settings are saved to configs but not shown in the GUI. */
    public boolean hidden;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);

    public abstract void reset();

    protected void changed() {
        ConfigManager.markDirty();
    }

    public Setting hide() {
        this.hidden = true;
        return this;
    }

    // ------------------------------------------------------------------

    public static final class Bool extends Setting {
        private final boolean def;
        private boolean value;

        public Bool(String name, boolean def) {
            super(name);
            this.def = def;
            this.value = def;
        }

        public boolean get() { return value; }

        public void set(boolean v) {
            if (v != value) {
                value = v;
                changed();
            }
        }

        public void toggle() { set(!value); }

        @Override public JsonElement toJson() { return new JsonPrimitive(value); }

        @Override public void fromJson(JsonElement e) {
            try {
                if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) value = e.getAsBoolean();
                else value = def;
            } catch (Exception ex) {
                value = def;
            }
        }

        @Override public void reset() { value = def; }
    }

    public static final class Num extends Setting {
        public final double min, max, step;
        private final double def;
        private double value;

        public Num(String name, double def, double min, double max, double step) {
            super(name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = clean(def);
            this.value = this.def;
        }

        private double clean(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) return def;
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round(v / step) * step;
            v = Math.max(min, Math.min(max, v));
            return Math.round(v * 1000.0) / 1000.0;
        }

        public double get() { return value; }
        public int getInt() { return (int) Math.round(value); }
        public float getFloat() { return (float) value; }

        public void set(double v) {
            v = clean(v);
            if (v != value) {
                value = v;
                changed();
            }
        }

        @Override public JsonElement toJson() { return new JsonPrimitive(value); }

        @Override public void fromJson(JsonElement e) {
            try {
                if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) value = clean(e.getAsDouble());
                else value = def;
            } catch (Exception ex) {
                value = def;
            }
        }

        @Override public void reset() { value = def; }
    }

    public static final class Mode extends Setting {
        public final String[] options;
        private final int def;
        private int index;

        public Mode(String name, int def, String... options) {
            super(name);
            this.options = options;
            this.def = Math.max(0, Math.min(options.length - 1, def));
            this.index = this.def;
        }

        public String get() { return options[index]; }
        public int index() { return index; }

        public void set(int i) {
            i = Math.max(0, Math.min(options.length - 1, i));
            if (i != index) {
                index = i;
                changed();
            }
        }

        @Override public JsonElement toJson() { return new JsonPrimitive(options[index]); }

        @Override public void fromJson(JsonElement e) {
            index = def;
            try {
                if (e != null && e.isJsonPrimitive()) {
                    String s = e.getAsString();
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equalsIgnoreCase(s)) {
                            index = i;
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
                index = def;
            }
        }

        @Override public void reset() { index = def; }
    }

    public static final class Color extends Setting {
        private final int def;
        private int rgb;

        public Color(String name, int def) {
            super(name);
            this.def = def & 0xFFFFFF;
            this.rgb = this.def;
        }

        public int get() { return rgb; }

        public void set(int v) {
            v &= 0xFFFFFF;
            if (v != rgb) {
                rgb = v;
                changed();
            }
        }

        @Override public JsonElement toJson() { return new JsonPrimitive(String.format("#%06X", rgb)); }

        @Override public void fromJson(JsonElement e) {
            rgb = def;
            try {
                if (e != null && e.isJsonPrimitive()) {
                    String s = e.getAsString().trim();
                    if (s.startsWith("#")) s = s.substring(1);
                    if (s.length() == 6) rgb = Integer.parseInt(s, 16) & 0xFFFFFF;
                }
            } catch (Exception ignored) {
                rgb = def;
            }
        }

        @Override public void reset() { rgb = def; }
    }
}
