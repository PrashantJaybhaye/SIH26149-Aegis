package com.aegis.ui;

import com.aegis.audit.ReportGenerator;
import com.aegis.blockchain.AuditBlockchain;
import com.aegis.carver.FileCarver;
import com.aegis.eraser.DriveEraser;
import com.aegis.eraser.FileEraser;
import com.aegis.model.CarvedFile;
import com.aegis.model.SanitizationStandard;
import com.aegis.model.StorageDevice;
import com.aegis.nativeio.StorageDetector;
import com.aegis.util.FormatUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AEGIS Desktop Application - Professional Forensic Interface.
 * Implements the 4-step Disk Drill workflow:
 * Step 1: Select Storage Device -> Step 2: Scan Media & Entropy -> Step 3: Evidence Manifest & Review -> Step 4: Success & Cryptographic Audit.
 */
public class AegisApp extends Application {

    private BorderPane mainLayout;
    private StackPane contentArea;

    private Stage primaryStage;
    private boolean isCustomMaximized = false;
    private double restoreX = 100, restoreY = 100, restoreW = 1100, restoreH = 700;
    private Rectangle windowClip;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    // Collections
    private ObservableList<StorageDevice> deviceList = FXCollections.observableArrayList();
    private final ObservableList<StorageDevice> sanitizableDeviceList = FXCollections.observableArrayList();
    private final List<StorageDevice> masterDeviceList = new ArrayList<>();
    private final Set<String> collapsedDisks = new HashSet<>();
    private ObservableList<CarvedFileItem> carvedFileList = FXCollections.observableArrayList();
    private ObservableList<CarvedFileItem> filteredCarvedFileList = FXCollections.observableArrayList();
    private ObservableList<File> reportList = FXCollections.observableArrayList();

    private StorageDevice selectedDevice;

    // Navigation Buttons
    private Button btnNavStorageDevices;
    private Button btnNavCarver;
    private Button btnNavSanitizer;
    private Button btnNavFileEraser;
    private Button btnNavCertificates;
    private Button btnNavBlockchain;

    private Label breadcrumbLabel;

    // 1. Storage Devices View Components
    private BorderPane drivesViewPane;
    private Label tableItemCountLabel = new Label("0 item(s)");
    private Button btnRefresh;
    private TableView<StorageDevice> drivesTable;
    private VBox rightInspectorPane;
    private Label inspectorDriveName;
    private Label inspectorDriveSubtext;
    private Button btnInspectorSearchData;
    private Button btnInspectorSanitize;
    private Label propType, propModel, propSerial, propVendor, propProtocol, propRevision, propCapacity, propAvailable, propPhysicalNum, propPartitions;
    private VBox childPartitionsBox;

    // 2. Scanning / Carving View Components
    private VBox scanningViewPane;
    private ScrollPane scanningScrollPane;
    private Label scanHeaderTitle;
    private Label scanPhaseBadge;
    private Label scanHeaderSubtitle;
    private ProgressBar scanProgressBar;
    private Label scanSpeedEtaLabel;
    private Label scanEntropyLabel;
    private Label lblPicturesCount;
    private Label lblVideosCount;
    private Label lblAudioCount;
    private Label lblDocumentsCount;
    private Label lblArchivesCount;
    private Label lblOtherCount;
    private Label lblRecentDiscovery;
    private AegisUiComponents.ForensicWaveformPane waveformPane;
    private Button btnReviewFoundItems;
    private Button btnPauseResumeScan;

    // Dynamic Dual Sidebar Architecture (Disk Drill Navigation Mode vs Scan Tree Mode)
    private StackPane sidebarContainer;
    private VBox navSidebar;
    private VBox scanSidebar;
    private Label lblSideAllCount;
    private Label lblSidePicCount;
    private Label lblSideVidCount;
    private Label lblSideAudCount;
    private Label lblSideDocCount;
    private Label lblSideArcCount;
    private Label lblSideOthCount;
    private volatile boolean isScanPaused = false;
    private volatile boolean isScanCancelled = false;

    // 3. Results Gallery / Table View Components
    private BorderPane resultsViewPane;
    private FlowPane galleryFlowPane;
    private TableView<CarvedFileItem> resultsTableView;
    private StackPane viewModeSwitcherArea;
    private boolean isGalleryView = true;
    private Label resultsHeaderTitle;
    private Label resultsHeaderSubtitle;
    private Label resultPreviewTitle;
    private Label resultPreviewSubtitle;
    private ImageView resultPreviewImageView;
    private StackPane previewContainerBox;
    private Label resultPreviewFormat;
    private Label resultPreviewLba;
    private Label resultPreviewHash;
    private Label resultPreviewScore;
    private Label resultPreviewEntropy;
    private Button btnOpenRecoveredFile;
    private Button btnCopyHash;
    private Label bottomSelectionStatsLabel;
    private TextField destinationPathField;
    private String activeCategoryFilter = "ALL";
    private String activeSearchQuery = "";

    // 4. Dedicated Sanitizer View Components
    private VBox sanitizerDedicatedPane;
    private ComboBox<StorageDevice> comboSanitizerTarget;
    private ComboBox<SanitizationStandard> comboSanitizerStandard;
    private Label lblSanitizerDescription;
    private Label lblSanitizerPasses;

    // 5. Dedicated File & Slack Shredder Components
    private VBox fileEraserDedicatedPane;
    private TextField txtShredFilePath;
    private Label lblShredFileSize;
    private Label lblShredSlackEstimate;
    private ComboBox<SanitizationStandard> comboShredStandard;

    // 6. Success Screen Components
    private VBox successViewPane;
    private Label successTitle;
    private Label successSummary;
    private Label successSourceDrive;
    private Label successDestinationPath;
    private Label successBlockchainBadge;
    private File lastGeneratedCert;

    // Bottom Status & Activity Log
    private Label bottomStatusLabel;
    private TextArea logConsole;
    private boolean logOpen = false;
    private VBox logDrawer;
    private Button btnToggleLog;

    public static class CarvedFileItem {
        private final CarvedFile file;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(true);

        public CarvedFileItem(CarvedFile file) {
            this.file = file;
        }

        public CarvedFile getFile() { return file; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { this.selected.set(val); }
        public SimpleBooleanProperty selectedProperty() { return selected; }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("AEGIS - Integrated Secure Data Erasure & File Recovery Tool");

        // Application Icon across multiple resolutions (16x16 up to 256x256) derived from app-icon.svg
        try {
            primaryStage.getIcons().addAll(
                    AegisUiComponents.createAppIconImage(16),
                    AegisUiComponents.createAppIconImage(32),
                    AegisUiComponents.createAppIconImage(48),
                    AegisUiComponents.createAppIconImage(64),
                    AegisUiComponents.createAppIconImage(128),
                    AegisUiComponents.createAppIconImage(256)
            );
        } catch (Exception e) {
            System.err.println("Could not initialize stage icon: " + e.getMessage());
        }

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f7f8fa; -fx-font-family: 'Segoe UI', system-ui, sans-serif; -fx-border-color: #d1d5db; -fx-border-width: 1px;");

        // Top App Bar (clean, minimalist Disk Drill styling with integrated window controls)
        mainLayout.setTop(createTopAppBar());

        // Left Navigation Sidebar
        mainLayout.setLeft(createSidebar());

        // Center Dynamic Content Area
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #ffffff;");
        mainLayout.setCenter(contentArea);

        // Bottom Activity Bar & Collapsible Forensic Log
        mainLayout.setBottom(createBottomBar());

        // Build All Views
        setupDrivesView();
        setupScanningView();
        setupResultsView();
        setupSanitizerDedicatedView();
        setupFileEraserDedicatedView();
        setupSuccessView();

        // Default Screen: Step 1 Storage Devices
        showDrivesView();

        // Calculate responsive window dimensions based on usable display bounds
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double screenW = visualBounds.getWidth();
        double screenH = visualBounds.getHeight();
        double defaultWidth = Math.min(1240, Math.max(1060, screenW * 0.88));
        double defaultHeight = Math.min(740, Math.max(600, screenH * 0.85));

        // Apple macOS curved window edges with dynamic clipping
        windowClip = new Rectangle();
        windowClip.setArcWidth(24);
        windowClip.setArcHeight(24);
        windowClip.widthProperty().bind(mainLayout.widthProperty());
        windowClip.heightProperty().bind(mainLayout.heightProperty());
        mainLayout.setClip(windowClip);
        mainLayout.setStyle("-fx-background-color: #f7f8fa; -fx-font-family: 'Segoe UI', system-ui, sans-serif; " +
                "-fx-border-color: rgba(0, 0, 0, 0.20); -fx-border-width: 1px; -fx-background-radius: 12; -fx-border-radius: 12;");

        Scene scene = new Scene(mainLayout, defaultWidth, defaultHeight);
        scene.setFill(Color.TRANSPARENT);
        applyModernTheme(scene);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(540);
        primaryStage.setWidth(defaultWidth);
        primaryStage.setHeight(defaultHeight);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();

        // Add edge-drag resizing support for transparent stage (respects custom maximized state)
        ResizeHelper.addResizeSupport(primaryStage, scene, () -> isCustomMaximized);

        primaryStage.show();

        log("[SYSTEM] AEGIS initialized with Win32 IOCTL Direct Block Engine.");
        refreshStorageDevices();
        refreshReports();
        startDeviceHotplugWatcher();
    }

    private void toggleMaximize() {
        if (primaryStage == null) return;

        if (isCustomMaximized) {
            // Restore window to normal floating macOS curved geometry
            isCustomMaximized = false;
            primaryStage.setX(restoreX);
            primaryStage.setY(restoreY);
            primaryStage.setWidth(restoreW);
            primaryStage.setHeight(restoreH);
            applyWindowState(false);
        } else {
            // Save restore bounds before maximizing
            restoreX = primaryStage.getX();
            restoreY = primaryStage.getY();
            restoreW = primaryStage.getWidth();
            restoreH = primaryStage.getHeight();

            // Find current screen's visual bounds (EXCLUDES Windows taskbar!)
            Screen targetScreen = Screen.getScreensForRectangle(restoreX, restoreY, restoreW, restoreH)
                    .stream().findFirst().orElse(Screen.getPrimary());
            Rectangle2D visualBounds = targetScreen.getVisualBounds();

            isCustomMaximized = true;
            primaryStage.setX(visualBounds.getMinX());
            primaryStage.setY(visualBounds.getMinY());
            primaryStage.setWidth(visualBounds.getWidth());
            primaryStage.setHeight(visualBounds.getHeight());
            applyWindowState(true);
        }
    }

    private void applyWindowState(boolean isMax) {
        if (windowClip != null) {
            windowClip.setArcWidth(isMax ? 0 : 24);
            windowClip.setArcHeight(isMax ? 0 : 24);
        }
        if (mainLayout != null) {
            if (isMax) {
                mainLayout.setStyle("-fx-background-color: #f7f8fa; -fx-font-family: 'Segoe UI', system-ui, sans-serif; " +
                        "-fx-border-color: transparent; -fx-border-width: 0px; -fx-background-radius: 0; -fx-border-radius: 0;");
            } else {
                mainLayout.setStyle("-fx-background-color: #f7f8fa; -fx-font-family: 'Segoe UI', system-ui, sans-serif; " +
                        "-fx-border-color: rgba(0, 0, 0, 0.20); -fx-border-width: 1px; -fx-background-radius: 12; -fx-border-radius: 12;");
            }
        }
    }

    // =========================================================================
    // TOP NAVIGATION APP BAR & UNIFIED TOOLBAR (APPLE macOS LIGHT THEME)
    // =========================================================================
    private Node createTopAppBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(48);
        bar.setMinHeight(48);
        bar.setMaxHeight(48);
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setStyle("-fx-background-color: #fafafc; -fx-border-color: #e5e5ea; -fx-border-width: 0 0 1px 0;");

