package org.cryptomator.ui.mainwindow;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes

	private final VaultListManager vaultListManager;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingVaultOver = new SimpleBooleanProperty();
	public StackPane root;

	@Inject
	public MainWindowController(VaultListManager vaultListManager, WrongFileAlertComponent.Builder wrongFileAlert) {
		this.vaultListManager = vaultListManager;
		this.wrongFileAlert = wrongFileAlert;
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowController");
		root.setOnDragEntered(this::handleDragEvent);
		root.setOnDragOver(this::handleDragEvent);
		root.setOnDragDropped(this::handleDragEvent);
		root.setOnDragExited(this::handleDragEvent);
	}

	private void handleDragEvent(DragEvent event) {
		if (DragEvent.DRAG_ENTERED.equals(event.getEventType()) && event.getGestureSource() == null) {
			draggingOver.set(true);
		} else if (DragEvent.DRAG_OVER.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.ANY);
			draggingVaultOver.set(event.getDragboard().getFiles().stream().map(File::toPath).anyMatch(this::containsVault));
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			Set<Path> vaultPaths = event.getDragboard().getFiles().stream().map(File::toPath).filter(this::containsVault).collect(Collectors.toSet());
			if (vaultPaths.isEmpty()) {
				wrongFileAlert.build().showWrongFileAlertWindow();
			} else {
				vaultPaths.forEach(this::addVault);
			}
			event.setDropCompleted(!vaultPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingOver.set(false);
			draggingVaultOver.set(false);
		}
	}

	private boolean containsVault(Path path) {
		if (path.getFileName().toString().equals(MASTERKEY_FILENAME)) {
			return true;
		} else if (Files.isDirectory(path) && Files.exists(path.resolve(MASTERKEY_FILENAME))) {
			return true;
		} else {
			return false;
		}
	}

	private void addVault(Path pathToVault) {
		try {
			if (pathToVault.getFileName().toString().equals(MASTERKEY_FILENAME)) {
				vaultListManager.add(pathToVault.getParent());
			} else {
				vaultListManager.add(pathToVault);
			}
		} catch (NoSuchFileException e) {
			LOG.debug("Not a vault: {}", pathToVault);
		}
	}

	/* Getter/Setter */

	public BooleanProperty draggingOverProperty() {
		return draggingOver;
	}

	public boolean isDraggingOver() {
		return draggingOver.get();
	}

	public BooleanProperty draggingVaultOverProperty() {
		return draggingVaultOver;
	}

	public boolean isDraggingVaultOver() {
		return draggingVaultOver.get();
	}
}
