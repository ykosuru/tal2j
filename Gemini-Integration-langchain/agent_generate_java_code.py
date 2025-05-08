import sys
import os
import re
from openai import OpenAI
from typing import List, Dict

# Adjust path
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
    return None

def run_agent(client: OpenAI, model: str, java_tech_spec_path: str) -> str:
    """
    Agent 4: Generates Java code based on Java technical specifications.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        java_tech_spec_path: Absolute path to the Java tech spec file (output from Agent 3).

    Returns:
        The absolute path to the generated Java code file, or None on failure.
    """
    print("\n--- Running Agent 4: Generate Java Code ---")
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt specific to Java code generation
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_generate_java.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Read the Java technical specs (input for this agent)
    try:
        java_tech_specs_md = read_text_file(java_tech_spec_path)
    except ValueError as e:
        print(f"Error: Could not read input Java tech spec file: {e}")
        return None

    # --- PROCESSING ---
    # Construct messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide the Java specs as context via an assistant message
        {"role": "assistant", "content": f"Here are the technical specifications for the Java code to be generated:\n```markdown\n{java_tech_specs_md}\n```"},
        {"role": "user", "content": "Generate the complete Java code based *only* on the provided Java technical specifications. Ensure the code is well-structured, includes necessary imports, and implements the logic described. Add Javadoc comments to the class and methods. Output only the Java code within a ```java Java Code``` block."}
    ]

    # Call the LLM
    response_raw = generate_response(client, model, messages, temperature=0.2) # Low temp for code generation
    java_code = extract_code_block(response_raw, language="java") # Expecting java

    if not java_code:
        print("Warning: Could not extract Java code block. Using raw response.")
        java_code = response_raw # Fallback

    # --- OUTPUT ---
    # Determine filename from class name if possible
    class_name = get_java_class_name(java_code)
    if class_name:
        output_filename = f"{class_name}.java"
    else:
        # Fallback based on spec filename
        base_filename = os.path.basename(java_tech_spec_path)
        java_code_filename_base = base_filename.replace('_java_tech_spec.md', '')
        output_filename = f"{java_code_filename_base}.java"
        print(f"Warning: Could not detect class name, using fallback filename: {output_filename}")

    output_subfolder = "output/java_code"
    try:
        output_path = write_to_file(
            content=java_code,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Java code saved to: {output_path}")
        return output_path
    except Exception as e:
        print(f"Error writing Java code file: {e}")
        return None

# Example of direct execution (optional)
if __name__ == "__main__":
    GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    if not GEMINI_API_KEY: raise ValueError("GEMINI_API_MUKI_DEV_KEY not set.")
    client = OpenAI(api_key=GEMINI_API_KEY, base_url="https://generativelanguage.googleapis.com/v1beta/openai/")
    model = "gemini-2.5-pro-exp-03-25"
    # Requires the output from Agent 3 to exist
    test_input_file = os.path.abspath(os.path.join(project_root_dir, "output", "java_technical_docs", "sample2_java_tech_spec.md"))

    if os.path.exists(test_input_file):
        result_path = run_agent(client, model, test_input_file)
        if result_path: print(f"\nAgent 4 finished successfully. Output: {result_path}")
        else: print("\nAgent 4 failed.")
    else: print(f"Test input file not found: {test_input_file}")
