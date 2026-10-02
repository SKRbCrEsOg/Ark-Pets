/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.Const;
import cn.harryh.arkpets.utils.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;


public class HostTray {
    protected TrayIcon trayIcon;
    protected boolean initialized = false;
    protected Map<UUID, MemberTray> arkPetTrays = new HashMap<>();

    /** The native Linux tray backend, used instead of {@link #trayIcon} when available. */
    private StatusNotifierTray sniTray;

    private JDialog popWindow;
    private JPopupMenu popMenu;
    private JMenu playerMenu;

    private Runnable onShowStage;
    private Runnable onCloseStage;

    private static HostTray instance;

    static {
        // Avoid AWT Thread problem.
        SwingUtilities.invokeLater(() -> {
            try {
                String laf = UIManager.getSystemLookAndFeelClassName();
                if (laf.contains("WindowsLookAndFeel")) {
                    UIManager.put("MenuItem.margin", new Insets(0, -16, 0, 0));
                    UIManager.put("Menu.margin", new Insets(0, -16, 0, 0));
                }
                UIManager.setLookAndFeel(laf);
            } catch (Exception ignored) {
            }
        });
        Const.FontsConfig.REGULAR.loadFontToSwing();
    }

    public static HostTray getInstance() {
        if (instance == null)
            instance = new HostTray();
        return instance;
    }

    private HostTray() {
        if (SystemTray.isSupported()) {
            // Ui Components:
            popWindow = new JDialog();
            popWindow.setUndecorated(true);
            popWindow.setSize(1, 1);
            JLabel innerLabel = new JLabel(" ArkPets ");
            innerLabel.setAlignmentX(0.5f);
            Component innerSep = Box.createVerticalStrut(5);

            playerMenu = new JMenu("角色管理");
            JMenuItem optExit = new JMenuItem("退出程序");
            optExit.addActionListener(e -> {
                Logger.info("HostTray", "Request to exit");
                if (onCloseStage != null)
                    onCloseStage.run();
            });

            popMenu = new JPopupMenu() {
                @Override
                public void firePopupMenuWillBecomeInvisible() {
                    popWindow.setVisible(false); // Hide the container when the menu is invisible.
                }
            };
            popMenu.add(innerLabel);
            popMenu.add(innerSep);
            popMenu.add(playerMenu);
            popMenu.add(optExit);
            popMenu.setSize(100, 24 * popMenu.getSubElements().length);

            Image image = loadIcon();
            image = scaleForTray(image);
            trayIcon = new TrayIcon(image, "ArkPets");
            trayIcon.setImageAutoSize(false);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    handlePopup(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    handlePopup(e);
                }

                private void handlePopup(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                        Logger.info("HostTray", "Tray icon popup requested");
                        Point p;
                        PointerInfo info = MouseInfo.getPointerInfo();
                        if (info != null) {
                            p = info.getLocation();
                        } else {
                            p = new Point(e.getX(), e.getY());
                        }
                        showDialog(p.x + 5, p.y);
                    }
                }
            });
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == 1)
                        showStage();
                }
            });
            SwingUtilities.updateComponentTreeUI(popMenu);
            popMenu.pack();
        } else {
            Logger.error("HostTray", "Tray is not supported.");
        }
    }

    public void applyTrayIcon() {
        if (initialized)
            return;
        /* Prefer a native StatusNotifierItem on Linux. Java AWT can only use the legacy
        XEmbed protocol there, and KDE's xembedsniproxy bridges it without forwarding any
        mouse event to Java clients (KDE bug 498824) - such an icon is unresponsive, so
        the aggregated pet menus could never be reached. */
        if (Const.isLinux) {
            sniTray = StatusNotifierTray.register(Const.appName, Const.appName, loadIcon(),
                    new StatusNotifierTray.Handler() {
                        @Override
                        public void onActivate(int x, int y) {
                            showStage();
                        }

                        @Override
                        public void onContextMenu(int x, int y) {
                            showDialogAt(x, y);
                        }

                        @Override
                        public void onSecondaryActivate(int x, int y) {
                            showStage();
                        }
                    });
            if (sniTray != null) {
                initialized = true;
                return;
            }
        }
        if (trayIcon == null)
            return;
        try {
            SystemTray.getSystemTray().add(trayIcon);
            Logger.info("HostTray", "HostTray icon applied");
            initialized = true;
        } catch (AWTException e) {
            Logger.error("HostTray", "Unable to apply HostTray icon, details see below.", e);
        }
    }

    /** Loads the tray icon image synchronously (Toolkit.getImage is async and would
     * yield a blank image when scaled immediately). */
    private static Image loadIcon() {
        try {
            return ImageIO.read(HostTray.class.getResource(Const.iconFilePng));
        } catch (Exception e) {
            Logger.error("HostTray", "Failed to load the tray icon image, details see below.", e);
            return Toolkit.getDefaultToolkit().createImage(HostTray.class.getResource(Const.iconFilePng));
        }
    }

    public void showDialog(int x, int y) {
        /* Use `System.setProperty("sun.java2d.uiScale", "1")` can also avoid system scaling.
        Here we will adapt the coordinate for system scaling artificially. See below. */
        GraphicsConfiguration gc = popWindow.getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
        }
        AffineTransform at = gc.getDefaultTransform();
        // AWT reports the pointer in device pixels; undo the UI scale to get the logical
        // coordinates that the popup window is positioned with.
        showDialogAt((int) (x / at.getScaleX()), (int) (y / at.getScaleY()));
    }

    /** Shows the menu at a logical screen position, as reported by the compositor
     * through the StatusNotifierItem protocol.
     */
    public void showDialogAt(int x, int y) {
        if (!initialized)
            return;
        // Show the JDialog together with the JPopupMenu.
        popWindow.setVisible(true);
        popWindow.setLocation(x, y - popMenu.getHeight());
        popMenu.show(popWindow, 0, 0);
    }

    /** Scales an image to the system tray icon size (explicit redraw; lazy
     * getScaledInstance is not honored by the Linux tray backend). */
    private static Image scaleForTray(Image source) {
        Dimension traySize = SystemTray.getSystemTray().getTrayIconSize();
        int w = traySize == null ? 24 : Math.max(1, traySize.width);
        int h = traySize == null ? 24 : Math.max(1, traySize.height);
        Logger.debug("HostTray", "Tray icon size " + w + "x" + h);
        java.awt.image.BufferedImage target =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return target;
    }

    public void showStage() {
        if (!initialized)
            return;
        Logger.info("HostTray", "Request to show stage");
        if (onShowStage != null)
            onShowStage.run();
    }

    public void setOnShowStage(Runnable handler) {
        onShowStage = handler;
    }

    public void setOnCloseStage(Runnable handler) {
        onCloseStage = handler;
    }

    public MemberTray getMemberTray(UUID uuid) {
        return arkPetTrays.get(uuid);
    }

    public void forEachMemberTray(Consumer<MemberTray> action) {
        arkPetTrays.values().forEach(action);
    }

    public void addMemberTray(JMenu menu) {
        playerMenu.add(menu);
    }

    public void removeMemberTray(JMenu menu) {
        playerMenu.remove(menu);
    }

    public void addMemberTray(UUID uuid, MemberTray tray) {
        arkPetTrays.put(uuid, tray);
    }

    public void removeMemberTray(UUID uuid) {
        getMemberTray(uuid).remove();
        arkPetTrays.remove(uuid);
    }
}
