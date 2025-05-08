# This agent takes the generated Java code and asks the LLM to create synthetic test data.

import sys
import os
from openai import OpenAI
from typing import List, Dict

# Adjust path to import from the parent directory
project_root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, project_root_dir)

from agent_python_utils.file_utils import read_prompt_file, read_text_file, write_to_file
from agent_python_utils.llm_utils import generate_response
from agent_python_utils.parsing_utils import extract_code_block

# Renamed function to run_agent for consistency
def run_agent(client: OpenAI, model: str, java_code_path: str) -> str:
    """
    Agent 6: Generates synthetic test data based on the generated Java code.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        java_code_path: Absolute path to the Java code file (output from Agent 4).

    Returns:
        The absolute path to the generated test data CSV file, or None on failure.
    """
    print("\n--- Running Agent 6: Generate Test Data ---") # Updated agent number
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_test_data.txt")
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
        {"role": "assistant", "content": f"Here is the Java code:\n```java\n{java_code}\n```"},
        {"role": "user", "content": "Based on the provided Java code, generate synthetic test data covering various scenarios (including edge cases). Format the data as a CSV block ```csv Test Data```. Include columns for inputs and expected outputs where applicable."}
    ]

    # Generate the test data
    test_data_raw = generate_response(client, model, messages, temperature=0.5)
    test_data_csv = extract_code_block(test_data_raw, language="csv")

    if not test_data_csv:
        print("Warning: Could not extract CSV code block from the response.")
        test_data_csv = test_data_raw # Save raw response as fallback

    # --- OUTPUT ---
    # Derive base filename from input Java code path
    base_filename_no_ext = os.path.splitext(os.path.basename(java_code_path))[0]
    output_filename = f"{base_filename_no_ext}_test_data.csv"
    output_subfolder = "output/test_data"

    try:
        output_path = write_to_file(
            content=test_data_csv,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Test data saved to: {output_path}")
        return output_path # Return the path on success
    except Exception as e:
        print(f"Error writing test data file: {e}")
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
    # Determine a plausible Java code filename based on sample input
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
    test_data_result_path = run_agent(client, model, test_input_path) # Use run_agent

    if test_data_result_path:
        print(f"\n--- Test Data Generation Test Complete --- Output: {test_data_result_path}")
    else:
        print("\n--- Test Data Generation Test Failed ---")