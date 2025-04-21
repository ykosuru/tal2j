package converted;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Translated Java code for the TAL program sample1.tal.txt
 * Simulates the behavior of the original TAL program.
 */
public class Sample1 {

    // Logger for potential logging (though not used in this specific translation)
    private static final Logger logger = LogManager.getLogger(SampleProgram.class);

    // Global declarations from TAL
    // Simulating TAL global variables with static fields
    private static int counter = 0; // Initialized to 0 by default in Java
    private static String message = "Hello, TAL!"; // Initialized as per TAL

    // LITERAL constants from TAL
    // Translated to static final fields in Java
    private static final int MAX_ITEMS = 100;
    private static final int MIN_ITEMS = 1; // Note: min_items is declared but not used in the TAL code

    /**
     * Simulates the TAL DEFINE print_proc = puts(message); #
     * Assumes 'puts' is equivalent to printing the global 'message' variable to standard output.
     */
    private static void printProc() {
        // In a more complex scenario, this might call a GuardianInterface method
        // or a more sophisticated output handler.
        System.out.println(message);
    }

    /**
     * Simulates the TAL PROC main MAIN;
     * Contains the main execution logic.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Procedure body local variables
        int i;
        int j;
        int errorCode; // Translated from error_code
        String outputBuffer; // Translated from output_buffer STRING(80)
        int localVar; // Translated from local_var declared mid-procedure

        // --- Start of executable statements ---
        i = 1;
        j = 10; // Note: j is assigned but not used later in the TAL code

        // output_buffer ':=' "Starting program...";
        outputBuffer = "Starting program...";
        // print_proc;
        printProc();

        // WHILE i < max_items DO ... END;
        while (i < MAX_ITEMS) {
            // BEGIN (inside WHILE)
            // IF i > 50 THEN output_buffer ':=' "More than halfway through";
            if (i > 50) {
                outputBuffer = "More than halfway through";
                // Optional: Log or print the change in outputBuffer if needed for verification
                // logger.debug("outputBuffer updated: {}", outputBuffer);
            }
            // counter := counter + 1;
            counter++;
            // i := i + 1;
            i++;
            // END (inside WHILE)
        }

        // Declaration inside the body (handled by declaring at method start in Java)
        // INT local_var;
        // local_var := 0;
        localVar = 0; // Note: localVar is assigned but not used later

        // error_code := 0;
        errorCode = 0; // Note: errorCode is assigned but not used later

        // CASE i OF ... END;
        // At this point, the WHILE loop finished when i reached MAX_ITEMS (100)
        switch (i) {
            // 10, 20, 30: counter := counter + 10;
            case 10:
            case 20:
            case 30:
                counter = counter + 10;
                break;
            // 50: counter := counter + 50;
            case 50:
                counter = counter + 50;
                break;
            // OTHERWISE counter := counter + 1;
            default:
                counter = counter + 1;
                break;
        }
        // Since i is 100 here, the default case will execute. counter becomes counter + 1.

        // message ':=' "Program completed!";
        message = "Program completed!";
        // print_proc;
        printProc();

        // --- End of main procedure ---
        logger.info("Program finished. Final counter value: {}", counter);
        // Optional: Print final state for verification
        System.out.println("Final counter value: " + counter);
        System.out.println("Final message value: " + message);
        System.out.println("Final outputBuffer value: " + outputBuffer); // Will be "More than halfway through"
    }
}

