module com.example.pastword {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pastword to javafx.fxml;
    exports com.example.pastword;
}