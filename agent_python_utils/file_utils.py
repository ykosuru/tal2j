import os

def read_prompt_file(file_path):
    """
    Reads a prompt (or any text file) from the specified file.
    :param file_path: Path to the file.
    :return: The content of the file as a string.
    """
    try:
        with open(file_path, 'r') as file:
            return file.read().strip()
    except FileNotFoundError:
        raise ValueError(f"File not found at: {file_path}")
    except Exception as e:
        raise ValueError(f"An error occurred while reading the file {file_path}: {e}")

# Renamed for clarity, as it reads any text file, not just TAL
def read_text_file(file_path):
    """
    Reads a text file for processing.
    :param file_path: Path to the file.
    :return: The content of the file as a string.
    """
    return read_prompt_file(file_path) # Reuse the core logic

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
            print (f"Output file: {filename} written to directory: {full_output_dir}")
        return output_file_path
    except Exception as e:
        print(f"Error writing to {output_file_path}: {str(e)}")
        raise