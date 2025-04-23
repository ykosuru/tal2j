from typing import List, Dict
# Note: You'll need to pass the 'client' and 'model' or initialize them here/globally
# Passing them is generally better practice.

def generate_response(client, model: str, messages: List[Dict], temperature=0.7) -> str:
   """
   Call LLM to get response using the provided client and model.

   Args:
      client: The initialized OpenAI client instance.
      model: The model name string to use.
      messages: List of message dictionaries for the conversation.
      temperature: The sampling temperature.

   Returns:
      The content of the response message.
   """
   try:
      response = client.chat.completions.create(
         model=model,
         n=1,
         temperature=temperature,
         messages=messages
      )
      return response.choices[0].message.content
   except Exception as e:
      print(f"Error calling LLM: {e}")
      # Decide how to handle errors, e.g., return None, raise exception
      raise