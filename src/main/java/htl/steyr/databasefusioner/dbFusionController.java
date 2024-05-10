package htl.steyr.databasefusioner;

import javafx.event.ActionEvent;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;

public class dbFusionController {
    public Label headerLabel;
    public Button startMigrationButton;
    public TextField namedbLabel;
    public TextField portTextField;
    public TextField benutzernameTextField;
    public PasswordField passwordTextField;
    public HashMap<String, ArrayList<String>> tableWithColumnNames;
    public Button chooseFileBtn;
    public static List<String> sqlCommandsToPaste = new ArrayList<>();
    public File selectedFile;

    public void chooseFileTwo(MouseEvent mouseEvent) throws IOException {
        selectedFile = chooseFile();
        if (selectedFile != null) {
            chooseFileBtn.setText(selectedFile.getName());
            processFile(selectedFile, "src/main/java/htl/steyr/databasefusioner/importCommands.txt");
            startMigrationButton.setDisable(false);
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

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("CREATE TABLE")) {
                int indexStartName = line.indexOf('`');
                int indexEndName = line.indexOf('`', indexStartName + 1);
                String tableName = line.substring(indexStartName + 1, indexEndName);
                ArrayList<String> columnNames = new ArrayList<>();

                while (!line.contains("PRIMARY KEY")) {
                    i++;
                    if (i < lines.size()) {
                        line = lines.get(i);
                        line = line.trim();
                        if (line.startsWith("`") && (!line.contains("PRIMARY KEY") || !line.contains("KEY"))) {
                            int indexStartColumnName = line.indexOf('`');
                            int indexEndColumnName = line.indexOf('`', indexStartColumnName + 1);
                            String columnName = line.substring(indexStartColumnName + 1, indexEndColumnName);
                            if (!columnName.equals("id")) {
                                columnNames.add(columnName);
                            }
                        }
                        if (line.startsWith("INSERT INTO")) {
                            command = new StringBuilder(line);

                        } else if (!command.isEmpty()) {
                            command.append("\n").append(line);
                        }

                        if (line.endsWith(";")) {
                            insertIntoCommands.add(command.toString());
                            command = new StringBuilder();
                        }
                    } else {
                        break;
                    }
                }
                tableWithColumnNames.put(tableName, columnNames);
            }

            if (line.startsWith("INSERT INTO")) {
                command = new StringBuilder(line);

            } else if (!command.isEmpty()) {
                command.append("\n").append(line);
            }

            if (line.endsWith(";")) {
                insertIntoCommands.add(command.toString());
                command = new StringBuilder();
            }
        }

        Path outputPath = Path.of(outputFileName);
        Files.write(outputPath, insertIntoCommands);

        alterImportCommands(tableWithColumnNames);
    }

    public List<String> readInsertCommandsFromFile(String filePath) {
        List<String> insertCommands = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    insertCommands.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return insertCommands;
    }

    private void alterImportCommands(HashMap<String, ArrayList<String>> tableStructure) {
        List<String> updatedInsertCommands = new ArrayList<>();
        String filePath = "src/main/java/htl/steyr/databasefusioner/importCommands.txt";
        List<String> insertCommands = readInsertCommandsFromFile(filePath);

        for (String insertCommand : insertCommands) {
            String[] tokens = insertCommand.split("\\s+");
            String tableName = tokens[2];
            tableName = tableName.replaceAll("`", "");
            ArrayList<String> columnNames = tableStructure.get(tableName);

            if (!columnNames.getFirst().toLowerCase().contains("id")) {
                Pattern pattern = Pattern.compile("\\(\\d+,");
                Matcher matcher = pattern.matcher(insertCommand);

                insertCommand = matcher.replaceAll("(");
            }

            StringBuilder newInsertCommand = new StringBuilder("INSERT INTO ");
            newInsertCommand.append(tableName).append(" (");
            newInsertCommand.append(String.join(", ", columnNames)).append(") ");
            newInsertCommand.append(insertCommand.substring(insertCommand.indexOf("VALUES"))); // Behalte die Werte bei

            updatedInsertCommands.add(newInsertCommand.toString());
        }

        insertCommands.clear();
        insertCommands.addAll(updatedInsertCommands);
        sqlCommandsToPaste.addAll(insertCommands);
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

    public void startMigration(MouseEvent mouseEvent) throws IOException {
        String username = benutzernameTextField.getText();
        String password = passwordTextField.getText();
        int port = Integer.parseInt(portTextField.getText());
        String db_name = namedbLabel.getText();
        String url = "jdbc:mysql://localhost:" + port + "/" + db_name;
        String path_file_commands = "C:\\Users\\johan\\OneDrive - HTBLA Steyr\\Johannes_Schule\\HTL\\4. Klasse\\INSY\\Programme\\databaseFusioner\\src\\main\\java\\htl\\steyr\\databasefusioner\\importCommands.txt";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            for (String insertCmd : sqlCommandsToPaste) {
                stmt.execute(insertCmd);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Node source = (Node) mouseEvent.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}