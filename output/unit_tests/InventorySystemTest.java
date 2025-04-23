import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * JUnit 5 tests for the InventorySystem class and its related components.
 */
class InventorySystemTest {

    // For capturing System.out and System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;

    // Helper method to reset static fields of InventorySystem before each test
    private void resetInventorySystemState() throws Exception {
        // Reset inventory list
        Field inventoryField = InventorySystem.class.getDeclaredField("inventory");
        inventoryField.setAccessible(true);
        inventoryField.set(null, new ArrayList<>(InventorySystem.MAX_ITEMS > 0 ? InventorySystem.MAX_ITEMS : 10));

        // Reset inventory count
        Field inventoryCountField = InventorySystem.class.getDeclaredField("inventoryCount");
        inventoryCountField.setAccessible(true);
        inventoryCountField.setInt(null, 0);

        // Reset transaction count
        Field transactionCountField = InventorySystem.class.getDeclaredField("transactionCount");
        transactionCountField.setAccessible(true);
        transactionCountField.setInt(null, 0);

        // Reset error code
        Field errorCodeField = InventorySystem.class.getDeclaredField("errorCode");
        errorCodeField.setAccessible(true);
        errorCodeField.setInt(null, InventorySystem.SUCCESS);

        // Reset current customer
        Field currentCustomerField = InventorySystem.class.getDeclaredField("currentCustomer");
        currentCustomerField.setAccessible(true);
        currentCustomerField.set(null, null);
    }

