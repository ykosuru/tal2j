package converted;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Translated Java code for the TAL program sample6.tal.txt
 * Demonstrates bit manipulation, expressions, and various operators.
 */
public class Sample6 {

    private static final Logger logger = LogManager.getLogger(BitOperationsDemo.class);

    /**
     * Simulates the TAL PROC bit_operations MAIN;
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        logger.info("Starting bit_operations simulation...");

        // Local variable declarations and initializations
        // INT value := %B10101010; -> Binary 10101010 = Decimal 170
        int value = 0b10101010; // 170 (0xAA)
        // INT mask := %177; -> Octal 177 = Decimal 127
        int mask = 0177; // 127 (0x7F)
        int result = 0; // Initialize result
        int dblResult = 0; // Initialize dbl_result (Java int is 32-bit)

        logger.debug("Initial values: value=0x{} ({}), mask=0x{} ({})",
                     Integer.toHexString(value).toUpperCase(), value,
                     Integer.toHexString(mask).toUpperCase(), mask);

        // --- Bit extraction ---
        logger.debug("--- Bit Extraction ---");
        // result := value.<0:7>; ! Extract high byte (bits 0-7 of 16)
        // In Java int (32-bit), for a simulated 16-bit value, bits 0-7 are shifted.
        result = (value >> 8) & 0xFF; // Shift right 8, mask to get only 8 bits
        logger.debug("value.<0:7>  (High Byte) : 0x{} ({})", Integer.toHexString(result).toUpperCase(), result); // Expected: 0x00 (since high byte of 0xAA is 00) - Corrected: value is 0xAA, high byte is 00 if we consider it 16-bit 0x00AA. If value is just 0xAA, high byte is 0xA. TAL likely treats INT as 16-bit, so 0x00AA. Let's assume value = 0x00AA for 16-bit context.
        value = 0x00AA; // Re-initialize assuming 16-bit context for extraction/deposit
        result = (value >> 8) & 0xFF;
        logger.debug("value=0x00AA; value.<0:7> (High Byte): 0x{} ({})", Integer.toHexString(result).toUpperCase(), result); // Expected: 0x00

        // result := value.<8:15>; ! Extract low byte (bits 8-15 of 16)
        result = value & 0xFF; // Mask lowest 8 bits
        logger.debug("value.<8:15>  (Low Byte)  : 0x{} ({})", Integer.toHexString(result).toUpperCase(), result); // Expected: 0xAA

        // result := value.<3>; ! Extract single bit (bit 3 of 16)
        // Bit 3 is 15 - 3 = 12 positions from the right (LSB=bit 15)
        result = (value >> 12) & 1;
        logger.debug("value.<3>     (Single Bit): {}", result); // Expected: 0 (0x00AA -> 0000 0000 1010 1010, bit 3 is 0)

        // --- Bit deposit ---
        logger.debug("--- Bit Deposit ---");
        value = 0x00AA; // Reset value for deposit tests
        logger.debug("Before deposit: value=0x{}", Integer.toHexString(value).toUpperCase());
        // value.<0:7> := %FF; ! Set high byte (bits 0-7 of 16) to FF
        value = (value & 0x00FF) | (0xFF << 8); // Clear high byte, OR with FF shifted left
        logger.debug("value.<0:7> := 0xFF       : value=0x{}", Integer.toHexString(value).toUpperCase()); // Expected: 0xFFAA

        // value.<10> := 1; ! Set bit 10 (5th bit from right, 15-10=5)
        value = value | (1 << 5);
        logger.debug("value.<10> := 1          : value=0x{}", Integer.toHexString(value).toUpperCase()); // Expected: 0xFFEA (FFAA | 0020 = FFEA)

        // value.<12:15> := 0; ! Clear bits 12-15 (lowest 4 bits)
        value = value & ~0x000F; // AND with mask FFF0
        logger.debug("value.<12:15> := 0       : value=0x{}", Integer.toHexString(value).toUpperCase()); // Expected: 0xFFE0 (FFEA & FFF0 = FFE0)

        // --- Shift operations ---
        logger.debug("--- Shift Operations (value=0x{}) ---", Integer.toHexString(value).toUpperCase()); // value is 0xFFE0
        // result := value << 2; ! Signed left shift
        result = value << 2;
        logger.debug("value << 2  (Signed Left) : 0x{} ({})", Integer.toHexString(result & 0xFFFF).toUpperCase(), (short)result); // Mask to 16 bits for display. FFE0 << 2 = FFB00 -> B00 (signed short -20480)

        // result := value >> 3; ! Signed right shift
        result = value >> 3; // Java >> is arithmetic (sign-extending)
        logger.debug("value >> 3  (Signed Right): 0x{} ({})", Integer.toHexString(result & 0xFFFF).toUpperCase(), (short)result); // FFE0 >> 3 = FFFC (signed short -4)

        // result := value '<<' 4; ! Unsigned left shift
        result = value << 4; // Java << shifts in zeros, same as unsigned
        logger.debug("value '<<' 4 (Unsigned L): 0x{} ({})", Integer.toHexString(result & 0xFFFF).toUpperCase(), result & 0xFFFF); // FFE0 << 4 = FE00

        // result := value '>>' 5; ! Unsigned right shift
        result = value >>> 5; // Java >>> is logical (zero-fill)
        logger.debug("value '>>' 5 (Unsigned R): 0x{} ({})", Integer.toHexString(result).toUpperCase(), result); // FFE0 >>> 5 = 07FF

        // --- Logical operations ---
        value = 0x00AA; // Reset value
        mask = 0x007F;  // Reset mask
        logger.debug("--- Logical Operations (value=0x{}, mask=0x{}) ---", Integer.toHexString(value).toUpperCase(), Integer.toHexString(mask).toUpperCase());
        // result := value LOR mask; ! Logical OR
        result = value | mask;
        logger.debug("value LOR mask : 0x{}", Integer.toHexString(result & 0xFFFF).toUpperCase()); // 00AA | 007F = 00FF

        // result := value LAND mask; ! Logical AND
        result = value & mask;
        logger.debug("value LAND mask: 0x{}", Integer.toHexString(result & 0xFFFF).toUpperCase()); // 00AA & 007F = 002A

        // result := value XOR mask; ! Exclusive OR
        result = value ^ mask;
        logger.debug("value XOR mask : 0x{}", Integer.toHexString(result & 0xFFFF).toUpperCase()); // 00AA ^ 007F = 00D5

        // result := NOT value; ! Logical negation (16-bit)
        result = ~value & 0xFFFF; // Complement and mask to 16 bits
        logger.debug("NOT value      : 0x{}", Integer.toHexString(result).toUpperCase()); // ~00AA = FF55

        // --- Arithmetic operations ---
        value = 170; // Reset to decimal 170 (0xAA)
        logger.debug("--- Arithmetic Operations (value={}) ---", value);
        // result := value + 10;
        result = value + 10;
        logger.debug("value + 10: {}", result); // 170 + 10 = 180

        // result := value - 5;
        result = value - 5;
        logger.debug("value - 5 : {}", result); // 170 - 5 = 165

        // result := value * 3;
        result = value * 3;
        logger.debug("value * 3 : {}", result); // 170 * 3 = 510

        // result := value / 2;
        result = value / 2;
        logger.debug("value / 2 : {}", result); // 170 / 2 = 85

        // --- Unsigned arithmetic operations ---
        // Treat 'value' (170) as a 16-bit unsigned value for these operations
        logger.debug("--- Unsigned Arithmetic (value=170) ---");
        int uValue = value & 0xFFFF; // Ensure we work with the 16-bit pattern

        // result := value '+' 10; ! Unsigned addition (simulated with masking)
        result = (uValue + 10) & 0xFFFF;
        logger.debug("value '+' 10: {}", result); // 170 + 10 = 180

        // result := value '-' 5; ! Unsigned subtraction (simulated with masking)
        result = (uValue - 5) & 0xFFFF;
        logger.debug("value '-' 5 : {}", result); // 170 - 5 = 165

        // dbl_result := value '*' 3; ! Unsigned multiplication -> INT(32)
        // Use long for intermediate to avoid signed overflow during calculation
        long unsignedMul = (uValue & 0xFFFFL) * 3L;
        dblResult = (int) unsignedMul; // Result fits in 32 bits
        logger.debug("value '*' 3 : {}", dblResult); // 170 * 3 = 510

        // result := $UDBL(value) '/' 2; ! Unsigned division
        // $UDBL converts INT to unsigned INT(32). Java int division works correctly for positive numbers.
        result = uValue / 2;
        logger.debug("$UDBL(value) '/' 2: {}", result); // 170 / 2 = 85

        // result := $UDBL(value) '\' 3; ! Modulo division
        result = uValue % 3;
        logger.debug("$UDBL(value) '\\' 3: {}", result); // 170 % 3 = 2

        // --- Complex expressions ---
        value = 170; // Reset
        logger.debug("--- Complex Expressions (value={}) ---", value);
        // result := ((value + 5) * 3 - 7) / 2;
        result = ((value + 5) * 3 - 7) / 2;
        logger.debug("((value + 5) * 3 - 7) / 2: {}", result); // ((170+5)*3 - 7)/2 = (175*3 - 7)/2 = (525-7)/2 = 518/2 = 259

        // result := (value.<0:3> << 4) LOR value.<4:7>;
        // Assumes value = 0x00AA = 0000 0000 1010 1010
        // value.<0:3> -> bits 0-3 -> 0000 -> 0
        // value.<4:7> -> bits 4-7 -> 0000 -> 0
        // result = (0 << 4) | 0 = 0
        // Let's try with value = 0xABCD = 1010 1011 1100 1101
        value = 0xABCD;
        int highNybble = (value >> 12) & 0xF; // bits 0-3 -> 1010 (A)
        int midNybble = (value >> 8) & 0xF;  // bits 4-7 -> 1011 (B)
        result = (highNybble << 4) | midNybble;
        logger.debug("(value.<0:3> << 4) LOR value.<4:7> (value=0xABCD): 0x{}", Integer.toHexString(result).toUpperCase()); // Expected: (A << 4) | B = 0xAB

        // --- Relational expressions ---
        value = 150; // Set value for test
        logger.debug("--- Relational Expressions (value={}) ---", value);
        // IF (value > 100) AND (value < 200) OR (value = 50) THEN result := 1;
        if ((value > 100 && value < 200) || (value == 50)) {
            result = 1;
            logger.debug("Relational IF condition is TRUE, result set to {}", result);
        } else {
            // result remains unchanged if condition is false
            logger.debug("Relational IF condition is FALSE, result remains {}", result);
        }

        // --- Assignment expression form ---
        value = 100; // Reset for test
        logger.debug("--- Assignment Expression (value={}) ---", value);
        // IF (result := value * 2) > 300 THEN result := 300;
        if ((result = value * 2) > 300) { // result becomes 200, condition is false
            logger.debug("Assignment IF condition TRUE (result={})", result);
            result = 300;
        } else {
             logger.debug("Assignment IF condition FALSE (result={})", result);
        }
        logger.debug("After Assignment IF: result={}", result); // Expected: 200

        // --- CASE expression form ---
        value = 2; // Set for test
        logger.debug("--- CASE Expression (value={}) ---", value);
        // result := CASE value OF BEGIN ... END;
        // Using Java 14+ switch expression for conciseness
        result = switch (value) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            default -> -1;
        };
        /* // Traditional switch equivalent:
        int tempResultCase;
        switch (value) {
            case 0: tempResultCase = 0; break;
            case 1: tempResultCase = 1; break;
            case 2: tempResultCase = 2; break;
            default: tempResultCase = -1; break;
        }
        result = tempResultCase;
        */
        logger.debug("After CASE expression: result={}", result); // Expected: 2

        // --- IF-THEN-ELSE expression form ---
        value = 150; // Set for test
        logger.debug("--- IF-THEN-ELSE Expression (value={}) ---", value);
        // result := IF value > 100 THEN value - 100 ELSE value;
        result = (value > 100) ? (value - 100) : value;
        logger.debug("After IF-THEN-ELSE expression: result={}", result); // Expected: 150 - 100 = 50

        logger.info("bit_operations simulation finished. Final result = {}", result);
        System.out.println("Final Result: " + result);
    }
}

