import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Represents a simple inventory item record.
 * Contains basic information about an item.
 */
class ItemRecord {
    /** Unique identifier for the item. */
    public int itemId;
    /** Name of the item. */
    public String name;
    /** Quantity of the item in stock. */
    public int quantity;
    /** Price per unit of the item. */
    public double price;

    /**
     * Constructs a new ItemRecord.
     *
     * @param itemId   The item's unique ID.
     * @param name     The item's name.
     * @param quantity The initial quantity.
     * @param price    The item's price.
     */
    public ItemRecord(int itemId, String name, int quantity, double price) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String toString() {
        return "ItemRecord{" +
               "itemId=" + itemId +
               ", name='" + name + '\'' +
               ", quantity=" + quantity +
               ", price=" + price +
               '}';
    }
}

/**
 * Represents a customer record.
 * Includes flags managed using bitwise operations.
 */
class CustomerRecord {
    /** Flag indicating the customer account is active (Bit 0). */
    public static final int FLAG_IS_ACTIVE = 0b0001; // 1
    /** Flag indicating the customer is tax-exempt (Bit 1). */
    public static final int FLAG_TAX_EXEMPT = 0b0010; // 2
    /** Flag indicating the customer record is reserved (Bit 2). */
    public static final int FLAG_RESERVED = 0b0100; // 4

    /** Unique identifier for the customer. */
    public int customerId;
    /** Customer's name. */
    public String customerName;
    /** Field storing status flags using bits. */
    private int flags;

    /**
     * Constructs a new CustomerRecord.
     *
     * @param customerId   The customer's unique ID.
     * @param customerName The customer's name.
     * @param initialFlags The initial flag settings.
     */
    public CustomerRecord(int customerId, String customerName, int initialFlags) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.flags = initialFlags;
    }

    /**
     * Gets the current value of the flags field.
     *
     * @return The integer representation of the flags.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Sets the flags field to a new value.
     *
     * @param newFlags The new integer value for the flags.
     */
    public void setFlags(int newFlags) {
        this.flags = newFlags;
    }

    /**
     * Checks if a specific flag is set.
     *
     * @param flagToCheck The flag constant (e.g., FLAG_IS_ACTIVE) to check.
     * @return true if the flag is set, false otherwise.
     */
    public boolean isFlagSet(int flagToCheck) {
        return (this.flags & flagToCheck) == flagToCheck;
    }

    @Override
    public String toString() {
        return "CustomerRecord{" +
               "customerId=" + customerId +
               ", customerName='" + customerName + '\'' +
               ", flags=" + Integer.toBinaryString(flags) + // Show flags in binary
               '}';
    }
}

/**
 * Placeholder for a logging service.
 */
class LoggingService {
    /**
     * Logs a transaction.
     * (Placeholder implementation - prints to console).
     *
     * @param command The command code that was executed.
     * @param count   The current transaction count.
     */
    public static void logTransaction(int command, int count) {
        System.out.println("[LOG] Transaction #" + count + ": Command " + command + " executed.");
    }
}

/**
 * Placeholder for a customer data service.
 */
class CustomerService {
    /**
     * Fetches customer data based on an ID.
     * (Placeholder implementation - returns a dummy customer).
     *
     * @param customerId The ID of the customer to fetch.
     * @return A CustomerRecord object, or null if not found (dummy implementation always returns one).
     */
    public static CustomerRecord fetchCustomerData(int customerId) {
        System.out.println("[SERVICE] Fetching data for customer ID: " + customerId);
        // Return a dummy customer for the bit manipulation example
        return new CustomerRecord(customerId, "Dummy Customer " + customerId, 0); // Start with flags unset
    }
}

/**
 * InventorySystem is a command-line application for managing a simple inventory.
 * It allows users to add, delete, update, query items, and generate reports.
 * The application uses static fields, custom classes, collections, and method calls.
 */
public class InventorySystem {

    // --- Constants ---
    /** Maximum allowed items in the inventory (example limit). */
    public static final int MAX_ITEMS = 100;
    // public static final int BUFFER_SIZE = 1024; // Optional: If StringBuilder buffer was used

