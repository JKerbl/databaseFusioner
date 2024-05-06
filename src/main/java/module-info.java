module htl.steyr.databasefusioner {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens htl.steyr.databasefusioner to javafx.fxml;
    exports htl.steyr.databasefusioner;
}