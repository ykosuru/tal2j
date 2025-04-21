package converted;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; // For simulating terminal input

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Wrapper class for simulating pass-by-reference for TAL INT parameters.
class IntWrapper {
    public int value;
    public IntWrapper() { this.value = 0; }
    public IntWrapper(int value) { this.value = value; }
    @Override public String toString() { return Integer.toString(value); }
}

/**
 * Simulates Guardian OS calls and environment for file handling.
 */
class GuardianInterface {
    private static final Logger logger = LogManager.getLogger(GuardianInterface.class);
    private static final Scanner consoleScanner = new Scanner(System.in); // For simulating terminal input

    // File system simulation state
    private static int nextFileNum = 10; // Start assigning file numbers from 10
    private static Map<Integer, MockFile> openFiles = new HashMap<>();
    private static final String MOCK_TERMINAL_NAME = "$TERM "; // Padded like TAL might
    private static final String MOCK_INPUT_CONTENT = "Line 1 of input file.\nSecond line here.\nEnd of file content.";
    private static final String MOCK_BAD_FILENAME = "BADFILE.DAT";

    // Mock file state class
    private static class MockFile {
        String fileName;
        ByteBuffer data; // Holds the actual file content for simulation
        boolean isOpen = true;
        int lastError = 0; // Stores the error code for the last operation
        boolean isTerminal = false;
        boolean isOutput = false;

        // Constructor for regular files
        MockFile(String name, ByteBuffer content, boolean output) {
            this.fileName = name;
            this.isOutput = output;
            if (content != null) {
                // Duplicate the buffer
                this.data = ByteBuffer.allocate(content.capacity()).order(ByteOrder.LITTLE_ENDIAN);
                this.data.put(content.array());
                this.data.position(0); // Ready for reading
            } else {
                // For output files, start with an empty, expandable buffer (simulated)
                this.data = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN); // Initial capacity
                this.data.limit(0); // Initially empty
            }
        }
        // Constructor for terminal
        MockFile(String name, boolean terminal) {
             this.fileName = name;
             this.isTerminal = terminal;
             this.data = null; // Terminal doesn't have pre-defined content buffer
        }

        // Simulate writing to an output file buffer
        void writeData(byte[] bytesToWrite, int length) {
            if (!isOutput && !isTerminal) {
                logger.error("Attempt to write to non-output file: {}", fileName);
                lastError = 9; // Permission denied / Invalid operation
                return;
            }
            if (isTerminal) {
                 System.out.print(new String(bytesToWrite, 0, length, StandardCharsets.ISO_8859_1));
                 lastError = 0;
                 return;
            }

            int writeLen = Math.min(length, bytesToWrite.length);
            // Ensure capacity
            if (data.position() + writeLen > data.capacity()) {
                // Simulate resizing the buffer (Java ByteBuffer needs explicit reallocation)
                int newCapacity = Math.max(data.capacity() * 2, data.position() + writeLen);
                ByteBuffer newData = ByteBuffer.allocate(newCapacity).order(ByteOrder.LITTLE_ENDIAN);
                data.flip(); // Prepare old buffer for reading
                newData.put(data); // Copy old data
                data = newData; // Replace with new buffer
                 logger.trace("Resized output buffer for {} to {}", fileName, newCapacity);
            }
            data.put(bytesToWrite, 0, writeLen);
            lastError = 0;
        }

