package com.me.tmw.debug.devtools;

import com.me.tmw.debug.devtools.tabs.ConsoleTab;
import com.me.tmw.debug.devtools.tabs.StructureTab;
import com.me.tmw.resource.Resources;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

public class DevTools extends StackPane {

    private static final String STYLE_SHEET = Resources.DEBUGGER.getCss("dev-tools");
    private static final String TAB_STYLE_SHEET = Resources.DEBUGGER.getCss("flat-tab");

    private final StructureTab structureTab;
    private final ConsoleTab consoleTab;
    private final TabPane tabPane = new TabPane();

    public DevTools(Parent root) {
        getStylesheets().add(TAB_STYLE_SHEET);

        structureTab = new StructureTab(root, this);
        consoleTab = new ConsoleTab(root);
        structureTab.setClosable(false);
        consoleTab.setClosable(false);

        tabPane.getTabs().addAll(structureTab, consoleTab);

        getChildren().add(tabPane);
    }

    public StructureTab getStructureTab() {
        return structureTab;
    }

    public ConsoleTab getConsoleTab() {
        return consoleTab;
    }
}
