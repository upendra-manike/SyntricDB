import os
from PIL import Image, ImageDraw, ImageFont

def create_comparison_gif():
    width, height = 1000, 620
    bg_dark = (15, 23, 42)      # #0F172A Slate 900
    card_bg_red = (30, 27, 38)   # Dark crimson shadow
    card_bg_green = (20, 35, 45) # Dark emerald shadow

    # Colors
    text_white = (248, 250, 252)
    text_gray = (148, 163, 184)
    red_accent = (244, 63, 94)    # Rose 500
    green_accent = (16, 185, 129) # Emerald 500
    blue_accent = (59, 130, 246)  # Blue 500
    purple_accent = (168, 85, 247)# Purple 500
    yellow_accent = (245, 158, 11)

    # Load Font
    try:
        font_title = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 28)
        font_sub = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 18)
        font_card_header = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 20)
        font_body = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 15)
        font_badge = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 13)
    except:
        font_title = font_sub = font_card_header = font_body = font_badge = ImageFont.load_default()

    frames = []

    def draw_base_layout(title, subtitle):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        # Header banner gradient line
        draw.rectangle([0, 0, width, 6], fill=purple_accent)
        # Title
        draw.text((40, 25), title, fill=text_white, font=font_title)
        draw.text((40, 65), subtitle, fill=text_gray, font=font_sub)
        return img, draw

    # --- SLIDE 1: Architecture Comparison ---
    for step in range(30): # 3 seconds at 100ms per frame
        img, draw = draw_base_layout(
            "⚡ SyntricDB vs. Traditional Multi-Database Stack",
            "Comparing Unified AI-Native Engine vs. Fragmented Database Infrastructure"
        )

        # Left Box: Traditional Stack
        draw.rounded_rectangle([40, 110, 480, 560], radius=16, fill=(24, 28, 42), outline=red_accent, width=2)
        draw.rectangle([40, 110, 480, 160], fill=(45, 20, 30))
        draw.text((60, 125), "❌ Traditional Fragmented Stack", fill=red_accent, font=font_card_header)

        trad_items = [
            ("🐘 PostgreSQL", "Relational SQL Data"),
            ("🔴 Redis", "In-Memory Cache Layer"),
            ("🔍 Elasticsearch", "Full-Text BM25 Search"),
            ("🌲 Pinecone / Qdrant", "Vector Embeddings (HNSW)"),
            ("⚡ Apache Kafka", "Event Stream Processing"),
            ("🤖 External LLM API", "High-latency HTTP calls")
        ]

        y_pos = 180
        for item, desc in trad_items:
            draw.text((60, y_pos), item, fill=text_white, font=font_body)
            draw.text((250, y_pos), desc, fill=text_gray, font=font_body)
            draw.line([60, y_pos + 26, 460, y_pos + 26], fill=(40, 45, 60), width=1)
            y_pos += 38

        # Draw Warning Box
        draw.rounded_rectangle([60, 470, 460, 540], radius=8, fill=(45, 20, 25))
        draw.text((75, 480), "⚠️ 5+ Network Hops | >380ms Query Latency", fill=(255, 120, 120), font=font_badge)
        draw.text((75, 505), "⚠️ Complex ETL Pipelines & Glue Code", fill=(255, 120, 120), font=font_badge)

        # Right Box: SyntricDB
        draw.rounded_rectangle([520, 110, 960, 560], radius=16, fill=(18, 38, 48), outline=green_accent, width=2)
        draw.rectangle([520, 110, 960, 160], fill=(15, 55, 45))
        draw.text((540, 125), "⚡ SyntricDB Unified AI-Native Engine", fill=green_accent, font=font_card_header)

        syntric_items = [
            ("📊 Unified SQL Engine", "Native SQL + ACID Storage"),
            ("🧠 HNSW Vector Index", "Built-in Vector Similarity"),
            ("📖 BM25 Full-Text Index", "Inverted Text Search"),
            ("⚡ In-Memory Cache", "Sub-millisecond LRU Cache"),
            ("🌊 Stream Processing", "Real-time Pub/Sub Events"),
            ("🤖 AI SQL Functions", "AI_EMBED() & AI_SUMMARIZE()")
        ]

        y_pos = 180
        for item, desc in syntric_items:
            draw.text((540, y_pos), item, fill=text_white, font=font_body)
            draw.text((740, y_pos), desc, fill=green_accent, font=font_body)
            draw.line([540, y_pos + 26, 940, y_pos + 26], fill=(30, 60, 65), width=1)
            y_pos += 38

        # Draw Success Box
        draw.rounded_rectangle([540, 470, 940, 540], radius=8, fill=(15, 60, 45))
        draw.text((555, 480), "✅ 0 Extra Network Hops | <12ms Query Latency", fill=(120, 255, 180), font=font_badge)
        draw.text((555, 505), "✅ Single Connection String & Zero ETL Pipelines", fill=(120, 255, 180), font=font_badge)

        frames.append(img)

    # --- SLIDE 2: Feature Matrix ---
    for step in range(30):
        img, draw = draw_base_layout(
            "📊 Feature-by-Feature Capability Comparison",
            "SyntricDB consolidates 5 standalone database engines into a single unified binary"
        )

        matrix = [
            ("Feature Capability", "Postgres + Stack", "SyntricDB Engine"),
            ("SQL Query Engine", "✅ Postgres", "✅ Native Unified SQL"),
            ("Vector Similarity (HNSW)", "❌ Needs Pinecone/Pgvector", "✅ Built-in HNSW Index"),
            ("Full-Text BM25 Search", "❌ Needs Elasticsearch", "✅ Built-in Inverted Index"),
            ("In-Memory Caching", "❌ Needs Redis", "✅ Integrated Memory Engine"),
            ("Event Streaming", "❌ Needs Kafka", "✅ Native Topic Pub/Sub"),
            ("AI Functions in SQL", "❌ Custom Code Required", "✅ AI_EMBED() / AI_SUMMARIZE()"),
            ("Deployment Complexity", "❌ 5 Servers & Configs", "✅ 1 Java Binary / Container")
        ]

        y_pos = 120
        for i, (col1, col2, col3) in enumerate(matrix):
            is_header = (i == 0)
            row_bg = (30, 41, 59) if is_header else ((23, 30, 48) if i % 2 == 0 else (15, 23, 42))
            draw.rounded_rectangle([40, y_pos, 960, y_pos + 44], radius=6, fill=row_bg)

            header_color = yellow_accent if is_header else text_white
            col2_color = text_gray if is_header else ((255, 120, 120) if "❌" in col2 else green_accent)
            col3_color = yellow_accent if is_header else green_accent

            draw.text((60, y_pos + 12), col1, fill=header_color, font=font_card_header if is_header else font_body)
            draw.text((450, y_pos + 12), col2, fill=col2_color, font=font_body)
            draw.text((730, y_pos + 12), col3, fill=col3_color, font=font_body)
            y_pos += 52

        frames.append(img)

    # Save to GIF
    output_path = "/Users/upendrakumarmanike/Documents/NovaDB/syntricdb_vs_others.gif"
    frames[0].save(
        output_path,
        save_all=True,
        append_images=frames[1:],
        duration=100,
        loop=0
    )
    print(f"GIF created successfully at {output_path}")

create_comparison_gif()
