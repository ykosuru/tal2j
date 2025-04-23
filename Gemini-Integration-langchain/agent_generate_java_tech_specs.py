import sys
import os
from openai import OpenAI
from typing import List, Dict

# Adjust path
project_root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, project_root_dir)

from agent_python_utils.file_utils import read_prompt_file, read_text_file, write_to_file
from agent_python_utils.llm_utils import generate_response
from agent_python_utils.parsing_utils import extract_code_block

def run_agent(client: OpenAI, model: str, tal_tech_spec_path: str) -> str:
    """
    Agent 3: Generates Java technical specifications based on TAL technical specifications.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        tal_tech_spec_path: Absolute path to the TAL tech spec file (output from Agent 2).

    Returns:
        The absolute path to the generated Java tech spec file, or None on failure.
    """
    print("\n--- Running Agent 3: Generate Java Tech Specs ---")
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt specific to Java tech specs
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_java_tech_specs.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Read the TAL technical specs (input for this agent)
    try:
        tal_tech_specs_md = read_text_file(tal_tech_spec_path)
    except ValueError as e:
        print(f"Error: Could not read input TAL tech spec file: {e}")
        return None

    # --- PROCESSING ---
    # Construct messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide the TAL specs as context via an assistant message
        {"role": "assistant", "content": f"Here are the technical specifications for the TAL program:\n```markdown\n{tal_tech_specs_md}\n```"},
        {"role": "user", "content": "Generate the equivalent Java technical specifications based on the provided TAL specifications. Maintain the same level of detail, including: Class/Method Description, Input Parameters (with Java types), Return Value (with Java type), Core Logic/Processing Steps (adapted for Java), Dependencies (e.g., other methods/classes), and Usage Examples or Edge Cases relevant to Java implementation. Output the documentation in Markdown format within a ```markdown Java Technical Specifications``` block."}
    ]

    # Call the LLM
    response_raw = generate_response(client, model, messages, temperature=0.5)
    java_tech_specs_md = extract_code_block(response_raw, language="markdown") # Expecting markdown

    if not java_tech_specs_md:
        print("Warning: Could not extract Java tech specs block. Using raw response.")
        java_tech_specs_md = response_raw # Fallback

    # --- OUTPUT ---
    # Save the Java technical specs
    base_filename = os.path.basename(tal_tech_spec_path)
    # Derive name from TAL spec filename
    java_spec_filename_base = base_filename.replace('_TAL_tech_spec.md', '')
    output_filename = f"{java_spec_filename_base}_java_tech_spec.md"
    output_subfolder = "output/java_technical_docs"
    try:
        output_path = write_to_file(
            content=java_tech_specs_md,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Java technical specs saved to: {output_path}")
        return output_path
    except Exception as e:
        print(f"Error writing Java tech specs file: {e}")
        return None

# Example of direct execution (optional)
if __name__ == "__main__":
    GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    if not GEMINI_API_KEY: raise ValueError("GEMINI_API_MUKI_DEV_KEY not set.")
    client = OpenAI(api_key=GEMINI_API_KEY, base_url="https://generativelanguage.googleapis.com/v1beta/openai/")
    model = "gemini-2.5-pro-exp-03-25"
    # Requires the output from Agent 2 to exist
    test_input_file = os.path.abspath(os.path.join(project_root_dir, "output", "tal_technical_docs", "sample2_TAL_tech_spec.md"))

    if os.path.exists(test_input_file):
        result_path = run_agent(client, model, test_input_file)
        if result_path: print(f"\nAgent 3 finished successfully. Output: {result_path}")
        else: print("\nAgent 3 failed.")
    else: print(f"Test input file not found: {test_input_file}")