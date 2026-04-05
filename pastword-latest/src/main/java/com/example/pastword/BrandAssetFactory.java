package com.example.pastword;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// Loads the shared logo artwork used across the app scenes.
public final class BrandAssetFactory {

    private BrandAssetFactory() {
    }

    // Creates the sidebar logo image sized to fit inside the left navigation rail.
    public static ImageView createSidebarLogo() {
        return createLogoView(180, "sidebar-logo-image");
    }

    // Creates the centered auth/logo mark used above the login and register forms.
    public static ImageView createAuthLogo() {
        return createLogoView(330, "brand-logo-image");
    }

    // Creates the centered content logo used on non-auth pages like settings.
    public static ImageView createContentLogo() {
        return createLogoView(250, "content-logo-image");
    }

    private static ImageView createLogoView(double fitWidth, String styleClass) {
        ImageView logoView = new ImageView(loadImage("logo.png"));
        logoView.getStyleClass().add(styleClass);
        logoView.setPreserveRatio(true);
        logoView.setFitWidth(fitWidth);
        logoView.setSmooth(true);
        return logoView;
    }

    private static Image loadImage(String fileName) {
        return new Image(BrandAssetFactory.class.getResource(
                "/com/example/pastword/assets/" + fileName).toExternalForm());
    }
}
