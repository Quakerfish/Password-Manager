package com.example.pastword;

import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

// Creates the inline SVG icons used across toolbar buttons and password toggles.
public final class SvgIconFactory {

    private SvgIconFactory() {
    }

    // Builds an icon-only action button for table rows and compact actions.
    public static Button createActionButton(String iconName, String accessibleText) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "table-icon-button");
        button.setGraphic(createIcon(iconName, "action-icon"));
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(false);
        return button;
    }

    // Builds the inline eye button used inside password fields.
    public static Button createInlineEyeButton(boolean visible) {
        Button button = new Button();
        button.getStyleClass().add("password-toggle-inline");
        setEyeGraphic(button, visible);
        button.setFocusTraversable(false);
        return button;
    }

    // Swaps the inline eye icon between hidden and visible states.
    public static void setEyeGraphic(Button button, boolean visible) {
        button.setGraphic(createIcon(visible ? "eye-off" : "eye", "password-toggle-icon"));
    }

    // Builds a decorative icon for non-button surfaces like search fields.
    public static StackPane createDecorativeIcon(String iconName, String styleClass) {
        return createIcon(iconName, styleClass);
    }

    private static StackPane createIcon(String iconName, String styleClass) {
        SVGPath path = new SVGPath();
        path.setContent(iconPath(iconName));
        path.getStyleClass().add(styleClass);
        return new StackPane(path);
    }

    private static String iconPath(String iconName) {
        return switch (iconName) {
            case "eye" -> "M1 12C3.8 7.1 8 4 12 4s8.2 3.1 11 8c-2.8 4.9-7 8-11 8S3.8 16.9 1 12Zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0-2.2A1.8 1.8 0 1 1 12 10a1.8 1.8 0 0 1 0 3.8Z";
            case "eye-off" -> "M2.6 3.5 1.2 4.9l3 3C2.8 9.1 1.8 10.5 1 12c2.8 4.9 7 8 11 8 1.9 0 3.9-.7 5.7-1.9l3.1 3.1 1.4-1.4L2.6 3.5Zm9.4 12.5c-3.1 0-6.3-2.4-8.6-6 .6-.9 1.3-1.7 2-2.4l2 2A4 4 0 0 0 12 16Zm0-8c2.2 0 4 1.8 4 4 0 .6-.1 1.1-.3 1.6l2.4 2.4c1.1-1 2.1-2.2 2.9-3.6-2.3-3.6-5.5-6-8.6-6-.9 0-1.8.2-2.7.5l1.8 1.8c.3-.1.7-.1 1-.1Z";
            case "pencil" -> "M3 17.2V21h3.8L18 9.8 14.2 6 3 17.2Zm17.7-10.1a1 1 0 0 0 0-1.4l-2.4-2.4a1 1 0 0 0-1.4 0L15 5.2 18.8 9l1.9-1.9Z";
            case "trash" -> "M6 7h2v11H6V7Zm5 0h2v11h-2V7Zm5 0h2v11h-2V7ZM4 4h5l1-1h4l1 1h5v2H4V4Zm2 4h12v13a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8Z";
            case "search" -> "M10.5 3a7.5 7.5 0 1 0 4.6 13.4l4.2 4.2 1.4-1.4-4.2-4.2A7.5 7.5 0 0 0 10.5 3Zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11Z";
            default -> "M4 4h16v16H4z";
        };
    }
}
