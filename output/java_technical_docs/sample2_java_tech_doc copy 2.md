 Technical Documentation
# Technical Documentation: Java Program - InventorySystem (Equivalent to TAL inventory_system)

## 1. Overview

**Program Name:** `InventorySystem` (Conceptual Java equivalent)

**Language:** Java (Targeting Java 8 or later for features like Streams, though basic implementation can use earlier versions)

**Platform:** Java Virtual Machine (JVM)

**Purpose:** This document specifies a Java console application that mirrors the functionality of the TAL `inventory_system` program. It serves as a basic command-line inventory management system, demonstrating core Java concepts like classes, objects, collections, methods, error handling, and input/output.

**Functionality:**
*   Initializes an in-memory inventory with sample data.
*   Presents a text-based menu of operations (ADD, DELETE, UPDATE, QUERY, REPORT, EXIT).
*   Reads user commands from the console.
*   Dispatches control to appropriate methods based on the command.
*   Simulates logging transactions via calls to a separate (potentially mock or interface-based) logging component.
*   Includes examples of object-oriented data representation and basic data manipulation.
*   Provides basic error handling for invalid input.

**Note:** This specification describes the structure and behavior. Core functionalities like detailed data validation, persistence (file I/O or database), and complex business logic within the command methods (`addItem`, `deleteItem`, etc.) require concrete implementation based on these specifications.

## 2. Class Structure

The application can be structured using several classes:

1.  **`InventorySystem` (Main Class):**
    *   Contains the `public static void main(String[] args)` method, serving as the application entry point.
    *   Holds static fields representing global state (like the inventory list, transaction counter, error codes/messages).
    *   Contains static methods for the main loop, menu display, command processing, and potentially helper functions.
    *   Coordinates calls to other classes/components.

2.  **`ItemRecord` (Data Class):**
    *   Represents a single inventory item. Replaces the TAL `STRUCT item_record`.
    *   Contains fields for `itemId` (int), `itemName` (String), `itemPrice` (BigDecimal), `quantityOnHand` (int), `reorderLevel` (int), `supplierId` (int), `lastUpdated` (String or preferably `java.time.LocalDate`).
    *   Includes constructors, getters, and potentially setters or a `toString()` method for display.

3.  **`CustomerRecord` (Data Class):**
    *   Represents a customer record. Replaces the TAL `STRUCT customer_record`.
    *   Contains fields for customer attributes like `customerId` (int), `customerName` (String), address fields (String), `accountBalance` (BigDecimal).
    *   Boolean flags (`isActive`, `hasCredit`, `taxExempt`) replace the TAL bit fields.
    *   Includes constructors, getters, setters, `toString()`. (Note: Less utilized in the core inventory loop based on the TAL example, primarily used for a specific bit manipulation demo).

4.  **`TransactionLogger` (Interface/Class):**
    *   Defines the contract or provides the implementation for logging transactions. Replaces the TAL `EXTERNAL PROC log_transaction`.
    *   Example method: `void log(int commandCode, int transactionId);`

5.  **`CustomerService` (Interface/Class):**
    *   Defines the contract or provides the implementation for fetching customer data. Replaces the TAL `EXTERNAL PROC fetch_customer_data`.
    *   Example method: `CustomerRecord fetchById(int customerId);` (Not used in the main loop of the example).

6.  **`ConsoleIO` (Utility Class - Optional):**
    *   Could encapsulate console input/output operations (reading commands, printing messages/menus) to keep the main class cleaner. Replaces implicit TAL I/O calls like `write_line`, `write`, `read_command`. Methods like `displayMenu(List<String> options)`, `prompt(String message)`, `readIntCommand()`.

## 3. Key Data Classes

### 3.1. `ItemRecord` Class

*   **Purpose:** Represents a single item in the inventory.
*   **Fields:**
    *   `private int itemId;`
    *   `private String itemName;`
    *   `private java.math.BigDecimal itemPrice;` // Use BigDecimal for accurate currency representation
    *   `private int quantityOnHand;`
    *   `private int reorderLevel;`
    *   `private int supplierId;`
    *   `private String lastUpdated;` // Or java.time.LocalDate for better date handling

### 3.2. `CustomerRecord` Class

