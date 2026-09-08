package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.SettingSection;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Wall-clock time, and optionally the in-game time. */
public final class ClockWidget extends LabeledHudWidget {

    /** Clock face. */
    public enum Format {
        HOURS_24("24 Hour"),
        HOURS_12("12 Hour");

        private final String display;

        Format(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    private static final DateTimeFormatter TWENTY_FOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TWENTY_FOUR_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TWELVE = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter TWELVE_SECONDS = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public final EnumSetting<Format> format;
    public final BooleanSetting seconds;
    public final BooleanSetting gameTime;

    public ClockWidget() {
        super("clock", "Clock", HudAnchor.TOP_RIGHT, -4.0F, 32.0F, false);

        this.format = add(new EnumSetting<>("hud.clock.format", "Format", "Clock face", Format.HOURS_24));
        this.format.section(SettingSection.APPEARANCE);

        this.seconds = add(new BooleanSetting("hud.clock.seconds", "Seconds", "Include seconds", false));
        this.seconds.section(SettingSection.APPEARANCE);

        this.gameTime = add(new BooleanSetting("hud.clock.gameTime", "In-Game Day",
                "Also show the current in-game day", false));
        this.gameTime.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        DateTimeFormatter formatter = this.format.get() == Format.HOURS_24
                ? (this.seconds.get() ? TWENTY_FOUR_SECONDS : TWENTY_FOUR)
                : (this.seconds.get() ? TWELVE_SECONDS : TWELVE);
        row("Time", LocalTime.now().format(formatter));

        if (this.gameTime.get() && client().world != null) {
            long day = client().world.getTimeOfDay() / 24_000L;
            row("Day", String.valueOf(day));
        }
    }
}
