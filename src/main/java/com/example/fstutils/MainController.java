package com.example.fstutils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fxmisc.richtext.InlineCssTextArea;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {
    @FXML
    private Label fileName;
    @FXML
    private Label type;
    @FXML
    private Label size;
    @FXML
    private Label labelDirectory;
    @FXML
    private InlineCssTextArea outputLogArea;
    @FXML
    private Button buttonSelectFile;
    @FXML
    private Button buttonConvert;
    @FXML
    private Button tabKeepScreen_buttonStartKeepScreen;
    @FXML
    private Button tabKeepScreen_buttonStop;

    @FXML
    private Button buttonChooseDirectory;
    @FXML
    private ProgressBar tabConvert_progressBar;
    @FXML
    private ProgressBar tabKeepScreen_progressBar;
    @FXML
    private CheckBox checkboxOpenFile;
    @FXML
    private CheckBox checkboxGetSourceFromClipBoard;
    private String pathDirectoryToSave;
    private File file;
    @FXML
    private ComboBox<String> tabKeepScreen_comboBoxChooseButton;
    @FXML
    private Label tabKeepScreen_logger;
    @FXML
    private TextField tabKeepScreen_second;
    private KeepScreenOn keepScreenOnClass;

    //TODO: keep current dir when choose file
    @FXML
    public void initialize() {
        initTabConvert();
        initTabKeepScreen();
    }
    private void initTabKeepScreen() {
        tabKeepScreen_logger.setText("");

        ObservableList<String> comboList = FXCollections.observableArrayList("NumLock", "ScrollLock");
        tabKeepScreen_comboBoxChooseButton.setItems(comboList);
        tabKeepScreen_comboBoxChooseButton.getSelectionModel().selectFirst();
    }
    private void initTabConvert() {
        clearInformationFile();
        File initialDirectory = new File(System.getProperty("user.home") + File.separator + "Documents");
        labelDirectory.setText(initialDirectory.getAbsolutePath());
        pathDirectoryToSave = initialDirectory.getAbsolutePath();
    }


    private void clearInformationFile() {
        if (this.file != null) {
            showLog("File " + fileName.getText() + " has been removed.");
        }
        this.file = null;
        fileName.setText("");
        type.setText("");
        size.setText("");
    }
    
    String backupDirectory;
    @FXML
    protected void onSelectFileClick(ActionEvent e) {

        Stage stage = (Stage) buttonSelectFile.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file..");
        String initDir;
        if (backupDirectory != null && !backupDirectory.isEmpty() && !backupDirectory.isBlank()) {
            initDir = backupDirectory;
        } else {
            initDir = System.getProperty("user.home") + File.separator + "Documents";
        }
               
        fileChooser.setInitialDirectory(new File(initDir));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files (*.json, *.txt)", "*.json", "*.txt"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            backupDirectory = file.getParent();
            
            this.file = file;
            String fileNameStr = file.getName();
            fileName.setText(fileNameStr);
            type.setText(Utils.getExtensionByStringHandling(fileNameStr).orElse(""));

            long bytes = file.length();
            double kilobytes = ((double) bytes / 1024);
            size.setText(String.format("%.2f KB", kilobytes));
            showLog("File '" + fileNameStr + "' has been chosen");
        } else {
            clearInformationFile();
        }
    }

    @FXML
    protected void startKeepScreen(ActionEvent e) {
        keepScreenOnClass = new KeepScreenOn();
        long ms;
        try {
            ms = Long.parseLong(tabKeepScreen_second.getText());
        } catch (Exception e1) {
            tabKeepScreen_logger.setText("Invalid second!");
            return;
        }
        if (ms < 6) {
            tabKeepScreen_logger.setText("Second should be greater than 6!");
            return;
        }

        keepScreenOnClass.setMilisSecond(ms * 1000);
        keepScreenOnClass.setButton(tabKeepScreen_comboBoxChooseButton.getValue());
        
        keepScreenOnClass.start();
        
        tabKeepScreen_logger.setText("Keep screen is running..");
        tabKeepScreen_progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        tabKeepScreen_buttonStartKeepScreen.setDisable(true);
        tabKeepScreen_buttonStop.setDisable(false);
        tabKeepScreen_comboBoxChooseButton.setDisable(true);
        tabKeepScreen_second.setDisable(true);
    }

    @FXML
    protected void stopKeepScreen(ActionEvent e) {
        if (keepScreenOnClass != null) {
            keepScreenOnClass.interrupt();
        }

        tabKeepScreen_logger.setText("Keep screen is stopped..");
        tabKeepScreen_progressBar.setProgress(0);
        tabKeepScreen_buttonStartKeepScreen.setDisable(false);
        tabKeepScreen_buttonStop.setDisable(true);
        tabKeepScreen_comboBoxChooseButton.setDisable(false);
        tabKeepScreen_second.setDisable(false);
    }

    @FXML
    protected void onActionChangeCheckboxClipboard(ActionEvent e) {

        if (checkboxGetSourceFromClipBoard.isSelected()) {
            clearInformationFile();
            buttonSelectFile.setDisable(true);
            showLog("Using source from clipboard.");
//            System.out.println(clipboard.getString());
        } else {
            showLog("Using source from file.");
            buttonSelectFile.setDisable(false);
        }
    }

    @FXML
    protected void onOpenDir(ActionEvent e) {
        Desktop desktop = Desktop.getDesktop();
        if (!validateDirectorySaveFile(pathDirectoryToSave)) {
            return;
        }
        try {
            File dirToOpen = new File(pathDirectoryToSave);
            desktop.open(dirToOpen);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    protected void onSelectDirectoryClick(ActionEvent e) {

        Stage stage = (Stage) buttonChooseDirectory.getScene().getWindow();


        DirectoryChooser directoryChooser = new DirectoryChooser();

        // Set the Dialog title
        directoryChooser.setTitle("Select a Directory");

        // Set initial directory
        File initialDirectory = new File(System.getProperty("user.home") + File.separator + "Downloads");
        directoryChooser.setInitialDirectory(initialDirectory);

        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            labelDirectory.setText(selectedDirectory.getAbsolutePath());
            pathDirectoryToSave = selectedDirectory.getAbsolutePath();
        } else {
            labelDirectory.setText("No directory selected.");
            pathDirectoryToSave = null;
        }
    }


    @FXML
    protected void onClickButtonConvert(ActionEvent e) {
        String contentToConvert;

        if (checkboxGetSourceFromClipBoard.isSelected()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            contentToConvert = clipboard.getString();
        } else {
            File file = this.file;
            if (!validateDirectorySaveFile(pathDirectoryToSave))
                return;
            if (!validateFile(file))
                return;

            Path path = file.toPath();
            try {
                contentToConvert = Files.readString(path);
            } catch (IOException ex) {
                showError("Read content of file fail!");
                return;
            }
        }


        if (contentToConvert == null || contentToConvert.isEmpty() || contentToConvert.isBlank()) {
            showError("Content to convert is empty!");
            return;
        }
        contentToConvert = contentToConvert.strip();

        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory() {
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }
        };
        Object obj;
        try {
            obj = parser.parse(contentToConvert, containerFactory);
        } catch (ParseException ex) {
            showError("Parse content error!");
            return;
        }

        LinkedList jsonArrayToConvert;
        if (obj instanceof LinkedList) {
            jsonArrayToConvert = (LinkedList) obj;
            if (jsonArrayToConvert.isEmpty()) {
                showError("JsonArray is empty!");
                return;
            }

        } else if (obj instanceof LinkedHashMap) {
            LinkedHashMap jsonObject = (LinkedHashMap) obj;

            PopupChooseFieldsController wc = new PopupChooseFieldsController();
            Map<String, LinkedList> map = wc.showStage(jsonObject);
            String idToGet = wc.getIdToGet();

            if (idToGet == null || idToGet.isEmpty() || idToGet.isBlank()) {
                showError("No field has been choose!");
                return;
            }

            jsonArrayToConvert = map.get(idToGet);
        } else {
            showError("Data not instance of jsonArray or jsonObject!");
            return;
        }

        if (jsonArrayToConvert.isEmpty()) {
            showError("Array is empty. No data to convert!");
            return;
        }

//        outputLogArea.clear();
        String fileName;
        if (checkboxGetSourceFromClipBoard.isSelected()) {
            SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy");
            fileName = "From_Clipboard_" + SIMPLE_DATE_FORMAT.format(new Date()) + ".xlsx";
        } else {
            fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            String name = fileName.substring(0,i);
            fileName = name + ".xlsx";
        }

        writeObjects2ExcelFile2(jsonArrayToConvert, pathDirectoryToSave + "\\" + fileName);
    }

    private boolean validateFile(File file) {
        boolean isValidFile = true;
        if (Objects.isNull(file)) {
            showError("Please choose a file first!");
            return false;
        }
        String extension = Utils.getExtensionByStringHandling(file.getName()).orElse(null);

        if (!List.of("json", "txt").contains(extension)) {
            isValidFile = false;
            showError("File extension not support!");
        }

        long bytes = file.length();
        if (bytes <= 0) {
            isValidFile = false;
            showError("File size is invalid!");
        }
        return isValidFile;
    }

    private boolean validateDirectorySaveFile(String pathDirectoryToSave) {
        if (pathDirectoryToSave == null || pathDirectoryToSave.isEmpty() || pathDirectoryToSave.isBlank()) {
            showError("Save to directory not valid!");
            return false;
        }

        Path path = Paths.get(pathDirectoryToSave);
        if (!Files.exists(path)) {
            showError("Save to directory not exist!");
            return false;
        }
        return true;
    }

    private void writeObjects2ExcelFile(LinkedList jsonArray, String filePath) {
        buttonConvert.setDisable(true);
        // Create a background Task
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    int progress = 0;
                    updateProgress(progress++, 10);

                    LinkedHashMap firstObj = (LinkedHashMap) jsonArray.get(0);
                    Set<String> header = firstObj.keySet();

                    String[] COLUMNs = new String[header.size()];
                    header.toArray(COLUMNs);
                    showLog("Creating excel file..");
                    Workbook workbook = new XSSFWorkbook();
                    updateProgress(progress++, 10);


                    CreationHelper createHelper = workbook.getCreationHelper();
                    showLog("Creating sheet 'result'..");
                    Sheet sheet = workbook.createSheet("result");
                    updateProgress(progress++, 10);

                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
//                    headerFont.setColor(IndexedColors.BLUE.getIndex());

                    CellStyle headerCellStyle = workbook.createCellStyle();
                    headerCellStyle.setFont(headerFont);

                    // Header
                    showLog("Creating header..");
                    int rowIdx = 0;

                    Row headerRow = sheet.createRow(rowIdx++);
                    Object temp = ((LinkedHashMap) jsonArray.get(0)).get(COLUMNs[0]);
                    if (COLUMNs.length == 1 && temp instanceof LinkedHashMap) {
                        List<String> setFields1 = new ArrayList<>((Set<String>) (((LinkedHashMap) temp).keySet()));
                        Row headerRow2 = sheet.createRow(rowIdx++);
                        for (int i = 0; i < setFields1.size(); i++) {
                            Cell cell = headerRow.createCell(i);
                            cell.setCellValue(COLUMNs[0]);
                            cell.setCellStyle(headerCellStyle);
                            Cell cell2 = headerRow2.createCell(i);
                            cell2.setCellValue(setFields1.get(i));
                            cell2.setCellStyle(headerCellStyle);
                            sheet.autoSizeColumn(i, true);
                        }

                        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, setFields1.size() - 1));
                    } else {
                        for (int col = 0; col < COLUMNs.length; col++) {
                            Cell cell = headerRow.createCell(col);
                            cell.setCellValue(COLUMNs[col]);
                            cell.setCellStyle(headerCellStyle);
                            sheet.autoSizeColumn(col);
                            
                        }
                    }


                    updateProgress(progress++, 10);

                    showLog("Creating row data..");
                    Object[] arrayObj = jsonArray.toArray();
                    List<Object> list = Arrays.stream(arrayObj).collect(Collectors.toList());
                    double d = list.size() / 6d;
                    int dive = (int) Math.ceil(d);
                    List<List<Object>> fullList = Utils.partition(list, dive);
                    int countRecord = 0;
                    for (List<Object> patrionList : fullList) {
                        countRecord += patrionList.size();
                        for (Object o : patrionList) {

                            LinkedHashMap component = (LinkedHashMap) o;

                            Row row = sheet.createRow(rowIdx++);

                            for (int j = 0; j < COLUMNs.length; j++) {
                                Object value = (component.get(COLUMNs[j]));
                                if (value instanceof LinkedHashMap) {
                                    LinkedHashMap valueObject = (LinkedHashMap) value;
                                    
                                    int idx = 0;
                                    for (String s : (Set<String>) valueObject.keySet()) {
                                        String valueToSet = String.valueOf(valueObject.get(s));
                                        if (Utils.isNumeric(valueToSet)) {
                                            row.createCell(idx++).setCellValue(Double.parseDouble(valueToSet));
                                        } else {
                                            row.createCell(idx++).setCellValue(valueToSet);
                                        }
                                    }
                                } else {
                                    String valueToSet = String.valueOf(value);
                                    if (Utils.isNumeric(valueToSet)) {
                                        row.createCell(j).setCellValue(Double.parseDouble(valueToSet));
                                    } else {
                                        row.createCell(j).setCellValue(valueToSet);
                                    }
                                }
                            }
                        }
                        showLog("Created " + countRecord + " of " + list.size() + " row.");
                        updateProgress(progress++, 10);
                    }

                    showLog("Saving file to directory..");
                    FileOutputStream fileOut = new FileOutputStream(filePath);
                    workbook.write(fileOut);
                    fileOut.close();
                    workbook.close();
                    updateProgress(10, 10);
                    showSuccess("File has been saved to '" + filePath + "'.");

                    return filePath;
                } catch (Exception e) {
                    updateProgress(0, 10);
                    throw e;
                }
                finally {
                    buttonConvert.setDisable(false);
                }

            }
        };

        // This method allows us to handle any Exceptions thrown by the task
        task.setOnFailed(wse -> {
            showError("Convert not success with exception: " + wse.getSource().getException().getMessage());
            wse.getSource().getException().printStackTrace();
        });

        // If the task completed successfully, perform other updates here
        task.setOnSucceeded(wse -> {
            showSuccess("Convert json successfully!");
            if (checkboxOpenFile.isSelected()) {
                showSuccess("File will be auto open..!");
                try {
                    Desktop desktop = Desktop.getDesktop();
                    File fileToOpen = new File(filePath);
                    desktop.open(fileToOpen);
                } catch (Exception ex) {
                    showError("Open file not success with exception: " + ex.getMessage());
                }
            }
        });

