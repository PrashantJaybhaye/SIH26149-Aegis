package com.aegis.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.FillRule;
import javafx.stage.Stage;

/**
 * High-fidelity vector graphics, responsive components, and real-time oscilloscopes for AEGIS.
 * All icons and visual cues are rendered via vector geometry on anti-aliased JavaFX Canvases.
 */
public class AegisUiComponents {

    // Precise SVG path definitions for the AEGIS application emblem
    public static final String APP_ICON_PATH_1 = "M 290.1 32.8 L 217.2 105.6 L 290.1 105.6 Z";
    public static final String APP_ICON_PATH_2 = "M 485.4 195.6 L 476.1 190.9 L 380.0 190.9 L 361.4 158.4 L 344.4 139.8 L 333.5 132.0 L 308.7 121.2 L 206.4 119.6 L 203.3 116.5 L 203.3 29.7 L 76.2 159.9 L 59.2 183.2 L 46.8 208.0 L 35.9 251.4 L 34.4 296.3 L 178.5 156.8 L 180.0 158.4 L 178.5 183.2 L 104.1 256.0 L 79.3 294.8 L 65.4 339.7 L 63.8 403.2 L 274.6 198.6 L 294.8 189.4 L 356.8 189.4 L 363.0 192.5 L 367.6 200.2 L 366.0 243.6 L 363.0 245.2 L 322.7 208.0 L 311.8 203.3 L 301.0 203.3 L 282.4 215.7 L 166.1 330.4 L 149.0 353.7 L 136.6 376.9 L 125.8 406.4 L 121.2 428.0 L 119.6 485.4 L 239.0 370.7 L 327.3 370.7 L 333.5 375.4 L 336.6 381.6 L 336.6 386.2 L 328.8 400.2 L 381.6 398.6 L 392.4 392.4 L 400.2 383.1 L 443.6 301.0 L 404.8 313.4 L 299.4 311.8 L 339.7 273.0 L 358.3 265.3 L 420.3 265.3 L 431.2 274.6 L 445.1 297.8 L 488.5 209.5 L 488.5 201.8 Z";

    /**
     * Renders modern window controls (Close, Minimize, Zoom/Maximize).
     */
    public static Node createWindowControls(Stage stage) {
        return createWindowControls(stage, null);
    }

