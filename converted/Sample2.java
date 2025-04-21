package converted;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; // For simulating read_command

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Wrapper classes (if needed for pass-by-reference, though not strictly required by this specific code)
class IntWrapper {
    public int value;
    public IntWrapper() { this.value = 0; }
    public IntWrapper(int value) { this.value = value; }
    @Override public String toString() { return Integer.toString(value); }
}

// --- Structure Definitions ---

/**
 * Represents the TAL STRUCT item_record.
 */
class ItemRecord {
    private static final int ITEM_ID_OFFSET = 0; // INT size 2
    private static final int ITEM_NAME_OFFSET = 2; // STRING[0:30] size 31
    private static final int ITEM_NAME_LENGTH = 31;
    private static final int ITEM_PRICE_OFFSET = 33; // Needs alignment? TAL often aligns non-STRING on word boundary. Assuming offset 34. Size 8 (FIXED(2))
    private static final int ITEM_PRICE_OFFSET_ALIGNED = 34;
    private static final int QUANTITY_ON_HAND_OFFSET = 42; // INT size 2
    private static final int REORDER_LEVEL_OFFSET = 44; // INT size 2
    private static final int SUPPLIER_ID_OFFSET = 46; // INT size 2
    private static final int LAST_UPDATED_OFFSET = 48; // STRING[0:10] size 11
    private static final int LAST_UPDATED_LENGTH = 11;
    // Total size needs careful calculation based on alignment.
    // 2 (id) + 31 (name) + 1 (padding?) + 8 (price) + 2 (qty) + 2 (reorder) + 2 (supplier) + 11 (date) + 1 (padding?) = 60? Let's assume 60 for now.
    public static final int BYTE_LENGTH = 60; // Adjust if alignment rules differ

    private ByteBuffer buffer;

