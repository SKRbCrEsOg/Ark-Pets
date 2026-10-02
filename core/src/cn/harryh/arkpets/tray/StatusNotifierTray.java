/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.natives.StatusNotifierItem;
import cn.harryh.arkpets.utils.Logger;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/** A StatusNotifierItem based tray icon, used on Linux instead of the AWT system tray.
 * <p>
 * Java AWT only implements the legacy XEmbed tray protocol on Linux. KDE Plasma 6 on
 * Wayland has no XEmbed tray and bridges such icons through {@code xembedsniproxy}, but
 * that bridge silently drops every mouse event for Java clients (KDE bug 498824, still
 * unfixed). The tray icon therefore shows up yet is completely unresponsive, so no menu
 * can ever be opened and no action can be forwarded to the pets.
 * <p>
 * Registering a StatusNotifierItem ourselves puts us on the native tray path: the shell
 * talks to <em>our</em> D-Bus object directly. Because we advertise no DBusMenu, KDE
 * falls back to calling {@link #ContextMenu(int, int)} with the pointer position, which
 * is all the existing Swing menus need.
 * <p>
 * See the <a href="https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/">
 * StatusNotifierItem specification</a>.
 */
public class StatusNotifierTray implements StatusNotifierItem {
    /** Receives the tray events. All callbacks run on the AWT event dispatch thread. */
    public interface Handler {
        void onActivate(int x, int y);

        void onContextMenu(int x, int y);

        void onSecondaryActivate(int x, int y);
    }

    private static final String ITEM_PATH           = "/StatusNotifierItem";
    private static final String WATCHER_SERVICE     = "org.kde.StatusNotifierWatcher";
    private static final String WATCHER_PATH        = "/StatusNotifierWatcher";
    private static final int[]  ICON_SIZES          = {16, 22, 24, 32, 48};
    private static final AtomicInteger INSTANCE_SEQ = new AtomicInteger();

    /** The D-Bus side of {@code org.kde.StatusNotifierWatcher}. */
    @DBusInterfaceName("org.kde.StatusNotifierWatcher")
    private interface StatusNotifierWatcher extends DBusInterface {
        void RegisterStatusNotifierItem(String service);
    }

    private final String id;
    private final String title;
    private final List<IconPixmap> pixmaps;
    private final Handler handler;

    private DBusConnection connection;
    private String busName;

    private StatusNotifierTray(String id, String title, List<IconPixmap> pixmaps, Handler handler) {
        this.id = id;
        this.title = title;
        this.pixmaps = pixmaps;
        this.handler = handler;
    }

    /** Registers a StatusNotifierItem with the current desktop's tray watcher.
     * @param id The item identifier reported to the tray.
     * @param title The item title shown as a tooltip.
     * @param icon The icon image, which must be at least {@code 48x48}.
     * @param handler The callback receiving tray events.
     * @return The registered instance, or null when this desktop has no
     *         StatusNotifierWatcher or registration failed; callers should then fall
     *         back to the AWT system tray.
     */
    public static StatusNotifierTray register(String id, String title, Image icon, Handler handler) {
        try {
            StatusNotifierTray tray = new StatusNotifierTray(id, title, toPixmaps(icon), handler);
            tray.doRegister();
            return tray;
        } catch (Throwable t) {
            Logger.warn("Tray", "StatusNotifierItem is unavailable (" + t.getMessage() + "), falling back to the AWT system tray.");
            return null;
        }
    }

    private void doRegister() throws Exception {
        connection = DBusConnectionBuilder.forSessionBus().build();
        busName = "org.kde.StatusNotifierItem-" + ProcessHandle.current().pid() + "-" + INSTANCE_SEQ.incrementAndGet();
        connection.requestBusName(busName);
        connection.exportObject(ITEM_PATH, this);
        StatusNotifierWatcher watcher =
                connection.getRemoteObject(WATCHER_SERVICE, WATCHER_PATH, StatusNotifierWatcher.class);
        watcher.RegisterStatusNotifierItem(busName);
        Logger.info("Tray", "StatusNotifierItem registered as " + busName);
    }

    /** Releases the D-Bus name and disconnects. The tray icon disappears with it. */
    public void close() {
        if (connection == null)
            return;
        try {
            connection.close();
            Logger.info("Tray", "StatusNotifierItem released (" + busName + ")");
        } catch (Exception e) {
            Logger.error("Tray", "Failed to release the StatusNotifierItem, details see below.", e);
        } finally {
            connection = null;
        }
    }

    /* DBUS INTERFACE IMPLEMENTATION */

    @Override
    public void Activate(int x, int y) {
        Logger.debug("Tray", "StatusNotifierItem activated @ " + x + ", " + y);
        SwingUtilities.invokeLater(() -> handler.onActivate(x, y));
    }

    @Override
    public void ContextMenu(int x, int y) {
        Logger.debug("Tray", "StatusNotifierItem context menu @ " + x + ", " + y);
        SwingUtilities.invokeLater(() -> handler.onContextMenu(x, y));
    }

    @Override
    public void SecondaryActivate(int x, int y) {
        Logger.debug("Tray", "StatusNotifierItem secondary-activated @ " + x + ", " + y);
        SwingUtilities.invokeLater(() -> handler.onSecondaryActivate(x, y));
    }

    @Override
    public void Scroll(int delta, String orientation) {
    }

    @Override
    public String getObjectPath() {
        return ITEM_PATH;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A Get(String interfaceName, String propertyName) {
        return (A) getAllProperties().get(propertyName).getValue();
    }

    @Override
    public Map<String, Variant<?>> GetAll(String interfaceName) {
        return getAllProperties();
    }

    @Override
    public <A> void Set(String interfaceName, String propertyName, A value) {
    }

    private Map<String, Variant<?>> getAllProperties() {
        Map<String, Variant<?>> properties = new LinkedHashMap<>();
        properties.put("Id",            new Variant<>(id));
        properties.put("Category",      new Variant<>("ApplicationStatus"));
        properties.put("Title",         new Variant<>(title));
        properties.put("Status",        new Variant<>("Active"));
        properties.put("WindowId",      new Variant<>(0));
        properties.put("IconThemePath", new Variant<>(""));
        properties.put("IconName",      new Variant<>(""));
        properties.put("IconPixmap",    new Variant<>(pixmaps, "a(iiay)"));
        // No DBusMenu is exported on purpose: KDE then calls ContextMenu(x, y) instead,
        // which lets the existing Swing menus be reused as-is.
        properties.put("Menu",          new Variant<>("/NO_DBUSMENU"));
        properties.put("ItemIsMenu",    new Variant<>(Boolean.FALSE));
        return properties;
    }

    /* ICON CONVERSION */

    private static List<IconPixmap> toPixmaps(Image source) {
        List<IconPixmap> list = new ArrayList<>(ICON_SIZES.length);
        for (int size : ICON_SIZES)
            list.add(toPixmap(source, size));
        return list;
    }

    private static IconPixmap toPixmap(Image source, int size) {
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, size, size, null);
        g.dispose();
        byte[] data = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = target.getRGB(x, y);
                int i = (y * size + x) * 4;
                data[i]     = (byte) ((argb >> 24) & 0xFF); // A
                data[i + 1] = (byte) ((argb >> 16) & 0xFF); // R
                data[i + 2] = (byte) ((argb >> 8) & 0xFF);  // G
                data[i + 3] = (byte) (argb & 0xFF);         // B
            }
        }
        return new IconPixmap(size, size, data);
    }
}
