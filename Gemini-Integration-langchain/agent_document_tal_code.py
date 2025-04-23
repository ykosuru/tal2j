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

def run_agent(client: OpenAI, model: str, tal_file_path: str) -> str:
    """
    Agent 1: Reads a TAL file, generates inline comments and explanations.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        tal_file_path: The absolute path to the input TAL file.

    Returns:
        The absolute path to the generated documented TAL file, or None on failure.
    """
    print("\n--- Running Agent 1: Document TAL Code ---")
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt specific to documenting TAL
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_document_tal.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Read the input TAL file
    try:
        tal_code = read_text_file(tal_file_path)
    except ValueError as e:
        print(f"Error: Could not read input TAL file: {e}")
        return None

    # --- PROCESSING ---
    # Construct messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": f"Explain the following TAL code. Insert comments into the code as appropriate before each relevant line or block, not at the end of lines. Output the fully documented code within a ```tal Documented Code``` block.\n\nTAL Code:\n```tal\n{tal_code}\n```"}
    ]

    # Call the LLM
    response_raw = generate_response(client, model, messages, temperature=0.3) # Lower temp for more factual comments
    documented_tal_code = extract_code_block(response_raw, language="tal")

    if not documented_tal_code:
        print("Warning: Could not extract documented TAL code block. Using raw response.")
        documented_tal_code = response_raw # Fallback

    # --- OUTPUT ---
    # Save the documented code
    base_filename = os.path.basename(tal_file_path)
    output_filename = base_filename # Keep original filename
    output_subfolder = "output/documented_tal"
    try:
        output_path = write_to_file(
            content=documented_tal_code,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Documented TAL code saved to: {output_path}")
        return output_path
    except Exception as e:
        print(f"Error writing documented TAL file: {e}")
        return None

# Example of direct execution (optional)
if __name__ == "__main__":
    # This part is for testing the agent individually
    GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    if not GEMINI_API_KEY:
        raise ValueError("GEMINI_API_MUKI_DEV_KEY is not set.")
    client = OpenAI(api_key=GEMINI_API_KEY, base_url="https://generativelanguage.googleapis.com/v1beta/openai/")
    model = "gemini-2.5-pro-exp-03-25"
    test_tal_file = os.path.abspath(os.path.join(project_root_dir, "samples", "sample2.tal"))

    if os.path.exists(test_tal_file):
        result_path = run_agent(client, model, test_tal_file)
        if result_path:
            print(f"\nAgent 1 finished successfully. Output: {result_path}")
        else:
            print("\nAgent 1 failed.")
    else:
        print(f"Test TAL file not found: {test_tal_file}")
