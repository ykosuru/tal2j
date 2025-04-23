import os
import re
import subprocess
import json
import logging
from typing import List, Dict, Any  # Type hints for better code readability
from langchain_anthropic import ChatAnthropic  # Integration with Anthropic's Claude LLM
from langchain_core.tools import StructuredTool  # For defining tools that the LLM can use
from langchain.prompts import ChatPromptTemplate  # For structured prompt creation
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage  # Message types for chat
from pydantic import BaseModel, Field  # For structured data validation

# Set up logging configuration to track execution flow and debug issues
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Initialize Claude model with specific version and temperature=0 for deterministic outputs
llm = ChatAnthropic(model="claude-3-5-sonnet-20241022", temperature=0)

# --- Input Models for Tools ---
# These Pydantic models define the schema for tool inputs, enabling type validation

class ParseTALInput(BaseModel):
    file_path: str = Field(description="Path to the TAL file")
    # This model captures the input for parsing TAL files

class BuildCallTreeInput(BaseModel):
    procedures: List[Dict[str, Any]] = Field(description="List of parsed TAL procedures")
    # This model captures the input for building a call tree

class TranslateTALInput(BaseModel):
    proc: Dict[str, Any] = Field(description="TAL procedure to translate")
    translated_procs: Dict[str, str] = Field(description="Previously translated procedures")
    # This model captures the input for translating TAL to Java

class CompileJavaInput(BaseModel):
    java_code: str = Field(description="Java code to compile")
    output_dir: str = Field(description="Directory for compiled output")
    # This model captures the input for compiling Java code

class SimulateTALInput(BaseModel):
    proc: Dict[str, Any] = Field(description="TAL procedure to simulate")
    test_case: Dict[str, Any] = Field(description="Test case inputs")
    # This model captures the input for simulating TAL execution

class RunJavaTestInput(BaseModel):
    java_code: str = Field(description="Java code to test")
    test_case: Dict[str, Any] = Field(description="Test case inputs")
    output_dir: str = Field(description="Directory for test output")
    # This model captures the input for running Java tests

class SuggestFixInput(BaseModel):
    java_code: str = Field(description="Java code with error")
    error_message: str = Field(description="Error message to fix")
    # This model captures the input for suggesting fixes to Java code

# --- Tools for Main Agent ---

def parse_tal_file(file_path: str) -> Dict[str, Any]:
    """Parse a TAL file and extract procedure definitions, calls, and test cases."""
    logger.info(f"Parsing TAL file: {file_path}")
    try:
        # Open and read the TAL file
        with open(file_path, 'r') as f:
            content = f.read()
        
        # Define regex patterns for TAL procedure extraction
        proc_pattern = r'PROC\s+(\w+)\s*(?:\((.*?)\))?\s*(?:MAIN)?\s*;([\s\S]*?)END;'
        call_pattern = r'CALL\s+(\w+)\s*\((.*?)\);'
        
        procedures = []
        # Iterate through all procedure matches in the TAL file
        for match in re.finditer(proc_pattern, content, re.MULTILINE | re.IGNORECASE):
            proc_name = match.group(1).lower()  # Extract procedure name
            params = match.group(2).split(',') if match.group(2) else []  # Extract parameters
            params = [p.strip() for p in params]  # Clean up parameter strings
            body = match.group(3)  # Extract procedure body
            
            # Extract all procedure calls within this procedure
            calls = [m.group(1).lower() for m in re.finditer(call_pattern, body, re.IGNORECASE)]
            
            # Generate simple test cases for procedures with integer variables
            test_cases = []
            if 'INT' in body.upper():
                test_cases.append({
                    'inputs': {p.split()[-1]: 10 * (i + 1) for i, p in enumerate(params)},
                    'expected_output': None
                })
            
            # Construct a structured representation of the procedure
            procedures.append({
                'name': proc_name,
                'params': params,
                'body': body.strip(),
                'calls': calls,
                'test_cases': test_cases
            })
        
        logger.info(f"Parsed {len(procedures)} procedures from {file_path}")
        return {'procedures': procedures, 'file_path': file_path}
    except Exception as e:
        # Handle and log any errors during parsing
        logger.error(f"Failed to parse {file_path}: {str(e)}")
        return {'error': f"Failed to parse {file_path}: {str(e)}"}

