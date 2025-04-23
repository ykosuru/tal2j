markdown
# Technical Documentation: TAL Program - inventory_system

## 1. Overview

**Program Name:** `inventory_system`

**Language:** TAL (Transaction Application Language) for HP NonStop (Tandem) systems.

**Purpose:** This program serves as a comprehensive example demonstrating various TAL features. It simulates a basic command-line inventory management system allowing users to perform operations like adding, deleting, updating, querying inventory items, and generating reports. It showcases the use of structures, arrays, pointers, global variables, external procedure calls, bit manipulation, and basic control flow constructs.

**Functionality:**
*   Initializes a sample inventory.
*   Presents a menu of operations (ADD, DELETE, UPDATE, QUERY, REPORT, EXIT).
*   Reads user commands.
*   Dispatches control to appropriate (stubbed) subprocedures based on the command.
*   Logs transactions (via an external call).
*   Includes examples of pointer usage and bit-field manipulation within structures.
*   Basic error handling using global error codes and messages.

**Note:** This program is primarily illustrative. Core functionalities like actual data input/output, file operations, and detailed business logic within the command procedures (`add_item`, `delete_item`, etc.) are stubbed and require implementation.

## 2. Program Structure

The program is organized as follows:

1.  **Declarations:**
    *   `NAME`: Program identifier.
    *   `FORWARD`: Pre-declarations for procedures and structures used before definition.
    *   `EXTERNAL`: Declarations for procedures defined in separate modules/libraries.
    *   `LITERAL`: Definition of named constants.
    *   `TYPE`: Simple type definitions (aliases).
    *   `STRUCT`: Definitions of complex data structures (`item_record`, `customer_record`).
    *   `INT .SG`: Declaration of system global pointers (e.g., `system_time`).
    *   `Global Variables`: Program-wide variables, including arrays and counters, initialized values.
    *   `Pointers`: Declaration of pointers to various data types/structures.
    *   `Read-only Arrays`: Arrays initialized with data, often marked 'P' (Protected).
    *   `Equivalenced Variables`: Variables initialized based on others or system values (e.g., `buffer_length = 'G'[10]`).
    *   `BLOCK`: Grouping of related global variables (e.g., `inventory_io`).
2.  **Main Procedure (`main_proc`):**
    *   The entry point of the program (`MAIN` attribute).
    *   Contains the main processing loop, menu display, command reading, and dispatching logic.
    *   Performs initialization and cleanup tasks.
3.  **Subprocedures:**
    *   Implementations of specific tasks called by `main_proc` (e.g., `initialize_inventory`, `display_menu`).
    *   Stubs for core inventory operations and I/O routines.

## 3. Key Data Structures

### 3.1. `STRUCT item_record`

*   **Purpose:** Represents a single item in the inventory.
*   **Fields:**
    *   `item_id` (INT): Unique identifier for the item.
    *   `item_name` (STRING[0:30]): Name of the item (max 31 characters).
    *   `item_price` (FIXED(2)): Price with 2 decimal places.
    *   `quantity_on_hand` (INT): Current stock level.
    *   `reorder_level` (INT): Minimum stock level before reordering.
    *   `supplier_id` (INT): Identifier of the item's supplier.
    *   `last_updated` (STRING[0:10]): Date of last update (e.g., "YYYY-MM-DD", max 11 chars).

### 3.2. `STRUCT customer_record`

