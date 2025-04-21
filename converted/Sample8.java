package converted;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Wrapper class for simulating pass-by-reference for TAL INT parameters.
class IntWrapper {
    public int value;
    public IntWrapper() { this.value = 0; }
    public IntWrapper(int value) { this.value = value; }
    @Override public String toString() { return Integer.toString(value); }
}

// Wrapper class for simulating pass-by-reference for TAL INT(32) parameters.
class LongWrapper {
    public long value;
    public LongWrapper() { this.value = 0L; }
    public LongWrapper(long value) { this.value = value; }
    @Override public String toString() { return Long.toString(value); }
}

// --- Simulated Structures ---
// These are simplified Java representations. A ByteBuffer approach would be
// needed for exact memory layout simulation, especially if redefines or
// complex equivalences were used.

// Assumed structure for tpsout^file (needs at least fnum)
class TpsoutFileRecord {
    public int fnum = -1; // File number
    // Add other fields if they exist and are used
}

// Assumed structure for tps^10.c^rec (needs at least record^code)
class CRec {
    public String recordCode = ""; // Assuming STRING, adjust size/type if known
}

// Assumed structure for tps^10
class Tps10Record {
    public CRec cRec = new CRec();
    // Add other fields if they exist and are used

    // Simulate $LEN(tps^10) - Requires actual definition. Assume 100 bytes for simulation.
    public static final int BYTE_LENGTH = 100;
    private ByteBuffer buffer = ByteBuffer.allocate(BYTE_LENGTH); // Internal buffer if needed

    public ByteBuffer getBuffer() {
        buffer.clear(); // Prepare for writing into
        return buffer;
    }

    // Method to parse buffer after reading (example)
    public void parseBuffer() {
        // Example: Assuming recordCode is the first 2 bytes
        if (buffer.limit() >= 2) {
             byte[] codeBytes = new byte[2];
             buffer.position(0);
             buffer.get(codeBytes);
             this.cRec.recordCode = new String(codeBytes, StandardCharsets.ISO_8859_1);
        } else {
             this.cRec.recordCode = "";
        }
    }
}


/**
 * Simulates Guardian OS calls and environment.
 */
class GuardianInterface {
    private static final Logger logger = LogManager.getLogger(GuardianInterface.class);

    // Severity constants mirroring TAL LOG^ERR
    public static final int INFO_PROCESS = 0;
    public static final int WARNING_PROCESS = 1;
    public static final int STOP_PROCESS = 2;
    public static final int ABEND_PROCESS = 3;

    // --- Mock File System and Process State ---
    private static Map<Integer, MockFile> openFiles = new HashMap<>();
    private static int nextFileNum = 100;
    private static boolean transactionActive = false;

    // Mock file state class
    private static class MockFile {
        String fileName;
        ByteBuffer data;
        boolean isOpen = true;
        int lastError = 0;
        boolean eof = false;
        boolean locked = false;
        int currentPosition = 0; // For KEYPOSITION simulation

        MockFile(String name, ByteBuffer data) {
            this.fileName = name;
            this.data = data != null ? data : ByteBuffer.allocate(0); // Handle null data
            this.eof = !this.data.hasRemaining();
        }
    }

    // --- Stubs for Guardian Procedures ---

    /** Simulates PROCESS^PARAMETERS */
    public static int processParameters(byte[] paramMsg, byte[] configNameBytes, int configNameLen,
                                        byte[] valueBuffer, IntWrapper valLenWrapper, int type) {
        String configName = new String(configNameBytes, 0, configNameLen, StandardCharsets.ISO_8859_1);
        logger.debug("STUB: PROCESS^PARAMETERS called for '{}', type={}", configName, type);

        // Simulate finding parameters based on configName
        if ("LOC-REQUEST".equals(configName)) {
            String mockValue = "/path/to/request/file"; // Example value
            byte[] valueBytes = mockValue.getBytes(StandardCharsets.ISO_8859_1);
            int lenToCopy = Math.min(valueBytes.length, valueBuffer.length);
            System.arraycopy(valueBytes, 0, valueBuffer, 0, lenToCopy);
            valLenWrapper.value = lenToCopy;
            logger.debug("STUB: Found '{}', returning value '{}', length={}", configName, mockValue, lenToCopy);
            return 0; // Success
        } else if ("MAX-OUTPUT-INDEX".equals(configName)) {
            int mockIntValue = 5; // Example value
             if (type == 1) { // integer type requested
                 // Simulate returning integer in buffer (as TAL might)
                 ByteBuffer tempBuf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
                 tempBuf.putShort((short)mockIntValue);
                 System.arraycopy(tempBuf.array(), 0, valueBuffer, 0, 2); // Copy 2 bytes
                 valLenWrapper.value = 2; // Length of integer
                 logger.debug("STUB: Found '{}', returning integer value {} in buffer, length=2", configName, mockIntValue);
                 return 0; // Success
             } else {
                 logger.warn("STUB: PROCESS^PARAMETERS: Incorrect type requested for {}", configName);
                 valLenWrapper.value = 0;
                 return -1; // Error - type mismatch simulation
             }
        } else {
            logger.warn("STUB: PROCESS^PARAMETERS: Parameter '{}' not found", configName);
            valLenWrapper.value = 0;
            return -1; // Simulate parameter not found error
        }
    }

