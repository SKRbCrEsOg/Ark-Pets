/** Copyright (c) 2022-2026, Harry Huang, Half Nothing
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.ArkPets;
import cn.harryh.arkpets.Const;
import cn.harryh.arkpets.animations.AnimData;
import cn.harryh.arkpets.concurrent.SocketClient;
import cn.harryh.arkpets.concurrent.SocketData;
import cn.harryh.arkpets.concurrent.SocketSession;
import cn.harryh.arkpets.utils.Logger;
import com.badlogic.gdx.Gdx;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Timer;
import java.util.TimerTask;

import static cn.harryh.arkpets.Const.durationNormal;
import static cn.harryh.arkpets.Const.iconFilePng;


public class MemberTrayImpl extends MemberTray {
    private final ArkPets arkPets;
    private final SocketClient client;
    private final JDialog popWindow;
    private final JPopupMenu popMenu;
    private TrayIcon icon;
    public AnimData keepAnim;

    /** The native Linux tray backend, used instead of {@link #icon} when available. */
    private StatusNotifierTray sniTray;

    /** Initializes a per-character tray icon instance for an ArkPets. <br/>
     * Must be used after Gdx.app was initialized.
     * @param boundArkPets The ArkPets instance that bound to the tray icon.
     * @param client The socket client that bound to the tray icon.
     */
    public MemberTrayImpl(ArkPets boundArkPets, SocketClient client) {
        super(getName(boundArkPets));
        arkPets = boundArkPets;
        this.client = client;

        // Ui Components:
        popWindow = new JDialog();
        popWindow.setUndecorated(true);
        popWindow.setSize(1, 1);
        JLabel innerLabel = new JLabel(" " + name + " ");
        innerLabel.setAlignmentX(0.5f);

        popMenu = new JPopupMenu() {
            @Override
            public void firePopupMenuWillBecomeInvisible() {
                popWindow.setVisible(false); // Hide the container when the menu is invisible.
            }
        };
        popMenu.add(innerLabel);
        popMenu.add(optKeepAnimEn);
        popMenu.add(optTransparentEn);
        if (arkPets.canChangeStage())
            popMenu.add(optChangeStage);
        popMenu.add(optExit);
        popMenu.setSize(100, 24 * popMenu.getSubElements().length);

        Runnable onConnected = this::onConnected;
        SocketSession session = new SocketClient.ClientSocketSession(client, this);
        client.connect(onConnected, session);
        if (!client.isConnected()) {
            onDisconnected();
            client.connectWithRetry(onConnected, session);
        }
    }

    private static String getName(ArkPets boundArkPets) {
        return (boundArkPets.config.character_label == null || boundArkPets.config.character_label.isEmpty()) ?
                "Unknown" : boundArkPets.config.character_label;
    }

    private TrayIcon getTrayIcon(Image image) {
        try {
            // Scale to the tray cell size; KDE's StatusNotifier/XEmbed proxy ignores
            // setImageAutoSize and would otherwise crop the large source image.
            image = scaleForTray(image);
            icon = new TrayIcon(image, name);
        } catch (Exception e) {
            Logger.error("MemberTray", "Unable to apply MemberTray icon, details see below.", e);
            return icon;
        }
        icon.setImageAutoSize(false);
        icon.addMouseListener(new MouseAdapter() {
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
                    Logger.info("MemberTray", "Tray icon popup requested");
                    Point p = pointerLocation(e);
                    showDialog(p.x + 5, p.y);
                }
            }
        });
        return icon;
    }

    /** Scales an image to the system tray icon size (explicit redraw; lazy
     * getScaledInstance is not honored by the Linux tray backend). */
    private static Image scaleForTray(Image source) {
        Dimension traySize = SystemTray.getSystemTray().getTrayIconSize();
        int w = traySize == null ? 24 : Math.max(1, traySize.width);
        int h = traySize == null ? 24 : Math.max(1, traySize.height);
        Logger.debug("MemberTray", "Tray icon size " + w + "x" + h);
        java.awt.image.BufferedImage target =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return target;
    }

    /** Loads the tray icon image synchronously (Toolkit.getImage is async and would
     * yield a blank image when scaled immediately). */
    private static Image loadIcon() {
        try {
            return ImageIO.read(MemberTrayImpl.class.getResource(iconFilePng));
        } catch (Exception e) {
            Logger.error("MemberTray", "Failed to load the tray icon image, details see below.", e);
            return Toolkit.getDefaultToolkit().createImage(MemberTrayImpl.class.getResource(iconFilePng));
        }
    }

    /** Gets the pointer's screen location, falling back to the event coordinates. */
    private static Point pointerLocation(MouseEvent e) {
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            if (info != null)
                return info.getLocation();
        } catch (Exception ignored) {
        }
        return new Point(e.getX(), e.getY());
    }

    @Override
    public void onExit() {
        Logger.info("MemberTray", "Request to exit");
        remove();
        client.disconnect();
        arkPets.cha.setAlpha(0f);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Gdx.app.exit();
            }
        }, (int) durationNormal.toMillis());
    }

    @Override
    public void onChangeStage() {
        Logger.info("MemberTray", "Request to change stage");
        arkPets.changeStage();
        if (keepAnim != null) {
            keepAnim = null;
            popMenu.remove(optKeepAnimDis);
            popMenu.add(optKeepAnimEn, 1);
        }
    }

    @Override
    public void onTransparentDis() {
        Logger.info("MemberTray", "Transparent disabled");
        arkPets.setTransparentMode(false);
        popMenu.remove(optTransparentDis);
        popMenu.add(optTransparentEn, 2);
    }

    @Override
    public void onTransparentEn() {
        Logger.info("MemberTray", "Transparent enabled");
        arkPets.setTransparentMode(true);
        popMenu.remove(optTransparentEn);
        popMenu.add(optTransparentDis, 2);
    }

    @Override
    public void onKeepAnimDis() {
        Logger.info("MemberTray", "Action-Mode disabled");
        keepAnim = null;
        popMenu.remove(optKeepAnimDis);
        popMenu.add(optKeepAnimEn, 1);
    }

    @Override
    public void onKeepAnimEn() {
        Logger.info("MemberTray", "Action-Mode enabled");
        keepAnim = arkPets.behavior.defaultAnim();
        popMenu.remove(optKeepAnimEn);
        popMenu.add(optKeepAnimDis, 1);
    }

    @Override
    public void sendOperation(SocketData.Operation operation) {
        client.sendRequest(SocketData.ofOperation(uuid, operation));
    }

    @Override
    public void remove() {
        popMenu.removeAll();
        popWindow.dispose();
        client.disconnect();
    }

    public void onConnected() {
        // If integration was succeeded, remove the ISOLATED tray icon.
        Logger.info("MemberTray", "Integrated tray service connected");
        if (icon != null) SystemTray.getSystemTray().remove(icon);
        client.sendRequest(SocketData.ofLogin(uuid, name));
        if (arkPets.canChangeStage())
            sendOperation(SocketData.Operation.CAN_CHANGE_STAGE);
        for (MenuElement element : popMenu.getSubElements()) {
            if (element.equals(optKeepAnimDis))
                sendOperation(SocketData.Operation.KEEP_ACTION);
            if (element.equals(optTransparentDis))
                sendOperation(SocketData.Operation.TRANSPARENT_MODE);
        }
    }

    public void onDisconnected() {
        // When connection was broken:
        Logger.info("MemberTray", "Integrated tray service disconnected");
        Image image = loadIcon();

        /* Prefer a native StatusNotifierItem on Linux. Java AWT can only use the legacy
        XEmbed protocol there, and KDE's xembedsniproxy bridges it without forwarding any
        mouse event to Java clients (KDE bug 498824) - such an icon is unresponsive. */
        if (Const.isLinux) {
            sniTray = StatusNotifierTray.register(name, name, image, new StatusNotifierTray.Handler() {
                @Override
                public void onActivate(int x, int y) {
                    showDialogAt(x, y);
                }

                @Override
                public void onContextMenu(int x, int y) {
                    showDialogAt(x, y);
                }

                @Override
                public void onSecondaryActivate(int x, int y) {
                    showDialogAt(x, y);
                }
            });
            if (sniTray != null) {
                Logger.info("MemberTray", "Isolated StatusNotifierItem applied");
                return;
            }
        }

        icon = getTrayIcon(image);
        // Add the ISOLATED tray icon to the system tray.
        try {
            SystemTray.getSystemTray().add(icon);
            Logger.info("MemberTray", "Isolated tray icon applied");
        } catch (Exception e) {
            Logger.error("MemberTray", "Unable to apply isolated tray icon, details see below", e);
        }
    }

    /** Hides the menu.
     */
    public synchronized void hideDialog() {
        if (popMenu.isVisible()) {
            popMenu.setVisible(false);
            Logger.debug("MemberTray", "Hidden");
        }
    }

    /** Shows the menu at the given coordinate.
     */
    public synchronized void showDialog(int x, int y) {
        /* Use `System.setProperty("sun.java2d.uiScale", "1")` can also avoid system scaling.
        Here we will adapt the coordinate for system scaling artificially. See below. */
        GraphicsConfiguration gc = popWindow.getGraphicsConfiguration();
        if (gc == null) {
            // The dialog has no peer yet (e.g. on Wayland before it is shown); fall back
            // to the default screen so the popup still appears.
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
    public synchronized void showDialogAt(int x, int y) {
        // Show the JDialog together with the JPopupMenu.
        popWindow.setLocation(x, y - popMenu.getHeight());
        popWindow.setVisible(true);
        popMenu.show(popWindow, 0, 0);
        Logger.debug("MemberTray", "Shown @ " + x + ", " + y);
    }

    /** Toggles the menu at the given coordinate.
     */
    public void toggleDialog(int x, int y) {
        if (popMenu.isVisible()) {
            hideDialog();
        } else {
            showDialog(x, y);
        }
    }
}
