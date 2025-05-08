import os
from dotenv import load_dotenv

# --- Core LangChain Components ---
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# --- Google Gemini Integration ---
# Use ChatGoogleGenerativeAI for chat-based interactions
from langchain_google_genai import ChatGoogleGenerativeAI

# --- 1. Load Environment Variables ---
# Load the .env file (if you're using one)
load_dotenv()

# Specify the model name to use
model_15_pro = "gemini-1.5-pro-latest"
model_25_pro = "gemini-2.5-pro-preview-03-25"
model_25_pro_exp = "gemini-2.5-pro-exp"

# Specify the model to use
model = model_25_pro

# Securely get the API key from the environment
# google_api_key = os.getenv("GEMINI_API_MUKI_DEV_KEY")
google_api_key = os.getenv("GEMINI_API_NEOTEK_KEY")

if not google_api_key:
    raise ValueError("GOOGLE_API_KEY not found in environment variables.")

# --- 2. Initialize the Gemini Model ---
# Specify the model name. "gemini-1.5-pro-latest" is the identifier for Gemini 1.5 Pro.
# You can adjust temperature (randomness) and other parameters here.
llm = ChatGoogleGenerativeAI(
    model=model,
    google_api_key=google_api_key, # Pass the key securely
    temperature=0.7,
    # convert_system_message_to_human=True # Optional: Some models might work better if system messages are treated as human messages. Test if needed.
)

# --- 3. Define a Prompt Template ---
# This structure helps guide the model's response.
# Use {variable_name} for placeholders.
prompt_template = ChatPromptTemplate.from_messages(
    [
        ("system", "You are a helpful assistant that translates English to French."),
        ("human", "Translate this sentence: {sentence}"),
    ]
)

# --- 4. Create an Output Parser ---
# This simply extracts the string content from the model's output message.
output_parser = StrOutputParser()

# --- 5. Create the LangChain Chain using LCEL (LangChain Expression Language) ---
# This pipes the components together: prompt -> model -> output parser
chain = prompt_template | llm | output_parser

# --- 6. Invoke the Chain ---
# Provide the input variables defined in your prompt template.
input_sentence = "Hello, how are you?"
print(f"Input Sentence: {input_sentence}")

try:
    response = chain.invoke({"sentence": input_sentence})
    print("\n--- Gemini 1.5 Pro Response ---")
    print(response)

except Exception as e:
    print(f"\nAn error occurred: {e}")
    print("Please check your API key, model name, and internet connection.")

# --- Example 2: Simple Invocation (without explicit chain) ---
print("\n--- Simple Invocation Example ---")
try:
    simple_response = llm.invoke(f"Explain the concept of 'Large Language Model' in simple terms.")
    # The response object is an AIMessage, access content with .content
    print(simple_response.content)
except Exception as e:
    print(f"\nAn error occurred during simple invocation: {e}")

# --- Example 3: Using a different prompt structure ---
print("\n--- Creative Writing Example ---")
creative_prompt = ChatPromptTemplate.from_template(
    "Write a short, futuristic story (max 100 words) about a {topic}."
)
creative_chain = creative_prompt | llm | output_parser
try:
    story = creative_chain.invoke({"topic": "cat exploring Mars"})
    print(story)
except Exception as e:
    print(f"\nAn error occurred during creative writing: {e}")