    /** Simulates LOG^ERR */
     public static void logErr(int severity, String message) {
         logErr(severity, message, 0); // Overload without specific code
     }
    public static void logErr(int severity, String message, int code) {
        String severityStr = switch (severity) {
            case INFO_PROCESS -> "INFO";
            case WARNING_PROCESS -> "WARN";
            case STOP_PROCESS -> "STOP";
            case ABEND_PROCESS -> "ABEND";
            default -> "UNKNOWN";
        };
        String logMessage = String.format("LOG^ERR (Severity: %s [%d], Code: %d): %s",
                                          severityStr, severity, code, message);
        switch (severity) {
            case INFO_PROCESS: logger.info(logMessage); break;
            case WARNING_PROCESS: logger.warn(logMessage); break;
            case STOP_PROCESS:
                logger.error(logMessage);
                logger.error("Simulating process STOP...");
                throw new SimulatedTerminationException("Process stopped", severity, code);
            case ABEND_PROCESS:
                logger.fatal(logMessage);
                logger.fatal("Simulating process ABEND...");
                throw new SimulatedTerminationException("Process abended", severity, code);
            default: logger.error(logMessage); break;
        }
    }

    /** Simulates BEGINTRANSACTION */
    public static int beginTransaction(LongWrapper transactionTagWrapper) {
        logger.debug("STUB: BEGINTRANSACTION called.");
        if (transactionActive) {
            logger.warn("STUB: BEGINTRANSACTION failed - transaction already active.");
            return -1; // Simulate error - already in transaction
        }
        transactionActive = true;
        transactionTagWrapper.value = System.currentTimeMillis(); // Assign a dummy tag
        logger.info("STUB: Transaction started with tag {}", transactionTagWrapper.value);
        return 0; // Success
    }

    /** Simulates ENDTRANSACTION */
    public static int endTransaction() {
        logger.debug("STUB: ENDTRANSACTION called.");
        if (!transactionActive) {
            logger.warn("STUB: ENDTRANSACTION failed - no active transaction.");
            return -1; // Simulate error - not in transaction
        }
        transactionActive = false;
        logger.info("STUB: Transaction ended.");
        return 0; // Success
    }

