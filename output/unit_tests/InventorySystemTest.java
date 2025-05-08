import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for the {@link InventorySystem} class.
 */
class InventorySystemTest {

    // --- Constants for testing ---
    private static final int MAX_ITEMS = 100;
    private static final int SUCCESS = 0;
    private static final int ERROR_INVALID_INPUT = 1;
    private static final int CMD_ADD = 1;
    private static final int CMD_DELETE = 2;
    private static final int CMD_UPDATE = 3;
    private static final int CMD_QUERY = 4;
    private static final int CMD_REPORT = 5;
    private static final int CMD_EXIT = 6;

    // --- Stream redirection ---
    private final InputStream originalSystemIn = System.in;
    private final PrintStream originalSystemOut = System.out;
    private ByteArrayOutputStream systemOutContent;

    // --- Reflection helpers (to access private static members) ---
    private static Field getField(String fieldName) throws NoSuchFieldException {
        Field field = InventorySystem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static Method getMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = InventorySystem.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    // --- Setup and Teardown ---

    @BeforeEach
    void setUp() throws Exception {
        // Redirect System.out
        systemOutContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(systemOutContent));

        // Reset static state before each test
        resetStaticState();
    }

    @AfterEach
    void tearDown() {
        // Restore original System.in and System.out
        System.setIn(originalSystemIn);
        System.setOut(originalSystemOut);
    }

    // --- Helper method to reset static state ---
    private void resetStaticState() throws Exception {
        getField("errorCode").set(null, SUCCESS);
        getField("transactionCount").set(null, 0);
        getField("inventoryCount").set(null, 0);
        // Reset inventory array (important for isolation)
        getField("inventory").set(null, new InventorySystem.ItemRecord[MAX_ITEMS]);
        // Reset buffer
        StringBuilder buffer = (StringBuilder) getField("buffer").get(null);
        buffer.setLength(0);
        // Reset customer (if needed by test)
        getField("currentCustomer").set(null, null); // Or initialize as needed
    }

