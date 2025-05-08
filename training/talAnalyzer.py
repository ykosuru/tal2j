import os
import re
import glob
from collections import defaultdict
from openai import OpenAI
import traceback

# Configuration
TAL_EXTENSION = "*.tal"
OUTPUT_FILE = "tal_code_review.md"
OPENAI_MODEL = "gpt-4"  # Adjust based on available model

# Initialize Open AI client
os.environ["OPENAI_API_KEY"]="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"
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# Regular expression patterns for TAL constructs
PATTERNS = {
    "io_operations": {
        "open": re.compile(r"\bOPEN\s+\w+\s*(?:,|\()", re.IGNORECASE),
        "read": re.compile(r"\bREAD\s+\w+\s*(?:,|\()", re.IGNORECASE),
        "write": re.compile(r"\bWRITE\s+\w+\s*(?:,|\()", re.IGNORECASE),
        "close": re.compile(r"\bCLOSE\s+\w+\s*(?:,|\()", re.IGNORECASE),
        "term_write_int": re.compile(r"\bterm_write_int\s*\(", re.IGNORECASE),
    },
    "procedures": re.compile(r"\bPROC\s+(\w+)\s*(?:\([^)]*\))?\s*(?:;|\{)", re.IGNORECASE),
    "functions": re.compile(r"\b(FIXED|INT| AtemSTRING)\s*FUNCTION\s+(\w+)\s*(?:\([^)]*\))?\s*(?:;|\{)", re.IGNORECASE),
    "control_structures": {
        "for_loop": re.compile(r"\bFOR\s+\w+\s*:=\s*[^;]+TO\s+[^;]+DO", re.IGNORECASE),
        "while_loop": re.compile(r"\bWHILE\s+[^;]+DO", re.IGNORECASE),
        "if_then_else": re.compile(r"\bIF\s+[^;]+THEN(?:\s*ELSE)?", re.IGNORECASE),
        "onerror": re.compile(r"\bONERROR\s+[^;]+DO", re.IGNORECASE),
    },
    "data_types": {
        "int_8": re.compile(r"\bINT\s*\(\s*8\s*\)\s+\w+", re.IGNORECASE),
        "int_16": re.compile(r"\bINT\s*\(\s*16\s*\)\s+\w+", re.IGNORECASE),
        "int_32": re.compile(r"\bINT\s*\(\s*32\s*\)\s+\w+", re.IGNORECASE),
        "int_64": re.compile(r"\bINT\s*\(\s*64\s*\)\s+\w+", re.IGNORECASE),
        "string": re.compile(r"\bSTRING\s+\w+\s*\[\s*\d+\s*\]", re.IGNORECASE),
    },
    "external_dependencies": {
        "extdecs": re.compile(r"\bEXTDECS\s*\(\s*(\w+)\s*\)", re.IGNORECASE),
        "system_calls": re.compile(r"\b(MYTERM|NUMOUT|FILEINFO|SEND|RECEIVE)\s*\(", re.IGNORECASE),
    },
    "sync_operations": {
        "wait": re.compile(r"\bWAIT\s*\(", re.IGNORECASE),
        "transaction": re.compile(r"\b(BEGINTRANSACTION|COMMITTRANSACTION|ABORTTRANSACTION)\s*(?:;|\()", re.IGNORECASE),
    },
    "structs": re.compile(r"\bSTRUCT\s+(\w+)\s*\(\s*\w+\s*\)\s*;\s*BEGIN\s*((?:[^;]+;)+)\s*END\s*;", re.IGNORECASE),
}

def extract_struct_members(struct_body):
    """Extract member names and types from a struct's body."""
    members = []
    lines = struct_body.split(";")
    for line in lines:
        line = line.strip()
        if not line:
            continue
        # Match simple member declarations (e.g., INT x, STRING y[10])
        member_match = re.match(r"\s*(INT\s*\(\s*\d+\s*\)|INT|STRING\s*\[\s*\d+\s*\]|STRING)\s+(\w+)", line, re.IGNORECASE)
        if member_match:
            members.append({"type": member_match.group(1), "name": member_match.group(2)})
    return members

