/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.platform;

import cn.harryh.arkpets.Const;
import cn.harryh.arkpets.utils.Logger;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** Exports the desktop's cursor theme and size so that GLFW can honour them.
 * <p>
 * GLFW's Wayland backend resolves the cursor purely from the {@code XCURSOR_THEME} and
 * {@code XCURSOR_SIZE} environment variables and silently falls back to the built-in
 * default theme at 16px when they are missing (see {@code loadCursorTheme()} in GLFW's
 * {@code wl_init.c}). KDE, however, keeps those settings in its own configuration and
 * does not export the variables to child processes, and its XDG settings portal does not
 * implement the {@code cursor-theme}/{@code cursor-size} keys either. The cursor inside a
 * pet window therefore reverts to a small default cursor instead of the one the user
 * configured.
 * <p>
 * The values are read from KDE's own configuration first and from the copies that GTK
 * keeps (written by {@code kde-gtk-config}) as a fallback. A theme is only exported when
 * its directory can actually be found, because GLFW fails to initialise if the cursor
 * theme cannot be loaded.
 */
public class CursorEnvironment {
    private static final String ENV_THEME = "XCURSOR_THEME";
    private static final String ENV_SIZE  = "XCURSOR_SIZE";

    /** The libc entry point used to modify this process's environment. Java's own
     * environment map is a snapshot taken at startup and cannot be written to, but GLFW
     * reads the native environment, which setenv() does update.
     */
    private interface LibC extends Library {
        int setenv(String name, String value, int overwrite);
    }

    /** Makes GLFW load the cursor the user configured, if it is not already exported.
     * Must be called before GLFW is initialised.
     */
    public static void apply() {
        if (!Const.isLinux)
            return;
        try {
            String theme = System.getenv(ENV_THEME);
            String size = System.getenv(ENV_SIZE);
            if (theme == null)
                theme = resolveTheme();
            if (size == null)
                size = resolveSize();
            if (theme == null && size == null)
                return;
            LibC libc = Native.load("c", LibC.class);
            if (theme != null)
                libc.setenv(ENV_THEME, theme, 1);
            if (size != null)
                libc.setenv(ENV_SIZE, size, 1);
            Logger.info("System", "Cursor environment set to " + theme + " @ " + size);
        } catch (Throwable t) {
            // Never let a cosmetic problem prevent the application from starting.
            Logger.warn("System", "Unable to export the cursor environment: " + t.getMessage());
        }
    }

    private static String resolveTheme() {
        String theme = readIni(configFile("kcminputrc"), "Mouse", "cursorTheme");
        if (theme == null)
            theme = readIni(configFile("gtk-3.0/settings.ini"), "Settings", "gtk-cursor-theme-name");
        if (theme == null)
            theme = readIni(configFile("gtk-4.0/settings.ini"), "Settings", "gtk-cursor-theme-name");
        if (theme == null)
            theme = readIni(homeFile(".gtkrc-2.0"), null, "gtk-cursor-theme-name");
        if (theme == null)
            return null;
        if (!themeExists(theme)) {
            Logger.debug("System", "Configured cursor theme \"" + theme + "\" was not found, leaving it unset.");
            return null;
        }
        return theme;
    }

    private static String resolveSize() {
        String size = readIni(configFile("kcminputrc"), "Mouse", "cursorSize");
        if (size == null)
            size = readIni(configFile("gtk-3.0/settings.ini"), "Settings", "gtk-cursor-theme-size");
        if (size == null)
            size = readIni(configFile("gtk-4.0/settings.ini"), "Settings", "gtk-cursor-theme-size");
        if (size == null)
            size = readIni(homeFile(".gtkrc-2.0"), null, "gtk-cursor-theme-size");
        if (size == null)
            return null;
        try {
            return Integer.parseInt(size.trim()) > 0 ? size.trim() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Checks whether a cursor theme can be resolved, the same way libwayland-cursor
     * searches for it.
     */
    private static boolean themeExists(String theme) {
        if (theme.isEmpty() || theme.equals("default"))
            return true;
        for (Path base : iconBaseDirs())
            if (Files.isDirectory(base.resolve(theme).resolve("cursors")))
                return true;
        return false;
    }

    private static List<Path> iconBaseDirs() {
        String home = System.getProperty("user.home");
        String dataHome = System.getenv("XDG_DATA_HOME");
        List<Path> dirs = new java.util.ArrayList<>();
        dirs.add(Path.of(dataHome != null && !dataHome.isEmpty() ? dataHome : home + "/.local/share", "icons"));
        dirs.add(Path.of(home, ".icons"));
        String dataDirs = System.getenv("XDG_DATA_DIRS");
        for (String dir : (dataDirs != null && !dataDirs.isEmpty() ? dataDirs : "/usr/local/share:/usr/share").split(":"))
            if (!dir.isEmpty())
                dirs.add(Path.of(dir, "icons"));
        return dirs;
    }

    private static Path configFile(String relative) {
        String configHome = System.getenv("XDG_CONFIG_HOME");
        Path base = configHome != null && !configHome.isEmpty()
                ? Path.of(configHome)
                : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve(relative);
    }

    private static Path homeFile(String relative) {
        return Path.of(System.getProperty("user.home"), relative);
    }

    /** Reads one key from a simple INI file, optionally scoped to a section. Surrounding
     * quotes are stripped, which the GTK 2 configuration file uses.
     */
    private static String readIni(Path file, String section, String key) {
        if (!Files.isReadable(file))
            return null;
        try {
            String currentSection = null;
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
                    continue;
                if (line.startsWith("[")) {
                    int end = line.indexOf(']');
                    currentSection = (end > 0 ? line.substring(1, end) : line.substring(1)).trim();
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator < 0)
                    continue;
                if (!line.substring(0, separator).trim().equalsIgnoreCase(key))
                    continue;
                if (section != null && !section.equalsIgnoreCase(currentSection))
                    continue;
                String value = line.substring(separator + 1).trim();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))))
                    value = value.substring(1, value.length() - 1).trim();
                return value.isEmpty() ? null : value;
            }
        } catch (IOException e) {
            Logger.debug("System", "Unable to read " + file + ": " + e.getMessage());
        }
        return null;
    }
}
