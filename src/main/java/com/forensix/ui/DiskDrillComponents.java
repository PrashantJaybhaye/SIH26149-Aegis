package com.forensix.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

/**
 * High-fidelity vector graphics and UI component builders replicating the Disk Drill design system.
 */
public class DiskDrillComponents {

    /**
     * Renders the Forensix Brand Logo (Modern vector shield + digital magnifying disk emblem + clean typography).
     */
    public static Node createBrandLogo() {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);

        Canvas logoCanvas = new Canvas(34, 34);
        GraphicsContext gc = logoCanvas.getGraphicsContext2D();

        // 1. Cyber-Forensic Shield Badge
        LinearGradient shieldGrad = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0078d4")),
                new Stop(1.0, Color.web("#0284c7")));
        gc.setFill(shieldGrad);
        gc.fillRoundRect(2, 2, 30, 30, 8, 8);

        // Subtle shield outline
        gc.setStroke(Color.color(1, 1, 1, 0.45));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(2, 2, 30, 30, 8, 8);

        // 2. Inner Hard Disk Platter Ring
        gc.setStroke(Color.web("#e0f2fe"));
        gc.setLineWidth(2.2);
        gc.strokeOval(8, 8, 18, 18);

        // 3. Central Platter Hub & Forensic Read Arm
        gc.setFill(Color.WHITE);
        gc.fillOval(14, 14, 6, 6);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.strokeLine(17, 17, 23, 11);

        // 4. Glowing Cyan Focus Node
        gc.setFill(Color.web("#38bdf8"));
        gc.fillOval(22, 10, 3.5, 3.5);

        VBox textBox = new VBox(1);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("FORENSIX");
        brand.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-letter-spacing: 0.8px;");

        Label tagline = new Label("DATA RECOVERY & FORENSICS");
        tagline.setStyle("-fx-font-size: 8.5px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.6px;");

        textBox.getChildren().addAll(brand, tagline);
        container.getChildren().addAll(logoCanvas, textBox);
        return container;
    }

    /**
     * Renders the iconic Disk Drill hardware disk illustration with a metallic enclosure and glowing green LED.
     */
    public static Node createHardwareDiskIllustration(double width, double height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Subtle drop shadow / perspective base
        gc.setFill(Color.color(0, 0, 0, 0.08));
        gc.fillRoundRect(14, height - 16, width - 28, 12, 6, 6);

        // Top surface: brushed metal gradient
        LinearGradient topGrad = new LinearGradient(0, 8, 0, height - 26, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f1f5f9")),
                new Stop(0.3, Color.web("#e2e8f0")),
                new Stop(1.0, Color.web("#cbd5e1")));
        gc.setFill(topGrad);
        gc.fillRoundRect(12, 8, width - 24, height - 28, 8, 8);

        // Top subtle outline
        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(12, 8, width - 24, height - 28, 8, 8);

        // Front face: dark charcoal bevel
        LinearGradient frontGrad = new LinearGradient(0, height - 26, 0, height - 12, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#334155")),
                new Stop(1.0, Color.web("#0f172a")));
        gc.setFill(frontGrad);
        gc.fillRoundRect(12, height - 26, width - 24, 18, 6, 6);

        // Glowing Green Status LED on front panel
        gc.setFill(Color.web("#22c55e"));
        gc.fillOval(26, height - 20, 6, 6);
        gc.setFill(Color.color(0.13, 0.77, 0.36, 0.45));
        gc.fillOval(23, height - 23, 12, 12);

        return canvas;
    }

    /**
     * Creates the vibrant Disk Drill category cards (Pictures, Videos, Audio, Documents, Archives, Other).
     */
    public static VBox createCategoryCard(String title, String countText, String iconSymbol, String startHex, String endHex) {
        VBox card = new VBox(8);
        card.setPrefSize(145, 118);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setAlignment(Pos.CENTER_LEFT);

        String baseStyle = String.format(
                "-fx-background-color: linear-gradient(to bottom right, %s, %s); " +
                "-fx-background-radius: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 8, 0, 0, 3); " +
                "-fx-cursor: hand;",
                startHex, endHex);

        card.setStyle(baseStyle);

        Label lblIcon = new Label(iconSymbol);
        lblIcon.setStyle("-fx-font-size: 26px; -fx-text-fill: white;");

        VBox textBox = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label lblCount = new Label(countText);
        lblCount.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255, 255, 255, 0.9);");

        textBox.getChildren().addAll(lblTitle, lblCount);
        card.getChildren().addAll(lblIcon, textBox);

        card.setOnMouseEntered(e -> card.setStyle(baseStyle + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 12, 0, 0, 5); -fx-translate-y: -2px;"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle + " -fx-translate-y: 0px;"));

        return card;
    }

    /**
     * Creates the metallic grey partition cards for the "Woohoo! Lost partitions found..." section (home.png).
     */
    public static VBox createLostPartitionCard(String name, String fsType, String sizeText) {
        VBox card = new VBox(6);
        card.setPrefSize(185, 115);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);

        String baseStyle = "-fx-background-color: linear-gradient(to bottom, #8a99ad, #64748b); " +
                "-fx-background-radius: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 8, 0, 0, 3); " +
                "-fx-cursor: hand;";
        card.setStyle(baseStyle);

        Label icon = new Label("🖴");
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: white;");

        VBox text = new VBox(2);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: white;");

        Label lblFs = new Label(fsType);
        lblFs.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");

        Label lblSize = new Label(sizeText);
        lblSize.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");

        text.getChildren().addAll(lblName, lblFs, lblSize);

        Region baselineBar = new Region();
        baselineBar.setPrefHeight(2);
        baselineBar.setMaxWidth(Double.MAX_VALUE);
        baselineBar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.45); -fx-background-radius: 2;");

        card.getChildren().addAll(icon, text, baselineBar);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #94a3b8, #475569); " +
                "-fx-background-radius: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 12, 0, 0, 5); " +
                "-fx-translate-y: -2px; " +
                "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(baseStyle + " -fx-translate-y: 0px;"));

        return card;
    }

    /**
     * Renders the large golden folder with green circular badge for the Success Screen (success screen.png).
     */
    public static Node createSuccessFolderBadge(double diameter) {
        StackPane badge = new StackPane();
        badge.setPrefSize(diameter, diameter);

        // Outer Green Ring
        Circle ring = new Circle(diameter / 2.0);
        ring.setFill(Color.web("#f0fdf4"));
        ring.setStroke(Color.web("#22c55e"));
        ring.setStrokeWidth(5);

        // Golden 3D Folder Illustration
        Canvas folderCanvas = new Canvas(diameter * 0.65, diameter * 0.55);
        GraphicsContext gc = folderCanvas.getGraphicsContext2D();
        double w = folderCanvas.getWidth();
        double h = folderCanvas.getHeight();

        // Back tab of folder
        gc.setFill(Color.web("#eab308"));
        gc.fillRoundRect(4, 2, w * 0.45, h * 0.35, 6, 6);

        // Back folder body
        gc.setFill(Color.web("#ca8a04"));
        gc.fillRoundRect(4, h * 0.2, w - 8, h * 0.75, 8, 8);

        // Front folder flap with gradient
        LinearGradient flapGrad = new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fde047")),
                new Stop(1.0, Color.web("#eab308")));
        gc.setFill(flapGrad);
        gc.fillRoundRect(0, h * 0.3, w, h * 0.7, 8, 8);

        // Small green checkmark emblem on folder
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
     * Visual 4-Step Process Breadcrumb / Workflow Tracker header.
     */
    public static HBox createWorkflowStepTracker(int currentStep) {
        HBox tracker = new HBox(8);
        tracker.setAlignment(Pos.CENTER_LEFT);
        tracker.setPadding(new Insets(8, 20, 8, 20));
        tracker.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        String[] stepNames = {
                "1. Select Device",
                "2. Scan & Analyze",
                "3. Review Evidence",
                "4. Success & Audit"
        };

        for (int i = 0; i < stepNames.length; i++) {
            int stepNum = i + 1;
            boolean isActive = (stepNum == currentStep);
            boolean isCompleted = (stepNum < currentStep);

            HBox pill = new HBox(6);
            pill.setAlignment(Pos.CENTER);
            pill.setPadding(new Insets(4, 10, 4, 10));

            Label num = new Label(isCompleted ? "✓" : String.valueOf(stepNum));
            num.setStyle(String.format(
                    "-fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 2 6; -fx-background-radius: 10; " +
                    "-fx-background-color: %s; -fx-text-fill: %s;",
                    isActive ? "#0078d4" : (isCompleted ? "#16a34a" : "#e5e7eb"),
                    isActive || isCompleted ? "white" : "#6b7280"));

            Label name = new Label(stepNames[i].substring(3));
            name.setStyle(String.format(
                    "-fx-font-size: 11px; -fx-font-weight: %s; -fx-text-fill: %s;",
                    isActive ? "bold" : "normal",
                    isActive ? "#111827" : (isCompleted ? "#16a34a" : "#6b7280")));

            pill.getChildren().addAll(num, name);
            if (isActive) {
                pill.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 12; -fx-border-color: #bfdbfe; -fx-border-radius: 12;");
            } else {
                pill.setStyle("-fx-background-color: transparent;");
            }

            tracker.getChildren().add(pill);

            if (i < stepNames.length - 1) {
                Label arrow = new Label("›");
                arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
                tracker.getChildren().add(arrow);
            }
        }

        return tracker;
    }

    /**
     * High-performance, real-time hardware oscilloscope canvas for sector entropy and read throughput.
     * Prevents UI stalls and provides continuous visual feedback during flash zero-fill sector reads.
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
        private javafx.animation.AnimationTimer animTimer;

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

            canvas = new Canvas(850, 180);
            widthProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.doubleValue() > 100) {
                    canvas.setWidth(newV.doubleValue() - 32);
                    render();
                }
            });

            getChildren().addAll(header, canvas);
            VBox.setVgrow(canvas, Priority.ALWAYS);

            // 60 FPS smooth rendering loop for carrier wave and radar sweep
            animTimer = new javafx.animation.AnimationTimer() {
                private long lastFrame = 0;
                @Override
                public void handle(long now) {
                    if (now - lastFrame >= 16_000_000L) { // ~60 FPS
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

            // Dark cybernetic monitor background
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

            // Horizontal Grid Lines & Y-Axis Labels
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

            // Vertical Time Division Markers
            int numDivisions = 6;
            for (int i = 0; i <= numDivisions; i++) {
                double x = leftMargin + (plotW * i / numDivisions);
                gc.setStroke(Color.color(0.12, 0.16, 0.23, 0.5));
                gc.strokeLine(x, topMargin, x, topMargin + plotH);
            }

            // Draw Speed Profile Trace (Emerald Green)
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

            // Compute Entropy Waveform Coordinates with Active DMA Carrier Sine Ripple
            int n = entropySamples.length;
            double[] xCoords = new double[n];
            double[] yCoords = new double[n];

            for (int i = 0; i < n; i++) {
                xCoords[i] = leftMargin + (plotW * i / (n - 1));
                double ent = entropySamples[i];
                if (ent <= 0.0001) {
                    // Fluid, living baseline carrier ripple indicating active hardware reading
                    double carrier = Math.sin((i + tickCounter * 0.5) * 0.35) * 3.5;
                    yCoords[i] = (topMargin + plotH) - 3.5 + carrier;
                } else {
                    double norm = Math.min(1.0, ent / 8.0);
                    yCoords[i] = topMargin + plotH * (1.0 - norm);
                }
            }

            // Gradient Fill under Entropy Curve
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

            // Stroke Entropy Line (Neon Cyan Glow)
            gc.setStroke(Color.web("#00e5ff"));
            gc.setLineWidth(2.2);
            gc.beginPath();
            gc.moveTo(xCoords[0], yCoords[0]);
            for (int i = 1; i < n; i++) {
                gc.lineTo(xCoords[i], yCoords[i]);
            }
            gc.stroke();

            // Smooth Moving Radar Sweep Line across plot
            double sweepPos = (tickCounter % 180) / 180.0;
            double sweepX = leftMargin + plotW * sweepPos;
            gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.35));
            gc.setLineWidth(1.2);
            gc.strokeLine(sweepX, topMargin, sweepX, topMargin + plotH);

            // Active Scan Head Indicator Node at Current Head
            double headX = xCoords[n - 1];
            double headY = yCoords[n - 1];
            gc.setFill(Color.web("#00e5ff"));
            gc.fillOval(headX - 4, headY - 4, 8, 8);
            gc.setFill(Color.WHITE);
            gc.fillOval(headX - 2, headY - 2, 4, 4);

            // Bottom Canvas HUD Info Bar
            gc.setFill(Color.web("#0f172a"));
            gc.fillRect(0, h - bottomMargin + 2, w, bottomMargin - 2);

            gc.setFill(Color.web("#94a3b8"));
            gc.fillText(String.format("LIVE DMA STREAM: LBA %,d \u2022 THROUGHPUT: %.1f MB/s \u2022 %s", lastLba, lastSpeed, lastPhase), leftMargin, h - 8);

            String statusEntropy = (lastEntropy <= 0.0001) ?
                    "0.0000 bits/byte [UNALLOCATED ZERO-FILL BLOCKS \u2014 STREAMING ACTIVE]" :
                    String.format("%.4f bits/byte [ACTIVE SECTOR PAYLOAD]", lastEntropy);

            gc.setFill(lastEntropy > 0.0001 ? Color.web("#38bdf8") : Color.web("#38bdf8"));
            gc.fillText("ENTROPY: " + statusEntropy, Math.max(leftMargin + 280, w - 460), h - 8);
        }
    }
}
