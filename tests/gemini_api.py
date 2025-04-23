from google import genai
import os

# GEMINI_API_KEY = os.getenv("GEMINI_API_NEOTEK_KEY")
GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")

# Load the API key from environment variables
GEMINI_API_KEY = os.getenv("GEMINI_API_MUKI_DEV_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_MUKI_DEV_KEY is not set in the environment.")

# Gemini Models that work with API version v1beta
# This works
model_15_pro = "gemini-1.5-pro-latest"
# This does not work with API version v1beta
model_25_pro = "gemini-2.5-pro-preview-03-25"
# This works 
model_25_pro_exp = "gemini-2.5-pro-exp"

# Assign the model name to use
model = model_25_pro 

client = genai.Client(api_key=GEMINI_API_KEY)

## Check what models are available
# List available models
try:
    models = client.models.list()  # Assuming `list()` is the correct method
    print("Available Models:")
    for model in models:
        print(f"Model ID: {model.display_name}, Description: {model.description}")
except Exception as e:
    print(f"An error occurred while listing models: {e}")



# chat = client.chats.create(model=model)

# response = chat.send_message("I have 2 dogs in my house.")
# print(response.text)

# response = chat.send_message("How many paws are in my house?")
# print(response.text)

# for message in chat.get_history():
#     print(f'role - {message.role}',end=": ")
#     print(message.parts[0].text)