    public static Node createWindowControls(Stage stage, Runnable onMaximizeToggle) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 6, 0, 0));

        final double dotSize = 13.5;

        // 1. Red Close Button
        Node redBtn = createVectorTrafficDot(dotSize, "CLOSE", "#ff5f56", "#e0443e", "#4a0002", () -> {
            Platform.exit();
            System.exit(0);
        });
        Tooltip.install(redBtn, new Tooltip("Close Window"));

        // 2. Yellow Minimize Button
        Node yellowBtn = createVectorTrafficDot(dotSize, "MINIMIZE", "#ffbd2e", "#dea123", "#523600", () -> {
            if (stage != null) stage.setIconified(true);
        });
        Tooltip.install(yellowBtn, new Tooltip("Minimize Window"));

        // 3. Green Zoom/Maximize Button
        Node greenBtn = createVectorTrafficDot(dotSize, "ZOOM", "#27c93f", "#1aab29", "#003800", () -> {
            if (onMaximizeToggle != null) {
                onMaximizeToggle.run();
            } else if (stage != null) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
        Tooltip.install(greenBtn, new Tooltip("Zoom / Maximize"));

        box.getChildren().addAll(redBtn, yellowBtn, greenBtn);
        return box;
    }

    private static Node createVectorTrafficDot(double size, String type, String fillHex, String strokeHex, String glyphHex, Runnable onClick) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        SimpleBooleanProperty isHovered = new SimpleBooleanProperty(false);

        Runnable redraw = () -> {
            gc.clearRect(0, 0, size, size);
            double r = (size - 1.2) / 2.0;
            double cx = size / 2.0;
            double cy = size / 2.0;

            gc.setFill(Color.web(fillHex));
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);

            gc.setStroke(Color.web(strokeHex));
            gc.setLineWidth(0.85);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

            double opacity = isHovered.get() ? 1.0 : 0.55;
            gc.setStroke(Color.web(glyphHex, opacity));
            gc.setFill(Color.web(glyphHex, opacity));

            switch (type) {
                case "CLOSE" -> {
                    gc.setLineWidth(1.35);
                    double d = 2.8;
                    gc.strokeLine(cx - d, cy - d, cx + d, cy + d);
                    gc.strokeLine(cx + d, cy - d, cx - d, cy + d);
                }
                case "MINIMIZE" -> {
                    gc.setLineWidth(1.45);
                    gc.strokeLine(cx - 3.2, cy, cx + 3.2, cy);
                }
                case "ZOOM" -> {
                    gc.setLineWidth(1.35);
                    gc.strokeLine(cx - 2.8, cy, cx + 2.8, cy);
                    gc.strokeLine(cx, cy - 2.8, cx, cy + 2.8);
                }
            }
        };

        redraw.run();

        canvas.setCursor(javafx.scene.Cursor.HAND);
        canvas.setOnMouseEntered(e -> {
            isHovered.set(true);
            redraw.run();
        });
        canvas.setOnMouseExited(e -> {
            isHovered.set(false);
            redraw.run();
        });
        canvas.setOnMouseClicked(e -> {
            if (onClick != null) onClick.run();
        });

        return canvas;
    }

    /**
     * Renders a modern sidebar toggle button with rounded hover styling.
     */
    public static Button createSidebarToggle(Runnable onToggle) {
        Button btn = new Button();
        btn.setPrefSize(28, 28);
        btn.setMinSize(28, 28);
        btn.setMaxSize(28, 28);
        btn.setCursor(javafx.scene.Cursor.HAND);

        Canvas canvas = new Canvas(16, 14);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Outer window frame
        gc.setStroke(Color.web("#52525b"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(0.6, 0.6, 14.8, 12.8, 3, 3);

        // Sidebar column divider
        gc.strokeLine(5.5, 0.6, 5.5, 13.4);

        // Sidebar panel tint
        gc.setFill(Color.web("#71717a", 0.35));
        gc.fillRoundRect(0.6, 0.6, 4.9, 12.8, 2, 2);

        btn.setGraphic(canvas);
        String normalStyle = "-fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 0;";
        String hoverStyle = "-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 6px; -fx-padding: 0;";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        btn.setOnAction(e -> {
            if (onToggle != null) onToggle.run();
        });

        return btn;
    }

    /**
     * Renders the AEGIS Brand Emblem and Typography.
     */
    public static Node createBrandLogo() {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        Canvas logoCanvas = new Canvas(24, 24);
        GraphicsContext gc = logoCanvas.getGraphicsContext2D();

        double iconTargetSize = 22.0;
        double scale = iconTargetSize / 512.0;
        gc.save();
        gc.translate(1.0, 1.0);
        gc.scale(scale, scale);
        gc.setFill(Color.web("#0071e3"));
        gc.setFillRule(FillRule.EVEN_ODD);
        gc.beginPath();
        gc.appendSVGPath(APP_ICON_PATH_1);
        gc.appendSVGPath(APP_ICON_PATH_2);
        gc.fill();
        gc.restore();

        HBox textAndBadge = new HBox(6);
        textAndBadge.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("AEGIS");
        brand.setStyle("-fx-font-family: -apple-system, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1d1d1f; -fx-letter-spacing: 0.5px;");

        Label badge = new Label("PRO");
        badge.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #86868b; " +
                "-fx-background-color: #f2f2f7; -fx-background-radius: 999px; -fx-padding: 1.5 6 1.5 6;");

        textAndBadge.getChildren().addAll(brand, badge);
        container.getChildren().addAll(logoCanvas, textAndBadge);
        return container;
    }

    /**
     * Generates a high-DPI rasterized Application Icon for Stage and taskbar.
     */
    public static Image createAppIconImage(int size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double radius = size * 0.23;
        LinearGradient bgGrad = new LinearGradient(0, 0, 0, size, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#007aff")),
                new Stop(1.0, Color.web("#0051ba")));
        gc.setFill(bgGrad);
        gc.fillRoundRect(0, 0, size, size, radius, radius);

        gc.setStroke(Color.color(1, 1, 1, 0.35));
        gc.setLineWidth(Math.max(1.0, size * 0.03));
        gc.strokeRoundRect(0.5, 0.5, size - 1.0, size - 1.0, radius, radius);

        double emblemSize = size * 0.62;
        double offset = (size - emblemSize) / 2.0;
        double scale = emblemSize / 512.0;

        gc.save();
        gc.translate(offset, offset);
        gc.scale(scale, scale);
        gc.setFill(Color.WHITE);
        gc.setFillRule(FillRule.EVEN_ODD);
        gc.beginPath();
        gc.appendSVGPath(APP_ICON_PATH_1);
        gc.appendSVGPath(APP_ICON_PATH_2);
        gc.fill();
        gc.restore();

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    public static Node createVectorBookIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#52525b"));
        gc.setLineWidth(1.4);
        gc.strokeArc(1.5, 2.5, size * 0.45, size * 0.7, 45, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(size * 0.5, 2.5, size * 0.45, size * 0.7, 15, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeLine(size * 0.5, 3.5, size * 0.5, size - 2.5);
        gc.strokeLine(1.5, size - 2.5, size * 0.5, size - 1.5);
        gc.strokeLine(size * 0.5, size - 1.5, size - 1.5, size - 2.5);
        return c;
    }

    public static Node createVectorLightningIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#0071e3"));
        double[] x = {size * 0.55, size * 0.2, size * 0.5, size * 0.4, size * 0.8, size * 0.45};
        double[] y = {1, size * 0.55, size * 0.55, size - 1, size * 0.4, size * 0.4};
        gc.fillPolygon(x, y, 6);
        return c;
    }

    public static Node createVectorHardDriveIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#f1f5f9"));
        gc.fillRoundRect(1.5, 3, size - 3, size - 6, 3, 3);
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.3);
        gc.strokeRoundRect(1.5, 3, size - 3, size - 6, 3, 3);
        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.0);
        gc.strokeLine(4, size - 6, size - 4, size - 6);
        gc.setFill(Color.web("#0071e3"));
        gc.fillOval(size - 6.5, 5, 2.8, 2.8);
        return c;
    }

    public static Node createVectorMagnifierIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#0071e3"));
        gc.setLineWidth(1.6);
        gc.strokeOval(2, 2, size * 0.58, size * 0.58);
        gc.setFill(Color.web("#e0f2fe", 0.6));
        gc.fillOval(2.8, 2.8, size * 0.58 - 1.6, size * 0.58 - 1.6);
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(2.2);
        gc.strokeLine(size * 0.58, size * 0.58, size - 2, size - 2);
        return c;
    }

    public static Node createHeaderShield(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.3);
        gc.beginPath();
        gc.moveTo(size * 0.5, 1.2);
        gc.lineTo(size - 2.0, 3.2);
        gc.lineTo(size - 2.0, size * 0.55);
        gc.quadraticCurveTo(size - 2.0, size - 1.5, size * 0.5, size - 0.8);
        gc.quadraticCurveTo(2.0, size - 1.5, 2.0, size * 0.55);
        gc.lineTo(2.0, 3.2);
        gc.closePath();
        gc.stroke();

        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.2);
        gc.strokeLine(size * 0.30, size * 0.52, size * 0.44, size * 0.68);
        gc.strokeLine(size * 0.44, size * 0.68, size * 0.72, size * 0.36);
        return c;
    }

    public static Node createSystemLockBadge(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillOval(1.0, 1.0, size - 2.0, size - 2.0);

        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.3);
        gc.strokeArc(size * 0.32, size * 0.22, size * 0.36, size * 0.32, 0, 180, javafx.scene.shape.ArcType.OPEN);

        gc.setFill(Color.web("#475569"));
        gc.fillRoundRect(size * 0.26, size * 0.44, size * 0.48, size * 0.36, 1.5, 1.5);
        return c;
    }

    public static Node createVaultBadge(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.1);
        gc.beginPath();
        gc.moveTo(size * 0.5, 1.2);
        gc.lineTo(size - 2.0, 3.0);
        gc.lineTo(size - 2.0, size * 0.55);
        gc.quadraticCurveTo(size - 2.0, size - 1.5, size * 0.5, size - 0.8);
        gc.quadraticCurveTo(2.0, size - 1.5, 2.0, size * 0.55);
        gc.lineTo(2.0, 3.0);
        gc.closePath();
        gc.stroke();

        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.1);
        gc.strokeLine(size * 0.30, size * 0.52, size * 0.44, size * 0.68);
        gc.strokeLine(size * 0.44, size * 0.68, size * 0.72, size * 0.36);
        return c;
    }

    public static Node createVectorShieldIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#f0fdf4"));
        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.4);
        gc.beginPath();
        gc.moveTo(size * 0.5, 1.5);
        gc.lineTo(size - 2.5, 3.5);
        gc.lineTo(size - 2.5, size * 0.55);
        gc.quadraticCurveTo(size - 2.5, size - 2, size * 0.5, size - 1);
        gc.quadraticCurveTo(2.5, size - 2, 2.5, size * 0.55);
        gc.lineTo(2.5, 3.5);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setStroke(Color.web("#16a34a"));
        gc.setLineWidth(1.4);
        gc.strokeLine(size * 0.32, size * 0.48, size * 0.46, size * 0.63);
        gc.strokeLine(size * 0.46, size * 0.63, size * 0.70, size * 0.35);
        return c;
    }

    public static Node createVectorEraserIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.4);
        gc.strokeOval(2, size - 6, 4.5, 4.5);
        gc.strokeOval(size - 6.5, size - 6, 4.5, 4.5);
        gc.strokeLine(5, size - 4, size - 3, 3);
        gc.strokeLine(size - 5, size - 4, 3, 3);
        return c;
    }

    public static Node createVectorCertificateIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#fffbeb"));
        gc.fillRoundRect(2, 1.5, size - 4, size - 3, 2, 2);
        gc.setStroke(Color.web("#d97706"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(2, 1.5, size - 4, size - 3, 2, 2);
        gc.setStroke(Color.web("#b45309"));
        gc.setLineWidth(1.0);
        gc.strokeLine(4.5, 5, size - 4.5, 5);
        gc.strokeLine(4.5, 8, size - 4.5, 8);
        gc.strokeLine(4.5, 11, size * 0.65, 11);
        gc.setFill(Color.web("#f59e0b"));
        gc.fillOval(size - 6.5, size - 6.5, 4.5, 4.5);
        return c;
    }

    public static Node createVectorBlockchainIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#0284c7"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(1.5, 4.5, size * 0.45, size * 0.5, 3, 3);
        gc.strokeRoundRect(size * 0.5, 4.5, size * 0.45, size * 0.5, 3, 3);
        gc.setStroke(Color.web("#38bdf8"));
        gc.strokeLine(size * 0.35, size * 0.5, size * 0.65, size * 0.5);
        return c;
    }

    public static Button createVectorChevronButton(boolean isDown, Runnable onToggle) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        final double size = 12.0;

        Node normalIcon = drawChevronCanvas(isDown, size, Color.web("#64748b"));
        Node hoverIcon = drawChevronCanvas(isDown, size, Color.web("#0071e3"));

        btn.setGraphic(normalIcon);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 4; -fx-cursor: hand; -fx-background-radius: 4;");

        btn.setOnMouseEntered(e -> {
            btn.setGraphic(hoverIcon);
            btn.setStyle("-fx-background-color: rgba(0, 113, 227, 0.10); -fx-padding: 3 4; -fx-cursor: hand; -fx-background-radius: 4;");
        });
        btn.setOnMouseExited(e -> {
            btn.setGraphic(normalIcon);
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 4; -fx-cursor: hand; -fx-background-radius: 4;");
        });

        if (onToggle != null) {
            btn.setOnAction(e -> {
                e.consume();
                onToggle.run();
            });
        }
        return btn;
    }

    private static Canvas drawChevronCanvas(boolean isDown, double size, Color strokeColor) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(strokeColor);
        gc.setLineWidth(1.65);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        if (isDown) {
            double x1 = size * 0.25;
            double y1 = size * 0.38;
            double x2 = size * 0.50;
            double y2 = size * 0.64;
            double x3 = size * 0.75;
            double y3 = size * 0.38;
            gc.strokeLine(x1, y1, x2, y2);
            gc.strokeLine(x2, y2, x3, y3);
        } else {
            double x1 = size * 0.38;
            double y1 = size * 0.25;
            double x2 = size * 0.64;
            double y2 = size * 0.50;
            double x3 = size * 0.38;
            double y3 = size * 0.75;
            gc.strokeLine(x1, y1, x2, y2);
            gc.strokeLine(x2, y2, x3, y3);
        }
        return canvas;
    }

    public static Node createVectorHardwareDiskRowIcon() {
        Canvas c = new Canvas(24, 14);
        GraphicsContext gc = c.getGraphicsContext2D();
        LinearGradient g = new LinearGradient(0, 0, 0, 14, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#475569")),
                new Stop(1, Color.web("#1e293b")));
        gc.setFill(g);
        gc.fillRoundRect(0.5, 1.5, 23, 11, 2.5, 2.5);

        gc.setStroke(Color.web("#64748b"));
        gc.setLineWidth(0.7);
        gc.strokeRoundRect(0.5, 1.5, 23, 11, 2.5, 2.5);

        gc.setFill(Color.web("#22c55e"));
        gc.fillOval(3.5, 5, 4, 4);
        gc.setFill(Color.color(0.13, 0.77, 0.36, 0.4));
        gc.fillOval(2, 3.5, 7, 7);
        return c;
    }

    public static Node createVectorWindowsLogoIcon() {
        Canvas c = new Canvas(15, 15);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#0078d4"));
        double gap = 1.8;
        double s = 5.8;
        gc.fillRect(0.8, 0.8, s, s);
        gc.fillRect(0.8 + s + gap, 0.8, s, s);
        gc.fillRect(0.8, 0.8 + s + gap, s, s);
        gc.fillRect(0.8 + s + gap, 0.8 + s + gap, s, s);
        return c;
    }

    public static Node createVectorFolderIcon(double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.web("#f59e0b"));
        gc.fillRoundRect(1.5, 2, size * 0.45, 4, 1.5, 1.5);
        gc.setFill(Color.web("#fbbf24"));
        gc.fillRoundRect(1.5, 4.5, size - 3, size - 6, 2, 2);
        gc.setStroke(Color.web("#d97706"));
        gc.setLineWidth(0.8);
        gc.strokeRoundRect(1.5, 4.5, size - 3, size - 6, 2, 2);
        return c;
    }

    public static Node createVectorCategoryIcon(String cat, double size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();
        switch (cat) {
            case "PICTURES" -> {
                gc.setFill(Color.web("#0284c7"));
                gc.fillRoundRect(1.5, 1.5, size - 3, size - 3, 2, 2);
                gc.setFill(Color.web("#38bdf8"));
                gc.fillOval(size * 0.65, size * 0.25, size * 0.2, size * 0.2);
                gc.setFill(Color.WHITE);
                double[] px = {1.5, size * 0.4, size * 0.65, size - 1.5, size - 1.5, 1.5};
                double[] py = {size - 1.5, size * 0.55, size * 0.7, size * 0.45, size - 1.5, size - 1.5};
                gc.fillPolygon(px, py, 6);
            }
            case "VIDEOS" -> {
                gc.setFill(Color.web("#ea580c"));
                gc.fillRoundRect(1.5, 1.5, size - 3, size - 3, 2, 2);
                gc.setFill(Color.WHITE);
                double[] vx = {size * 0.35, size * 0.35, size * 0.75};
                double[] vy = {size * 0.25, size * 0.75, size * 0.5};
                gc.fillPolygon(vx, vy, 3);
            }
            case "AUDIO" -> {
                gc.setFill(Color.web("#0d9488"));
                gc.fillRoundRect(1.5, 1.5, size - 3, size - 3, 2, 2);
                gc.setFill(Color.WHITE);
                gc.fillOval(size * 0.25, size * 0.6, size * 0.25, size * 0.25);
                gc.fillOval(size * 0.6, size * 0.5, size * 0.25, size * 0.25);
                gc.fillRect(size * 0.43, size * 0.25, 2, size * 0.45);
                gc.fillRect(size * 0.78, size * 0.2, 2, size * 0.45);
                gc.fillRect(size * 0.43, size * 0.2, size * 0.37, 2.5);
            }
            case "DOCUMENTS" -> {
                gc.setFill(Color.web("#e11d48"));
                gc.fillRoundRect(2, 1.5, size - 4, size - 3, 2, 2);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.2);
                gc.strokeLine(4.5, size * 0.4, size - 4.5, size * 0.4);
                gc.strokeLine(4.5, size * 0.6, size - 4.5, size * 0.6);
                gc.strokeLine(4.5, size * 0.8, size * 0.65, size * 0.8);
            }
            case "ARCHIVES" -> {
                gc.setFill(Color.web("#9333ea"));
                gc.fillRoundRect(1.5, 1.5, size - 3, size - 3, 2, 2);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.2);
                gc.strokeLine(size * 0.5, 1.5, size * 0.5, size - 1.5);
                gc.setFill(Color.web("#fef08a"));
                gc.fillRect(size * 0.35, size * 0.45, size * 0.3, size * 0.25);
            }
            default -> {
                gc.setFill(Color.web("#64748b"));
                gc.fillOval(2, 2, size - 4, size - 4);
                gc.setFill(Color.WHITE);
                gc.fillOval(size * 0.35, size * 0.35, size * 0.3, size * 0.3);
            }
        }
        return c;
    }

    public static Node createDriveIllustration(double width, double height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.color(0, 0, 0, 0.08));
        gc.fillRoundRect(14, height - 16, width - 28, 12, 6, 6);

        LinearGradient topGrad = new LinearGradient(0, 8, 0, height - 26, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f1f5f9")),
                new Stop(0.3, Color.web("#e2e8f0")),
                new Stop(1.0, Color.web("#cbd5e1")));
        gc.setFill(topGrad);
        gc.fillRoundRect(12, 8, width - 24, height - 28, 8, 8);

        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(12, 8, width - 24, height - 28, 8, 8);

        LinearGradient frontGrad = new LinearGradient(0, height - 26, 0, height - 12, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#334155")),
                new Stop(1.0, Color.web("#0f172a")));
        gc.setFill(frontGrad);
        gc.fillRoundRect(12, height - 26, width - 24, 18, 6, 6);

        gc.setFill(Color.web("#22c55e"));
        gc.fillOval(26, height - 20, 6, 6);
        gc.setFill(Color.color(0.13, 0.77, 0.36, 0.45));
        gc.fillOval(23, height - 23, 12, 12);

        return canvas;
    }

    public static VBox createCategoryCard(String title, String countText, String iconSymbol, String startHex, String endHex) {
        VBox card = new VBox(6);
        card.setPrefHeight(84);
        card.setMinHeight(76);
        card.setMaxHeight(92);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER_LEFT);

        String baseStyle = String.format(
                "-fx-background-color: linear-gradient(to bottom right, %s, %s); " +
                "-fx-background-radius: 10px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 6, 0, 0, 2); " +
                "-fx-cursor: hand;",
                startHex, endHex);

        card.setStyle(baseStyle);

        Label lblIcon = new Label(iconSymbol);
        lblIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

        VBox textBox = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label lblCount = new Label(countText);
        lblCount.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255, 255, 255, 0.9);");

        textBox.getChildren().addAll(lblTitle, lblCount);
        card.getChildren().addAll(lblIcon, textBox);

        card.setOnMouseEntered(e -> card.setStyle(baseStyle + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 10, 0, 0, 4); -fx-translate-y: -2px;"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle + " -fx-translate-y: 0px;"));

        return card;
    }

    public static Node createSuccessFolderBadge(double diameter) {
        StackPane badge = new StackPane();
        badge.setPrefSize(diameter, diameter);

        Circle ring = new Circle(diameter / 2.0);
        ring.setFill(Color.web("#f0fdf4"));
        ring.setStroke(Color.web("#22c55e"));
        ring.setStrokeWidth(5);

        Canvas folderCanvas = new Canvas(diameter * 0.65, diameter * 0.55);
        GraphicsContext gc = folderCanvas.getGraphicsContext2D();
        double w = folderCanvas.getWidth();
        double h = folderCanvas.getHeight();

        gc.setFill(Color.web("#eab308"));
        gc.fillRoundRect(4, 2, w * 0.45, h * 0.35, 6, 6);

        gc.setFill(Color.web("#ca8a04"));
        gc.fillRoundRect(4, h * 0.2, w - 8, h * 0.75, 8, 8);

        LinearGradient flapGrad = new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fde047")),
                new Stop(1.0, Color.web("#eab308")));
        gc.setFill(flapGrad);
        gc.fillRoundRect(0, h * 0.3, w, h * 0.7, 8, 8);

        gc.setFill(Color.web("#16a34a"));
        gc.fillOval(w - 20, h - 20, 18, 18);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.5);
        gc.strokeLine(w - 16, h - 11, w - 12, h - 7);
        gc.strokeLine(w - 12, h - 7, w - 6, h - 15);

        badge.getChildren().addAll(ring, folderCanvas);
        return badge;
    }

    /**
     * High-performance, real-time hardware oscilloscope canvas for sector entropy and read throughput.
     */
    public static class ForensicWaveformPane extends VBox {
        private final Canvas canvas;
        private final Label lblThroughputBadge;
        private final Label lblStatusDetail;
        private final double[] entropySamples = new double[100];
        private final double[] speedSamples = new double[100];
        private int sampleCount = 0;
        private double lastEntropy = 0.0;
        private double lastSpeed = 0.0;
        private long lastLba = 0;
        private String lastPhase = "Initializing Sector Stream";
        private long tickCounter = 0;
        private final javafx.animation.AnimationTimer animTimer;

        public ForensicWaveformPane() {
            super(8);
            setPadding(new Insets(10, 14, 10, 14));
            setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label("REAL-TIME HARDWARE SECTOR STREAM & CARVING RADAR");
            title.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

            HBox legend = new HBox(12);
            legend.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().addAll(
                    createLegendItem("\u25CF Entropy (0\u20138 bits/byte)", "#00e5ff"),
                    createLegendItem("\u25CF Read Speed (MB/s)", "#10b981"),
                    createLegendItem("\u25CF Hardware DMA Carrier", "#f59e0b")
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            lblStatusDetail = new Label("Hardware Stream Active");
            lblStatusDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

            lblThroughputBadge = new Label("\u25CF DMA BUS: 0.0 MB/s");
            lblThroughputBadge.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #15803d; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 12; -fx-border-color: #bbf7d0; -fx-border-radius: 12;");

            header.getChildren().addAll(title, legend, spacer, lblStatusDetail, lblThroughputBadge);

            canvas = new Canvas(850, 120);
            widthProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.doubleValue() > 100) {
                    canvas.setWidth(newV.doubleValue() - 32);
                    render();
                }
            });
            heightProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.doubleValue() > 60) {
                    canvas.setHeight(Math.max(90, Math.min(180, newV.doubleValue() - 42)));
                    render();
                }
            });

            getChildren().addAll(header, canvas);
            VBox.setVgrow(canvas, Priority.ALWAYS);

            animTimer = new javafx.animation.AnimationTimer() {
                private long lastFrame = 0;
                @Override
                public void handle(long now) {
                    if (now - lastFrame >= 16_000_000L) {
                        tickCounter++;
                        render();
                        lastFrame = now;
                    }
                }
            };
            animTimer.start();
            render();
        }

        private static Node createLegendItem(String text, String colorHex) {
            Label lbl = new Label(text);
            lbl.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s;", colorHex));
            return lbl;
        }

        public void pushSample(double entropy, double speedMBs, long lba, String phase) {
            System.arraycopy(entropySamples, 1, entropySamples, 0, entropySamples.length - 1);
            System.arraycopy(speedSamples, 1, speedSamples, 0, speedSamples.length - 1);

            entropySamples[entropySamples.length - 1] = Math.max(0.0, Math.min(8.0, entropy));
            speedSamples[speedSamples.length - 1] = Math.max(0.0, speedMBs);

            if (sampleCount < entropySamples.length) sampleCount++;
            lastEntropy = entropy;
            lastSpeed = speedMBs;
            lastLba = lba;
            if (phase != null) lastPhase = phase;

            lblThroughputBadge.setText(String.format("\u25CF DMA BUS ACTIVE: %.1f MB/s", speedMBs));
            if (entropy <= 0.0001) {
                lblStatusDetail.setText("Streaming Unallocated Flash Blocks (Zero-Fill)");
                lblStatusDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #0284c7; -fx-font-weight: bold;");
            } else {
                lblStatusDetail.setText("Active Forensic Signature Payload Detected");
                lblStatusDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        }

        public void reset() {
            java.util.Arrays.fill(entropySamples, 0.0);
            java.util.Arrays.fill(speedSamples, 0.0);
            sampleCount = 0;
            lastEntropy = 0.0;
            lastSpeed = 0.0;
            lastLba = 0;
            tickCounter = 0;
            render();
        }

        public void stopAnimation() {
            if (animTimer != null) animTimer.stop();
        }

        public void startAnimation() {
            if (animTimer != null) animTimer.start();
        }

        private void render() {
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            if (w < 50 || h < 50) return;

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);

            gc.setFill(Color.web("#0b1329"));
            gc.fillRoundRect(0, 0, w, h, 6, 6);
            gc.setStroke(Color.web("#1e293b"));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(0, 0, w, h, 6, 6);

            double leftMargin = 68;
            double rightMargin = 20;
            double topMargin = 20;
            double bottomMargin = 26;
            double plotW = w - leftMargin - rightMargin;
            double plotH = h - topMargin - bottomMargin;

            gc.setLineWidth(0.8);
            String[] yLabels = {
                    "8.0 Encrypted",
                    "6.0 Media",
                    "4.0 Doc/Data",
                    "2.0 Tables",
                    "0.0 Zero-Fill"
            };

            for (int i = 0; i <= 4; i++) {
                double y = topMargin + plotH * (i / 4.0);
                gc.setStroke(Color.web("#1e293b"));
                gc.strokeLine(leftMargin, y, leftMargin + plotW, y);

                gc.setFill(Color.web("#64748b"));
                gc.fillText(yLabels[i], 4, y + 4);
            }

            int numDivisions = 6;
            for (int i = 0; i <= numDivisions; i++) {
                double x = leftMargin + (plotW * i / numDivisions);
                gc.setStroke(Color.color(0.12, 0.16, 0.23, 0.5));
                gc.strokeLine(x, topMargin, x, topMargin + plotH);
            }

            double maxSpeedScale = 40.0;
            for (double s : speedSamples) {
                if (s > maxSpeedScale) maxSpeedScale = s * 1.2;
            }

            gc.setStroke(Color.color(0.06, 0.73, 0.51, 0.65));
            gc.setLineWidth(1.4);
            for (int i = 0; i < entropySamples.length - 1; i++) {
                double x1 = leftMargin + (plotW * i / (entropySamples.length - 1));
                double y1 = topMargin + plotH * (1.0 - Math.min(1.0, speedSamples[i] / maxSpeedScale) * 0.85);
                double x2 = leftMargin + (plotW * (i + 1) / (entropySamples.length - 1));
                double y2 = topMargin + plotH * (1.0 - Math.min(1.0, speedSamples[i + 1] / maxSpeedScale) * 0.85);
                gc.strokeLine(x1, y1, x2, y2);
            }

            int n = entropySamples.length;
            double[] xCoords = new double[n];
            double[] yCoords = new double[n];

            for (int i = 0; i < n; i++) {
                xCoords[i] = leftMargin + (plotW * i / (n - 1));
                double ent = entropySamples[i];
                if (ent <= 0.0001) {
                    double carrier = Math.sin((i + tickCounter * 0.5) * 0.35) * 3.5;
                    yCoords[i] = (topMargin + plotH) - 3.5 + carrier;
                } else {
                    double norm = Math.min(1.0, ent / 8.0);
                    yCoords[i] = topMargin + plotH * (1.0 - norm);
                }
            }

            gc.beginPath();
            gc.moveTo(xCoords[0], topMargin + plotH);
            for (int i = 0; i < n; i++) {
                gc.lineTo(xCoords[i], yCoords[i]);
            }
            gc.lineTo(xCoords[n - 1], topMargin + plotH);
            gc.closePath();

            LinearGradient fillGrad = new LinearGradient(0, topMargin, 0, topMargin + plotH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.0, 0.9, 1.0, 0.35)),
                    new Stop(0.8, Color.color(0.0, 0.9, 1.0, 0.08)),
                    new Stop(1.0, Color.color(0.0, 0.9, 1.0, 0.0)));
            gc.setFill(fillGrad);
            gc.fill();

            gc.setStroke(Color.web("#00e5ff"));
            gc.setLineWidth(2.2);
            gc.beginPath();
            gc.moveTo(xCoords[0], yCoords[0]);
            for (int i = 1; i < n; i++) {
                gc.lineTo(xCoords[i], yCoords[i]);
            }
            gc.stroke();

            double sweepPos = (tickCounter % 180) / 180.0;
            double sweepX = leftMargin + plotW * sweepPos;
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.35));
            gc.setLineWidth(1.2);
            gc.strokeLine(sweepX, topMargin, sweepX, topMargin + plotH);

            double headX = xCoords[n - 1];
            double headY = yCoords[n - 1];
            gc.setFill(Color.web("#00e5ff"));
            gc.fillOval(headX - 4, headY - 4, 8, 8);
            gc.setFill(Color.WHITE);
            gc.fillOval(headX - 2, headY - 2, 4, 4);

            gc.setFill(Color.web("#0f172a"));
            gc.fillRect(0, h - bottomMargin + 2, w, bottomMargin - 2);

            gc.setFill(Color.web("#94a3b8"));
            gc.fillText(String.format("LIVE DMA STREAM: LBA %,d \u2022 THROUGHPUT: %.1f MB/s \u2022 %s", lastLba, lastSpeed, lastPhase), leftMargin, h - 8);

            String statusEntropy = (lastEntropy <= 0.0001) ?
                    "0.0000 bits/byte [UNALLOCATED ZERO-FILL BLOCKS \u2014 STREAMING ACTIVE]" :
                    String.format("%.4f bits/byte [ACTIVE SECTOR PAYLOAD]", lastEntropy);

            gc.setFill(Color.web("#38bdf8"));
            gc.fillText("ENTROPY: " + statusEntropy, Math.max(leftMargin + 280, w - 460), h - 8);
        }
    }
}

