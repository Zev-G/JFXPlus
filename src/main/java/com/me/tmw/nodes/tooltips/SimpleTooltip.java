package com.me.tmw.nodes.tooltips;

import com.me.tmw.resource.Resources;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.util.function.Consumer;

public class SimpleTooltip extends Tooltip {

    private static final String STYLE_SHEET = Resources.NODES.getCss("tooltip");

    public static void apply(Control control, String text) {
        control.getStylesheets().add(STYLE_SHEET);
        control.setTooltip(new SimpleTooltip(text));
    }

    public static void apply(Consumer<Tooltip> applier, Parent parent, String text) {
        parent.getStylesheets().add(STYLE_SHEET);
        applier.accept(new SimpleTooltip(text));
    }
    public static void install(Control control, String text) {
        apply(control, text);
    }

    public static void install(Consumer<Tooltip> applier, Parent parent, String text) {
        apply(applier, parent, text);
    }

    private SimpleTooltip(String text) {
        super(text);
        setShowDuration(Duration.INDEFINITE);
        ownerChanged(null, getOwnerNode());
        ownerNodeProperty().addListener((observable, oldValue, newValue) -> ownerChanged(oldValue, newValue));
    }

    private void ownerChanged(Node oldValue, Node newValue) {
        if (newValue != null && newValue.getScene() != null && !newValue.getScene().getStylesheets().contains(STYLE_SHEET)) {
            newValue.getScene().getStylesheets().add(STYLE_SHEET);
        }
        if (oldValue != null && oldValue.getScene() != null) {
            oldValue.getScene().getStylesheets().remove(STYLE_SHEET);
        }
    }

}