    /** Status code for successful execution. */
    public static final int SUCCESS = 0;
    /** Error code for file not found (example). */
    public static final int ERROR_FILE_NOT_FOUND = 1; // Example error code
    /** Error code for invalid user input. */
    public static final int ERROR_INVALID_INPUT = 2;
    /** Error code for general system errors (example). */
    public static final int ERROR_SYSTEM = 3; // Example error code

    /** Command code to exit the application. */
    private static final int EXIT_COMMAND = 6;
    /** Command code representing an invalid command read from input. */
    private static final int INVALID_COMMAND_MARKER = -1;

    // --- Static Fields (Simulating Global State) ---

    /** Stores the inventory items. */
    private static ArrayList<ItemRecord> inventory;
    /** Current number of items in inventory (can be derived from inventory.size()). */
    private static int inventoryCount;
    /** Counter for processed transactions. */
    private static int transactionCount;
    /** Stores the most recent error status code. */
    private static int errorCode;
    /** Array containing error descriptions indexed by error code. */
    private static final String[] ERROR_MESSAGES = {
            "Operation successful.", // Corresponds to SUCCESS (0)
            "Error: File not found.", // Corresponds to ERROR_FILE_NOT_FOUND (1)
            "Error: Invalid input provided.", // Corresponds to ERROR_INVALID_INPUT (2)
            "Error: General system error occurred." // Corresponds to ERROR_SYSTEM (3)
            // Add more messages as needed
    };

    /** Reference to a CustomerRecord object for bit manipulation example. */
    private static CustomerRecord currentCustomer; // Note: Static usage might not be ideal in larger apps.

    // Optional: Global buffer if complex string building is needed across methods.
    // private static StringBuilder buffer;

    /**
     * The main entry point for the Inventory System application.
     * Initializes the system, runs the main command loop, and handles termination.
     *
     * @param args Command-line arguments (not used by this application).
     */
    public static void main(String[] args) {
        // 1. Initialization
        inventory = new ArrayList<>(MAX_ITEMS > 0 ? MAX_ITEMS : 10); // Initial capacity hint
        inventoryCount = 0;
        transactionCount = 0;
        errorCode = SUCCESS;
        // buffer = new StringBuilder(BUFFER_SIZE); // If buffer is used

        initializeInventory();

        int cmd = 0; // User command choice

        // Use try-with-resources for Scanner to ensure it's closed
        try (Scanner scanner = new Scanner(System.in)) {
            // 2. Main Command Loop
            while (cmd != EXIT_COMMAND) {
                displayMenu();
                cmd = readCommand(scanner);

                switch (cmd) {
                    case 1:
                        addItem(scanner);
                        break;
                    case 2:
                        deleteItem(scanner);
                        break;
                    case 3:
                        updateItem(scanner);
                        break;
                    case 4:
                        queryItem(scanner);
                        break;
                    case 5:
                        generateReport();
                        break;
                    case EXIT_COMMAND: // Command 6: Exit
                        System.out.println("Exiting Inventory System.");
                        break;
                    case INVALID_COMMAND_MARKER: // Error already handled in readCommand
                        break; // Continue loop after error message
                    default:
                        System.err.println("Invalid command code entered: " + cmd);
                        errorCode = ERROR_INVALID_INPUT;
                        printError();
                        break;
                }

                // Log transaction if a valid command (not exit or invalid marker) was processed
                if (cmd != EXIT_COMMAND && cmd != INVALID_COMMAND_MARKER && errorCode == SUCCESS) {
                    transactionCount++;
                    try {
                        LoggingService.logTransaction(cmd, transactionCount);
                    } catch (Exception e) {
                        // Handle potential exceptions from the logging service
                        System.err.println("Error logging transaction: " + e.getMessage());
                        errorCode = ERROR_SYSTEM; // Set a general system error
                        // Optionally call printError() here or let the final check handle it
                    }
                }
                // Reset error code for the next iteration unless it was a fatal error
                // For this simple model, we reset it if it was just an input error
                 if (errorCode == ERROR_INVALID_INPUT) {
                     errorCode = SUCCESS; // Allow user to try again
                 }
            }
        } // Scanner is automatically closed here

        // 3. Cleanup/Termination
        if (errorCode != SUCCESS && errorCode != ERROR_INVALID_INPUT) { // Don't reprint input errors on exit
            System.err.println("Exiting with errors.");
            printError(); // Print final error status if not SUCCESS
        }

        // Illustrative Bit Manipulation Example
        if (!inventory.isEmpty()) { // Check if inventory has items before proceeding
            System.out.println("\n--- Bit Manipulation Example ---");
            // Fetch or create a customer record for demonstration
            // Using a fixed ID for simplicity in this example
            currentCustomer = CustomerService.fetchCustomerData(101);

            if (currentCustomer != null) {
                System.out.println("Initial Customer State: " + currentCustomer);

                // Set isActive and taxExempt flags using bitwise OR
                int flags = currentCustomer.getFlags();
                flags |= CustomerRecord.FLAG_IS_ACTIVE;
                flags |= CustomerRecord.FLAG_TAX_EXEMPT;
                currentCustomer.setFlags(flags);
                System.out.println("After setting ACTIVE and TAX_EXEMPT: " + currentCustomer);
                System.out.println("Is Active? " + currentCustomer.isFlagSet(CustomerRecord.FLAG_IS_ACTIVE));
                System.out.println("Is Tax Exempt? " + currentCustomer.isFlagSet(CustomerRecord.FLAG_TAX_EXEMPT));

                // Clear the reserved flag using bitwise AND and NOT
                // Assume it might have been set elsewhere (or set it first for demo)
                // currentCustomer.setFlags(currentCustomer.getFlags() | CustomerRecord.FLAG_RESERVED); // Optional: set it first
                flags = currentCustomer.getFlags();
                flags &= ~CustomerRecord.FLAG_RESERVED; // Clear the reserved bit
                currentCustomer.setFlags(flags);
                System.out.println("After clearing RESERVED: " + currentCustomer);
                System.out.println("Is Reserved? " + currentCustomer.isFlagSet(CustomerRecord.FLAG_RESERVED));

            } else {
                System.err.println("Could not retrieve customer data for bit manipulation example.");
                // Optionally set an error code if this is critical
                // errorCode = ERROR_SYSTEM;
            }
            System.out.println("--- End Bit Manipulation Example ---");
        } else {
             System.out.println("\nSkipping bit manipulation example (inventory is empty).");
        }


        // Exit with the final status code
        System.exit(errorCode); // Use the final errorCode for exit status
    }