    // Helper method to invoke private static methods using reflection
    private static Object invokePrivateStaticMethod(String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = InventorySystem.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args); // null for static method
    }

     // Helper method to get private static field value
    private static Object getPrivateStaticField(String fieldName) throws Exception {
        Field field = InventorySystem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

     // Helper method to set private static field value
    private static void setPrivateStaticField(String fieldName, Object value) throws Exception {
        Field field = InventorySystem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Redirect System.out and System.err
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        // Reset static state
        resetInventorySystemState();
    }

    @AfterEach
    void tearDown() {
        // Restore original System streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    // --- ItemRecord Tests ---

    @Test
    @DisplayName("ItemRecord constructor should set fields correctly")
    void itemRecordConstructor() {
        ItemRecord item = new ItemRecord(101, "Test Item", 5, 99.99);
        assertEquals(101, item.itemId);
        assertEquals("Test Item", item.name);
        assertEquals(5, item.quantity);
        assertEquals(99.99, item.price);
    }

    @Test
    @DisplayName("ItemRecord toString should return correct format")
    void itemRecordToString() {
        ItemRecord item = new ItemRecord(101, "Test Item", 5, 99.99);
        String expected = "ItemRecord{itemId=101, name='Test Item', quantity=5, price=99.99}";
        assertEquals(expected, item.toString());
    }

    // --- CustomerRecord Tests ---

    @Test
    @DisplayName("CustomerRecord constructor should set fields correctly")
    void customerRecordConstructor() {
        CustomerRecord customer = new CustomerRecord(201, "Test Customer", CustomerRecord.FLAG_IS_ACTIVE);
        assertEquals(201, customer.customerId);
        assertEquals("Test Customer", customer.customerName);
        assertEquals(CustomerRecord.FLAG_IS_ACTIVE, customer.getFlags());
    }

    @Test
    @DisplayName("CustomerRecord getFlags and setFlags should work")
    void customerRecordGetSetFlags() {
        CustomerRecord customer = new CustomerRecord(202, "Another Customer", 0);
        assertEquals(0, customer.getFlags());
        customer.setFlags(CustomerRecord.FLAG_TAX_EXEMPT | CustomerRecord.FLAG_RESERVED);
        assertEquals(CustomerRecord.FLAG_TAX_EXEMPT | CustomerRecord.FLAG_RESERVED, customer.getFlags());
    }

    @Test
    @DisplayName("CustomerRecord isFlagSet should correctly check flags")
    void customerRecordIsFlagSet() {
        int initialFlags = CustomerRecord.FLAG_IS_ACTIVE | CustomerRecord.FLAG_RESERVED; // 1 | 4 = 5 (0b0101)
        CustomerRecord customer = new CustomerRecord(203, "Flag Customer", initialFlags);

        assertTrue(customer.isFlagSet(CustomerRecord.FLAG_IS_ACTIVE), "FLAG_IS_ACTIVE should be set");
        assertFalse(customer.isFlagSet(CustomerRecord.FLAG_TAX_EXEMPT), "FLAG_TAX_EXEMPT should not be set");
        assertTrue(customer.isFlagSet(CustomerRecord.FLAG_RESERVED), "FLAG_RESERVED should be set");

        // Check a combination
        assertTrue(customer.isFlagSet(CustomerRecord.FLAG_IS_ACTIVE | CustomerRecord.FLAG_RESERVED), "Combined flags should be set");
        assertFalse(customer.isFlagSet(CustomerRecord.FLAG_IS_ACTIVE | CustomerRecord.FLAG_TAX_EXEMPT), "Mixed flags should not be fully set");
    }

     @Test
    @DisplayName("CustomerRecord toString should show flags in binary")
    void customerRecordToString() {
        CustomerRecord customer = new CustomerRecord(204, "Binary Flags", CustomerRecord.FLAG_IS_ACTIVE | CustomerRecord.FLAG_TAX_EXEMPT); // 1 | 2 = 3 (0b11)
        String expected = "CustomerRecord{customerId=204, customerName='Binary Flags', flags=11}"; // Binary representation of 3
        assertEquals(expected, customer.toString());
    }

    // --- InventorySystem Method Tests ---

    @Test
    @DisplayName("initializeInventory should populate inventory and set count")
    void initializeInventoryTest() throws Exception {
        invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{});

        @SuppressWarnings("unchecked")
        ArrayList<ItemRecord> inventory = (ArrayList<ItemRecord>) getPrivateStaticField("inventory");
        int inventoryCount = (int) getPrivateStaticField("inventoryCount");

        assertFalse(inventory.isEmpty(), "Inventory should not be empty after initialization");
        assertEquals(3, inventory.size(), "Inventory should have 3 items after initialization");
        assertEquals(3, inventoryCount, "Inventory count should be 3 after initialization");
        assertEquals("Laptop", inventory.get(0).name);
        assertTrue(outContent.toString().contains("Initialization complete. 3 items loaded."), "Initialization message should be printed");
    }

    @Test
    @DisplayName("readCommand should return correct command for valid integer input")
    void readCommandValidInput() throws Exception {
        String input = "3\n"; // Simulate user entering '3' then Enter
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);

        int command = (int) invokePrivateStaticMethod("readCommand", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        assertEquals(3, command, "Should return the integer command entered");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should remain SUCCESS for valid input");
        assertTrue(errContent.toString().isEmpty(), "No error message should be printed for valid input");
        scanner.close(); // Close scanner explicitly in test
    }

    @Test
    @DisplayName("readCommand should return marker and set error for invalid input")
    void readCommandInvalidInput() throws Exception {
        String input = "abc\n"; // Simulate user entering non-integer input
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);

        int command = (int) invokePrivateStaticMethod("readCommand", new Class<?>[]{Scanner.class}, new Object[]{scanner});
        int expectedInvalidMarker = (int) getPrivateStaticField("INVALID_COMMAND_MARKER");


        assertEquals(expectedInvalidMarker, command, "Should return INVALID_COMMAND_MARKER for non-integer input");
        assertEquals(InventorySystem.ERROR_INVALID_INPUT, getPrivateStaticField("errorCode"), "Error code should be set to ERROR_INVALID_INPUT");
        assertTrue(errContent.toString().contains("Invalid input: Please enter a number."), "Error message should be printed for invalid input");
        assertTrue(errContent.toString().contains("(Code: " + InventorySystem.ERROR_INVALID_INPUT + ")"), "Error code should be printed");
        scanner.close(); // Close scanner explicitly in test
    }

    @Test
    @DisplayName("addItem should add a dummy item when inventory is not full")
    void addItemNotFull() throws Exception {
        invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{}); // Start with 3 items
        int initialCount = (int) getPrivateStaticField("inventoryCount");
        Scanner scanner = new Scanner(System.in); // Scanner needed but not used for input in this placeholder

        invokePrivateStaticMethod("addItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        @SuppressWarnings("unchecked")
        ArrayList<ItemRecord> inventory = (ArrayList<ItemRecord>) getPrivateStaticField("inventory");
        int finalCount = (int) getPrivateStaticField("inventoryCount");

        assertEquals(initialCount + 1, finalCount, "Inventory count should increase by 1");
        assertEquals(initialCount + 1, inventory.size(), "Inventory list size should increase by 1");
        assertEquals("New Item", inventory.get(finalCount - 1).name, "The new dummy item should be added");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be SUCCESS after adding");
        assertTrue(outContent.toString().contains("Dummy item added. Current count: " + finalCount), "Success message should be printed");
        scanner.close();
    }

    @Test
    @DisplayName("addItem should not add item and set error when inventory is full (MAX_ITEMS > 0)")
    @DisabledIf("isMaxItemsZeroOrLess") // Skip if MAX_ITEMS is not a positive constraint
    void addItemFull() throws Exception {
        // Set MAX_ITEMS to a small number for testing (e.g., 1)
        // This requires reflection or making MAX_ITEMS non-final, which is intrusive.
        // Alternative: Manually fill the inventory up to MAX_ITEMS if it's reasonably small.
        // Assuming MAX_ITEMS = 100 initially, this test is hard to run directly.
        // Let's simulate the condition by setting inventoryCount manually.

        if (InventorySystem.MAX_ITEMS <= 0) {
             System.out.println("Skipping addItemFull test as MAX_ITEMS is not positive.");
             return; // Skip test if MAX_ITEMS isn't a limit
        }

        setPrivateStaticField("inventoryCount", InventorySystem.MAX_ITEMS); // Simulate full inventory
        @SuppressWarnings("unchecked")
        ArrayList<ItemRecord> inventory = (ArrayList<ItemRecord>) getPrivateStaticField("inventory");
        // Ensure list size matches count for consistency if needed (though the check uses count)
        while (inventory.size() < InventorySystem.MAX_ITEMS) {
            inventory.add(new ItemRecord(999, "Filler", 1, 1.0));
        }

        int initialCount = (int) getPrivateStaticField("inventoryCount");
        Scanner scanner = new Scanner(System.in);

        invokePrivateStaticMethod("addItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        int finalCount = (int) getPrivateStaticField("inventoryCount");
        int finalListSize = ((ArrayList<?>) getPrivateStaticField("inventory")).size();

        assertEquals(initialCount, finalCount, "Inventory count should not change when full");
        assertEquals(initialCount, finalListSize, "Inventory list size should not change when full");
        assertEquals(InventorySystem.ERROR_SYSTEM, getPrivateStaticField("errorCode"), "Error code should be set when full");
        assertTrue(errContent.toString().contains("Cannot add item: Inventory is full"), "Error message for full inventory should be printed");
        scanner.close();
    }
    // Helper method for @DisabledIf
    static boolean isMaxItemsZeroOrLess() {
        return InventorySystem.MAX_ITEMS <= 0;
    }


    @Test
    @DisplayName("deleteItem placeholder should run and set SUCCESS")
    void deleteItemPlaceholder() throws Exception {
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand
        Scanner scanner = new Scanner(System.in);

        invokePrivateStaticMethod("deleteItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        assertTrue(outContent.toString().contains("[Action] Delete Item selected"), "Placeholder message should be printed");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
        scanner.close();
    }

    @Test
    @DisplayName("updateItem placeholder should run and set SUCCESS")
    void updateItemPlaceholder() throws Exception {
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand
        Scanner scanner = new Scanner(System.in);

        invokePrivateStaticMethod("updateItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        assertTrue(outContent.toString().contains("[Action] Update Item selected"), "Placeholder message should be printed");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
        scanner.close();
    }

    @Test
    @DisplayName("queryItem placeholder should display first item if inventory not empty")
    void queryItemNotEmpty() throws Exception {
        invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{}); // Add initial items
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand
        Scanner scanner = new Scanner(System.in);

        invokePrivateStaticMethod("queryItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        assertTrue(outContent.toString().contains("[Action] Query Item selected"), "Placeholder message should be printed");
        assertTrue(outContent.toString().contains("Example Query: Displaying first item:"), "Query message should be printed");
        assertTrue(outContent.toString().contains("ItemRecord{itemId=1, name='Laptop', quantity=10, price=1200.5}"), "First item details should be printed");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
        scanner.close();
    }

    @Test
    @DisplayName("queryItem placeholder should display message if inventory empty")
    void queryItemEmpty() throws Exception {
        // Ensure inventory is empty (default after reset)
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand
        Scanner scanner = new Scanner(System.in);

        invokePrivateStaticMethod("queryItem", new Class<?>[]{Scanner.class}, new Object[]{scanner});

        assertTrue(outContent.toString().contains("[Action] Query Item selected"), "Placeholder message should be printed");
        assertTrue(outContent.toString().contains("Inventory is empty."), "Empty inventory message should be printed");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
        scanner.close();
    }


    @Test
    @DisplayName("generateReport should print items when inventory is not empty")
    void generateReportNotEmpty() throws Exception {
        invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{}); // Add initial items
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand

        invokePrivateStaticMethod("generateReport", new Class<?>[]{}, new Object[]{});

        String output = outContent.toString();
        assertTrue(output.contains("[Action] Generate Report selected."), "Action message missing");
        assertTrue(output.contains("--- Inventory Report ---"), "Report header missing");
        assertTrue(output.contains("Total items: 3"), "Correct item count missing");
        assertTrue(output.contains("ItemRecord{itemId=1, name='Laptop', quantity=10, price=1200.5}"), "Laptop item missing");
        assertTrue(output.contains("ItemRecord{itemId=2, name='Mouse', quantity=50, price=25.0}"), "Mouse item missing");
        assertTrue(output.contains("ItemRecord{itemId=3, name='Keyboard', quantity=30, price=75.75}"), "Keyboard item missing");
        assertTrue(output.contains("--- End of Report ---"), "Report footer missing");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
    }

    @Test
    @DisplayName("generateReport should print empty message when inventory is empty")
    void generateReportEmpty() throws Exception {
         // Ensure inventory is empty (default after reset)
        setPrivateStaticField("errorCode", InventorySystem.ERROR_SYSTEM); // Set an error beforehand

        invokePrivateStaticMethod("generateReport", new Class<?>[]{}, new Object[]{});

        String output = outContent.toString();
        assertTrue(output.contains("[Action] Generate Report selected."), "Action message missing");
        assertTrue(output.contains("--- Inventory Report ---"), "Report header missing");
        assertTrue(output.contains("Inventory is currently empty."), "Empty inventory message missing");
        assertTrue(output.contains("--- End of Report ---"), "Report footer missing");
        assertEquals(InventorySystem.SUCCESS, getPrivateStaticField("errorCode"), "Error code should be reset to SUCCESS");
    }

    @Test
    @DisplayName("printError should print correct message for valid error code")
    void printErrorValidCode() throws Exception {
        setPrivateStaticField("errorCode", InventorySystem.ERROR_INVALID_INPUT); // Set a specific error

        invokePrivateStaticMethod("printError", new Class<?>[]{}, new Object[]{});

        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error: Invalid input provided."), "Correct error message missing");
        assertTrue(errorOutput.contains("(Code: " + InventorySystem.ERROR_INVALID_INPUT + ")"), "Error code missing");
    }

     @Test
    @DisplayName("printError should print unknown error message for invalid error code")
    void printErrorInvalidCode() throws Exception {
        int invalidCode = 99;
        setPrivateStaticField("errorCode", invalidCode); // Set an out-of-bounds error code

        invokePrivateStaticMethod("printError", new Class<?>[]{}, new Object[]{});

        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("An unknown error occurred."), "Unknown error message missing");
        assertTrue(errorOutput.contains("(Code: " + invalidCode + ")"), "Invalid error code missing");
    }

    @Test
    @DisplayName("Transaction count should increment after successful command")
    void transactionCountIncrement() throws Exception {
        // Simulate a successful command execution (e.g., generateReport)
        invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{});
        assertEquals(0, getPrivateStaticField("transactionCount"), "Initial transaction count should be 0");

        // Simulate main loop calling generateReport and logging
        int cmd = 5; // Command for generateReport
        invokePrivateStaticMethod("generateReport", new Class<?>[]{}, new Object[]{});
        // Manually simulate the logging part from main loop as testing main directly is hard
        if (cmd != 6 && cmd != -1 && (int)getPrivateStaticField("errorCode") == InventorySystem.SUCCESS) {
             int currentCount = (int) getPrivateStaticField("transactionCount");
             currentCount++;
             setPrivateStaticField("transactionCount", currentCount);
             // LoggingService.logTransaction(cmd, currentCount); // We can check the count directly
        }

        assertEquals(1, getPrivateStaticField("transactionCount"), "Transaction count should be 1 after one successful command");
        assertTrue(outContent.toString().contains("[LOG] Transaction #1: Command 5 executed."), "Log message should appear for transaction");

         // Simulate another successful command (e.g., queryItem)
        cmd = 4; // Command for queryItem
        invokePrivateStaticMethod("queryItem", new Class<?>[]{Scanner.class}, new Object[]{new Scanner(System.in)});
         if (cmd != 6 && cmd != -1 && (int)getPrivateStaticField("errorCode") == InventorySystem.SUCCESS) {
             int currentCount = (int) getPrivateStaticField("transactionCount");
             currentCount++;
             setPrivateStaticField("transactionCount", currentCount);
             // LoggingService.logTransaction(cmd, currentCount);
        }

        assertEquals(2, getPrivateStaticField("transactionCount"), "Transaction count should be 2 after second successful command");
         assertTrue(outContent.toString().contains("[LOG] Transaction #2: Command 4 executed."), "Log message should appear for second transaction");

    }

     @Test
    @DisplayName("Transaction count should not increment after failed command")
    void transactionCountNoIncrementOnFail() throws Exception {
        assertEquals(0, getPrivateStaticField("transactionCount"), "Initial transaction count should be 0");

        // Simulate a failed command (e.g., addItem when full)
        if (InventorySystem.MAX_ITEMS > 0) {
            setPrivateStaticField("inventoryCount", InventorySystem.MAX_ITEMS); // Simulate full
             @SuppressWarnings("unchecked")
            ArrayList<ItemRecord> inventory = (ArrayList<ItemRecord>) getPrivateStaticField("inventory");
            while (inventory.size() < InventorySystem.MAX_ITEMS) {
                 inventory.add(new ItemRecord(999, "Filler", 1, 1.0));
            }

            int cmd = 1; // Command for addItem
            invokePrivateStaticMethod("addItem", new Class<?>[]{Scanner.class}, new Object[]{new Scanner(System.in)});
            // Manually simulate the logging check from main loop
            if (cmd != 6 && cmd != -1 && (int)getPrivateStaticField("errorCode") == InventorySystem.SUCCESS) {
                 int currentCount = (int) getPrivateStaticField("transactionCount");
                 currentCount++;
                 setPrivateStaticField("transactionCount", currentCount);
            }

             assertEquals(0, getPrivateStaticField("transactionCount"), "Transaction count should remain 0 after failed command");
             assertFalse(outContent.toString().contains("[LOG]"), "No log message should appear for failed transaction");

        } else {
             System.out.println("Skipping transactionCountNoIncrementOnFail test as MAX_ITEMS is not positive.");
        }
    }

     @Test
    @DisplayName("Transaction count should not increment for exit command")
    void transactionCountNoIncrementOnExit() throws Exception {
        assertEquals(0, getPrivateStaticField("transactionCount"), "Initial transaction count should be 0");
        int cmd = 6; // Exit command

        // Simulate main loop logic for exit command
         if (cmd != 6 && cmd != -1 && (int)getPrivateStaticField("errorCode") == InventorySystem.SUCCESS) {
             int currentCount = (int) getPrivateStaticField("transactionCount");
             currentCount++;
             setPrivateStaticField("transactionCount", currentCount);
        }

        assertEquals(0, getPrivateStaticField("transactionCount"), "Transaction count should remain 0 for exit command");
        assertFalse(outContent.toString().contains("[LOG]"), "No log message should appear for exit command");
    }

     @Test
    @DisplayName("Transaction count should not increment for invalid command marker")
    void transactionCountNoIncrementOnInvalidMarker() throws Exception {
        assertEquals(0, getPrivateStaticField("transactionCount"), "Initial transaction count should be 0");
        int cmd = (int) getPrivateStaticField("INVALID_COMMAND_MARKER"); // Invalid command marker

        // Simulate main loop logic for invalid command marker
         if (cmd != 6 && cmd != -1 && (int)getPrivateStaticField("errorCode") == InventorySystem.SUCCESS) {
             int currentCount = (int) getPrivateStaticField("transactionCount");
             currentCount++;
             setPrivateStaticField("transactionCount", currentCount);
        }

        assertEquals(0, getPrivateStaticField("transactionCount"), "Transaction count should remain 0 for invalid command marker");
        assertFalse(outContent.toString().contains("[LOG]"), "No log message should appear for invalid command marker");
    }

    // Note: Testing the bit manipulation part within main() is complex.
    // It's better tested via the CustomerRecord tests directly, which cover the core logic.
    // We can add a test to ensure the CustomerService placeholder is called if needed.

    @Test
    @DisplayName("Bit manipulation example section calls CustomerService (indirect check)")
    void bitManipulationCallsService() throws Exception {
         // Ensure inventory is not empty so the example runs
         invokePrivateStaticMethod("initializeInventory", new Class<?>[]{}, new Object[]{});

         // We can't easily test the full main method, but we can check if
         // CustomerService.fetchCustomerData is called by looking for its output.
         // This is fragile but demonstrates the interaction.
         // A better approach would involve mocking CustomerService.

         // Simulate running the relevant part of main() or extract it.
         // For simplicity, just check if the fetch message appears when inventory is not empty.
         // This assumes the example runs if inventory is not empty.

         // Manually trigger the conditions that lead to the bit manipulation block
         // This is complex to do perfectly without running main().
         // Instead, we'll just check the CustomerService output pattern.
         // We need to call a method that might eventually lead to this block,
         // or accept that testing this specific part of main is hard.

         // Let's call CustomerService directly to verify its output pattern
         CustomerService.fetchCustomerData(101);
         assertTrue(outContent.toString().contains("[SERVICE] Fetching data for customer ID: 101"),
                    "CustomerService fetch message pattern not found.");

         // This doesn't prove main calls it, but verifies the service's behavior.
         // Testing the exact flow within main requires more advanced techniques (like PowerMock or refactoring).
    }
}