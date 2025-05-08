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
import agent_generate_brd             # <-- Import the new BRD agent
import agent_generate_java_tech_specs # Original Agent 3 becomes Agent 4
import agent_generate_java_code       # Original Agent 4 becomes Agent 5
import agent_generate_flow_diagram    # Original Agent 5 becomes Agent 6
import agent_generate_test_data       # Original Agent 6 becomes Agent 7
import agent_generate_unit_tests      # Original Agent 7 becomes Agent 8

def main():
    # Set up logging configuration
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
        raise ValueError("API Key environment variable not set.") # Generic message

    client = OpenAI(
        api_key=GEMINI_API_KEY,
        base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
    )
    model = "gemini-2.5-pro-exp-03-25"

    # --- Agent Execution Pipeline ---
    current_input_path = initial_tal_file_path
    final_output_paths = {}

    # Agent 1: Document TAL
    logger.info("Executing Agent 1: Document TAL Code")
    documented_tal_path = agent_document_tal_code.run_agent(client, model, current_input_path)
    if not documented_tal_path: logger.error ("Orchestration failed at Agent 1."); return
    final_output_paths['documented_tal'] = documented_tal_path
    current_input_path = documented_tal_path

    # Agent 2: Generate TAL Tech Specs
    logger.info("Executing Agent 2: Generate TAL Tech Specs")
    tal_tech_spec_path = agent_generate_tal_tech_specs.run_agent(client, model, current_input_path)
    if not tal_tech_spec_path: logger.error("Orchestration failed at Agent 2."); return
    final_output_paths['tal_tech_spec'] = tal_tech_spec_path
    # Input for Agent 3 (BRD) and Agent 4 (Java Specs) is the TAL Tech Spec
    tal_tech_spec_input_path = tal_tech_spec_path

    # Agent 3 (New): Generate Business Requirements Document (BRD)
    logger.info("Executing Agent 3: Generate Business Requirements Document")
    brd_path = agent_generate_brd.run_agent(client, model, tal_tech_spec_input_path)
    if not brd_path: logger.error("Orchestration failed at Agent 3 (BRD)."); return
    final_output_paths['business_requirements'] = brd_path
    # This agent's output doesn't directly feed the next step in this linear flow

    # Agent 4 (Original 3): Generate Java Tech Specs
    logger.info("Executing Agent 4: Generate Java Tech Specs")
    java_tech_spec_path = agent_generate_java_tech_specs.run_agent(client, model, tal_tech_spec_input_path) # Still uses TAL spec input
    if not java_tech_spec_path: logger.error("Orchestration failed at Agent 4 (Java Specs)."); return
    final_output_paths['java_tech_spec'] = java_tech_spec_path
    java_tech_spec_input_path = java_tech_spec_path # Keep for Agent 6 (Flow Diagram)

    # Agent 5 (Original 4): Generate Java Code (Uses Java Tech Spec)
    logger.info("Executing Agent 5: Generate Java Code")
    java_code_path = agent_generate_java_code.run_agent(client, model, java_tech_spec_input_path) # Uses Java Spec input from previous step
    if not java_code_path: logger.error("Orchestration failed at Agent 5 (Java Code)."); return
    final_output_paths['java_code'] = java_code_path
    java_code_input_path = java_code_path # Keep for Agents 7 & 8

    # Agent 6 (Original 5): Generate Flow Diagram (Uses Java Tech Spec)
    logger.info("Executing Agent 6: Generate Flow Diagram")
    flow_diagram_path = agent_generate_flow_diagram.run_agent(client, model, java_tech_spec_input_path) # Uses Java Spec input
    if not flow_diagram_path: logger.error("Orchestration failed at Agent 6 (Flow Diagram)."); return
    final_output_paths['flow_diagram'] = flow_diagram_path

    # Agent 7 (Original 6): Generate Test Data (Uses Java Code)
    logger.info("Executing Agent 7: Generate Test Data")
    test_data_path = agent_generate_test_data.run_agent(client, model, java_code_input_path) # Uses Java Code input
    if not test_data_path: logger.error("Orchestration failed at Agent 7 (Test Data)."); return
    final_output_paths['test_data'] = test_data_path

    # Agent 8 (Original 7): Generate Unit Tests (Uses Java Code)
    logger.info("Executing Agent 8: Generate Unit Tests")
    unit_tests_path = agent_generate_unit_tests.run_agent(client, model, java_code_input_path) # Uses Java Code input
    if not unit_tests_path: logger.error("Orchestration failed at Agent 8 (Unit Tests)."); return
    final_output_paths['unit_tests'] = unit_tests_path


    # --- Completion ---
    logger.info("--- Agent Orchestration Complete ---")
    logger.info("Final output files generated:")
    # Sort items for consistent output order
    for key, path in sorted(final_output_paths.items()):
        logger.info(f"- {key}: {path}")

if __name__ == "__main__":
    main()