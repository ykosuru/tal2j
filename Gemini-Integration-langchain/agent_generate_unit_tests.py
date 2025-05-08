# This agent takes the generated Java code and asks the LLM to create JUnit test cases.
import sys
import os
import re
from openai import OpenAI
from typing import List, Dict

# Adjust path to import from the parent directory
project_root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, project_root_dir)

from agent_python_utils.file_utils import read_prompt_file, read_text_file, write_to_file
from agent_python_utils.llm_utils import generate_response
from agent_python_utils.parsing_utils import extract_code_block

def get_java_class_name(java_code: str) -> str:
    """Extracts the public class name from Java code."""
    match = re.search(r'public\s+class\s+(\w+)', java_code)
    if match:
        return match.group(1)
    # Attempt to find interface name as fallback
    match = re.search(r'public\s+interface\s+(\w+)', java_code)
    if match:
        return match.group(1)
    return None

# Renamed function to run_agent for consistency
def run_agent(client: OpenAI, model: str, java_code_path: str) -> str:
    """
    Agent 7: Generates JUnit test cases for the generated Java code.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        java_code_path: Absolute path to the Java code file (output from Agent 4).

    Returns:
        The absolute path to the generated JUnit test file, or None on failure.
    """
    print("\n--- Running Agent 7: Generate Unit Tests ---") # Updated agent number
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_unit_tests.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Load the Java Code using the provided path
    try:
        java_code = read_text_file(java_code_path)
    except ValueError as e:
        print(f"Error: Could not read Java code input file: {e}")
        return None

    # --- PROCESSING ---
    # Set the messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide Java code as context via assistant message
        {"role": "assistant", "content": f"Here is the Java code to test:\n```java\n{java_code}\n```"},
        {"role": "user", "content": "Based on the provided Java code, generate comprehensive JUnit 5 test cases. Include tests for normal operation, edge cases, and potential error conditions. Use appropriate assertions (e.g., assertEquals, assertThrows, assertTrue). Ensure necessary imports for JUnit are included. Output only the complete Java code for the test class in a ```java JUnit Tests``` code block."}
    ]

    # Generate the unit tests
    unit_tests_raw = generate_response(client, model, messages, temperature=0.4)
    unit_tests_code = extract_code_block(unit_tests_raw, language="java")

    if not unit_tests_code:
        print("Warning: Could not extract Java code block for unit tests from the response.")
        unit_tests_code = unit_tests_raw # Save raw response as fallback

    # --- OUTPUT ---
    # Determine the test class name (often based on the original class name)
    original_class_name = get_java_class_name(java_code)
    if original_class_name:
        test_class_name = f"{original_class_name}Test"
    else:
        # Fallback based on filename if class name not found
        base_filename_no_ext = os.path.splitext(os.path.basename(java_code_path))[0]
        test_class_name = f"{base_filename_no_ext}Test"
        print(f"Warning: Could not detect class name, using fallback test name: {test_class_name}")
    output_filename = f"{test_class_name}.java"
    output_subfolder = "output/unit_tests"

    try:
        output_path = write_to_file(
            content=unit_tests_code,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Unit test code saved to: {output_path}")
        return output_path # Return the path on success
    except Exception as e:
        print(f"Error writing unit test file: {e}")
        return None # Return None on failure


# --- Main execution block (for individual testing) ---
if __name__ == "__main__":
    # --- LLM Initialization ---
    GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    if not GEMINI_API_KEY:
        raise ValueError("GEMINI_API_MUKI_DEV_KEY is not set in the environment.")

    client = OpenAI(
        api_key=GEMINI_API_KEY,
        base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
    )
    model = "gemini-2.5-pro-exp-03-25"

    # --- Configuration for testing ---
    # Requires the output from Agent 4 (Java Code) to exist
    base_input_filename = "sample2.tal"
    base_filename_no_ext = os.path.splitext(base_input_filename)[0]
    # Assuming Agent 4 might create Sample2.java or similar
    test_input_path = os.path.abspath(os.path.join(project_root_dir, "output", "java_code", f"{base_filename_no_ext}.java")) # Adjust if Agent 4 uses class name

    if not os.path.exists(test_input_path):
         # Try a potential class name based filename if the simple one fails
         potential_class_name = base_filename_no_ext.capitalize() # Simple guess
         test_input_path_alt = os.path.abspath(os.path.join(project_root_dir, "output", "java_code", f"{potential_class_name}.java"))
         if os.path.exists(test_input_path_alt):
             test_input_path = test_input_path_alt
         else:
             print(f"Error: Test input Java code file not found at {test_input_path} or {test_input_path_alt}")
             sys.exit(1) # Exit if input not found for testing

    # --- Execute Agent ---
    unit_tests_result_path = run_agent(client, model, test_input_path) # Use run_agent

    if unit_tests_result_path:
        print(f"\n--- Unit Test Generation Test Complete --- Output: {unit_tests_result_path}")
    else:
        print("\n--- Unit Test Generation Test Failed ---")
