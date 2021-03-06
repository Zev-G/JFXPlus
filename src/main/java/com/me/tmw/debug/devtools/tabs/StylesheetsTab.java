package com.me.tmw.debug.devtools.tabs;

import com.me.tmw.debug.devtools.nodeinfo.SheetsInfo;
import com.me.tmw.debug.devtools.tabs.FilesTab.Source;
import com.me.tmw.nodes.richtextfx.languages.CSSLang;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class StylesheetsTab extends Tab {

    private final StructureTab structureTab;
    private final Map<Node, SheetsInfo> sheetsInfoMap = new HashMap<>();
    private final StackPane noStylesheets = new StackPane(new Label("Can't load style sheets."));

    private final ComboBox<String> typeComboBox = new ComboBox<>() {
        {
            this.getItems().addAll("Temp File", "File Path", "URL");
            this.getSelectionModel().select("Temp File");
        }
    };
    private final TextField newStylesheetField = new TextField() {
        {
            promptTextProperty().bind(
                    Bindings.createStringBinding(() -> {
                        String selected = typeComboBox.getSelectionModel().getSelectedItem();
                        if (selected == null) {
                            selected = "";
                        }
                        switch (selected) { // Not using enhanced switch for backwards compatibility with computers at school which use jdk11.
                            case "File Path":
                                return "Location of the file, e.g. \"C:\\Windows\\stylesheet.css\"";
                            case "URL":
                                return "URL of the stylesheet, e.g. \"https://www.domain/files/stylesheet.css\"";
                            case "Temp File":
                                return "Any valid file name, e.g. \"stylesheet.css\"";
                            default:
                                return "";
                        }
                    }, typeComboBox.getSelectionModel().selectedItemProperty())
            );
            setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    addStylesheet();
                    newStylesheetField.setText("");
                }
            });
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    };
    private final Button add = new Button("Add") {
        {
            setOnAction(event -> {
                addStylesheet();
                newStylesheetField.setText("");
            });
        }
    };
    private final HBox topBar = new HBox(typeComboBox, newStylesheetField, new Group(), add) {
        {
            setSpacing(2);
        }
    };

    private final StackPane stylesheetsPlaceHolder = new StackPane();
    private final VBox display = new VBox(topBar, stylesheetsPlaceHolder);

    public StylesheetsTab(StructureTab structureTab) {
        super("Stylesheets (Beta)");
        setContent(display);
        this.structureTab = structureTab;

        VBox.setVgrow(stylesheetsPlaceHolder, Priority.ALWAYS);

        structureTab.getSceneTree().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (structureTab.getInfoTabPane().getSelectionModel().getSelectedItem() == this && newValue != null && newValue.getValue() instanceof Parent) {
                load((Parent) newValue.getValue());
            } else {
                stylesheetsPlaceHolder.getChildren().setAll(noStylesheets);
            }
        });
    }

    public void load(Parent value) {
        if (!sheetsInfoMap.containsKey(value)) {
            SheetsInfo sheetsInfo = new SheetsInfo(value, url -> {
                structureTab.getTools().getFilesTab().loadURL(url, new CSSLang());
                structureTab.getTools().selectTab(structureTab.getTools().getFilesTab());
            });
            sheetsInfoMap.put(value, sheetsInfo);
        }
        ScrollPane scrollPane = new ScrollPane(sheetsInfoMap.get(value));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        stylesheetsPlaceHolder.getChildren().setAll(scrollPane);
    }

    private void addStylesheet() {
        URL result;
        String selected = typeComboBox.getSelectionModel().getSelectedItem();
        if (structureTab.getSceneTree().getSelectionModel().getSelectedItem() == null || !(structureTab.getSceneTree().getSelectionModel().getSelectedItem().getValue() instanceof Parent) || selected == null || newStylesheetField.getText().isEmpty()) {
            return;
        }
        if (selected.equals("File Path") || selected.equals("Temp File")) {
            File file;
            String filePath = newStylesheetField.getText();
            if (selected.equals("File Path")) {
                file = new File(filePath);
            } else {
                int lastIndexOf = filePath.lastIndexOf('.');
                String name;
                String ending;
                if (lastIndexOf == -1) {
                    name = filePath;
                    ending = ".css";
                } else {
                    name = filePath.substring(0, lastIndexOf);
                    ending = filePath.substring(lastIndexOf);
                }
                try {
                    file = Files.createTempFile(name, ending).toFile();
                } catch (IOException e) {
                    return;
                }
            }

            try {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        return;
                    }
                }
                result = file.toURI().toURL();
            } catch (IOException e) {
                return;
            }
        } else if (selected.equals("URL")) {
            try {
                result = new URL(newStylesheetField.getText());
            } catch (MalformedURLException e) {
                return;
            }
        } else {
            return;
        }
        ((Parent) structureTab.getSceneTree().getSelectionModel().getSelectedItem().getValue()).getStylesheets().add(result.toExternalForm());
        Source source = structureTab.getTools().getFilesTab().loadURL(result);
        source.setOnSaved(text -> {
            Parent selectedParent = ((Parent) structureTab.getSceneTree().getSelectionModel().getSelectedItem().getValue());
            selectedParent.getStylesheets().remove(result.toExternalForm());
            selectedParent.getStylesheets().add(result.toExternalForm());
        });
    }

}
