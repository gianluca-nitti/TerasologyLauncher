/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.launcher.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public final class GuiUtils {

    private static final Logger logger = LoggerFactory.getLogger(GuiUtils.class);

    private GuiUtils() {
    }

    private static void showMessageDialog(Alert.AlertType type, String title, String message, Stage owner) {
        FutureTask<Void> dialog = new FutureTask<>(() -> {
            final Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.initOwner(owner);

            alert.showAndWait();
            return null;
        });

        Platform.runLater(dialog);
        try {
            dialog.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Uh oh, something went wrong with the dialog!", e);
        }
    }

    public static void showWarningMessageDialog(Stage owner, String message) {
        showMessageDialog(Alert.AlertType.WARNING, BundleUtils.getLabel("message_error_title"), message, owner);
    }

    public static void showErrorMessageDialog(Stage owner, String message) {
        showMessageDialog(Alert.AlertType.ERROR, BundleUtils.getLabel("message_error_title"), message, owner);
    }

    public static void showInfoMessageDialog(Stage owner, String message) {
        showMessageDialog(Alert.AlertType.INFORMATION, BundleUtils.getLabel("message_information_title"), message, owner);
    }

    public static boolean showBinaryChoicheDialog(String title, String message, String btnTrue, String btnFalse) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        ButtonType buttonTypeTrue = new ButtonType(btnTrue, ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeFalse = new ButtonType(btnFalse, ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonTypeTrue, buttonTypeFalse);
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == buttonTypeTrue;
    }

    public static File chooseDirectoryDialog(Stage owner, final File directory, final String title) {
        try {
            DirectoryUtils.checkDirectory(directory);
        } catch (IOException e) {
            logger.error("Could not use {} as default directory!", directory, e);
            return null;
        }

        File selected = null;

        if (Platform.isFxApplicationThread()) {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(directory);
            directoryChooser.setTitle(title);

            selected = directoryChooser.showDialog(owner);
        } else {
            final FutureTask<File> chooseDirectory = new FutureTask<>(() -> {
                final DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(directory);
                directoryChooser.setTitle(title);

                return directoryChooser.showDialog(owner);
            });

            Platform.runLater(chooseDirectory);
            try {
                selected = chooseDirectory.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.warn("Uh oh, something went wrong with the dialog!", e);
            }
        }

        // directory proposal needs to be deleted if the user chose a different one
        if (!directory.equals(selected) && !DirectoryUtils.containsFiles(directory)) {
            if (!directory.delete()) {
                logger.warn("Could not delete unused default directory! {}", directory);
            }
        }
        return selected;
    }

    public static void openFileBrowser(Stage owner, final File directory, final String errorMsg) {
        try {
            DirectoryUtils.checkDirectory(directory);
            EventQueue.invokeLater(() -> {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(directory);
                    } catch (IOException e) {
                        logger.error("The directory could not be opened! {}", directory, e);
                        GuiUtils.showErrorMessageDialog(owner, errorMsg + "\n" + directory);
                    }
                }
            });
        } catch (IOException e) {
            logger.error("The directory could not be opened! {}", directory, e);
            GuiUtils.showErrorMessageDialog(owner, errorMsg + "\n" + directory);
        }
    }
}
