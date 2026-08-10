import os
from PIL import Image, ImageDraw, ImageFont

def create_ai_trends_gif():
    width, height = 1080, 675
    bg_dark = (10, 15, 30)         # Cyber dark background
    card_bg_dark = (18, 26, 46)
    card_bg_highlight = (24, 38, 68)

    # Vibrant Cyber Neon Palette
    neon_cyan = (0, 242, 254)
    electric_purple = (168, 85, 247)
    emerald_green = (52, 211, 153)
    amber_gold = (255, 210, 0)
    rose_pink = (244, 63, 94)
    text_white = (255, 255, 255)
    text_gray = (160, 175, 200)

    # Load Fonts
    try:
        font_main_title = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 28)
        font_sub = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 17)
        font_header = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 21)
        font_body = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 15)
        font_badge = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 13)
        font_big_stat = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 32)
    except:
        font_main_title = font_sub = font_header = font_body = font_badge = font_big_stat = ImageFont.load_default()

    frames = []

    def draw_top_bar(draw, title, badge_text):
        draw.rectangle([0, 0, width, 8], fill=electric_purple)
        draw.rounded_rectangle([40, 25, 240, 55], radius=15, fill=(60, 20, 90))
        draw.text((55, 32), badge_text, fill=electric_purple, font=font_badge)
        draw.text((260, 26), title, fill=text_white, font=font_main_title)

    # -------------------------------------------------------------------------
    # SLIDE 1: EVOLUTION OF AI TECH STACKS (2023 - 2026) (15 frames)
    # -------------------------------------------------------------------------
    for f in range(15):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "The Rapid Evolution of AI Engineering 🚀", "AI TRENDS 2026")

        draw.text((40, 75), "How AI Architecture Shifted from Simple Wrappers to Autonomous Agentic Ecosystems", fill=text_gray, font=font_sub)

        timeline = [
            ("2023", "💬 Simple Prompt Wrappers", "Basic ChatGPT APIs, naive text prompts, zero context.", (100, 110, 140)),
            ("2024", "📄 Basic RAG & Vector DBs", "Vector embeddings, Pinecone/Qdrant, naive chunking.", (50, 90, 160)),
            ("2025", "🤖 Multi-Agent Frameworks", "CrewAI, AutoGen, LangGraph, tool-calling agents.", (120, 60, 160)),
            ("2026", "🧠 Autonomous Swarms & Unified AI Platforms", "Agentic coding, hybrid HNSW+BM25 search, real-time multimodal streaming.", (168, 85, 247))
        ]

        y_pos = 125
        for year, title, desc, col in timeline:
            draw.rounded_rectangle([40, y_pos, 1040, y_pos + 105], radius=14, fill=card_bg_dark, outline=col, width=2)
            draw.rounded_rectangle([60, y_pos + 18, 160, y_pos + 85], radius=10, fill=col)
            draw.text((80, y_pos + 38), year, fill=text_white, font=font_main_title)

            draw.text((190, y_pos + 22), title, fill=text_white, font=font_header)
            draw.text((190, y_pos + 58), desc, fill=text_gray, font=font_body)

            y_pos += 122

        frames.append(img)

    # -------------------------------------------------------------------------
    # SLIDE 2: TOP 5 GAME-CHANGING AI TRENDS (15 frames)
    # -------------------------------------------------------------------------
    for f in range(15):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "Top 5 Game-Changing AI Trends Right Now 💥", "THE BREAKTHROUGHS")

        draw.text((40, 75), "The core technologies defining modern software engineering and enterprise AI.", fill=text_gray, font=font_sub)

        trends = [
            ("1. Agentic AI & Swarms 🤖", "Autonomous agents executing complex multi-step coding, debugging & ops tasks."),
            ("2. Open-Source LLM Superiority 🔓", "DeepSeek, Llama 3 & Qwen matching closed API models at 1/10th the cost."),
            ("3. Hybrid RAG & Unified Vector Engines ⚡", "Merging Vector HNSW, BM25 Search & SQL into single ultra-low latency stores."),
            ("4. Real-Time Multimodal Voice & Vision 🎙️", "End-to-end streaming models processing text, voice & video simultaneously."),
            ("5. Enterprise AI Guardrails & DLP 🛡️", "Zero-data-leak sandboxing, privacy masking, and on-premise execution.")
        ]

        y_pos = 125
        for title, desc in trends:
            draw.rounded_rectangle([40, y_pos, 1040, y_pos + 90], radius=12, fill=card_bg_dark, outline=neon_cyan, width=1)
            draw.text((65, y_pos + 18), title, fill=neon_cyan, font=font_header)
            draw.text((65, y_pos + 52), desc, fill=text_gray, font=font_body)
            y_pos += 102

        frames.append(img)

    # -------------------------------------------------------------------------
    # SLIDE 3: WHAT EVERY AI ENGINEER MUST MASTER (20 frames)
    # -------------------------------------------------------------------------
    for f in range(20):
        img = Image.new("RGB", (width, height), bg_dark)
        draw = ImageDraw.Draw(img)
        draw_top_bar(draw, "What Every Software Engineer Must Master in 2026 🎯", "THE ROADMAP")

        draw.text((40, 75), "Key skill sets needed to build production-grade, high-scale AI systems.", fill=text_gray, font=font_sub)

        skills = [
            ("🧠 Agentic Systems Design", "ReAct loops, function calling, tool orchestration & plan verification."),
            ("📊 Hybrid Vector RAG", "HNSW indexing, sparse BM25, graph memory & reranking pipelines."),
            ("⚡ Inference Optimization", "vLLM, AWQ quantization, speculative decoding & KV caching."),
            ("🛡️ AI Security & Governance", "Prompt injection defense, DLP data masking & output validation.")
        ]

        y_pos = 130
        for i, (title, desc) in enumerate(skills):
            draw.rounded_rectangle([40, y_pos, 1040, y_pos + 98], radius=14, fill=card_bg_highlight, outline=emerald_green, width=2)
            draw.text((70, y_pos + 20), title, fill=emerald_green, font=font_header)
            draw.text((70, y_pos + 56), desc, fill=text_white, font=font_body)
            y_pos += 112

        # Footer Prompt Call to Action
        draw.rounded_rectangle([40, 580, 1040, 645], radius=10, fill=(30, 58, 110))
        draw.text((60, 598), "💡 Which AI trend are you focusing on this year? Let me know in the comments! 👇", fill=amber_gold, font=font_header)

        frames.append(img)

    output_path = "/Users/upendrakumarmanike/Documents/NovaDB/ai_trends_2026.gif"
    frames[0].save(
        output_path,
        save_all=True,
        append_images=frames[1:],
        duration=120,
        loop=0
    )
    print(f"AI Trends GIF created successfully at {output_path}")

create_ai_trends_gif()
