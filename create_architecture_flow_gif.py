import os
from PIL import Image, ImageDraw, ImageFont

def create_architecture_flow_gif():
    width, height = 1040, 660
    bg_dark = (15, 23, 42)        # Slate 900
    panel_bg = (30, 41, 59)       # Slate 800
    card_bg = (23, 32, 51)

    # Colors
    text_white = (248, 250, 252)
    text_gray = (148, 163, 184)
    text_cyan = (56, 189, 248)
    text_purple = (192, 132, 252)
    text_green = (52, 211, 153)
    text_amber = (251, 191, 36)

    blue_accent = (59, 130, 246)
    purple_accent = (168, 85, 247)
    emerald_accent = (16, 185, 129)
    rose_accent = (244, 63, 94)

    # Load Fonts
    try:
        font_title = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 26)
        font_sub = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 17)
        font_node_header = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 18)
        font_body = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 14)
        font_badge = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 12)
    except:
        font_title = font_sub = font_node_header = font_body = font_badge = ImageFont.load_default()

    frames = []

    # Node boxes coordinates
    client_box = [40, 140, 240, 520]
    netty_box = [290, 140, 470, 520]
    engine_box = [520, 140, 750, 520]
    storage_box = [800, 140, 1000, 520]

    stages = [
        ("Step 1: Client Sends Request", "Client SDK sends SQL Query + Vector embedding parameters over HTTP", 0.0),
        ("Step 2: Security & Netty Handler", "Netty receives HTTP packet, verifies PBKDF2 Auth & Rate Limiting", 0.2),
        ("Step 3: SQL Parser & Query Optimizer", "Parses AST and selects optimal strategy (Vector HNSW + BM25)", 0.4),
        ("Step 4: Storage & Index Engine Lookup", "Executes parallel lookup in LSM Tree, HNSW Graph & Inverted Index", 0.6),
        ("Step 5: AI Engine & Stream Notification", "Evaluates AI SQL functions and publishes real-time topic stream", 0.8),
        ("Step 6: High-Speed Response (<12ms)", "Serializes JSON result packet and delivers response back to Client", 1.0)
    ]

    # Generate 60 frames (10 frames per step for smooth flow)
    total_frames = 60
    for frame_idx in range(total_frames):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)

        # Header banner gradient line
        draw.rectangle([0, 0, width, 6], fill=purple_accent)

        # Current step info
        step_idx = min(frame_idx // 10, len(stages) - 1)
        step_title, step_desc, progress = stages[step_idx]

        # Draw Header
        draw.text((40, 25), "⚡ SyntricDB Architecture Data Flow", fill=text_white, font=font_title)
        draw.text((40, 62), f"{step_title} — {step_desc}", fill=text_cyan, font=font_sub)

        # Draw 4 System Columns (Client -> Transport -> Query Engine -> Storage Layer)
        
        # 1. CLIENT APPLICATION
        draw.rounded_rectangle(client_box, radius=14, fill=card_bg, outline=blue_accent, width=2)
        draw.rectangle([40, 140, 240, 185], fill=(30, 58, 138))
        draw.text((55, 152), "💻 Client App / SDK", fill=text_white, font=font_node_header)
        client_items = ["• Python Client", "• Node.js SDK", "• Spring Data JPA", "• REST / cURL", "• Go / C# Client"]
        for idx, item in enumerate(client_items):
            draw.text((55, 210 + idx * 35), item, fill=text_gray, font=font_body)

        # 2. NETTY & SECURITY HANDLER
        draw.rounded_rectangle(netty_box, radius=14, fill=card_bg, outline=purple_accent, width=2)
        draw.rectangle([290, 140, 470, 185], fill=(88, 28, 135))
        draw.text((305, 152), "🔒 Netty Gateway", fill=text_white, font=font_node_header)
        netty_items = ["• Netty 4 Server", "• PBKDF2 Auth", "• Rate Limiter", "• IP Firewall", "• REST Handler"]
        for idx, item in enumerate(netty_items):
            draw.text((305, 210 + idx * 35), item, fill=text_gray, font=font_body)

        # 3. QUERY & AI ENGINE
        draw.rounded_rectangle(engine_box, radius=14, fill=card_bg, outline=emerald_accent, width=2)
        draw.rectangle([520, 140, 750, 185], fill=(6, 78, 59))
        draw.text((535, 152), "⚙️ Query & AI Engine", fill=text_white, font=font_node_header)
        engine_items = ["• AST SQL Parser", "• Query Optimizer", "• AI_EMBED()", "• AI_SUMMARIZE()", "• Stream Pub/Sub"]
        for idx, item in enumerate(engine_items):
            draw.text((535, 210 + idx * 35), item, fill=text_gray, font=font_body)

        # 4. UNIFIED STORAGE & INDEXES
        draw.rounded_rectangle(storage_box, radius=14, fill=card_bg, outline=rose_accent, width=2)
        draw.rectangle([800, 140, 1000, 185], fill=(136, 19, 55))
        draw.text((815, 152), "💾 Unified Storage", fill=text_white, font=font_node_header)
        storage_items = ["• LSM-Tree WAL", "• MemTable / SSTable", "• HNSW Vector Graph", "• BM25 Inverted Index", "• LRU Hot Cache"]
        for idx, item in enumerate(storage_items):
            draw.text((815, 210 + idx * 35), item, fill=text_gray, font=font_body)

        # Draw Flow Connectors (Arrows between nodes)
        draw.line([240, 330, 290, 330], fill=text_gray, width=2)
        draw.line([470, 330, 520, 330], fill=text_gray, width=2)
        draw.line([750, 330, 800, 330], fill=text_gray, width=2)

        # Animated Glowing Packets
        # Forward Request Packet (Client -> DB) for frames 0 to 30
        if frame_idx <= 30:
            pct = frame_idx / 30.0
            pkt_x = 140 + int(pct * 720) # Move from x=140 to x=860
            pkt_y = 330
            # Packet circle
            draw.ellipse([pkt_x - 12, pkt_y - 12, pkt_x + 12, pkt_y + 12], fill=emerald_accent, outline=text_white, width=2)
            draw.text((pkt_x - 28, pkt_y - 28), "SQL Request ➔", fill=text_green, font=font_badge)

        # Backward Response Packet (DB -> Client) for frames 30 to 60
        else:
            pct = (frame_idx - 30) / 30.0
            pkt_x = 860 - int(pct * 720) # Move back from x=860 to x=140
            pkt_y = 360
            # Packet circle
            draw.ellipse([pkt_x - 12, pkt_y - 12, pkt_x + 12, pkt_y + 12], fill=blue_accent, outline=text_white, width=2)
            draw.text((pkt_x - 30, pkt_y + 16), "⬅ JSON (<12ms)", fill=text_cyan, font=font_badge)

        # Footer Metric Bar
        draw.rounded_rectangle([40, 560, 1000, 620], radius=10, fill=(30, 41, 59))
        draw.text((60, 580), "⏱️ End-to-End Latency: <12ms", fill=text_green, font=font_node_header)
        draw.text((400, 580), "🛡️ Protocol: HTTP/REST over Netty 4", fill=text_cyan, font=font_node_header)
        draw.text((750, 580), "⚡ Unified Engine Mode", fill=text_amber, font=font_node_header)

        frames.append(img)

    output_path = "/Users/upendrakumarmanike/Documents/NovaDB/syntricdb_architecture_flow.gif"
    frames[0].save(
        output_path,
        save_all=True,
        append_images=frames[1:],
        duration=100,
        loop=0
    )
    print(f"Architecture Flow GIF created at {output_path}")

create_architecture_flow_gif()