*   **Purpose:** Represents a customer record (Note: Primarily used for bit manipulation example in this code, not fully integrated).
*   **Fields:**
    *   `customer_id` (INT): Unique identifier for the customer.
    *   `customer_name` (STRING[0:50]): Customer's name (max 51 characters).
    *   `address_line1` (STRING[0:30]): Address line 1 (max 31 characters).
    *   `address_line2` (STRING[0:30]): Address line 2 (max 31 characters).
    *   `city` (STRING[0:20]): City (max 21 characters).
    *   `state` (STRING[0:2]): State abbreviation (max 3 characters).
    *   `zip_code` (STRING[0:9]): Zip code (max 10 characters).
    *   `account_balance` (FIXED(2)): Account balance with 2 decimal places.
    *   `FILLER 2`: 2 bytes of padding for alignment purposes.
    *   `is_active` (UNSIGNED(1)): Bit flag (1=active, 0=inactive).
    *   `has_credit` (UNSIGNED(1)): Bit flag (1=credit allowed, 0=no credit).
    *   `tax_exempt` (UNSIGNED(1)): Bit flag (1=tax exempt, 0=taxable).
    *   `reserved` (UNSIGNED(13)): 13 reserved bits for future use.

## 4. Global Variables & Constants

*   `max_items` (LITERAL): Max number of items the `inventory` array can hold (1000).
*   `buffer_size` (LITERAL): Size of the global string buffer `buffer` (4096).
*   `success`, `error_...` (LITERALs): Status codes (0, 1, 2, 3).
*   `error_code` (INT): Holds the status code of the last operation (0 for success).
*   `error_messages` (STRING[0:3]): Array of messages corresponding to `error_code`.
*   `transaction_count` (INT): Counter for processed transactions.
*   `inventory` (STRUCT item_record[0:max_items-1]): Array holding all inventory item records.
*   `inventory_count` (INT): Current number of active items in the `inventory` array.
*   `buffer` (STRING .): Pointer to a global character buffer (size `buffer_size`), used for temporary string operations and I/O.
*   `command_codes`, `command_names` (INT[], STRING[]): Parallel arrays holding numeric codes and string names for menu commands. Marked 'P' (Protected/Persistent).
*   `buffer_length` (INT): Initialized from `'G'[10]`, likely a system global indicating process information (e.g., default terminal buffer length or similar context).
*   `inventory_io` (BLOCK): Contains variables related to potential file I/O (`filename`, `file_error`, `record_count`). Not used in the current implementation.

## 5. External Dependencies

*   `PROC log_transaction`: Assumed to be defined externally. Called after each valid command (except EXIT) to log the operation. Requires command code and transaction count as parameters.
*   `PROC fetch_customer_data`: Assumed to be defined externally. Declared but not called in the provided code. Likely intended for retrieving customer details.
*   **Implicit System Procedures:** The code implies the existence of procedures for terminal I/O:
    *   `write_line(STRING line; INT len)`: Writes a string followed by a newline.
    *   `write(STRING line; INT len)`: Writes a string without a newline.
    *   `read_command(INT REF cmd)`: Reads user input and returns the command code. (Stubbed in this code).
*   **System Globals:** Accesses `'G'[10]` which depends on the Guardian OS environment configuration.

## 6. Procedure Descriptions

### 6.1. `INT PROC main_proc, MAIN`

*   **Purpose:** Main program entry point and control flow orchestrator.
*   **Parameters:** None explicit. Uses global variables extensively.
*   **Return Value:** `INT` - Returns `success` (0) upon normal termination.
*   **Logic:**
    1.  Initializes the global `buffer`.
    2.  Calls `initialize_inventory` to populate sample data.
    3.  Enters a `WHILE` loop that continues until the user command (`cmd`) is 6 (EXIT).
    4.  Inside the loop:
        *   Calls `display_menu` to show options.
        *   Calls `read_command` (stubbed) to get user input into `cmd`.
        *   Uses a `CASE` statement to call the appropriate subprocedure based on `cmd`:
            *   1: `add_item` (stubbed)
            *   2: `delete_item` (stubbed)
            *   3: `update_item` (stubbed)
            *   4: `query_item` (stubbed)
            *   5: `generate_report` (stubbed)
            *   6: Does nothing (loop terminates).
            *   `OTHERWISE`: Sets `error_code` to `error_invalid_input` and calls `print_error`.
        *   If `cmd` is not 6, increments `transaction_count` and calls external `log_transaction`.
    5.  After the loop:
        *   Sets `error_ptr` to the address of `error_code`.
        *   Checks if `error_code` (via `error_ptr^`) is non-zero and calls `print_error` if it is (final status check).
        *   **Demonstrates bit manipulation:** If inventory exists, it attempts to modify bit fields (`is_active`, `tax_exempt`, `reserved`) of a `customer_record` pointed to by `current_customer`. **(See Potential Issues)**.
    6.  Returns `success`.
