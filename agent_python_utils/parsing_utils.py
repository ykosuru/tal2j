def extract_code_block(response: str, language: str = None) -> str:
   """
   Extract code block from LLM response, optionally removing language identifier.

   Args:
       response: The string response from the LLM.
       language: Optional language identifier (e.g., "tal", "java") to remove from the start.

   Returns:
       The extracted code block content, or the original response if no block found.
   """
   if '```' not in response:
      return response

   parts = response.split('```', 2)
   if len(parts) < 2:
       return response # Malformed code block

   code_block = parts[1].strip()

   # Remove optional language identifier if provided or detected
   first_line = code_block.split('\n', 1)[0].strip()
   if language and code_block.startswith(language):
       code_block = code_block[len(language):].lstrip()
   elif first_line.isalpha() and first_line in ["tal", "java", "python", "markdown", "md"]: # Common cases
       code_block = code_block[len(first_line):].lstrip()


   return code_block