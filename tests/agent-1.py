from re import M
from litellm import completion
from typing import List, Dict
import sys
import os
from openai import OpenAI

def generate_response(messages: List[Dict], model_name: str=None, temperature=0.7) -> str:
   """Call LLM to get response"""
   # response = completion(
   #    model="openai/gpt-4",
   #    messages=messages,
   #    max_tokens=1000000
   # )
   """
   Call LLM to get response
   
   Args:
      messages: List of message dictionaries for the conversation
      model_name: Optional model name to use (defaults to global model if not specified)
   
   Returns:
      The content of the response message
   """

   model_to_use = model_name if model_name else model
   temperature = temperature
   response = client.chat.completions.create(
      model=model_to_use,
      n=1,
      temperature=temperature,
      messages=messages
   )

   return response.choices[0].message.content

# Set the system prompt from a file_content
# Read the system prompt from a file
# Function to read the system prompt from a file
def read_system_prompt_file(file_path):
    """
    Reads the system prompt from the specified file.
    :param file_path: Path to the file containing the system prompt.
    :return: The content of the file as a string.
    """
    try:
        with open(file_path, 'r') as file:
            return file.read().strip()
    except FileNotFoundError:
        raise ValueError(f"System prompt file not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the system prompt file: {e}")

def extract_code_block(response: str) -> str:
   """Extract code block from response"""

   if not '```' in response:
      return response

   code_block = response.split('```')[1].strip()
   # Check for "tal" or "java" at the start and remove

   if code_block.startswith("tal"):
      code_block = code_block[3:]
   elif code_block.startswith("java"):
      code_block = code_block[4:]

   return code_block
# Set the system prompt from a file_content
# Read the system prompt from a file

# Function to read the system prompt from a file
def read_prompt_file(file_path):
    """
    Reads the system prompt from the specified file.
    :param file_path: Path to the file containing the system prompt.
    :return: The content of the file as a string.    
    """
    try:
        with open(file_path, 'r') as file:
            return file.read().strip()
    except FileNotFoundError:
        raise ValueError(f"System prompt file not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the system prompt file: {e}")

def read_tal_file(file_path):
    """
    Reads the TAL file for processing
    :param file_path: Path to the file containing the TAL file.
    :return: The content of the file as a string.
    """
    try:
        with open(file_path, 'r') as file:
            return file.read().strip()
    except FileNotFoundError:
        raise ValueError(f"System prompt file not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the system prompt file: {e}")

def write_to_file(content: str, output_dir: str, filename: str, subfolder: str = None) -> str:
    """
    Write content to a file in the specified output directory.
    
    Args:
        content: The content to write to the file
        output_dir: The base output directory (project root or absolute path)
        filename: The name of the file to create
        subfolder: Optional subfolder within the output directory
    
    Returns:
        The full path to the created file
    """
    # Create the full output path
    if subfolder:
        full_output_dir = os.path.join(output_dir, subfolder)
    else:
        full_output_dir = output_dir
    
    # Create directory if it doesn't exist
    os.makedirs(full_output_dir, exist_ok=True)
    
    # Create the full file path
    output_file_path = os.path.join(full_output_dir, filename)
    
    # Write the content to the file
    try:
        with open(output_file_path, 'w') as f:
            f.write(content)
            print (f"output file : {filename} written to directory : {full_output_dir}")
        return output_file_path
    except Exception as e:
        print(f"Error writing to {output_file_path}: {str(e)}")
        raise