    /** Simulates KEYPOSITION */
    public static int keyposition(int fileNum, ByteBuffer keyBuffer, ByteBuffer dataBuffer) {
        // Simplified: Just reset the simulated position to 0
        logger.debug("STUB: KEYPOSITION called for fileNum {}. Resetting position to 0.", fileNum);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("STUB: KEYPOSITION error - file {} not open.", fileNum);
            if (file != null) file.lastError = 10;
            return -1;
        }
        file.currentPosition = 0;
        file.data.position(0); // Reset underlying buffer position too
        file.eof = !file.data.hasRemaining();
        file.lastError = 0;
        return 0; // Success
    }

    /** Simulates FILEINFO */
    public static void fileInfo(int fileNum, IntWrapper errorWrapper) {
        logger.debug("STUB: FILEINFO called for fileNum {}", fileNum);
        MockFile file = openFiles.get(fileNum);
        if (file != null && file.isOpen) {
            errorWrapper.value = file.lastError;
            logger.debug("STUB: FILEINFO returning last error {} for fileNum {}", file.lastError, fileNum);
            // Don't clear error here, let READ/WRITE clear on success
        } else {
            errorWrapper.value = 10; // File not open error
            logger.warn("STUB: FILEINFO returning error 10 (File Not Open) for fileNum {}", fileNum);
        }
    }

    /** Simulates READ */
    public static int read(int fileNum, ByteBuffer buffer, int readMax, IntWrapper bytesReadWrapper) {
        logger.debug("STUB: READ called for fileNum {}, readMax={}", fileNum, readMax);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("STUB: READ error - file {} not open.", fileNum);
            if (file != null) file.lastError = 10;
            bytesReadWrapper.value = 0;
            return -1;
        }
        if (file.eof) {
            logger.warn("STUB: READ called on EOF for fileNum {}.", fileNum);
            file.lastError = 11; // EOF
            bytesReadWrapper.value = 0;
            return -1; // Indicate EOF
        }

        // Simulate reading from file.data
        buffer.clear(); // Prepare buffer for writing
        int bytesToRead = Math.min(readMax, file.data.remaining());
        bytesToRead = Math.min(bytesToRead, buffer.capacity());

        if (bytesToRead > 0) {
            byte[] temp = new byte[bytesToRead];
            file.data.get(temp);
            buffer.put(temp);
            buffer.flip(); // Prepare for reading by caller
            bytesReadWrapper.value = bytesToRead;
            file.currentPosition += bytesToRead;
            if (!file.data.hasRemaining()) {
                file.eof = true;
                logger.debug("STUB: READ reached EOF on fileNum {}.", fileNum);
            }
            file.lastError = 0; // Success
            logger.debug("STUB: READ successful for fileNum {}, read {} bytes.", fileNum, bytesToRead);
            return 0;
        } else {
            // No bytes left, should have been caught by file.eof check above, but handle defensively
            file.eof = true;
            file.lastError = 11; // EOF
            bytesReadWrapper.value = 0;
            logger.warn("STUB: READ found no bytes to read, setting EOF for fileNum {}.", fileNum);
            return -1; // Indicate EOF
        }
    }

    /** Simulates LOCKREC */
    public static int lockrec(int fileNum) {
        logger.debug("STUB: LOCKREC called for fileNum {}", fileNum);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("STUB: LOCKREC error - file {} not open.", fileNum);
            if (file != null) file.lastError = 10;
            return -1;
        }
        if (file.locked) {
             logger.warn("STUB: LOCKREC warning - file {} already locked.", fileNum);
             file.lastError = 16; // Record locked? Use plausible error
             return -1;
        }
        file.locked = true;
        file.lastError = 0;
        logger.debug("STUB: LOCKREC successful for fileNum {}.", fileNum);
        return 0; // Success
    }

    /** Simulates WRITEUPDATEUNLOCK */
    public static int writeUpdateUnlock(int fileNum, ByteBuffer buffer, int writeCount) {
        logger.debug("STUB: WRITEUPDATEUNLOCK called for fileNum {}, writeCount={}", fileNum, writeCount);
        MockFile file = openFiles.get(fileNum);
        if (file == null || !file.isOpen) {
            logger.error("STUB: WRITEUPDATEUNLOCK error - file {} not open.", fileNum);
            if (file != null) file.lastError = 10;
            return -1;
        }
        if (!file.locked) {
             logger.error("STUB: WRITEUPDATEUNLOCK error - file {} not locked.", fileNum);
             file.lastError = 17; // Record not locked? Use plausible error
             return -1;
        }

        // Simulate the write/update part (here we just log, could modify file.data)
        if (writeCount == 0) {
             logger.debug("STUB: WRITEUPDATEUNLOCK simulating delete (writeCount=0) for fileNum {}.", fileNum);
             // In a real sim, remove the record at current position
        } else {
             logger.debug("STUB: WRITEUPDATEUNLOCK simulating update with {} bytes for fileNum {}.", writeCount, fileNum);
             // In a real sim, replace/update the record at current position
        }

        // Unlock
        file.locked = false;
        file.lastError = 0;
        logger.debug("STUB: WRITEUPDATEUNLOCK successful for fileNum {}.", fileNum);
        return 0; // Success
    }

    // Helper to add mock files for testing
    public static void addMockFile(int fnum, String name, String content) {
         ByteBuffer buffer = null;
         if (content != null) {
              buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.ISO_8859_1));
         }
         openFiles.put(fnum, new MockFile(name, buffer));
         logger.info("Added mock file: fnum={}, name='{}', content='{}...'", fnum, name, content == null ? "null" : content.substring(0, Math.min(10, content.length())));
    }

     // Custom exception for simulated termination
     public static class SimulatedTerminationException extends RuntimeException {
         public SimulatedTerminationException(String message, int severity, int code) {
             super(String.format("%s (Severity: %d, Code: %d)", message, severity, code));
         }
     }
}


