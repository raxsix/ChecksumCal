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
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Ragnar
 */
public class CheckSumCal extends Application {

    private static final int MAX_FILE_SIZE = 32768;
    private static final String TEMP_FILE_NAME = "temp.bin";
    private static final String RESULT_FILE_NAME = "result.bin";

    private int mOriginalChecksum;
    private int mTunedChecksum;
    private int mCorrectionValue;

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));

        Scene scene = new Scene(root);

        // stage.setScene(scene);
        // stage.show();
        mOriginalChecksum = calculateChecksum("test.bin");

        mTunedChecksum = calculateChecksum("test2.bin");

        mCorrectionValue = calculateCorrectionValue(checkSumDivision(mOriginalChecksum, mTunedChecksum));

        System.out.println("OriginalChecksum: " + mOriginalChecksum);
        System.out.println("TunedChecksum: " + mTunedChecksum);

        System.out.println("Division: " + checkSumDivision(mOriginalChecksum, mTunedChecksum));
        System.out.println("Division in Hex: " + Integer.toHexString(checkSumDivision(mOriginalChecksum, mTunedChecksum)));

        System.out.println("Correction: " + calculateCorrectionValue(checkSumDivision(mOriginalChecksum, mTunedChecksum)));

        correctChecksum("test2.bin", mCorrectionValue, mOriginalChecksum);
    }

    private static void correctChecksum(String filePath, int correctionValue, int originalChecksum) {

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

                if (i % 16 == 0 && array[i] == 255 && array[i + 1] == 255 && array[i + 2] == 255) {

                    for (int j = 0; j < correctionValue; j++) {

                        if (array[i + j] == 255) {

                            count++;
                        }
                    }

                    if (count == correctionValue) {

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

            int tempChecksum = calculateChecksum(TEMP_FILE_NAME);

            File tempFile = new File(TEMP_FILE_NAME);
            tempFile.delete();

            array[placeToWrite + correctionValue] = finalCorrection(checkSumDivision(tempChecksum, originalChecksum));

            try (OutputStream os = new FileOutputStream(RESULT_FILE_NAME)) {
                for (int c : array) {
                    os.write(c);
                }
                os.close();
            }

            int resultChecksum = calculateChecksum(RESULT_FILE_NAME);

            if (resultChecksum == originalChecksum) {
                System.out.println("CONCRATULATIONS TUNED CHECKSUM == ORIGINAL");
            }

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

    public final static int calculateChecksum(String filePath) {

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

//                        String paddingZero = (readByte < 16) ? "0" : "";
//                        System.out.print(paddingZero + Integer.toHexString(readByte) + " ");
//                        line[i] = (readByte >= 33 && readByte <= 126) ? (char) readByte : '.';
                    }

                    //System.out.println(new String(line));
                }

                devisionResult = (int) count / 65536;
                multiplyResult = devisionResult * 65536;
                result = (int) (count - multiplyResult);

            }

        } catch (Exception e1) {

        }

        return result;
    }

}