def get_procedure_code(lines, proc_name, proc_line_num):
    """Extract the full code block of a procedure starting at proc_line_num."""
    proc_code = []
    in_proc = False
    for i, line in enumerate(lines[proc_line_num-1:], proc_line_num):
        line = line.strip()
        if not in_proc and re.match(PATTERNS["procedures"], line):
            in_proc = True
        if in_proc:
            proc_code.append(line)
            if "END" in line.upper():
                break
    return "\n".join(proc_code)

def analyze_procedure_with_llm(proc_name, proc_code, filename, structs, io_ops, dependencies):
    """Use Open AI LLM to generate a detailed description of the procedure's functionality."""
    context = f"""
        File: {filename}
        Procedure: {proc_name}
        Code:
        {proc_code}

        Relevant Structs:
        {'\n'.join([f"- {s['name']}: {', '.join([f'{m['name']} ({m['type']})' for m in s['members']])}" for s in structs]) or 'None'}

        I/O Operations:
        {'\n'.join([f"- {op}: {', '.join(lines)}" for op, lines in io_ops.items() if lines]) or 'None'}

        Dependencies:
        {'\n'.join([f"- {dep}: {', '.join(lines)}" for dep, lines in dependencies.items() if lines]) or 'None'}

        Task: Provide a detailed description of the procedure's functionality, including its purpose, inputs, outputs, and key operations, in natural language.
    """
    try:
        response = openai_client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": "You are a code analysis expert specializing in TAL (Transaction Application Language) for Tandem systems. Generate clear, detailed descriptions of procedure functionality based on provided code and context."},
                {"role": "user", "content": context}
            ],
            max_tokens=500
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        return f"Error analyzing procedure {proc_name}: {str(e)}\n{traceback.format_exc()}"

def analyze_tal_file(filepath, lines=None):
    """Analyze a single TAL file and return a dictionary of findings."""
    findings = {
        "io_operations": defaultdict(list),
        "procedures": [],
        "procedure_details": {},
        "functions": [],
        "control_structures": defaultdict(list),
        "data_types": defaultdict(list),
        "external_dependencies": defaultdict(list),
        "sync_operations": defaultdict(list),
        "structs": [],
        "execution_flow": [],
    }
    
    if lines is None:
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                lines = f.readlines()
        except UnicodeDecodeError:
            with open(filepath, "r", encoding="latin-1") as f:
                lines = f.readlines()
    
    current_context = None
    for line_num, line in enumerate(lines, 1):
        line = line.strip()
        if not line or line.startswith("!"):  # Skip empty lines and comments
            continue

        # Track execution flow
        proc_match = PATTERNS["procedures"].search(line)
        func_match = PATTERNS["functions"].search(line)
        if proc_match:
            current_context = f"Procedure {proc_match.group(1)}"
            findings["execution_flow"].append(f"{current_context} starts at line {line_num}")
        elif func_match:
            current_context = f"Function {func_match.group(2)}"
            findings["execution_flow"].append(f"{current_context} starts at line {line_num}")
        elif "END" in line.upper() and current_context:
            findings["execution_flow"].append(f"{current_context} ends at line {line_num}")
            current_context = None

        # Match I/O operations
        for op, pattern in PATTERNS["io_operations"].items():
            if pattern.search(line):
                findings["io_operations"][op].append(f"Line {line_num}: {line.strip()}")

        # Match procedures
        if proc_match:
            proc_name = proc_match.group(1)
            findings["procedures"].append(f"{proc_name} at line {line_num}")
            # Extract and analyze procedure code with LLM
            proc_code = get_procedure_code(lines, proc_name, line_num)
            findings["procedure_details"][proc_name] = analyze_procedure_with_llm(
                proc_name, proc_code, os.path.basename(filepath),
                findings["structs"], findings["io_operations"], findings["external_dependencies"]
            )

        # Match functions
        if func_match:
            findings["functions"].append(f"{func_match.group(2)} (Type: {func_match.group(1)}) at line {line_num}")

        # Match control structures
        for ctrl, pattern in PATTERNS["control_structures"].items():
            if pattern.search(line):
                findings["control_structures"][ctrl].append(f"Line {line_num}: {line.strip()}")

        # Match data types
        for dtype, pattern in PATTERNS["data_types"].items():
            if pattern.search(line):
                findings["data_types"][dtype].append(f"Line {line_num}: {line.strip()}")

        # Match external dependencies
        for dep, pattern in PATTERNS["external_dependencies"].items():
            if pattern.search(line):
                findings["external_dependencies"][dep].append(f"Line {line_num}: {line.strip()}")

        # Match synchronization operations
        for sync, pattern in PATTERNS["sync_operations"].items():
            if pattern.search(line):
                findings["sync_operations"][sync].append(f"Line {line_num}: {line.strip()}")

        # Match structs
        struct_match = PATTERNS["structs"].search("\n".join(lines[max(0, line_num-10):line_num+10]))
        if struct_match and struct_match.start() <= line_num <= struct_match.end():
            struct_name = struct_match.group(1)
            struct_body = struct_match.group(2)
            members = extract_struct_members(struct_body)
            findings["structs"].append({"name": struct_name, "members": members})

    return findings

