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

def run_agent(client: OpenAI, model: str, documented_tal_path: str) -> str:
    """
    Agent 2: Generates TAL technical specifications from documented TAL code.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        documented_tal_path: Absolute path to the documented TAL file (output from Agent 1).

    Returns:
        The absolute path to the generated TAL tech spec file, or None on failure.
    """
    print("\n--- Running Agent 2: Generate TAL Tech Specs ---")
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt specific to TAL tech specs
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_tal_tech_specs.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Read the documented TAL code (input for this agent)
    try:
        documented_tal_code = read_text_file(documented_tal_path)
    except ValueError as e:
        print(f"Error: Could not read input documented TAL file: {e}")
        return None

    # --- PROCESSING ---
    # Construct messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide the documented code as context via an assistant message (simulating conversation flow)
        {"role": "assistant", "content": f"Here is the documented TAL code:\n```tal\n{documented_tal_code}\n```"},
        {"role": "user", "content": "Generate comprehensive technical specifications for the provided TAL program. Include sections for: Program Description, Input Parameters (with types and descriptions), Return Value (if any), Core Logic/Processing Steps, Dependencies (e.g., called procedures), and Usage Examples or Edge Cases. Output the documentation in Markdown format within a ```markdown TAL Technical Specifications``` block."}
    ]

    # Call the LLM
    response_raw = generate_response(client, model, messages, temperature=0.5)
    tal_tech_specs_md = extract_code_block(response_raw, language="markdown") # Expecting markdown

    if not tal_tech_specs_md:
        print("Warning: Could not extract TAL tech specs block. Using raw response.")
        tal_tech_specs_md = response_raw # Fallback

    # --- OUTPUT ---
    # Save the TAL technical specs
    base_filename = os.path.basename(documented_tal_path)
    output_filename = f"{os.path.splitext(base_filename)[0]}_TAL_tech_spec.md"
    output_subfolder = "output/tal_technical_docs"
    try:
        output_path = write_to_file(
            content=tal_tech_specs_md,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"TAL technical specs saved to: {output_path}")
        return output_path
    except Exception as e:
        print(f"Error writing TAL tech specs file: {e}")
        return None

# Example of direct execution (optional)
if __name__ == "__main__":
    GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    if not GEMINI_API_KEY: raise ValueError("GEMINI_API_MUKI_DEV_KEY not set.")
    client = OpenAI(api_key=GEMINI_API_KEY, base_url="https://generativelanguage.googleapis.com/v1beta/openai/")
    model = "gemini-2.5-pro-exp-03-25"
    # Requires the output from Agent 1 to exist
    test_input_file = os.path.abspath(os.path.join(project_root_dir, "output", "documented_tal", "sample2.tal"))

    if os.path.exists(test_input_file):
        result_path = run_agent(client, model, test_input_file)
        if result_path: print(f"\nAgent 2 finished successfully. Output: {result_path}")
        else: print("\nAgent 2 failed.")
    else: print(f"Test input file not found: {test_input_file}")