*   **Purpose:** Represents a customer record.
*   **Fields:**
    *   `private int customerId;`
    *   `private String customerName;`
    *   `private String addressLine1;`
    *   `private String addressLine2;`
    *   `private String city;`
    *   `private String state;`
    *   `private String zipCode;`
    *   `private java.math.BigDecimal accountBalance;`
    *   `private boolean isActive;` // Replaces UNSIGNED(1) is_active
    *   `private boolean hasCredit;` // Replaces UNSIGNED(1) has_credit
    *   `private boolean taxExempt;` // Replaces UNSIGNED(1) tax_exempt
    *   // No equivalent for FILLER or reserved bits needed unless specific packing is required (rare in Java).

## 4. Key Constants and Static Fields (within `InventorySystem` class)

*   `public static final int MAX_ITEMS = 1000;`
*   `public static final int SUCCESS = 0;`
*   `public static final int ERROR_FILE_NOT_FOUND = 1;` // May be less relevant if not doing file I/O
*   `public static final int ERROR_INVALID_INPUT = 2;`
*   `public static final int ERROR_SYSTEM = 3;` // Generic system error
*   `private static int errorCode = SUCCESS;` // Holds last operation status
*   `private static final Map<Integer, String> ERROR_MESSAGES = Map.of( SUCCESS, "Success", ERROR_FILE_NOT_FOUND, "File not found", ERROR_INVALID_INPUT, "Invalid input", ERROR_SYSTEM, "System error" );` // Requires Java 9+, use static initializer block for older versions.
*   `private static int transactionCount = 0;`
*   `private static List<ItemRecord> inventory = new ArrayList<>(MAX_ITEMS);` // Use ArrayList for dynamic resizing up to MAX_ITEMS if needed, or manage size explicitly.
*   `// No direct equivalent for TAL pointers like error_ptr or buffer needed.`
*   `// Java uses references implicitly.`
*   `private static final int[] COMMAND_CODES = {1, 2, 3, 4, 5, 6};`
*   `private static final String[] COMMAND_NAMES = {"ADD", "DELETE", "UPDATE", "QUERY", "REPORT", "EXIT"};`
*   `// No direct equivalent for G[10] - would use System Properties, Env Vars, or Config Files in Java.`
*   `// No BLOCK equivalent - fields can be grouped logically or put in helper classes.`

## 5. External Dependencies / Interfaces (Conceptual)

*   **`TransactionLogger` Interface:**
    *   `void log(int commandCode, int transactionId);`
    *   An implementation (e.g., `ConsoleTransactionLogger`, `FileTransactionLogger`) would be instantiated or injected.
*   **`CustomerService` Interface:**
    *   `CustomerRecord fetchById(int customerId);`
    *   An implementation would be provided if customer fetching is needed.
*   **Console I/O:** Handled using standard Java `java.util.Scanner` for input and `System.out.print`/`println` for output, potentially wrapped in a `ConsoleIO` utility class.

## 6. Method Descriptions (within `InventorySystem` class)

### 6.1. `public static void main(String[] args)`

*   **Purpose:** Application entry point. Controls the main execution flow.
*   **Parameters:** `String[] args` - Command line arguments (not used in this example).
*   **Return Value:** `void`. Exits with `System.exit(0)` on success or non-zero on error if needed.
*   **Logic:**
    1.  Instantiate necessary components (e.g., `TransactionLogger`).
    2.  Call `initializeInventory()`.
    3.  Enter a `while` loop that continues until the user command is EXIT (e.g., 6).
    4.  Inside the loop:
        *   Call `displayMenu()`.
        *   Call a method to read and validate user command (e.g., `readCommand()`).
        *   Use a `switch` statement or `if-else if` chain to dispatch to appropriate methods based on the command:
            *   1: `addItem()` (stubbed)
            *   2: `deleteItem()` (stubbed)
            *   3: `updateItem()` (stubbed)
            *   4: `queryItem()` (stubbed)
            *   5: `generateReport()` (stubbed)
            *   6: Set loop termination flag.
            *   `default`: Set `errorCode = ERROR_INVALID_INPUT`, call `printError()`.
        *   If not exiting, increment `transactionCount` and call `transactionLogger.log(command, transactionCount)`.
    5.  Perform any final cleanup if necessary.
    6.  Optionally, demonstrate bit manipulation equivalent using boolean flags on a `CustomerRecord` object.
