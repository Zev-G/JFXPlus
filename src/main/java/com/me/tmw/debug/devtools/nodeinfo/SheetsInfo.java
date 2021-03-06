package com.me.tmw.debug.devtools.nodeinfo;

import com.me.tmw.nodes.util.NodeMisc;
import com.me.tmw.resource.Resources;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.css.Stylesheet;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SheetsInfo extends NodeInfo {

    private static final String STYLE_SHEET = Resources.DEBUGGER.getCss("stylesheets");

    private Parent node;
    private Consumer<String> open;

    private final Accordion sheets = new Accordion();

    private final ObservableList<String> viewedSheets = FXCollections.observableArrayList();
    private final Map<String, SheetInfo> viewedSheetsSheetInfoMap = new HashMap<>();

    private final CssParser parser = new CssParser();

    private final ObservableList<String> stylesheets = FXCollections.observableArrayList();
    private final ObservableList<StylesheetBinding> stylesheetLists = FXCollections.observableArrayList();
    private final ObservableList<Parent> parents = FXCollections.observableArrayList();

    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<>(this, "placeholder", new Label("No stylesheets found for this node."));

    private ChangeListener<Node> tempPlaceHolderListener;
    private final InvalidationListener sheetsInvalidated = observable -> {
        List<String> invalidSheets = new ArrayList<>(viewedSheets);
        List<String> newSheets = new ArrayList<>();
        for (String sheet : stylesheets) {
            invalidSheets.remove(sheet);
            if (!viewedSheets.contains(sheet) && !newSheets.contains(sheet) && sheet != null) {
                newSheets.add(sheet);
            }
        }
        viewedSheets.addAll(newSheets);
        for (String invalidSheet : invalidSheets) {
            viewedSheets.remove(invalidSheet);
            sheets.getPanes().remove(
                    viewedSheetsSheetInfoMap.remove(invalidSheet)
            );
        }
        for (String newSheet : newSheets) {
            URL url;
            try {
                url = new URL(newSheet);
            } catch (MalformedURLException e) {
                continue;
            }
            Stylesheet stylesheet;
            try {
                stylesheet = parser.parse(url);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            SheetInfo info = new SheetInfo(stylesheet, node, () -> open.accept(newSheet));
            if (!node.getStylesheets().contains(newSheet)) {
                info.setInherited(true);
            }
            viewedSheetsSheetInfoMap.put(newSheet, info);
            sheets.getPanes().add(info);
        }
    };

    public SheetsInfo(Parent parent, Consumer<String> open) {
        this.node = parent;
        this.open = open;
        getStylesheets().add(STYLE_SHEET);

        stylesheets.addListener(sheetsInvalidated);
        sheetsInvalidated.invalidated(null);
        NodeMisc.runAndAddListener(viewedSheets, observable -> {
            if (viewedSheets.isEmpty()) {
                getChildren().add(placeholder.get());
                tempPlaceHolderListener = (observableValue, oldValue, newValue) -> {
                    getChildren().remove(oldValue);
                    getChildren().add(newValue);
                };
                placeholder.addListener(tempPlaceHolderListener);
            } else {
                if (tempPlaceHolderListener != null) {
                    placeholder.removeListener(tempPlaceHolderListener);
                    tempPlaceHolderListener = null;
                }
                getChildren().remove(placeholder.get());
            }
        });

        StylesheetBinding nodeStylesheets = new StylesheetBinding(parent);
        stylesheetLists.add(nodeStylesheets);

        AtomicReference<StylesheetBinding> sceneBinding = new AtomicReference<>();
        AtomicReference<StylesheetBinding> userAgentBinding = new AtomicReference<>();
        AtomicReference<InvalidationListener> userAgentListener = new AtomicReference<>();
        ChangeListener<Scene> sceneListener = (observableValue, oldValue, newValue) -> {
            if (sceneBinding.get() != null) {
                sceneBinding.get().disconnect();
            }
            if (userAgentBinding.get() != null) {
                userAgentBinding.get().disconnect();
            }
            if (oldValue != null && userAgentListener.get() != null) {
                oldValue.userAgentStylesheetProperty().removeListener(userAgentListener.get());
            }
            if (newValue != null) {
                StylesheetBinding newBinding = new StylesheetBinding(newValue.getStylesheets());
                stylesheetLists.add(newBinding);
                sceneBinding.set(newBinding);

                ObservableList<String> userAgentStylesheets = FXCollections.observableArrayList(newValue.getUserAgentStylesheet());
                userAgentListener.set(observable -> userAgentStylesheets.setAll(newValue.getUserAgentStylesheet()));
                newValue.userAgentStylesheetProperty().addListener(userAgentListener.get());
                StylesheetBinding userAgent = new StylesheetBinding(userAgentStylesheets);
                stylesheetLists.add(userAgent);
            } else {
                sceneBinding.set(null);
                userAgentListener.set(null);
                userAgentBinding.set(null);
            }
        };
        sceneListener.changed(parent.sceneProperty(), null, parent.getScene());
        parent.sceneProperty().addListener(sceneListener);

        getChildren().add(sheets);

        loadParentList(parent);
    }

    private List<ParentBinding> loadParentList(Parent parent) {
        if (parent != null) {
            List<ParentBinding> dependencies = new ArrayList<>();
            ReadOnlyObjectProperty<Parent> grandParentProperty = parent.parentProperty();
            AtomicReference<Parent> lastGrandParent = new AtomicReference<>(null);
            AtomicReference<StylesheetBinding> lastStylesheetBinding = new AtomicReference<>(null);
            AtomicReference<Runnable> disconnectRef = new AtomicReference<>();
            InvalidationListener grandParentUpdater = observable -> {
                Parent grandParent = grandParentProperty.get();
                if (grandParent != lastGrandParent.get()) {
                    parents.remove(lastGrandParent.get());
                    if (lastStylesheetBinding.get() != null) {
                        lastStylesheetBinding.get().disconnect();
                    }
                    if (!dependencies.isEmpty()) {
                        dependencies.forEach(ParentBinding::disconnect);
                        dependencies.clear();
                    }
                    if (grandParent != null) {
                        parents.add(grandParent);
                        StylesheetBinding stylesheetBinding = new StylesheetBinding(grandParent);
                        stylesheetLists.add(stylesheetBinding);
                        dependencies.add(new ParentBinding(parent, disconnectRef.get()));
                        dependencies.addAll(loadParentList(grandParent));
                        lastGrandParent.set(grandParent);
                        lastStylesheetBinding.set(stylesheetBinding);
                    }
                }
            };
            disconnectRef.set(
                    () -> {
                        grandParentProperty.removeListener(grandParentUpdater);
                        parents.remove(grandParentProperty.get());
                        if (lastStylesheetBinding.get() != null) {
                            lastStylesheetBinding.get().disconnect();
                        }
                    }
            );
            grandParentProperty.addListener(grandParentUpdater);
            grandParentUpdater.invalidated(grandParentProperty);
            return dependencies;
        }
        return Collections.emptyList();
    }

    private static class ParentBinding {

        private final Parent parent;
        private final Runnable disconnect;

        private ParentBinding(Parent parent, Runnable disconnect) {
            this.parent = parent;
            this.disconnect = disconnect;
        }

        public Parent getParent() {
            return parent;
        }

        public Runnable getDisconnect() {
            return disconnect;
        }

        public void disconnect() {
            disconnect.run();
        }
    }

    private class StylesheetBinding {

        private final ObservableList<String> stylesheets;
        private final Runnable disconnector;

        private Set<String> previousValue = new HashSet<>();

        private StylesheetBinding(Parent parent) {
            this(parent.getStylesheets());
        }

        private StylesheetBinding(ObservableList<String> stylesheets) {
            this.stylesheets = stylesheets;
            InvalidationListener invalidated = observable -> {
                List<String> added = new ArrayList<>();
                List<String> removed = new ArrayList<>(previousValue);
                for (String value : stylesheets) {
                    removed.remove(value);
                    if (!previousValue.contains(value) || removed.contains(value)) {
                        added.add(value);
                    }
                }
                SheetsInfo.this.stylesheets.removeAll(removed);
                SheetsInfo.this.stylesheets.addAll(added);
                previousValue = new HashSet<>(stylesheets);
            };
            invalidated.invalidated(stylesheets);
            stylesheets.addListener(invalidated);
            disconnector = () -> {
                stylesheets.removeListener(invalidated);
                stylesheetLists.remove(this);
                SheetsInfo.this.stylesheets.removeAll(stylesheets);
            };
        }

        public void disconnect() {
            disconnector.run();
        }

        public ObservableList<String> getStylesheets() {
            return stylesheets;
        }

        public Runnable getDisconnector() {
            return disconnector;
        }

    }

}
