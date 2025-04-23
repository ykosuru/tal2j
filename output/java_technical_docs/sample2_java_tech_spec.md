Java Technical Specifications
# Java Technical Specifications: InventorySystem

## 1. Class Description

`InventorySystem` is a Java command-line application designed to manage a simple inventory. It provides a text-based interface for users to perform basic inventory operations such as adding, deleting, updating, querying items, and generating reports. The application utilizes static fields (simulating global variables), custom classes (representing data structures like `ItemRecord` and `CustomerRecord`), collections (like `ArrayList` for dynamic arrays), and method calls to manage inventory data and interact with the user and potentially other system components (like a logging service). It includes basic error handling mechanisms and demonstrates features like bitwise operations within data objects.

## 2. Input Parameters

The main entry point method `public static void main(String[] args)` accepts command-line arguments, although these are not used by the core application logic described. Program control and data input are handled through:

*   **User Interaction:** The program prompts the user to enter commands via standard input (`System.in`), typically read using a `java.util.Scanner` object within the `readCommand` method.
*   **Static Fields:** May potentially read system-level information via Java APIs (e.g., `System.currentTimeMillis()` or `java.time` package instead of `system_time`, system properties instead of `'G'[10]`).
*   **External Data:** May interact with external data sources via methods like `CustomerService.fetchCustomerData()` (though not called in the main loop shown).

## 3. Return Value

*   **Type:** `void` (for the `main` method).
*   **Description:** The `main` method itself does not return a value. Program termination status is communicated to the operating system via `System.exit(int statusCode)`.
    *   `SUCCESS` (defined as `0`): Indicates successful execution and normal termination.
    *   Other integer values (corresponding to defined error constants like `ERROR_INVALID_INPUT`) can be returned via `System.exit()` based on the final value of the `errorCode` field.

## 4. Core Logic/Processing Steps

1.  **Initialization:**
    *   Initializes static fields:
        *   `inventory`: An `ArrayList<ItemRecord>` is instantiated.
        *   `inventoryCount`: Initialized to `0` (or managed implicitly by `inventory.size()`).
        *   `transactionCount`: Initialized to `0`.
        *   `errorCode`: Initialized to `SUCCESS` (`0`).
        *   `buffer`: A `StringBuilder` could be initialized if needed for complex string manipulations, though simple `System.out.print`/`println` might suffice.
    *   Calls the static method `initializeInventory()` to populate the `inventory` list with sample data and update `inventoryCount` (or rely on `inventory.size()`).
    *   Initializes a local variable `cmd` (e.g., `int cmd = 0;`).
    *   Initializes input mechanism (e.g., `Scanner scanner = new Scanner(System.in);`).
2.  **Main Command Loop:**
    *   Enters a `while` loop that continues as long as the user command (`cmd`) is not `EXIT_COMMAND` (e.g., `6`).
    *   Calls `displayMenu()` to show available commands to the user (using `System.out.println`).
    *   Calls `readCommand(scanner)` to get the user's command choice (handling potential `InputMismatchException`) and store it in `cmd`.
    *   Uses a `switch` statement to dispatch control based on the value of `cmd`:
        *   `case 1`: Calls `addItem(scanner)`.
        *   `case 2`: Calls `deleteItem(scanner)`.
        *   `case 3`: Calls `updateItem(scanner)`.
        *   `case 4`: Calls `queryItem(scanner)`.
        *   `case 5`: Calls `generateReport()`.
        *   `case 6`: Sets loop termination condition (e.g., break or let `while` condition handle it).
        *   `default`: Sets static field `errorCode` to `ERROR_INVALID_INPUT` and calls `printError()`.
    *   If the command was not `EXIT_COMMAND`:
        *   Increments the static `transactionCount` field.
        *   Calls an external logging method (e.g., `LoggingService.logTransaction(cmd, transactionCount)`).
3.  **Cleanup/Termination:**
    *   Closes resources (e.g., `scanner.close()` - preferably using try-with-resources).
    *   Checks the final value of the static `errorCode` field. If non-zero, calls `printError()` one last time.
    *   **Illustrative Bit Manipulation:** If `inventory.size() > 0`:
        *   Obtains a reference to a `CustomerRecord` object (e.g., `CustomerRecord currentCustomer = CustomerService.fetchCustomerData(someId);` or potentially retrieves one related to an inventory item). *Note: Requires proper object instantiation or retrieval to avoid `NullPointerException`.*
        *   Sets the `isActive` and `taxExempt` flags within the `currentCustomer` object using bitwise OR operations (e.g., `currentCustomer.setFlags(currentCustomer.getFlags() | CustomerRecord.FLAG_IS_ACTIVE | CustomerRecord.FLAG_TAX_EXEMPT);`).
        *   Clears the `reserved` flag using bitwise AND and NOT operations (e.g., `currentCustomer.setFlags(currentCustomer.getFlags() & ~CustomerRecord.FLAG_RESERVED);`).
    *   Exits the application with the appropriate status code: `System.exit(errorCode == SUCCESS ? SUCCESS : errorCode);` (or just `System.exit(SUCCESS)` if only normal termination is handled here).

## 5. Dependencies

### 5.1. Internal Methods (private static):

