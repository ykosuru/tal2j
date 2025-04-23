# TAL Technical Specifications: inventory_system

## 1. Program Description

The `inventory_system` program provides a basic, menu-driven interface for managing an inventory of items. It allows users to perform common inventory operations such as adding, deleting, updating, and querying items, as well as generating a simple report. The program utilizes global variables, structures, arrays, pointers, and calls to both internal subprocedures and external routines for logging and potentially data retrieval. It includes basic error handling and demonstrates features like bit manipulation within data structures.

## 2. Input Parameters

The main program (`main_proc`) does not accept formal parameters upon invocation. However, it interacts with the user and the system environment for input:

*   **User Input:**
    *   **Type:** Integer (Command Selection)
    *   **Description:** The program prompts the user to enter a numeric command corresponding to the menu options displayed (1-6). This input is read via the `read_command` subprocedure (implementation not shown). Further input may be required by specific subprocedures like `add_item`, `delete_item`, etc. (details depend on their implementation).
*   **System Environment:**
    *   **Type:** System Globals (e.g., Pointers)
    *   **Description:** The program may implicitly rely on system global variables, such as `system_time` (declared but not explicitly used in the main flow shown) and `buffer_length` (aliased from `'G'[10]`).

## 3. Return Value

*   **Type:** `INT` (Integer)
*   **Description:** The `main_proc` returns an integer status code to the calling environment (e.g., the operating system or a higher-level process).
    *   `0` (`success`): Indicates successful execution and normal termination.
    *   Non-zero values are possible if error handling logic were to return specific error codes, although the current implementation explicitly returns `success` (0). The final value of `error_code` is pointed to by `error_ptr` before exit but not explicitly returned by `main_proc`.

## 4. Core Logic/Processing Steps

1.  **Initialization:**
    *   The global pointer `buffer` is allocated memory (`buffer_size` bytes) and initialized with spaces.
    *   The `initialize_inventory` subprocedure is called to populate the global `inventory` array with predefined sample item records and set the initial `inventory_count`.
2.  **Main Processing Loop:**
    *   The program enters a `WHILE` loop that continues as long as the user command (`cmd`) is not equal to 6 (Exit).
    *   **Display Menu:** The `display_menu` subprocedure is called to present the available command options (Add, Delete, Update, Query, Report, Exit) to the user.
    *   **Read Command:** The `read_command` subprocedure (implementation assumed) is called to get the user's numeric command choice and store it in the local variable `cmd`.
    *   **Command Dispatch:** A `CASE` statement evaluates the `cmd` variable:
        *   Commands 1-5 trigger calls to their respective subprocedures (`add_item`, `delete_item`, `update_item`, `query_item`, `generate_report`). (Implementations not shown).
        *   Command 6 (Exit) does nothing, allowing the `WHILE` loop condition to terminate the loop.
        *   Any other command value (`OTHERWISE`) sets the global `error_code` to `error_invalid_input` and calls `print_error` to display the corresponding message.
    *   **Transaction Logging:** If the command was not 6 (Exit), the global `transaction_count` is incremented, and the external procedure `log_transaction` is called with the command code and current transaction count.
3.  **Cleanup and Termination:**
    *   After the loop terminates (command 6 entered), the global pointer `error_ptr` is set to the address of the global `error_code` variable.
    *   If `error_ptr` (effectively `error_code`) is non-zero, the `print_error` subprocedure is called one last time.
    *   **Bit Manipulation Example:** If `inventory_count` is greater than 0, the program attempts to manipulate bit flags (`is_active`, `tax_exempt`, `reserved`) within a `customer_record` structure pointed to by `current_customer`. *Note: This section assumes `current_customer` points to valid, allocated memory, which is not explicitly shown.*
    *   The program returns the value of the `success` literal (0) to the operating environment.

## 5. Dependencies

*   **External Procedures:**
    *   `log_transaction`: Called to log details of processed commands. Requires linking with the module providing this procedure.
    *   `fetch_customer_data`: Declared but not called in the provided code snippet. If used, would require linking.
*   **Internal Subprocedures (Called):**
    *   `initialize_inventory`: Populates initial inventory data.
    *   `display_menu`: Shows the command menu.
    *   `print_error`: Displays error messages based on `error_code`. (Definition not provided).
    *   `read_command`: Reads user command input. (Definition not provided).
    *   `add_item`, `delete_item`, `update_item`, `query_item`, `generate_report`: Handle specific inventory operations. (Definitions not provided).
    *   `write_line`, `write`: Assumed low-level procedures for terminal output. (Definitions not provided).
*   **Global Variables:**
    *   `error_code`: Stores the status of the last operation.
    *   `error_messages`: Array of error message strings.
    *   `transaction_count`: Counter for processed transactions.
    *   `inventory`: Array holding `item_record` structures.
    *   `inventory_count`: Current number of items in the `inventory` array.
    *   `system_time` (via `.SG`): Pointer to system time (declared, not used in main flow).
    *   `error_ptr` (via `.EXT`): External pointer, set to address of `error_code`.
    *   `current_customer`: Pointer to a `customer_record`.
    *   `buffer`: Pointer to a general-purpose string buffer.
    *   `last_error`: Alias for `error_code`.
    *   `buffer_length`: Alias for system global `'G'[10]`.
*   **Data Structures:**
    *   `item_record`: Structure defining inventory item data.
    *   `customer_record`: Structure defining customer data.
*   **Constants:**
    *   `max_items`, `max_customers`, `buffer_size`, `success`, `error_file_not_found`, `error_invalid_input`, `error_system`.

## 6. Usage Examples or Edge Cases

*   **Standard Usage:** The user starts the program, sees the menu, enters a number (1-5) to perform an action or 6 to quit. The program prompts for further details if needed for actions like adding or updating items.
*   **Invalid Input:** If the user enters a number other than 1-6 at the main menu prompt, the program sets `error_code` to `error_invalid_input` (2) and displays the "Invalid input" message via `print_error`.
*   **Error Handling:** The program relies on setting the global `error_code` variable when errors occur (e.g., invalid input, potentially file errors within I/O operations not shown). The `print_error` procedure uses this code to display the appropriate message from the `error_messages` array.
*   **Inventory Limit:** The `inventory` array has a fixed size defined by `max_items`. Attempts to add items beyond this limit would likely fail or cause buffer overflows if not handled correctly within the `add_item` procedure (implementation not shown).
*   **External Dependencies:** The program will fail during linking or execution if the external procedures (`log_transaction`) are not available or cannot be loaded.
*   **Bit Manipulation Caveat:** The bit manipulation example towards the end assumes that `current_customer` points to a valid `customer_record`. If this pointer has not been properly initialized (e.g., by allocating memory or calling a function like `fetch_customer_data`), this code could lead to memory access violations or unpredictable behavior.
*   **Unimplemented Procedures:** Many core functions (`add_item`, `delete_item`, `update_item`, `query_item`, `generate_report`, `print_error`, `read_command`, `write_line`, `write`) are called but not defined in the provided code. The program's full functionality depends on their correct implementation.