/**
 * Translated Java code for the TAL program sample8.tal.txt (CONFIG^SETUP^P100).
 */
public class Sample8 {

    private static final Logger logger = LogManager.getLogger(ConfigSetup.class);

    // --- Global Variables (Simulated as Static Fields) ---
    // These would typically be initialized elsewhere or passed into the method.
    // Provide default values for simulation.
    static byte[] paramMsg = "PARAM1=VALUE1;PARAM2=VALUE2".getBytes(StandardCharsets.ISO_8859_1); // Example param message
    static StringBuffer forTacl = new StringBuffer(100);
    static int forTaclLength = 0;
    static int maxOutputIndexGi = 5; // Default value, might be overwritten by params
    static int[] outputFileItemCntGi = new int[maxOutputIndexGi]; // Initial size
    static int fileIndexGi = 0; // Used as loop counter

    // Simulate tpsout^file array - needs initialization with file numbers
    static TpsoutFileRecord[] tpsoutFile = new TpsoutFileRecord[maxOutputIndexGi];
    static {
        for (int i = 0; i < tpsoutFile.length; i++) {
            tpsoutFile[i] = new TpsoutFileRecord();
            // Assign mock file numbers - these files need to be added to GuardianInterface
            tpsoutFile[i].fnum = 200 + i;
            GuardianInterface.addMockFile(200 + i, "TPSOUT" + i + ".DAT",
                "01DATARECORD1\n00CONTROLREC\n02DATARECORD2\n99CONTROLREC\n03DATARECORD3");
        }
    }

    // Simulate tps^10 record structure
    static Tps10Record tps10 = new Tps10Record();

    // Assumed literal/constant for comparison
    static final int MAX_OUTPUT_INDEX_L = 10; // Example limit

    // Helper method to simulate TAL string assignment ':=' to a byte array
    private static void assignStringLiteral(byte[] dest, String literal) {
         byte[] literalBytes = literal.getBytes(StandardCharsets.ISO_8859_1);
         int lenToCopy = Math.min(literalBytes.length, dest.length);
         // Copy the literal
         System.arraycopy(literalBytes, 0, dest, 0, lenToCopy);
         // Pad remaining space with spaces
         if (lenToCopy < dest.length) {
             Arrays.fill(dest, lenToCopy, dest.length, (byte) ' ');
         }
         // Optional: Null terminate if required by subsequent calls
         // if (dest.length > 0) dest[Math.min(literalBytes.length, dest.length - 1)] = 0;
    }
     // Helper method to simulate TAL string assignment ':=' to a StringBuffer
    private static void assignString(StringBuffer sb, String value) {
        sb.setLength(0); // Clear
        sb.append(value);
    }
     // Helper method to simulate TAL string assignment with FOR
    private static void assignBytesFor(StringBuffer sb, byte[] src, int count) {
         sb.setLength(0);
         int len = Math.min(count, src.length);
         sb.append(new String(src, 0, len, StandardCharsets.ISO_8859_1));
    }
     // Helper method to simulate null termination for TAL string assignment
    private static void assignStringLiteralWithNull(byte[] dest, String literal) {
         byte[] literalBytes = literal.getBytes(StandardCharsets.ISO_8859_1);
         int lenToCopy = Math.min(literalBytes.length, dest.length -1); // Leave space for null
         // Copy the literal
         System.arraycopy(literalBytes, 0, dest, 0, lenToCopy);
         // Null terminate
         dest[lenToCopy] = 0;
         // Pad remaining space with spaces (optional, depends on usage)
         if (lenToCopy + 1 < dest.length) {
             Arrays.fill(dest, lenToCopy + 1, dest.length, (byte) ' ');
         }
    }


