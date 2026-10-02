/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.natives.DBusMenu;
import cn.harryh.arkpets.utils.Logger;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/** Exposes an existing Swing {@link JPopupMenu} tree as a {@code com.canonical.dbusmenu}.
 * <p>
 * The tree is walked on every {@link #GetLayout(int, int, List)} call rather than being
 * mirrored, so the dynamic parts of the ArkPets menus (a pet appearing or a menu entry
 * being swapped) are always picked up. Node ids stay stable across calls because they
 * are assigned per component instance and kept in an identity map; a host that clicks an
 * entry therefore always reaches the same {@link JMenuItem}.
 */
public class SwingDBusMenu implements DBusMenu {
    /** The object path the menu is exported at, referenced by the item's {@code Menu} property. */
    public static final String MENU_PATH = "/MenuBar";

    private final Supplier<JPopupMenu> rootSupplier;
    private final Map<Component, Integer> idByComponent = new IdentityHashMap<>();
    private final Map<Integer, Component> componentById = new HashMap<>();
    private int nextId = 1;
    private int revision = 1;

    public SwingDBusMenu(Supplier<JPopupMenu> rootSupplier) {
        this.rootSupplier = rootSupplier;
    }

    /* DBUS INTERFACE IMPLEMENTATION */

    @Override
    public LayoutReply<UInt32, LayoutItem> GetLayout(int parentId, int recursionDepth, List<String> propertyNames) {
        LayoutItem item;
        if (parentId == 0) {
            item = new LayoutItem(0, properties(Map.of("children-display", "submenu")),
                    collect(menuRoot(), recursionDepth));
        } else {
            Component parent = componentById.get(parentId);
            Map<String, Variant<?>> props = parent instanceof JMenu
                    ? properties(Map.of("children-display", "submenu"))
                    : properties(Map.of());
            item = new LayoutItem(parentId, props, collect(parent, recursionDepth));
        }
        return new LayoutReply<UInt32, LayoutItem>(new UInt32(revision), item);
    }

    @Override
    public List<PropertyUpdate> GetGroupProperties(List<Integer> ids, List<String> propertyNames) {
        List<PropertyUpdate> updates = new ArrayList<>();
        for (Integer id : ids) {
            Component c = componentById.get(id);
            if (c != null)
                updates.add(new PropertyUpdate(id, describe(c)));
        }
        return updates;
    }

    @Override
    public Variant<?> GetProperty(int id, String name) {
        Component c = componentById.get(id);
        return c == null ? null : describe(c).get(name);
    }

    @Override
    public void Event(int id, String eventId, Variant<?> data, UInt32 timestamp) {
        if (!"clicked".equals(eventId))
            return;
        Component c = componentById.get(id);
        if (!(c instanceof JMenuItem) || c instanceof JMenu || !c.isEnabled())
            return;
        Logger.debug("Tray", "Menu entry " + id + " clicked");
        JMenuItem item = (JMenuItem) c;
        SwingUtilities.invokeLater(item::doClick);
    }

    @Override
    public List<Integer> EventGroup(List<MenuEvent> events) {
        for (MenuEvent event : events)
            Event(event.id, event.eventId, event.data, event.timestamp);
        return Collections.emptyList();
    }

    @Override
    public boolean AboutToShow(int id) {
        // Always request a refresh. This must not report "unchanged": hosts use a false
        // return as "reuse the cached menu", and the ArkPets menus change while they are
        // in use (enabling transparent mode swaps that entry for its counterpart, a pet
        // appearing adds a submenu, ...). Skipping the refresh left the host showing a
        // stale entry, so e.g. transparent mode could be switched on but never off.
        // The menus are tiny, so the extra GetLayout per opening is negligible.
        return true;
    }

    @Override
    public AboutToShowReply<List<Integer>, List<Integer>> AboutToShowGroup(List<Integer> ids) {
        return new AboutToShowReply<List<Integer>, List<Integer>>(Collections.<Integer>emptyList(),
                Collections.<Integer>emptyList());
    }

