package com.me.tmw.nodes.util;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

import java.util.Collection;
import java.util.function.IntFunction;

public final class NodeMisc {

    public static final SnapshotParameters TRANSPARENT_SNAPSHOT_PARAMETERS = new SnapshotParameters();

    static {
        TRANSPARENT_SNAPSHOT_PARAMETERS.setFill(Color.TRANSPARENT);
    }

    @SafeVarargs
    public static <T> boolean allEqual(T... vals) {
        if (vals.length <= 1) return true;
        T first = vals[0];
        for (int i = 1; i < vals.length; i++) {
            if (vals[i] != first) {
                return false;
            }
        }
        return true;
    }

    public static Border simpleBorder(Paint paint, double width) {
        return new Border(new BorderStroke(paint, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(width)));
    }

    public static void addToGridPane(GridPane gridPane, Collection<? extends Node> nodes, IntFunction<Integer> xConverter) {
        addToGridPane(gridPane, nodes, xConverter, 0, x -> 0, 0);
    }
    public static void addToGridPane(GridPane gridPane, Collection<? extends Node> nodes, IntFunction<Integer> xConverter, int startX, IntFunction<Integer> yConverter, int startY) {
        for (Node node : nodes) {
            gridPane.add(node, startX, startY);
            startX = xConverter.apply(startX);
            startY = yConverter.apply(startY);
        }
    }

    public static String colorToCss(Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        double opacity = color.getOpacity();
        return "rgba(" + red + ", " + green + ", " + blue + ", " + opacity + ")";
    }

    public static SVGPath svgPath(String s) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(s);
        return svgPath;
    }
}
