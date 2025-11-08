import pandas as pd
import json
import os

def convert_table_to_markdown(file_path: str) -> str:
    if file_path.endswith('.csv'):
        df = pd.read_csv(file_path)
    elif file_path.endswith('.xlsx'):
        df = pd.read_excel(file_path)
    else:
        raise ValueError("Unsupported file format. Please provide a .csv or .xlsx file.")

    return df.to_markdown(index=False)

def is_uniform_dict_list(lst):
    if not isinstance(lst, list) or not lst:
        return False
    if not all(isinstance(item, dict) for item in lst):
        return False
    keys = set(lst[0].keys())
    return all(set(item.keys()) == keys for item in lst)

def flatten_nested_fields(item: dict) -> dict:
    flat_item = {}
    for key, value in item.items():
        if isinstance(value, dict):
            for subkey, subvalue in value.items():
                flat_item[f"{subkey}"] = subvalue
        elif isinstance(value, list):
            flat_item[key] = ", ".join(map(str, value))
        else:
            flat_item[key] = value
    return flat_item

def convert_json_to_markdown(file_path: str) -> str:
    with open(file_path, 'r', encoding="utf-8") as file:
        data = json.load(file)

    def recurse(obj, depth=0):
        indent = "  " * depth
        md = ""
        if isinstance(obj, dict):
            for key, value in obj.items():
                if isinstance(value, list) and is_uniform_dict_list(value):
                    # 표로 출력
                    flat_rows = [flatten_nested_fields(item) for item in value]
                    df = pd.DataFrame(flat_rows)
                    md += f"{indent}### {key} (표)\n\n"
                    md += df.to_markdown(index=False)
                    md += "\n\n"
                else:
                    md += f"{indent}- **{key}**:\n{recurse(value, depth + 1)}\n"
        elif isinstance(obj, list):
            if is_uniform_dict_list(obj):
                flat_rows = [flatten_nested_fields(item) for item in obj]
                df = pd.DataFrame(flat_rows)
                md += f"{indent}(표)\n\n"
                md += df.to_markdown(index=False)
                md += "\n\n"
            else:
                for i, item in enumerate(obj):
                    md += f"{indent}- 항목 {i+1}:\n{recurse(item, depth + 1)}\n"
        else:
            md += f"{indent}  {obj}"
        return md

    return recurse(data)

def save_markdown(output_text: str, output_path: str):
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(output_text)

# if __name__ == "__main__":
#     input_file = "example.json"  # 수정 가능
#     output_file = os.path.splitext(input_file)[0] + ".md"

#     if input_file.endswith(('.csv', '.xlsx')):
#         markdown = convert_table_to_markdown(input_file)
#     elif input_file.endswith('.json'):
#         markdown = convert_json_to_markdown(input_file)
#     else:
#         raise ValueError("지원되지 않는 파일 형식입니다.")

#     save_markdown(markdown, output_file)
#     print(f"✅ Markdown 파일이 생성되었습니다: {output_file}")