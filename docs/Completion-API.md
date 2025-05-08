# client.chat.completions.create API Details

Okay, here's a breakdown of the common parameters for the `client.chat.completions.create` method, based on the OpenAI API standard which the Gemini compatibility layer aims to follow. Note that not all parameters might be fully supported or behave identically with the Gemini backend compared to OpenAI models.

```python
# Example structure
response = client.chat.completions.create(
    model="gemini-2.5-pro-exp-03-25",  # Or another model ID
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Explain the theory of relativity."}
    ],
    # --- Optional Parameters Below ---
    temperature=0.7,
    top_p=1.0,
    n=1,
    max_tokens=150,
    # stop=["\n", " Human:"],
    # presence_penalty=0.0,
    # frequency_penalty=0.0,
    # logit_bias=None,
    # user="user-1234",
    # response_format={"type": "text"}, # or {"type": "json_object"}
    # seed=42,
    # tools=None,
    # tool_choice=None,
    # stream=False
)
```

### Required Parameters:

1.  **`model`** (`str`)

    - **Explanation**: The ID of the model to use for the completion (e.g., `"gemini-2.5-pro-exp-03-25"`, `"gpt-4"`, etc.).
    - **Example**: `model="gemini-2.5-pro-exp-03-25"`

2.  **`messages`** (`List[Dict]`)
    - **Explanation**: A list of message objects representing the conversation history. Each message object must have a `role` (`"system"`, `"user"`, or `"assistant"`) and `content` (`str`). The conversation must start with an optional `system` message, followed by alternating `user` and `assistant` messages. The last message must be from the `user`.
    - **Example**:
      ```python
      messages=[
          {"role": "system", "content": "You are a helpful assistant."},
          {"role": "user", "content": "Who won the world series in 2020?"},
          {"role": "assistant", "content": "The Los Angeles Dodgers won the World Series in 2020."},
          {"role": "user", "content": "Where was it played?"}
      ]
      ```

### Optional Parameters:

3.  **`temperature`** (`float`, default: typically 0.7 or 1.0)

    - **Explanation**: Controls randomness. Lowering the temperature (e.g., 0.2) makes the output more focused and deterministic. Increasing it (e.g., 1.0) makes it more random and creative. A value of 0 aims for the most likely output.
    - **Range**: 0.0 to 2.0 (check specific model limits).
    - **Example**: `temperature=0.5` (for more predictable output)

4.  **`top_p`** (`float`, default: 1.0)

    - **Explanation**: Nucleus sampling. The model considers only the tokens comprising the top `p` probability mass. So, `0.1` means only tokens comprising the top 10% probability mass are considered. It's an alternative to `temperature`. It's generally recommended to alter only one of `temperature` or `top_p`.
    - **Range**: 0.0 to 1.0.
    - **Example**: `top_p=0.9` (considers top 90% probability mass)

5.  **`n`** (`int`, default: 1)

    - **Explanation**: How many chat completion choices to generate for each input message. Note that you will be charged for all generated tokens across all choices.
    - **Example**: `n=3` (generates 3 different responses)

6.  **`max_tokens`** (`int`, default: depends on model)

    - **Explanation**: The maximum number of tokens to generate in the chat completion. The total length of input tokens and generated tokens is limited by the model's context length.
    - **Example**: `max_tokens=500`

7.  **`stop`** (`str` or `List[str]`, default: `None`)

    - **Explanation**: Up to 4 sequences where the API will stop generating further tokens. The returned text will not contain the stop sequence.
    - **Example**: `stop=["\n", "Observation:"]`

8.  **`presence_penalty`** (`float`, default: 0.0)

    - **Explanation**: Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.
    - **Range**: -2.0 to 2.0.
    - **Example**: `presence_penalty=0.5` (discourages repeating tokens already present)

9.  **`frequency_penalty`** (`float`, default: 0.0)

    - **Explanation**: Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.
    - **Range**: -2.0 to 2.0.
    - **Example**: `frequency_penalty=0.2` (discourages repeating frequent tokens)

10. **`logit_bias`** (`Dict[str, float]`, default: `None`)

    - **Explanation**: Modifies the likelihood of specified tokens appearing in the completion. Accepts a JSON object mapping token IDs (integers) to bias values (-100 to 100). Use cautiously.
    - **Example**: `logit_bias={"1758": -100}` (strongly discourages token ID 1758)

11. **`user`** (`str`, default: `None`)

    - **Explanation**: A unique identifier representing your end-user, which can help the API provider monitor and detect abuse.
    - **Example**: `user="user-id-5678"`

12. **`response_format`** (`Dict`, default: `{"type": "text"}`)

    - **Explanation**: Specifies the format for the model's output. Use `{"type": "json_object"}` to enable JSON mode (requires specific instructions in the prompt and model support).
    - **Example**: `response_format={"type": "json_object"}`

13. **`seed`** (`int`, default: `None`)

    - **Explanation**: If specified, the system will make a best effort to sample deterministically, such that repeated requests with the same `seed` and parameters should return the same result. Determinism is not guaranteed. Requires compatible models.
    - **Example**: `seed=12345`

14. **`tools`** (`List[Dict]`, default: `None`)

    - **Explanation**: A list of tools the model may call. Currently, only functions are supported.
    - **Example**: See OpenAI documentation for function calling format.

15. **`tool_choice`** (`str` or `Dict`, default: `None`)

    - **Explanation**: Controls which (if any) tool is called by the model. `"none"` means no tool call, `"auto"` is default, or specify a function like `{"type": "function", "function": {"name": "my_function"}}`.
    - **Example**: `tool_choice={"type": "function", "function": {"name": "get_weather"}}`

16. **`stream`** (`bool`, default: `False`)
    - **Explanation**: If set to `True`, partial message deltas will be sent back as data-only server-sent events as they become available.
    - **Example**: `stream=True`

Remember to check the specific documentation for the Gemini API compatibility layer or the model you are using, as support and behavior for these parameters might vary.

Similar code found with 2 license types
