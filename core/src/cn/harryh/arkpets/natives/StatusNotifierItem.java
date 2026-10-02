/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.natives;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;


/** The StatusNotifierItem D-Bus interface, the tray protocol used by KDE Plasma and
 * (through an applet) by GNOME Shell. Java AWT can only speak the legacy XEmbed tray
 * protocol on Linux, which KDE bridges through xembedsniproxy; that bridge drops every
 * mouse event for Java clients (KDE bug 498824), so the AWT tray icon is unresponsive
 * under Plasma Wayland. Registering this interface directly avoids the broken bridge.
 * <p>
 * See also: <a href="https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/">
 * StatusNotifierItem specification</a>.
 */
@DBusInterfaceName("org.kde.StatusNotifierItem")
public interface StatusNotifierItem extends DBusInterface, Properties {
    /** The item was activated (usually a left click). */
    void Activate(int x, int y);

    /** A context menu was requested (usually a right click). */
    void ContextMenu(int x, int y);

    /** The item was secondary-activated (usually a middle click). */
    void SecondaryActivate(int x, int y);

    /** The item was scrolled. */
    void Scroll(int delta, String orientation);

    /** An icon pixmap in the ARGB32 network-byte-order format required by the specification. */
    class IconPixmap extends Struct {
        @Position(0)
        public final int width;
        @Position(1)
        public final int height;
        @Position(2)
        public final byte[] bytes;

        public IconPixmap(int width, int height, byte[] bytes) {
            this.width = width;
            this.height = height;
            this.bytes = bytes;
        }
    }
}
