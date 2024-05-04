module htl.steyr.databasefusioner {
    requires javafx.controls;
    requires javafx.fxml;


    opens htl.steyr.databasefusioner to javafx.fxml;
    exports htl.steyr.databasefusioner;
}