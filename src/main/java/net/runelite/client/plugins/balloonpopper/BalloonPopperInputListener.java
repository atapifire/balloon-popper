package net.runelite.client.plugins.balloonpopper;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.inject.Inject;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;

public class BalloonPopperInputListener implements MouseListener, MouseWheelListener {
    private final BalloonPopperGuideOverlay guideOverlay;
    private final BalloonPopperLevelUpOverlay levelUpOverlay;
    private final BalloonPopperPlugin plugin;

    @Inject
    public BalloonPopperInputListener(BalloonPopperGuideOverlay guideOverlay,
            BalloonPopperLevelUpOverlay levelUpOverlay, BalloonPopperPlugin plugin) {
        this.guideOverlay = guideOverlay;
        this.levelUpOverlay = levelUpOverlay;
        this.plugin = plugin;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        if (guideOverlay != null && guideOverlay.isMouseOverOverlay(e.getPoint())) {
            guideOverlay.handleClick(e.getPoint());
            e.consume();
        } else if (levelUpOverlay != null && levelUpOverlay.isMouseOverOverlay(e.getPoint())) {
            plugin.hideLevelUp();
            e.consume();
        }
        return e;
    }

    @Override
    public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e) {
        if (guideOverlay != null && guideOverlay.isMouseOverGrid(e.getPoint())) {
            guideOverlay.handleMouseWheel(e.getWheelRotation());
            e.consume();
        }
        return e;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e) {
        return e;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        return e;
    }
}
