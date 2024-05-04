package htl.steyr.databasefusioner;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class dbFusionController {

    public Label fileOneChosenLabel;
    public Label fileTwoChosenLabel;
    public Label headerLabel;

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
            processFile(file, "output2.txt");
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open SQL Dump File");
        return fileChooser.showOpenDialog(null);
    }

    private void processFile(File file, String outputFileName) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> createTableCommands = new ArrayList<>();
        StringBuilder command = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("CREATE TABLE")) {
                command = new StringBuilder(line);
            } else if (command.length() != 0) {
                command.append("\n").append(line);
            }

            if (line.endsWith(";")) {
                createTableCommands.add(command.toString());
                command = new StringBuilder();
            }
        }

        Path outputPath = Path.of(outputFileName);
        Files.write(outputPath, createTableCommands);
    }
}