         // Simulate reading from an input file buffer
        int readData(byte[] buffer, int readMax) {
             if (isOutput || isTerminal || data == null) {
                 logger.error("Attempt to read from non-input file or terminal: {}", fileName);
                 lastError = 9; // Invalid operation
                 return -1; // Indicate error immediately
             }
             if (!data.hasRemaining()) {
                 lastError = 11; // EOF
                 return 0; // Indicate EOF by returning 0 bytes read
             }
             int bytesToRead = Math.min(readMax, data.remaining());
             bytesToRead = Math.min(bytesToRead, buffer.length); // Don't read more than buffer can hold
             data.get(buffer, 0, bytesToRead);
             lastError = 0;
             return bytesToRead;
        }
    }

    /**
     * Simulates MYTERM - gets the home terminal name.
     */
    public static int myTerm(byte[] nameBuffer) {
        logger.debug("MYTERM called.");
        byte[] termNameBytes = MOCK_TERMINAL_NAME.getBytes(StandardCharsets.ISO_8859_1);
        int len = Math.min(termNameBytes.length, nameBuffer.length);
        Arrays.fill(nameBuffer, (byte) ' '); // Pad buffer first
        System.arraycopy(termNameBytes, 0, nameBuffer, 0, len);
        logger.debug("MYTERM returning name: '{}'", MOCK_TERMINAL_NAME.trim());
        return 0; // Success
    }

    /**
     * Simulates OPEN - opens a file or device.
     * Overload for standard open (read/update).
     */
    public static int open(byte[] filenameBytes, IntWrapper fileNumWrapper) {
        return open(filenameBytes, fileNumWrapper, 0); // Default access mode
    }

    /**
     * Simulates OPEN - opens a file or device with access mode.
     * access = 1 simulates create/exclusive write for output files.
     */
    public static int open(byte[] filenameBytes, IntWrapper fileNumWrapper, int access) {
        String filename = new String(filenameBytes, StandardCharsets.ISO_8859_1).trim();
        logger.debug("OPEN called for filename: '{}', access: {}", filename, access);

        if (filename.equals(MOCK_TERMINAL_NAME.trim())) {
            int fileNum = nextFileNum++;
            fileNumWrapper.value = fileNum;
            openFiles.put(fileNum, new MockFile(filename, true)); // Mark as terminal
            logger.info("Opened terminal '{}' as fileNum {}", filename, fileNum);
            return 0; // Success
        }

        if (filename.equals(MOCK_BAD_FILENAME)) {
            fileNumWrapper.value = -1;
            logger.error("Simulating OPEN error 4 (File Not Found) for file: {}", filename);
            // Error state needs to be retrievable by FILEINFO for fileNum -1
            return -1; // Immediate error
        }

        // Simulate opening a regular file
        int fileNum = nextFileNum++;
        fileNumWrapper.value = fileNum;
        boolean isOutput = (access == 1); // Treat access 1 as output/create

        ByteBuffer content = null;
        if (!isOutput) {
            // Simulate content for input file
            content = ByteBuffer.wrap(MOCK_INPUT_CONTENT.getBytes(StandardCharsets.ISO_8859_1));
        }

        openFiles.put(fileNum, new MockFile(filename, content, isOutput));
        logger.info("Opened file '{}' as fileNum {} (Output={})", filename, fileNum, isOutput);
        return 0; // Success
    }

    /**
     * Simulates CLOSE.
     */
    public static int close(int fileNum) {
        logger.debug("CLOSE called for fileNum {}", fileNum);
        MockFile file = openFiles.remove(fileNum);
        if (file != null) {
            if (file.isOpen) {
                file.isOpen = false;
                logger.info("File {} ('{}') closed.", fileNum, file.fileName);
                return 0; // Success
            } else {
                logger.warn("CLOSE warning: File {} ('{}') already marked closed.", fileNum, file.fileName);
                return -1; // Indicate warning/error
            }
        } else {
            logger.warn("CLOSE warning: Attempt to close non-existent fileNum {}", fileNum);
            return -1; // Indicate error/warning
        }
    }

    /**
     * Simulates WRITEREAD - writes to terminal, reads response.
     */
    public static int writeRead(int fileNum, byte[] buffer, int writeCount, int readMax, IntWrapper bytesReadWrapper) {
        logger.debug("WRITEREAD called for fileNum {}, writeCount={}, readMax={}", fileNum, writeCount, readMax);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen || !file.isTerminal) {
            logger.error("WRITEREAD error: fileNum {} is not an open terminal.", fileNum);
            if (file != null) file.lastError = 10; // File not open or invalid type
            bytesReadWrapper.value = 0;
            return -1; // Error
        }

        // Simulate write prompt
        String prompt = new String(buffer, 0, Math.min(writeCount, buffer.length), StandardCharsets.ISO_8859_1);
        System.out.print(prompt); // Write prompt to console without newline

        // Simulate read response
        String input = "";
        try {
             input = consoleScanner.nextLine();
        } catch (Exception e) {
             logger.error("Error reading from console scanner", e);
             file.lastError = 1; // Generic I/O error
             bytesReadWrapper.value = 0;
             return -1;
        }

        byte[] inputBytes = input.getBytes(StandardCharsets.ISO_8859_1);
        int bytesToCopy = Math.min(inputBytes.length, readMax);
        bytesToCopy = Math.min(bytesToCopy, buffer.length - writeCount); // Ensure space in buffer

        if (bytesToCopy < 0) bytesToCopy = 0; // Handle case where writeCount >= buffer.length

        // Copy read data into the buffer *after* the prompt data
        System.arraycopy(inputBytes, 0, buffer, writeCount, bytesToCopy);
        bytesReadWrapper.value = bytesToCopy;
        logger.debug("WRITEREAD read {} bytes: '{}'", bytesToCopy, input.substring(0, bytesToCopy));

        file.lastError = 0;
        return 0; // Success
    }

    /**
     * Simulates READ.
     */
    public static int read(int fileNum, byte[] buffer, int readMax, IntWrapper bytesReadWrapper) {
        logger.debug("READ called for fileNum {}, readMax={}", fileNum, readMax);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("READ error: fileNum {} is not open.", fileNum);
            if (file != null) file.lastError = 10;
            bytesReadWrapper.value = 0;
            return -1; // Error
        }
         if (file.isOutput || file.isTerminal) {
             logger.error("READ error: Attempt to read from output file or terminal {}.", fileNum);
             file.lastError = 9; // Invalid operation
             bytesReadWrapper.value = 0;
             return -1;
         }

        int bytesRead = file.readData(buffer, readMax);

        if (bytesRead == 0 && file.lastError == 11) { // Check for EOF condition set by readData
             logger.warn("READ detected EOF for fileNum {}", fileNum);
             bytesReadWrapper.value = 0;
             return -1; // Return error code for EOF
        } else if (bytesRead < 0) { // Check for other read errors
             logger.error("READ error occurred for fileNum {}", fileNum);
             bytesReadWrapper.value = 0;
             return -1; // Return error code
        } else {
             bytesReadWrapper.value = bytesRead;
             logger.debug("READ successful for fileNum {}, read {} bytes.", fileNum, bytesRead);
             return 0; // Success
        }
    }

    /**
     * Simulates WRITE.
     */
    public static int write(int fileNum, byte[] buffer, int writeCount) {
        logger.debug("WRITE called for fileNum {}, writeCount={}", fileNum, writeCount);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("WRITE error: fileNum {} is not open.", fileNum);
             if (file != null) file.lastError = 10;
            return -1; // Error
        }
         if (!file.isOutput && !file.isTerminal) {
             logger.error("WRITE error: Attempt to write to non-output file {}.", fileNum);
             file.lastError = 9; // Invalid operation
             return -1;
         }

        file.writeData(buffer, writeCount);
        if (file.lastError != 0) {
             logger.error("WRITE error occurred for fileNum {}. Error code: {}", fileNum, file.lastError);
             return -1;
        }

        logger.debug("WRITE successful for fileNum {}.", fileNum);
        return 0; // Success
    }

    /**
     * Simulates FILEINFO.
     */
    public static void fileInfo(int fileNum, IntWrapper errorWrapper) {
        logger.debug("FILEINFO called for fileNum {}", fileNum);
        MockFile file = openFiles.get(fileNum);
        if (fileNum == -1) {
            // Error occurred during OPEN before a valid fileNum was assigned
            errorWrapper.value = 4; // Simulate file system error (e.g., Not Found/Permission from OPEN)
            logger.warn("FILEINFO returning error 4 for invalid fileNum {}", fileNum);
        } else if (file != null && file.isOpen) {
            errorWrapper.value = file.lastError;
            logger.debug("FILEINFO returning last error {} for fileNum {}", file.lastError, fileNum);
            file.lastError = 0; // Clear error after reporting
        } else {
            errorWrapper.value = 10; // File not open error
            logger.warn("FILEINFO returning error 10 (File Not Open) for fileNum {}", fileNum);
        }
    }

    // Stubs for other system calls mentioned but not used in this specific logic
    public static void awaitio(int fileNum) {
        logger.debug("STUB: AWAITIO called for fileNum {}", fileNum);
        // No action needed in this simulation
    }

    public static void position(int fileNum /* other params */) {
        logger.debug("STUB: POSITION called for fileNum {}", fileNum);
        // No action needed in this simulation
    }

     /**
     * Simulates the $INT function (basic version).
     * In TAL, this might handle various types. Here, we assume it's just returning the int value.
     */
    public static int dollarInt(int value) {
        // In this context, it seems to be used just before converting to a character.
        // It might be intended to handle potential non-INT types, but here it's redundant.
        return value;
    }
}

