/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package checksumcal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author Ragnar
 */
public class FXMLDocumentController implements Initializable {

    private static final int MAX_FILE_SIZE = 32768;
    private static final String TEMP_FILE_NAME = "temp.bin";
    private static final String RESULT_FILE_NAME = "result.bin";

    private int mOriginalChecksum;
    private int mTunedChecksum;
    private int mCorrectionValue;
    private File mOriginalFile;
    private File mTunedFile;

    @FXML
    private Button originalButton;

    @FXML
    private Button tunedButton;

    @FXML
    private Button newChecksumButton;

    @FXML
    private Label originalLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private Label tunedLabel;

    @FXML
    private Label infoLabel;

    @FXML
    private Parent root;

    @FXML
    private void handleOriginalButtonAction(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage) root.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {

            if (event.getSource().equals(originalButton)) {
                originalLabel.textProperty().set(file.getName());
                mOriginalFile = file;
            } else {
                tunedLabel.textProperty().set(file.getName());
                mTunedFile = file;
            }
        }

        // If both files are selected only then we can hit the calculate button
        if (mOriginalFile != null && mTunedFile != null) {
            newChecksumButton.setDisable(false);
        }
    }

    @FXML
    private void handleCalculatedButtonAction(ActionEvent event) {

        mOriginalChecksum = calculateChecksum(mOriginalFile.getAbsolutePath(), false);
        originalLabel.setText("Checksum: " + Integer.toHexString(mOriginalChecksum).toUpperCase());

        mTunedChecksum = calculateChecksum(mTunedFile.getAbsolutePath(), false);
        tunedLabel.setText("Checksum: " + Integer.toHexString(mTunedChecksum).toUpperCase());

        if (mOriginalChecksum < mTunedChecksum) {

            mCorrectionValue = calculateCorrectionValue(checkSumDivision(mOriginalChecksum, mTunedChecksum));
            correctChecksum(mTunedFile.getAbsolutePath(), mCorrectionValue, mOriginalChecksum);
        } else {
            infoLabel.setTextFill(Color.web("#bf0101"));
            infoLabel.setText("ERROR");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        newChecksumButton.setDisable(true);

    }

    public final static int calculateChecksum(String filePath, boolean withPrint) {

        File f;
        long count = 0;
        int devisionResult;
        int multiplyResult;
        int result = 0;
        int intex = 0;

        try {
            f = new File(filePath);
            int readByte;
            try (FileInputStream fis = new FileInputStream(f)) {
                while (fis.available() > 0) {
                    char[] line = new char[16];
                    for (int i = 0; i < 16; i++) {

                        readByte = fis.read();

                        count += readByte;

                        if (withPrint) {
                            String paddingZero = (readByte < 16) ? "0" : "";
                            System.out.print(paddingZero + Integer.toHexString(readByte) + " ");
                            line[i] = (readByte >= 33 && readByte <= 126) ? (char) readByte : '.';
                        }
                    }

                    if (withPrint) {
                        System.out.println(new String(line));
                    }

                }

                devisionResult = (int) count / 65536;
                multiplyResult = devisionResult * 65536;
                result = (int) (count - multiplyResult);

            }

        } catch (Exception e1) {

        }
        return result;
    }

    private void correctChecksum(String filePath, int correctionValue, int originalChecksum) {

        File f;
        int intex = 0;
        int count = 0;
        int placeToWrite = 0;

        try {
            f = new File(filePath);
            int[] array = new int[(int) f.length()];
            int readByte;
            try (FileInputStream fis = new FileInputStream(f)) {

                while (fis.available() > 0) {

                    for (int i = 0; i < 16; i++) {

                        readByte = fis.read();
                        array[intex] = readByte;
                        intex++;
                    }
                }
            }

            for (int i = 0; i < array.length; i++) {

                if (i % 16 == 0) {

                    for (int j = 0; j < correctionValue + 32; j++) {

                        if (array[i + j] == 255) {

                            count++;
                        }
                    }

                    if (count == (correctionValue + 32)) {

                        System.out.println("right place is: " + i + "->" + (i + correctionValue));
                        placeToWrite = i;
                        break;
                    }
                    count = 0;
                }
            }

            for (int i = placeToWrite; i < (placeToWrite + correctionValue); i++) {

                array[i] = 0;
            }

            try (OutputStream os = new FileOutputStream(TEMP_FILE_NAME)) {
                for (int c : array) {
                    os.write(c);
                }
                os.close();
            }

            int tempChecksum = calculateChecksum(TEMP_FILE_NAME, false);

            File tempFile = new File(TEMP_FILE_NAME);
            tempFile.delete();

            array[placeToWrite + correctionValue] = finalCorrection(checkSumDivision(tempChecksum, originalChecksum));

            try (OutputStream os = new FileOutputStream(RESULT_FILE_NAME)) {
                for (int c : array) {
                    os.write(c);
                }
                os.close();
            }
            int resultChecksum = calculateChecksum(RESULT_FILE_NAME, true);

            resultLabel.setText("result.bin Checksum is: " + Integer.toHexString(resultChecksum).toUpperCase());

            if (resultChecksum == originalChecksum) {
                System.out.println("VICTORY! TUNED CHECKSUM == ORIGINAL");
                infoLabel.setTextFill(Color.web("#00a108"));
                infoLabel.setText("SUCCESS!");
            } else {
                infoLabel.setTextFill(Color.web("#bf0101"));
                infoLabel.setText("SOMETHING WENT WRONG");
            }
            System.out.println("changes made: " + placeToWrite + "->" + (placeToWrite + correctionValue));

        } catch (Exception e1) {

        }
    }

    private static int finalCorrection(int divisionResult) {

        return 255 - divisionResult;
    }

    private static int calculateCorrectionValue(int divisionResult) {

        return divisionResult / 255;
    }

    public static int checkSumDivision(int original, int tuned) {

        return Math.abs(tuned - original);
    }

}