    public ItemRecord() {
        this.buffer = ByteBuffer.allocate(BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        // Initialize buffer content if necessary
    }

    public ItemRecord(ByteBuffer buffer) {
        if (buffer == null || buffer.capacity() < BYTE_LENGTH) {
            throw new IllegalArgumentException("Provided buffer is null or too small for ItemRecord");
        }
        this.buffer = buffer.slice(buffer.position(), BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int getItemId() { return buffer.getShort(ITEM_ID_OFFSET); }
    public void setItemId(int value) { buffer.putShort(ITEM_ID_OFFSET, (short) value); }

    public String getItemName() {
        byte[] bytes = new byte[ITEM_NAME_LENGTH];
        buffer.position(ITEM_NAME_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setItemName(String value) {
        byte[] bytes = new byte[ITEM_NAME_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, ITEM_NAME_LENGTH));
        buffer.position(ITEM_NAME_OFFSET);
        buffer.put(bytes);
    }

    public BigDecimal getItemPrice() {
        long fixedValue = buffer.getLong(ITEM_PRICE_OFFSET_ALIGNED);
        return BigDecimal.valueOf(fixedValue, 2); // FIXED(2) -> scale 2
    }
    public void setItemPrice(BigDecimal value) {
        long fixedValue = value.setScale(2, RoundingMode.HALF_UP).unscaledValue().longValue();
        buffer.putLong(ITEM_PRICE_OFFSET_ALIGNED, fixedValue);
    }

    public int getQuantityOnHand() { return buffer.getShort(QUANTITY_ON_HAND_OFFSET); }
    public void setQuantityOnHand(int value) { buffer.putShort(QUANTITY_ON_HAND_OFFSET, (short) value); }

    public int getReorderLevel() { return buffer.getShort(REORDER_LEVEL_OFFSET); }
    public void setReorderLevel(int value) { buffer.putShort(REORDER_LEVEL_OFFSET, (short) value); }

    public int getSupplierId() { return buffer.getShort(SUPPLIER_ID_OFFSET); }
    public void setSupplierId(int value) { buffer.putShort(SUPPLIER_ID_OFFSET, (short) value); }

    public String getLastUpdated() {
        byte[] bytes = new byte[LAST_UPDATED_LENGTH];
        buffer.position(LAST_UPDATED_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setLastUpdated(String value) {
        byte[] bytes = new byte[LAST_UPDATED_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, LAST_UPDATED_LENGTH));
        buffer.position(LAST_UPDATED_OFFSET);
        buffer.put(bytes);
    }

    public ByteBuffer getBuffer() {
        buffer.position(0);
        return buffer;
    }

     public static ItemRecord fromBytes(byte[] data) {
        if (data == null || data.length < BYTE_LENGTH) {
             // Handle error: return null or throw exception or return default
             return new ItemRecord(); // Return default for simplicity
        }
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        return new ItemRecord(wrappedBuffer);
    }

    @Override
    public String toString() {
        return "ItemRecord{" +
               "itemId=" + getItemId() +
               ", itemName='" + getItemName() + '\'' +
               ", itemPrice=" + getItemPrice() +
               ", quantityOnHand=" + getQuantityOnHand() +
               ", reorderLevel=" + getReorderLevel() +
               ", supplierId=" + getSupplierId() +
               ", lastUpdated='" + getLastUpdated() + '\'' +
               '}';
    }
}

/**
 * Represents the TAL STRUCT customer_record.
 * Includes handling for bit fields.
 */
class CustomerRecord {
    private static final int CUSTOMER_ID_OFFSET = 0; // INT size 2
    private static final int CUSTOMER_NAME_OFFSET = 2; // STRING[0:50] size 51
    private static final int CUSTOMER_NAME_LENGTH = 51;
    private static final int ADDRESS_LINE1_OFFSET = 53; // STRING[0:30] size 31
    private static final int ADDRESS_LINE1_LENGTH = 31;
    private static final int ADDRESS_LINE2_OFFSET = 84; // STRING[0:30] size 31
    private static final int ADDRESS_LINE2_LENGTH = 31;
    private static final int CITY_OFFSET = 115; // STRING[0:20] size 21
    private static final int CITY_LENGTH = 21;
    private static final int STATE_OFFSET = 136; // STRING[0:2] size 3
    private static final int STATE_LENGTH = 3;
    private static final int ZIP_CODE_OFFSET = 139; // STRING[0:9] size 10
    private static final int ZIP_CODE_LENGTH = 10;
    private static final int ACCOUNT_BALANCE_OFFSET = 149; // Needs alignment? Assume 150. Size 8 (FIXED(2))
    private static final int ACCOUNT_BALANCE_OFFSET_ALIGNED = 150;
    private static final int FILLER_OFFSET = 158; // FILLER 2 size 2
    private static final int BIT_FLAGS_OFFSET = 160; // Start of the word containing bit flags. Size 2 (16 bits)

    // Bit masks within the BIT_FLAGS_OFFSET word (assuming standard TAL bit numbering 0-15 L->R)
    private static final int IS_ACTIVE_MASK = 1 << (15 - 0); // Bit 0
    private static final int HAS_CREDIT_MASK = 1 << (15 - 1); // Bit 1
    private static final int TAX_EXEMPT_MASK = 1 << (15 - 2); // Bit 2
    // Reserved bits 3-15

    public static final int BYTE_LENGTH = 162; // Calculated size

    private ByteBuffer buffer;

    public CustomerRecord() {
        this.buffer = ByteBuffer.allocate(BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        // Initialize buffer content if necessary
    }

     public CustomerRecord(ByteBuffer buffer) {
        if (buffer == null || buffer.capacity() < BYTE_LENGTH) {
            throw new IllegalArgumentException("Provided buffer is null or too small for CustomerRecord");
        }
        this.buffer = buffer.slice(buffer.position(), BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    // --- Getters and Setters for standard fields ---
    public int getCustomerId() { return buffer.getShort(CUSTOMER_ID_OFFSET); }
    public void setCustomerId(int value) { buffer.putShort(CUSTOMER_ID_OFFSET, (short) value); }

    public String getCustomerName() {
        byte[] bytes = new byte[CUSTOMER_NAME_LENGTH];
        buffer.position(CUSTOMER_NAME_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setCustomerName(String value) {
        byte[] bytes = new byte[CUSTOMER_NAME_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, CUSTOMER_NAME_LENGTH));
        buffer.position(CUSTOMER_NAME_OFFSET);
        buffer.put(bytes);
    }

    // ... (Add getters/setters for address_line1, address_line2, city, state, zip_code similarly) ...
     public String getAddressLine1() {
        byte[] bytes = new byte[ADDRESS_LINE1_LENGTH];
        buffer.position(ADDRESS_LINE1_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setAddressLine1(String value) {
        byte[] bytes = new byte[ADDRESS_LINE1_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, ADDRESS_LINE1_LENGTH));
        buffer.position(ADDRESS_LINE1_OFFSET);
        buffer.put(bytes);
    }
     public String getAddressLine2() {
        byte[] bytes = new byte[ADDRESS_LINE2_LENGTH];
        buffer.position(ADDRESS_LINE2_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setAddressLine2(String value) {
        byte[] bytes = new byte[ADDRESS_LINE2_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, ADDRESS_LINE2_LENGTH));
        buffer.position(ADDRESS_LINE2_OFFSET);
        buffer.put(bytes);
    }
     public String getCity() {
        byte[] bytes = new byte[CITY_LENGTH];
        buffer.position(CITY_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setCity(String value) {
        byte[] bytes = new byte[CITY_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, CITY_LENGTH));
        buffer.position(CITY_OFFSET);
        buffer.put(bytes);
    }
     public String getState() {
        byte[] bytes = new byte[STATE_LENGTH];
        buffer.position(STATE_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setState(String value) {
        byte[] bytes = new byte[STATE_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, STATE_LENGTH));
        buffer.position(STATE_OFFSET);
        buffer.put(bytes);
    }
     public String getZipCode() {
        byte[] bytes = new byte[ZIP_CODE_LENGTH];
        buffer.position(ZIP_CODE_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setZipCode(String value) {
        byte[] bytes = new byte[ZIP_CODE_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, ZIP_CODE_LENGTH));
        buffer.position(ZIP_CODE_OFFSET);
        buffer.put(bytes);
    }


    public BigDecimal getAccountBalance() {
        long fixedValue = buffer.getLong(ACCOUNT_BALANCE_OFFSET_ALIGNED);
        return BigDecimal.valueOf(fixedValue, 2); // FIXED(2) -> scale 2
    }
    public void setAccountBalance(BigDecimal value) {
        long fixedValue = value.setScale(2, RoundingMode.HALF_UP).unscaledValue().longValue();
        buffer.putLong(ACCOUNT_BALANCE_OFFSET_ALIGNED, fixedValue);
    }

    // --- Getters and Setters for bit fields ---
    private short getFlagsWord() {
        return buffer.getShort(BIT_FLAGS_OFFSET);
    }
    private void setFlagsWord(short flags) {
        buffer.putShort(BIT_FLAGS_OFFSET, flags);
    }

    public boolean isActive() {
        return (getFlagsWord() & IS_ACTIVE_MASK) != 0;
    }
    public void setActive(boolean active) {
        short flags = getFlagsWord();
        if (active) {
            flags |= IS_ACTIVE_MASK;
        } else {
            flags &= ~IS_ACTIVE_MASK;
        }
        setFlagsWord(flags);
    }

    public boolean hasCredit() {
        return (getFlagsWord() & HAS_CREDIT_MASK) != 0;
    }
    public void setHasCredit(boolean hasCredit) {
        short flags = getFlagsWord();
        if (hasCredit) {
            flags |= HAS_CREDIT_MASK;
        } else {
            flags &= ~HAS_CREDIT_MASK;
        }
        setFlagsWord(flags);
    }

    public boolean isTaxExempt() {
        return (getFlagsWord() & TAX_EXEMPT_MASK) != 0;
    }
    public void setTaxExempt(boolean taxExempt) {
        short flags = getFlagsWord();
        if (taxExempt) {
            flags |= TAX_EXEMPT_MASK;
        } else {
            flags &= ~TAX_EXEMPT_MASK;
        }
        setFlagsWord(flags);
    }

    // Getter/Setter for reserved bits (optional, might just be ignored)
    public int getReservedBits() {
        // Mask out the used bits and shift right
        return (getFlagsWord() & 0x1FFF); // Mask for bits 3-15
    }
    public void setReservedBits(int value) {
        short flags = getFlagsWord();
        // Clear existing reserved bits and set new ones
        flags = (short) ((flags & ~0x1FFF) | (value & 0x1FFF));
        setFlagsWord(flags);
    }

    public ByteBuffer getBuffer() {
        buffer.position(0);
        return buffer;
    }

     public static CustomerRecord fromBytes(byte[] data) {
        if (data == null || data.length < BYTE_LENGTH) {
             // Handle error
             return new CustomerRecord(); // Return default
        }
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        return new CustomerRecord(wrappedBuffer);
    }

    @Override
    public String toString() {
        return "CustomerRecord{" +
               "customerId=" + getCustomerId() +
               ", customerName='" + getCustomerName() + '\'' +
               // ... other fields ...
               ", accountBalance=" + getAccountBalance() +
               ", isActive=" + isActive() +
               ", hasCredit=" + hasCredit() +
               ", taxExempt=" + isTaxExempt() +
               '}';
    }
}

/**
 * Simulates Guardian OS calls and environment.
 */
class GuardianInterface {
    private static final Logger logger = LogManager.getLogger(GuardianInterface.class);
    private static final Scanner consoleScanner = new Scanner(System.in); // For simulating input

    // --- Stubs for External Procedures ---
    public static void logTransaction(int commandCode, int transactionId) {
        logger.info("STUB: log_transaction called with Command: {}, Transaction ID: {}", commandCode, transactionId);
        // In a real simulation, this might write to a log file or database.
    }

    public static void fetchCustomerData(int customerId, CustomerRecord customerRec) {
        logger.info("STUB: fetch_customer_data called for Customer ID: {}", customerId);
        // Simulate fetching data and populating the record
        customerRec.setCustomerId(customerId);
        customerRec.setCustomerName("Simulated Customer " + customerId);
        customerRec.setAddressLine1("123 Simulation St");
        customerRec.setCity("Virtual City");
        customerRec.setState("ST");
        customerRec.setZipCode("00000");
        customerRec.setAccountBalance(new BigDecimal("123.45"));
        customerRec.setActive(true);
        customerRec.setHasCredit(true);
        customerRec.setTaxExempt(false);
        logger.debug("STUB: Populated customer record: {}", customerRec);
    }

    // --- Stubs for I/O ---
    public static void writeLine(byte[] buffer, int length) {
        // Simulate writing a line to the terminal/output device
        String line = new String(buffer, 0, Math.min(length, buffer.length), StandardCharsets.ISO_8859_1).trim();
        System.out.println(line);
        logger.debug("STUB: write_line: '{}'", line);
    }

    public static void readCommand(IntWrapper cmdWrapper) {
        // Simulate reading a command from the user
        System.out.print("Simulated Input> "); // Prompt
        try {
            String input = consoleScanner.nextLine();
            cmdWrapper.value = Integer.parseInt(input.trim());
            logger.debug("STUB: read_command received: {}", cmdWrapper.value);
        } catch (NumberFormatException e) {
            logger.warn("STUB: read_command received invalid input, returning 0");
            cmdWrapper.value = 0; // Return 0 or an error indicator on bad input
        } catch (Exception e) {
             logger.error("STUB: Error reading command", e);
             cmdWrapper.value = 0; // Default or error value
        }
    }

     // Simulate LOG^ERR (from previous example, slightly adapted)
    public static final int INFO_PROCESS = 0;
    public static final int WARNING_PROCESS = 1;
    public static final int STOP_PROCESS = 2;
    public static final int ABEND_PROCESS = 3;

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
                // In a real app, might throw a specific exception or call System.exit
                break;
            case ABEND_PROCESS:
                logger.fatal(logMessage);
                logger.fatal("Simulating process ABEND...");
                // In a real app, might throw a specific exception or call System.exit
                break;
            default: logger.error(logMessage); break;
        }
         // For simulation, maybe throw exception for STOP/ABEND if needed to halt flow
         // if (severity == STOP_PROCESS || severity == ABEND_PROCESS) {
         //     throw new RuntimeException("Simulated Termination: " + logMessage);
         // }
    }
}

/**
 * Translated Java code for the TAL program sample2.tal.txt (inventory_system).
 */
public class Sample2 {

    private static final Logger logger = LogManager.getLogger(InventorySystem.class);

    // --- Global Literals ---
    private static final int MAX_ITEMS = 1000;
    private static final int MAX_CUSTOMERS = 500; // Declared but not used in provided snippet
    private static final int BUFFER_SIZE = 4096;
    private static final int SUCCESS = 0;
    private static final int ERROR_FILE_NOT_FOUND = 1;
    private static final int ERROR_INVALID_INPUT = 2;
    private static final int ERROR_SYSTEM = 3;

    // --- Global Variables ---
    private static int errorCode = 0; // error_code := 0;
    // Read-only array simulation for error_messages
    private static final String[] ERROR_MESSAGES = {
        "Success",                  // Index 0
        "File not found",           // Index 1
        "Invalid input",            // Index 2
        "System error"              // Index 3
    };
    private static int transactionCount = 0; // transaction_count := 0;

    // Global inventory array (using ItemRecord objects)
    // We need a way to manage the array of structures. Using ByteBuffer is complex for arrays.
    // A Java List or Array of ItemRecord objects is more idiomatic for simulation.
    private static ItemRecord[] inventory = new ItemRecord[MAX_ITEMS];
    private static int inventoryCount = 0; // inventory_count := 0;

    // --- Pointer Declarations (Simulated) ---
    // INT .EXT error_ptr; -> Simulated by direct access to static errorCode
    // STRUCT customer_record .current_customer; -> Simulated by a static reference
    private static CustomerRecord currentCustomer = new CustomerRecord(); // Allocate one instance for simulation
    // STRING .buffer; -> Simulated by a byte array
    private static byte[] bufferBytes = new byte[BUFFER_SIZE];
    // Note: The TAL code `buffer := buffer_size * [" "]` initializes the buffer.

    // --- Read-Only Arrays ('P') ---
    private static final int[] COMMAND_CODES = {1, 2, 3, 4, 5, 6};
    private static final String[] COMMAND_NAMES = {"ADD", "DELETE", "UPDATE", "QUERY", "REPORT", "EXIT"};

    // --- Equivalenced Variables (Simulated) ---
    // INT last_error = error_code; -> Use errorCode directly
    // INT buffer_length = 'G'[10]; -> Simulate with a static variable, value needs context. Using BUFFER_SIZE as placeholder.
    private static int bufferLength = BUFFER_SIZE; // Placeholder value

    // --- Global Block Declarations (Simulated) ---
    static class InventoryIoBlock {
        static String filename = ""; // Initialize appropriately if needed
        static int fileError = 0;
        static int recordCount = 0;
    }

    // --- System Global Pointer Declarations (Simulated) ---
    // INT .SG system_time; -> Simulate with a placeholder
    private static int systemTime = 0; // Placeholder, actual access is OS-dependent

    // --- Forward/External Procedure Stubs ---
    // FORWARD PROC print_error; -> Implemented below
    // EXTERNAL PROC log_transaction; -> Stubbed in GuardianInterface
    // EXTERNAL PROC fetch_customer_data; -> Stubbed in GuardianInterface

    // --- Main Procedure ---
    public static void main(String[] args) {
        // Local variables from main_proc
        // int i; // Declared but not used in snippet
        // int status; // Declared but not used in snippet
        // String input_buffer[0:100]; // Not used in snippet
        // String temp_str[0:50]; // Not used in snippet
        // int j; // Declared but not used in snippet
        int cmd; // Local variable for command

        logger.info("Starting Inventory System Simulation...");

        // Initialize buffer
        // buffer := buffer_size * [" "];
        Arrays.fill(bufferBytes, (byte) ' ');
        logger.debug("Buffer initialized with spaces.");

        // Initialize inventory with sample data
        // CALL initialize_inventory;
        initializeInventory();

        // Main processing loop
        cmd = 0;
        // WHILE cmd <> 6 DO
        while (cmd != 6) {
            // BEGIN (inside WHILE)

            // Display menu and get command
            // CALL display_menu;
            displayMenu();
            // CALL read_command(cmd); -> Needs wrapper for pass-by-reference
            IntWrapper cmdWrapper = new IntWrapper(cmd);
            GuardianInterface.readCommand(cmdWrapper);
            cmd = cmdWrapper.value;

            // Process command
            // CASE cmd OF
            switch (cmd) {
                // 1: CALL add_item;
                case 1: addItem(); break;
                // 2: CALL delete_item;
                case 2: deleteItem(); break;
                // 3: CALL update_item;
                case 3: updateItem(); break;
                // 4: CALL query_item;
                case 4: queryItem(); break;
                // 5: CALL generate_report;
                case 5: generateReport(); break;
                // 6: ; ! Exit
                case 6:
                    logger.info("Exit command received.");
                    break;
                // OTHERWISE
                default:
                    // error_code := error_invalid_input;
                    errorCode = ERROR_INVALID_INPUT;
                    // CALL print_error;
                    printError();
                    break;
            } // END CASE

            // Log transaction if not exiting
            // IF cmd <> 6 THEN
            if (cmd != 6) {
                // transaction_count := transaction_count + 1;
                transactionCount++;
                // CALL log_transaction(cmd, transaction_count);
                GuardianInterface.logTransaction(cmd, transactionCount);
            } // ENDIF
            // END (inside WHILE)
        } // ENDWHILE

        // Clean up
        // error_ptr := @error_code; -> Simulated by direct access
        // IF error_ptr <> 0 THEN -> IF errorCode <> 0 THEN
        if (errorCode != 0) {
            // CALL print_error;
            printError();
        } // ENDIF

        // Example of bit manipulation
        // IF inventory_count > 0 THEN
        if (inventoryCount > 0) {
            // current_customer := @customer_record; -> This TAL is problematic.
            // Assuming it means point to *some* customer record instance.
            // For simulation, let's fetch or use the static one.
            // Let's simulate fetching customer 1 if available, otherwise use the static placeholder.
            // This requires a mechanism to store/retrieve customers, which isn't defined.
            // Using the static placeholder `currentCustomer` initialized earlier.
            // If we wanted to simulate fetching:
            // GuardianInterface.fetchCustomerData(1, currentCustomer); // Fetch data for customer 1

            logger.debug("Simulating bit manipulation on currentCustomer instance.");
            // current_customer.is_active := 1;
            currentCustomer.setActive(true);
            // current_customer.tax_exempt := 1;
            currentCustomer.setTaxExempt(true);
            // current_customer.reserved := 0;
            currentCustomer.setReservedBits(0);

            logger.info("Customer Record after bit manipulation: {}", currentCustomer);

        } // ENDIF

        logger.info("Inventory System Simulation finished. Final error code: {}", errorCode);
        // RETURN success;
        // In Java main, the program simply exits. We can simulate the return code if needed.
        System.exit(SUCCESS); // Exit with success code
    }

    // --- Subprocedure Implementations (or Stubs) ---

    /**
     * Simulates PROC initialize_inventory;
     */
    private static void initializeInventory() {
        logger.info("Initializing inventory...");
        // Set initial inventory items
        inventoryCount = 3;

        // Item 1
        inventory[0] = new ItemRecord(); // Allocate record
        inventory[0].setItemId(1001);
        inventory[0].setItemName("Widget A");
        inventory[0].setItemPrice(new BigDecimal("19.99"));
        inventory[0].setQuantityOnHand(150);
        inventory[0].setReorderLevel(25);
        inventory[0].setSupplierId(101);
        inventory[0].setLastUpdated("2025-04-01");

        // Item 2
        inventory[1] = new ItemRecord();
        inventory[1].setItemId(1002);
        inventory[1].setItemName("Widget B");
        inventory[1].setItemPrice(new BigDecimal("29.99"));
        inventory[1].setQuantityOnHand(75);
        inventory[1].setReorderLevel(15);
        inventory[1].setSupplierId(102);
        inventory[1].setLastUpdated("2025-04-01");

        // Item 3
        inventory[2] = new ItemRecord();
        inventory[2].setItemId(1003);
        inventory[2].setItemName("Widget C");
        inventory[2].setItemPrice(new BigDecimal("39.99"));
        inventory[2].setQuantityOnHand(50);
        inventory[2].setReorderLevel(10);
        inventory[2].setSupplierId(101);
        inventory[2].setLastUpdated("2025-04-01");

        logger.debug("Inventory initialized with {} items.", inventoryCount);
    }

    /**
     * Simulates PROC display_menu;
     * Note: The TAL code provided is incomplete.
     */
    private static void displayMenu() {
        logger.debug("Displaying menu...");
        // buffer := "==== Inventory System Menu ====";
        String menuHeader = "==== Inventory System Menu ====";
        byte[] headerBytes = menuHeader.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(headerBytes, 0, bufferBytes, 0, headerBytes.length);
        // CALL write_line(buffer, 30); -> Use length of string
        GuardianInterface.writeLine(bufferBytes, menuHeader.length());

        // Display command options
        // FOR i := 0 TO 5 DO
        for (int i = 0; i <= 5; i++) {
            // buffer := "  "; -> Clear buffer or prepare section
            Arrays.fill(bufferBytes, 0, 15, (byte)' '); // Clear first 15 bytes

            // buffer[2] := command_codes[i]; -> TAL assigns INT to STRING byte - gets low byte ASCII? Or numeric char? Assuming numeric char.
            String codeStr = Integer.toString(COMMAND_CODES[i]);
            bufferBytes[2] = (byte) codeStr.charAt(0); // Put ASCII digit

            // buffer[3] := ". ";
            bufferBytes[3] = (byte) '.';
            bufferBytes[4] = (byte) ' ';

            // buffer[5] := command_names[i]; -> Copy string
            byte[] nameBytes = COMMAND_NAMES[i].getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(nameBytes, 0, bufferBytes, 5, Math.min(nameBytes.length, 10)); // Copy up to 10 chars

            // CALL write_line(buffer, 15);
            GuardianInterface.writeLine(bufferBytes, 15);
        } // ENDFOR

        // buffer := "Enter command: ";
        String prompt = "Enter command: ";
        byte[] promptBytes = prompt.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(promptBytes, 0, bufferBytes, 0, promptBytes.length);
        // CALL write -> TAL code is incomplete here, assuming write_line
        GuardianInterface.writeLine(bufferBytes, prompt.length());
        // The original TAL code ends abruptly here within display_menu
    }

    /**
     * Simulates FORWARD PROC print_error;
     */
    private static void printError() {
        String message = "Unknown Error";
        if (errorCode >= 0 && errorCode < ERROR_MESSAGES.length) {
            message = ERROR_MESSAGES[errorCode];
        }
        logger.error("Error occurred: Code={}, Message='{}'", errorCode, message);
        // Simulate printing to a specific error output if needed
        System.err.println("ERROR [" + errorCode + "]: " + message);
    }

    // --- Stubs for other called procedures ---
    private static void addItem() {
        logger.info("STUB: add_item called.");
        // Add simulation logic here
    }

    private static void deleteItem() {
        logger.info("STUB: delete_item called.");
        // Add simulation logic here
    }

    private static void updateItem() {
        logger.info("STUB: update_item called.");
        // Add simulation logic here
    }

    private static void queryItem() {
        logger.info("STUB: query_item called.");
        // Add simulation logic here
    }

    private static void generateReport() {
        logger.info("STUB: generate_report called.");
        // Add simulation logic here
    }
}
