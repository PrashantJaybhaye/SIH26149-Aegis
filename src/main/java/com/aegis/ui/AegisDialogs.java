package com.aegis.ui;

import com.aegis.model.SanitizationStandard;
import com.aegis.model.StorageDevice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Centralized modal dialogs and alert popups for the AEGIS application.
 * Decouples modal construction, safety notices, and styling from main views.
 */
public final class AegisDialogs {

    private AegisDialogs() {
        // Prevent instantiation
    }

    /**
     * Standardized information popup.
     */
    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Standardized error alert popup.
     */
    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays the complete AEGIS architectural and compliance documentation modal.
     */
    public static void showHelpDocumentationDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AEGIS Documentation");
        alert.setHeaderText("AEGIS - Integrated Data Erasure & File Recovery Tool");
        alert.setContentText("AEGIS implements direct low-level block I/O with NIST SP 800-88 Rev. 1 compliance, cryptographic sanitization, and deep signature recovery.\n\n" +
                "• Step 1: Select Storage Device or Attach Raw Disk Image\n" +
                "• Step 2: High-speed Raw Stream Carving & Shannon Entropy Waveform\n" +
                "• Step 3: Deep Evidence Manifest Inspection & Preview\n" +
                "• Step 4: Cryptographic SHA-256 Audit & Blockchain Attestation");
        alert.showAndWait();
    }

    /**
     * Opens a file chooser to attach a raw forensic disk image (.raw, .dd, .img).
     */
    public static void showAttachDiskImageDialog(Stage owner, Consumer<File> onFileSelected) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach Forensic Disk Image (.raw, .dd, .img)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Forensic Disk Images (*.raw, *.dd, *.img)", "*.raw", "*.dd", "*.img", "*.*")
        );
        File file = chooser.showOpenDialog(owner);
        if (file != null && onFileSelected != null) {
            onFileSelected.accept(file);
        }
    }

    /**
     * Shows a non-destructive forensic pre-scan confirmation modal.
     */
    public static void showCarvingConfirmationDialog(Stage owner, StorageDevice dev, Runnable onConfirm) {
        if (dev == null) {
            showInfoAlert("No Device Selected", "Please select a storage drive from the list first.");
            return;
        }

        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Confirm Forensic Evidence Carving");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setPrefWidth(500);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', system-ui, sans-serif;");

        // Header with verification badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Node shieldIcon = AegisUiComponents.createVectorShieldIcon(30);
        VBox titleBox = new VBox(2);
        Label title = new Label("Confirm Forensic Evidence Carving");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1d1d1f;");
        Label subtitle = new Label("Review target media and forensic scan parameters before proceeding.");
        subtitle.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #86868b;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(shieldIcon, titleBox);

        // Target Media Card
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        HBox targetRow = new HBox(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        Node diskIcon = AegisUiComponents.createVectorHardwareDiskRowIcon();
        String devName = dev.getModel() != null && !dev.getModel().isEmpty() ? dev.getModel() : dev.getDevicePath();
        Label driveName = new Label(devName);
        driveName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        targetRow.getChildren().addAll(diskIcon, driveName);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));

        grid.add(createPropLabel("Target Path:"), 0, 0);
        grid.add(createValLabel(dev.getDevicePath()), 1, 0);

        grid.add(createPropLabel("Total Capacity:"), 0, 1);
        grid.add(createValLabel(dev.getFormattedSize()), 1, 1);

        grid.add(createPropLabel("Connection / FS:"), 0, 2);
        grid.add(createValLabel(dev.getInterfaceType()), 1, 2);

        grid.add(createPropLabel("Access Mode:"), 0, 3);
        Label readOnlyBadge = new Label("✔ Non-Destructive Direct Read (GENERIC_READ)");
        readOnlyBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #16a34a;");
        grid.add(readOnlyBadge, 1, 3);

        card.getChildren().addAll(targetRow, new Separator(), grid);

        // Forensic Safety Explanation
        Label notice = new Label("AEGIS will scan raw unallocated sectors in-memory using Win32 direct block streaming. " +
                "Zero bytes will be written or modified on the source media.");
        notice.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-wrap-text: true; -fx-line-spacing: 2px;");

        // Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(6, 0, 0, 0));

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 7 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnConfirm = new Button("Start Evidence Carving");
        btnConfirm.setStyle("-fx-background-color: #0071e3; -fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 7 18; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 113, 227, 0.25), 6, 0, 0, 2);");
        btnConfirm.setOnAction(e -> {
            dialog.close();
            if (onConfirm != null) onConfirm.run();
        });

        buttonBar.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(header, card, notice, buttonBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    /**
     * Prompts the user to select a sanitization standard and confirm permanent erasure.
     */
    public static void promptSanitizationStandard(StorageDevice dev, Consumer<SanitizationStandard> onConfirm) {
        if (dev.isSystemDrive()) {
            showInfoAlert("SAFETY LOCK", "Operating System drive cannot be sanitized! Hardware lock is engaged.");
            return;
        }

        ChoiceDialog<SanitizationStandard> dialog = new ChoiceDialog<>(SanitizationStandard.NIST_800_88_CLEAR, SanitizationStandard.values());
        dialog.setTitle("Select Sanitization Standard");
        dialog.setHeaderText("Choose Sanitization Method");
        dialog.setContentText("Target: " + dev.getModel() + " (" + dev.getFormattedSize() + ")\nStandard:");

        Optional<SanitizationStandard> opt = dialog.showAndWait();
        if (opt.isEmpty()) return;

        SanitizationStandard standard = opt.get();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("PERMANENT MEDIA ERASURE");
        alert.setHeaderText("Irreversible Wipe Warning");
        alert.setContentText("Target: " + dev.getDevicePath() + " (" + dev.getModel() + ")\n"
                + "Standard: " + standard.getDisplayName() + " (" + standard.getTotalPasses() + " Passes)\n\n"
                + "All sectors will be PERMANENTLY DESTROYED beyond laboratory recovery. Are you sure?");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK && onConfirm != null) {
            onConfirm.accept(standard);
        }
    }

    private static Label createPropLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMinWidth(95);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        return lbl;
    }

    private static Label createValLabel(String text) {
        Label lbl = new Label(text != null ? text : "--");
        lbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        return lbl;
    }
}

