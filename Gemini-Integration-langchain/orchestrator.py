import sys
import os
from openai import OpenAI
import logging

# Adjust path to import agents and utils
project_root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, project_root_dir)
sys.path.insert(0, os.path.dirname(__file__)) # Add agent directory to path

# Import the run_agent function from each agent module
import agent_document_tal_code
import agent_generate_tal_tech_specs
import agent_generate_java_tech_specs
import agent_generate_java_code
import agent_generate_flow_diagram
import agent_generate_test_data
import agent_generate_unit_tests # <-- Import the new agent

def main():
    # Set up logging configuration to track execution flow and debug issues
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
    logger = logging.getLogger(__name__)
    logger.info ("Starting Agent Orchestration")

    # --- Configuration ---
    input_tal_filename = "sample2.tal"
    project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    initial_tal_file_path = os.path.join(project_folder, "samples", input_tal_filename)

    if not os.path.exists(initial_tal_file_path):
        logger.error(f"Error: Initial TAL file not found at {initial_tal_file_path}")
        return

    # --- LLM Initialization ---
    # GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
    GEMINI_API_KEY = os.getenv("GEMINI_API_NEOTEK_KEY")
    if not GEMINI_API_KEY:
        raise ValueError("GEMINI_API_MUKI_DEV_KEY environment variable not set.")

    client = OpenAI(
        api_key=GEMINI_API_KEY,
        base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
    )
    model = "gemini-2.5-pro-exp-03-25"

    # --- Agent Execution Pipeline ---
    current_input_path = initial_tal_file_path
    final_output_paths = {}

    # Agent 1: Document TAL
    documented_tal_path = agent_document_tal_code.run_agent(client, model, current_input_path)
    if not documented_tal_path: logger.error ("Orchestration failed at Agent 1."); return
    final_output_paths['documented_tal'] = documented_tal_path
    current_input_path = documented_tal_path

    # Agent 2: Generate TAL Tech Specs
    tal_tech_spec_path = agent_generate_tal_tech_specs.run_agent(client, model, current_input_path)
    if not tal_tech_spec_path: logger.error("Orchestration failed at Agent 2."); return
    final_output_paths['tal_tech_spec'] = tal_tech_spec_path
    current_input_path = tal_tech_spec_path

    # Agent 3: Generate Java Tech Specs
    java_tech_spec_path = agent_generate_java_tech_specs.run_agent(client, model, current_input_path)
    if not java_tech_spec_path: logger.error("Orchestration failed at Agent 3."); return
    final_output_paths['java_tech_spec'] = java_tech_spec_path
    java_tech_spec_input_path = java_tech_spec_path # Keep for Agent 5

    # Agent 4: Generate Java Code (Uses Java Tech Spec)
    java_code_path = agent_generate_java_code.run_agent(client, model, java_tech_spec_input_path)
    if not java_code_path: logger.error("Orchestration failed at Agent 4."); return
    final_output_paths['java_code'] = java_code_path
    java_code_input_path = java_code_path # Keep for Agents 6 & 7

    # Agent 5: Generate Flow Diagram (Uses Java Tech Spec)
    flow_diagram_path = agent_generate_flow_diagram.run_agent(client, model, java_tech_spec_input_path)
    if not flow_diagram_path: logger.error("Orchestration failed at Agent 5."); return
    final_output_paths['flow_diagram'] = flow_diagram_path

    # Agent 6: Generate Test Data (Uses Java Code)
    test_data_path = agent_generate_test_data.run_agent(client, model, java_code_input_path)
    if not test_data_path: logger.error("Orchestration failed at Agent 6."); return
    final_output_paths['test_data'] = test_data_path

    # Agent 7: Generate Unit Tests (Uses Java Code) # <-- Add Agent 7 call
    unit_tests_path = agent_generate_unit_tests.run_agent(client, model, java_code_input_path)
    if not unit_tests_path: logger.error("Orchestration failed at Agent 7."); return # Add failure check
    final_output_paths['unit_tests'] = unit_tests_path # Store the output path


    # --- Completion ---
    logger.info("\n--- Agent Orchestration Complete ---")
    logger.info("Final output files generated:")
    # Sort items for consistent output order
    for key, path in sorted(final_output_paths.items()):
        logger.info(f"- {key}: {path}")

if __name__ == "__main__":
    main()