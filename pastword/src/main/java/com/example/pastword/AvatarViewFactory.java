package com.example.pastword;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;

public final class AvatarViewFactory {

    private AvatarViewFactory() {
    }

    public static StackPane createAvatar(UserAccount account, double size, String styleClass) {
        return createAvatar(account.getUsername(), account.getPhotoPath(), size, styleClass);
    }

    public static StackPane createAvatar(String username, String photoPath, double size, String styleClass) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar-shell");
        if (styleClass != null && !styleClass.isBlank()) {
            avatar.getStyleClass().add(styleClass);
        }
        avatar.setPrefSize(size, size);
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);

        Circle frame = new Circle(size / 2);
        frame.getStyleClass().add("avatar-circle");

        ImageView imageView = createImageView(photoPath, size);
        if (imageView != null) {
            avatar.getChildren().addAll(frame, imageView);
        } else {
            Label initials = new Label(buildInitials(username));
            initials.getStyleClass().add("avatar-initials");
            avatar.getChildren().addAll(frame, initials);
        }

        return avatar;
    }

    private static ImageView createImageView(String photoPath, double size) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }

        File file = new File(photoPath);
        if (!file.isFile()) {
            return null;
        }

        try {
            double imageSize = size - 8;
            Image image = new Image(file.toURI().toString(), imageSize, imageSize, true, true, false);
            if (image.isError()) {
                return null;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(imageSize);
            imageView.setFitHeight(imageSize);
            imageView.setPreserveRatio(false);

            Circle clip = new Circle(imageSize / 2);
            clip.setCenterX(imageSize / 2);
            clip.setCenterY(imageSize / 2);
            imageView.setClip(clip);
            return imageView;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String buildInitials(String username) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isBlank()) {
            return "?";
        }

        String[] words = trimmed.split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, 1).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
    }
}
