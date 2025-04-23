import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * The {@code InventorySystem} class manages a basic inventory of items via a
 * text-based console interface.
 * It replicates the functionality of the original TAL {@code inventory_system} module.
 * It presents a menu to the user, allowing them to perform operations such as
 * adding, deleting, updating, and querying inventory items, as well as generating
 * reports. The class utilizes static variables to store inventory data, error
 * status, and configuration constants.
 */
public class InventorySystem {

    // --- Constants ---
    private static final int MAX_ITEMS = 100; // Maximum number of inventory items
    private static final int MAX_CUSTOMERS = 50; // Example constant (not fully used)
    private static final int BUFFER_SIZE = 4096; // Capacity for the StringBuilder buffer

    // Status Codes
    private static final int SUCCESS = 0;
    private static final int ERROR_INVALID_INPUT = 1;
    private static final int ERROR_IO_FAILURE = 2; // Example for potential I/O errors

    // Command Codes (as per COMMAND_CODES array)
    private static final int CMD_ADD = 1;
    private static final int CMD_DELETE = 2;
    private static final int CMD_UPDATE = 3;
    private static final int CMD_QUERY = 4;
    private static final int CMD_REPORT = 5;
    private static final int CMD_EXIT = 6;

    // --- Static Variables (Global State) ---

    /** Stores the status code of the last operation. 0 indicates success. */
    private static int errorCode = SUCCESS;

    /** Contains text descriptions for error codes. Indexed by errorCode. */
    private static final String[] ERROR_MESSAGES = {
            "Operation successful.", // 0: SUCCESS
            "Error: Invalid command or input.", // 1: ERROR_INVALID_INPUT
            "Error: Input/Output failure." // 2: ERROR_IO_FAILURE
            // Add more error messages as needed
    };

    /** Tracks the number of processed commands (transactions). */
    private static int transactionCount = 0;

    /** Array holding the inventory data objects. */
    private static ItemRecord[] inventory;

    /** Current number of items in the inventory array. */
    private static int inventoryCount = 0;

    /** Reference used in the example bit manipulation section. Needs proper initialization. */
    private static CustomerRecord currentCustomer; // Initialized in main