    @Override
    public String getObjectPath() {
        return MENU_PATH;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A Get(String interfaceName, String propertyName) {
        return (A) menuProperties().get(propertyName).getValue();
    }

    @Override
    public Map<String, Variant<?>> GetAll(String interfaceName) {
        return menuProperties();
    }

    @Override
    public <A> void Set(String interfaceName, String propertyName, A value) {
    }

    /* TREE WALKING */

    private JPopupMenu menuRoot() {
        return rootSupplier.get();
    }

    /** Collects the direct children of a container, honouring the requested depth. */
    private List<Variant<LayoutItem>> collect(Component parent, int depth) {
        List<Variant<LayoutItem>> children = new ArrayList<>();
        if (parent == null || depth == 0)
            return children;
        for (Component child : childrenOf(parent)) {
            int id = idOf(child);
            // JPopupMenu instances only ever appear as an implementation detail of a
            // JMenu, which is already represented by its own entry.
            if (child instanceof JPopupMenu)
                continue;
            children.add(new Variant<LayoutItem>(
                    new LayoutItem(id, describe(child), collect(child, depth < 0 ? -1 : depth - 1)),
                    "(ia{sv}av)"));
        }
        return children;
    }

    private static Component[] childrenOf(Component c) {
        if (c instanceof JMenu)
            return ((JMenu) c).getMenuComponents();
        if (c instanceof Container)
            return ((Container) c).getComponents();
        return new Component[0];
    }

    private static Map<String, Variant<?>> properties(Map<String, String> raw) {
        Map<String, Variant<?>> props = new LinkedHashMap<>();
        raw.forEach((k, v) -> props.put(k, new Variant<String>(v)));
        return props;
    }

    /** Describes one Swing component as DBusMenu properties. */
    private Map<String, Variant<?>> describe(Component c) {
        Map<String, Variant<?>> props = new LinkedHashMap<>();
        String label = labelOf(c);
        if (label != null)
            props.put("label", new Variant<String>(label));
        if (c instanceof JMenu) {
            props.put("enabled", new Variant<Boolean>(c.isEnabled()));
            props.put("visible", new Variant<Boolean>(c.isVisible()));
            props.put("children-display", new Variant<String>("submenu"));
        } else if (c instanceof JMenuItem) {
            props.put("enabled", new Variant<Boolean>(c.isEnabled()));
            props.put("visible", new Variant<Boolean>(c.isVisible()));
        } else if (label != null) {
            // A plain JLabel: a non-interactive section heading in the ArkPets menus.
            props.put("enabled", new Variant<Boolean>(Boolean.FALSE));
            props.put("visible", new Variant<Boolean>(c.isVisible()));
        } else {
            // Separators and the invisible struts used as spacing.
            props.put("type", new Variant<String>("separator"));
            props.put("visible", new Variant<Boolean>(c.isVisible()));
        }
        return props;
    }

    private static String labelOf(Component c) {
        String text = null;
        if (c instanceof AbstractButton)
            text = ((AbstractButton) c).getText();
        else if (c instanceof JLabel)
            text = ((JLabel) c).getText();
        if (text == null)
            return null;
        text = text.replace("&", "").trim();
        return text.isEmpty() ? null : text;
    }

    private int idOf(Component c) {
        Integer id = idByComponent.get(c);
        if (id != null)
            return id;
        int assigned = nextId++;
        idByComponent.put(c, assigned);
        componentById.put(assigned, c);
        return assigned;
    }

    private Map<String, Variant<?>> menuProperties() {
        Map<String, Variant<?>> props = new LinkedHashMap<>();
        props.put("Version", new Variant<UInt32>(new UInt32(VERSION)));
        props.put("TextDirection", new Variant<String>("ltr"));
        props.put("Status", new Variant<String>("normal"));
        props.put("IconThemePath", new Variant<List<String>>(Collections.<String>emptyList(), "as"));
        return props;
    }
}
