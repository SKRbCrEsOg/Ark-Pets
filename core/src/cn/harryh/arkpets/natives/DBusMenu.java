/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.natives;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;


/** The {@code com.canonical.dbusmenu} interface, the menu companion of
 * {@link StatusNotifierItem}.
 * <p>
 * Hosts that render tray menus themselves (KDE Plasma, GNOME Shell with the
 * AppIndicator extension, Waybar, ...) get the menu from here instead of asking the
 * item to show one. Exposing it is what makes the tray work across desktops: a host
 * that finds no DBusMenu either calls {@code ContextMenu()} (KDE) or shows no icon at
 * all (GNOME).
 * <p>
 * See the <a href="https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/">
 * StatusNotifierItem specification</a> and the
 * <a href="https://gitlab.freedesktop.org/dbus/dbusmenu">DBusMenu reference</a>.
 */
@DBusInterfaceName("com.canonical.dbusmenu")
public interface DBusMenu extends DBusInterface, Properties {
    /** The D-Bus menu revision sent on every {@code LayoutUpdated} signal. */
    int VERSION = 3;

    /** Gets the menu tree below the given parent.
     * @param parentId The parent node id; {@code 0} is the root.
     * @param recursionDepth How many levels of children to include, negative for all.
     * @param propertyNames The properties to include, empty for all of them.
     * @return The revision together with the layout subtree.
     */
    LayoutReply<UInt32, LayoutItem> GetLayout(int parentId, int recursionDepth, List<String> propertyNames);

    /** Gets the properties of several nodes at once. */
    List<PropertyUpdate> GetGroupProperties(List<Integer> ids, List<String> propertyNames);

    /** Gets a single property of a node. */
    Variant<?> GetProperty(int id, String name);

    /** Notifies the menu that the user interacted with a node.
     * @param id The node id.
     * @param eventId The event name, typically {@code "clicked"}, {@code "opened"} or
     *                {@code "closed"}.
     * @param data Event specific payload, usually an empty variant.
     * @param timestamp The event time, in milliseconds.
     */
    void Event(int id, String eventId, Variant<?> data, UInt32 timestamp);

    /** Sends several events at once. Returns the ids that could not be processed. */
    List<Integer> EventGroup(List<MenuEvent> events);

    /** Tells the menu it is about to be shown, so it can refresh itself.
     * @return Whether the node's children need to be fetched again.
     */
    boolean AboutToShow(int id);

    /** The batched form of {@link #AboutToShow(int)};
     * @return The ids that need to be fetched again, followed by the unknown ids.
     */
    AboutToShowReply<List<Integer>, List<Integer>> AboutToShowGroup(List<Integer> ids);

    /** One menu node: {@code (ia{sv}av)}. */
    class LayoutItem extends Struct {
        @Position(0)
        public final int id;
        @Position(1)
        public final Map<String, Variant<?>> properties;
        @Position(2)
        public final List<Variant<LayoutItem>> children;

        public LayoutItem(int id, Map<String, Variant<?>> properties, List<Variant<LayoutItem>> children) {
            this.id = id;
            this.properties = properties;
            this.children = children;
        }
    }

    /** The reply of {@link #GetLayout(int, int, List)}.
     * <p>
     * This extends {@link Tuple} rather than {@link Struct} on purpose: the wire
     * signature has to be the two separate out arguments {@code u(ia{sv}av)}. A Struct
     * would be marshalled as the single argument {@code (u(ia{sv}av))}, which hosts
     * reject because their generated {@code QDBusPendingReply<uint, LayoutItem>} no
     * longer matches, leaving an empty menu.
     * <p>
     * The (otherwise unused) type parameters are required as well: dbus-java derives
     * the out-argument types from the generic return type and its Tuple handling only
     * accepts parameterised types.
     */
    class LayoutReply<T1, T2> extends Tuple {
        @Position(0)
        public final UInt32 revision;
        @Position(1)
        public final LayoutItem layout;

        public LayoutReply(UInt32 revision, LayoutItem layout) {
            this.revision = revision;
            this.layout = layout;
        }
    }

    /** One entry of the reply of {@link #GetGroupProperties(List, List)}: {@code (ia{sv})}. */
    class PropertyUpdate extends Struct {
        @Position(0)
        public final int id;
        @Position(1)
        public final Map<String, Variant<?>> properties;

        public PropertyUpdate(int id, Map<String, Variant<?>> properties) {
            this.id = id;
            this.properties = properties;
        }
    }

    /** One entry accepted by {@link #EventGroup(List)}: {@code (isvu)}. */
    class MenuEvent extends Struct {
        @Position(0)
        public final int id;
        @Position(1)
        public final String eventId;
        @Position(2)
        public final Variant<?> data;
        @Position(3)
        public final UInt32 timestamp;

        public MenuEvent(int id, String eventId, Variant<?> data, UInt32 timestamp) {
            this.id = id;
            this.eventId = eventId;
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    /** The reply of {@link #AboutToShowGroup(List)}; see {@link LayoutReply} for why this
     * is a {@link Tuple}.
     */
    class AboutToShowReply<T1, T2> extends Tuple {
        @Position(0)
        public final List<Integer> updatesNeeded;
        @Position(1)
        public final List<Integer> idErrors;

        public AboutToShowReply(List<Integer> updatesNeeded, List<Integer> idErrors) {
            this.updatesNeeded = updatesNeeded;
            this.idErrors = idErrors;
        }
    }
}
