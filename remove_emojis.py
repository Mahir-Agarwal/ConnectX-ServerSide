import os, re

custom_symbols = [
    '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '',
    '', '', ''
]

# Adding some extra AI symbols if they slipped through
def remove_emojis_from_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
        modified = content
        for symbol in custom_symbols:
            modified = modified.replace(symbol, '')
            
        # Basic regex for emoji ranges
        emoji_pattern = re.compile(
            u"("
            u"\ud83c[\udf00-\udfff]|"
            u"\ud83d[\udc00-\ude4f\ude80-\udeff]|"
            u"[\u2600-\u26FF\u2700-\u27BF]|"
            u"[\u2190-\u21FF]|"
            u"[\u2300-\u23FF]"
            u")+", flags=re.UNICODE)
            
        modified = emoji_pattern.sub(r'', modified)
        
        # Another pass for unicode emojis using characters like '', '', ''
        # These are in the supplementary multilingual plane. We can build a regex for \U00010000-\U0010FFFF (all supplementary planes).
        # Actually in python 3 it's easy:
        # We can just check for any char with ord(c) > 0x1F000
        
        filtered = []
        for c in modified:
            if 0x1F300 <= ord(c) <= 0x1FAFF: # Main emoji block
                continue
            if 0x2600 <= ord(c) <= 0x27BF: # Misc symbols and dingbats
                continue
            filtered.append(c)
        modified = "".join(filtered)

        if modified != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(modified)
            print(f"Updated {filepath}")
    except Exception as e:
        # Ignore files that aren't utf-8 text (like binaries)
        pass

for root, dirs, files in os.walk(r'm:\ConnectX'):
    # Exclude directories
    dirs[:] = [d for d in dirs if d not in ['.git', 'node_modules', 'target', '.idea', 'build', '.gemini']]
    for file in files:
        if file.endswith(('.java', '.js', '.py', '.md', '.yml', '.yaml', '.xml', '.properties', '.txt', '.sh', '.bat', '.env.example', '.json')):
            remove_emojis_from_file(os.path.join(root, file))
print("Done!")