    // --- Internal Helper Methods ---

    /**
     * Populates the inventory list with initial sample data.
     * Updates the inventoryCount accordingly.
     */
    private static void initializeInventory() {
        System.out.println("Initializing inventory with sample data...");
        inventory.add(new ItemRecord(1, "Laptop", 10, 1200.50));
        inventory.add(new ItemRecord(2, "Mouse", 50, 25.00));
        inventory.add(new ItemRecord(3, "Keyboard", 30, 75.75));
        inventoryCount = inventory.size(); // Update count
        System.out.println("Initialization complete. " + inventoryCount + " items loaded.");
    }

    /**
     * Displays the main menu options to the console (System.out).
     */
    private static void displayMenu() {
        System.out.println("\n--- Inventory System Menu ---");
        System.out.println("1. Add Item");
        System.out.println("2. Delete Item");
        System.out.println("3. Update Item");
        System.out.println("4. Query Item");
        System.out.println("5. Generate Report");
        System.out.println("6. Exit");
        System.out.print("Enter command (1-6): ");
    }

    /**
     * Reads an integer command from the user via the provided Scanner.
     * Includes basic input validation for non-integer input.
     *
     * @param scanner The Scanner object to read input from.
     * @return The integer command entered by the user, or INVALID_COMMAND_MARKER if input is invalid.
     */
    private static int readCommand(Scanner scanner) {
        try {
            int command = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character left by nextInt()
            return command;
        } catch (InputMismatchException e) {
            System.err.println("Invalid input: Please enter a number.");
            scanner.nextLine(); // Consume the invalid input
            errorCode = ERROR_INVALID_INPUT;
            printError(); // Print error immediately
            return INVALID_COMMAND_MARKER; // Return a marker for invalid input
        }
    }

