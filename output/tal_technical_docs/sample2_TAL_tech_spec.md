# TAL Technical Specifications: inventory_system

## 1. Program Description

The `inventory_system` program is a TAL module designed to manage a basic inventory of items. It presents a text-based menu to the user, allowing them to perform operations such as adding, deleting, updating, and querying inventory items, as well as generating reports. The program utilizes global variables to store inventory data, error status, and configuration constants. It demonstrates the use of arrays, structures, pointers, subprocedures, external procedure calls, and basic error handling.

## 2. Input Parameters

The main entry point (`main_proc`) does not accept direct parameters upon invocation. However, the program relies on the following inputs during execution:

*   **User Command Input:** An integer value entered by the user via the terminal/input device in response to the menu prompt. This input is read by the (placeholder) `read_command` subprocedure and stored in the local variable `cmd` within `main_proc`. Valid inputs are expected to be integers from 1 to 6.
*   **Data for Operations (Implied):** Although the core logic for add, delete, update, and query is currently implemented as placeholders, a fully functional version would require additional user input (e.g., item ID, name, price) specific to each operation.

## 3. Return Value

The `main_proc` procedure returns an `INT` value upon termination:

*   **`success` (0):** Indicates normal program termination without unhandled errors encountered during the main processing loop or cleanup.

*Note: While error conditions are detected and reported via `print_error`, the current logic only explicitly returns `success` (0) from `main_proc`.*

## 4. Core Logic/Processing Steps

1.  **Initialization:**
    *   The global string pointer `buffer` is allocated with a size of `buffer_size` (4096 bytes) and initialized with spaces.
    *   The `initialize_inventory` subprocedure is called to populate the global `inventory` array with predefined sample item records and set the `inventory_count`.
    *   The local command variable `cmd` is initialized to 0.

2.  **Main Processing Loop:**
    *   The program enters a `WHILE` loop that continues as long as `cmd` is not equal to 6 (the EXIT command).
    *   **Display Menu:** The `display_menu` subprocedure is called to print the available command options (1-6) and their descriptions (fetched from `command_codes` and `command_names` arrays) to the output device using the global `buffer`.
    *   **Read Command:** The `read_command` subprocedure (placeholder) is called to obtain the user's menu choice, which is stored in the `cmd` variable.
    *   **Command Dispatch (CASE Statement):**
        *   If `cmd` is 1-5, the corresponding placeholder subprocedure (`add_item`, `delete_item`, `update_item`, `query_item`, `generate_report`) is called. These currently only print a "not implemented" message.
        *   If `cmd` is 6, the `CASE` statement does nothing, and the `WHILE` loop condition will become false, causing the loop to terminate.
        *   If `cmd` is any other value (`OTHERWISE`), the global `error_code` is set to `error_invalid_input`, and the `print_error` subprocedure is called to display the corresponding error message.
    *   **Transaction Logging:** If the command was not 6 (EXIT), the global `transaction_count` is incremented, and the external procedure `log_transaction` is called, passing the command code (`cmd`) and the updated transaction count.

3.  **Cleanup and Termination:**
    *   After the `WHILE` loop terminates (user entered 6):
        *   The address of the global `error_code` variable is assigned to the global pointer `error_ptr`.
        *   The program checks if the value pointed to by `error_ptr` (i.e., `error_code`) is non-zero. If it is, `print_error` is called again (this might be redundant if the error was already printed within the loop).
        *   An example bit manipulation sequence is performed: If `inventory_count` > 0, it sets the `is_active` and `tax_exempt` bits in a `customer_record` structure pointed to by `current_customer`. *Note: The logic assumes `current_customer` points to a valid record, which is not explicitly established in the provided flow.*
        *   The program returns the `success` (0) status code.

## 5. Dependencies