//        task.setOnCancelled(wse -> {
//            buttonConvert.setOnAction(e -> {onClickButtonConvert()});
//            
//        });
//        buttonConvert.setText("Stop");
//        buttonConvert.setOnAction(e -> task.cancel());

        // Before starting our task, we need to bind our UI values to the properties on the task
        tabConvert_progressBar.progressProperty().bind(task.progressProperty());

        // Now, start the task on a background thread
        new Thread(task).start();
    }

 
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void writeObjects2ExcelFile2(LinkedList jsonArray, String filePath) {
        buttonConvert.setDisable(true);
        // Create a background Task
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    int progress = 0;
                    updateProgress(progress++, 10);

              
                    showLog("Creating excel file..");
                    Workbook workbook = new XSSFWorkbook();
                    updateProgress(progress++, 10);


                    CreationHelper createHelper = workbook.getCreationHelper();
                    showLog("Creating sheet 'result'..");
                    Sheet sheet = workbook.createSheet("result");
                    updateProgress(progress++, 10);

                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
//                    headerFont.setColor(IndexedColors.BLUE.getIndex());

                    CellStyle headerCellStyle = workbook.createCellStyle();
                    headerCellStyle.setFont(headerFont);

                    // Header
                    showLog("Creating header..");

                    int colIdx = 0;
                    Row headerRow = sheet.createRow(0);

                    int idx = 0;
                    LinkedHashMap firstObj = (LinkedHashMap) jsonArray.get(idx);
                    
                    Map<String, Integer> mapHeaderWithIdxColumn = new HashMap<>();
                    
                    for (String field :  (Set<String>) firstObj.keySet()) {
                        Object fieldData = firstObj.get(field);

                        while (fieldData == null && idx < jsonArray.size()) {
                            fieldData = ((LinkedHashMap) jsonArray.get(idx++)).get(field);
                        }
                        idx = 0;
                        
                        if (!(fieldData instanceof LinkedList)) {
                            mapHeaderWithIdxColumn.put(field, colIdx);
                            
                            setValue(headerRow, colIdx++, field, headerCellStyle);
                        } else {
                            LinkedList child = (LinkedList) fieldData;

                            while ((child == null || child.isEmpty()) && idx < jsonArray.size()) {
                                child = (LinkedList) ((LinkedHashMap) jsonArray.get(idx++)).get(field);
                            }

                            if (child != null && !child.isEmpty()) {
                                LinkedHashMap firstObjChild = (LinkedHashMap) child.get(0);

                                Set<String> headerChildSet = (Set<String>) firstObjChild.keySet();
                                for (String headerChild : headerChildSet) {
                                    mapHeaderWithIdxColumn.put(field + "__" + headerChild, colIdx);

                                    setValue(headerRow, colIdx++, field + "__" + headerChild, headerCellStyle);
                                    
                                }
                            } else {
                                mapHeaderWithIdxColumn.put(field , colIdx);
                                setValue(headerRow, colIdx++, field , headerCellStyle);
                            }
                        }
                    }
                    
                    updateProgress(progress++, 10);

                    showLog("Creating row data..");
                    Object[] arrayObj = jsonArray.toArray();
                    List<Object> list = Arrays.stream(arrayObj).collect(Collectors.toList());
                    List<List<Object>> fullList = Utils.partition(list, (int) Math.ceil(list.size() / 6d));
                    
                    int countRecord = 0;
                    int rowIdx = 1;
                    
                    for (List<Object> patrionList : fullList) {
                        countRecord += patrionList.size();
                        
                        for (Object o : patrionList) {
                            LinkedHashMap component = (LinkedHashMap) o;

                            int backupRowIdx = rowIdx;
                            int maxRow = rowIdx;
                            Row row = sheet.createRow(rowIdx);

                            for (String field : (Set<String>) component.keySet()) {
                                Object fieldData = component.get(field);

                                if (fieldData instanceof LinkedList) {
                                    LinkedList childData = (LinkedList) fieldData;
                                    if (childData.isEmpty()) {
                                        continue;
                                    }
                                    for (Object object : childData) {
                                        if (rowIdx > maxRow) {
                                            maxRow = rowIdx;
                                        }
                                        LinkedHashMap child = (LinkedHashMap) object;
                                       
                                        for (String fieldOfChild : (Set<String>) child.keySet()) {
                                            String key = field + "__" + fieldOfChild;
                                            int idxHeader = mapHeaderWithIdxColumn.get(key);
                                            setValue(row, idxHeader, child.get(fieldOfChild));
                                        }
                                       
                                        rowIdx++;
                                        row = sheet.getRow(rowIdx) == null ? sheet.createRow(rowIdx) : sheet.getRow(rowIdx);
                                    }

                                    row = sheet.getRow(backupRowIdx);
                                    rowIdx = backupRowIdx;
                                } else {
                                    Integer idxHeader = mapHeaderWithIdxColumn.getOrDefault(field, null);
                                    if (idxHeader != null) {
                                        setValue(row, idxHeader, component.get(field));
                                    }
                                }
                            }
                            rowIdx = maxRow + 1;
                        }
                        
                        showLog("Created " + countRecord + " of " + list.size() + " row.");
                        updateProgress(progress++, 10);
                    }

                    showLog("Saving file to directory..");
                    FileOutputStream fileOut = new FileOutputStream(filePath);
                    workbook.write(fileOut);
                    fileOut.close();
                    workbook.close();
                    updateProgress(10, 10);
                    showSuccess("File has been saved to '" + filePath + "'.");

                    return filePath;
                } catch (Exception e) {
                    updateProgress(0, 10);
                    throw e;
                }
                finally {
                    buttonConvert.setDisable(false);
                }

            }
        };

        // This method allows us to handle any Exceptions thrown by the task
        task.setOnFailed(wse -> {
            showError("Convert not success with exception: " + wse.getSource().getException().getMessage());
            wse.getSource().getException().printStackTrace();
        });

        // If the task completed successfully, perform other updates here
        task.setOnSucceeded(wse -> {
            showSuccess("Convert json successfully!");
            if (checkboxOpenFile.isSelected()) {
                showSuccess("File will be auto open..!");
                try {
                    Desktop desktop = Desktop.getDesktop();
                    File fileToOpen = new File(filePath);
                    desktop.open(fileToOpen);
                } catch (Exception ex) {
                    showError("Open file not success with exception: " + ex.getMessage());
                }
            }
        });

