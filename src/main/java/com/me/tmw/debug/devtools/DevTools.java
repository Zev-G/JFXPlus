package com.me.tmw.debug.devtools;

import com.me.tmw.css.Sheets;
import com.me.tmw.debug.devtools.tabs.ConsoleTab;
import com.me.tmw.debug.devtools.tabs.StructureTab;
import com.me.tmw.nodes.control.svg.SVG;
import com.me.tmw.nodes.util.NodeMisc;
import com.me.tmw.resource.Resources;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static com.me.tmw.css.Sheets.Essentials.*;

public class DevTools extends StackPane {

    private static final String STYLE_SHEET = Resources.DEBUGGER.getCss("dev-tools");
    private static final String TAB_STYLE_SHEET = Resources.DEBUGGER.getCss("flat-tab");

    private final StructureTab structureTab;
    private final ConsoleTab consoleTab;
    private final TabPane tabPane = new TabPane();

    private final Button close = new Button("", NodeMisc.svgPath(SVG.X, 0.5));
    private final Button detach = new Button("", NodeMisc.svgPath(SVG.OPEN, 0.8));

    private final HBox buttons = new HBox();

    private final DevToolsContainer container;
    private boolean attachedToContainer = true;

    private Stage detachedStage;

    public DevTools(Parent root, DevToolsContainer container) {
        this.container = container;
        getStylesheets().addAll(TAB_STYLE_SHEET, Sheets.Essentials.STYLE_SHEET);

        close.getStyleClass().addAll(TRANSPARENT_BUTTON_CLASS, LIGHT_SVG_BUTTON_CLASS, HAND_CURSOR_CLASS);
        detach.getStyleClass().addAll(TRANSPARENT_BUTTON_CLASS, LIGHT_SVG_BUTTON_CLASS, HAND_CURSOR_CLASS);
        close.setOnAction(event -> hide());
        detach.setOnAction(event -> {
            attachedToContainer = !attachedToContainer;
            if (attachedToContainer) {
                container.attach(this);
                if (detachedStage != null) {
                    detachedStage.close();
                    detachedStage = null;
                }
            } else {
                container.remove(this);
                detachedStage = new Stage();
                detachedStage.setScene(new Scene(this));
                detachedStage.show();
            }
        });

        buttons.setAlignment(Pos.TOP_RIGHT);
        buttons.setPadding(new Insets(5, 7.5, 5, 0));
        buttons.setSpacing(5);
        buttons.setPickOnBounds(false);
        buttons.getChildren().addAll(detach, close);

        structureTab = new StructureTab(root, this);
        consoleTab = new ConsoleTab(root);
        structureTab.setClosable(false);
        consoleTab.setClosable(false);

        tabPane.getTabs().addAll(structureTab, consoleTab);

        getChildren().addAll(tabPane, buttons);
    }

    public StructureTab getStructureTab() {
        return structureTab;
    }

    public ConsoleTab getConsoleTab() {
        return consoleTab;
    }

    public void hide() {
        if (attachedToContainer) {
            container.remove(this);
        } else {
            attachedToContainer = true;
            detachedStage.hide();
            detachedStage = null;
        }
    }

    public boolean isShown() {
        return container.isShowing(this) || (detachedStage != null && detachedStage.isShowing());
    }

}
