
 Technical Documentation
# Program: inventory_system

## 1. Overview

**Program Name:** `inventory_system`

**Language:** TAL (Transaction Application Language) for HP NonStop (Tandem) systems.

**Purpose:** This program serves as a comprehensive example demonstrating various TAL features. It simulates a basic command-line inventory management system allowing users to add, delete, update, query items, and generate reports.

**Key Features Demonstrated:**
*   Global variable and literal declarations.
*   Structure (`STRUCT`) definitions for data records (`item_record`, `customer_record`).
*   Array definitions and initialization (static and pointer-based).
*   Pointer (`.`) usage and assignment (`@`).
*   Subprocedure (`PROC`) definitions and calls.
*   Forward declarations (`FORWARD`).
*   External procedure calls (`EXTERNAL PROC`).
*   Basic control flow (`WHILE`, `IF`, `CASE`).
*   Bit manipulation within structures.
*   Equivalenced variables (`=`).
*   Global blocks (`BLOCK`).
*   Basic error handling using global error codes and messages.

**Note:** This program is primarily illustrative. Many core functionalities (user input, file I/O, detailed business logic) are represented by **stubbed procedures** and require full implementation for practical use.

## 2. Compilation and Execution

This program is intended to be compiled using the TAL compiler on an HP NonStop system. It would typically be linked with other modules providing the `EXTERNAL` procedures (`log_transaction`, `fetch_customer_data`) and potentially standard I/O libraries if the stubbed I/O procedures (`write`, `write_line`, `read_command`) were replaced with actual implementations.