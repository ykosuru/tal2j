import sys
import os
from openai import OpenAI
from typing import List, Dict
import logging # Use logging

# Adjust path
project_root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, project_root_dir)

from agent_python_utils.file_utils import read_prompt_file, read_text_file, write_to_file
from agent_python_utils.llm_utils import generate_response
from agent_python_utils.parsing_utils import extract_code_block

# Setup logger for this agent
logger = logging.getLogger(__name__)

def run_agent(client: OpenAI, model: str, tal_tech_spec_path: str) -> str:
    """
    Agent 3 (New): Generates a Business Requirements Document (BRD) from TAL technical specifications.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        tal_tech_spec_path: Absolute path to the TAL tech spec file (output from Agent 2).

    Returns:
        The absolute path to the generated BRD Markdown file, or None on failure.
    """
    logger.info("--- Running Agent 3 (New): Generate Business Requirements Document ---")
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt specific to BRD generation
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_brd.txt")
    if not os.path.exists(system_prompt_file):
        logger.warning(f"System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Read the TAL technical specs (input for this agent)
    try:
        tal_tech_specs_md = read_text_file(tal_tech_spec_path)
    except ValueError as e:
        logger.error(f"Could not read input TAL tech spec file: {e}")
        return None

    # --- PROCESSING ---
    # Construct messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide the TAL specs as context via an assistant message
        {"role": "assistant", "content": f"Here are the technical specifications for the existing TAL program:\n```markdown\n{tal_tech_specs_md}\n```"},
        {"role": "user", "content": "Based *only* on the provided TAL technical specifications, generate a Business Requirements Document (BRD) for a project aiming to modernize or replace this functionality. Infer the business purpose and requirements from the technical details. Structure the BRD with sections like: Introduction/Purpose, Business Problem/Opportunity, Proposed Solution Overview, Key Functional Requirements (derived from TAL logic), and potentially Non-Functional Requirements (if inferable, e.g., performance, data handling). Output the BRD in Markdown format within a ```markdown Business Requirements Document``` block."}
    ]

    # Call the LLM
    response_raw = generate_response(client, model, messages, temperature=0.6) # Slightly higher temp for inference
    brd_md = extract_code_block(response_raw, language="markdown") # Expecting markdown

    if not brd_md:
        logger.warning("Could not extract BRD markdown block. Using raw response.")
        brd_md = response_raw # Fallback

    # --- OUTPUT ---
    # Save the BRD
    base_filename = os.path.basename(tal_tech_spec_path)
    # Derive name from TAL spec filename
    brd_filename_base = base_filename.replace('_TAL_tech_spec.md', '')
    output_filename = f"{brd_filename_base}_BRD.md"
    output_subfolder = "output/business_requirements"
    try:
        output_path = write_to_file(
            content=brd_md,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        logger.info(f"Business Requirements Document saved to: {output_path}")
        return output_path
    except Exception as e:
        logger.error(f"Error writing BRD file: {e}")
        return None

# Example of direct execution (optional)
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
    GEMINI_API_KEY = os.getenv("GEMINI_API_NEOTEK_KEY") # Use the correct key
    if not GEMINI_API_KEY: raise ValueError("API Key not set.")
    client = OpenAI(api_key=GEMINI_API_KEY, base_url="https://generativelanguage.googleapis.com/v1beta/openai/")
    model = "gemini-2.5-pro-exp-03-25"
    # Requires the output from Agent 2 to exist
    test_input_file = os.path.abspath(os.path.join(project_root_dir, "output", "tal_technical_docs", "sample2_TAL_tech_spec.md"))

    if os.path.exists(test_input_file):
        result_path = run_agent(client, model, test_input_file)
        if result_path: logger.info(f"\nAgent 3 (BRD) test finished successfully. Output: {result_path}")
        else: logger.error("\nAgent 3 (BRD) test failed.")
    else: logger.error(f"Test input file not found: {test_input_file}")