    /**
     * Simulates TAL PROC CONFIG^SETUP^P100;
     */
    public static void configSetupP100() {
        logger.info("Entering CONFIG^SETUP^P100 simulation...");

        // --- Local Variable Declarations ---
        int resultLi;
        IntWrapper configRetLenLiWrapper = new IntWrapper(); // Wrapper for output param
        // INT .buffer^li[0:19] -> Use ByteBuffer for equivalencing
        ByteBuffer bufferLiBuffer = ByteBuffer.allocate(20 * 2).order(ByteOrder.LITTLE_ENDIAN); // 20 INTs = 40 bytes
        // INT .byte^counter^li[0:1] -> Use ByteBuffer
        ByteBuffer byteCounterLiBuffer = ByteBuffer.allocate(2 * 2).order(ByteOrder.LITTLE_ENDIAN); // 2 INTs = 4 bytes
        IntWrapper bytesReadLiWrapper = new IntWrapper();
        boolean continueLi;
        int idxLi;

        // STRING .config^in^ls[0:19] -> byte[20]
        byte[] configInLs = new byte[20];
        // STRING .buffer^ls := @buffer^li '<<' 1 -> Use bufferLiBuffer directly
        // STRING .byte^counter^ls := @byte^counter^li '<<' 1 -> Use byteCounterLiBuffer directly

        // INT(32) transaction^tag^li32;
        LongWrapper transactionTagLi32Wrapper = new LongWrapper();

        int err;
        int len;
        // string .config^name[0:23] := 24 * [" "];
        byte[] configName = new byte[24];
        Arrays.fill(configName, (byte)' ');
        // string .ptr; -> Not needed directly in Java for length calc
        // string .value^buffer[0:40];
        byte[] valueBuffer = new byte[41];
        IntWrapper valLenWrapper = new IntWrapper(); // Wrapper for output param

        try {
            // --- Processing LOC-REQUEST ---
            logger.debug("Processing LOC-REQUEST parameter...");
            // config^name ':=' "LOC-REQUEST" -> @ptr;
            String locRequestStr = "LOC-REQUEST";
            assignStringLiteral(configName, locRequestStr);
            // len := @ptr '-' @config^name;
            len = locRequestStr.length(); // Java string length

            // err := process^parameters(param^msg, config^name, len, value^buffer, val_len, 0);
            err = GuardianInterface.processParameters(paramMsg, configName, len, valueBuffer, valLenWrapper, 0);

            // IF err OR val_len > 26 THEN log^err(...) ELSE begin ... end;
            if (err != 0 || valLenWrapper.value > 26) {
                GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS, "LOC-REQUEST is missing/invalid");
                // Exception thrown by logErr will terminate this block
            } else {
                // begin
                // for^tacl ':=' value^buffer for val_len & "." -> @ptr;
                assignBytesFor(forTacl, valueBuffer, valLenWrapper.value);
                forTacl.append(".");
                // for^tacl^length := @ptr '-' @for^tacl;
                forTaclLength = forTacl.length();
                logger.debug("Processed LOC-REQUEST: forTacl='{}', length={}", forTacl, forTaclLength);
                // end;
            }

            // --- Processing MAX-OUTPUT-INDEX ---
            logger.debug("Processing MAX-OUTPUT-INDEX parameter...");
            // config^in^ls ':=' "MAX-OUTPUT-INDEX" & [0];
            assignStringLiteralWithNull(configInLs, "MAX-OUTPUT-INDEX");

            // result^li := PROCESS^PARAMETERS ( param^msg, config^in^ls, , buffer^ls, config^ret^len^li, 1);
            // Pass bufferLiBuffer for output, configRetLenLiWrapper for length
            resultLi = GuardianInterface.processParameters(paramMsg, configInLs, -1, // length=-1 indicates null terminated source
                                                         bufferLiBuffer.array(), // Pass underlying byte array
                                                         configRetLenLiWrapper,
                                                         1); // integer type

            // IF result^li OR buffer^li > max^output^index^l THEN log^err(...)
            int bufferValue = 0;
            if (configRetLenLiWrapper.value >= 2) { // Check if enough bytes were returned for an INT
                 bufferValue = bufferLiBuffer.getShort(0); // Read INT from buffer
            } else if (resultLi == 0) { // If processParams succeeded but returned too few bytes
                 logger.error("PROCESS^PARAMETERS returned success but insufficient length ({}) for integer MAX-OUTPUT-INDEX", configRetLenLiWrapper.value);
                 resultLi = -1; // Treat as error
            }

            // Check against the assumed literal limit
            if (resultLi != 0 || bufferValue > MAX_OUTPUT_INDEX_L) {
                GuardianInterface.logErr(GuardianInterface.STOP_PROCESS,
                                         "Invalid MAX-OUTPUT-INDEX param value: " + bufferValue);
                // Exception thrown by logErr will terminate
            }

            // max^output^index^gi := buffer^li;
            maxOutputIndexGi = bufferValue;
            logger.info("Set maxOutputIndexGi to {}", maxOutputIndexGi);

            // Resize global arrays if necessary based on the new maxOutputIndexGi
            if (outputFileItemCntGi.length < maxOutputIndexGi) {
                 logger.warn("Resizing outputFileItemCntGi from {} to {}", outputFileItemCntGi.length, maxOutputIndexGi);
                 outputFileItemCntGi = Arrays.copyOf(outputFileItemCntGi, maxOutputIndexGi);
                 // Also resize tpsoutFile array
                 TpsoutFileRecord[] oldTpsout = tpsoutFile;
                 tpsoutFile = new TpsoutFileRecord[maxOutputIndexGi];
                 System.arraycopy(oldTpsout, 0, tpsoutFile, 0, oldTpsout.length);
                 for(int k=oldTpsout.length; k<maxOutputIndexGi; k++) {
                     tpsoutFile[k] = new TpsoutFileRecord(); // Initialize new elements
                     tpsoutFile[k].fnum = 200 + k; // Assign mock fnum
                     GuardianInterface.addMockFile(200 + k, "TPSOUT" + k + ".DAT", ""); // Add mock file
                 }
            }

            // FOR idx^li := 0 TO max^output^index^l - 1 DO ... -> Corrected: Use max^output^index^gi
            logger.debug("Initializing outputFileItemCntGi array (size={})...", maxOutputIndexGi);
            for (idxLi = 0; idxLi <= maxOutputIndexGi - 1; idxLi++) {
                outputFileItemCntGi[idxLi] = 0;
            }

            // --- Check/Clean Output Files ---
            logger.info("Checking/Cleaning output files...");
            // result^li := BEGINTRANSACTION(transaction^tag^li32);
            resultLi = GuardianInterface.beginTransaction(transactionTagLi32Wrapper);
            if (resultLi != 0) {
                 GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS, "BEGINTRANSACTION failed");
            }