/**
 * Translated Java code for the TAL program sample7.tal.txt
 */
public class Sample7 {

    private static final Logger logger = LogManager.getLogger(FileHandlingDemo.class);

    // Helper method to copy bytes and pad with spaces, simulating TAL string assignment
    private static void assignBytes(byte[] dest, int destOffset, byte[] src, int srcOffset, int count) {
        int bytesToCopy = Math.min(count, src.length - srcOffset);
        bytesToCopy = Math.min(bytesToCopy, dest.length - destOffset); // Ensure fits in dest

        if (bytesToCopy < 0) bytesToCopy = 0;

        // Copy the source bytes
        System.arraycopy(src, srcOffset, dest, destOffset, bytesToCopy);

        // Pad remaining space in the destination segment (up to count) with spaces
        if (bytesToCopy < count) {
            int paddingStart = destOffset + bytesToCopy;
            int paddingCount = count - bytesToCopy;
            paddingCount = Math.min(paddingCount, dest.length - paddingStart); // Ensure padding fits
            if (paddingCount > 0) {
                Arrays.fill(dest, paddingStart, paddingStart + paddingCount, (byte) ' ');
            }
        }
    }

     // Helper method to assign a String literal to a byte array
    private static void assignStringLiteral(byte[] dest, String literal) {
         byte[] literalBytes = literal.getBytes(StandardCharsets.ISO_8859_1);
         assignBytes(dest, 0, literalBytes, 0, dest.length); // Assign and pad to full length
         // Ensure null termination if needed (though TAL often relies on length)
         if (dest.length > 0) dest[Math.min(literalBytes.length, dest.length - 1)] = 0;
    }