    /** General-purpose buffer for string formatting and console I/O. */
    private static final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);

    /** Numeric codes for menu commands. */
    private static final int[] COMMAND_CODES = { CMD_ADD, CMD_DELETE, CMD_UPDATE, CMD_QUERY, CMD_REPORT, CMD_EXIT };

    /** Text names for menu commands. Must correspond to COMMAND_CODES. */
    private static final String[] COMMAND_NAMES = {
            "Add Item",
            "Delete Item",
            "Update Item",
            "Query Item",
            "Generate Report",
            "Exit"
    };

    // --- Nested Classes (Representing Structures) ---

    /**
     * Defines the structure for an inventory item.
     */
    static class ItemRecord {
        int itemId;
        String name;
        double price;
        int quantity;

        /**
         * Constructs an ItemRecord.
         * @param itemId The unique ID of the item.
         * @param name The name of the item.
         * @param price The price of the item.
         * @param quantity The quantity in stock.
         */
        ItemRecord(int itemId, String name, double price, int quantity) {
            this.itemId = itemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "ItemRecord{" +
                   "itemId=" + itemId +
                   ", name='" + name + '\'' +
                   ", price=" + price +
                   ", quantity=" + quantity +
                   '}';
        }
    }

    /**
     * Defines the structure for a customer record.
     * Used in the bit manipulation example.
     */
    static class CustomerRecord {
        int customerId;
        boolean isActive;   // Represents the 'isActive' bit/flag
        boolean taxExempt; // Represents the 'taxExempt' bit/flag

        /**
         * Constructs a CustomerRecord.
         * @param customerId The unique ID of the customer.
         */
        CustomerRecord(int customerId) {
            this.customerId = customerId;
            this.isActive = false; // Default state
            this.taxExempt = false; // Default state
        }

        @Override
        public String toString() {
            return "CustomerRecord{" +
                   "customerId=" + customerId +
                   ", isActive=" + isActive +
                   ", taxExempt=" + taxExempt +
                   '}';
        }
    }

    // --- External Dependency Simulation ---

    /**
     * Simulates an external logging mechanism.
     */
    static class ExternalLogger {
        /**
         * Logs a transaction. In a real system, this might write to a file or database.
         * @param commandCode The code of the command executed.
         * @param count The current transaction count.
         */
        public static void logTransaction(int commandCode, int count) {
            // Simulate logging the transaction
            System.out.println("[External Logger] Logged transaction: Command=" + commandCode + ", Count=" + count);
        }
    }

    // --- Static Methods (Representing Subprocedures) ---

    /**
     * Initializes the inventory array with sample data.
     * Sets the initial {@code inventoryCount}.
     */
    private static void initializeInventory() {
        inventory = new ItemRecord[MAX_ITEMS];
        // Add some sample items
        inventory[0] = new ItemRecord(101, "Laptop", 1200.50, 10);
        inventory[1] = new ItemRecord(102, "Mouse", 25.00, 50);
        inventory[2] = new ItemRecord(103, "Keyboard", 75.75, 30);
        inventoryCount = 3;
        // Initialize other items to null or handle empty slots appropriately
        writeLine("Inventory initialized with " + inventoryCount + " items.");
    }

    /**
     * Displays the main menu options to the console using the static buffer.
     */
    private static void displayMenu() {
        buffer.setLength(0); // Clear the buffer
        buffer.append("\n--- Inventory System Menu ---\n");
        for (int i = 0; i < COMMAND_CODES.length; i++) {
            buffer.append(COMMAND_CODES[i]).append(". ").append(COMMAND_NAMES[i]).append("\n");
        }
        buffer.append("---------------------------\n");
        buffer.append("Enter command number: ");
        write(buffer.toString()); // Use write to avoid extra newline before input
    }

    /**
     * Reads an integer command from the standard input (console).
     * Includes basic validation for integer input.
     *
     * @return The integer command entered by the user, or -1 if input was invalid.
     */
    private static int readCommand() {
        Scanner scanner = new Scanner(System.in);
        int command = -1; // Default to invalid command indicator
        try {
            command = scanner.nextInt();
        } catch (InputMismatchException e) {
            writeLine("Error: Please enter a valid integer command.");
            scanner.next(); // Consume the invalid input
            errorCode = ERROR_INVALID_INPUT; // Set error code for invalid format
            // No need to call printError here, handled in main loop's default case
        }
        // Note: Scanner is not closed here to keep System.in open for the loop.
        // In a larger application, manage resources more carefully.
        return command;
    }

    /**
     * Prints an error message to the console based on the current {@code errorCode}.
     * Resets {@code errorCode} to {@code SUCCESS} after printing.
     */
    private static void printError() {
        if (errorCode != SUCCESS && errorCode < ERROR_MESSAGES.length) {
            writeLine(ERROR_MESSAGES[errorCode]);
        } else if (errorCode != SUCCESS) {
            writeLine("Error: An unknown error occurred (Code: " + errorCode + ")");
        }
        errorCode = SUCCESS; // Reset error code after reporting
    }

    /**
     * Placeholder method for adding an item to the inventory.
     * Currently prints a "not implemented" message.
     */
    private static void addItem() {
        // TODO: Implement item addition logic (get input, validate, add to array, check bounds)
        writeLine("Add item functionality not yet implemented.");
        // Example: Check if inventory is full
        // if (inventoryCount >= MAX_ITEMS) {
        //     errorCode = ERROR_INVENTORY_FULL; // Need to define this error code
        //     printError();
        //     return;
        // }
        // ... read item details ...
        // inventory[inventoryCount++] = newItem;
    }

    /**
     * Placeholder method for deleting an item from the inventory.
     * Currently prints a "not implemented" message.
     */
    private static void deleteItem() {
        // TODO: Implement item deletion logic (get item ID, find, remove/shift elements)
        writeLine("Delete item functionality not yet implemented.");
    }

    /**
     * Placeholder method for updating an existing item in the inventory.
     * Currently prints a "not implemented" message.
     */
    private static void updateItem() {
        // TODO: Implement item update logic (get item ID, find, get new details, update)
        writeLine("Update item functionality not yet implemented.");
    }

    /**
     * Placeholder method for querying (searching for) an item in the inventory.
     * Currently prints a "not implemented" message.
     */
    private static void queryItem() {
        // TODO: Implement item query logic (get item ID/name, find, display details)
        writeLine("Query item functionality not yet implemented.");
    }

    /**
     * Placeholder method for generating an inventory report.
     * Currently prints a "not implemented" message.
     */
    private static void generateReport() {
        // TODO: Implement report generation logic (iterate through inventory, format, print)
        writeLine("Generate report functionality not yet implemented.");
        // Example: Print current items
        // buffer.setLength(0);
        // buffer.append("\n--- Inventory Report ---\n");
        // for (int i = 0; i < inventoryCount; i++) {
        //     if (inventory[i] != null) {
        //         buffer.append(inventory[i].toString()).append("\n");
        //     }
        // }
        // buffer.append("--- End of Report ---\n");
        // writeLine(buffer.toString());
    }

    /**
     * Writes a string followed by a newline character to the standard output.
     * Placeholder for a potentially more complex output routine.
     * @param message The string to write.
     */
    private static void writeLine(String message) {
        System.out.println(message);
    }

    /**
     * Writes a string without a trailing newline character to the standard output.
     * Placeholder for a potentially more complex output routine.
     * @param message The string to write.
     */
    private static void write(String message) {
        System.out.print(message);
    }

    // --- Main Application Logic ---

    /**
     * The main entry point for the Inventory System application.
     * Initializes the system, runs the command loop, and handles termination.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // 1. Initialization
        initializeInventory();
        // Initialize the example customer record
        currentCustomer = new CustomerRecord(9001); // Example customer ID

        int cmd = 0; // User command input

        // 2. Main Processing Loop
        while (cmd != CMD_EXIT) {
            displayMenu();
            cmd = readCommand();

            // Check if readCommand itself indicated an input format error
            if (errorCode == ERROR_INVALID_INPUT && cmd == -1) {
                 printError(); // Print the error set by readCommand
                 cmd = 0; // Reset cmd to continue loop safely
                 continue; // Skip switch and transaction logging
            }


            // 3. Command Dispatch
            boolean validCommandProcessed = false;
            switch (cmd) {
                case CMD_ADD:
                    addItem();
                    validCommandProcessed = true;
                    break;
                case CMD_DELETE:
                    deleteItem();
                    validCommandProcessed = true;
                    break;
                case CMD_UPDATE:
                    updateItem();
                    validCommandProcessed = true;
                    break;
                case CMD_QUERY:
                    queryItem();
                    validCommandProcessed = true;
                    break;
                case CMD_REPORT:
                    generateReport();
                    validCommandProcessed = true;
                    break;
                case CMD_EXIT:
                    writeLine("Exiting Inventory System...");
                    break; // Exit command, loop will terminate
                default:
                    // Handles invalid numeric commands (e.g., 0, 7, negative numbers)
                    // Also catches the -1 from readCommand if not handled earlier
                    if (cmd != -1) { // Avoid double error message if already handled
                       errorCode = ERROR_INVALID_INPUT;
                       printError();
                    }
                    break; // Invalid command number
            }

            // 4. Transaction Logging (if command was valid and not EXIT)
            if (validCommandProcessed) {
                transactionCount++;
                ExternalLogger.logTransaction(cmd, transactionCount);
            }
        } // End of while loop

        // 5. Cleanup and Termination
        writeLine("Performing final checks...");

        // Check for any outstanding errors (might be redundant if handled in loop)
        if (errorCode != SUCCESS) {
            printError(); // Print any error that occurred just before exiting
        }

        // Example bit manipulation sequence (using boolean fields)
        if (inventoryCount > 0 && currentCustomer != null) {
            writeLine("Updating customer status flags (example)...");
            currentCustomer.isActive = true;   // Set isActive flag
            currentCustomer.taxExempt = true; // Set taxExempt flag
            writeLine("Current Customer State: " + currentCustomer);
        } else if (currentCustomer == null) {
             writeLine("Skipping customer update example: currentCustomer is null.");
        }


        writeLine("Inventory System terminated normally.");
        System.exit(SUCCESS); // Terminate with success code
    }
}