            // FOR file^index^gi := 0 TO max^output^index^gi - 1 DO ... END;
            for (fileIndexGi = 0; fileIndexGi <= maxOutputIndexGi - 1; fileIndexGi++) {
                logger.debug("Processing file index {}", fileIndexGi);
                // BEGIN (FOR loop)
                // buffer^li := 0;
                bufferLiBuffer.putInt(0, 0); // Reset first int in buffer

                // CALL KEYPOSITION (tpsout^file.fnum, buffer^li, buffer^li);
                int keyposStatus = GuardianInterface.keyposition(
                    tpsoutFile[fileIndexGi].fnum,
                    bufferLiBuffer, // Pass buffer for key (using first int)
                    bufferLiBuffer  // Pass buffer for data (not really used here)
                );

                // IF <> THEN ... END;
                if (keyposStatus != 0) {
                    // BEGIN (IF <>)
                    GuardianInterface.fileInfo(tpsoutFile[fileIndexGi].fnum, errorWrapper);
                    GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS,
                                             "File error # on initial position to TPS OUT FILE", errorWrapper.value);
                    // END;
                }

                continueLi = true;
                // WHILE continue^li DO ... END;
                while (continueLi) {
                    // BEGIN (WHILE loop)
                    // CALL READ (tpsout^file.fnum, tps^10, $LEN(tps^10), bytes^read^li);
                    int readStatus = GuardianInterface.read(
                        tpsoutFile[fileIndexGi].fnum,
                        tps10.getBuffer(), // Pass the buffer from the tps10 object
                        Tps10Record.BYTE_LENGTH, // Use defined length
                        bytesReadLiWrapper
                    );

                    // IF <> THEN ... ELSE ... END; (Check read status)
                    if (readStatus != 0) { // read not successful
                        // BEGIN (IF <> for READ)
                        GuardianInterface.fileInfo(tpsoutFile[fileIndexGi].fnum, errorWrapper);
                        if (errorWrapper.value == 11) { // EOF is error code 11 in simulation
                            logger.debug("EOF reached for file index {}", fileIndexGi);
                            continueLi = false; // Exit WHILE loop
                        } else {
                            // BEGIN (ELSE for EOF check)
                            GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS,
                                                     "READ error # on file TPS OUT, initial read", errorWrapper.value);
                            // END;
                        }
                        // END (IF <> for READ)
                    } else { // successful read
                        // BEGIN (ELSE for READ)
                        tps10.parseBuffer(); // Parse the data read into the buffer
                        logger.trace("Read record with code: '{}'", tps10.cRec.recordCode);

                        // IF (tps^10.c^rec.record^code = "00") OR (...) THEN ... ELSE ... END;
                        if (Objects.equals(tps10.cRec.recordCode, "00") ||
                            Objects.equals(tps10.cRec.recordCode, "99")) {
                            // BEGIN (Control Record Found)
                            logger.debug("Found control record ('{}'), deleting...", tps10.cRec.recordCode);
                            // CALL LOCKREC (tpsout^file.fnum);
                            int lockStatus = GuardianInterface.lockrec(tpsoutFile[fileIndexGi].fnum);
                            // IF <> THEN ... END;
                            if (lockStatus != 0) {
                                // BEGIN (IF <> for LOCKREC)
                                GuardianInterface.fileInfo(tpsoutFile[fileIndexGi].fnum, errorWrapper);
                                GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS,
                                                         "File error # on lockrec of TPS OUT FILE", errorWrapper.value);
                                // END;
                            }
                            // CALL WRITEUPDATEUNLOCK (tpsout^file.fnum, tps^10, 0);
                            int writeStatus = GuardianInterface.writeUpdateUnlock(
                                tpsoutFile[fileIndexGi].fnum,
                                tps10.getBuffer(), // Pass buffer (though content ignored for delete)
                                0 // writeCount = 0 implies delete
                            );
                            // IF <> THEN ... END;
                            if (writeStatus != 0) {
                                // BEGIN (IF <> for WRITEUPDATEUNLOCK)
                                GuardianInterface.fileInfo(tpsoutFile[fileIndexGi].fnum, errorWrapper);
                                GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS,
                                                         "WRITE error # on file TPS OUT, init ", errorWrapper.value);
                                // END;
                            }
                            // END (Control Record Found)
                        } else { // successful read of non control record
                            // output^file^item^cnt^gi[file^index^gi] := ... + 1;
                            outputFileItemCntGi[fileIndexGi]++;
                            logger.trace("Incremented count for file index {}: {}", fileIndexGi, outputFileItemCntGi[fileIndexGi]);
                        }
                        // END (ELSE for READ)
                    }
                    // END (WHILE loop)
                }
                // END (FOR loop)
            }

            // result^li := ENDTRANSACTION;
            resultLi = GuardianInterface.endTransaction();
            if (resultLi != 0) {
                 // Log warning or error, but maybe don't abend? Depends on requirements.
                 logger.warn("ENDTRANSACTION failed with result {}", resultLi);
            }

        } catch (GuardianInterface.SimulatedTerminationException e) {
            logger.error("Process terminated by LOG^ERR: {}", e.getMessage());
            // Perform any necessary cleanup before exiting simulation if needed
        } catch (Exception e) {
            logger.fatal("Unhandled Java exception in CONFIG^SETUP^P100", e);
            // Optionally call logErr to simulate TAL abend
            try {
                 GuardianInterface.logErr(GuardianInterface.ABEND_PROCESS, "Unhandled Java Exception: " + e.getMessage());
            } catch (GuardianInterface.SimulatedTerminationException te) {
                 // Ignore termination exception here as we are already handling the outer exception
            }
        } finally {
             // Ensure transaction is ended if it was started and an error occurred before the explicit end
             if (GuardianInterface.transactionActive) {
                  logger.warn("Transaction was still active in finally block, attempting to end.");
                  GuardianInterface.endTransaction();
             }
        }

        logger.info("Exiting CONFIG^SETUP^P100 simulation.");
    } // END of PROC

    // Main method to run the simulation
    public static void main(String[] args) {
        configSetupP100();
        System.out.println("Simulation Complete.");
        System.out.println("Final Item Counts: " + Arrays.toString(outputFileItemCntGi));
    }
}

