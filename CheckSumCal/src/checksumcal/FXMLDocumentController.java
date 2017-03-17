/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package checksumcal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * When tuning the ECU(Engine control unit)file and you do not recalculate the
 * checksum the car will not start again The goal of this program is to correct
 * the tuned ECU file checksum that it will match the original one This program
 * takes in two files which are described in hexadecimal and the max file size
 * should be 32768 byte because this is the size of specific ECU flash memory.
 *
 * @author Ragnar
 */
public class FXMLDocumentController implements Initializable {

    private static final int MAX_FILE_SIZE = 32768;  // Max allowed file size in bytes
    private static final int MAX_BYTE_VALUE_IN_DEC = 65535; // IN HEX it is FFFF
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

    /**
     * This method is used both in selecting the original file and tuned file
     *
     * @param event
     */
    @FXML
    private void handleFileSelectButtonAction(ActionEvent event) {

        // This will open file chooser dialog window to select a file
        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage) root.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        // If file exists and do not exeed the max  file size
        if (file != null && file.length() <= MAX_FILE_SIZE) {

            // Figuring out which button was pressed
            if (event.getSource().equals(originalButton)) {
                originalLabel.textProperty().set(file.getName());

                // Original file button was pressed and we get the file
                mOriginalFile = file;
            } else {
                tunedLabel.textProperty().set(file.getName());

                // Original file button was pressed and we get the file
                mTunedFile = file;
            }
        } else {
            if (event.getSource().equals(originalButton)) {
                originalLabel.setTextFill(Color.web("#bf0101"));
                originalLabel.textProperty().set("PROBLEM WITH THE FILE");
            } else {
                tunedLabel.setTextFill(Color.web("#bf0101"));
                tunedLabel.textProperty().set("PROBLEM WITH THE FILE");
            }
        }

        // If both files are selected only then we can show and press the calculate button
        if (mOriginalFile != null && mTunedFile != null) {

            infoLabel.setText("");
            newChecksumButton.setDisable(false);
        }
    }

    @FXML
    private void handleCalculateButtonAction(ActionEvent event) {

        mOriginalChecksum = calculateChecksum(mOriginalFile.getAbsolutePath(), true);
        originalLabel.setText("Checksum: " + Integer.toHexString(mOriginalChecksum).toUpperCase());

        mTunedChecksum = calculateChecksum(mTunedFile.getAbsolutePath(), true);
        tunedLabel.setText("Checksum: " + Integer.toHexString(mTunedChecksum).toUpperCase());

        if (mOriginalChecksum == mTunedChecksum) {
            infoLabel.setText("SAME FILE");
        } else {
            infoLabel.setTextFill(Color.web("#bf0101"));
            infoLabel.setText("ERROR");
        }

        // Most of the time the tuned file checksum will be large because the chip tuner probably will rise map values
        if (mOriginalChecksum < mTunedChecksum) {

            mCorrectionValue = calculateCorrectionValue(checkSumDivision(mOriginalChecksum, mTunedChecksum));
            correctChecksum(mTunedFile.getAbsolutePath(), mCorrectionValue, mOriginalChecksum);
        }
        
        // DOTO: handle this rear case when the tuned checksum will be smaller than original
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        newChecksumButton.setDisable(true);

    }

    /**
     *
     * @param filePath
     * @param withTerminalPrintOut
     * @return calculated checksum
     */
    public final static int calculateChecksum(String filePath, boolean withTerminalPrintOut) {

        File f;
        long count = 0;
        int devisionResult;
        int multiplyResult;
        int result = 0;

        try {
            f = new File(filePath);
            int readByte;
            try (FileInputStream fis = new FileInputStream(f)) {
                while (fis.available() > 0) {
                    char[] line = new char[16];
                    for (int i = 0; i < 16; i++) {

                        // One byte is 8 bits and the max number can be 2^8=256
                        // This is the numeric value of that byte
                        readByte = fis.read();

                        count += readByte;

                        // This is for printing out the ECU map to terminal
                        if (withTerminalPrintOut) {

                            /* If the byte value is less than 16 the it will be the leading zero
                               because in hex we only need one place to describe a number until 16 
                               15 in hex is F and 16 in hex is 10 so from that on we need 2 places which means no leading 0 
                             */
                            String paddingZero = (readByte < 16) ? "0" : "";

                            // Just print out the hex value with or without the leading zero link 0F means 15
                            System.out.print(paddingZero + Integer.toHexString(readByte) + " ");

                            /* we only want to print out letter characters like a b c, 
                               according to ASCII Table they are between 33 and 126.
                               otherwise just print .
                             */
                            line[i] = (readByte >= 33 && readByte <= 126) ? (char) readByte : '.';
                            //System.out.println(new String(line));
                        }
                    }

                    if (withTerminalPrintOut) {
                        System.out.println(new String(line));
                    }
                    // If FFFF has been exeeded then the calculation will start from zero again
                    // We need to know how many times it will exeed the max value 
                    devisionResult = (int) count / MAX_BYTE_VALUE_IN_DEC;

                    // Now we need to calculate the remainder for correcting the new checksum
                    multiplyResult = devisionResult * MAX_BYTE_VALUE_IN_DEC;

                    // This is the remaider
                    result = (int) (count - multiplyResult);
                }
            }

            } catch (IOException e1) {

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
