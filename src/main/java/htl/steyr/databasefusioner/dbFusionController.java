package htl.steyr.databasefusioner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.sql.*;

public class dbFusionController {

    public Label fileOneChosenLabel;
    public Label fileTwoChosenLabel;
    public Label headerLabel;
    public Button startMigrationButton;
    public TextField namedbLabel;
    public TextField portTextField;
    public TextField benutzernameTextField;
    public PasswordField passwordTextField;

    public void chooseFileOne(MouseEvent mouseEvent) throws IOException {
        File file = chooseFile();
        if (file != null) {
            fileOneChosenLabel.setText(file.getName());
            processFile(file, "output1.txt");
        }
    }

    public void chooseFileTwo(MouseEvent mouseEvent) throws IOException {
        File file = chooseFile();
        if (file != null) {
            fileTwoChosenLabel.setText(file.getName());
            processFile(file, "src/main/java/htl/steyr/databasefusioner/importCommands.txt");
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open SQL Dump File");
        return fileChooser.showOpenDialog(null);
    }

    private void processFile(File file, String outputFileName) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> insertIntoCommands = new ArrayList<>();
        StringBuilder command = new StringBuilder();

        Map<String, ArrayList<String>> tableWithColumnNames = new HashMap<>();

        for (String line : lines) {
            if (line.startsWith("CREATE TABLE")) {
                int indexStartName = line.indexOf('`');
                int indexEndName = line.indexOf('`', indexStartName + 1);
                String tableName = line.substring(indexStartName + 1, indexEndName);
                ArrayList<String> columnNames = new ArrayList<>();

                while (!line.contains("PRIMARY KEY")) {
                    line = lines.get(lines.indexOf(line) + 1); // go to the next line
                    if (line.contains("`") && !line.contains("PRIMARY KEY")) {
                        int indexStartColumnName = line.indexOf('`');
                        int indexEndColumnName = line.indexOf('`', indexStartColumnName + 1);
                        String columnName = line.substring(indexStartColumnName + 1, indexEndColumnName);
                        if (!columnName.equals("id")) { // 'id' column not included
                            columnNames.add(columnName);
                        }
                    }
                }
                tableWithColumnNames.put(tableName, columnNames);
            }

          //  if (line.startsWith("LOCK TABLES")) {
            if (line.startsWith("INSERT INTO")) {
                command = new StringBuilder(line);

            } else if (!command.isEmpty()) {
                command.append("\n").append(line);
            }

           // if (line.endsWith("UNLOCK TABLES;")) {
            if (line.endsWith(";")) {
                insertIntoCommands.add(command.toString());
                command = new StringBuilder();
            }
        }

        Path outputPath = Path.of(outputFileName);
        Files.write(outputPath, insertIntoCommands);

        System.out.println(tableWithColumnNames);
       // alterImportCommands();

        if (!fileTwoChosenLabel.getText().isEmpty()){
            startMigrationButton.setDisable(false);
        }


    }

    private void alterImportCommands() {

    }

    public void showMigrationScene(ActionEvent actionEvent){

        FXMLLoader loader = new FXMLLoader(getClass().getResource("migrationWindow.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    public void startMigration(MouseEvent mouseEvent) {

        String username = benutzernameTextField.getText();
        String password = passwordTextField.getText();
        int port = Integer.parseInt(portTextField.getText());
        String db_name = namedbLabel.getText();
        String url = "jdbc:mysql://localhost:" + port + "/" + db_name;
        String path_file_commands = "C:\\Users\\johan\\OneDrive - HTBLA Steyr\\Johannes_Schule\\HTL\\4. Klasse\\INSY\\Programme\\databaseFusioner\\src\\main\\java\\htl\\steyr\\databasefusioner\\importCommands.txt";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            String sql = new String(Files.readAllBytes(Paths.get(path_file_commands)));
            for (String singleSql : sql.split(";")) {
                stmt.execute(singleSql);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        Node source = (Node) mouseEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}