*   **Globals Used/Modified:** `buffer`, `inventory_count`, `cmd` (local), `error_code`, `transaction_count`, `error_ptr`, `current_customer`.

### 6.2. `PROC initialize_inventory`

*   **Purpose:** Populates the global `inventory` array with initial hardcoded sample data for testing.
*   **Parameters:** None.
*   **Return Value:** None.
*   **Logic:**
    1.  Sets `inventory_count` to 3.
    2.  Assigns values to all fields of `inventory[0]`, `inventory[1]`, and `inventory[2]`.
*   **Globals Used/Modified:** `inventory`, `inventory_count`.

### 6.3. `PROC display_menu`

*   **Purpose:** Displays the command menu to the user's terminal.
*   **Parameters:** None.
*   **Return Value:** None.
*   **Logic:**
    1.  Uses the global `buffer` to format lines.
    2.  Prints the menu title using `write_line` (stubbed).
    3.  Loops from `i = 0` to `5`.
    4.  Inside the loop, formats each menu option using `command_codes[i]` and `command_names[i]` into the `buffer`.
    5.  Prints each menu option using `write_line` (stubbed).
    6.  Prints the "Enter command: " prompt using `write` (stubbed).
*   **Globals Used/Modified:** `buffer`. Uses `command_codes`, `command_names`. Relies on `write_line`, `write`.

### 6.4. Stubbed Procedures

The following procedures are declared and called but lack implementation:

*   `PROC print_error`: **Intended Purpose:** Display the error message from `error_messages` corresponding to the current `error_code`.
*   `PROC add_item`: **Intended Purpose:** Prompt user for new item details, validate input, add a new record to the `inventory` array, and update `inventory_count`.
*   `PROC delete_item`: **Intended Purpose:** Prompt user for an item ID to delete, find the item, remove it from the `inventory` array (e.g., by shifting elements or marking as inactive), and update `inventory_count`.
*   `PROC update_item`: **Intended Purpose:** Prompt user for an item ID and fields to update, find the item, validate input, and modify the corresponding `inventory` record.
*   `PROC query_item`: **Intended Purpose:** Prompt user for an item ID, find the item, and display its details.
*   `PROC generate_report`: **Intended Purpose:** Iterate through the `inventory` array and print a formatted report of all items (or based on criteria).
*   `PROC read_command(INT REF cmd)`: **Intended Purpose:** Read input from the user terminal, parse it as an integer command, and return it via the `cmd` reference parameter. Needs input validation.
*   `PROC write_line(STRING line; INT len)`: **Intended Purpose:** Write the provided string `line` (up to `len` characters) to the standard output (terminal) followed by a newline character.
*   `PROC write(STRING line; INT len)`: **Intended Purpose:** Write the provided string `line` (up to `len` characters) to the standard output (terminal) *without* a newline character.

## 7. Error Handling

*   Uses a global integer `error_code` to store the status of operations.
*   `LITERAL` constants define specific error codes (`error_file_not_found`, `error_invalid_input`, `error_system`) and success (`success = 0`).
*   A global array `error_messages` holds corresponding text messages.
*   The `print_error` procedure (stubbed) is intended to display the message associated with the current `error_code`.
*   Invalid menu commands trigger `error_invalid_input`.
*   Other error conditions (e.g., file errors, item not found) would need to be implemented in the stubbed procedures, setting `error_code` appropriately.

## 8. Build and Execution (Conceptual)

1.  **Compilation:** Compile the TAL source file (`inventory_system.tal`) using the TAL compiler.