//        task.setOnCancelled(wse -> {
//            buttonConvert.setOnAction(e -> {onClickButtonConvert()});
//            
//        });
//        buttonConvert.setText("Stop");
//        buttonConvert.setOnAction(e -> task.cancel());

        // Before starting our task, we need to bind our UI values to the properties on the task
        tabConvert_progressBar.progressProperty().bind(task.progressProperty());

        // Now, start the task on a background thread
        new Thread(task).start();
    }

    public void setValue(Row row, int idxColumn, Object value) {
        setValue(row, idxColumn, value, null);
    }    
    public void setValue(Row row, int idxColumn, Object value, CellStyle style) {
        String valueToSet = String.valueOf(value);
        Cell cell = row.createCell(idxColumn);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (Utils.isNumeric(valueToSet)) {
            cell.setCellValue(Double.parseDouble(valueToSet));
        } else {
            cell.setCellValue(valueToSet);
        }
    }
    public void showLog(String text) {
        Platform.runLater(() -> {
            outputLogArea.append(" * " + text + "\n", "-fx-fill: black;");
            moveCaretToEnd();
        });
    }

    public void showError(String text) {
        Platform.runLater(() -> {
            outputLogArea.append(" * " + text + "\n", "-fx-fill: red;");
            moveCaretToEnd();
        });

    }

    public void showSuccess(String text) {
        Platform.runLater(() -> {
            outputLogArea.append(" * " + text + "\n", "-fx-fill: blue;");
            moveCaretToEnd();
        });
    }

    private void moveCaretToEnd() {
        outputLogArea.moveTo(outputLogArea.getLength());
        outputLogArea.requestFollowCaret();
    }
}