*   **Internal Subprocedures Called:**
    *   `initialize_inventory`: Populates initial inventory data.
    *   `display_menu`: Shows the user menu.
    *   `read_command` (Placeholder): Reads user command input.
    *   `print_error`: Displays error messages based on `error_code`.
    *   `add_item` (Placeholder): Stub for adding items.
    *   `delete_item` (Placeholder): Stub for deleting items.
    *   `update_item` (Placeholder): Stub for updating items.
    *   `query_item` (Placeholder): Stub for querying items.
    *   `generate_report` (Placeholder): Stub for generating reports.
    *   `write_line` (Placeholder): Writes a string followed by a newline to output.
    *   `write` (Placeholder): Writes a string without a newline to output.

*   **External Procedures Called:**
    *   `log_transaction`: Called after each valid command (except EXIT) to log the operation. Assumed to be defined in an external library or module.
    *   `fetch_customer_data`: Declared as EXTERNAL but *not* called in the provided code snippet.

*   **Key Global Variables/Data Structures Used:**
    *   `error_code` (INT): Stores the status of the last operation.
    *   `error_messages` (STRING Array): Contains text descriptions for error codes.
    *   `transaction_count` (INT): Tracks the number of processed commands.
    *   `inventory` (Array of `item_record` STRUCT): Holds the inventory data.
    *   `inventory_count` (INT): Current number of items in the `inventory` array.
    *   `error_ptr` (INT Pointer): Pointer intended to hold the address of `error_code`.
    *   `current_customer` (Pointer to `customer_record` STRUCT): Used in the example bit manipulation section.
    *   `buffer` (STRING Pointer): General-purpose buffer for string formatting and I/O.
    *   `command_codes` (INT Array): Numeric codes for menu commands.
    *   `command_names` (STRING Array): Text names for menu commands.
    *   `inventory_io` (BLOCK): Structure grouping file I/O related variables (declared but not actively used in the core logic shown).
    *   `item_record` (STRUCT): Defines the structure for an inventory item.
    *   `customer_record` (STRUCT): Defines the structure for a customer record (used in bit manipulation example).
    *   Literals (`max_items`, `max_customers`, `buffer_size`, `success`, error codes): Define constants used throughout the program.

*   **System Globals:**
    *   `.SG system_time` (INT Pointer): Declared pointer to a system global variable (not actively used in the provided logic).
    *   `'G'[10]` (Example): Referenced in the `EQUIVALENCE` for `buffer_length` (illustrative).

## 6. Usage Examples or Edge Cases

*   **Typical Usage:**
    1.  The program starts and displays the menu.
    2.  User enters `1` (ADD). The program prints "Add item functionality not yet implemented." and calls `log_transaction`.
    3.  The menu is displayed again.
    4.  User enters `4` (QUERY). The program prints "Query item functionality not yet implemented." and calls `log_transaction`.
    5.  The menu is displayed again.
    6.  User enters `6` (EXIT). The program terminates normally, returning `success` (0).

*   **Edge Cases & Limitations:**
    *   **Invalid Command Input:** Entering a non-numeric value or a number outside the 1-6 range when prompted for a command. The `OTHERWISE` clause handles invalid *numeric* commands (e.g., 7 or 0) by setting `error_code` and calling `print_error`. Handling non-numeric input depends on the implementation of `read_command`.
    *   **Placeholder Functionality:** Core inventory operations (add, delete, update, query, report) are not implemented.
    *   **I/O Implementation:** The `read_command`, `write_line`, and `write` procedures are placeholders. Actual implementation would require specific system calls or library functions for terminal I/O.
    *   **Inventory Limits:** The `inventory` array has a fixed size (`max_items`). The placeholder `add_item` does not check if the array is full.
    *   **Error Handling:** Error handling is basic. `print_error` displays messages based on `error_code`, but complex error recovery is not implemented. The final check using `error_ptr` might be redundant.
    *   **External Dependencies:** The program relies on the external `log_transaction` procedure being available and functioning correctly.
    *   **Buffer Usage:** The global `buffer` is reused for various purposes (menu display, error messages, placeholder output). Care must be taken in a full implementation to avoid buffer overflows, especially when concatenating strings or handling variable-length input/output.
    *   **Bit Manipulation Context:** The bit manipulation example operates on `current_customer`, but the code doesn't show how this pointer is initialized to a valid `customer_record` instance within the main workflow. It appears illustrative rather than fully integrated.