 Code
/**
 * This Markdown block contains the Java code implementation corresponding to the
 * previously generated Java Technical Specification for the InventorySystem.
 */

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

// --- Data Class for Inventory Items ---

/**
 * Represents a single item in the inventory.
 * Corresponds to the TAL STRUCT item_record.
 * Uses BigDecimal for precise price representation.
 */
class ItemRecord {
    private int itemId;
    private String itemName;
    private BigDecimal itemPrice;
    private int quantityOnHand;
    private int reorderLevel;
    private int supplierId;
    private LocalDate lastUpdated; // Using LocalDate for better date handling

    /**
     * Constructor for ItemRecord.
     *
     * @param itemId         Unique identifier for the item.
     * @param itemName       Name of the item.
     * @param itemPrice      Price of the item.
     * @param quantityOnHand Current stock level.
     * @param reorderLevel   Minimum stock level before reordering.
     * @param supplierId     Identifier of the item's supplier.
     * @param lastUpdated    Date the record was last updated.
     * @throws NullPointerException if itemName or itemPrice or lastUpdated is null.
     * @throws IllegalArgumentException if itemId, quantityOnHand, reorderLevel, or supplierId are negative.
     */
    public ItemRecord(int itemId, String itemName, BigDecimal itemPrice, int quantityOnHand, int reorderLevel, int supplierId, LocalDate lastUpdated) {
        if (itemName == null || itemPrice == null || lastUpdated == null) {
            throw new NullPointerException("Item Name, Price, and Last Updated Date cannot be null.");
        }
        if (itemId < 0 || quantityOnHand < 0 || reorderLevel < 0 || supplierId < 0) {
            throw new IllegalArgumentException("IDs and Quantities cannot be negative.");
        }
        this.itemId = itemId;
        this.itemName = Objects.requireNonNull(itemName, "Item Name cannot be null.");
        this.itemPrice = Objects.requireNonNull(itemPrice, "Item Price cannot be null.");
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
        this.supplierId = supplierId;
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "Last Updated Date cannot be null.");
    }

    // --- Getters ---
    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getItemPrice() { return itemPrice; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public int getReorderLevel() { return reorderLevel; }
    public int getSupplierId() { return supplierId; }
    public LocalDate getLastUpdated() { return lastUpdated; }

    // --- Setters (Example - implement as needed for update functionality) ---
    public void setItemName(String itemName) { this.itemName = Objects.requireNonNull(itemName); }
    public void setItemPrice(BigDecimal itemPrice) { this.itemPrice = Objects.requireNonNull(itemPrice); }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand >= 0 ? quantityOnHand : 0; } // Basic validation
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel >= 0 ? reorderLevel : 0; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId >= 0 ? supplierId : 0; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = Objects.requireNonNull(lastUpdated); }


    @Override
    public String toString() {
        return String.format("ItemRecord{itemId=%d, itemName='%s', itemPrice=%.2f, quantityOnHand=%d, reorderLevel=%d, supplierId=%d, lastUpdated=%s}",
                itemId, itemName, itemPrice, quantityOnHand, reorderLevel, supplierId, lastUpdated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemRecord that = (ItemRecord) o;
        return itemId == that.itemId; // Equality based on ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
}

// --- Data Class for Customer Records ---

/**
 * Represents a customer record.
 * Corresponds to the TAL STRUCT customer_record.
 * Uses boolean flags instead of TAL bit fields.
 */
class CustomerRecord {
    private int customerId;
    private String customerName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private BigDecimal accountBalance;
    private boolean isActive;
    private boolean hasCredit;
    private boolean taxExempt;

    // Constructor
    public CustomerRecord(int customerId, String customerName, String addressLine1, String addressLine2, String city, String state, String zipCode, BigDecimal accountBalance, boolean isActive, boolean hasCredit, boolean taxExempt) {
        this.customerId = customerId;
        this.customerName = Objects.requireNonNull(customerName);
        this.addressLine1 = addressLine1; // Allow null/empty
        this.addressLine2 = addressLine2; // Allow null/empty
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.accountBalance = Objects.requireNonNull(accountBalance);
        this.isActive = isActive;
        this.hasCredit = hasCredit;
        this.taxExempt = taxExempt;
    }

    // --- Getters ---
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public BigDecimal getAccountBalance() { return accountBalance; }
    public boolean isActive() { return isActive; }
    public boolean hasCredit() { return hasCredit; }
    public boolean isTaxExempt() { return taxExempt; }

     // --- Setters (Example - implement as needed) ---
    public void setActive(boolean active) { isActive = active; }
    public void setHasCredit(boolean hasCredit) { this.hasCredit = hasCredit; }
    public void setTaxExempt(boolean taxExempt) { this.taxExempt = taxExempt; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = Objects.requireNonNull(accountBalance); }
    // Add other setters as needed

    @Override
    public String toString() {
        return "CustomerRecord{" +
                "customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                // ... other fields ...
                ", isActive=" + isActive +
                ", hasCredit=" + hasCredit +
                ", taxExempt=" + taxExempt +
                '}';
    }
     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerRecord that = (CustomerRecord) o;
        return customerId == that.customerId; // Equality based on ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
}

// --- Transaction Logging Interface and Implementation ---

/**
 * Interface defining the contract for transaction logging.
 * Corresponds to the TAL EXTERNAL PROC log_transaction.
 */
interface TransactionLogger {
    /**
     * Logs a transaction.
     * @param commandCode The numeric code of the command executed.
     * @param commandName The name of the command executed.
     * @param transactionId A unique identifier for this transaction instance.
     */
    void log(int commandCode, String commandName, int transactionId);
}

/**
 * A simple console-based implementation of TransactionLogger.
 */
class ConsoleTransactionLogger implements TransactionLogger {
    @Override
    public void log(int commandCode, String commandName, int transactionId) {
        System.out.printf("[LOG] Transaction #%d: Command %d (%s) executed.%n",
                          transactionId, commandCode, commandName);
    }
}

// --- Main Application Class ---

/**
 * Main class for the Inventory System application.
 * Contains the main loop, menu handling, command dispatching,
 * and static variables representing the application's state.
 * Corresponds to the TAL program inventory_system and MAIN procedure main_proc.
 */
public class InventorySystem {

    // --- Constants (equivalent to TAL LITERALs) ---
    public static final int MAX_ITEMS = 1000; // Example limit
    public static final int SUCCESS = 0;
    // Note: File Not Found error might be handled differently (e.g., Exceptions) if file I/O is added.
    // public static final int ERROR_FILE_NOT_FOUND = 1;
    public static final int ERROR_INVALID_INPUT = 2;
    public static final int ERROR_SYSTEM = 3; // Generic error
    public static final int ERROR_ITEM_NOT_FOUND = 4; // Added for specific cases
    public static final int ERROR_ITEM_EXISTS = 5;   // Added for specific cases

    // --- Static State Variables (equivalent to TAL Globals) ---
    private static int errorCode = SUCCESS; // Holds last operation status
    private static int transactionCount = 0;

    // Using a Map for inventory for efficient lookup by ID
    // private static List<ItemRecord> inventory = new ArrayList<>(); // Original List approach
    private static Map<Integer, ItemRecord> inventory = new HashMap<>();

    // Command definitions
    private static final Map<Integer, String> COMMANDS = new LinkedHashMap<>(); // LinkedHashMap preserves insertion order for menu display
    static {
        COMMANDS.put(1, "ADD");
        COMMANDS.put(2, "DELETE");
        COMMANDS.put(3, "UPDATE");
        COMMANDS.put(4, "QUERY");
        COMMANDS.put(5, "REPORT");
        COMMANDS.put(6, "EXIT");
    }

    // Error messages map
    private static final Map<Integer, String> ERROR_MESSAGES = new HashMap<>();
    static {
        ERROR_MESSAGES.put(SUCCESS, "Operation successful.");
        ERROR_MESSAGES.put(ERROR_INVALID_INPUT, "Error: Invalid input provided.");
        ERROR_MESSAGES.put(ERROR_SYSTEM, "Error: An unexpected system error occurred.");
        ERROR_MESSAGES.put(ERROR_ITEM_NOT_FOUND, "Error: Item ID not found in inventory.");
        ERROR_MESSAGES.put(ERROR_ITEM_EXISTS, "Error: Item ID already exists in inventory.");
        // Add other specific error messages as needed
    }

    // --- Dependencies ---
    private static final TransactionLogger logger = new ConsoleTransactionLogger(); // Using console logger
    private static final Scanner scanner = new Scanner(System.in); // Input scanner

    /**
     * Main entry point of the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        System.out.println("Inventory System Initializing...");
        initializeInventory();
        System.out.println("Initialization Complete.");

        boolean running = true;
        while (running) {
            displayMenu();
            int command = readCommand();
            errorCode = SUCCESS; // Reset error code before processing command

            if (command == -1) { // Handle invalid non-numeric input from readCommand
                errorCode = ERROR_INVALID_INPUT;
                printError();
                continue; // Skip rest of the loop and show menu again
            }

            String commandName = COMMANDS.getOrDefault(command, "INVALID");

            switch (command) {
                case 1: // ADD
                    addItem();
                    break;
                case 2: // DELETE
                    deleteItem();
                    break;
                case 3: // UPDATE
                    updateItem();
                    break;
                case 4: // QUERY
                    queryItem();
                    break;
                case 5: // REPORT
                    generateReport();
                    break;
                case 6: // EXIT
                    running = false;
                    System.out.println("Exiting Inventory System.");
                    break;
                default:
                    System.err.println("Error: Unknown command code entered.");
                    errorCode = ERROR_INVALID_INPUT; // Should ideally be caught by readCommand validation
                    break;
            }

            // Log transaction if not exiting and command was valid (or handled)
            if (running && command >= 1 && command <= 5) {
                 // Only log successful or handled operations for simplicity here
                 // More robust logging might log errors too.
                if (errorCode == SUCCESS) { // Example: Only log successful ops
                    transactionCount++;
                    logger.log(command, commandName, transactionCount);
                }
                // Always print error message if an error occurred during processing
                if (errorCode != SUCCESS) {
                    printError();
                }
            } else if (running && errorCode != SUCCESS) {
                 // Print error for invalid command case (caught by default in switch or readCommand)
                 printError();
            }
             System.out.println(); // Add a blank line for readability
        }

        // Example of CustomerRecord manipulation (like TAL bit manipulation demo)
        demonstrateCustomerRecordUpdate();

        // Cleanup
        scanner.close();
        System.out.println("Scanner closed. Application finished.");
    }

    /**
     * Initializes the inventory with sample data.
     * Corresponds to TAL initialize_inventory.
     */
    private static void initializeInventory() {
        inventory.clear(); // Clear previous data if any
        try {
            // Using Map: key is itemId
            inventory.put(1001, new ItemRecord(1001, "Widget A", new BigDecimal("19.99"), 150, 25, 101, LocalDate.parse("2024-01-10")));
            inventory.put(1002, new ItemRecord(1002, "Widget B", new BigDecimal("29.99"), 75, 15, 102, LocalDate.parse("2024-01-11")));
            inventory.put(1003, new ItemRecord(1003, "Widget C", new BigDecimal("39.99"), 50, 10, 101, LocalDate.parse("2024-01-12")));
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
            System.err.println("FATAL: Error initializing sample inventory data: " + e.getMessage());
            // In a real app, might terminate or try loading from a file.
            errorCode = ERROR_SYSTEM;
            printError();
        }
    }

    /**
     * Displays the main menu options to the console.
     * Corresponds to TAL display_menu.
     */
    private static void displayMenu() {
        System.out.println("==== Inventory System Menu ====");
        for (Map.Entry<Integer, String> entry : COMMANDS.entrySet()) {
            System.out.printf("  %d. %s%n", entry.getKey(), entry.getValue());
        }
        System.out.print("Enter command: ");
    }

    /**
     * Reads and validates an integer command from the console.
     * Includes basic validation for integer format and range.
     *
     * @return The integer command code if valid, or -1 if the input was not a valid integer.
     *         Sets errorCode if input is valid integer but outside command range.
     */
    private static int readCommand() {
        int command = -1;
        try {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                errorCode = ERROR_INVALID_INPUT;
                return -1; // Treat empty input as invalid
            }
            command = Integer.parseInt(line);
            if (!COMMANDS.containsKey(command)) {
                 System.err.println("Error: Command code must be between 1 and " + COMMANDS.size());
                 errorCode = ERROR_INVALID_INPUT;
                 // Return the invalid command code itself, main loop will handle error message
                 // Or return -1 to force re-prompt implicitly via main loop logic
                 return command; // Let main loop handle unknown command
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Please enter a valid number.");
            errorCode = ERROR_INVALID_INPUT; // Set error code
            return -1; // Indicate non-numeric input failure
        }
        return command;
    }


    /**
     * Prints the error message corresponding to the current static errorCode.
     * Corresponds to TAL print_error.
     */
    private static void printError() {
        String message = ERROR_MESSAGES.getOrDefault(errorCode, "Unknown error code: " + errorCode);
        System.err.println(message); // Print errors to stderr
    }

    // --- Stubbed Methods for Core Functionality ---

    /**
     * STUB: Adds a new item to the inventory.
     * Prompts user for details, validates, and adds to the inventory map.
     * Sets errorCode on failure (e.g., invalid input, item exists).
     */
    private static void addItem() {
        System.out.println("--- Add New Item ---");
        try {
            System.out.print("Enter Item ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (inventory.containsKey(id)) {
                errorCode = ERROR_ITEM_EXISTS;
                return; // Exit early
            }
            if (id < 0) throw new IllegalArgumentException("ID cannot be negative.");

            System.out.print("Enter Item Name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty.");


            System.out.print("Enter Price (e.g., 19.99): ");
            BigDecimal price = new BigDecimal(scanner.nextLine().trim());
             if (price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price cannot be negative.");


            System.out.print("Enter Quantity on Hand: ");
            int qty = Integer.parseInt(scanner.nextLine().trim());
            if (qty < 0) throw new IllegalArgumentException("Quantity cannot be negative.");

            System.out.print("Enter Reorder Level: ");
            int reorder = Integer.parseInt(scanner.nextLine().trim());
             if (reorder < 0) throw new IllegalArgumentException("Reorder level cannot be negative.");

            System.out.print("Enter Supplier ID: ");
            int supplier = Integer.parseInt(scanner.nextLine().trim());
             if (supplier < 0) throw new IllegalArgumentException("Supplier ID cannot be negative.");

            // Use current date for lastUpdated
            LocalDate updatedDate = LocalDate.now();

            ItemRecord newItem = new ItemRecord(id, name, price, qty, reorder, supplier, updatedDate);
            inventory.put(id, newItem);
            System.out.println("Item added successfully.");
            errorCode = SUCCESS;

        } catch (NumberFormatException e) {
            System.err.println("Invalid number format entered.");
            errorCode = ERROR_INVALID_INPUT;
        } catch (IllegalArgumentException | NullPointerException e) {
             System.err.println("Invalid input: " + e.getMessage());
             errorCode = ERROR_INVALID_INPUT;
        }
        // No return needed here, errorCode is set
    }

    /**
     * STUB: Deletes an item from the inventory based on user-provided ID.
     * Sets errorCode if item not found or input is invalid.
     */
    private static void deleteItem() {
        System.out.println("--- Delete Item ---");
         try {
            System.out.print("Enter Item ID to delete: ");
            int id = Integer.parseInt(scanner.nextLine().trim());

            if (inventory.containsKey(id)) {
                inventory.remove(id);
                System.out.println("Item ID " + id + " deleted successfully.");
                errorCode = SUCCESS;
            } else {
                errorCode = ERROR_ITEM_NOT_FOUND;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format for ID.");
            errorCode = ERROR_INVALID_INPUT;
        }
    }

    /**
     * STUB: Updates an existing item in the inventory.
     * Prompts user for ID and fields to update.
     * Sets errorCode on failure (e.g., not found, invalid input).
     */
    private static void updateItem() {
        System.out.println("--- Update Item ---");
         try {
            System.out.print("Enter Item ID to update: ");
            int id = Integer.parseInt(scanner.nextLine().trim());

            ItemRecord item = inventory.get(id);
            if (item == null) {
                errorCode = ERROR_ITEM_NOT_FOUND;
                return;
            }

            System.out.println("Current details: " + item);
            System.out.println("Enter new values (leave blank to keep current):");

            System.out.printf("Enter new Item Name [%s]: ", item.getItemName());
            String nameStr = scanner.nextLine().trim();
            if (!nameStr.isEmpty()) item.setItemName(nameStr);


            System.out.printf("Enter new Price [%s]: ", item.getItemPrice().toPlainString());
            String priceStr = scanner.nextLine().trim();
            if (!priceStr.isEmpty()) item.setItemPrice(new BigDecimal(priceStr));


            System.out.printf("Enter new Quantity on Hand [%d]: ", item.getQuantityOnHand());
            String qtyStr = scanner.nextLine().trim();
             if (!qtyStr.isEmpty()) item.setQuantityOnHand(Integer.parseInt(qtyStr));


            System.out.printf("Enter new Reorder Level [%d]: ", item.getReorderLevel());
            String reorderStr = scanner.nextLine().trim();
             if (!reorderStr.isEmpty()) item.setReorderLevel(Integer.parseInt(reorderStr));

            // Set last updated date to now
            item.setLastUpdated(LocalDate.now());

            System.out.println("Item ID " + id + " updated successfully.");
            errorCode = SUCCESS;

        } catch (NumberFormatException e) {
            System.err.println("Invalid number format entered.");
            errorCode = ERROR_INVALID_INPUT;
        } catch (IllegalArgumentException | NullPointerException e) {
             System.err.println("Invalid input: " + e.getMessage());
             errorCode = ERROR_INVALID_INPUT;
        }
    }

    /**
     * STUB: Queries and displays details for a specific item ID.
     * Sets errorCode if item not found or input is invalid.
     */
    private static void queryItem() {
        System.out.println("--- Query Item ---");
        try {
            System.out.print("Enter Item ID to query: ");
            int id = Integer.parseInt(scanner.nextLine().trim());

            ItemRecord item = inventory.get(id);
            if (item != null) {
                System.out.println("Item Details:");
                System.out.println(item); // Uses toString() method
                errorCode = SUCCESS;
            } else {
                errorCode = ERROR_ITEM_NOT_FOUND;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format for ID.");
            errorCode = ERROR_INVALID_INPUT;
        }
    }

    /**
     * STUB: Generates and prints a report of all items in the inventory.
     */
    private static void generateReport() {
        System.out.println("--- Inventory Report ---");
        if (inventory.isEmpty()) {
            System.out.println("Inventory is empty.");
        } else {
            System.out.println("------------------------------------------------------------------------------------");
            System.out.printf("%-8s %-20s %-10s %-10s %-10s %-10s %-12s%n",
                              "Item ID", "Name", "Price", "Quantity", "Reorder", "Supplier", "Last Updated");
            System.out.println("------------------------------------------------------------------------------------");
            // Sort items by ID for consistent report order
            List<ItemRecord> sortedItems = new ArrayList<>(inventory.values());
            sortedItems.sort(Comparator.comparingInt(ItemRecord::getItemId));

            for (ItemRecord item : sortedItems) {
                System.out.printf("%-8d %-20.20s %-10.2f %-10d %-10d %-10d %-12s%n",
                                  item.getItemId(),
                                  item.getItemName(),
                                  item.getItemPrice(),
                                  item.getQuantityOnHand(),
                                  item.getReorderLevel(),
                                  item.getSupplierId(),
                                  item.getLastUpdated());
            }
             System.out.println("------------------------------------------------------------------------------------");
        }
        errorCode = SUCCESS; // Report generation is generally successful if it runs
    }

     /**
     * Demonstrates updating boolean flags on a CustomerRecord object,
     * similar to the bit manipulation example in the TAL code.
     */
    private static void demonstrateCustomerRecordUpdate() {
        System.out.println("\n--- Demonstrating Customer Record Update (Boolean Flags) ---");
         // Create a sample customer
        CustomerRecord customer = new CustomerRecord(
            9001, "Demo Customer", "123 Main St", "", "Anytown", "CA", "90210",
            new BigDecimal("100.00"), false, false, false); // Start with flags off

        System.out.println("Initial Customer State: " + customer);

        // Equivalent of setting TAL bit fields:
        customer.setActive(true);
        customer.setTaxExempt(true);
        // customer.setHasCredit(false); // Example: Ensure credit is off

        System.out.println("Updated Customer State: Active=" + customer.isActive() +
                           ", HasCredit=" + customer.hasCredit() +
                           ", TaxExempt=" + customer.isTaxExempt());
        System.out.println("---------------------------------------------------------");
    }
}