*   `initializeInventory()`: Populates the `inventory` list with initial data.
*   `displayMenu()`: Displays the command menu options to `System.out`.
*   `readCommand(Scanner scanner)`: Reads and returns an integer command from the user via the provided `Scanner`. Includes basic input validation/error handling.
*   `addItem(Scanner scanner)`: Handles adding a new inventory item (details TBD).
*   `deleteItem(Scanner scanner)`: Handles deleting an inventory item (details TBD).
*   `updateItem(Scanner scanner)`: Handles updating an inventory item (details TBD).
*   `queryItem(Scanner scanner)`: Handles querying inventory item details (details TBD).
*   `generateReport()`: Handles generating an inventory report (details TBD).
*   `printError()`: Prints an error message to `System.err` based on the current value of the static `errorCode` field, possibly using `ERROR_MESSAGES`.

### 5.2. External Dependencies (Classes/Methods):

*   `LoggingService.logTransaction(int command, int count)`: Logs transaction details (assumed static method in another class).
*   `CustomerService.fetchCustomerData(...)`: Fetches customer data (assumed static method in another class).
*   `java.lang.System`: For `System.in`, `System.out`, `System.err`, `System.exit()`.
*   `java.util.Scanner`: For reading user input.
*   `java.util.ArrayList`: For the dynamic `inventory` list.
*   `java.lang.StringBuilder` (Optional): For efficient string construction if needed.

### 5.3. Static Fields (Class Variables):

*   `errorCode` / `lastError` (`int`): Stores the most recent error status. Initialized to `SUCCESS`.
*   `ERROR_MESSAGES` (`String[]`): Array or Map containing error descriptions, indexed by error code.
*   `transactionCount` (`int`): Counter for processed transactions. Initialized to `0`.
*   `inventory` (`ArrayList<ItemRecord>`): Holds `ItemRecord` objects.
*   `inventoryCount` (`int` - Potentially redundant): Current number of items in `inventory` (can use `inventory.size()`).
*   `currentCustomer` (`CustomerRecord` - Potentially local or instance variable): Reference to a `CustomerRecord` object, used for bit manipulation example. *Note: In the Java context, this might be better handled locally within a method or as part of a larger session object rather than a static field unless truly global state is intended.*
*   `buffer` (`StringBuilder` - Optional): Global buffer if complex string building is needed across methods.

### 5.4. Data Structures (Classes):

*   `ItemRecord`: POJO class defining the structure for an inventory item (fields like `itemId`, `name`, `quantity`, `price`).
*   `CustomerRecord`: POJO class defining the structure for a customer record. Contains fields, potentially including an `int` or `byte` field to store status flags managed via bitwise operations (e.g., `flags`). Includes constants for bit masks (e.g., `FLAG_IS_ACTIVE`, `FLAG_TAX_EXEMPT`, `FLAG_RESERVED`).

### 5.5. Constants (public static final):

*   `MAX_ITEMS` (`int`): Maximum allowed items (may influence initial capacity of `ArrayList` or validation logic).
*   `BUFFER_SIZE` (`int`): Potential size for `StringBuilder` if used.
*   `SUCCESS`, `ERROR_FILE_NOT_FOUND`, `ERROR_INVALID_INPUT`, `ERROR_SYSTEM` (`int`): Status and error codes.
*   `COMMAND_CODES`, `COMMAND_NAMES` (`int[]`, `String[]` or `Map<Integer, String>`): Definitions for menu options.
*   Bit mask constants within `CustomerRecord` (e.g., `FLAG_IS_ACTIVE = 0b0001`, `FLAG_TAX_EXEMPT = 0b0010`, `FLAG_RESERVED = 0b0100`).

## 6. Usage Examples or Edge Cases

*   **Standard Usage:** The application is run (e.g., `java InventorySystem`). It presents a menu, and the user enters numeric commands (1-6) to perform inventory actions or exit. Input is read from the console.
*   **Invalid Input:** Entering non-numeric input when a number is expected will cause `Scanner` to throw an `InputMismatchException`, which should be caught in `readCommand` to set `errorCode = ERROR_INVALID_INPUT` and return an invalid command marker. Entering a numeric command outside the 1-6 range triggers the `default` case in the `switch`, sets `errorCode`, and calls `printError`.
*   **Error Handling:** Relies on setting the static `errorCode` field. The `printError` method uses this code (potentially indexing into `ERROR_MESSAGES`) to display information to `System.err`. Errors during external calls (e.g., `LoggingService.logTransaction`) might throw exceptions that need to be caught, or they might return error codes/statuses that need checking. Standard Java practice might favor exceptions over error codes for many scenarios.
*   **Data Limits:** If using `ArrayList`, the primary limit is available memory. If fixed-size arrays were used, `ArrayIndexOutOfBoundsException` is possible. Logic within `addItem` should check against `MAX_ITEMS` if it represents a business rule, not just an initial capacity suggestion.
*   **Object References (Null Safety):** The `currentCustomer` reference must be checked for `null` before attempting bitwise operations or any method calls on it to prevent `NullPointerException`. This emphasizes the need for proper object creation or retrieval (e.g., ensuring `CustomerService.fetchCustomerData` returns a valid object or handles the "not found" case gracefully).
*   **Resource Management:** The `Scanner` object connected to `System.in` should be closed when the application exits to release system resources. Using a `try-with-resources` block for the `Scanner` in the `main` method is the recommended approach.
*   **Concurrency:** As specified (using static fields and methods), this application is not inherently thread-safe. If multiple threads were introduced, access to shared static fields like `inventory`, `transactionCount`, and `errorCode` would require synchronization.
*   **External Dependencies:** The application's functionality relies on the correct implementation and availability of external classes/methods like `LoggingService` and `CustomerService`. Runtime errors (`ClassNotFoundException`, `IOException`, or custom exceptions from these services) could occur and may need handling.