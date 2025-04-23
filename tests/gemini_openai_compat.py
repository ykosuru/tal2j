from openai import OpenAI
import os
import json

# Path to the system prompt file
# Dynamically determine the project folder based on the script's location
# For absolute path
project_folder = os.path.dirname(os.path.abspath(__file__))

# For paths relative to the project folder
# Define the project root folder explicitly as the tal2j folder
project_folder = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


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

# Check available models for the OPENAI APIs
## Check what models are available
# List available models
# try:
#     models = client.models.list()
#     print("Available Gemini-2.0-Pro Models:")

#     # Filter models containing "gemini-2.0-pro"
#     gemini_2_5_pro_models = [model for model in models if "gemini-2.5-pro" in model.id]

#     if not gemini_2_5_pro_models:
#         print("No gemini-2.5-pro models found.")
#     else:
#         for model in gemini_2_5_pro_models:
#             print(f"Model Description: {model.id}")

# except Exception as e:
#     print(f"An error occurred while listing models: {e}")

# Set the model?
# model = model_25_pro_exp

model = "gemini-2.5-pro-exp-03-25"

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

def read_prompts_as_list(file_path):
    """
    Reads a file containing multiple prompts (one per line) and returns them as a list.
    :param file_path: Path to the file containing the prompts.
    :return: A list of prompts.
    """
    try:
        with open(file_path, 'r') as file:
            # Read all lines, strip whitespace, and return as a list
            return [line.strip() for line in file if line.strip()]
    except FileNotFoundError:
        raise ValueError(f"Prompt file not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the prompt file: {e}")

def load_prompts(file_path):
    """
    Load user prompts from a JSON file and return them as a list of prompt objects.
    """
    try:
         with open(file_path, 'r') as file:
            prompts = json.load(file)
         return prompts
    except FileNotFoundError:
        raise ValueError(f"Prompt file not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the prompt file: {e}")

# Setup the messages array
messages = []

# Load the system prompt
system_prompt_file = os.path.join(project_folder, "prompts", "system_prompt.txt")
system_prompt = read_prompt_file(system_prompt_file)

# Add the system prompt to the messages array
messages.append({"role": "system", "content": system_prompt})

# Load the user prompts
user_prompts_file = os.path.join(project_folder, "prompts", "user_prompts.txt")
user_prompts = read_prompts_as_list(user_prompts_file)

# Add each user prompt to the messages array
for prompt in user_prompts:
    messages.append({"role": "user", "content": prompt})

# Print the messages array for debugging
print("Messages Array:")
for message in messages:
    print(message)

# Use the messages array in the chat completion
response = client.chat.completions.create(
    model=model,
    n=1,
    messages=messages
)

# Print the response
print("Response:")
print(response.choices[0].message)
