import os
import shutil

root_dir = "."

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if 'com.uber' in content:
        new_content = content.replace('com.uber', 'com.adaptive')
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for dirpath, dirnames, filenames in os.walk(root_dir):
    if '.git' in dirpath or 'target' in dirpath:
        continue
        
    for filename in filenames:
        if filename.endswith(('.java', '.xml', '.properties', '.imports')):
            replace_in_file(os.path.join(dirpath, filename))

print("File contents updated.")

# Now rename directories
for dirpath, dirnames, filenames in os.walk(root_dir, topdown=False):
    if '.git' in dirpath or 'target' in dirpath:
        continue
    
    if os.path.basename(dirpath) == 'uber':
        parent = os.path.dirname(dirpath)
        new_dir = os.path.join(parent, 'adaptive')
        os.rename(dirpath, new_dir)
        print(f"Renamed directory {dirpath} to {new_dir}")

print("Directory structure updated.")
