package converted;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents the TAL STRUCT address (substructure within EmployeeTemplate).
 * Provides static methods to access fields within a larger ByteBuffer representing EmployeeTemplate.
 */
class Address {
    // Offsets relative to the start of the EmployeeTemplate structure
    static final int STREET_OFFSET = 50; // STRING[0:19] size 20
    static final int STREET_LENGTH = 20;
    static final int CITY_OFFSET = 70;   // STRING[0:14] size 15
    static final int CITY_LENGTH = 15;
    static final int STATE_OFFSET = 85;  // STRING[0:1] size 2
    static final int STATE_LENGTH = 2;
    static final int ZIP_OFFSET = 87;    // STRING[0:4] size 5
    static final int ZIP_LENGTH = 5;
    // Total size of Address substructure = 20 + 15 + 2 + 5 = 42 bytes

    // Helper to read string field from the main buffer at the correct sub-offset
    private static String getStringField(ByteBuffer buffer, int fieldOffset, int fieldLength) {
        byte[] bytes = new byte[fieldLength];
        buffer.position(fieldOffset);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }

    // Helper to write string field to the main buffer at the correct sub-offset
    private static void setStringField(ByteBuffer buffer, int fieldOffset, int fieldLength, String value) {
        byte[] bytes = new byte[fieldLength];
        Arrays.fill(bytes, (byte) ' '); // Pad with spaces
        if (value != null) {
            byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, fieldLength));
        }
        buffer.position(fieldOffset);
        buffer.put(bytes);
    }

    // Static accessors operating on the EmployeeTemplate buffer
    public static String getStreet(ByteBuffer buffer) { return getStringField(buffer, STREET_OFFSET, STREET_LENGTH); }
    public static void setStreet(ByteBuffer buffer, String value) { setStringField(buffer, STREET_OFFSET, STREET_LENGTH, value); }

    public static String getCity(ByteBuffer buffer) { return getStringField(buffer, CITY_OFFSET, CITY_LENGTH); }
    public static void setCity(ByteBuffer buffer, String value) { setStringField(buffer, CITY_OFFSET, CITY_LENGTH, value); }

    public static String getState(ByteBuffer buffer) { return getStringField(buffer, STATE_OFFSET, STATE_LENGTH); }
    public static void setState(ByteBuffer buffer, String value) { setStringField(buffer, STATE_OFFSET, STATE_LENGTH, value); }

    public static String getZip(ByteBuffer buffer) { return getStringField(buffer, ZIP_OFFSET, ZIP_LENGTH); }
    public static void setZip(ByteBuffer buffer, String value) { setStringField(buffer, ZIP_OFFSET, ZIP_LENGTH, value); }
}

/**
 * Represents the TAL STRUCT employee_template (*).
 * Uses a ByteBuffer to simulate memory layout.
 */
class EmployeeTemplate {
    private static final int EMP_ID_OFFSET = 0;        // INT size 2
    private static final int NAME_OFFSET = 2;          // STRING[0:29] size 30
    private static final int NAME_LENGTH = 30;
    private static final int DEPT_OFFSET = 32;         // STRING[0:9] size 10
    private static final int DEPT_LENGTH = 10;
    private static final int SALARY_OFFSET = 42;       // FIXED(2) size 8 (Assuming word alignment starts at 42)
    // Address substructure starts at offset 50 (SALARY_OFFSET + 8)
    // Total size = 50 (start of address) + 42 (size of address) = 92 bytes
    public static final int BYTE_LENGTH = 92;

    private ByteBuffer buffer;

