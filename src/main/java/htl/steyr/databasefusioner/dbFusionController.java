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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    public HashMap<String, ArrayList<String>> tableWithColumnNames;
    public Button chooseFileBtn;

/*    public void chooseFileOne(MouseEvent mouseEvent) throws IOException {
        File file = chooseFile();
        if (file != null) {
            fileOneChosenLabel.setText(file.getName());
            processFile(file, "output1.txt");
        }
    }*/

//    public void chooseFileTwo(MouseEvent mouseEvent) throws IOException {
//        File file = chooseFile();
//        if (file != null) {
//            fileTwoChosenLabel.setText(file.getName());
//            processFile(file, "src/main/java/htl/steyr/databasefusioner/importCommands.txt");
//        }
//    }

    public void chooseFileTwo(MouseEvent mouseEvent) throws IOException {
        File file = chooseFile();
        if (file != null) {
            chooseFileBtn.setText(file.getName());

            processFile(file, "src/main/java/htl/steyr/databasefusioner/importCommands.txt");
            //alterImportCommands(tableWithColumnNames); // Aufruf der Methode, um die Import-Befehle zu ändern
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

        tableWithColumnNames = new HashMap<>();

        for (String line : lines) {
            if (line.startsWith("CREATE TABLE")) {
                int indexStartName = line.indexOf('`');
                int indexEndName = line.indexOf('`', indexStartName + 1);
                String tableName = line.substring(indexStartName + 1, indexEndName).replaceAll("`", ""); // Entferne die Grave-Akzente
                ArrayList<String> columnNames = new ArrayList<>();

                while (!line.contains("PRIMARY KEY")) {
                    line = lines.get(lines.indexOf(line) + 1); // go to the next line
                    if (line.contains("`") && !line.contains("PRIMARY KEY")) {
                        int indexStartColumnName = line.indexOf('`');
                        int indexEndColumnName = line.indexOf('`', indexStartColumnName + 1);
                        String columnName = line.substring(indexStartColumnName + 1, indexEndColumnName).replaceAll("`", ""); // Entferne die Grave-Akzente
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
        alterImportCommands(tableWithColumnNames);

        if (!chooseFileBtn.getText().equals("Datei auswählen")) {
            startMigrationButton.setDisable(false);
        }
    }

    public List<String> readInsertCommandsFromFile(String filePath) {
        List<String> insertCommands = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            // Jetzt sind alle Zeilen der Datei in der Liste "lines" gespeichert
            // Du kannst diese Liste durchgehen und jede Zeile anzeigen oder weiterverarbeiten
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    insertCommands.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Gib die Liste mit den nicht-leeren Zeilen zurück
        return insertCommands;
    }

 /*   private void alterImportCommands(HashMap<String, ArrayList<String>> tableStructure) {
        // TODO: 1) Jedes Insert-Statement aus der Datei holen.
        // TODO: 2) nach dem Namen der Tabelle die Attribute reinspeichern
        // TODO: 3) die id aus dem Insert-Into herausfiltern

    }*/

    private void alterImportCommands(HashMap<String, ArrayList<String>> tableStructure) {
        List<String> updatedInsertCommands = new ArrayList<>();
        String filePath = "src/main/java/htl/steyr/databasefusioner/importCommands.txt";
        List<String> insertCommands = readInsertCommandsFromFile(filePath);

        for (String insertCommand : insertCommands) {
            String[] tokens = insertCommand.split("\\s+");
            String tableName = tokens[2];  // Index 3 is the name of the table!
            tableName = tableName.replaceAll("`", "");
            ArrayList<String> columnNames = tableStructure.get(tableName);

            if (!columnNames.getFirst().toLowerCase().contains("id")) {
                Pattern pattern = Pattern.compile("\\(\\d+,");
                Matcher matcher = pattern.matcher(insertCommand);

                // Ersetze alle Vorkommen der IDs, die direkt nach einer öffnenden Klammer stehen, durch ein leeres Zeichen
                insertCommand = matcher.replaceAll("(");
            }


            // Erstelle ein neues Insert-Statement mit den aktualisierten Spaltennamen
            StringBuilder newInsertCommand = new StringBuilder("INSERT INTO ");
            newInsertCommand.append(tableName).append(" (");
            newInsertCommand.append(String.join(", ", columnNames)).append(") ");
            newInsertCommand.append(insertCommand.substring(insertCommand.indexOf("VALUES"))); // Behalte die Werte bei

            // Füge das aktualisierte Insert-Statement der Liste hinzu
            updatedInsertCommands.add(newInsertCommand.toString());
        }

        // Ersetze die ursprünglichen Insert-Statements durch die aktualisierten
        insertCommands.clear();
        insertCommands.addAll(updatedInsertCommands);
        System.out.println(updatedInsertCommands);
    }

    public void showMigrationScene(ActionEvent actionEvent) {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("migrationWindow.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            System.out.println("Problem beim Laden der Migration Scene!");
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