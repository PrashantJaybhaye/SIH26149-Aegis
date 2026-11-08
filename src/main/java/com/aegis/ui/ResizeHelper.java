package com.aegis.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.function.BooleanSupplier;

/**
 * Utility helper enabling smooth edge and corner resizing for undecorated JavaFX stages.
 */
public final class ResizeHelper {

    private static final int BORDER_THRESHOLD = 6;

    private static double startX;
    private static double startY;
    private static double startWidth;
    private static double startHeight;
    private static double stageX;
    private static double stageY;
    private static boolean isResizing = false;
    private static Cursor currentCursor = Cursor.DEFAULT;

    private ResizeHelper() {
        // Utility class
    }

    /**
     * Installs mouse event listeners on the specified scene to allow resizing the undecorated stage.
     *
     * @param stage target JavaFX stage
     * @param scene window scene
     * @param isMaximizedSupplier supplier indicating whether the stage is currently maximized
     */
    public static void addResizeSupport(Stage stage, Scene scene, BooleanSupplier isMaximizedSupplier) {
        scene.setOnMouseMoved(e -> {
            if (isMaximizedSupplier.getAsBoolean() || stage.isMaximized()) {
                if (currentCursor != Cursor.DEFAULT) {
                    currentCursor = Cursor.DEFAULT;
                    scene.setCursor(null);
                }
                return;
            }
            double x = e.getSceneX();
            double y = e.getSceneY();
            double w = scene.getWidth();
            double h = scene.getHeight();

            Cursor newCursor = Cursor.DEFAULT;
            if (x < BORDER_THRESHOLD && y < BORDER_THRESHOLD) {
                newCursor = Cursor.NW_RESIZE;
            } else if (x > w - BORDER_THRESHOLD && y < BORDER_THRESHOLD) {
                newCursor = Cursor.NE_RESIZE;
            } else if (x < BORDER_THRESHOLD && y > h - BORDER_THRESHOLD) {
                newCursor = Cursor.SW_RESIZE;
            } else if (x > w - BORDER_THRESHOLD && y > h - BORDER_THRESHOLD) {
                newCursor = Cursor.SE_RESIZE;
            } else if (x < BORDER_THRESHOLD) {
                newCursor = Cursor.W_RESIZE;
            } else if (x > w - BORDER_THRESHOLD) {
                newCursor = Cursor.E_RESIZE;
            } else if (y < BORDER_THRESHOLD) {
                newCursor = Cursor.N_RESIZE;
            } else if (y > h - BORDER_THRESHOLD) {
                newCursor = Cursor.S_RESIZE;
            }

            if (newCursor != currentCursor) {
                currentCursor = newCursor;
                scene.setCursor(newCursor == Cursor.DEFAULT ? null : newCursor);
            }
        });

        scene.setOnMousePressed(e -> {
            if (currentCursor != Cursor.DEFAULT && !isMaximizedSupplier.getAsBoolean() && !stage.isMaximized()) {
                isResizing = true;
                startX = e.getScreenX();
                startY = e.getScreenY();
                startWidth = stage.getWidth();
                startHeight = stage.getHeight();
                stageX = stage.getX();
                stageY = stage.getY();
                e.consume();
            } else {
                isResizing = false;
            }
        });

        scene.setOnMouseDragged(e -> {
            if (!isResizing || isMaximizedSupplier.getAsBoolean() || stage.isMaximized()) {
                return;
            }
            double dx = e.getScreenX() - startX;
            double dy = e.getScreenY() - startY;

            if (currentCursor == Cursor.E_RESIZE || currentCursor == Cursor.NE_RESIZE || currentCursor == Cursor.SE_RESIZE) {
                stage.setWidth(Math.max(stage.getMinWidth(), startWidth + dx));
            }
            if (currentCursor == Cursor.S_RESIZE || currentCursor == Cursor.SW_RESIZE || currentCursor == Cursor.SE_RESIZE) {
                stage.setHeight(Math.max(stage.getMinHeight(), startHeight + dy));
            }
            if (currentCursor == Cursor.W_RESIZE || currentCursor == Cursor.NW_RESIZE || currentCursor == Cursor.SW_RESIZE) {
                double newWidth = startWidth - dx;
                if (newWidth >= stage.getMinWidth()) {
                    stage.setX(stageX + dx);
                    stage.setWidth(newWidth);
                }
            }
            if (currentCursor == Cursor.N_RESIZE || currentCursor == Cursor.NW_RESIZE || currentCursor == Cursor.NE_RESIZE) {
                double newHeight = startHeight - dy;
                if (newHeight >= stage.getMinHeight()) {
                    stage.setY(stageY + dy);
                    stage.setHeight(newHeight);
                }
            }
            e.consume();
        });

        scene.setOnMouseReleased(e -> {
            isResizing = false;
            currentCursor = Cursor.DEFAULT;
            scene.setCursor(null);
        });
    }
}