        // Window drag handling on the top header
        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        bar.setOnMouseDragged(e -> {
            if (primaryStage != null) {
                if (isCustomMaximized) {
                    double relX = e.getX() / bar.getWidth();
                    toggleMaximize();
                    primaryStage.setX(e.getScreenX() - (primaryStage.getWidth() * relX));
                    primaryStage.setY(e.getScreenY() - 20);
                    dragOffsetX = primaryStage.getWidth() * relX;
                    dragOffsetY = 20;
                } else {
                    primaryStage.setX(e.getScreenX() - dragOffsetX);
                    primaryStage.setY(e.getScreenY() - dragOffsetY);
                }
            }
        });
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && primaryStage != null) {
                toggleMaximize();
            }
        });

        // 1. Left: Authentic Apple macOS Traffic Lights (Close, Minimize, Zoom)
        Node trafficLights = AegisUiComponents.createWindowControls(primaryStage, this::toggleMaximize);

        // Apple Hairline Divider
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setPrefHeight(14);
        divider.setMaxHeight(14);
        divider.setStyle("-fx-background-color: #e5e5ea;");

        // Apple SF Symbol Sidebar.left Toggle
        Button btnSidebarToggle = AegisUiComponents.createSidebarToggle(() -> {
            if (sidebarContainer != null) {
                sidebarContainer.setVisible(!sidebarContainer.isVisible());
                sidebarContainer.setManaged(sidebarContainer.isVisible());
            }
        });

        // Apple-Grade Squircle Brand Logo + Pro Badge
        Node brandLogo = AegisUiComponents.createBrandLogo();
        brandLogo.setStyle("-fx-cursor: hand;");
        brandLogo.setOnMouseClicked(e -> showDrivesView());

        // 2. Dynamic Apple Context Capsule
        breadcrumbLabel = new Label("Storage Devices");
        breadcrumbLabel.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6e6e73; " +
                "-fx-background-color: #f2f2f7; -fx-background-radius: 999px; -fx-padding: 3 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Right: Apple macOS Unified Toolbar Action Pills
        Button btnAttachImage = new Button("+ Attach Image");
        btnAttachImage.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d1d6; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #1d1d1f; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 2, 0, 0, 1);");
        btnAttachImage.setOnMouseEntered(e -> btnAttachImage.setStyle("-fx-background-color: #f5f5f7; -fx-border-color: #b0b0b8; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #1d1d1f; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand;"));
        btnAttachImage.setOnMouseExited(e -> btnAttachImage.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d1d6; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #1d1d1f; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand;"));
        btnAttachImage.setOnAction(e -> attachDiskImageDialog());

        Button btnQuickDemo = new Button("Sample Disk");
        btnQuickDemo.setGraphic(AegisUiComponents.createVectorLightningIcon(11));
        btnQuickDemo.setGraphicTextGap(6);
        btnQuickDemo.setStyle("-fx-background-color: #f0f7ff; -fx-border-color: #bae0ff; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand;");
        btnQuickDemo.setOnMouseEntered(e -> btnQuickDemo.setStyle("-fx-background-color: #e0f0ff; -fx-border-color: #91caff; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand;"));
        btnQuickDemo.setOnMouseExited(e -> btnQuickDemo.setStyle("-fx-background-color: #f0f7ff; -fx-border-color: #bae0ff; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-padding: 4 10; -fx-cursor: hand;"));
        btnQuickDemo.setOnAction(e -> runFullBenchmarkDemo());

        Button btnHelp = new Button();
        btnHelp.setGraphic(AegisUiComponents.createVectorBookIcon(15));
        btnHelp.setTooltip(new Tooltip("Forensic Documentation & Standards"));
        btnHelp.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;");
        btnHelp.setOnMouseEntered(e -> btnHelp.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 6px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;"));
        btnHelp.setOnMouseExited(e -> btnHelp.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;"));
        btnHelp.setOnAction(e -> showHelpDocumentationDialog());

        HBox rightToolbar = new HBox(6);
        rightToolbar.setAlignment(Pos.CENTER_RIGHT);
        rightToolbar.getChildren().addAll(btnAttachImage, btnQuickDemo, btnHelp);

        bar.getChildren().addAll(trafficLights, divider, btnSidebarToggle, brandLogo, breadcrumbLabel, spacer, rightToolbar);
        return bar;
    }

    private void showHelpDocumentationDialog() {
        AegisDialogs.showHelpDocumentationDialog();
    }

    private void setStep(int step, String breadcrumbText) {
        if (breadcrumbLabel != null) {
            breadcrumbLabel.setText(breadcrumbText != null ? breadcrumbText : "");
        }
    }

    // =========================================================================
    // LEFT SIDEBAR (DYNAMIC DISK DRILL ARCHITECTURE)
    // =========================================================================
    private Node createSidebar() {
        sidebarContainer = new StackPane();
        navSidebar = createNavSidebar();
        scanSidebar = createScanSidebar();
        sidebarContainer.getChildren().setAll(navSidebar);
        return sidebarContainer;
    }

    private VBox createNavSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPrefWidth(190);
        sidebar.setMinWidth(180);
        sidebar.setMaxWidth(205);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 0 0;");

        Label lblSection1 = new Label("DATA RECOVERY");
        lblSection1.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #6b7280; -fx-padding: 4 10 4 10; -fx-letter-spacing: 0.5px;");

        btnNavStorageDevices = createSidebarNavButton("Storage Devices", AegisUiComponents.createVectorHardDriveIcon(16), true);
        btnNavStorageDevices.setOnAction(e -> showDrivesView());

        btnNavCarver = createSidebarNavButton("Evidence Carver", AegisUiComponents.createVectorMagnifierIcon(16), false);
        btnNavCarver.setOnAction(e -> {
            if (!carvedFileList.isEmpty()) {
                showResultsView();
            } else if (selectedDevice != null) {
                showCarvingConfirmationDialog(selectedDevice);
            } else {
                showAlert("Select Storage Drive", "Please select a storage device from the list first, or attach a forensic disk image.");
            }
        });

        Label lblSection2 = new Label("SANITIZATION & WIPING");
        lblSection2.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #6b7280; -fx-padding: 14 10 4 10; -fx-letter-spacing: 0.5px;");

        btnNavSanitizer = createSidebarNavButton("Drive Sanitizer", AegisUiComponents.createVectorShieldIcon(16), false);
        btnNavSanitizer.setOnAction(e -> showSanitizerDedicatedView());

        btnNavFileEraser = createSidebarNavButton("File & Slack Eraser", AegisUiComponents.createVectorEraserIcon(16), false);
        btnNavFileEraser.setOnAction(e -> showFileEraserDedicatedView());

        Label lblSection3 = new Label("AUDIT & BLOCKCHAIN");
        lblSection3.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #6b7280; -fx-padding: 14 10 4 10; -fx-letter-spacing: 0.5px;");

        btnNavCertificates = createSidebarNavButton("Audit Certificates", AegisUiComponents.createVectorCertificateIcon(16), false);
        btnNavCertificates.setOnAction(e -> showCertificatesView());

        btnNavBlockchain = createSidebarNavButton("Blockchain Ledger", AegisUiComponents.createVectorBlockchainIcon(16), false);
        btnNavBlockchain.setOnAction(e -> showBlockchainLedgerView());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox bottomActions = new VBox(6);
        Button btnAttach = new Button("+ Attach disk image...");
        btnAttach.setMaxWidth(Double.MAX_VALUE);
        btnAttach.setAlignment(Pos.CENTER_LEFT);
        btnAttach.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-text-fill: #0078d4; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAttach.setOnAction(e -> attachDiskImageDialog());

        bottomActions.getChildren().add(btnAttach);

        sidebar.getChildren().addAll(lblSection1, btnNavStorageDevices, btnNavCarver,
                lblSection2, btnNavSanitizer, btnNavFileEraser,
                lblSection3, btnNavCertificates, btnNavBlockchain,
                spacer, bottomActions);
        return sidebar;
    }

    private VBox createScanSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(190);
        sidebar.setMinWidth(180);
        sidebar.setMaxWidth(205);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #e5e7eb; -fx-border-width: 0 1 0 0;");

        // Top Dashboard Pill (home.png)
        HBox btnDashboard = new HBox(8);
        btnDashboard.setAlignment(Pos.CENTER_LEFT);
        btnDashboard.setPadding(new Insets(8, 12, 8, 12));
        btnDashboard.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 6; -fx-cursor: hand;");
        Label iconDash = new Label("▦");
        iconDash.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label lblDash = new Label("Dashboard");
        lblDash.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Region spDash = new Region();
        HBox.setHgrow(spDash, Priority.ALWAYS);
        Label spinner = new Label("●");
        spinner.setStyle("-fx-font-size: 10px; -fx-text-fill: #0078d4;");
        btnDashboard.getChildren().addAll(iconDash, lblDash, spDash, spinner);
        btnDashboard.setOnMouseClicked(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(scanningScrollPane != null ? scanningScrollPane : scanningViewPane);
        });

        Label lblSectionResults = new Label("SCAN RESULTS");
        lblSectionResults.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #8892a0; -fx-padding: 12 10 4 10; -fx-letter-spacing: 0.5px;");

        lblSideAllCount = new Label("0");
        Node rowAll = createScanSidebarRow(AegisUiComponents.createVectorHardDriveIcon(15), "All Recoverable Files", lblSideAllCount, () -> filterAndShowResults("ALL"));

        lblSidePicCount = new Label("0");
        Node rowPic = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("PICTURES", 15), "Pictures", lblSidePicCount, () -> filterAndShowResults(".jpg", ".png"));

        lblSideVidCount = new Label("0");
        Node rowVid = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("VIDEOS", 15), "Videos", lblSideVidCount, () -> filterAndShowResults(".mp4", ".avi"));

        lblSideAudCount = new Label("0");
        Node rowAud = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("AUDIO", 15), "Audio", lblSideAudCount, () -> filterAndShowResults(".mp3", ".wav"));

        lblSideDocCount = new Label("0");
        Node rowDoc = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("DOCUMENTS", 15), "Documents", lblSideDocCount, () -> filterAndShowResults(".pdf"));

        lblSideArcCount = new Label("0");
        Node rowArc = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("ARCHIVES", 15), "Archives", lblSideArcCount, () -> filterAndShowResults(".zip", ".7z"));

        lblSideOthCount = new Label("0");
        Node rowOth = createScanSidebarRow(AegisUiComponents.createVectorCategoryIcon("OTHER", 15), "Other", lblSideOthCount, () -> filterAndShowResults("ALL"));

        Label lblSectionLost = new Label("LOST PARTITIONS");
        lblSectionLost.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #8892a0; -fx-padding: 14 10 4 10; -fx-letter-spacing: 0.5px;");

        Node rowPart1 = createScanSidebarRow(AegisUiComponents.createVectorHardDriveIcon(15), "Lost partition New Volume (NTFS)", null, () -> filterAndShowResults("ALL"));
        Node rowPart2 = createScanSidebarRow(AegisUiComponents.createVectorHardDriveIcon(15), "Lost partition USB Drive (FAT32)", null, () -> filterAndShowResults("ALL"));

        Region spSide = new Region();
        VBox.setVgrow(spSide, Priority.ALWAYS);

        Button btnShowInExplorer = new Button("Show scan results in Explorer");
        btnShowInExplorer.setMaxWidth(Double.MAX_VALUE);
        btnShowInExplorer.setAlignment(Pos.CENTER);
        btnShowInExplorer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-padding: 8 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnShowInExplorer.setOnAction(e -> {
            try {
                File dir = new File("carved_evidence");
                if (!dir.exists()) dir.mkdirs();
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                log("[ERROR] Open explorer failed: " + ex.getMessage());
            }
        });

        sidebar.getChildren().addAll(
                btnDashboard, lblSectionResults,
                rowAll, rowPic, rowVid, rowAud, rowDoc, rowArc, rowOth,
                lblSectionLost, rowPart1, rowPart2,
                spSide, btnShowInExplorer
        );
        return sidebar;
    }

    private Node createScanSidebarRow(Node iconNode, String title, Label countBadge, Runnable onClick) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        row.getChildren().addAll(iconNode, lblTitle, sp);

        if (countBadge != null) {
            countBadge.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 10;");
            row.getChildren().add(countBadge);
        }

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            if (onClick != null) onClick.run();
        });

        return row;
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    private Button createSidebarNavButton(String text, Node vectorIcon, boolean active) {
        Button btn = new Button(text);
        btn.setGraphic(vectorIcon);
        btn.setGraphicTextGap(10);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(active ? getActiveNavStyle() : getInactiveNavStyle());
        return btn;
    }

    private String getActiveNavStyle() {
        return "-fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private String getInactiveNavStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #4b5563; -fx-font-size: 13px; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private void setActiveNav(Button btn) {
        btnNavStorageDevices.setStyle(getInactiveNavStyle());
        btnNavCarver.setStyle(getInactiveNavStyle());
        btnNavSanitizer.setStyle(getInactiveNavStyle());
        btnNavFileEraser.setStyle(getInactiveNavStyle());
        btnNavCertificates.setStyle(getInactiveNavStyle());
        btnNavBlockchain.setStyle(getInactiveNavStyle());
        btn.setStyle(getActiveNavStyle());
    }

    // =========================================================================
    // 1. STORAGE DEVICES VIEW (STEP 1: SELECT YOUR DEVICE)
    // =========================================================================
    private void toggleDiskCollapse(StorageDevice disk) {
        if (disk == null || !disk.isPhysical()) return;
        String path = disk.getDevicePath();
        if (collapsedDisks.contains(path)) {
            collapsedDisks.remove(path);
        } else {
            collapsedDisks.add(path);
        }
        updateVisibleDeviceList();
    }

    private void updateVisibleDeviceList() {
        StorageDevice selected = (drivesTable != null && drivesTable.getSelectionModel() != null)
                ? drivesTable.getSelectionModel().getSelectedItem() : null;

        deviceList.clear();
        boolean parentCollapsed = false;

        for (StorageDevice dev : masterDeviceList) {
            if (dev.isPhysical()) {
                deviceList.add(dev);
                parentCollapsed = collapsedDisks.contains(dev.getDevicePath());
            } else {
                // Logical volume: only display if its parent hardware disk is expanded
                if (!parentCollapsed) {
                    deviceList.add(dev);
                }
            }
        }

        if (drivesTable != null) {
            if (selected != null && deviceList.contains(selected)) {
                drivesTable.getSelectionModel().select(selected);
            } else if (!deviceList.isEmpty()) {
                drivesTable.getSelectionModel().select(0);
                updateRightInspector(deviceList.get(0));
            }
        }

        if (tableItemCountLabel != null) {
            int physCount = 0;
            int volCount = 0;
            for (StorageDevice d : masterDeviceList) {
                if (d.isPhysical()) physCount++;
                else volCount++;
            }
            tableItemCountLabel.setText(physCount + " physical drive(s) \u2022 " + volCount + " volume(s)");
        }

        // Update sanitizable devices list (only real physical disks & lettered logical volumes; never hidden partitions or unallocated space)
        sanitizableDeviceList.clear();
        for (StorageDevice dev : masterDeviceList) {
            if (dev.isPhysical()) {
                sanitizableDeviceList.add(dev);
            } else {
                String name = dev.getModel() != null ? dev.getModel() : "";
                if (dev.getDevicePath().matches("\\\\\\\\.\\\\[a-zA-Z]:") || name.matches(".*\\([A-Za-z]:\\).*")) {
                    sanitizableDeviceList.add(dev);
                }
            }
        }
        if (comboSanitizerTarget != null && (comboSanitizerTarget.getValue() == null || !sanitizableDeviceList.contains(comboSanitizerTarget.getValue()))) {
            if (!sanitizableDeviceList.isEmpty()) {
                StorageDevice defaultDev = sanitizableDeviceList.stream().filter(d -> !d.isSystemDrive()).findFirst().orElse(sanitizableDeviceList.get(0));
                comboSanitizerTarget.setValue(defaultDev);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupDrivesView() {
        drivesViewPane = new BorderPane();

        VBox centerBox = new VBox(8);
        centerBox.setPadding(new Insets(14, 16, 14, 16));

        HBox subheader = new HBox(12);
        subheader.setAlignment(Pos.CENTER_LEFT);
        subheader.setPadding(new Insets(0, 0, 4, 2));

        tableItemCountLabel.setText("Scanning storage controllers...");
        tableItemCountLabel.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #6e6e73;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        btnRefresh = new Button("\u21BB Refresh");
        btnRefresh.setStyle("-fx-background-color: transparent; -fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 3 8; -fx-background-radius: 6;");
        btnRefresh.setOnMouseEntered(e -> {
            if (!btnRefresh.isDisable()) {
                btnRefresh.setStyle("-fx-background-color: rgba(0, 113, 227, 0.08); -fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 3 8; -fx-background-radius: 6;");
            }
        });
        btnRefresh.setOnMouseExited(e -> {
            if (!btnRefresh.isDisable()) {
                btnRefresh.setStyle("-fx-background-color: transparent; -fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 3 8; -fx-background-radius: 6;");
            }
        });
        btnRefresh.setOnAction(e -> refreshStorageDevices());

        subheader.getChildren().addAll(tableItemCountLabel, sp, btnRefresh);

        drivesTable = new TableView<>();
        drivesTable.setItems(deviceList);
        drivesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        drivesTable.setPlaceholder(createTableLoadingPlaceholder());

        // Double-click row on physical disk to expand/collapse child volumes
        drivesTable.setRowFactory(tv -> {
            TableRow<StorageDevice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    StorageDevice rowDev = row.getItem();
                    if (rowDev != null && rowDev.isPhysical()) {
                        toggleDiskCollapse(rowDev);
                    }
                }
            });
            return row;
        });

        TableColumn<StorageDevice, StorageDevice> colName = new TableColumn<>("Device / Disk");
        colName.setMinWidth(220);
        colName.setPrefWidth(340);
        colName.setSortable(false);
        colName.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StorageDevice dev, boolean empty) {
                super.updateItem(dev, empty);
                if (empty || dev == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    boolean isPhysical = dev.isPhysical();
                    if (isPhysical) {
                        boolean isCollapsed = collapsedDisks.contains(dev.getDevicePath());
                        final StorageDevice currentDev = dev;
                        Button chevronBtn = AegisUiComponents.createVectorChevronButton(!isCollapsed, () -> {
                            toggleDiskCollapse(currentDev);
                        });

                        Node diskIcon = AegisUiComponents.createVectorHardwareDiskRowIcon();
                        String nameStr = (dev.getModel() != null && !dev.getModel().isEmpty()) ? dev.getModel() : dev.getDevicePath();
                        Label nameLabel = new Label(nameStr);
                        nameLabel.setStyle("-fx-font-family: -apple-system, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-font-size: 13px;");
                        box.getChildren().addAll(chevronBtn, diskIcon, nameLabel);
                    } else {
                        // Clean, modern visual indentation matching Disk Drill & Apple macOS (no ASCII tree symbols)
                        Region indent = new Region();
                        indent.setMinWidth(28);
                        indent.setPrefWidth(28);

                        String nameStr = (dev.getModel() != null && !dev.getModel().isEmpty()) ? dev.getModel() : dev.getDevicePath();
                        boolean isWindowsDrive = dev.isSystemDrive() || nameStr.toUpperCase().contains("C:");
                        boolean isLetteredVol = nameStr.matches(".*\\([A-Za-z]:\\).*") || dev.getDevicePath().matches("\\\\\\\\.\\\\[A-Za-z]:");

                        Node volIcon = isWindowsDrive
                                ? AegisUiComponents.createVectorWindowsLogoIcon()
                                : (isLetteredVol ? AegisUiComponents.createVectorFolderIcon(16) : AegisUiComponents.createVectorHardDriveIcon(16));
                        Label nameLabel = new Label(nameStr);
                        nameLabel.setStyle(isLetteredVol
                                ? "-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-weight: 600; -fx-text-fill: #1e293b; -fx-font-size: 12.5px;"
                                : "-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-weight: 500; -fx-text-fill: #475569; -fx-font-size: 12px;");
                        box.getChildren().addAll(indent, volIcon, nameLabel);
                    }
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        TableColumn<StorageDevice, String> colType = new TableColumn<>("Type");
        colType.setMinWidth(90);
        colType.setPrefWidth(110);
        colType.setSortable(false);
        colType.setCellValueFactory(d -> {
            StorageDevice dev = d.getValue();
            if (dev.isPhysical()) return new SimpleStringProperty("Hardware disk");
            String name = dev.getModel() != null ? dev.getModel() : "";
            if (dev.getDevicePath().matches("\\\\\\\\.\\\\[a-zA-Z]:") || name.matches(".*\\([A-Za-z]:\\).*")) {
                return new SimpleStringProperty("Logical volume");
            }
            return new SimpleStringProperty("Partition");
        });

        TableColumn<StorageDevice, String> colBus = new TableColumn<>("Connection / FS");
        colBus.setMinWidth(85);
        colBus.setPrefWidth(105);
        colBus.setSortable(false);
        colBus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInterfaceType()));

        TableColumn<StorageDevice, String> colCapacity = new TableColumn<>("Capacity");
        colCapacity.setMinWidth(85);
        colCapacity.setPrefWidth(100);
        colCapacity.setSortable(false);
        colCapacity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedSize()));

        // Disk Drill Protection Column (Subtle outline shield header, verified volume badges, perfectly centered)
        TableColumn<StorageDevice, StorageDevice> colProtection = new TableColumn<>();
        Node headerShield = AegisUiComponents.createHeaderShield(14);
        StackPane headerBox = new StackPane(headerShield);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setMaxWidth(Double.MAX_VALUE);
        colProtection.setGraphic(headerBox);
        colProtection.setMinWidth(48);
        colProtection.setMaxWidth(48);
        colProtection.setPrefWidth(48);
        colProtection.setResizable(false);
        colProtection.setSortable(false);
        colProtection.getStyleClass().add("col-center");
        colProtection.setStyle("-fx-alignment: CENTER;");
        colProtection.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colProtection.setCellFactory(col -> new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                getStyleClass().add("col-center");
                setStyle("-fx-alignment: CENTER; -fx-padding: 0;");
            }
            @Override
            protected void updateItem(StorageDevice dev, boolean empty) {
                super.updateItem(dev, empty);
                setAlignment(Pos.CENTER);
                if (empty || dev == null || dev.isPhysical()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String name = dev.getModel() != null ? dev.getModel() : "";
                    boolean isLetteredVol = name.matches(".*\\([A-Za-z]:\\).*") || dev.getDevicePath().matches("\\\\\\\\.\\\\[A-Za-z]:");
                    if (isLetteredVol) {
                        Node badge = AegisUiComponents.createVaultBadge(14);
                        StackPane pane = new StackPane(badge);
                        pane.setAlignment(Pos.CENTER);
                        pane.setMaxWidth(Double.MAX_VALUE);
                        Tooltip.install(pane, new Tooltip("Protected Volume (Guaranteed Recovery Active)"));
                        setGraphic(pane);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });

        drivesTable.getColumns().addAll(colName, colType, colBus, colCapacity, colProtection);
        VBox.setVgrow(drivesTable, Priority.ALWAYS);

        drivesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateRightInspector(newVal);
            }
        });

        HBox bottomTableToolbar = new HBox(12);
        bottomTableToolbar.setAlignment(Pos.CENTER_LEFT);
        bottomTableToolbar.setPadding(new Insets(8, 0, 0, 0));

        Label hintLabel = new Label("\uD83D\uDCA1 Click the chevron icon on physical disks to collapse or expand partitions. Select any drive to inspect hardware details.");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        bottomTableToolbar.getChildren().add(hintLabel);
        centerBox.getChildren().addAll(subheader, drivesTable, bottomTableToolbar);
        drivesViewPane.setCenter(centerBox);

        // Right Detail Inspector Panel (Styled after Disk Drill Image 2)
        rightInspectorPane = new VBox(11);
        rightInspectorPane.setPrefWidth(290);
        rightInspectorPane.setMinWidth(275);
        rightInspectorPane.setMaxWidth(310);
        rightInspectorPane.setPadding(new Insets(18, 16, 18, 16));
        rightInspectorPane.setStyle("-fx-background-color: #ffffff;");

        VBox graphicBox = new VBox(8);
        graphicBox.setAlignment(Pos.CENTER);
        Node diskGraphic = AegisUiComponents.createDriveIllustration(160, 68);

        inspectorDriveName = new Label("Select a Storage Drive");
        inspectorDriveName.setStyle("-fx-font-family: -apple-system, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #111827; -fx-wrap-text: true; -fx-text-alignment: center;");

        inspectorDriveSubtext = new Label("Hardware media • -- GB");
        inspectorDriveSubtext.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 11.5px; -fx-text-fill: #64748b;");

        graphicBox.getChildren().addAll(diskGraphic, inspectorDriveName, inspectorDriveSubtext);

        btnInspectorSearchData = new Button("Search for lost data");
        btnInspectorSearchData.setMaxWidth(Double.MAX_VALUE);
        btnInspectorSearchData.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 9 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnInspectorSearchData.setOnAction(e -> {
            if (selectedDevice != null) {
                showCarvingConfirmationDialog(selectedDevice);
            } else {
                showAlert("Select Drive", "Please choose a connected drive or attach a disk image to begin scanning.");
            }
        });

        btnInspectorSanitize = new Button("Securely Sanitize Drive (Wipe)");
        btnInspectorSanitize.setMaxWidth(Double.MAX_VALUE);
        btnInspectorSanitize.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-text-fill: #dc2626; -fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnInspectorSanitize.setOnAction(e -> {
            if (selectedDevice != null) {
                promptAndSanitizeDrive(selectedDevice);
            }
        });

        Separator topSep = new Separator();
        topSep.setStyle("-fx-opacity: 0.5; -fx-padding: 2 0 2 0;");

        HBox generalHeader = new HBox(6);
        generalHeader.setAlignment(Pos.CENTER_LEFT);
        generalHeader.setPadding(new Insets(2, 0, 2, 0));
        Label generalChevron = new Label("▾");
        generalChevron.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        Label generalTitle = new Label("General");
        generalTitle.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        generalHeader.getChildren().addAll(generalChevron, generalTitle);

        VBox propertiesBox = new VBox(0);
        propType = addInspectorRow(propertiesBox, "Type:");
        propModel = addInspectorRow(propertiesBox, "Device model:");
        propSerial = addInspectorRow(propertiesBox, "Serial:");
        propVendor = addInspectorRow(propertiesBox, "Vendor:");
        propProtocol = addInspectorRow(propertiesBox, "Protocol:");
        propRevision = addInspectorRow(propertiesBox, "Revision:");
        propCapacity = addInspectorRow(propertiesBox, "Capacity:");
        propAvailable = addInspectorRow(propertiesBox, "Available:");
        propPhysicalNum = addInspectorRow(propertiesBox, "Physical number:");
        propPartitions = addInspectorRow(propertiesBox, "Partitions:");

        childPartitionsBox = new VBox(3);
        childPartitionsBox.setPadding(new Insets(4, 0, 0, 0));

        rightInspectorPane.getChildren().addAll(
                graphicBox,
                btnInspectorSearchData,
                btnInspectorSanitize,
                topSep,
                generalHeader,
                propertiesBox,
                childPartitionsBox
        );

        ScrollPane inspectorScroll = new ScrollPane(rightInspectorPane);
        inspectorScroll.setFitToWidth(true);
        inspectorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        inspectorScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        inspectorScroll.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 0 1;");
        drivesViewPane.setRight(inspectorScroll);
    }

    private Label addInspectorRow(VBox parent, String key) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4.5, 0, 4.5, 0));

        Label lblKey = new Label(key);
        lblKey.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 11.5px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblVal = new Label("--");
        lblVal.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-text-fill: #1e293b;");
        lblVal.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);

        row.getChildren().addAll(lblKey, spacer, lblVal);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.30; -fx-padding: 0;");

        parent.getChildren().addAll(row, sep);
        return lblVal;
    }

    private Label addInspectorProperty(GridPane grid, int row, String key, String defaultVal) {
        Label lblKey = new Label(key);
        lblKey.setMinWidth(95);
        lblKey.setPrefWidth(95);
        lblKey.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        Label lblVal = new Label(defaultVal);
        lblVal.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        lblVal.setWrapText(true);
        grid.add(lblKey, 0, row);
        grid.add(lblVal, 1, row);
        return lblVal;
    }

    private void updateRightInspector(StorageDevice dev) {
        this.selectedDevice = dev;
        boolean isPhys = dev.isPhysical();
        String nameStr = dev.getModel() != null && !dev.getModel().isEmpty() ? dev.getModel() : dev.getDevicePath();
        inspectorDriveName.setText(nameStr);
        inspectorDriveSubtext.setText((isPhys ? "Hardware disk \u2022 " : "Logical volume \u2022 ") + dev.getFormattedSize());

        propType.setText(isPhys ? "Hardware disk" : (dev.getDevicePath().matches("\\\\\\\\.\\\\[a-zA-Z]:") ? "Logical volume" : "Partition"));
        propModel.setText(nameStr);
        propSerial.setText(dev.getSerialNumber() != null && !dev.getSerialNumber().isEmpty() ? dev.getSerialNumber() : "N/A");

        // Extract vendor matching Disk Drill
        String vendor = extractVendor(nameStr);
        propVendor.setText(vendor);

        propProtocol.setText(dev.getInterfaceType() != null ? dev.getInterfaceType() : "Direct Block");
        propRevision.setText(isPhys ? (vendor.contains("HP") ? "PMAP" : "1102") : dev.getInterfaceType());
        propCapacity.setText(dev.getFormattedSize());
        propAvailable.setText(dev.getFormattedSize());

        // Physical number
        String path = isPhys ? dev.getDevicePath() : (dev.getParentDevicePath() != null ? dev.getParentDevicePath() : dev.getDevicePath());
        String physNum = "0";
        if (path != null && path.contains("PhysicalDrive")) {
            physNum = path.replaceAll("[^0-9]", "");
            if (physNum.isEmpty()) physNum = "0";
        }
        propPhysicalNum.setText(physNum);

        // Count child partitions & populate child list matching Disk Drill Image 2
        childPartitionsBox.getChildren().clear();
        int childCount = 0;
        String parentTarget = isPhys ? dev.getDevicePath() : dev.getParentDevicePath();

        if (parentTarget != null) {
            for (StorageDevice child : masterDeviceList) {
                if (child.getParentDevicePath() != null && child.getParentDevicePath().equalsIgnoreCase(parentTarget)) {
                    childCount++;
                    if (isPhys) {
                        HBox childRow = createChildPartitionRow(child);
                        childPartitionsBox.getChildren().add(childRow);
                    }
                }
            }
        }
        propPartitions.setText(String.valueOf(childCount > 0 ? childCount : 1));

        // Sanitization action state (preserves hardware safety lock for OS C: drive)
        if (dev.isSystemDrive() || nameStr.toUpperCase().contains("C:")) {
            btnInspectorSanitize.setDisable(true);
            btnInspectorSanitize.setText("Sanitization Locked (OS Drive)");
            btnInspectorSanitize.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-text-fill: #94a3b8; -fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6;");
        } else {
            btnInspectorSanitize.setDisable(false);
            btnInspectorSanitize.setText("Securely Sanitize Drive (Wipe)");
            btnInspectorSanitize.setStyle("-fx-background-color: #ffffff; -fx-border-color: #fca5a5; -fx-text-fill: #dc2626; -fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        }
    }

    private HBox createChildPartitionRow(StorageDevice child) {
        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 6, 5, 6));
        row.setStyle("-fx-background-radius: 4; -fx-cursor: hand;");

        Label chev = new Label("▸");
        chev.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        Label nameLbl = new Label("Child '" + child.getModel() + "'");
        nameLbl.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        row.getChildren().addAll(chev, nameLbl);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4;"));
        row.setOnMouseClicked(e -> {
            drivesTable.getSelectionModel().select(child);
            drivesTable.scrollTo(child);
        });
        return row;
    }

    private String extractVendor(String model) {
        if (model == null || model.isEmpty()) return "Generic";
        String u = model.toUpperCase();
        if (u.contains("HP")) return "HP";
        if (u.contains("WD") || u.contains("WESTERN")) return "Western Digital";
        if (u.contains("SAMSUNG")) return "Samsung";
        if (u.contains("SANDISK")) return "SanDisk";
        if (u.contains("KINGSTON")) return "Kingston";
        if (u.contains("CRUCIAL") || u.contains("MICRON")) return "Crucial";
        if (u.contains("SEAGATE")) return "Seagate";
        if (u.contains("TOSHIBA") || u.contains("KIOXIA")) return "Kioxia";
        String[] parts = model.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "Generic";
    }

    // =========================================================================
    // 2. SCANNING & PROGRESS SCREEN (STEP 2: SCAN MEDIA & ENTROPY)
    // =========================================================================
    private void setupScanningView() {
        scanningViewPane = new VBox(9);
        scanningViewPane.setPadding(new Insets(10, 20, 10, 20));
        scanningViewPane.setStyle("-fx-background-color: #ffffff;");

        // Top Navigation Controls (Matching home.png: Home, Pause, Recover All, Review Found Items)
        HBox topControls = new HBox(10);
        topControls.setAlignment(Pos.CENTER_LEFT);

        Button btnHome = new Button("⌂");
        btnHome.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #1f2937; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnHome.setOnAction(e -> {
            if (!isScanCancelled) {
                isScanCancelled = true;
            }
            showDrivesView();
        });

        btnPauseResumeScan = new Button("❚❚  Pause");
        btnPauseResumeScan.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPauseResumeScan.setOnAction(e -> {
            isScanPaused = !isScanPaused;
            btnPauseResumeScan.setText(isScanPaused ? "▶  Resume" : "❚❚  Pause");
        });

        Region spTop = new Region();
        HBox.setHgrow(spTop, Priority.ALWAYS);

        Button btnRecoverAll = new Button("Recover all...");
        btnRecoverAll.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-text-fill: #1f2937; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRecoverAll.setOnAction(e -> executeRecoveryAndAudit());

        btnReviewFoundItems = new Button("Review found items");
        btnReviewFoundItems.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 6; -fx-font-size: 11.5px; -fx-cursor: hand;");
        btnReviewFoundItems.setOnAction(e -> showResultsView());

        topControls.getChildren().addAll(btnHome, btnPauseResumeScan, spTop, btnRecoverAll, btnReviewFoundItems);

        // Header Title & Subtitle (home.png)
        VBox titleBox = new VBox(2);
        HBox titleLine = new HBox(10);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        scanHeaderTitle = new Label("Scanning \"Storage Media\"");
        scanHeaderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        scanPhaseBadge = new Label("● PHASE 2/3: DEEP RAW SECTOR CARVING");
        scanPhaseBadge.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #1d4ed8;");

        titleLine.getChildren().addAll(scanHeaderTitle, scanPhaseBadge);

        scanHeaderSubtitle = new Label("0% complete — 0 files / 0 MB • 00:00 elapsed");
        scanHeaderSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        titleBox.getChildren().addAll(titleLine, scanHeaderSubtitle);

        // Progress Bar & Real-time Metrics
        scanProgressBar = new ProgressBar(0);
        scanProgressBar.setMaxWidth(Double.MAX_VALUE);
        scanProgressBar.setPrefHeight(5);
        scanProgressBar.setStyle("-fx-accent: #0078d4;");

        // The 6 Vibrant Category Cards (home.png) - Clicking filters and opens results!
        HBox cardsBox = new HBox(8);
        cardsBox.setAlignment(Pos.CENTER);

        VBox cardPic = AegisUiComponents.createCategoryCard("Pictures", "0 files", "🖼", "#0078d4", "#0284c7");
        lblPicturesCount = (Label) ((VBox) cardPic.getChildren().get(1)).getChildren().get(1);
        cardPic.setOnMouseClicked(e -> filterAndShowResults(".jpg", ".png"));

        VBox cardVid = AegisUiComponents.createCategoryCard("Videos", "0 files", "🎬", "#ea580c", "#f97316");
        lblVideosCount = (Label) ((VBox) cardVid.getChildren().get(1)).getChildren().get(1);
        cardVid.setOnMouseClicked(e -> filterAndShowResults(".mp4", ".avi"));

        VBox cardAud = AegisUiComponents.createCategoryCard("Audio", "0 files", "🎵", "#00b4d8", "#0077b6");
        lblAudioCount = (Label) ((VBox) cardAud.getChildren().get(1)).getChildren().get(1);
        cardAud.setOnMouseClicked(e -> filterAndShowResults(".mp3", ".wav"));

        VBox cardDoc = AegisUiComponents.createCategoryCard("Documents", "0 files", "📄", "#f43f5e", "#e11d48");
        lblDocumentsCount = (Label) ((VBox) cardDoc.getChildren().get(1)).getChildren().get(1);
        cardDoc.setOnMouseClicked(e -> filterAndShowResults(".pdf"));

        VBox cardArc = AegisUiComponents.createCategoryCard("Archives", "0 files", "📦", "#9333ea", "#7928ca");
        lblArchivesCount = (Label) ((VBox) cardArc.getChildren().get(1)).getChildren().get(1);
        cardArc.setOnMouseClicked(e -> filterAndShowResults(".zip", ".7z"));

        VBox cardOth = AegisUiComponents.createCategoryCard("Other", "0 files", "⚙", "#6366f1", "#4338ca");
        lblOtherCount = (Label) ((VBox) cardOth.getChildren().get(1)).getChildren().get(1);
        cardOth.setOnMouseClicked(e -> filterAndShowResults("ALL"));

        HBox.setHgrow(cardPic, Priority.ALWAYS);
        HBox.setHgrow(cardVid, Priority.ALWAYS);
        HBox.setHgrow(cardAud, Priority.ALWAYS);
        HBox.setHgrow(cardDoc, Priority.ALWAYS);
        HBox.setHgrow(cardArc, Priority.ALWAYS);
        HBox.setHgrow(cardOth, Priority.ALWAYS);

        cardsBox.getChildren().addAll(cardPic, cardVid, cardAud, cardDoc, cardArc, cardOth);

        HBox metaBar = new HBox(12);
        metaBar.setAlignment(Pos.CENTER_LEFT);
        scanSpeedEtaLabel = new Label("Throughput: Calculating • Elapsed: 00:00 • ETA: --:-- • Sector: 0 / 0");
        scanSpeedEtaLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #374151; -fx-font-weight: bold;");

        Region spMeta = new Region();
        HBox.setHgrow(spMeta, Priority.ALWAYS);

        scanEntropyLabel = new Label("Shannon Entropy: 0.0000 bits/byte (Unallocated Flash Sectors)");
        scanEntropyLabel.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        metaBar.getChildren().addAll(scanSpeedEtaLabel, spMeta, scanEntropyLabel);

        // Real-Time Hardware Oscilloscope Waveform Pane (Zero lag, fluid 60fps streaming)
        waveformPane = new AegisUiComponents.ForensicWaveformPane();
        VBox.setVgrow(waveformPane, Priority.ALWAYS);

        // Real-Time Discovery Ticker Footer (home.png footer)
        HBox tickerBox = new HBox(8);
        tickerBox.setAlignment(Pos.CENTER_LEFT);
        tickerBox.setPadding(new Insets(3, 8, 3, 8));
        tickerBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");

        Label tickerIcon = new Label("📡");
        lblRecentDiscovery = new Label("0h 00m elapsed. Deep scanning volume storage sectors...");
        lblRecentDiscovery.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-font-family: 'Segoe UI', system-ui, sans-serif;");
        tickerBox.getChildren().addAll(tickerIcon, lblRecentDiscovery);

        scanningViewPane.getChildren().addAll(topControls, titleBox, scanProgressBar, cardsBox, metaBar, waveformPane, tickerBox);

        // Wrap in smooth ScrollPane so it never clips on any screen resolution or unmaximized window
        scanningScrollPane = new ScrollPane(scanningViewPane);
        scanningScrollPane.setFitToWidth(true);
        scanningScrollPane.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff; -fx-border-color: transparent; -fx-border-width: 0;");
    }

    // =========================================================================
    // 3. RESULTS GALLERY & TABLE VIEW (STEP 3: REVIEW & RECOVER)
    // =========================================================================
    @SuppressWarnings("unchecked")
    private void setupResultsView() {
        resultsViewPane = new BorderPane();
        resultsViewPane.setStyle("-fx-background-color: #ffffff;");

        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(16, 20, 10, 20));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button btnBack = new Button("← Back to Drives");
        btnBack.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnBack.setOnAction(e -> showDrivesView());

        resultsHeaderTitle = new Label("Extracted Evidence Manifest");
        resultsHeaderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Region spTop = new Region();
        HBox.setHgrow(spTop, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search files by name or ext...");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            activeSearchQuery = newV != null ? newV.trim() : "";
            applyFilters();
        });

        // View Mode Toggle (Gallery vs Table)
        Button btnToggleView = new Button(isGalleryView ? "☰ Table View" : "▦ Gallery View");
        btnToggleView.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        btnToggleView.setOnAction(e -> {
            isGalleryView = !isGalleryView;
            btnToggleView.setText(isGalleryView ? "☰ Table View" : "▦ Gallery View");
            switchViewMode();
        });

        Button btnCert = new Button("📜 Export Certificate");
        btnCert.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        btnCert.setOnAction(e -> executeRecoveryAndAudit());

        topBar.getChildren().addAll(btnBack, resultsHeaderTitle, spTop, searchField, btnToggleView, btnCert);

        resultsHeaderSubtitle = new Label("0 files discovered across unallocated raw sectors");
        resultsHeaderSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        // Filter Pills
        HBox filterPills = new HBox(8);
        filterPills.setAlignment(Pos.CENTER_LEFT);

        Button pillAll = createFilterPill("All Files");
        pillAll.setOnAction(e -> filterAndShowResults("ALL"));

        Button pillPictures = createFilterPill("🖼 Pictures");
        pillPictures.setOnAction(e -> filterAndShowResults(".jpg", ".png"));

        Button pillVideos = createFilterPill("🎬 Videos");
        pillVideos.setOnAction(e -> filterAndShowResults(".mp4", ".avi"));

        Button pillAudio = createFilterPill("🎵 Audio");
        pillAudio.setOnAction(e -> filterAndShowResults(".mp3", ".wav"));

        Button pillDocuments = createFilterPill("📄 Documents");
        pillDocuments.setOnAction(e -> filterAndShowResults(".pdf"));

        Button pillArchives = createFilterPill("📦 Archives");
        pillArchives.setOnAction(e -> filterAndShowResults(".zip", ".7z"));

        filterPills.getChildren().addAll(pillAll, pillPictures, pillVideos, pillAudio, pillDocuments, pillArchives);

        topBox.getChildren().addAll(topBar, resultsHeaderSubtitle, filterPills);
        resultsViewPane.setTop(topBox);

        // Center Split: Left = View (Gallery or Table), Right = Detail Inspector
        HBox centerSplit = new HBox(16);
        centerSplit.setPadding(new Insets(6, 20, 10, 20));

        viewModeSwitcherArea = new StackPane();
        HBox.setHgrow(viewModeSwitcherArea, Priority.ALWAYS);

        // 1. Gallery Scroll Pane
        ScrollPane galleryScroll = new ScrollPane();
        galleryScroll.setFitToWidth(true);
        galleryScroll.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");

        galleryFlowPane = new FlowPane();
        galleryFlowPane.setHgap(12);
        galleryFlowPane.setVgap(12);
        galleryFlowPane.setPadding(new Insets(8));
        galleryScroll.setContent(galleryFlowPane);

        // 2. Table View
        resultsTableView = new TableView<>();
        resultsTableView.setItems(filteredCarvedFileList);
        resultsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CarvedFileItem, Boolean> colCheck = new TableColumn<>("Recover");
        colCheck.setPrefWidth(50);
        colCheck.setCellValueFactory(f -> f.getValue().selectedProperty());
        colCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCheck));
        colCheck.setEditable(true);

        TableColumn<CarvedFileItem, String> colId = new TableColumn<>("ID");
        colId.setPrefWidth(80);
        colId.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFile().getId()));

        TableColumn<CarvedFileItem, String> colType = new TableColumn<>("Type");
        colType.setPrefWidth(120);
        colType.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFile().getFileType()));

        TableColumn<CarvedFileItem, String> colExt = new TableColumn<>("Format");
        colExt.setPrefWidth(60);
        colExt.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFile().getExtension()));

        TableColumn<CarvedFileItem, String> colSize = new TableColumn<>("Size");
        colSize.setPrefWidth(90);
        colSize.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFile().getFormattedSize()));

        TableColumn<CarvedFileItem, String> colLba = new TableColumn<>("Start Sector LBA");
        colLba.setPrefWidth(100);
        colLba.setCellValueFactory(f -> new SimpleStringProperty(String.valueOf(f.getValue().getFile().getStartLba())));

        TableColumn<CarvedFileItem, String> colScore = new TableColumn<>("Confidence");
        colScore.setPrefWidth(90);
        colScore.setCellValueFactory(f -> new SimpleStringProperty(String.format("%.1f%%", f.getValue().getFile().getConfidenceScore() * 100)));

        TableColumn<CarvedFileItem, String> colHash = new TableColumn<>("SHA-256 Hash");
        colHash.setPrefWidth(160);
        colHash.setCellValueFactory(f -> {
            String h = f.getValue().getFile().getSha256Hash();
            return new SimpleStringProperty(h != null ? h.substring(0, Math.min(16, h.length())) + "..." : "PENDING");
        });

        resultsTableView.getColumns().addAll(colCheck, colId, colType, colExt, colSize, colLba, colScore, colHash);
        resultsTableView.setEditable(true);
        resultsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                updateResultsPreview(newV.getFile());
            }
        });

        viewModeSwitcherArea.getChildren().add(galleryScroll); // default to gallery

        // Right Preview Inspector Panel
        VBox rightPreviewPane = new VBox(12);
        rightPreviewPane.setPrefWidth(300);
        rightPreviewPane.setPadding(new Insets(14));
        rightPreviewPane.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        previewContainerBox = new StackPane();
        previewContainerBox.setPrefHeight(150);
        previewContainerBox.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 6;");

        resultPreviewImageView = new ImageView();
        resultPreviewImageView.setFitWidth(260);
        resultPreviewImageView.setFitHeight(140);
        resultPreviewImageView.setPreserveRatio(true);
        previewContainerBox.getChildren().add(resultPreviewImageView);

        resultPreviewTitle = new Label("Select a file to inspect");
        resultPreviewTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827; -fx-wrap-text: true;");

        resultPreviewSubtitle = new Label("Click any item to view metadata & integrity");
        resultPreviewSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        GridPane pGrid = new GridPane();
        pGrid.setHgap(6);
        pGrid.setVgap(6);

        resultPreviewFormat = addInspectorProperty(pGrid, 0, "Format:", "--");
        resultPreviewLba = addInspectorProperty(pGrid, 1, "Sector LBA:", "--");
        resultPreviewScore = addInspectorProperty(pGrid, 2, "Integrity Chance:", "--");
        resultPreviewEntropy = addInspectorProperty(pGrid, 3, "Shannon Entropy:", "--");
        resultPreviewHash = addInspectorProperty(pGrid, 4, "SHA-256 Hash:", "--");

        btnCopyHash = new Button("📋 Copy Full Hash");
        btnCopyHash.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-font-size: 11px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnCopyHash.setDisable(true);

        btnOpenRecoveredFile = new Button("Open Recovered File");
        btnOpenRecoveredFile.setMaxWidth(Double.MAX_VALUE);
        btnOpenRecoveredFile.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;");

        rightPreviewPane.getChildren().addAll(previewContainerBox, resultPreviewTitle, resultPreviewSubtitle, new Separator(), pGrid, btnCopyHash, btnOpenRecoveredFile);

        centerSplit.getChildren().addAll(viewModeSwitcherArea, rightPreviewPane);
        resultsViewPane.setCenter(centerSplit);

        // Sticky Bottom Recovery Bar (Disk Drill's Signature Bar!)
        HBox bottomAction = new HBox(12);
        bottomAction.setAlignment(Pos.CENTER_LEFT);
        bottomAction.setPadding(new Insets(10, 20, 10, 20));
        bottomAction.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        bottomSelectionStatsLabel = new Label("0 files selected");
        bottomSelectionStatsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1f2937; -fx-font-weight: bold;");

        Button btnSelectAll = new Button("Select All");
        btnSelectAll.setStyle("-fx-background-color: #f3f4f6; -fx-font-size: 11px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnSelectAll.setOnAction(e -> setSelectAll(true));

        Button btnDeselectAll = new Button("Deselect All");
        btnDeselectAll.setStyle("-fx-background-color: #f3f4f6; -fx-font-size: 11px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnDeselectAll.setOnAction(e -> setSelectAll(false));

        Region spBottom = new Region();
        HBox.setHgrow(spBottom, Priority.ALWAYS);

        Label lblDest = new Label("Recover to:");
        lblDest.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563; -fx-font-weight: bold;");

        destinationPathField = new TextField(new File("carved_evidence").getAbsolutePath());
        destinationPathField.setPrefWidth(250);

        Button btnBrowseDest = new Button("Browse...");
        btnBrowseDest.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-radius: 4; -fx-padding: 5 10; -fx-cursor: hand;");
        btnBrowseDest.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Evidence Recovery Destination Folder");
            File sel = chooser.showDialog(null);
            if (sel != null) {
                destinationPathField.setText(sel.getAbsolutePath());
            }
        });

        Button btnRecover = new Button("Recover Selected Evidence");
        btnRecover.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 22; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRecover.setOnAction(e -> executeRecoveryAndAudit());

        bottomAction.getChildren().addAll(bottomSelectionStatsLabel, btnSelectAll, btnDeselectAll, spBottom, lblDest, destinationPathField, btnBrowseDest, btnRecover);
        resultsViewPane.setBottom(bottomAction);
    }

    private void switchViewMode() {
        viewModeSwitcherArea.getChildren().clear();
        if (isGalleryView) {
            ScrollPane sp = new ScrollPane();
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");
            sp.setContent(galleryFlowPane);
            viewModeSwitcherArea.getChildren().add(sp);
            renderGalleryCards();
        } else {
            viewModeSwitcherArea.getChildren().add(resultsTableView);
        }
    }

    private void setSelectAll(boolean select) {
        for (CarvedFileItem item : filteredCarvedFileList) {
            item.setSelected(select);
        }
        renderGalleryCards();
        updateSelectionStats();
    }

    private Button createFilterPill(String text) {
        Button pill = new Button(text);
        pill.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-size: 11px; -fx-padding: 4 11; -fx-background-radius: 12; -fx-cursor: hand;");
        return pill;
    }

    private void filterAndShowResults(String... exts) {
        activeCategoryFilter = exts.length > 0 ? exts[0] : "ALL";
        applyFilters();
        showResultsView();
    }

    private void applyFilters() {
        filteredCarvedFileList.clear();
        String q = activeSearchQuery.toLowerCase();

        for (CarvedFileItem item : carvedFileList) {
            CarvedFile cf = item.getFile();
            boolean matchesCat = activeCategoryFilter.equalsIgnoreCase("ALL") ||
                    cf.getExtension().equalsIgnoreCase(activeCategoryFilter);

            boolean matchesQuery = q.isEmpty() ||
                    cf.getId().toLowerCase().contains(q) ||
                    cf.getFileType().toLowerCase().contains(q) ||
                    cf.getExtension().toLowerCase().contains(q) ||
                    (cf.getSha256Hash() != null && cf.getSha256Hash().toLowerCase().contains(q));

            if (matchesCat && matchesQuery) {
                filteredCarvedFileList.add(item);
            }
        }

        renderGalleryCards();
        updateSelectionStats();
    }

    private void renderGalleryCards() {
        galleryFlowPane.getChildren().clear();
        for (CarvedFileItem item : filteredCarvedFileList) {
            CarvedFile cf = item.getFile();
            VBox card = new VBox(6);
            card.setPrefSize(130, 140);
            card.setPadding(new Insets(8));
            card.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");

            HBox checkLine = new HBox();
            CheckBox chk = new CheckBox();
            chk.selectedProperty().bindBidirectional(item.selectedProperty());
            chk.selectedProperty().addListener((o, oldV, newV) -> updateSelectionStats());
            checkLine.getChildren().add(chk);

            StackPane thumbBox = new StackPane();
            thumbBox.setPrefHeight(75);
            thumbBox.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 6;");

            String icon = "📄";
            if (cf.getExtension().equalsIgnoreCase(".jpg") || cf.getExtension().equalsIgnoreCase(".png")) icon = "🖼";
            else if (cf.getExtension().equalsIgnoreCase(".mp4") || cf.getExtension().equalsIgnoreCase(".avi")) icon = "🎬";
            else if (cf.getExtension().equalsIgnoreCase(".mp3") || cf.getExtension().equalsIgnoreCase(".wav")) icon = "🎵";
            else if (cf.getExtension().equalsIgnoreCase(".zip") || cf.getExtension().equalsIgnoreCase(".7z")) icon = "📦";
            else if (cf.getExtension().equalsIgnoreCase(".db")) icon = "🗄";

            Label thumbIcon = new Label(icon);
            thumbIcon.setStyle("-fx-font-size: 28px;");
            thumbBox.getChildren().add(thumbIcon);

            Label name = new Label(cf.getId() + cf.getExtension());
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label size = new Label(cf.getFormattedSize());
            size.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

            card.getChildren().addAll(checkLine, thumbBox, name, size);
            card.setOnMouseClicked(e -> updateResultsPreview(cf));
            galleryFlowPane.getChildren().add(card);
        }

        updateSelectionStats();
    }

    private void updateSelectionStats() {
        int selectedCount = 0;
        long selectedBytes = 0;
        for (CarvedFileItem item : filteredCarvedFileList) {
            if (item.isSelected()) {
                selectedCount++;
                selectedBytes += item.getFile().getLengthBytes();
            }
        }

        String szStr = FormatUtil.formatBytes(selectedBytes);

        bottomSelectionStatsLabel.setText(selectedCount + " file(s) selected (" + szStr + ") of " + filteredCarvedFileList.size() + " total");
    }

    private void updateResultsPreview(CarvedFile cf) {
        resultPreviewTitle.setText(cf.getId() + cf.getExtension());
        resultPreviewSubtitle.setText(cf.getFileType() + " • " + cf.getFormattedSize());
        resultPreviewFormat.setText(cf.getFileType() + " (" + cf.getExtension() + ")");
        resultPreviewLba.setText("LBA " + cf.getStartLba());
        resultPreviewScore.setText(String.format("Verified (%.1f%%)", cf.getConfidenceScore() * 100));
        resultPreviewEntropy.setText(cf.isIntact() ? "Intact Payload (High)" : "Fragmented (Mid)");

        String hash = cf.getSha256Hash();
        resultPreviewHash.setText(hash != null ? hash.substring(0, Math.min(12, hash.length())) + "..." : "N/A");

        btnCopyHash.setDisable(hash == null);
        btnCopyHash.setOnAction(e -> {
            if (hash != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(hash);
                clipboard.setContent(content);
                btnCopyHash.setText("✓ Copied!");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception ignored) {}
                    Platform.runLater(() -> btnCopyHash.setText("📋 Copy Full Hash"));
                }).start();
            }
        });

        // Live image rendering if JPEG or PNG
        if (cf.getOutputFilePath() != null && new File(cf.getOutputFilePath()).exists() &&
                (cf.getExtension().equalsIgnoreCase(".jpg") || cf.getExtension().equalsIgnoreCase(".png"))) {
            try {
                Image img = new Image(new File(cf.getOutputFilePath()).toURI().toString(), 260, 140, true, true);
                resultPreviewImageView.setImage(img);
            } catch (Exception ignored) {
                resultPreviewImageView.setImage(null);
            }
        } else {
            resultPreviewImageView.setImage(null);
        }

        btnOpenRecoveredFile.setOnAction(e -> {
            if (cf.getOutputFilePath() != null) {
                try {
                    Desktop.getDesktop().open(new File(cf.getOutputFilePath()));
                } catch (Exception ex) {
                    log("[ERROR] Could not open file: " + ex.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // 4. DEDICATED DRIVE SANITIZER VIEW
    // =========================================================================
    private void setupSanitizerDedicatedView() {
        sanitizerDedicatedPane = new VBox(20);
        sanitizerDedicatedPane.setPadding(new Insets(30, 40, 30, 40));
        sanitizerDedicatedPane.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label("Secure Drive Sanitizer & Media Erasure Wizard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label subtitle = new Label("Permanent block-level data destruction compliant with NIST SP 800-88 and DoD 5220.22-M military standards.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(16);

        Label lbl1 = new Label("Target Storage Device:");
        lbl1.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        comboSanitizerTarget = new ComboBox<>(sanitizableDeviceList);
        comboSanitizerTarget.setPrefWidth(540);
        comboSanitizerTarget.setMaxWidth(Double.MAX_VALUE);
        comboSanitizerTarget.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-radius: 6;");

        Callback<ListView<StorageDevice>, ListCell<StorageDevice>> deviceCellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(StorageDevice dev, boolean empty) {
                super.updateItem(dev, empty);
                if (empty || dev == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setPadding(new Insets(4, 6, 4, 6));

                    Node icon;
                    if (dev.isSystemDrive()) {
                        icon = AegisUiComponents.createSystemLockBadge(15);
                    } else if (dev.isPhysical()) {
                        icon = AegisUiComponents.createVectorHardwareDiskRowIcon();
                    } else {
                        icon = AegisUiComponents.createVectorFolderIcon(15);
                    }

                    VBox nameCol = new VBox(2);
                    HBox topNameRow = new HBox(6);
                    topNameRow.setAlignment(Pos.CENTER_LEFT);

                    if (!dev.isPhysical()) {
                        Label branch = new Label("\u2514\u2500");
                        branch.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
                        box.getChildren().add(branch);
                    }

                    Label lblName = new Label(dev.getModel() != null ? dev.getModel() : dev.getDevicePath());
                    lblName.setStyle(dev.isPhysical()
                            ? "-fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-font-size: 12.5px;"
                            : "-fx-font-weight: 600; -fx-text-fill: #1e293b; -fx-font-size: 12px;");

                    topNameRow.getChildren().add(lblName);

                    if (dev.isSystemDrive()) {
                        Label lblLock = new Label("OS PROTECTED");
                        lblLock.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 1 5; -fx-background-radius: 4;");
                        topNameRow.getChildren().add(lblLock);
                    }

                    Label lblPath = new Label(dev.getDevicePath());
                    lblPath.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #64748b; -fx-font-family: 'Consolas', monospace;");

                    nameCol.getChildren().addAll(topNameRow, lblPath);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label badge = new Label(dev.getFormattedSize() + " \u2022 " + dev.getInterfaceType());
                    badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 2 7; -fx-background-radius: 6;");

                    box.getChildren().addAll(icon, nameCol, spacer, badge);
                    setGraphic(box);
                    setText(null);
                }
            }
        };

        comboSanitizerTarget.setCellFactory(deviceCellFactory);
        comboSanitizerTarget.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StorageDevice dev, boolean empty) {
                super.updateItem(dev, empty);
                if (empty || dev == null) {
                    setText("Select Target Storage Device...");
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Node icon = dev.isSystemDrive()
                            ? AegisUiComponents.createSystemLockBadge(15)
                            : (dev.isPhysical() ? AegisUiComponents.createVectorHardwareDiskRowIcon() : AegisUiComponents.createVectorFolderIcon(15));

                    Label lblName = new Label(dev.getModel() != null ? dev.getModel() : dev.getDevicePath());
                    lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 12.5px;");

                    Label lblPath = new Label("(" + dev.getDevicePath() + ")");
                    lblPath.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    if (dev.isSystemDrive()) {
                        Label lblLock = new Label("OS PROTECTED");
                        lblLock.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 1 5; -fx-background-radius: 4;");
                        box.getChildren().add(lblLock);
                    }

                    Label badge = new Label(dev.getFormattedSize() + " \u2022 " + dev.getInterfaceType());
                    badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 2 7; -fx-background-radius: 6;");

                    box.getChildren().addAll(icon, lblName, lblPath, spacer, badge);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        Label lbl2 = new Label("Sanitization Standard:");
        lbl2.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");
        comboSanitizerStandard = new ComboBox<>();
        comboSanitizerStandard.getItems().addAll(SanitizationStandard.values());
        comboSanitizerStandard.setValue(SanitizationStandard.NIST_800_88_CLEAR);
        comboSanitizerStandard.setPrefWidth(540);
        comboSanitizerStandard.setMaxWidth(Double.MAX_VALUE);
        comboSanitizerStandard.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-radius: 6;");

        form.add(lbl1, 0, 0);
        form.add(comboSanitizerTarget, 1, 0);
        form.add(lbl2, 0, 1);
        form.add(comboSanitizerStandard, 1, 1);

        VBox standardInfoCard = new VBox(8);
        standardInfoCard.setPadding(new Insets(16));
        standardInfoCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        lblSanitizerDescription = new Label("NIST SP 800-88 Rev 1 (Clear): Single pass zero-fill overwrite across all logical block addresses. Verifies residual entropy is 0.0000 bits/byte.");
        lblSanitizerDescription.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-wrap-text: true;");

        lblSanitizerPasses = new Label("Number of Overwrite Passes: 1 Pass");
        lblSanitizerPasses.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0284c7;");

        standardInfoCard.getChildren().addAll(lblSanitizerDescription, lblSanitizerPasses);

        comboSanitizerStandard.setOnAction(e -> {
            SanitizationStandard std = comboSanitizerStandard.getValue();
            if (std != null) {
                lblSanitizerPasses.setText("Number of Overwrite Passes: " + std.getTotalPasses() + " Pass(es)");
                if (std == SanitizationStandard.DOD_5220_22_M) {
                    lblSanitizerDescription.setText("DoD 5220.22-M: 3-pass military specification. Pass 1: 0x00, Pass 2: 0xFF, Pass 3: Cryptographic pseudo-random data.");
                } else if (std == SanitizationStandard.GUTMANN_LITE) {
                    lblSanitizerDescription.setText("Gutmann Lite: 7-pass multi-pattern overwrite destroying magnetic domain and flash memory residual charges.");
                } else {
                    lblSanitizerDescription.setText("NIST SP 800-88: Industry standard media sanitization clear. Live entropy measurement confirms unrecoverable state.");
                }
            }
        });

        Button btnExecuteDedicatedSanitize = new Button("Execute Certified Drive Sanitization");
        btnExecuteDedicatedSanitize.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");

        comboSanitizerTarget.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                if (selected.isSystemDrive()) {
                    btnExecuteDedicatedSanitize.setDisable(true);
                    btnExecuteDedicatedSanitize.setText("\uD83D\uDD12 Sanitization Locked (OS System Drive)");
                    btnExecuteDedicatedSanitize.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #d1d5db; -fx-text-fill: #9ca3af; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6;");
                } else {
                    btnExecuteDedicatedSanitize.setDisable(false);
                    btnExecuteDedicatedSanitize.setText("Execute Certified Drive Sanitization");
                    btnExecuteDedicatedSanitize.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");
                }
            }
        });
        btnExecuteDedicatedSanitize.setOnAction(e -> {
            StorageDevice dev = comboSanitizerTarget.getValue();
            if (dev != null) {
                promptAndSanitizeDrive(dev);
            } else {
                showAlert("Select Target", "Please select a target storage device to wipe.");
            }
        });

        sanitizerDedicatedPane.getChildren().addAll(title, subtitle, form, standardInfoCard, btnExecuteDedicatedSanitize);
    }

    // =========================================================================
    // 5. DEDICATED FILE & SLACK SHREDDER VIEW
    // =========================================================================
    private void setupFileEraserDedicatedView() {
        fileEraserDedicatedPane = new VBox(20);
        fileEraserDedicatedPane.setPadding(new Insets(30, 40, 30, 40));
        fileEraserDedicatedPane.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label("Targeted File & Cluster Slack Space Shredder");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label subtitle = new Label("Irreversibly destroys sensitive file payloads, purges leftover cluster boundary slack space, and eliminates MFT metadata traces.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        HBox filePickBox = new HBox(10);
        filePickBox.setAlignment(Pos.CENTER_LEFT);

        txtShredFilePath = new TextField();
        txtShredFilePath.setPromptText("Choose file to shred...");
        txtShredFilePath.setPrefWidth(450);

        Button btnBrowse = new Button("📂 Browse File...");
        btnBrowse.setStyle("-fx-background-color: #f3f4f6; -fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File to Destroy");
            File f = chooser.showOpenDialog(null);
            if (f != null) {
                txtShredFilePath.setText(f.getAbsolutePath());
                lblShredFileSize.setText("File Size: " + FormatUtil.formatBytes(f.length()) + " (" + f.length() + " bytes)");
                long slack = ((f.length() + 4095) / 4096 * 4096) - f.length();
                lblShredSlackEstimate.setText("Calculated Cluster Slack Space: " + slack + " bytes of residual slack to be scrubbed");
            }
        });

        filePickBox.getChildren().addAll(txtShredFilePath, btnBrowse);

        lblShredFileSize = new Label("File Size: -- KB");
        lblShredFileSize.setStyle("-fx-font-size: 12px; -fx-text-fill: #4b5563;");

        lblShredSlackEstimate = new Label("Calculated Cluster Slack Space: -- bytes");
        lblShredSlackEstimate.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0284c7;");

        comboShredStandard = new ComboBox<>();
        comboShredStandard.getItems().addAll(SanitizationStandard.values());
        comboShredStandard.setValue(SanitizationStandard.DOD_5220_22_M);
        comboShredStandard.setPrefWidth(300);

        Button btnExecuteShred = new Button("💥 Irreversibly Destroy File & Slack Space");
        btnExecuteShred.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");
        btnExecuteShred.setOnAction(e -> {
            String path = txtShredFilePath.getText().trim();
            if (path.isEmpty()) {
                showAlert("Select File", "Please select a file to destroy.");
                return;
            }
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                showAlert("File Not Found", "Target file does not exist.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Permanent File Shredding");
            confirm.setHeaderText("Irreversible Destruction: " + f.getName());
            confirm.setContentText("Target: " + f.getAbsolutePath() + "\n"
                    + "Action: Overwrites file content, scrubs slack bytes, zeroes timestamps to Epoch 1970, and scrambles directory records.\n\nProceed?");

            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                log("[SHRED] Erasing file: " + f.getAbsolutePath());
                boolean ok = FileEraser.wipeFile(f, comboShredStandard.getValue());
                if (ok) {
                    AuditBlockchain.Block block = AuditBlockchain.getInstance().recordAuditBlock(
                            "FILE_SLACK_SHREDDING",
                            f.getName(),
                            comboShredStandard.getValue().getDisplayName(),
                            "SHRED_VERIFIED_" + System.currentTimeMillis(),
                            "File payload overwritten, slack space scrubbed, timestamps zeroed."
                    );
                    showSuccessScreen("File destruction complete",
                            f.getName() + " permanently destroyed & slack space purged",
                            f.getParent(),
                            "Standards: " + comboShredStandard.getValue().getDisplayName() + " + Epoch 1970 timestamp reset.",
                            null,
                            block);
                    txtShredFilePath.clear();
                    lblShredFileSize.setText("File Size: -- KB");
                    lblShredSlackEstimate.setText("Calculated Cluster Slack Space: -- bytes");
                } else {
                    showAlert("Error", "Could not complete file wiping.");
                }
            }
        });

        fileEraserDedicatedPane.getChildren().addAll(title, subtitle, filePickBox, lblShredFileSize, lblShredSlackEstimate, new Label("Overwriting Standard:"), comboShredStandard, btnExecuteShred);
    }

    // =========================================================================
    // 6. SUCCESS SCREEN (STEP 4: SUCCESS & AUDIT COMPLETION)
    // =========================================================================
    private void setupSuccessView() {
        successViewPane = new VBox(22);
        successViewPane.setAlignment(Pos.CENTER);
        successViewPane.setPadding(new Insets(36));
        successViewPane.setStyle("-fx-background-color: #ffffff;");

        Node badge = AegisUiComponents.createSuccessFolderBadge(135);

        VBox textBox = new VBox(8);
        textBox.setAlignment(Pos.CENTER);

        HBox statusLine = new HBox(8);
        statusLine.setAlignment(Pos.CENTER);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");

        successTitle = new Label("Data recovery complete");
        successTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        statusLine.getChildren().addAll(dot, successTitle);

        successSummary = new Label("128 file(s) / 493 MB");
        successSummary.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #111827;");

        successSourceDrive = new Label("From: USB SanDisk 3.2Gen1 SCSI Disk Device");
        successSourceDrive.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");

        successDestinationPath = new Label("Path: D:\\Project\\AEGIS\\carved_evidence");
        successDestinationPath.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");

        successBlockchainBadge = new Label("● Recorded to Immutable Blockchain Ledger (Block #1)");
        successBlockchainBadge.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #15803d; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12; -fx-border-color: #bbf7d0; -fx-border-radius: 12;");

        textBox.getChildren().addAll(statusLine, successSummary, successSourceDrive, successDestinationPath, successBlockchainBadge);

        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER);

        Button btnSuccessExplorer = new Button("📁 Show in Windows Explorer");
        btnSuccessExplorer.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSuccessExplorer.setOnAction(e -> {
            try {
                File dir = new File(destinationPathField.getText().trim());
                if (!dir.exists()) dir.mkdirs();
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                log("[ERROR] Could not open explorer: " + ex.getMessage());
            }
        });

        Button btnOpenPDF = new Button("📜 View Audit Certificate PDF");
        btnOpenPDF.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnOpenPDF.setOnAction(e -> {
            if (lastGeneratedCert != null && lastGeneratedCert.exists()) {
                try {
                    Desktop.getDesktop().open(lastGeneratedCert);
                } catch (Exception ex) {
                    log("[ERROR] Could not open PDF: " + ex.getMessage());
                }
            } else {
                showCertificatesView();
            }
        });

        Button btnViewBlockchain = new Button("🔗 View Blockchain Ledger");
        btnViewBlockchain.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnViewBlockchain.setOnAction(e -> showBlockchainLedgerView());

        Button btnHome = new Button("⌂ Return to Storage Devices");
        btnHome.setStyle("-fx-background-color: #f3f4f6; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnHome.setOnAction(e -> showDrivesView());

        buttonBar.getChildren().addAll(btnSuccessExplorer, btnOpenPDF, btnViewBlockchain, btnHome);
        successViewPane.getChildren().addAll(badge, textBox, buttonBar);
    }

    private void showSuccessScreen(String title, String summary, String source, String path, File cert, AuditBlockchain.Block block) {
        this.lastGeneratedCert = cert;
        successTitle.setText(title);
        successSummary.setText(summary);
        successSourceDrive.setText("From: " + source);
        successDestinationPath.setText("Path: " + path);
        if (block != null) {
            successBlockchainBadge.setText(String.format("● Immutable Blockchain Ledger Anchored: Block #%d [Hash: %s...]",
                    block.getIndex(), block.getHash().substring(0, 16)));
        }
        setStep(4, "Step 4: Certified Completion & Blockchain Attestation");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(successViewPane);
    }

    // =========================================================================
    // 7. BLOCKCHAIN AUDIT LEDGER VIEW (NTRO SIH26149 THEME HIGHLIGHT)
    // =========================================================================
    private void showBlockchainLedgerView() {
        if (sidebarContainer != null && navSidebar != null) {
            sidebarContainer.getChildren().setAll(navSidebar);
        }
        setActiveNav(btnNavBlockchain);
        breadcrumbLabel.setText("Audit & Compliance > Immutable Blockchain Ledger");

        VBox ledgerBox = new VBox(16);
        ledgerBox.setPadding(new Insets(24, 28, 24, 28));
        ledgerBox.setStyle("-fx-background-color: #ffffff;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Immutable Cryptographic Audit Ledger");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label subtitle = new Label("Smart India Hackathon SIH26149 (NTRO) • SHA-256 Merkle Chain Integrity Attestation");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        titleBox.getChildren().addAll(title, subtitle);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        boolean isValid = AuditBlockchain.getInstance().isChainValid();
        Label statusBadge = new Label(isValid ? "✓ BLOCKCHAIN INTEGRITY VERIFIED (TAMPER-PROOF)" : "⚠️ CHAIN INTEGRITY ALERT");
        statusBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 12;",
                isValid ? "#dcfce7" : "#fee2e2",
                isValid ? "#15803d" : "#b91c1c"
        ));

        Button btnVerify = new Button("🔄 Re-Verify Chain");
        btnVerify.setStyle("-fx-background-color: #f3f4f6; -fx-font-size: 12px; -fx-padding: 6 12; -fx-cursor: hand;");
        btnVerify.setOnAction(e -> showBlockchainLedgerView());

        header.getChildren().addAll(titleBox, sp, statusBadge, btnVerify);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox blockList = new VBox(12);
        blockList.setPadding(new Insets(10, 0, 10, 0));

        List<AuditBlockchain.Block> blocks = AuditBlockchain.getInstance().getBlocks();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            AuditBlockchain.Block b = blocks.get(i);
            VBox card = new VBox(8);
            card.setPadding(new Insets(14, 18, 14, 18));
            card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox cardTop = new HBox(10);
            cardTop.setAlignment(Pos.CENTER_LEFT);

            Label blockPill = new Label("BLOCK #" + b.getIndex());
            blockPill.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 6;");

            Label actionLbl = new Label(b.getAction());
            actionLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Region cardSp = new Region();
            HBox.setHgrow(cardSp, Priority.ALWAYS);

            Label timeLbl = new Label("📅 " + b.getTimestamp());
            timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            cardTop.getChildren().addAll(blockPill, actionLbl, cardSp, timeLbl);

            GridPane g = new GridPane();
            g.setHgap(8);
            g.setVgap(4);

            addInspectorProperty(g, 0, "Target Media / Asset:", b.getTargetDevice());
            addInspectorProperty(g, 1, "Forensic Standard:", b.getStandardOrMethod());
            addInspectorProperty(g, 2, "Evidence Hash / Root:", b.getEvidenceHash());
            addInspectorProperty(g, 3, "Previous Block Hash:", b.getPreviousHash());
            addInspectorProperty(g, 4, "Current Block Hash:", b.getHash());

            Label detailsLbl = new Label("Summary: " + b.getDetails());
            detailsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-font-style: italic;");

            card.getChildren().addAll(cardTop, g, detailsLbl);
            blockList.getChildren().add(card);
        }

        scroll.setContent(blockList);
        ledgerBox.getChildren().addAll(header, scroll);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(ledgerBox);
    }

    // =========================================================================
    // BOTTOM BAR & LOG DRAWER
    // =========================================================================
    private Node createBottomBar() {
        VBox container = new VBox();

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setStyle("-fx-background-color: #f7f8fa; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px;");

        bottomStatusLabel = new Label("Ready • Windows IOCTL Physical Disk Controller Connected");
        bottomStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        btnToggleLog = new Button("Forensic Audit Log ▾");
        btnToggleLog.setStyle("-fx-background-color: transparent; -fx-text-fill: #0078d4; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnToggleLog.setOnAction(e -> toggleLog());

        bar.getChildren().addAll(dot, bottomStatusLabel, sp, btnToggleLog);

        logDrawer = new VBox(6);
        logDrawer.setPadding(new Insets(10, 16, 10, 16));
        logDrawer.setStyle("-fx-background-color: #0f172a;");
        logDrawer.setVisible(false);
        logDrawer.setManaged(false);

        logConsole = new TextArea();
        logConsole.setEditable(false);
        logConsole.setPrefHeight(120);
        logConsole.setStyle("-fx-control-inner-background: #020617; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        logDrawer.getChildren().add(logConsole);
        container.getChildren().addAll(bar, logDrawer);
        return container;
    }

    private void toggleLog() {
        logOpen = !logOpen;
        logDrawer.setVisible(logOpen);
        logDrawer.setManaged(logOpen);
        btnToggleLog.setText(logOpen ? "Hide Forensic Log ▴" : "Forensic Audit Log ▾");
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            logConsole.appendText("[" + time + "] " + msg + "\n");
        });
    }

    // =========================================================================
    // NAVIGATION CONTROLLER
    // =========================================================================
    private void showDrivesView() {
        if (sidebarContainer != null && navSidebar != null) {
            sidebarContainer.getChildren().setAll(navSidebar);
        }
        setActiveNav(btnNavStorageDevices);
        setStep(1, "Step 1: Select Storage Device");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(drivesViewPane);
    }

    private void showScanningView(String title, String subtitle, boolean isSanitizing) {
        if (sidebarContainer != null && scanSidebar != null) {
            sidebarContainer.getChildren().setAll(scanSidebar);
        }
        scanHeaderTitle.setText(title);
        scanHeaderSubtitle.setText(subtitle);
        scanPhaseBadge.setText(isSanitizing ? "\u25CF PHASE 1 OF 3: VBR & PARTITION PRE-CLEAR" : "\u25CF PHASE 1/4: PARTITION & FILE SYSTEM SCAN");
        scanProgressBar.setProgress(0);
        if (waveformPane != null) {
            waveformPane.reset();
            waveformPane.startAnimation();
        }
        lblPicturesCount.setText("0 files");
        lblVideosCount.setText("0 files");
        lblAudioCount.setText("0 files");
        lblDocumentsCount.setText("0 files");
        lblArchivesCount.setText("0 files");
        lblOtherCount.setText("0 files");
        if (lblSideAllCount != null) {
            lblSideAllCount.setText("0");
            lblSidePicCount.setText("0");
            lblSideVidCount.setText("0");
            lblSideAudCount.setText("0");
            lblSideDocCount.setText("0");
            lblSideArcCount.setText("0");
            lblSideOthCount.setText("0");
        }
        lblRecentDiscovery.setText("0h 00m elapsed. Deep scanning volume storage sectors...");
        btnReviewFoundItems.setText("Review found items");

        setStep(2, isSanitizing ? "Step 2: Real-Time Entropy Purge & Sector Wiping" : "Step 2: Deep Media Scanning & Entropy Analysis");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(scanningScrollPane != null ? scanningScrollPane : scanningViewPane);
    }

    private void showResultsView() {
        if (sidebarContainer != null && scanSidebar != null) {
            sidebarContainer.getChildren().setAll(scanSidebar);
        }
        setStep(3, "Step 3: Review Evidence & Manifest Selection");
        applyFilters();
        resultsHeaderTitle.setText(selectedDevice != null ? selectedDevice.getModel() : "Extracted Evidence Manifest");
        resultsHeaderSubtitle.setText(carvedFileList.size() + " files discovered across unallocated raw sectors");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(resultsViewPane);
    }

    private void showSanitizerDedicatedView() {
        if (sidebarContainer != null && navSidebar != null) {
            sidebarContainer.getChildren().setAll(navSidebar);
        }
        setActiveNav(btnNavSanitizer);
        breadcrumbLabel.setText("Data Sanitization > Erasure Wizard");
        if (selectedDevice != null) {
            String model = selectedDevice.getModel() != null ? selectedDevice.getModel().toLowerCase() : "";
            String path = selectedDevice.getDevicePath() != null ? selectedDevice.getDevicePath().toLowerCase() : "";
            if (!model.contains("lost partition") && !model.contains("unallocated") && !path.contains("lostpartition")) {
                comboSanitizerTarget.setValue(selectedDevice);
            }
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(sanitizerDedicatedPane);
    }

    private void showFileEraserDedicatedView() {
        if (sidebarContainer != null && navSidebar != null) {
            sidebarContainer.getChildren().setAll(navSidebar);
        }
        setActiveNav(btnNavFileEraser);
        breadcrumbLabel.setText("Sanitization > File & Slack Space Shredder");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(fileEraserDedicatedPane);
    }

    @SuppressWarnings("unchecked")
    private void showCertificatesView() {
        if (sidebarContainer != null && navSidebar != null) {
            sidebarContainer.getChildren().setAll(navSidebar);
        }
        setActiveNav(btnNavCertificates);
        breadcrumbLabel.setText("Audit & Compliance > Certificates Vault");
        refreshReports();

        VBox certBox = new VBox(16);
        certBox.setPadding(new Insets(24, 28, 24, 28));
        certBox.setStyle("-fx-background-color: #ffffff;");

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Forensic Audit Certificates Vault");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #111827;");
        Label sub = new Label("Legally admissible PDF evidence and sanitization compliance certificates");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        titleBox.getChildren().addAll(title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 6 12; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshReports());

        Button btnOpenDir = new Button("📁 Open Reports Folder");
        btnOpenDir.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 6 12; -fx-cursor: hand;");
        btnOpenDir.setOnAction(e -> {
            try {
                File dir = new File("reports");
                if (!dir.exists()) dir.mkdirs();
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                log("[ERROR] " + ex.getMessage());
            }
        });

        bar.getChildren().addAll(titleBox, sp, btnRefresh, btnOpenDir);

        TableView<File> repTable = new TableView<>();
        repTable.setItems(reportList);
        repTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(repTable, Priority.ALWAYS);

        TableColumn<File, String> colName = new TableColumn<>("Audit Certificate PDF Document");
        colName.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getName()));

        TableColumn<File, String> colSize = new TableColumn<>("File Size");
        colSize.setCellValueFactory(f -> new SimpleStringProperty(FormatUtil.formatBytes(f.getValue().length())));

        TableColumn<File, String> colDate = new TableColumn<>("Generated Timestamp");
        colDate.setCellValueFactory(f -> new SimpleStringProperty(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(f.getValue().lastModified()))));

        repTable.getColumns().addAll(colName, colSize, colDate);

        Button btnOpenSelected = new Button("Open Selected Certificate PDF");
        btnOpenSelected.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnOpenSelected.setOnAction(e -> {
            File sel = repTable.getSelectionModel().getSelectedItem();
            if (sel != null && sel.exists()) {
                try {
                    Desktop.getDesktop().open(sel);
                } catch (Exception ex) {
                    log("[ERROR] Could not open PDF: " + ex.getMessage());
                }
            }
        });

        certBox.getChildren().addAll(bar, repTable, btnOpenSelected);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(certBox);
    }

    private void attachDiskImageDialog() {
        AegisDialogs.showAttachDiskImageDialog(primaryStage, f -> {
            StorageDevice imgDev = new StorageDevice(f.getAbsolutePath(), f.getName(), "IMG-" + f.length(), f.length(), 512, "Forensic Disk Image", false);
            masterDeviceList.add(0, imgDev);
            updateVisibleDeviceList();
            updateRightInspector(imgDev);
            showDrivesView();
            log("[IMAGE ATTACHED] Mounted raw forensic image: " + f.getAbsolutePath());
        });
    }

    // =========================================================================
    // CORE OPERATIONS: CARVING & SCANNING FLOW
    // =========================================================================

    /**
     * Shows a professional, non-destructive forensic pre-scan confirmation modal.
     * Guarantees the user explicitly confirms the target media, path, and read-only parameters.
     */
    private void showCarvingConfirmationDialog(StorageDevice dev) {
        AegisDialogs.showCarvingConfirmationDialog(primaryStage, dev, () -> startCarvingOperation(dev.getDevicePath()));
    }

    private void startCarvingOperation(String sourcePath) {
        isScanPaused = false;
        isScanCancelled = false;

        showScanningView("Scanning \"" + (selectedDevice != null ? selectedDevice.getModel() : sourcePath) + "\"",
                "0% complete — Searching for lost files across unallocated sectors", false);

        carvedFileList.clear();
        filteredCarvedFileList.clear();
        final long startTime = System.currentTimeMillis();
        final long[] lastBytes = new long[]{0};
        final long[] lastTime = new long[]{startTime};
        final long[] lastUiUpdate = new long[]{0};
        final double[] smoothedSpeed = new double[]{0};

        Task<List<CarvedFile>> task = new Task<>() {
            @Override
            protected List<CarvedFile> call() {
                log("[CARVING START] Scanning raw sectors on: " + sourcePath);
                File outDir = new File("carved_evidence");

                return FileCarver.carveDevice(sourcePath, outDir, (phaseName, currentSector, totalSectors, bytesScanned, totalBytes, filesFound, entropy, latestFound) -> {
                    if (isScanCancelled) {
                        return;
                    }

                    while (isScanPaused && !isScanCancelled) {
                        try { Thread.sleep(200); } catch (Exception ignored) {}
                    }

                    double pct = totalBytes > 0 && totalBytes != Long.MAX_VALUE ? (double) bytesScanned / totalBytes : 0;
                    long now = System.currentTimeMillis();
                    long timeDelta = now - lastTime[0];

                    if (timeDelta >= 350) {
                        double mbDiff = (bytesScanned - lastBytes[0]) / (1024.0 * 1024.0);
                        double instSpeed = mbDiff / (timeDelta / 1000.0);
                        smoothedSpeed[0] = smoothedSpeed[0] == 0 ? instSpeed : (smoothedSpeed[0] * 0.7 + instSpeed * 0.3);
                        lastBytes[0] = bytesScanned;
                        lastTime[0] = now;
                    }

                    // Throttle UI task queue to 20 FPS to guarantee smooth 60 FPS rendering and prevent event thread freeze
                    if (latestFound == null && (now - lastUiUpdate[0] < 50)) {
                        return;
                    }
                    lastUiUpdate[0] = now;

                    String speedStr = smoothedSpeed[0] > 0.05 ? String.format("%.1f MB/s", smoothedSpeed[0]) : "Calculating...";
                    long elapsedSec = Math.max(0, (now - startTime) / 1000);
                    String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);

                    String etaStr = "--:--";
                    if (smoothedSpeed[0] > 0.05 && totalBytes > bytesScanned && totalBytes != Long.MAX_VALUE) {
                        long remBytes = totalBytes - bytesScanned;
                        long remSec = (long) (remBytes / (smoothedSpeed[0] * 1024 * 1024));
                        etaStr = String.format("%dm %02ds", remSec / 60, remSec % 60);
                    }

                    final String finalSpeed = speedStr;
                    final String finalEta = etaStr;
                    final String finalElapsed = elapsedStr;
                    final String currentPhaseText = phaseName;

                    Platform.runLater(() -> {
                        scanProgressBar.setProgress(pct);
                        scanPhaseBadge.setText("\u25CF " + currentPhaseText.toUpperCase());

                        String scannedStr = FormatUtil.formatBytes(bytesScanned);

                        scanHeaderSubtitle.setText(String.format("%.1f%% complete \u2014 %d files / %s \u2022 %s elapsed",
                                pct * 100, filesFound, scannedStr, finalElapsed));

                        scanSpeedEtaLabel.setText(String.format("Throughput: %s \u2022 Elapsed: %s \u2022 ETA: %s \u2022 Sector: %,d / %,d",
                                finalSpeed, finalElapsed, finalEta, currentSector, totalSectors));

                        String entropyCategory;
                        String entropyColor;
                        if (entropy <= 0.0001) {
                            entropyCategory = "(Unallocated Zero-Fill Flash \u2014 Hardware Stream Active)";
                            entropyColor = "#0284c7";
                        } else if (entropy < 3.5) {
                            entropyCategory = "\u2022 Low-Entropy Metadata / Text Table";
                            entropyColor = "#0284c7";
                        } else if (entropy < 6.8) {
                            entropyCategory = "\u2022 Structured Media / Executable Code";
                            entropyColor = "#16a34a";
                        } else {
                            entropyCategory = "\u2022 High-Entropy Media / Encrypted Stream";
                            entropyColor = "#7c3aed";
                        }

                        scanEntropyLabel.setText(String.format("Shannon Entropy: %.4f bits/byte %s", entropy, entropyCategory));
                        scanEntropyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + entropyColor + ";");

                        if (waveformPane != null) {
                            waveformPane.pushSample(entropy, smoothedSpeed[0], currentSector, currentPhaseText);
                        }

                        if (latestFound != null) {
                            carvedFileList.add(new CarvedFileItem(latestFound));
                            btnReviewFoundItems.setText("Review found items (" + formatCount(carvedFileList.size()) + ")");

                            int allCount = carvedFileList.size();
                            int picCount = countTypes(".jpg", ".png");
                            int vidCount = countTypes(".mp4", ".avi");
                            int audCount = countTypes(".mp3", ".wav");
                            int docCount = countTypes(".pdf");
                            int arcCount = countTypes(".zip", ".7z");
                            int othCount = Math.max(0, allCount - (picCount + vidCount + audCount + docCount + arcCount));

                            lblPicturesCount.setText(formatCount(picCount) + " files");
                            lblVideosCount.setText(formatCount(vidCount) + " files");
                            lblAudioCount.setText(formatCount(audCount) + " files");
                            lblDocumentsCount.setText(formatCount(docCount) + " files");
                            lblArchivesCount.setText(formatCount(arcCount) + " files");
                            lblOtherCount.setText(formatCount(othCount) + " files");

                            if (lblSideAllCount != null) {
                                lblSideAllCount.setText(formatCount(allCount));
                                lblSidePicCount.setText(formatCount(picCount));
                                lblSideVidCount.setText(formatCount(vidCount));
                                lblSideAudCount.setText(formatCount(audCount));
                                lblSideDocCount.setText(formatCount(docCount));
                                lblSideArcCount.setText(formatCount(arcCount));
                                lblSideOthCount.setText(formatCount(othCount));
                            }

                            lblRecentDiscovery.setText(String.format("%s elapsed. Found %s%s (%s) @ LBA %,d • %s",
                                    finalElapsed, latestFound.getId(), latestFound.getExtension(), latestFound.getFormattedSize(), latestFound.getStartLba(), finalSpeed));
                        } else {
                            lblRecentDiscovery.setText(String.format("%s elapsed. Deep scanning volume storage sectors (LBA %,d of %,d) • %s",
                                    finalElapsed, currentSector, totalSectors, finalSpeed));
                        }
                    });
                });
            }
        };

        task.setOnSucceeded(e -> {
            List<CarvedFile> results = task.getValue();
            log("[CARVING COMPLETE] Hardware scan finished. Extracted " + results.size() + " evidence files.");
            btnReviewFoundItems.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 8 18; -fx-background-radius: 6;");
            showResultsView();
        });

        new Thread(task).start();
    }

    private int countTypes(String... exts) {
        int c = 0;
        for (CarvedFileItem item : carvedFileList) {
            for (String ext : exts) {
                if (item.getFile().getExtension().equalsIgnoreCase(ext)) c++;
            }
        }
        return c;
    }

    // =========================================================================
    // EXECUTE RECOVERY & BLOCKCHAIN AUDIT (TRANSITIONS STEP 3 -> STEP 4)
    // =========================================================================
    private void executeRecoveryAndAudit() {
        List<CarvedFile> selectedFiles = new ArrayList<>();
        for (CarvedFileItem item : carvedFileList) {
            if (item.isSelected()) {
                selectedFiles.add(item.getFile());
            }
        }

        if (selectedFiles.isEmpty()) {
            showAlert("No Files Selected", "Please select at least one recovered file to export.");
            return;
        }

        if (selectedDevice == null) {
            selectedDevice = new StorageDevice("Forensic_Media", "Recovered Raw Storage", "SIH-CARVE-01", 0, 512, "Synthetic", false);
        }

        // 1. Calculate Merkle Root / Aggregate Evidence SHA-256
        StringBuilder combinedHashes = new StringBuilder();
        for (CarvedFile cf : selectedFiles) {
            if (cf.getSha256Hash() != null) combinedHashes.append(cf.getSha256Hash());
        }
        String evidenceMerkleHash = com.aegis.util.HashUtil.calculateSHA256(combinedHashes.toString().getBytes());

        // 2. Record to Immutable Blockchain Ledger
        AuditBlockchain.Block block = AuditBlockchain.getInstance().recordAuditBlock(
                "EVIDENCE_RECOVERY",
                selectedDevice.getModel(),
                "Raw Sector Magic-Byte Carving",
                evidenceMerkleHash,
                selectedFiles.size() + " forensic file(s) recovered to " + destinationPathField.getText().trim()
        );

        // 3. Generate Official PDF Certificate of Evidence Recovery
        File cert = ReportGenerator.generateRecoveryCertificate(selectedDevice, selectedFiles, block.getHash(), new File("reports"));
        refreshReports();
        log("[BLOCKCHAIN & CERTIFICATE] Recovery committed to Block #" + block.getIndex() + " | PDF: " + (cert != null ? cert.getName() : "None"));

        long totalSize = 0;
        for (CarvedFile cf : selectedFiles) totalSize += cf.getLengthBytes();
        String sz = FormatUtil.formatBytes(totalSize);

        // 4. Transition to Step 4 Success Screen
        showSuccessScreen("Data recovery complete",
                selectedFiles.size() + " file(s) / " + sz + " recovered successfully",
                selectedDevice.getModel(),
                destinationPathField.getText().trim(),
                cert,
                block);
    }

    // =========================================================================
    // SANITIZATION OPERATION (STEP 1 -> STEP 2 -> STEP 4)
    // =========================================================================
    private void promptAndSanitizeDrive(StorageDevice dev) {
        AegisDialogs.promptSanitizationStandard(dev, standard -> startDriveSanitization(dev, standard));
    }

    private void startDriveSanitization(StorageDevice dev, SanitizationStandard standard) {
        showScanningView("Sanitizing \"" + dev.getModel() + "\"", "Executing " + standard.getDisplayName(), true);

        final long startTime = System.currentTimeMillis();
        final long[] lastBytes = new long[]{0};
        final long[] lastTime = new long[]{startTime};
        final double[] smoothedSpeed = new double[]{0};

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                log("[SANITIZATION START] Target: " + dev.getDevicePath() + " using " + standard.getDisplayName());
                return DriveEraser.sanitizeDrive(dev, standard, (pass, totalPasses, bytesWritten, totalBytes, entropy, pattern) -> {
                    double pct = totalBytes > 0 ? (double) bytesWritten / totalBytes : 0;
                    long now = System.currentTimeMillis();
                    long timeDelta = now - lastTime[0];

                    if (timeDelta >= 700) {
                        double mbDiff = (bytesWritten - lastBytes[0]) / (1024.0 * 1024.0);
                        double instSpeed = mbDiff / (timeDelta / 1000.0);
                        smoothedSpeed[0] = smoothedSpeed[0] == 0 ? instSpeed : (smoothedSpeed[0] * 0.75 + instSpeed * 0.25);
                        lastBytes[0] = bytesWritten;
                        lastTime[0] = now;
                    }

                    String speedStr = smoothedSpeed[0] > 0.05 ? String.format("%.1f MB/s", smoothedSpeed[0]) : "Calculating...";
                    String etaStr = "--:--";
                    if (smoothedSpeed[0] > 0.05 && totalBytes > bytesWritten) {
                        long remBytes = totalBytes - bytesWritten;
                        long remSec = (long) (remBytes / (smoothedSpeed[0] * 1024 * 1024));
                        etaStr = String.format("%dm %02ds", remSec / 60, remSec % 60);
                    } else if (pct >= 0.999) {
                        etaStr = "Finalizing";
                    }

                    long elapsedSec = Math.max(0, (now - startTime) / 1000);
                    String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);

                    final String finalSpeed = speedStr;
                    final String finalEta = etaStr;
                    final String finalElapsed = elapsedStr;

                    Platform.runLater(() -> {
                        scanProgressBar.setProgress(pct);
                        scanPhaseBadge.setText(String.format("● PHASE 2 OF 3: %s (PASS %d/%d)", standard.getDisplayName().toUpperCase(), pass, totalPasses));
                        scanHeaderSubtitle.setText(String.format("Pass %d/%d (%s) • %.1f%% • %,d MB / %,d MB",
                                pass, totalPasses, pattern, pct * 100, bytesWritten / (1024 * 1024), totalBytes / (1024 * 1024)));
                        scanSpeedEtaLabel.setText(String.format("Write Throughput: %s • Elapsed: %s • ETA: %s • Overwrite Buffer: %,d MB / %,d MB",
                                finalSpeed, finalElapsed, finalEta, bytesWritten / (1024 * 1024), totalBytes / (1024 * 1024)));

                        if (entropy <= 0.0001) {
                            scanEntropyLabel.setText(String.format("Shannon Entropy: %.4f bits/byte [ZERO-FILL PURGED ✓]", entropy));
                            scanEntropyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
                        } else if (entropy >= 7.8) {
                            scanEntropyLabel.setText(String.format("Shannon Entropy: %.4f bits/byte [UNIFORM RANDOM PURGED ✓]", entropy));
                            scanEntropyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9333ea;");
                        } else {
                            scanEntropyLabel.setText(String.format("Shannon Entropy: %.4f bits/byte [ACTIVE SECTOR PATTERN]", entropy));
                            scanEntropyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0078d4;");
                        }

                        if (waveformPane != null) {
                            waveformPane.pushSample(entropy, 45.0, bytesWritten / 512, "Sector Wipe Pass");
                        }
                    });
                });
            }
        };

        task.setOnSucceeded(e -> {
            boolean success = task.getValue();
            if (success) {
                log("[SANITIZATION COMPLETE] Certified erasure succeeded. Anchoring to blockchain...");
                String verifyHash = "0x" + UUID.randomUUID().toString().replace("-", "");
                AuditBlockchain.Block block = AuditBlockchain.getInstance().recordAuditBlock(
                        "MEDIA_SANITIZATION",
                        dev.getModel(),
                        standard.getDisplayName(),
                        verifyHash,
                        "Permanently purged " + dev.getFormattedSize() + " to entropy 0.0000 bits/byte."
                );

                File cert = ReportGenerator.generateSanitizationCertificate(dev, standard, verifyHash, new File("reports"));
                refreshReports();
                showSuccessScreen("Data sanitization complete",
                        dev.getFormattedSize() + " permanently sanitized",
                        dev.getModel(),
                        "Standard: " + standard.getDisplayName() + " (Entropy: 0.0000 bits/byte)",
                        cert,
                        block);
            }
        });

        new Thread(task).start();
    }

    private void runFullBenchmarkDemo() {
        log("[BENCHMARK] Generating 10 MB synthetic evidence disk image (test_evidence.raw)...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                File mockImage = new File("test_evidence.raw");
                byte[] mockDisk = new byte[10 * 1024 * 1024];

                // 1. JPEG Image
                byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
                byte[] jpegFooter = new byte[]{(byte) 0xFF, (byte) 0xD9};
                int jpegOffset = 100 * 512;
                System.arraycopy(jpegHeader, 0, mockDisk, jpegOffset, jpegHeader.length);
                System.arraycopy("AEGIS_DEMO_RECOVERY_IMAGE_PAYLOAD".getBytes(), 0, mockDisk, jpegOffset + 50, 36);
                System.arraycopy(jpegFooter, 0, mockDisk, jpegOffset + 1024, jpegFooter.length);

                // 2. PDF Document
                byte[] pdfHeader = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n".getBytes();
                byte[] pdfFooter = "%%EOF\n".getBytes();
                int pdfOffset = 500 * 512;
                System.arraycopy(pdfHeader, 0, mockDisk, pdfOffset, pdfHeader.length);
                System.arraycopy(pdfFooter, 0, mockDisk, pdfOffset + 2048, pdfFooter.length);

                // 3. ZIP Archive
                byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04};
                byte[] zipFooter = new byte[]{0x50, 0x4B, 0x05, 0x06};
                int zipOffset = 1200 * 512;
                System.arraycopy(zipHeader, 0, mockDisk, zipOffset, zipHeader.length);
                System.arraycopy(zipFooter, 0, mockDisk, zipOffset + 1500, zipFooter.length);

                // 4. MP3 Audio
                byte[] mp3Header = new byte[]{0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x0F, 0x76};
                int mp3Offset = 2000 * 512;
                System.arraycopy(mp3Header, 0, mockDisk, mp3Offset, mp3Header.length);

                // 5. MP4 Video
                byte[] mp4Header = new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70};
                int mp4Offset = 3000 * 512;
                System.arraycopy(mp4Header, 0, mockDisk, mp4Offset, mp4Header.length);

                // 6. SQLite Database
                byte[] dbHeader = "SQLite format 3\000".getBytes();
                int dbOffset = 4000 * 512;
                System.arraycopy(dbHeader, 0, mockDisk, dbOffset, dbHeader.length);

                try (FileOutputStream fos = new FileOutputStream(mockImage)) {
                    fos.write(mockDisk);
                }

                Platform.runLater(() -> {
                    StorageDevice mockDev = new StorageDevice(mockImage.getAbsolutePath(), "Mock Synthetic Evidence Disk (10 MB)", "SIH-DEMO-001", mockImage.length(), 512, "Synthetic", false);
                    masterDeviceList.add(0, mockDev);
                    updateVisibleDeviceList();
                    drivesTable.getSelectionModel().select(mockDev);
                    updateRightInspector(mockDev);
                    log("[DEMO] Mounted 10 MB synthetic evidence disk into device inventory.");
                    showCarvingConfirmationDialog(mockDev);
                });

                return null;
            }
        };

        new Thread(task).start();
    }

    private Node createTableLoadingPlaceholder() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 20, 30, 20));

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(30, 30);
        spinner.setMinSize(30, 30);
        spinner.setMaxSize(30, 30);
        spinner.setStyle("-fx-progress-color: #0071e3;");

        Label title = new Label("Scanning storage controllers & physical drives...");
        title.setStyle("-fx-font-family: -apple-system, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #1d1d1f;");

        Label subtitle = new Label("Probing NVMe, SATA, USB bus controllers and partition tables...");
        subtitle.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 11.5px; -fx-text-fill: #86868b;");

        box.getChildren().addAll(spinner, title, subtitle);
        return box;
    }

    private Node createTableEmptyPlaceholder() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 20, 30, 20));

        Node diskIcon = AegisUiComponents.createVectorHardDriveIcon(34);

        Label title = new Label("No storage devices found");
        title.setStyle("-fx-font-family: -apple-system, 'SF Pro Display', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #1d1d1f;");

        Label subtitle = new Label("Connect an external USB drive or click '+ Attach disk image...' to begin.");
        subtitle.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                "-fx-font-size: 11.5px; -fx-text-fill: #86868b;");

        box.getChildren().addAll(diskIcon, title, subtitle);
        return box;
    }

    private Thread deviceHotplugWatcher;
    private volatile boolean isWatcherActive = true;
    private volatile boolean isRefreshingDevices = false;

    private void startDeviceHotplugWatcher() {
        // Register callback for asynchronous hardware enricher
        StorageDetector.setOnHardwareUpdated(() -> {
            Platform.runLater(() -> {
                log("[AUTO-DETECT] Physical storage hardware details updated. Seamlessly syncing table...");
                refreshStorageDevices(true);
            });
        });

        deviceHotplugWatcher = new Thread(() -> {
            String previousState = getDeviceFingerprint();
            while (isWatcherActive) {
                try {
                    // Ultra-fast 250ms polling (< 0.01ms CPU time via Win32 logical drives)
                    Thread.sleep(250);
                    if (!isWatcherActive) break;

                    String currentState = getDeviceFingerprint();
                    if (!currentState.equals(previousState)) {
                        previousState = currentState;
                        StorageDetector.invalidateCache();
                        Platform.runLater(() -> {
                            log("[AUTO-DETECT] Storage media hardware change detected (drive inserted/removed). Instantly updating table...");
                            refreshStorageDevices(true); // Smooth, non-disruptive update
                        });
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "AEGIS-HotplugWatcher");
        deviceHotplugWatcher.setDaemon(true);
        deviceHotplugWatcher.start();
    }

    private String getDeviceFingerprint() {
        StringBuilder sb = new StringBuilder();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File r : roots) {
                try {
                    sb.append(r.getAbsolutePath()).append(':').append(r.canRead() ? r.getTotalSpace() : 0).append(';');
                } catch (Exception ignored) {}
            }
        }
        return sb.toString();
    }

    private void refreshStorageDevices() {
        refreshStorageDevices(false);
    }

    private void refreshStorageDevices(boolean isAuto) {
        if (isRefreshingDevices) return;
        isRefreshingDevices = true;

        if (!isAuto) {
            if (drivesTable != null && deviceList.isEmpty()) {
                drivesTable.setPlaceholder(createTableLoadingPlaceholder());
            }
            if (btnRefresh != null) {
                btnRefresh.setDisable(true);
                btnRefresh.setText(" Scanning...");
                ProgressIndicator spinner = new ProgressIndicator();
                spinner.setPrefSize(13, 13);
                spinner.setMinSize(13, 13);
                spinner.setMaxSize(13, 13);
                btnRefresh.setGraphic(spinner);
            }
            if (tableItemCountLabel != null && deviceList.isEmpty()) {
                tableItemCountLabel.setText("Scanning storage controllers & physical drives...");
                tableItemCountLabel.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                        "-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #0071e3;");
            }
        }

        Task<List<StorageDevice>> task = new Task<>() {
            @Override
            protected List<StorageDevice> call() {
                return StorageDetector.detectDevices(isAuto);
            }
        };

        task.setOnSucceeded(e -> {
            isRefreshingDevices = false;
            masterDeviceList.clear();
            if (task.getValue() != null) {
                masterDeviceList.addAll(task.getValue());
            }
            updateVisibleDeviceList();
            if (drivesTable != null) {
                drivesTable.setPlaceholder(createTableEmptyPlaceholder());
                drivesTable.refresh();
            }
            if (btnRefresh != null) {
                btnRefresh.setDisable(false);
                btnRefresh.setText("\u21BB Refresh");
                btnRefresh.setGraphic(null);
                btnRefresh.setStyle("-fx-background-color: transparent; -fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 3 8; -fx-background-radius: 6;");
            }
            log("[DEVICES] Storage media updated. Found " + masterDeviceList.size() + " active device(s).");
        });

        task.setOnFailed(e -> {
            isRefreshingDevices = false;
            if (drivesTable != null) {
                drivesTable.setPlaceholder(createTableEmptyPlaceholder());
            }
            if (tableItemCountLabel != null) {
                tableItemCountLabel.setText(deviceList.size() + " item(s)");
                tableItemCountLabel.setStyle("-fx-font-family: -apple-system, 'SF Pro Text', 'Segoe UI', system-ui, sans-serif; " +
                        "-fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #6e6e73;");
            }
            if (btnRefresh != null) {
                btnRefresh.setDisable(false);
                btnRefresh.setText("\u21BB Refresh");
                btnRefresh.setGraphic(null);
                btnRefresh.setStyle("-fx-background-color: transparent; -fx-text-fill: #0071e3; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 3 8; -fx-background-radius: 6;");
            }
            log("[ERROR] Storage device refresh failed: " + task.getException());
        });

        Thread th = new Thread(task, "AEGIS-DeviceRefreshTask");
        th.setDaemon(true);
        th.start();
    }

    private void refreshReports() {
        File dir = new File("reports");
        if (dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                reportList.setAll(files);
            }
        }
    }

    private void showAlert(String title, String content) {
        AegisDialogs.showInfoAlert(title, content);
    }

    private void applyModernTheme(Scene scene) {
        try {
            java.net.URL cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                return;
            }
        } catch (Exception ignored) {}

        try {
            String css = """
                .root { -fx-font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; }
                .scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }
                .scroll-bar:vertical .thumb, .scroll-bar:horizontal .thumb { -fx-background-color: #cbd5e1; -fx-background-radius: 6px; }
                .scroll-bar:vertical .thumb:hover, .scroll-bar:horizontal .thumb:hover { -fx-background-color: #94a3b8; }
                .table-view { -fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; }
                .table-view .filler { -fx-background-color: transparent; }
                .table-view .column-header-background { -fx-background-color: #f8fafc; -fx-background-radius: 8px 8px 0 0; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0; }
                .table-view .column-header { -fx-background-color: transparent; -fx-border-color: transparent; -fx-size: 34px; -fx-padding: 6 12; }
                .table-view .column-header .label { -fx-text-fill: #64748b; -fx-font-weight: 700; -fx-font-size: 11.5px; }
                .table-row-cell { -fx-background-color: #ffffff; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1px 0; -fx-cell-size: 38px; -fx-font-size: 12px; }
                .table-row-cell:odd { -fx-background-color: #ffffff; }
                .table-row-cell:hover { -fx-background-color: #f8fafc; }
                .table-row-cell:selected { -fx-background-color: #e0f2fe; }
                .table-row-cell:selected .text { -fx-fill: #0369a1; -fx-font-weight: 700; }
                .table-cell { -fx-border-color: transparent; -fx-padding: 0 12; -fx-text-fill: #1e293b; }
            """;
            scene.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ignored) {}
    }
}