def generate_report(folder_path):
    """Generate a Markdown report for all TAL files in the folder."""
    report = ["# TAL Source Code Analysis Report", f"Generated on {os.path.basename(folder_path)}", ""]
    
    tal_files = glob.glob(os.path.join(folder_path, TAL_EXTENSION))
    if not tal_files:
        report.append("No TAL files found in the specified folder.")
        return report

    for tal_file in tal_files:
        filename = os.path.basename(tal_file)
        report.append(f"## File: {filename}")
        findings = analyze_tal_file(tal_file)

        # Structs
        report.append("### Structs")
        if findings["structs"]:
            for struct in findings["structs"]:
                report.append(f"- **{struct['name']}**:")
                for member in struct["members"]:
                    report.append(f"  - {member['name']} ({member['type']})")
        else:
            report.append("- None")

        # I/O Operations
        report.append("### Input/Output Operations")
        if any(findings["io_operations"].values()):
            for op, lines in findings["io_operations"].items():
                report.append(f"- **{op.replace('_', ' ').title()}**:")
                report.extend([f"  - {line}" for line in lines] or ["  - None"])
        else:
            report.append("- None")

        # Procedures
        report.append("### Procedures")
        if findings["procedures"]:
            for proc in findings["procedures"]:
                proc_name = proc.split(" at ")[0]
                report.append(f"- {proc}")
                report.append(f"  **Functionality**: {findings['procedure_details'].get(proc_name, 'No description available')}")
        else:
            report.append("- None")

        # Functions
        report.append("### Functions")
        report.extend([f"- {func}" for func in findings["functions"]] or ["- None"])

        # Control Structures
        report.append("### Control Structures")
        if any(findings["control_structures"].values()):
            for ctrl, lines in findings["control_structures"].items():
                report.append(f"- **{ctrl.replace('_', ' ').title()}**:")
                report.extend([f"  - {line}" for line in lines] or ["  - None"])
        else:
            report.append("- None")

        # Data Types
        report.append("### Data Types")
        if any(findings["data_types"].values()):
            for dtype, lines in findings["data_types"].items():
                report.append(f"- **{dtype.replace('_', ' ').title()}**:")
                report.extend([f"  - {line}" for line in lines] or ["  - None"])
        else:
            report.append("- None")

        # External Dependencies
        report.append("### External Dependencies")
        if any(findings["external_dependencies"].values()):
            for dep, lines in findings["external_dependencies"].items():
                report.append(f"- **{dep.replace('_', ' ').title()}**:")
                report.extend([f"  - {line}" for line in lines] or ["  - None"])
        else:
            report.append("- None")

        # Synchronization Operations
        report.append("### Synchronization Operations")
        if any(findings["sync_operations"].values()):
            for sync, lines in findings["sync_operations"].items():
                report.append(f"- **{sync.replace('_', ' ').title()}**:")
                report.extend([f"  - {line}" for line in lines] or ["  - None"])
        else:
            report.append("- None")

        # Execution Flow
        report.append("### Execution Flow")
        report.extend([f"- {flow}" for flow in findings["execution_flow"]] or ["- None"])

        report.append("")  # Separator between files

    return report

def main():
    folder_path = input("Enter the folder path containing TAL files: ").strip()
    if not os.path.isdir(folder_path):
        print("Error: Invalid folder path.")
        return

    report = generate_report(folder_path)
    
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(report))
    
    print(f"Analysis complete. Report saved to {OUTPUT_FILE}")

if __name__ == "__main__":
    main()