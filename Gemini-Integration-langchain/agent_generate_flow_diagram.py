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
def run_agent(client: OpenAI, model: str, java_tech_spec_path: str) -> str:
    """
    Agent 5: Generates a flow diagram description based on the Java technical specification.

    Args:
        client: The initialized OpenAI client.
        model: The model name string.
        java_tech_spec_path: Absolute path to the Java tech spec file (output from Agent 3).

    Returns:
        The absolute path to the generated flow diagram file, or None on failure.
    """
    print("\n--- Running Agent 5: Generate Flow Diagram ---") # Updated agent number
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    # --- INPUTS ---
    # Load the System Prompt
    system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt_flow_diagram.txt")
    if not os.path.exists(system_prompt_file):
        print(f"Warning: System prompt {system_prompt_file} not found. Using generic prompt.")
        system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt") # Fallback
    system_prompt = read_prompt_file(system_prompt_file)

    # Load the Java Technical Specification using the provided path
    try:
        java_tech_spec = read_text_file(java_tech_spec_path)
    except ValueError as e:
        print(f"Error: Could not read Java Tech Spec input file: {e}")
        return None

    # --- PROCESSING ---
    # Set the messages for the LLM
    messages = [
        {"role": "system", "content": system_prompt},
        # Provide the Java specs as context via an assistant message
        {"role": "assistant", "content": f"Here are the technical specifications for the Java program:\n```markdown\n{java_tech_spec}\n```"},
        {"role": "user", "content": "Based on the provided Java technical specification, generate a flow diagram description using Mermaid syntax. Focus on the main processing steps and logic flow. Output only the Mermaid code block ```mermaid Flow Diagram```."}
    ]

    # Generate the flow diagram description
    flow_diagram_raw = generate_response(client, model, messages, temperature=0.3)
    flow_diagram_code = extract_code_block(flow_diagram_raw, language="mermaid")

    if not flow_diagram_code:
        print("Warning: Could not extract Mermaid code block from the response.")
        flow_diagram_code = flow_diagram_raw # Save raw response as fallback

    # --- OUTPUT ---
    # Derive base filename from input path
    base_filename_no_ext = os.path.splitext(os.path.basename(java_tech_spec_path))[0].replace('_java_tech_spec', '')
    output_filename = f"{base_filename_no_ext}_flow.mmd" # Mermaid file extension
    output_subfolder = "output/flow_diagrams"

    try:
        output_path = write_to_file(
            content=flow_diagram_code,
            output_dir=project_folder,
            filename=output_filename,
            subfolder=output_subfolder
        )
        print(f"Flow diagram description saved to: {output_path}")
        return output_path # Return the path on success
    except Exception as e:
        print(f"Error writing flow diagram file: {e}")
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
    # Requires the output from Agent 3 (Java Tech Spec) to exist
    test_input_path = os.path.abspath(os.path.join(project_root_dir, "output", "java_technical_docs", "sample2_java_tech_spec.md"))

    if not os.path.exists(test_input_path):
         print(f"Error: Test input file not found at {test_input_path}")
    else:
        # --- Execute Agent ---
        flow_diagram_result_path = run_agent(client, model, test_input_path) # Use run_agent

        if flow_diagram_result_path:
            print(f"\n--- Flow Diagram Generation Test Complete --- Output: {flow_diagram_result_path}")
        else:
            print("\n--- Flow Diagram Generation Test Failed ---")