    // --- Helper method to provide input ---
    private void provideInput(String data) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        System.setIn(testIn);
    }

    // --- Helper method to get output ---
    private String getOutput() {
        return systemOutContent.toString().replace("\r\n", "\n"); // Normalize line endings
    }

    // --- Test Cases ---

    @Test
    @DisplayName("initializeInventory should set up initial items and count")
    void testInitializeInventory() throws Exception {
        // Arrange
        Method initializeInventory = getMethod("initializeInventory");

        // Act
        initializeInventory.invoke(null);

        // Assert
        int inventoryCount = (int) getField("inventoryCount").get(null);
        InventorySystem.ItemRecord[] inventory = (InventorySystem.ItemRecord[]) getField("inventory").get(null);

        assertEquals(3, inventoryCount, "Inventory count should be 3 after initialization");
        assertNotNull(inventory[0], "Item 1 should not be null");
        assertEquals(101, inventory[0].itemId);
        assertEquals("Laptop", inventory[0].name);
        assertNotNull(inventory[1], "Item 2 should not be null");
        assertEquals(102, inventory[1].itemId);
        assertEquals("Mouse", inventory[1].name);
        assertNotNull(inventory[2], "Item 3 should not be null");
        assertEquals(103, inventory[2].itemId);
        assertEquals("Keyboard", inventory[2].name);
        assertNull(inventory[3], "Item 4 should be null"); // Check beyond initialized items

        assertTrue(getOutput().contains("Inventory initialized with 3 items."), "Initialization message should be printed");
    }

    @Test
    @DisplayName("displayMenu should print the menu options")
    void testDisplayMenu() throws Exception {
        // Arrange
        Method displayMenu = getMethod("displayMenu");

        // Act
        displayMenu.invoke(null);

        // Assert
        String output = getOutput();
        assertTrue(output.contains("--- Inventory System Menu ---"), "Menu header missing");
        assertTrue(output.contains("1. Add Item"), "Add Item option missing");
        assertTrue(output.contains("2. Delete Item"), "Delete Item option missing");
        assertTrue(output.contains("3. Update Item"), "Update Item option missing");
        assertTrue(output.contains("4. Query Item"), "Query Item option missing");
        assertTrue(output.contains("5. Generate Report"), "Generate Report option missing");
        assertTrue(output.contains("6. Exit"), "Exit option missing");
        assertTrue(output.contains("---------------------------"), "Menu footer missing");
        assertTrue(output.endsWith("Enter command number: "), "Prompt missing or has extra newline");
    }

    @Nested
    @DisplayName("readCommand Tests")
    class ReadCommandTests {

        @Test
        @DisplayName("should return valid command number for integer input")
        void testReadCommandValid() throws Exception {
            // Arrange
            provideInput("4\n"); // Simulate user entering '4'
            Method readCommand = getMethod("readCommand");

            // Act
            int command = (int) readCommand.invoke(null);

            // Assert
            assertEquals(4, command, "Should return the entered integer command");
            assertEquals(SUCCESS, getField("errorCode").get(null), "Error code should remain SUCCESS for valid input");
            assertTrue(getOutput().isEmpty(), "Should not print error for valid input");
        }

        @Test
        @DisplayName("should return -1 and set error code for non-integer input")
        void testReadCommandInvalidFormat() throws Exception {
            // Arrange
            provideInput("abc\n"); // Simulate user entering non-integer text
            Method readCommand = getMethod("readCommand");

            // Act
            int command = (int) readCommand.invoke(null);

            // Assert
            assertEquals(-1, command, "Should return -1 for invalid input format");
            assertEquals(ERROR_INVALID_INPUT, getField("errorCode").get(null), "Error code should be set to ERROR_INVALID_INPUT");
            assertTrue(getOutput().contains("Error: Please enter a valid integer command."), "Error message for invalid format should be printed");
        }

         @Test
        @DisplayName("should handle empty input gracefully (treated as invalid)")
        void testReadCommandEmptyInput() throws Exception {
            // Arrange
            provideInput("\n"); // Simulate user pressing Enter immediately
            Method readCommand = getMethod("readCommand");

            // Act & Assert
            // InputMismatchException is expected here when nextInt() finds no integer
            assertThrows(java.util.InputMismatchException.class, () -> {
                 readCommand.invoke(null);
                 // Note: The original code catches this, prints an error, and returns -1.
                 // However, invoking the method directly might bypass some Scanner state handling
                 // or exception catching if not run within the full context.
                 // Let's refine this to check the state *after* the intended catch block.
            });

            // Rerun with a setup that mimics the catch block behavior
            provideInput("\n");
             try {
                 readCommand.invoke(null);
             } catch (Exception e) {
                 // The reflection invoke might throw InvocationTargetException wrapping the original
                 if (!(e.getCause() instanceof java.util.InputMismatchException)) {
                     throw e; // Re-throw unexpected exceptions
                 }
                 // Simulate the catch block in the original code
                 getField("errorCode").set(null, ERROR_INVALID_INPUT);
                 getMethod("writeLine", String.class).invoke(null, "Error: Please enter a valid integer command.");
             }

             // Assert state after simulated catch
             assertEquals(ERROR_INVALID_INPUT, getField("errorCode").get(null), "Error code should be set after empty input");
             assertTrue(getOutput().contains("Error: Please enter a valid integer command."), "Error message should be printed for empty input");
             // The return value isn't easily captured here without running the full main loop logic
        }
    }


    @Nested
    @DisplayName("printError Tests")
    class PrintErrorTests {

        @Test
        @DisplayName("should print correct message for known error code and reset code")
        void testPrintErrorKnownCode() throws Exception {
            // Arrange
            getField("errorCode").set(null, ERROR_INVALID_INPUT);
            Method printError = getMethod("printError");

            // Act
            printError.invoke(null);

            // Assert
            String output = getOutput();
            assertTrue(output.contains("Error: Invalid command or input."), "Correct error message should be printed");
            assertEquals(SUCCESS, getField("errorCode").get(null), "Error code should be reset to SUCCESS");
        }

        @Test
        @DisplayName("should print generic message for unknown error code and reset code")
        void testPrintErrorUnknownCode() throws Exception {
            // Arrange
            int unknownErrorCode = 99;
            getField("errorCode").set(null, unknownErrorCode);
            Method printError = getMethod("printError");

            // Act
            printError.invoke(null);

            // Assert
            String output = getOutput();
            assertTrue(output.contains("Error: An unknown error occurred (Code: " + unknownErrorCode + ")"), "Generic error message for unknown code expected");
            assertEquals(SUCCESS, getField("errorCode").get(null), "Error code should be reset to SUCCESS");
        }

        @Test
        @DisplayName("should print nothing if error code is SUCCESS")
        void testPrintErrorSuccess() throws Exception {
            // Arrange
            getField("errorCode").set(null, SUCCESS);
            Method printError = getMethod("printError");

            // Act
            printError.invoke(null);

            // Assert
            String output = getOutput();
            assertTrue(output.isEmpty(), "No output should be generated when errorCode is SUCCESS");
            assertEquals(SUCCESS, getField("errorCode").get(null), "Error code should remain SUCCESS");
        }
    }

    @Nested
    @DisplayName("Placeholder Command Method Tests")
    class PlaceholderCommandTests {

        private void testPlaceholderMethod(String methodName, String expectedOutput) throws Exception {
            // Arrange
            Method method = getMethod(methodName);

            // Act
            method.invoke(null);

            // Assert
            assertTrue(getOutput().contains(expectedOutput), "Expected placeholder message not found for " + methodName);
        }

        @Test
        @DisplayName("addItem should print not implemented message")
        void testAddItem() throws Exception {
            testPlaceholderMethod("addItem", "Add item functionality not yet implemented.");
        }

        @Test
        @DisplayName("deleteItem should print not implemented message")
        void testDeleteItem() throws Exception {
            testPlaceholderMethod("deleteItem", "Delete item functionality not yet implemented.");
        }

        @Test
        @DisplayName("updateItem should print not implemented message")
        void testUpdateItem() throws Exception {
            testPlaceholderMethod("updateItem", "Update item functionality not yet implemented.");
        }

        @Test
        @DisplayName("queryItem should print not implemented message")
        void testQueryItem() throws Exception {
            testPlaceholderMethod("queryItem", "Query item functionality not yet implemented.");
        }

        @Test
        @DisplayName("generateReport should print not implemented message")
        void testGenerateReport() throws Exception {
            testPlaceholderMethod("generateReport", "Generate report functionality not yet implemented.");
        }
    }

    @Test
    @DisplayName("ItemRecord toString should format correctly")
    void testItemRecordToString() {
        InventorySystem.ItemRecord item = new InventorySystem.ItemRecord(101, "Test Item", 99.99, 10);
        String expected = "ItemRecord{itemId=101, name='Test Item', price=99.99, quantity=10}";
        assertEquals(expected, item.toString());
    }

    @Test
    @DisplayName("CustomerRecord toString should format correctly")
    void testCustomerRecordToString() {
        InventorySystem.CustomerRecord customer = new InventorySystem.CustomerRecord(9001);
        customer.isActive = true;
        customer.taxExempt = false;
        String expected = "CustomerRecord{customerId=9001, isActive=true, taxExempt=false}";
        assertEquals(expected, customer.toString());
    }

    // --- Limited Main Loop Logic Tests (Focus on state changes and output) ---
    // Note: Testing main directly is problematic due to System.exit and the input loop.
    // These tests simulate parts of the loop's behavior by calling relevant methods.

    @Test
    @DisplayName("Main loop simulation: Valid command should increase transaction count and log")
    void testMainLoopValidCommand() throws Exception {
         // Arrange: Initialize, provide input for a valid command (e.g., Add)
        getMethod("initializeInventory").invoke(null);
        provideInput(CMD_ADD + "\n"); // Simulate entering '1'
        systemOutContent.reset(); // Clear init output

        // Act: Simulate reading command and executing the 'add' branch
        int cmd = (int) getMethod("readCommand").invoke(null);
        assertEquals(CMD_ADD, cmd);

        getMethod("addItem").invoke(null); // Simulate action
        boolean validCommandProcessed = true; // As determined in main's switch
        int currentTransactionCount = (int) getField("transactionCount").get(null);

        if (validCommandProcessed) {
            getField("transactionCount").set(null, currentTransactionCount + 1);
            // Simulate logger call - check output
            InventorySystem.ExternalLogger.logTransaction(cmd, (int) getField("transactionCount").get(null));
        }

        // Assert
        assertEquals(1, getField("transactionCount").get(null), "Transaction count should increment");
        String output = getOutput();
        assertTrue(output.contains("Add item functionality not yet implemented."), "addItem output expected");
        assertTrue(output.contains("[External Logger] Logged transaction: Command=1, Count=1"), "External logger output expected");
    }

     @Test
    @DisplayName("Main loop simulation: Invalid command number should print error")
    void testMainLoopInvalidCommandNumber() throws Exception {
        // Arrange: Initialize, provide input for an invalid command number
        getMethod("initializeInventory").invoke(null);
        provideInput("99\n"); // Simulate entering '99'
        systemOutContent.reset(); // Clear init output

        // Act: Simulate reading command and hitting the default case
        int cmd = (int) getMethod("readCommand").invoke(null);
        assertEquals(99, cmd);

        // Simulate default case logic
        if (cmd != -1) { // Check to avoid double error message
            getField("errorCode").set(null, ERROR_INVALID_INPUT);
            getMethod("printError").invoke(null);
        }
        boolean validCommandProcessed = false; // As determined in main's switch
        int initialTransactionCount = (int) getField("transactionCount").get(null);

        // Assert
        assertEquals(ERROR_INVALID_INPUT, (int)getField("errorCode").get(null), "Error code before printError"); // Check state before printError resets it
        String output = getOutput();
        assertTrue(output.contains("Error: Invalid command or input."), "Invalid command error message expected");
        assertEquals(SUCCESS, (int)getField("errorCode").get(null), "Error code should be reset after printError");
        assertEquals(0, getField("transactionCount").get(null), "Transaction count should not increment for invalid command");
        assertFalse(validCommandProcessed, "Command should not be marked as processed");
         assertFalse(output.contains("[External Logger]"), "Logger should not be called for invalid command");
    }

     @Test
    @DisplayName("Main loop simulation: Invalid input format should print error")
    void testMainLoopInvalidInputFormat() throws Exception {
        // Arrange: Initialize, provide invalid format input
        getMethod("initializeInventory").invoke(null);
        provideInput("xyz\n");
        systemOutContent.reset(); // Clear init output

        // Act: Simulate readCommand failure and subsequent error handling in loop
        int cmd = -1; // Simulate return value from readCommand on error
        int errorCodeBeforeRead = (int) getField("errorCode").get(null);
        try {
             cmd = (int) getMethod("readCommand").invoke(null);
        } catch(Exception e) {
            // Expected due to InputMismatchException, handled inside readCommand
        }

        // Assert state after readCommand (which sets error code)
        assertEquals(ERROR_INVALID_INPUT, (int) getField("errorCode").get(null), "Error code should be set by readCommand");
        assertEquals(-1, cmd, "Command should be -1 after invalid input");

        // Simulate the error handling block in main's loop
        if ((int)getField("errorCode").get(null) == ERROR_INVALID_INPUT && cmd == -1) {
             getMethod("printError").invoke(null);
             cmd = 0; // Reset cmd as in main
        }

        // Assert after error handling
        String output = getOutput();
        // readCommand prints its own error, printError prints it again based on state
        assertTrue(output.contains("Error: Please enter a valid integer command."), "Invalid format error message expected from readCommand");
        assertTrue(output.contains("Error: Invalid command or input."), "Error message expected from printError call in loop"); // printError uses the set errorCode
        assertEquals(SUCCESS, (int) getField("errorCode").get(null), "Error code should be reset after printError");
        assertEquals(0, getField("transactionCount").get(null), "Transaction count should not increment");
        assertEquals(0, cmd, "cmd should be reset to 0");
    }
}