    /**
     * Simulates the TAL PROC file_handling MAIN;
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        logger.info("Starting file_handling simulation...");

        // --- Variable Declarations ---
        // File numbers
        IntWrapper terminalWrapper = new IntWrapper(-1); // Use wrappers for output params
        IntWrapper inFileWrapper = new IntWrapper(-1);
        IntWrapper outFileWrapper = new IntWrapper(-1);

        // Buffers and variables
        // STRING .filename[0:30]; -> byte[31]
        byte[] filename = new byte[31];
        // STRING .buffer[0:255]; -> byte[256]
        byte[] buffer = new byte[256];
        IntWrapper lengthWrapper = new IntWrapper(0); // For WRITEREAD/READ output
        IntWrapper errorWrapper = new IntWrapper(0); // For FILEINFO output

        int terminal = -1, inFile = -1, outFile = -1; // Local ints to hold file numbers

        try {
            // Get home terminal name and open it
            // CALL MYTERM(filename);
            GuardianInterface.myTerm(filename);
            // CALL OPEN(filename, terminal);
            if (GuardianInterface.open(filename, terminalWrapper) < 0) {
                logger.fatal("Failed to open terminal. Aborting.");
                return; // Cannot proceed without terminal
            }
            terminal = terminalWrapper.value;

            // Create a prompt
            // buffer ':=' "Enter filename: ";
            String prompt = "Enter filename: ";
            assignStringLiteral(buffer, prompt); // Assign literal to buffer
            // length := 16; ! Length of prompt
            int promptLength = prompt.length(); // Use actual length

            // Read filename from terminal
            // CALL WRITEREAD(terminal, buffer, length, 30, length);
            // Read up to 30 bytes into buffer *after* the prompt
            if (GuardianInterface.writeRead(terminal, buffer, promptLength, 30, lengthWrapper) < 0) {
                 logger.error("WRITEREAD failed on terminal.");
                 GuardianInterface.close(terminal);
                 return;
            }
            int filenameLength = lengthWrapper.value; // Actual bytes read for filename

            // buffer[length + 16] := 0; ! Null-terminate the input
            // The input starts at buffer[promptLength]
            int termPos = promptLength + filenameLength;
            if (termPos < buffer.length) {
                buffer[termPos] = 0;
            } else {
                 logger.warn("Cannot null-terminate filename input, buffer too small or input too long.");
            }

            // Copy the entered filename
            // filename ':=' buffer[16] FOR length;
            // Copy from buffer[promptLength] for filenameLength bytes
            assignBytes(filename, 0, buffer, promptLength, filenameLength);
            // filename[length] := 0; ! Null-terminate
             if (filenameLength < filename.length) {
                 filename[filenameLength] = 0;
             }
             String enteredFilename = new String(filename, 0, filenameLength, StandardCharsets.ISO_8859_1);
             logger.info("Filename entered: '{}'", enteredFilename);


            // Open input file
            // CALL OPEN(filename, in_file);
            if (GuardianInterface.open(filename, inFileWrapper) < 0) {
                // IF < THEN BEGIN ... END;
                GuardianInterface.fileInfo(inFileWrapper.value, errorWrapper); // Get error code
                int error = errorWrapper.value;
                logger.error("Error opening input file '{}'. Code: {}", enteredFilename, error);

                // buffer ':=' "Error opening input file: ";
                String errMsg = "Error opening input file: " + error; // Java way to format
                assignStringLiteral(buffer, errMsg);

                // ! Convert error to string and append - TAL code is flawed
                // INT err_pos := 26;
                // error := $INT(error);
                // buffer[err_pos].<0:15> := error + "0"; -> Incorrect TAL
                // Java equivalent: Write the error message string
                GuardianInterface.write(terminal, buffer, errMsg.length());

                GuardianInterface.close(terminal); // Close terminal before returning
                return; // Exit procedure simulation
            }
            inFile = inFileWrapper.value;

            // Prepare output filename
            // buffer ':=' "output "; -> Should be a valid filename
            String outFilenameStr = "output.dat"; // Use a more standard name
            assignStringLiteral(buffer, outFilenameStr);

            // Open output file (creation)
            // CALL OPEN(buffer, out_file, 1);
            if (GuardianInterface.open(buffer, outFileWrapper, 1) < 0) {
                 GuardianInterface.fileInfo(outFileWrapper.value, errorWrapper);
                 int error = errorWrapper.value;
                 logger.error("Error opening output file '{}'. Code: {}", outFilenameStr, error);
                 String errMsg = "Error opening output file: " + error;
                 assignStringLiteral(buffer, errMsg);
                 GuardianInterface.write(terminal, buffer, errMsg.length());
                 GuardianInterface.close(terminal);
                 GuardianInterface.close(inFile); // Close input file too
                 return;
            }
            outFile = outFileWrapper.value;

            // Read from input and write to output
            logger.info("Starting file copy from {} to {}", enteredFilename, outFilenameStr);
            // WHILE 1 DO BEGIN ... END;
            while (true) {
                // CALL READ(in_file, buffer, 80, length);
                int readStatus = GuardianInterface.read(inFile, buffer, 80, lengthWrapper);

                // IF < THEN RETURN; ! Error or EOF
                if (readStatus < 0) {
                    GuardianInterface.fileInfo(inFile, errorWrapper);
                    if (errorWrapper.value == 11) { // EOF
                        logger.info("EOF reached on input file.");
                    } else {
                        logger.error("Error reading input file. Code: {}", errorWrapper.value);
                        // Optionally report error to terminal
                         String errMsg = "Error reading input file: " + errorWrapper.value;
                         assignStringLiteral(buffer, errMsg);
                         GuardianInterface.write(terminal, buffer, errMsg.length());
                    }
                    break; // Exit WHILE loop on error or EOF
                }

                int bytesJustRead = lengthWrapper.value;
                // IF length > 0 THEN CALL WRITE(out_file, buffer, length);
                if (bytesJustRead > 0) {
                    if (GuardianInterface.write(outFile, buffer, bytesJustRead) < 0) {
                         GuardianInterface.fileInfo(outFile, errorWrapper);
                         logger.error("Error writing to output file. Code: {}", errorWrapper.value);
                         // Optionally report error to terminal
                         String errMsg = "Error writing output file: " + errorWrapper.value;
                         assignStringLiteral(buffer, errMsg);
                         GuardianInterface.write(terminal, buffer, errMsg.length());
                         break; // Exit WHILE loop on write error
                    }
                } else {
                     // Should not happen if READ returns 0 without error, but handle defensively
                     logger.warn("READ returned 0 bytes without EOF error, exiting loop.");
                     break;
                }
            } // END WHILE

            // Close files
            logger.debug("Closing files...");
            GuardianInterface.close(inFile);
            GuardianInterface.close(outFile);

            // Report success
            // buffer ':=' "File transfer complete.";
            String successMsg = "File transfer complete.";
            assignStringLiteral(buffer, successMsg);
            // CALL WRITE(terminal, buffer, 23);
            GuardianInterface.write(terminal, buffer, successMsg.length());

            GuardianInterface.close(terminal); // Close terminal at the end

        } catch (Exception e) {
            logger.fatal("Unhandled exception in file_handling simulation", e);
            // Ensure files opened are closed in case of unexpected error
            if (terminal != -1) GuardianInterface.close(terminal);
            if (inFile != -1) GuardianInterface.close(inFile);
            if (outFile != -1) GuardianInterface.close(outFile);
        } finally {
             consoleScanner.close(); // Close scanner when done
        }

        logger.info("file_handling simulation finished.");
    }
}

