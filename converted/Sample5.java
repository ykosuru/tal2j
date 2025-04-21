package converted;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Objects; // For checking nulls if we used wrappers for optional params

/**
 * Wrapper class for simulating pass-by-reference for TAL INT parameters.
 */
class IntWrapper {
    public int value;
    public IntWrapper() { this.value = 0; }
    public IntWrapper(int value) { this.value = value; }
    @Override public String toString() { return Integer.toString(value); }
}

/**
 * Translated Java code for the TAL program sample5.tal.txt
 * Demonstrates procedures, subprocedures, parameter passing, entry points, and forward declarations.
 */
public class Sample5 {

    private static final Logger logger = LogManager.getLogger(ProcedureDemo.class);

    // Note: No global variables declared in the TAL sample outside procedures.

    // --- Function (Typed Procedure) ---

    /**
     * Simulates TAL: INT PROC calculate_sum(a, b);
     * Returns the sum of two integers.
     * @param a First integer (pass-by-value).
     * @param b Second integer (pass-by-value).
     * @return The sum of a and b.
     */
    public static int calculateSum(int a, int b) {
        logger.trace("Entering calculateSum({}, {})", a, b);
        // RETURN a + b;
        int sum = a + b;
        logger.trace("Exiting calculateSum, returning {}", sum);
        return sum;
    }

    // --- Procedure with Value and Reference Parameters ---

    /**
     * Simulates TAL: PROC modify_values(in_val, out_ref);
     * Doubles the input value and returns it via the reference parameter.
     * @param inVal Input value (pass-by-value).
     * @param outRef Output reference parameter (simulated pass-by-reference).
     */
    public static void modifyValues(int inVal, IntWrapper outRef) {
        logger.trace("Entering modifyValues(inVal={}, outRef={})", inVal, outRef.value);
        // INT local_val := in_val * 2;
        int localVal = inVal * 2;
        // out_ref := local_val;
        outRef.value = localVal;
        logger.trace("Exiting modifyValues, outRef set to {}", outRef.value);
    }

    // --- Procedure with Variable Parameters ---

    /**
     * Simulates TAL: PROC optional_params(a, b, c) VARIABLE;
     * This version handles the case where 'c' is provided.
     * Simulates the logic using standard Java parameters. The check for presence
     * is handled by which overloaded method is called.
     * @param a Parameter 'a'.
     * @param b Parameter 'b'.
     * @param c Parameter 'c' (passed by reference for modification).
     */
    public static void optionalParams(int a, int b, IntWrapper c) {
        logger.trace("Entering optionalParams(a={}, b={}, c={}) - c provided", a, b, c.value);
        // IF $PARAM(a) AND $PARAM(b) THEN c := a + b;
        // Since all params are provided in this overload, we execute the logic.
        c.value = a + b;
        logger.trace("Exiting optionalParams (c provided), c set to {}", c.value);
    }

    /**
     * Simulates TAL: PROC optional_params(a, b, c) VARIABLE;
     * This version handles the case where 'c' is *not* provided.
     * It returns the calculated value or default.
     * @param a Parameter 'a'.
     * @param b Parameter 'b'.
     * @return The calculated value for 'c' (a + b) or the default (0).
     */
    public static int optionalParams(int a, int b) {
        logger.trace("Entering optionalParams(a={}, b={}) - c not provided", a, b);
        // IF NOT $PARAM(c) THEN c := 0;
        int c_simulated = 0; // Simulate the default value when c is not passed.

        // IF $PARAM(a) AND $PARAM(b) THEN c := a + b;
        // Since a and b are provided, calculate the sum.
        c_simulated = a + b;

        logger.trace("Exiting optionalParams (c not provided), calculated c = {}", c_simulated);
        return c_simulated; // Return the calculated value
    }


    // --- Main Procedure with Subprocedures ---

