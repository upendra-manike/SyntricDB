import urllib.parse
import base64
import requests
import json

SYNTRICDB_URL = "syntricdb://admin:syntricdb_secret_pass@localhost:8080/default"

def parse_connection_url(url_str):
    clean_url = url_str.replace("jdbc:syntricdb://", "http://").replace("syntricdb://", "http://")
    parsed = urllib.parse.urlparse(clean_url)
    host_name = parsed.hostname or "localhost"
    port_num = parsed.port or 8080
    api_url = f"http://{host_name}:{port_num}/api/sql"
    
    headers = {"Content-Type": "application/json"}
    if parsed.username and parsed.password:
        auth_str = f"{parsed.username}:{parsed.password}"
        b64_auth = base64.b64encode(auth_str.encode("utf-8")).decode("utf-8")
        headers["Authorization"] = f"Basic {b64_auth}"
    
    database = parsed.path.strip("/") if parsed.path else "default"
    return api_url, headers, database

def execute_query(sql_statement):
    api_url, headers, database = parse_connection_url(SYNTRICDB_URL)
    payload = {"sql": sql_statement, "database": database}
    
    response = requests.post(api_url, json=payload, headers=headers)
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception(f"Query execution failed ({response.status_code}): {response.text}")

def main():
    print("=================================================")
    print("🐍 SyntricDB Python SDK & REST Integration Demo")
    print(f"🔗 Connection URL: {SYNTRICDB_URL}")
    print("=================================================")

    # 1. Create Table
    create_sql = """
    CREATE TABLE developers (
        id VARCHAR PRIMARY KEY,
        name VARCHAR,
        role VARCHAR,
        experience_years INT,
        bio VARCHAR,
        embedding FLOAT_VECTOR(128)
    );
    """
    try:
        execute_query(create_sql)
        print("✅ Created 'developers' table.")
    except Exception as e:
        print(f"ℹ️ Table creation info: {e}")

    # 2. Insert Records with Auto AI Embeddings
    insert_sql = """
    INSERT INTO developers VALUES (
        'dev_401',
        'Alice Johnson',
        'Senior AI Engineer',
        8,
        'Specializes in PyTorch, LLM fine-tuning, and sub-millisecond vector indexing.',
        AI_EMBED('Senior AI Engineer PyTorch LLM fine-tuning vector index')
    );
    """
    res = execute_query(insert_sql)
    print(f"✅ Inserted Developer Record: {res.get('message', 'OK')}")

    # 3. Hybrid SQL + Vector Search
    vector_sql = """
    SELECT id, name, role, experience_years, bio 
    FROM developers 
    WHERE experience_years >= 5 
      AND embedding SIMILAR TO 'LLM fine tuning vector index' 
    TOP 1;
    """
    search_res = execute_query(vector_sql)
    print("\n🔍 SyntricDB Hybrid Vector Search Results:")
    print(json.dumps(search_res, indent=2))

    print("=================================================")

if __name__ == "__main__":
    main()