def build_call_tree(procedures: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Build a call tree from a list of procedures, detecting cycles."""
    logger.info("Building call tree")
    # Create a dictionary mapping procedure names to their calls
    call_graph = {p['name']: p['calls'] for p in procedures}
    # Identify leaf procedures (those that don't call other procedures)
    leaves = [p['name'] for p in procedures if not p['calls']]
    
    # Helper function to detect cycles in the call graph using DFS
    def has_cycle(node: str, visited: set, rec_stack: set) -> bool:
        visited.add(node)
        rec_stack.add(node)
        for neighbor in call_graph.get(node, []):
            if neighbor not in visited:
                if has_cycle(neighbor, visited, rec_stack):
                    return True
            elif neighbor in rec_stack:
                # If a node is already in the recursion stack, we have a cycle
                logger.warning(f"Cycle detected involving {neighbor}")
                return True
        rec_stack.remove(node)  # Remove the node from recursion stack after processing
        return False
    
    # Check for cycles in the call graph
    visited = set()
    rec_stack = set()
    for proc in call_graph:
        if proc not in visited:
            if has_cycle(proc, visited, rec_stack):
                logger.error("Call graph contains cycles, which may cause issues")
    
    # Build a hierarchical tree representation of the call graph
    tree = {}
    def build_node(proc_name: str, visited_nodes: set) -> Dict:
        if proc_name in visited_nodes:
            logger.warning(f"Skipping {proc_name} to avoid cycle")
            return None
        visited_nodes.add(proc_name)
        node = {'name': proc_name, 'children': []}
        called_procs = call_graph.get(proc_name, [])
        for called in called_procs:
            if called in call_graph:
                child_node = build_node(called, visited_nodes.copy())
                if child_node:
                    node['children'].append(child_node)
        return node
    
    # Identify root procedures (those that aren't called by any other procedure)
    all_procs = set(call_graph.keys())
    called_procs = set(c for calls in call_graph.values() for c in calls)
    roots = all_procs - called_procs
    
    # Build the tree starting from each root procedure
    for root in roots:
        tree[root] = build_node(root, set())
    
    logger.info(f"Call tree built with {len(tree)} roots and {len(leaves)} leaves")
    return {'tree': tree, 'leaves': leaves}

def translate_tal_to_java(proc: Dict[str, Any], translated_procs: Dict[str, str]) -> Dict[str, str]:
    """Translate a TAL procedure to Java, using previously translated procedures."""
    proc_name = proc['name']
    logger.info(f"Translating procedure: {proc_name}")
    
    params = proc['params']
    body = proc['body']
    
    # Convert TAL parameter declarations to Java
    java_params = []
    for param in params:
        if 'INT' in param.upper():
            java_params.append(f"int {param.split()[-1]}")
        elif 'STRING' in param.upper():
            java_params.append(f"String {param.split()[-1]}")
        else:
            # Default to Object for unrecognized types
            java_params.append(f"Object {param.split()[-1]}")
    
    # Convert TAL syntax to Java syntax using regex replacements
    java_body = body
    java_body = re.sub(r'INT\s+(\w+)', r'int \1', java_body, flags=re.IGNORECASE)  # INT -> int
    java_body = re.sub(r'STRING\s+(\w+)\[.*?\]', r'String \1', java_body, flags=re.IGNORECASE)  # STRING[n] -> String
    java_body = re.sub(r':=', '=', java_body)  # := -> =
    java_body = re.sub(r'END;', '}', java_body, flags=re.IGNORECASE)  # END; -> }
    java_body = re.sub(r'BEGIN', '{', java_body, flags=re.IGNORECASE)  # BEGIN -> {
    
    # Replace CALL statements with Java method calls
    for called_proc in proc['calls']:
        if called_proc in translated_procs:
            java_body = re.sub(
                rf'CALL\s+{called_proc}\s*\((.*?)\);',
                f"{called_proc}(\\1);",
                java_body,
                flags=re.IGNORECASE
            )
    
    # Determine if the procedure should return a value
    if any(p.lower().startswith('result') for p in params):
        # If there's a parameter named result*, add a return statement
        java_body += f"\n    return {next(p.split()[-1] for p in params if p.lower().startswith('result'))};"
        java_code = f"public static int {proc_name}({', '.join(java_params)}) {{\n{java_body}\n}}"
    else:
        # Otherwise, make it a void method
        java_code = f"public static void {proc_name}({', '.join(java_params)}) {{\n{java_body}\n}}"
    
    return {proc_name: java_code}

# --- Tools for Critique Agent ---

def compile_java_code(java_code: str, output_dir: str) -> Dict[str, Any]:
    """Compile Java code and return compilation result."""
    logger.info("Compiling Java code")
    try:
        # Create output directory if it doesn't exist
        os.makedirs(output_dir, exist_ok=True)
        # Write Java code to a file
        java_file = os.path.join(output_dir, "TranslatedProgram.java")
        with open(java_file, 'w') as f:
            f.write(java_code)
        
        # Compile the Java code using javac
        result = subprocess.run(
            ['javac', java_file],
            capture_output=True,
            text=True
        )
        
        # Check if compilation was successful
        if result.returncode == 0:
            logger.info("Compilation successful")
            return {'status': 'success', 'message': 'Compilation successful'}
        else:
            logger.error(f"Compilation failed: {result.stderr}")
            return {'status': 'error', 'message': result.stderr}
    except Exception as e:
        # Handle any exceptions during compilation
        logger.error(f"Compilation error: {str(e)}")
        return {'status': 'error', 'message': str(e)}

def simulate_tal_execution(proc: Dict[str, Any], test_case: Dict[str, Any]) -> Dict[str, Any]:
    """Simulate TAL procedure execution for a test case."""
    logger.info(f"Simulating TAL execution for {proc['name']}")
    try:
        inputs = test_case['inputs']  # Get test case inputs
        body = proc['body']  # Get procedure body
        
        # Initialize variables with test inputs
        variables = inputs.copy()
        output = None
        
        # Simple interpreter for TAL - process each line
        for line in body.split('\n'):
            line = line.strip()
            if ':=' in line:  # Assignment operation
                var, expr = line.split(':=')
                var = var.strip().split()[-1]  # Extract variable name
                expr = expr.strip().replace(';', '')  # Remove semicolon
                
                # Very basic expression evaluation (only handles addition)
                if '+' in expr:
                    parts = expr.split('+')
                    left = variables.get(parts[0].strip(), 0)
                    right = variables.get(parts[1].strip(), 0)
                    variables[var] = left + right
                    # If the result is stored in a variable named "result*", capture it
                    if var.lower().startswith('result'):
                        output = variables[var]
        
        return {'output': output, 'variables': variables}
    except Exception as e:
        # Handle any exceptions during simulation
        logger.error(f"Simulation failed: {str(e)}")
        return {'error': f"Simulation failed: {str(e)}"}

def run_java_test(java_code: str, test_case: Dict[str, Any], output_dir: str) -> Dict[str, Any]:
    """Run Java code with test inputs and capture output."""
    logger.info("Running Java test")
    try:
        # Create a test driver that calls the translated method with test inputs
        test_driver = f"""
        public class TestDriver {{
            {java_code.replace('public class TranslatedProgram', 'static class TranslatedProgram')}
            public static void main(String[] args) {{
                int result = TranslatedProgram.add_numbers({test_case['inputs']['x']}, {test_case['inputs']['y']});
                System.out.println(result);
            }}
        }}
        """
        
        # Write test driver to a file
        test_file = os.path.join(output_dir, "TestDriver.java")
        with open(test_file, 'w') as f:
            f.write(test_driver)
        
        # Compile the test driver
        compile_result = subprocess.run(
            ['javac', test_file],
            capture_output=True,
            text=True
        )
        if compile_result.returncode != 0:
            logger.error(f"Test compilation failed: {compile_result.stderr}")
            return {'error': f"Test compilation failed: {compile_result.stderr}"}
        
        # Run the test and capture output
        run_result = subprocess.run(
            ['java', '-cp', output_dir, 'TestDriver'],
            capture_output=True,
            text=True
        )
        if run_result.returncode != 0:
            logger.error(f"Test execution failed: {run_result.stderr}")
            return {'error': f"Test execution failed: {run_result.stderr}"}
        
        # Parse and return the output
        output = run_result.stdout.strip()
        logger.info(f"Java test output: {output}")
        return {'output': int(output) if output.isdigit() else output}
    except Exception as e:
        # Handle any exceptions during test execution
        logger.error(f"Test run failed: {str(e)}")
        return {'error': f"Test run failed: {str(e)}"}

def suggest_fix(java_code: str, error_message: str) -> Dict[str, str]:
    """Suggest a fix for a Java compilation or runtime error."""
    logger.info("Suggesting fix for Java error")
    # Apply simple heuristic fixes based on common error patterns
    if 'cannot find symbol' in error_message.lower():
        # Symbol not found - likely a typo or missing declaration
        suggestion = "// Check method names and parameters for typos\n" + java_code
    elif 'return type' in error_message.lower():
        # Return type mismatch - change void to int
        suggestion = java_code.replace('void', 'int') + "\n// Added return type"
    else:
        # Generic suggestion for other errors
        suggestion = java_code + "\n// Review error: " + error_message
    
    return {'fixed_code': suggestion}

# Define tools with Anthropic-compatible schemas
tools = [
    StructuredTool.from_function(
        func=parse_tal_file,
        name="parse_tal_file",
        description="Parse a TAL file to extract procedure definitions, calls, and test cases.",
        args_schema=ParseTALInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=build_call_tree,
        name="build_call_tree",
        description="Build a call tree from a list of TAL procedures, detecting cycles.",
        args_schema=BuildCallTreeInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=translate_tal_to_java,
        name="translate_tal_to_java",
        description="Translate a TAL procedure to Java, using previously translated procedures.",
        args_schema=TranslateTALInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=compile_java_code,
        name="compile_java_code",
        description="Compile Java code and return compilation result.",
        args_schema=CompileJavaInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=simulate_tal_execution,
        name="simulate_tal_execution",
        description="Simulate TAL procedure execution for a test case.",
        args_schema=SimulateTALInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=run_java_test,
        name="run_java_test",
        description="Run Java code with test inputs and capture output.",
        args_schema=RunJavaTestInput,
        return_direct=False
    ),
    StructuredTool.from_function(
        func=suggest_fix,
        name="suggest_fix",
        description="Suggest a fix for a Java compilation or runtime error.",
        args_schema=SuggestFixInput,
        return_direct=False
    ),
]

# Convert tools to Anthropic-compatible format
def convert_to_anthropic_tools(tools):
    """Convert LangChain tools to Anthropic's expected tool format."""
    anthropic_tools = []
    for tool in tools:
        # Extract schema information from the tool
        schema = tool.args_schema.schema()
        # Create an Anthropic-compatible tool structure
        anthropic_tool = {
            "name": tool.name,
            "description": tool.description,
            "type": "custom",  # Custom tool type for Anthropic API
            "input_schema": {
                "type": "object",
                "properties": schema.get("properties", {}),
                "required": schema.get("required", [])
            }
        }
        anthropic_tools.append(anthropic_tool)
    return anthropic_tools

# Convert our tools to Anthropic's format
anthropic_tools = convert_to_anthropic_tools(tools)

# --- Prompt Templates ---

# Main prompt for the TAL to Java conversion agent
main_prompt = ChatPromptTemplate.from_messages([
    SystemMessage(content="You are an expert in converting TAL (Transaction Application Language) to Java. Process TAL files, build a call tree, and translate procedures to Java in a depth-first manner, starting from leaf procedures. Use the provided tools to parse files, build the call tree, and translate procedures. Ensure translations are accurate and syntactically correct. Output the Java code wrapped in a class named 'TranslatedProgram'."),
    ("human", "{input}"),
])

# Critique prompt for verifying the correctness of the translation
critique_prompt = ChatPromptTemplate.from_messages([
    SystemMessage(content="You are a Critique Agent verifying Java code translated from TAL. Compile the Java code, run tests, and compare outputs with TAL's simulated execution to ensure functional equivalence. If issues are found, suggest fixes and recompile. Output the final verified Java code or report persistent issues. Be precise and thorough in your analysis."),
    ("human", "{input}"),
])

# --- Custom Agent Loop ---

def serialize_message(message):
    """Serialize an AIMessage object to a dictionary for logging."""
    # Extract relevant fields from the message for cleaner logging
    result = {
        "content": message.content,
        "tool_calls": message.tool_calls,
        "additional_kwargs": message.additional_kwargs,
        "type": message.type
    }
    return result

def run_agent_loop(llm, prompt, tools, anthropic_tools, input_data, max_iterations=10):
    """Run an agent loop that interacts with tools and makes decisions."""
    # Format the initial prompt with input data
    messages = prompt.format_messages(input=input_data)
    for iteration in range(max_iterations):
        logger.info(f"Agent loop iteration {iteration + 1}")
        # Get model response
        response = llm.invoke(messages, tools=anthropic_tools)
        
        # Log the response for debugging
        logger.info(f"Claude response: {json.dumps(serialize_message(response), indent=2)}")
        
        # If no tool calls and response is a string, return the final response
        if isinstance(response.content, str) and not response.tool_calls:
            logger.info("No tool calls; returning response")
            return response.content
        
        # Add model's response to conversation history
        messages.append(AIMessage(content=response.content, tool_calls=response.tool_calls))
        
        # If no tool calls but we're expecting them, handle this case
        if not response.tool_calls:
            logger.warning("No tool calls in response")
            messages.append(AIMessage(content="No tool calls provided"))
            continue
        
        # Process each tool call
        for tool_call in response.tool_calls:
            tool_name = tool_call.get("name")
            tool_args = tool_call.get("input") or tool_call.get("arguments", {})
            tool_call_id = tool_call.get("id")
            
            # Validate tool call format
            if not tool_name or not tool_call_id:
                logger.error(f"Invalid tool call: {tool_call}")
                messages.append(AIMessage(content=f"Invalid tool call: {tool_call}", tool_call_id=tool_call_id))
                continue
            
            # Find the requested tool
            tool = next((t for t in tools if t.name == tool_name), None)
            if not tool:
                logger.error(f"Tool {tool_name} not found")
                messages.append(AIMessage(content=f"Error: Tool {tool_name} not found", tool_call_id=tool_call_id))
                continue
            
            try:
                # Invoke the tool with given arguments
                logger.info(f"Invoking tool {tool_name} with args: {tool_args}")
                result = tool.invoke(tool_args)
                messages.append(AIMessage(content=json.dumps(result), tool_call_id=tool_call_id))
            except Exception as e:
                # Handle errors during tool invocation
                logger.error(f"Tool {tool_name} failed: {str(e)}")
                messages.append(AIMessage(content=f"Error in {tool_name}: {str(e)}", tool_call_id=tool_call_id))
        
    # If we reach this point, we've exhausted our iterations
    logger.warning("Max iterations reached")
    return "Error: Max iterations reached"

# --- Main Agentic Flow ---

def process_tal_files(input_dir: str, output_dir: str) -> str:
    """Process TAL files, convert to Java, and verify with Critique Agent."""
    logger.info(f"Processing TAL files in {input_dir}")
    
    # Step 1: Parse TAL files
    # Find all .tal files in the input directory
    tal_files = [os.path.join(input_dir, f) for f in os.listdir(input_dir) if f.endswith('.tal')]
    all_procedures = []
    # Parse each TAL file and collect all procedures
    for file_path in tal_files:
        result = parse_tal_file(file_path)
        if 'error' not in result:
            all_procedures.extend(result['procedures'])
    
    # Check if we found any procedures
    if not all_procedures:
        logger.error("No procedures parsed from TAL files")
        return "Error: No procedures parsed from TAL files"
    
    # Step 2: Build call tree
    # Create a call tree to understand dependencies between procedures
    call_tree_result = build_call_tree(all_procedures)
    tree = call_tree_result['tree']
    leaves = call_tree_result['leaves']
    
    # Step 3: Translate procedures
    # Initialize collections for translated procedures and tracking
    translated_procs = {}
    visited = set()
    
    # Helper function for depth-first traversal and translation
    def translate_dfs(proc_name: str, procedures: List[Dict[str, Any]]):
        # Skip if already visited to prevent infinite recursion
        if proc_name in visited:
            logger.warning(f"Skipping {proc_name} as it was already visited")
            return
        
        # Mark as visited
        visited.add(proc_name)
        # Find the procedure data
        proc = next((p for p in procedures if p['name'] == proc_name), None)
        if not proc:
            logger.warning(f"Procedure {proc_name} not found")
            return
        
        # First translate dependencies (depth-first)
        for child in proc['calls']:
            if child not in translated_procs:
                translate_dfs(child, procedures)
        
        # Then translate this procedure
        if proc_name not in translated_procs:
            result = translate_tal_to_java(proc, translated_procs)
            translated_procs.update(result)
    
    # Start translation from leaves (procedures with no calls) for bottom-up approach
    for leaf in leaves:
        translate_dfs(leaf, all_procedures)
    # Then make sure all procedures are translated
    for proc in all_procedures:
        translate_dfs(proc['name'], all_procedures)
    
    # Step 4: Combine into Java class
    # Create a Java class containing all translated methods
    java_code = "public class TranslatedProgram {\n"
    for java_method in translated_procs.values():
        java_code += f"    {java_method}\n\n"
    java_code += "    public static void main(String[] args) {\n        // Entry point\n    }\n}"
    
    # Step 5: Critique and verify
    # Prepare input for the critique agent
    critique_input = {
        'java_code': java_code,
        'procedures': all_procedures,
        'output_dir': output_dir
    }
    # Run the critique agent to validate the translation
    critique_result = run_agent_loop(
        llm,
        critique_prompt,
        tools,
        anthropic_tools,
        json.dumps(critique_input)
    )
    
    logger.info("Processing complete")
    return critique_result

# Example usage
if __name__ == "__main__":
    # Define input and output directories
    # input_dir = "./tal_files"
    input_dir = "./samples"
    output_dir = "./muki_java_output"
   
    # Create output directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)
    # Process TAL files and get the final Java code
    final_java_code = process_tal_files(input_dir, output_dir)
    # Print the result
    print(final_java_code)

