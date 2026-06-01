import os
import re
import glob

drawable_dir = r"h:\HOCKY3NAM3\ROOTIE\mobile_rootie_finalproject\app\src\main\res\drawable"

def parse_svg_to_vector(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if there is an <svg> tag anywhere (even inside HTML comments)
    svg_match = re.search(r'<svg[^>]*>.*?</svg>', content, re.DOTALL)
    if not svg_match:
        # If it doesn't contain an <svg>, let's see if there is a commented-out SVG
        commented_svg_match = re.search(r'<!--\s*(<svg[^>]*>.*?</svg>)\s*-->', content, re.DOTALL)
        if commented_svg_match:
            svg_text = commented_svg_match.group(1)
        else:
            return False
    else:
        svg_text = svg_match.group(0)
    
    print(f"Processing: {os.path.basename(file_path)}")
    
    # Extract width, height, and viewBox
    width_match = re.search(r'width="([^"]+)"', svg_text)
    height_match = re.search(r'height="([^"]+)"', svg_text)
    viewbox_match = re.search(r'viewBox="([^"]+)"', svg_text)
    
    width = "24dp"
    height = "24dp"
    viewport_width = "24"
    viewport_height = "24"
    
    if viewbox_match:
        parts = viewbox_match.group(1).split()
        if len(parts) == 4:
            viewport_width = parts[2]
            viewport_height = parts[3]
            width = f"{viewport_width}dp"
            height = f"{viewport_height}dp"
            
    if width_match:
        w_val = width_match.group(1)
        if not w_val.endswith('dp') and not w_val.endswith('px'):
            width = f"{w_val}dp"
        else:
            width = w_val
            
    if height_match:
        h_val = height_match.group(1)
        if not h_val.endswith('dp') and not h_val.endswith('px'):
            height = f"{h_val}dp"
        else:
            height = h_val
            
    # Find all path and circle elements
    elements = re.findall(r'<(path|circle)[^>]*>', svg_text)
    
    # We will iterate and extract all attributes
    vector_paths = []
    
    # Simple regex to find elements and their attributes
    elem_matches = re.finditer(r'<(path|circle)([^>]*)/?>', svg_text)
    for m in elem_matches:
        tag = m.group(1)
        attrs_str = m.group(2)
        
        # Parse attributes
        attrs = {}
        for attr_m in re.finditer(r'([\w-]+)="([^"]*)"', attrs_str):
            attrs[attr_m.group(1)] = attr_m.group(2)
            
        if tag == 'path':
            d = attrs.get('d', '')
            fill = attrs.get('fill', '')
            stroke = attrs.get('stroke', '')
            stroke_width = attrs.get('stroke-width', '')
            stroke_linecap = attrs.get('stroke-linecap', '')
            stroke_linejoin = attrs.get('stroke-linejoin', '')
            fill_rule = attrs.get('fill-rule', '')
            
            # Map fill color
            fill_color = "#FFFFFF" # default
            if fill == 'none':
                fill_color = "#00000000"
            elif fill.startswith('#'):
                fill_color = fill
            elif fill == 'white':
                fill_color = "#FFFFFF"
            elif fill == 'black':
                fill_color = "#000000"
                
            # If fill is missing, standard SVG defaults to black, but let's check if stroke is set
            if 'fill' not in attrs:
                if stroke:
                    fill_color = "#00000000"
                else:
                    fill_color = "#3E4D44" # Use ROOTIE brand green as default if fill is omitted
            
            path_xml = f'    <path\n        android:fillColor="{fill_color}"'
            if d:
                path_xml += f'\n        android:pathData="{d}"'
            if stroke and stroke != 'none':
                path_xml += f'\n        android:strokeColor="{stroke}"'
            if stroke_width:
                path_xml += f'\n        android:strokeWidth="{stroke_width}"'
            if stroke_linecap:
                path_xml += f'\n        android:strokeLineCap="{stroke_linecap}"'
            if stroke_linejoin:
                path_xml += f'\n        android:strokeLineJoin="{stroke_linejoin}"'
            if fill_rule == 'evenodd':
                path_xml += f'\n        android:fillType="evenOdd"'
                
            path_xml += " />"
            vector_paths.append(path_xml)
            
        elif tag == 'circle':
            cx = float(attrs.get('cx', '0'))
            cy = float(attrs.get('cy', '0'))
            r = float(attrs.get('r', '0'))
            fill = attrs.get('fill', '#FFFFFF')
            
            # Convert circle to SVG path
            d = f"M {cx-r},{cy} a {r},{r} 0 1,0 {r*2},0 a {r},{r} 0 1,0 {-r*2},0"
            
            fill_color = "#FFFFFF"
            if fill == 'none':
                fill_color = "#00000000"
            elif fill.startswith('#'):
                fill_color = fill
            elif fill == 'white':
                fill_color = "#FFFFFF"
            elif fill == 'black':
                fill_color = "#000000"
                
            path_xml = f'    <path\n        android:fillColor="{fill_color}"\n        android:pathData="{d}" />'
            vector_paths.append(path_xml)
            
    # Construct the final Vector XML
    vector_xml = f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    vector_xml += f'    android:width="{width}"\n'
    vector_xml += f'    android:height="{height}"\n'
    vector_xml += f'    android:viewportWidth="{viewport_width}"\n'
    vector_xml += f'    android:viewportHeight="{viewport_height}">\n'
    vector_xml += '\n'.join(vector_paths)
    vector_xml += '\n</vector>\n'
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(vector_xml)
    print(f"Successfully converted {os.path.basename(file_path)}")
    return True

# Scan all XML files in drawable folder
xml_files = glob.glob(os.path.join(drawable_dir, "*.xml"))
count = 0
for file in xml_files:
    if parse_svg_to_vector(file):
        count += 1

print(f"Total files converted: {count}")
