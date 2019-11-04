package GUI;


import GUI.MapViewer.GatewayPainter;
import GUI.MapViewer.NumberPainter;
import GUI.util.GUIUtil;
import IotDomain.Environment;
import IotDomain.networkentity.Gateway;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigureGatewayPanel {
    private JPanel mainPanel;
    private JPanel drawPanel;
    private Environment environment;
    private static JXMapViewer mapViewer = new JXMapViewer();
    // Create a TileFactoryInfo for OpenStreetMap
    private static TileFactoryInfo info = new OSMTileFactoryInfo();
    private static DefaultTileFactory tileFactory = new DefaultTileFactory(info);
    private MapMouseAdapter mouseAdapter = new MapMouseAdapter(this);
    private MainGUI parent;

    public ConfigureGatewayPanel(Environment environment, MainGUI parent) {
        this.parent = parent;
        this.environment = environment;
        loadMap(false);
        for (MouseListener ml : mapViewer.getMouseListeners()) {
            mapViewer.removeMouseListener(ml);
        }
        mapViewer.addMouseListener(mouseAdapter);
        mapViewer.setZoom(6);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
    }

    public void refresh() {
        loadMap(true);
        parent.refresh();
    }

    private void loadMap(Boolean isRefresh) {
        mapViewer.setTileFactory(tileFactory);
        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(8);


        int i = 1;
        Map<Waypoint, Integer> gateways = new HashMap<>();
        for (Gateway gateway : environment.getGateways()) {
            gateways.put(new DefaultWaypoint(new GeoPosition(environment.toLatitude(gateway.getYPos()), environment.toLongitude(gateway.getXPos()))), i);
            i++;
        }

        GatewayPainter<Waypoint> gatewayPainter = new GatewayPainter<>();
        gatewayPainter.setWaypoints(gateways.keySet());

        NumberPainter<Waypoint> gatewayNumberPainter = new NumberPainter<>(NumberPainter.Type.GATEWAY);
        gatewayNumberPainter.setWaypoints(gateways);

        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        painters.add(gatewayPainter);
        painters.add(gatewayNumberPainter);


        // Draw the borders
        painters.addAll(GUIUtil.getBorderPainters(environment.getMaxXpos(), environment.getMaxYpos()));


        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);

        if (!isRefresh) {
            mapViewer.setAddressLocation(environment.getMapCenter());
            mapViewer.setZoom(5);
        }

        drawPanel.add(mapViewer);
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        drawPanel = new JPanel();
        drawPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(drawPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        drawPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 15), null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 15), null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        mainPanel.add(spacer3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(15, -1), null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        mainPanel.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(15, -1), null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private class MapMouseAdapter implements MouseListener {
        private ConfigureGatewayPanel panel;

        MapMouseAdapter(ConfigureGatewayPanel panel) {
            this.panel = panel;
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
                Point p = e.getPoint();
                GeoPosition geo = mapViewer.convertPointToGeoPosition(p);
                Boolean exists = false;
                for (Gateway gateway : environment.getGateways()) {
                    Integer xDistance = Math.abs(environment.toMapXCoordinate(geo) - gateway.getXPos());
                    Integer yDistance = environment.toMapYCoordinate(geo) - gateway.getYPos();
                    if (xDistance < 100 && yDistance > -20 && yDistance < 250) {
                        JFrame frame = new JFrame("Gateway settings");
                        GatewayGUI gatewayGUI = new GatewayGUI(gateway, frame);
                        frame.setContentPane(gatewayGUI.getMainPanel());
                        frame.setPreferredSize(new Dimension(600, 400));
                        frame.setMinimumSize(new Dimension(600, 400));
                        frame.setVisible(true);
                        exists = true;
                    }
                }

                if (!exists) {
                    JFrame frame = new JFrame("New gateway");
                    NewGatewayGUI newGatewayGUI = new NewGatewayGUI(environment, geo, frame, panel);
                    frame.setContentPane(newGatewayGUI.getMainPanel());
                    frame.setPreferredSize(new Dimension(600, 400));
                    frame.setMinimumSize(new Dimension(600, 400));
                    frame.setVisible(true);
                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

}
