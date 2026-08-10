import os
from PIL import Image, ImageDraw, ImageFont

def create_viral_linkedin_gif():
    width, height = 1080, 675
    bg_dark = (10, 15, 30)         # Ultra-dark cyber blue
    card_bg_dark = (18, 26, 46)
    card_bg_highlight = (24, 38, 68)

    # Neon Colors for Viral Visual Appeal
    neon_cyan = (0, 242, 254)
    electric_blue = (79, 172, 254)
    emerald_green = (0, 255, 135)
    rose_red = (255, 75, 110)
    amber_gold = (255, 210, 0)
    text_white = (255, 255, 255)
    text_gray = (160, 175, 200)

    # Load Fonts
    try:
        font_main_title = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 30)
        font_sub = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 18)
        font_header = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 22)
        font_code = ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", 16)
        font_body = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 16)
        font_badge = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 14)
        font_big_stat = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 36)
    except:
        font_main_title = font_sub = font_header = font_code = font_body = font_badge = font_big_stat = ImageFont.load_default()

    frames = []

    def draw_top_bar(draw, title, category_badge):
        # Top gradient border line
        draw.rectangle([0, 0, width, 8], fill=neon_cyan)
        # Category Tag
        draw.rounded_rectangle([40, 25, 220, 55], radius=15, fill=(30, 58, 110))
        draw.text((55, 32), category_badge, fill=neon_cyan, font=font_badge)
        # Title
        draw.text((240, 26), title, fill=text_white, font=font_main_title)

    # -------------------------------------------------------------------------
    # PART 1: THE PROBLEM (15 frames)
    # -------------------------------------------------------------------------
    for f in range(15):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "Why Modern AI Stacks Are Broken 💥", "ARCHITECTURE")

        draw.text((40, 75), "Stitching 5 standalone databases causes extreme latency, high cloud costs & ETL complexity.", fill=text_gray, font=font_sub)

        # Left Column: The Fragmented Nightmare Card
        draw.rounded_rectangle([40, 120, 520, 630], radius=18, fill=card_bg_dark, outline=rose_red, width=2)
        draw.rectangle([40, 120, 520, 175], fill=(60, 20, 35))
        draw.text((60, 138), "❌ Traditional Fragmented Stack", fill=rose_red, font=font_header)

        stack_items = [
            ("🐘 PostgreSQL", "Relational Storage", "$3,500/mo"),
            ("🔴 Redis", "In-Memory Caching", "$1,200/mo"),
            ("🔍 Elasticsearch", "BM25 Text Search", "$4,800/mo"),
            ("🌲 Pinecone", "Vector HNSW Graph", "$4,500/mo"),
            ("🌊 Apache Kafka", "Event Stream Pub/Sub", "$2,000/mo")
        ]

        y_pos = 195
        for item, desc, cost in stack_items:
            draw.text((60, y_pos), item, fill=text_white, font=font_body)
            draw.text((230, y_pos), desc, fill=text_gray, font=font_body)
            draw.text((410, y_pos), cost, fill=rose_red, font=font_body)
            draw.line([60, y_pos + 28, 500, y_pos + 28], fill=(45, 30, 45), width=1)
            y_pos += 42

        # Warning Stat Box
        draw.rounded_rectangle([60, 490, 500, 605], radius=12, fill=(45, 20, 30))
        draw.text((80, 505), "🐢 Avg Latency: 420 ms", fill=rose_red, font=font_big_stat)
        draw.text((80, 555), "💸 Total Cost: $16,000 / month | 5 Network Hops", fill=text_gray, font=font_badge)

        # Right Column: Key Pain Points
        draw.rounded_rectangle([550, 120, 1040, 630], radius=18, fill=card_bg_dark, outline=(60, 70, 100), width=1)
        draw.text((580, 145), "⚠️ Major Engineering Headaches", fill=amber_gold, font=font_header)

        pain_points = [
            "❌ 5 Network Roundtrips per request",
            "❌ Constant ETL Pipeline failures & data desync",
            "❌ 5 Security credentials & permission models",
            "❌ High cloud infrastructure bills",
            "❌ Complex local development setup"
        ]
        y_pos = 210
        for pp in pain_points:
            draw.text((580, y_pos), pp, fill=text_white, font=font_body)
            y_pos += 50

        frames.append(img)

    # -------------------------------------------------------------------------
    # PART 2: THE SOLUTION — SYNTRICDB (15 frames)
    # -------------------------------------------------------------------------
    for f in range(15):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "SyntricDB: Unified AI-Native Engine ⚡", "THE REVOLUTION")

        draw.text((40, 75), "Consolidating SQL, Vector Search, BM25 Search, Memory Cache & Streaming into 1 Java 21 Engine.", fill=text_gray, font=font_sub)

        # Main Highlight Box
        draw.rounded_rectangle([40, 120, 1040, 630], radius=18, fill=card_bg_highlight, outline=emerald_green, width=3)
        draw.rectangle([40, 120, 1040, 175], fill=(15, 60, 45))
        draw.text((60, 138), "⚡ SyntricDB Unified All-in-One Engine (Java 21 LTS + Netty 4)", fill=emerald_green, font=font_header)

        # 3 Big Stat Badges
        draw.rounded_rectangle([70, 200, 360, 310], radius=14, fill=(10, 40, 30), outline=emerald_green, width=1)
        draw.text((90, 215), "⚡ 11.4 ms", fill=emerald_green, font=font_big_stat)
        draw.text((90, 265), "36x Faster Query Latency", fill=text_gray, font=font_badge)

        draw.rounded_rectangle([390, 200, 680, 310], radius=14, fill=(10, 40, 30), outline=neon_cyan, width=1)
        draw.text((410, 215), "💰 $450 /mo", fill=neon_cyan, font=font_big_stat)
        draw.text((410, 265), "97% Cloud Cost Reduction", fill=text_gray, font=font_badge)

        draw.rounded_rectangle([710, 200, 1010, 310], radius=14, fill=(10, 40, 30), outline=amber_gold, width=1)
        draw.text((730, 215), "🔌 1 Line", fill=amber_gold, font=font_big_stat)
        draw.text((730, 265), "Zero ETL | Single Connection String", fill=text_gray, font=font_badge)

        # Capabilities checklist grid
        caps = [
            ("📊 Unified SQL Engine", "Full ACID + Table Schemas"),
            ("🧠 HNSW Vector Index", "Cos Similarity Nearest Neighbors"),
            ("📖 BM25 Full-Text Index", "Inverted Term Search"),
            ("⚡ In-Memory Cache", "Sub-ms Hot LRU Caching"),
            ("🌊 Stream Pub/Sub", "Real-Time Event Topics"),
            ("🤖 Native AI SQL", "AI_EMBED() & AI_SUMMARIZE()")
        ]

        y_pos = 340
        for i, (cap, desc) in enumerate(caps):
            col_x = 70 if i % 2 == 0 else 550
            if i % 2 == 0 and i > 0:
                y_pos += 65
            draw.rounded_rectangle([col_x, y_pos, col_x + 450, y_pos + 55], radius=10, fill=(15, 23, 42))
            draw.text((col_x + 20, y_pos + 10), cap, fill=text_white, font=font_body)
            draw.text((col_x + 20, y_pos + 30), desc, fill=emerald_green, font=font_badge)

        frames.append(img)

    # -------------------------------------------------------------------------
    # PART 3: LIVE CODE & RESULT TYPING DEMO (20 frames)
    # -------------------------------------------------------------------------
    code_text = "SELECT id, title, AI_SUMMARIZE(content) AS summary\nFROM documents\nWHERE embedding SIMILAR TO 'AI Vector Engine' TOP 3;"

    for f in range(20):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "Native AI SQL Query Execution Demo 💻", "LIVE SQL CODE")

        draw.text((40, 75), "Write standard SQL with built-in vector similarity search and LLM functions.", fill=text_gray, font=font_sub)

        # IDE Code Card
        draw.rounded_rectangle([40, 120, 1040, 320], radius=14, fill=(15, 23, 42), outline=electric_blue, width=2)
        # Window controls dots
        draw.ellipse([60, 138, 72, 150], fill=(255, 95, 86))
        draw.ellipse([80, 138, 92, 150], fill=(255, 189, 46))
        draw.ellipse([100, 138, 112, 150], fill=(39, 201, 63))
        draw.text((130, 135), "syntricdb_query_editor.sql", fill=text_gray, font=font_badge)

        # Draw code
        chars_to_show = min(len(code_text), int((f + 1) * (len(code_text) / 15)))
        current_code = code_text[:chars_to_show]
        
        c_y = 175
        for line in current_code.split("\n"):
            draw.text((60, c_y), line, fill=neon_cyan, font=font_code)
            c_y += 26

        # Query Output Results Table (appears in second half)
        if f >= 8:
            draw.rounded_rectangle([40, 340, 1040, 630], radius=14, fill=card_bg_dark, outline=emerald_green, width=2)
            draw.rectangle([40, 340, 1040, 385], fill=(15, 55, 45))
            draw.text((60, 353), "✅ Execution Successful (Time: 11.2 ms | 3 Rows Returned)", fill=emerald_green, font=font_header)

            headers = ["id", "title", "summary", "_similarity_score"]
            draw.text((60, 400), "doc_101  | High Performance Vector Indexing  | Next-gen HNSW vector search... | 0.9842", fill=text_white, font=font_code)
            draw.line([60, 435, 1020, 435], fill=(40, 50, 70))
            draw.text((60, 450), "doc_204  | Unified AI Database Engine Architecture | Single binary Java 21 engine... | 0.9518", fill=text_white, font=font_code)
            draw.line([60, 485, 1020, 485], fill=(40, 50, 70))
            draw.text((60, 500), "doc_309  | BM25 Full-Text & Inverted Search  | Inverted term index matching... | 0.9104", fill=text_white, font=font_code)

            # Footer Installation Prompt
            draw.rounded_rectangle([60, 550, 1020, 610], radius=10, fill=(10, 40, 30))
            draw.text((80, 565), "🍏 One-Line Install: curl -fsSL https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/mac/install_mac.sh | bash", fill=amber_gold, font=font_body)

        frames.append(img)

    # Save to high-quality GIF
    output_path = "/Users/upendrakumarmanike/Documents/NovaDB/syntricdb_viral_showcase.gif"
    frames[0].save(
        output_path,
        save_all=True,
        append_images=frames[1:],
        duration=120,
        loop=0
    )
    print(f"Viral Showcase GIF created at {output_path}")

create_viral_linkedin_gif()