    // Constructor for creating a new instance (like a direct STRUCT)
    public EmployeeTemplate() {
        this.buffer = ByteBuffer.allocate(BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        // Initialize buffer if needed (e.g., fill strings with spaces)
        Arrays.fill(buffer.array(), NAME_OFFSET, NAME_OFFSET + NAME_LENGTH, (byte) ' ');
        Arrays.fill(buffer.array(), DEPT_OFFSET, DEPT_OFFSET + DEPT_LENGTH, (byte) ' ');
        Address.setStreet(buffer, ""); // Initialize address fields
        Address.setCity(buffer, "");
        Address.setState(buffer, "");
        Address.setZip(buffer, "");
    }

    // Constructor to wrap an existing buffer slice (for array elements or pointers)
    public EmployeeTemplate(ByteBuffer buffer) {
        if (buffer == null || buffer.capacity() < BYTE_LENGTH) {
             throw new IllegalArgumentException("Provided buffer is null or too small for EmployeeTemplate");
        }
        // Use slice to avoid modifying the original buffer's position/limit unintentionally
        this.buffer = buffer.slice(buffer.position(), BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    // --- Getters and Setters ---
    public int getEmpId() { return buffer.getShort(EMP_ID_OFFSET); }
    public void setEmpId(int value) { buffer.putShort(EMP_ID_OFFSET, (short) value); }

    public String getName() {
        byte[] bytes = new byte[NAME_LENGTH];
        buffer.position(NAME_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setName(String value) {
        byte[] bytes = new byte[NAME_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        if (value != null) {
            byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, NAME_LENGTH));
        }
        buffer.position(NAME_OFFSET);
        buffer.put(bytes);
    }

    public String getDept() {
        byte[] bytes = new byte[DEPT_LENGTH];
        buffer.position(DEPT_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setDept(String value) {
        byte[] bytes = new byte[DEPT_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
         if (value != null) {
            byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, DEPT_LENGTH));
        }
        buffer.position(DEPT_OFFSET);
        buffer.put(bytes);
    }

    public BigDecimal getSalary() {
        long fixedValue = buffer.getLong(SALARY_OFFSET);
        return BigDecimal.valueOf(fixedValue, 2); // FIXED(2) -> scale 2
    }
    public void setSalary(BigDecimal value) {
        long fixedValue = value.setScale(2, RoundingMode.HALF_UP).unscaledValue().longValue();
        buffer.putLong(SALARY_OFFSET, fixedValue);
    }

    // --- Accessors for Address Substructure ---
    public String getStreet() { return Address.getStreet(this.buffer); }
    public void setStreet(String value) { Address.setStreet(this.buffer, value); }

    public String getCity() { return Address.getCity(this.buffer); }
    public void setCity(String value) { Address.setCity(this.buffer, value); }

    public String getState() { return Address.getState(this.buffer); }
    public void setState(String value) { Address.setState(this.buffer, value); }

    public String getZip() { return Address.getZip(this.buffer); }
    public void setZip(String value) { Address.setZip(this.buffer, value); }

    // Method to get the underlying buffer
    public ByteBuffer getBuffer() {
        buffer.position(0); // Reset position before returning
        return buffer;
    }

    // Static method to get the byte length of the structure
    public static int getByteLength() {
        return BYTE_LENGTH;
    }

    @Override
    public String toString() {
        return "EmployeeTemplate{" +
               "empId=" + getEmpId() +
               ", name='" + getName() + '\'' +
               ", dept='" + getDept() + '\'' +
               ", salary=" + getSalary() +
               ", address={street='" + getStreet() + "', city='" + getCity() + "', state='" + getState() + "', zip='" + getZip() + "'}" +
               '}';
    }
}

/**
 * Represents the TAL STRUCT s with redefinition.
 */
class S_Structure {
    private static final int A_OFFSET = 0;      // INT[0:2] size 6 bytes
    private static final int A_LENGTH_BYTES = 6;
    private static final int B_OFFSET = 6;      // STRING[0:5] size 6 bytes
    private static final int B_LENGTH = 6;
    // C redefines A. C[0] is INT(32) at offset 0, size 4 bytes.
    private static final int C_OFFSET = 0;
    public static final int BYTE_LENGTH = 12;   // Total size 6 + 6 = 12

    private ByteBuffer buffer;

    public S_Structure() {
        this.buffer = ByteBuffer.allocate(BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    public S_Structure(ByteBuffer buffer) {
         if (buffer == null || buffer.capacity() < BYTE_LENGTH) {
             throw new IllegalArgumentException("Provided buffer is null or too small for S_Structure");
         }
        this.buffer = buffer.slice(buffer.position(), BYTE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    }

    // Accessors for 'a' (INT array)
    public int getA(int index) {
        if (index < 0 || index > 2) throw new IndexOutOfBoundsException("Index " + index + " out of bounds for 'a'");
        return buffer.getShort(A_OFFSET + index * 2); // INT is 2 bytes
    }
    public void setA(int index, int value) {
        if (index < 0 || index > 2) throw new IndexOutOfBoundsException("Index " + index + " out of bounds for 'a'");
        buffer.putShort(A_OFFSET + index * 2, (short) value);
    }

    // Accessors for 'b' (STRING array)
    public String getB() {
        byte[] bytes = new byte[B_LENGTH];
        buffer.position(B_OFFSET);
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }
    public void setB(String value) {
        byte[] bytes = new byte[B_LENGTH];
        Arrays.fill(bytes, (byte) ' ');
        if (value != null) {
            byte[] valueBytes = value.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(valueBytes, 0, bytes, 0, Math.min(valueBytes.length, B_LENGTH));
        }
        buffer.position(B_OFFSET);
        buffer.put(bytes);
    }

    // Accessors for 'c' (INT(32) redefinition of 'a')
    public int getC(int index) {
        if (index != 0) throw new IndexOutOfBoundsException("Index " + index + " out of bounds for 'c'");
        // Reads 4 bytes starting at offset 0 (covers a[0] and a[1])
        return buffer.getInt(C_OFFSET + index * 4);
    }
    public void setC(int index, int value) {
        if (index != 0) throw new IndexOutOfBoundsException("Index " + index + " out of bounds for 'c'");
        // Writes 4 bytes starting at offset 0
        buffer.putInt(C_OFFSET + index * 4, value);
    }

    public ByteBuffer getBuffer() {
        buffer.position(0);
        return buffer;
    }

     public static int getByteLength() {
        return BYTE_LENGTH;
    }
}


/**
 * Translated Java code for the TAL program sample3.tal.txt
 */
public class Sample3 {

    private static final Logger logger = LogManager.getLogger(StructureDemo.class);

    // Simulate global storage for the indirect array employee_db
    // Allocate one large buffer to hold all 100 potential records
    private static final int MAX_EMPLOYEES = 100;
    private static ByteBuffer employeeDbBuffer = ByteBuffer.allocate(MAX_EMPLOYEES * EmployeeTemplate.getByteLength())
                                                        .order(ByteOrder.LITTLE_ENDIAN);

    // Simulate the structure pointer emp_ptr using an offset into employeeDbBuffer
    private static int empPtrOffset = -1; // -1 indicates not pointing anywhere initially

    /**
     * Helper method to get a view (slice) of the buffer for a specific employee record.
     * @param index The index of the employee record (0-99).
     * @return A ByteBuffer slice representing the record, or null if index is invalid.
     */
    private static ByteBuffer getEmployeeBufferSlice(int index) {
        if (index < 0 || index >= MAX_EMPLOYEES) {
            logger.error("Invalid index {} for employee_db access.", index);
            return null;
        }
        int offset = index * EmployeeTemplate.getByteLength();
        // Return a slice to avoid modifying the main buffer's position/limit
        return employeeDbBuffer.slice(offset, EmployeeTemplate.getByteLength()).order(ByteOrder.LITTLE_ENDIAN);
    }

     /**
     * Helper method to access an employee record via its index.
     * Returns an EmployeeTemplate object wrapping the relevant buffer slice.
     * @param index The index of the employee record (0-99).
     * @return An EmployeeTemplate object, or null if index is invalid.
     */
    private static EmployeeTemplate getEmployeeRecord(int index) {
        ByteBuffer slice = getEmployeeBufferSlice(index);
        return (slice != null) ? new EmployeeTemplate(slice) : null;
    }

    /**
     * Simulates the TAL PROC structure_demo MAIN;
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("Starting structure_demo simulation...");

        // Direct structure simulation
        // STRUCT person (employee_template);
        EmployeeTemplate person = new EmployeeTemplate();
        logger.debug("Created direct structure 'person'");

        // Initialize structure 'person'
        // person.emp_id := 1001;
        person.setEmpId(1001);
        // person.name ':=' "John Smith";
        person.setName("John Smith");
        // person.dept ':=' "Engineering";
        person.setDept("Engineering");
        // person.salary := 5000.00F;
        person.setSalary(new BigDecimal("5000.00"));
        // person.address.street ':=' "123 Main St";
        person.setStreet("123 Main St");
        // person.address.city ':=' "Anytown";
        person.setCity("Anytown");
        // person.address.state ':=' "CA";
        person.setState("CA");
        // person.address.zip ':=' "94086";
        person.setZip("94086");
        logger.debug("Initialized 'person': {}", person);

        // Access structure elements in database (employeeDbBuffer)
        // employee_db[0] := person;
        // Copy bytes from person's buffer to the main db buffer at offset 0
        ByteBuffer personBuffer = person.getBuffer();
        employeeDbBuffer.position(0);
        employeeDbBuffer.put(personBuffer);
        logger.debug("Copied 'person' to employee_db[0]");

        // employee_db[1].emp_id := 1002;
        ByteBuffer emp1Buffer = getEmployeeBufferSlice(1);
        if (emp1Buffer != null) {
            emp1Buffer.putShort(0, (short) 1002); // Directly set emp_id at offset 0 within the slice

            // employee_db[1].name ':=' "Jane Doe";
            EmployeeTemplate emp1 = new EmployeeTemplate(emp1Buffer); // Wrap slice to use setters
            emp1.setName("Jane Doe");

            // employee_db[1].salary := 6000.00F;
            emp1.setSalary(new BigDecimal("6000.00"));
            logger.debug("Initialized employee_db[1]: {}", emp1);
        }

        // Use structure pointer
        // @emp_ptr := @employee_db[0];
        empPtrOffset = 0 * EmployeeTemplate.getByteLength(); // Offset of employee_db[0]
        logger.debug("Set emp_ptr to offset {}", empPtrOffset);

        // emp_ptr.salary := 5250.00F;
        if (empPtrOffset != -1) {
             // Access the main buffer at the pointer offset
             employeeDbBuffer.putLong(empPtrOffset + 42, // Salary offset within the record
                 new BigDecimal("5250.00").setScale(2, RoundingMode.HALF_UP).unscaledValue().longValue());
             logger.debug("Updated salary via emp_ptr for record at offset {}", empPtrOffset);
             // Verify the change in employee_db[0]
             EmployeeTemplate emp0 = getEmployeeRecord(0);
             logger.debug("employee_db[0] after pointer update: {}", emp0);
        }

        // Structure with redefinition simulation
        // STRUCT s; ... END;
        S_Structure s = new S_Structure();
        s.setA(0, 1);
        s.setA(1, 2);
        s.setA(2, 3);
        s.setB("XYZ");
        logger.debug("Initialized 's': a=[{},{},{}], b='{}'", s.getA(0), s.getA(1), s.getA(2), s.getB());

        // Access redefined field 'c'
        int cValue = s.getC(0); // Reads bytes 0-3 (a[0] and a[1]) as INT(32)
        logger.debug("Value of s.c[0] (redefined from s.a[0], s.a[1]): {}", cValue);
        // Modify via 'c'
        s.setC(0, 0xABCD1234); // Set 4 bytes
        logger.debug("Modified 's' via c[0]: a=[{},{},{}], b='{}'", s.getA(0), s.getA(1), s.getA(2), s.getB());
        // Note: a[0] and a[1] will reflect the bytes of 0xABCD1234 according to endianness.

        // Move operation with structure
        // employee_db[2] ':=' employee_db[0] FOR $LEN(employee_db[0]) / 2;
        int lenToCopy = EmployeeTemplate.getByteLength() / 2;
        int sourceOffset = 0 * EmployeeTemplate.getByteLength();
        int destOffset = 2 * EmployeeTemplate.getByteLength();

        if (destOffset + lenToCopy <= employeeDbBuffer.capacity()) {
            // Create a temporary buffer to hold the source data
            byte[] tempCopy = new byte[lenToCopy];
            employeeDbBuffer.position(sourceOffset);
            employeeDbBuffer.get(tempCopy);

            // Write the copied data to the destination
            employeeDbBuffer.position(destOffset);
            employeeDbBuffer.put(tempCopy);
            logger.debug("Copied {} bytes from employee_db[0] to employee_db[2]", lenToCopy);

            // Verify the partial copy
             EmployeeTemplate emp2 = getEmployeeRecord(2);
             logger.debug("employee_db[2] after partial move: {}", emp2); // Note: toString might look odd due to partial data
        } else {
             logger.error("Partial move destination out of bounds.");
        }


        logger.info("structure_demo simulation finished.");
    }
}