    /**
     * Handles adding a new item to the inventory.
     * (Placeholder implementation - details TBD).
     *
     * @param scanner The Scanner object for potential further input.
     */
    private static void addItem(Scanner scanner) {
        System.out.println("[Action] Add Item selected (Not fully implemented).");
        // TODO: Implement logic to get item details from user and add to 'inventory'
        // Remember to check against MAX_ITEMS if it's a hard limit
        // Update inventoryCount if successful
        if (inventoryCount >= MAX_ITEMS && MAX_ITEMS > 0) {
             System.err.println("Cannot add item: Inventory is full (Max: " + MAX_ITEMS + ").");
             errorCode = ERROR_SYSTEM; // Or a more specific error code
             printError();
        } else {
            // Example: Add a dummy item for now
            int newItemId = inventory.isEmpty() ? 1 : inventory.get(inventory.size() - 1).itemId + 1;
            inventory.add(new ItemRecord(newItemId, "New Item", 1, 10.0));
            inventoryCount = inventory.size(); // Update count
            System.out.println("Dummy item added. Current count: " + inventoryCount);
            errorCode = SUCCESS; // Reset error code on successful action (placeholder)
        }
    }

    /**
     * Handles deleting an item from the inventory.
     * (Placeholder implementation - details TBD).
     *
     * @param scanner The Scanner object for potential further input (e.g., item ID).
     */
    private static void deleteItem(Scanner scanner) {
        System.out.println("[Action] Delete Item selected (Not fully implemented).");
        // TODO: Implement logic to get item ID from user and remove from 'inventory'
        // Remember to update inventoryCount if successful
        errorCode = SUCCESS; // Reset error code on successful action (placeholder)
    }

    /**
     * Handles updating an existing item in the inventory.
     * (Placeholder implementation - details TBD).
     *
     * @param scanner The Scanner object for potential further input (e.g., item ID, new data).
     */
    private static void updateItem(Scanner scanner) {
        System.out.println("[Action] Update Item selected (Not fully implemented).");
        // TODO: Implement logic to get item ID and new details, then update in 'inventory'
        errorCode = SUCCESS; // Reset error code on successful action (placeholder)
    }

    /**
     * Handles querying details of an inventory item.
     * (Placeholder implementation - details TBD).
     *
     * @param scanner The Scanner object for potential further input (e.g., item ID or name).
     */
    private static void queryItem(Scanner scanner) {
        System.out.println("[Action] Query Item selected (Not fully implemented).");
        // TODO: Implement logic to get search criteria and display matching item(s) from 'inventory'
        if (!inventory.isEmpty()) {
            System.out.println("Example Query: Displaying first item:");
            System.out.println(inventory.get(0));
        } else {
            System.out.println("Inventory is empty.");
        }
        errorCode = SUCCESS; // Reset error code on successful action (placeholder)
    }

    /**
     * Handles generating and displaying an inventory report.
     * (Placeholder implementation - details TBD).
     */
    private static void generateReport() {
        System.out.println("[Action] Generate Report selected.");
        System.out.println("--- Inventory Report ---");
        if (inventory.isEmpty()) {
            System.out.println("Inventory is currently empty.");
        } else {
            System.out.println("Total items: " + inventoryCount); // Use inventoryCount
            // Or System.out.println("Total items: " + inventory.size());
            for (ItemRecord item : inventory) {
                System.out.println(item); // Uses ItemRecord.toString()
            }
        }
        System.out.println("--- End of Report ---");
        errorCode = SUCCESS; // Reset error code on successful action
    }

    /**
     * Prints an error message to System.err based on the current value
     * of the static `errorCode` field, using the `ERROR_MESSAGES` array.
     */
    private static void printError() {
        if (errorCode >= 0 && errorCode < ERROR_MESSAGES.length) {
            System.err.println(ERROR_MESSAGES[errorCode] + " (Code: " + errorCode + ")");
        } else {
            System.err.println("An unknown error occurred. (Code: " + errorCode + ")");
        }
        // Note: We don't reset errorCode here, the calling logic decides when to reset.
    }
}