*   **Exceptions:** May handle `InputMismatchException` from scanner, or custom exceptions from business logic methods.

### 6.2. `private static void initializeInventory()`

*   **Purpose:** Populates the `inventory` list with sample `ItemRecord` objects.
*   **Parameters:** None.
*   **Return Value:** `void`.
*   **Logic:**
    1.  Clears the existing `inventory` list (optional).
    2.  Creates new `ItemRecord` objects with hardcoded data.
    3.  Adds these objects to the `inventory` list.
    4.  Updates `inventory_count` (if maintained separately from `inventory.size()`).
*   **Exceptions:** None expected.

### 6.3. `private static void displayMenu()`

*   **Purpose:** Displays the command menu to the console.
*   **Parameters:** None.
*   **Return Value:** `void`.
*   **Logic:**
    1.  Prints the menu title using `System.out.println()`.
    2.  Loops through the `COMMAND_CODES` and `COMMAND_NAMES` arrays.
    3.  Formats and prints each menu option (e.g., "1. ADD") using `System.out.println()`.
    4.  Prints the prompt "Enter command: " using `System.out.print()`.
*   **Exceptions:** None expected.

### 6.4. `private static int readCommand()` (Example - part of main or ConsoleIO)

*   **Purpose:** Reads an integer command from the console. Includes basic validation.
*   **Parameters:** None (uses a shared `Scanner` object).
*   **Return Value:** `int` - The validated command code, or potentially a special value (e.g., -1 or `ERROR_INVALID_INPUT`) if input is invalid.
*   **Logic:**
    1.  Use `Scanner.hasNextInt()` to check for valid integer input.
    2.  If valid, read using `Scanner.nextInt()`.
    3.  Consume the remaining newline using `Scanner.nextLine()`.
    4.  Check if the integer is within the valid command range (1-6).
    5.  Return the valid command or an error indicator.
    6.  If input is not an integer, consume the invalid input, print an error message, and return an error indicator.
*   **Exceptions:** Can handle `InputMismatchException` internally, or propagate it.

### 6.5. Stubbed Methods (Signatures and Purpose)

*   `private static void addItem()`: **Purpose:** Prompts user for new item details, creates an `ItemRecord`, adds it to `inventory`. **Needs implementation:** Input prompts, validation, adding to list.
*   `private static void deleteItem()`: **Purpose:** Prompts for item ID, finds and removes the corresponding `ItemRecord` from `inventory`. **Needs implementation:** Input prompt, search logic, removal logic.
*   `private static void updateItem()`: **Purpose:** Prompts for item ID and fields to update, finds the `ItemRecord`, modifies it. **Needs implementation:** Input prompts, search logic, update logic, validation.
*   `private static void queryItem()`: **Purpose:** Prompts for item ID, finds the `ItemRecord`, displays its details. **Needs implementation:** Input prompt, search logic, formatted output.
*   `private static void generateReport()`: **Purpose:** Iterates through `inventory` and prints a formatted report to the console. **Needs implementation:** Iteration, formatted output.
*   `private static void printError()`: **Purpose:** Prints the error message associated with the current `errorCode`. **Needs implementation:** Look up message in `ERROR_MESSAGES` map and print using `System.err.println()`.

## 7. Error Handling Strategy

*   Use `static final int` constants for error codes (`SUCCESS`, `ERROR_INVALID_INPUT`, etc.).
*   Maintain a `static int errorCode` field to hold the status of the last operation.
*   Use a `static final Map<Integer, String>` (`ERROR_MESSAGES`) to map codes to user-friendly messages.
*   The `printError()` method displays the message corresponding to `errorCode`.
*   Input validation should be performed in input reading methods (like `readCommand`) or at the beginning of processing methods (`addItem`, etc.), setting `errorCode` upon failure.
*   **Alternative/Recommended:** For more complex errors (e.g., item not found during delete/update, file errors if implemented), throwing custom checked or unchecked exceptions (e.g., `ItemNotFoundException extends Exception`, `InvalidCommandException extends RuntimeException`) is often preferred in Java. The main loop would then include `try-catch` blocks.

## 8. Build and Execution

1.  **Compilation:**
    *   Save the code into `.java` files (e.g., `InventorySystem.java`, `ItemRecord.java`).
    *   Compile using a Java Development Kit (JDK):