    /**
     * Simulates TAL: PROC procedure_demo MAIN;
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        logger.info("Starting procedure_demo simulation...");

        // Local variables for main procedure
        int value1 = 10;
        int value2 = 20;
        // Use IntWrapper for result because subprocedures modify it
        IntWrapper resultWrapper = new IntWrapper(0);
        logger.debug("Initial values: value1={}, value2={}, result={}", value1, value2, resultWrapper.value);

        // Call function and use return value
        // result := calculate_sum(value1, value2);
        resultWrapper.value = calculateSum(value1, value2);
        logger.debug("After calculate_sum: result={}", resultWrapper.value); // Expected: 30

        // Call procedure with reference parameter
        // CALL modify_values(result, value1);
        // Need to wrap value1 for pass-by-reference simulation
        IntWrapper value1Wrapper = new IntWrapper(value1);
        modifyValues(resultWrapper.value, value1Wrapper);
        value1 = value1Wrapper.value; // Update local value1 from wrapper
        logger.debug("After modify_values: value1={}, result={}", value1, resultWrapper.value); // Expected: value1=60, result=30

        // Call procedure with optional parameters
        // CALL optional_params(5, 10); ! c is not provided
        int optionalResult1 = optionalParams(5, 10);
        logger.debug("After optional_params(5, 10): returned c={}", optionalResult1); // Expected: 15 (used for calculation, not assigned back)

        // CALL optional_params(5, 10, 15); ! All parameters provided
        // Need a wrapper for the 'c' parameter to receive the modification
        IntWrapper cWrapper = new IntWrapper(15); // Initial value doesn't matter here as it gets overwritten
        optionalParams(5, 10, cWrapper);
        logger.debug("After optional_params(5, 10, cWrapper): cWrapper.value={}", cWrapper.value); // Expected: 15

        // --- Subprocedure Calls ---
        // Note: Subprocedures access 'result' and 'value1' from the outer scope.
        // In Java, we pass these as arguments. 'result' needs to be passed by reference.

        // Call subprocedure
        // CALL local_computation(5, 6);
        localComputation(5, 6, value1, resultWrapper); // Pass value1 and resultWrapper
        logger.debug("After local_computation: result={}", resultWrapper.value); // Expected: 30 + 60 = 90

        // Call entry point
        // CALL alternate_entry;
        localComputationAlternateEntry(resultWrapper); // Pass resultWrapper
        logger.debug("After alternate_entry: result={}", resultWrapper.value); // Expected: 90 * 2 = 180

        // Call the forward-declared subprocedure
        // CALL forward_sub(100);
        forwardSub(100, resultWrapper); // Pass resultWrapper
        logger.debug("After forward_sub: result={}", resultWrapper.value); // Expected: 100 + 180 = 280

        logger.info("procedure_demo simulation finished. Final result={}", resultWrapper.value);
        System.out.println("Final Result: " + resultWrapper.value);
    }

    // --- Subprocedure Implementations ---

    /**
     * Simulates TAL: SUBPROC local_computation(x, y);
     * @param x Parameter x.
     * @param y Parameter y.
     * @param procValue1 Value of value1 from the calling procedure's scope.
     * @param procResultWrapper Reference to result from the calling procedure's scope.
     */
    private static void localComputation(int x, int y, int procValue1, IntWrapper procResultWrapper) {
        logger.trace("Entering localComputation(x={}, y={}, procValue1={}, procResult={})", x, y, procValue1, procResultWrapper.value);
        // Local to the subprocedure
        // INT sublocal := x * y;
        int sublocal = x * y;
        logger.trace("Sublocal calculated: {}", sublocal);

        // Can access the procedure's variables (passed as parameters)
        // result := sublocal + value1;
        procResultWrapper.value = sublocal + procValue1;
        logger.trace("Exiting localComputation, procResult set to {}", procResultWrapper.value);
        // Execution continues to alternate_entry logic if called directly,
        // but here we assume separate calls for entry points.
    }

    /**
     * Simulates TAL: ENTRY alternate_entry; within local_computation.
     * @param procResultWrapper Reference to result from the calling procedure's scope.
     */
    private static void localComputationAlternateEntry(IntWrapper procResultWrapper) {
        logger.trace("Entering localComputationAlternateEntry(procResult={})", procResultWrapper.value);
        // Code following the ENTRY label
        // result := result * 2;
        procResultWrapper.value = procResultWrapper.value * 2;
        logger.trace("Exiting localComputationAlternateEntry, procResult set to {}", procResultWrapper.value);
    }

    /**
     * Simulates TAL: SUBPROC forward_sub(z);
     * This is the actual implementation corresponding to the FORWARD declaration.
     * @param z Parameter z.
     * @param procResultWrapper Reference to result from the calling procedure's scope.
     */
    private static void forwardSub(int z, IntWrapper procResultWrapper) {
        logger.trace("Entering forwardSub(z={}, procResult={})", z, procResultWrapper.value);
        // BEGIN
        // result := z + result;
        procResultWrapper.value = z + procResultWrapper.value;
        // END;
        logger.trace("Exiting forwardSub, procResult set to {}", procResultWrapper.value);
    }
}

