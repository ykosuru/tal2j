
 Technical Documentation
# Program: inventory_system - Technical Specification

## 1. Overview

**Program Name:** `inventory_system`

**Language:** TAL (Transaction Application Language) for HP NonStop (Tandem) systems.

**Purpose:** This program provides a foundational example of a TAL application simulating a basic command-line inventory management system. It demonstrates core TAL programming constructs including data structures, arrays, pointers, procedures, control flow, external calls, and basic error handling.

**Functionality:** The program presents a menu to the user with options to Add, Delete, Update, Query inventory items, generate a simple Report, or Exit. It initializes a small inventory in memory and processes user commands within a loop. Transaction logging is simulated via an external call.

**Key Features Demonstrated:**
*   Global variable and literal declarations (`LITERAL`, `INT`, `STRING`, `FIXED`).
*   Structure (`STRUCT`) definitions for data records (`item_record`, `customer_record`).
*   Array definitions and initialization (static `P` type and dynamic).
*   Pointer (`.`) usage, address operator (`@`), and assignment.
*   Subprocedure (`PROC`) definitions, calls, and forward declarations (`FORWARD`).
*   External procedure calls (`EXTERNAL PROC`) and external pointer usage (`.EXT`).
*   Control flow: `WHILE` loops, `IF...THEN...ELSE...ENDIF`, `CASE` statements.
*   Bit manipulation within structure fields (`UNSIGNED(n)`).
*   Equivalenced variables (`=`).
*   Global blocks (`BLOCK`).
*   Basic error handling using a global error code and message array.
*   Main program entry point (`MAIN`).

**Note:** This program is primarily illustrative. Core functionalities like user input (`read_command`), data persistence (file I/O within `inventory_io` block or elsewhere), detailed business logic for item management (`add_item`, `delete_item`, etc.), reporting (`generate_report`), and terminal output (`write`, `write_line`) are represented by **stubbed procedures** and require full implementation for practical use.

## 2. Compilation and Execution

*   **Compilation:** Requires the TAL compiler on an HP NonStop system.