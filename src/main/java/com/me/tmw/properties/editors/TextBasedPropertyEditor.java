package com.me.tmw.properties.editors;

import com.me.tmw.nodes.control.FillWidthTextField;
import com.me.tmw.nodes.tooltips.SimpleTooltip;
import javafx.beans.property.Property;

public abstract class TextBasedPropertyEditor<T> extends PropertyEditorBase<T> {

    protected final FillWidthTextField textField = new FillWidthTextField();

    private String ignoreS = null;
    private T ignoreT = null;

    public TextBasedPropertyEditor(String name, Property<T> value) {
        super(name, value);

        textField.setText(convertToString(get()));

        addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(ignoreT)) {
                ignoreS = convertToString(newValue);
                textField.setText(ignoreS);
                ignoreS = null;
            }
        });

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(ignoreS)) {
                try {
                    ignoreT = convertToValue(newValue);
                    set(ignoreT);
                    ignoreT = null;
                    removeError();
                } catch (RuntimeException e) {
                    addError();
                }
            } else {
                removeError();
            }
        });
        textField.setPromptText(value.getName());
        SimpleTooltip.install(textField, value.getName());

        setNode(textField);
    }

    private void removeError() {
        textField.getStyleClass().remove("invalid-text");
    }
    private void addError() {
        if (!textField.getStyleClass().contains("invalid-text")) {
            textField.getStyleClass().add("invalid-text");
        }
    }

    protected abstract T convertToValue(String text);
    protected abstract String convertToString(T value);

}
