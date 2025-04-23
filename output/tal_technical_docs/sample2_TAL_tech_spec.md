TAL Technical Specifications
# TAL Technical Specifications: inventory_system

## 1. Program Description

`inventory_system` is a TAL program designed to manage a simple inventory. It provides a command-line interface for users to perform basic inventory operations such as adding, deleting, updating, querying items, and generating reports. The program utilizes global variables, structures, arrays, pointers, and external procedure calls to manage inventory data and interact with the user and potentially other system components (like logging). It includes basic error handling and demonstrates features like bit manipulation within structures.

## 2. Input Parameters

The main entry point procedure `main_proc` does not accept any formal input parameters upon invocation. Program control and data input are handled through:

*   **User Interaction:** The program prompts the user to enter commands via standard input (implicitly handled by the `read_command` procedure).
*   **System Globals:** May potentially read system-level information (e.g., `system_time`, `buffer_length` via `'G'[10]`).
*   **External Data:** May interact with external data sources via procedures like `fetch_customer_data` (though not called in the main loop shown).

## 3. Return Value

*   **Type:** `INT`
*   **Description:** The `main_proc` returns an integer status code to the operating system or calling process upon termination.
    *   `success` (defined as `0`): Indicates successful execution and normal termination.
    *   Other values are possible if error handling logic were added to return different codes based on `error_code`.

## 4. Core Logic/Processing Steps

1.  **Initialization:**
    *   Allocates and initializes a global string buffer (`buffer`) with spaces.
    *   Calls `initialize_inventory` to populate the `inventory` array with sample data and set `inventory_count`.
    *   Initializes the local `cmd` variable to `0`.
2.  **Main Command Loop:**
    *   Enters a `WHILE` loop that continues as long as the user command (`cmd`) is not `6` (EXIT).
    *   Calls `display_menu` to show available commands to the user.
    *   Calls `read_command` (implementation not shown) to get the user's command choice and store it in `cmd`.
    *   Uses a `CASE` statement to dispatch control based on the value of `cmd`:
        *   `1`: Calls `add_item`.
        *   `2`: Calls `delete_item`.
        *   `3`: Calls `update_item`.
        *   `4`: Calls `query_item`.
        *   `5`: Calls `generate_report`.
        *   `6`: Does nothing (loop condition handles termination).
        *   `OTHERWISE`: Sets `error_code` to `error_invalid_input` and calls `print_error`.
    *   If the command was not `6` (EXIT):
        *   Increments the global `transaction_count`.
        *   Calls the external procedure `log_transaction` with the command code and transaction count.
3.  **Cleanup/Termination:**
    *   Assigns the address of the global `error_code` to the external pointer `error_ptr`.
    *   Checks if `error_ptr` (effectively `error_code`) is non-zero. If so, calls `print_error` one last time.
    *   **Illustrative Bit Manipulation:** If `inventory_count` > 0:
        *   Assigns an address (presumably of a `customer_record` structure) to the `current_customer` pointer. *Note: The source address `@customer_record` is potentially problematic without prior allocation or data fetching.*
        *   Sets the `is_active` and `tax_exempt` bit fields within the pointed-to structure to `1`.
        *   Clears the `reserved` bit field to `0`.
    *   Returns the `success` status code (`0`).

## 5. Dependencies

### 5.1. Internal Procedures Called:

*   `initialize_inventory`: Populates the inventory array with initial data.
*   `display_menu`: Displays the command menu options to the user.
*   `read_command` (Implementation not shown): Reads a command from the user.
*   `add_item` (Implementation not shown): Handles adding a new inventory item.
*   `delete_item` (Implementation not shown): Handles deleting an inventory item.
*   `update_item` (Implementation not shown): Handles updating an inventory item.
*   `query_item` (Implementation not shown): Handles querying inventory item details.
*   `generate_report` (Implementation not shown): Handles generating an inventory report.
*   `print_error` (Forward declared, implementation not shown): Prints an error message based on `error_code`.

### 5.2. External Procedures Called:

*   `log_transaction`: Logs transaction details (defined externally).
*   `fetch_customer_data` (Declared, but not called in `main_proc`): Fetches customer data (defined externally).
*   `write_line` (Assumed external/system): Writes a string buffer followed by a newline to output.
*   `write` (Assumed external/system): Writes a string buffer without a newline to output.

### 5.3. Global Variables Used:

*   `error_code` / `last_error`: Stores the most recent error status.
*   `error_messages`: Array of strings containing error descriptions.
*   `transaction_count`: Counter for processed transactions.
*   `inventory`: Array of `item_record` structures holding inventory data.
*   `inventory_count`: Current number of items in the `inventory` array.
*   `error_ptr`: External pointer, assigned to the address of `error_code`.
*   `current_customer`: Pointer to a `customer_record` structure.
*   `buffer`: Global string buffer for I/O and string manipulation.
*   `buffer_length`: Alias for a system global `'G'[10]`.
*   `system_time`: Pointer to a system global variable for time.
*   `inventory_io` (BLOCK): Contains file I/O related variables (filename, file_error, record_count).

### 5.4. Structures Used:

*   `item_record`: Defines the structure for an inventory item.
*   `customer_record`: Defines the structure for a customer record (including bit fields).

### 5.5. Literals Used:

*   `max_items`, `max_customers`, `buffer_size`: Size limits and buffer definition.
*   `success`, `error_file_not_found`, `error_invalid_input`, `error_system`: Status and error codes.
*   `command_codes`, `command_names`: Read-only arrays defining menu options.

## 6. Usage Examples or Edge Cases

*   **Standard Usage:** The program is run, presents a menu, and the user enters numeric commands (1-6) to perform inventory actions or exit.
*   **Invalid Input:** Entering a command number not between 1 and 6 will trigger the `OTHERWISE` case, set `error_code` to `error_invalid_input` (2), and call `print_error`.
*   **Error Handling:** The program relies on setting the `error_code` variable. The `print_error` procedure (implementation not shown) is expected to use this code (potentially indexing into `error_messages`) to display relevant information. Errors during external calls (`log_transaction`) are not explicitly handled in the provided `main_proc` logic.
*   **Data Limits:** The program uses `max_items` to size the `inventory` array. No logic is shown to prevent adding items beyond this limit, which could lead to array bounds errors if not handled within `add_item`.
*   **Pointer Safety:** The assignment `current_customer := @customer_record` requires that `@customer_record` provides a valid memory address for a `customer_record` structure. If this structure hasn't been allocated or fetched (e.g., via `fetch_customer_data`), this assignment could lead to memory access violations or unpredictable behavior during the bit manipulation steps. Similarly, `error_ptr := @error_code` assumes `error_code` has a valid address.
*   **Buffer Allocation:** The allocation `buffer := buffer_size * [" "]` could potentially fail if insufficient memory is available, though this is less common in typical Guardian environments.
*   **External Dependencies:** The program's functionality relies heavily on the correct implementation and availability of external procedures like `log_transaction`, `read_command`, `write_line`, `write`, and the various item management procedures (`add_item`, etc.). Failure or errors within these external components will impact the program's execution.