def process_request ():

   # For paths relative to the project folder
   # Define the project root folder explicitly
   # TODO: Fix this later
   project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

   # Load the System Prompt
   # Read the system prompt from the file
   # Load the system prompt
   system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt")
   system_prompt = read_prompt_file(system_prompt_file)
   
   # Read the tal file
   # TODO: Define TAL File path relative to the project folder
   tal_file_path = os.path.join(project_folder, "samples", "sample2.tal")
   tal_file = read_tal_file(tal_file_path) 

   # Set the system and user messages   
   # Hard coding for now, but should be readable from a file so developers can manipuate outside the code
   messages = [
      {"role": "system", "content": system_prompt}
   ]

   # -----------STEP-1: Generate Comments with Explanation ---------------------  
   # First prompt - Document TAL code
   messages.append({
      "role": "user",
      "content": f"Explain the TAL code in the TAL program appended. Insert comments into the file as appropriate before the line and not at the end of the line. Output the documented code in the code block ```tal Code Documented ```. Here is the TAL program:  {tal_file}."
   })
   tal_documented_code = generate_response(messages)

   # Parse the response to get the function code
   tal_documented_code = extract_code_block(tal_documented_code)

   # Save documented code
   base_filename = os.path.basename(tal_file_path)  # Get the original filename
   write_to_file(
      content=tal_documented_code,
      output_dir=project_folder,
      filename=base_filename,
      subfolder="output/documented_tal"
   )


   # -----------STEP-2: Generate Tech Spec ---------------------
   # Add assistant's response to conversation
   # Only include the generated code with comments

   messages.append({"role": "assistant", "content": "```TAL Documented Code\n\n" + tal_documented_code+"\n\n```"})

   # # Second prompt - Add documentation
   messages.append({
      "role": "user",
      "content": "Generate comprehensive documentation for this TAL Program, including description, parameters, "
                 "return value, examples, and edge cases. Output the documentation in a mark down document format in the code block ```tal Technical Documentation```."
   })
   tal_tech_doc_md = generate_response(messages=messages, temperature=0.5)
   tal_tech_doc_md = extract_code_block(tal_tech_doc_md)

   # Save technical documentation
   write_to_file(
      content=tal_tech_doc_md,
      output_dir=project_folder,
      filename=f"{os.path.splitext(base_filename)[0]}_TAL_tech_doc.md",
      subfolder="output/tal_technical_docs"
   )

   # -----------STEP-3 Generate Tech Spec for Java ---------------------
   # Add assistant's response to conversation
   # Only include the generated code with comments

   messages.append({"role": "assistant", "content": "```TAL Program Technical Specifications\n\n" + tal_tech_doc_md + "\n\n```"})

   # # Third prompt - Add documentation
   messages.append({
      "role": "user",
      "content": "Generate comprehensive equivalent java technical specification documentation from the generated TAL technical specification, that can be implemented as java code. Include description, parameters, return value, examples, and edge cases. Output the documentation in a mark down document format in the code block ```java Technical Documentation```."
   })

   # Using temperature = 0.5 to restrict it to tal specifications generated. May need to play around here.
   java_tech_doc_md = generate_response(messages=messages, temperature=0.5)
   java_tech_doc_md = extract_code_block(java_tech_doc_md)

   # Save the java technical documentation
   java_tech_doc_md_filename = write_to_file(
      content=java_tech_doc_md,
      output_dir=project_folder,
      filename=f"{os.path.splitext(base_filename)[0]}_java_tech_doc.md",
      subfolder="output/java_technical_docs"
   )
  
   # -----------STEP-4 Generate Java Code from the Java Tech Spec ---------------------
   # Add assistant's response to conversation
   # Only include the generated code with comments

   messages.append({"role": "assistant", "content": "```Java Technical Specifications \n\n" + java_tech_doc_md + "\n\n```"})

   # # Fourth prompt - Add documentation
   messages.append({
      "role": "user",
      "content": "Generate Java code from the generated Java technical specification, including description, parameters, return value, examples, and edge cases. Output the documentation in a mark down document format in the code block ```java Code```."
   })
   java_code = generate_response(messages)
   java_code = extract_code_block(java_code)

   # Save the java code
   # TODO - Java code filename should reflect the Java class in the code
   java_code = write_to_file(
      content=java_code,
      output_dir=project_folder,
      filename=f"{os.path.splitext(base_filename)[0]}.java",
      subfolder="output/java_code"
   )
   # # Add documentation response to conversation
   # messages.append({"role": "assistant", "content": "\`\`\`python\n\n"+documented_function+"\n\n\`\`\`"})

   # # Third prompt - Add test cases
   # messages.append({
   #    "role": "user",
   #    "content": "Add unittest test cases for this function, including tests for basic functionality, "
   #               "edge cases, error cases, and various input scenarios. Output the code in a \`\`\`python code block\`\`\`."
   # })
   # test_cases = generate_response(messages)
   # # We will likely run into random problems here depending on if it outputs JUST the test cases or the
   # # test cases AND the code. This is the type of issue we will learn to work through with agents in the course.
   # test_cases = extract_code_block(test_cases)
   # print("\n=== Test Cases ===")
   # print(test_cases)

   # Generate filename from TAL program description
   # filename = tal_file.lower()
   # filename = ''.join(c for c in filename if c.isalnum() or c.isspace())
   # filename = filename.replace(' ', '_')[:30] + '.tal'

   #    # Generate filename and save to output directory relative to project root
   # output_dir = os.path.join(project_folder, "output", "documented_tal")
   # os.makedirs(output_dir, exist_ok=True)  # Create output directory if it doesn't exist
   
   # # Create filename from TAL program description
   # base_filename = os.path.basename(tal_file_path)  # Get the original filename
   # output_filename = os.path.join(output_dir, base_filename)
   
   # # Save final version
   # with open(output_filename, 'w') as f:
   #     f.write(tal_documented_code)


   return tal_documented_code, tal_tech_doc_md, java_tech_doc_md, java_code

if __name__ == "__main__":

   # Available Gemini-2.5-Pro Models supported for v1Beta API:
# Model Description: models/gemini-2.5-pro-exp-03-25
# Model Description: models/gemini-2.5-pro-preview-03-25

# Initialize LLM parameters
# Load the API key from environment variables

   GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
   if not GEMINI_API_KEY:
      raise ValueError("GEMINI_API_MUKI_DEV_KEY is not set in the environment.")

   # Initialize the client
   client = OpenAI(
      api_key=GEMINI_API_KEY,
      base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
   )

   model = "gemini-2.5-pro-exp-03-25"

   # function_code, tests, filename = generate_response()
   tal_documented_code, tal_tech_doc_md, java_tech_doc_md, java_code = process_request()

   #TODO: Keep a count of the tokens through the process
